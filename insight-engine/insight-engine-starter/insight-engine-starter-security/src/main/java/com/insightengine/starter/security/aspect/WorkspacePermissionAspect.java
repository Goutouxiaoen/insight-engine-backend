package com.insightengine.starter.security.aspect;

import com.insightengine.common.annotation.WorkspacePermission;
import com.insightengine.common.core.BizException;
import com.insightengine.common.core.ErrorCode;
import com.insightengine.starter.security.workspace.WorkspacePermissionChecker;
import com.insightengine.starter.web.context.UserContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.ReflectivePropertyAccessor;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link WorkspacePermission} 的判定切面：**第二层鉴权**（空间维度）。
 *
 * <h3>执行时机与顺序</h3>
 * <pre>
 * 请求 → JwtAuthFilter（解析 token → perms 进 SecurityContext）→ Controller 方法
 *      →【第一层】@PreAuthorize（动作类别；无权限 403/2006）
 *      →【第二层】本切面（空间维度；无权限 403/2006）
 *      → 方法体
 * </pre>
 *
 * <p>顺序说明：`@PreAuthorize` 由 Spring Security 的方法级 AOP 拦截，本切面显式声明
 * {@code @Order(Ordered.LOWEST_PRECEDENCE)} 保证**后执行**（此前依赖"默认就是最低优先级"的隐式约定，
 * 2026-09-17 code review 要求显式化）；两层都拒绝时先由第一层给出 2006——语义上"先说动作、再说空间"。</p>
 *
 * <h3>空间 ID 的取值</h3>
 * <ol>
 *   <li>注解写了 {@code workspaceIdExpr} → 按 SpEL 相对方法参数求值（如 {@code #request.workspaceId}）；
 *       <b>需要编译期保留参数名</b>（根 POM 已开 {@code -parameters}）；</li>
 *   <li>未写 → 用当前 token 的 {@code ws_id}（{@code UserContext.getWorkspaceId()}）；</li>
 *   <li>两者都取不到 → 视为"无法判定"，直接拒绝（fail-fast，不静默放行）。</li>
 * </ol>
 *
 * <h3>为什么失败要抛 2006 而不是 1003</h3>
 * <p>语义上这是"权限不足"（该用户在目标空间没有该权限），与业务规则禁止（1003）区分开：
 * 前端据此提示"无权限"，而 1003 提示"操作不允许"。</p>
 */
@Aspect
@Order(Ordered.LOWEST_PRECEDENCE)
public class WorkspacePermissionAspect {

    private static final Logger log = LoggerFactory.getLogger(WorkspacePermissionAspect.class);

    private static final ParameterNameDiscoverer PARAMETER_NAME_DISCOVERER = new DefaultParameterNameDiscoverer();
    private static final SpelExpressionParser SPEL_PARSER = new SpelExpressionParser();

    /**
     * SpEL 表达式缓存：{@code 表达式字符串 → Expression}。
     *
     * <p>实测结论（2026-09-17，`javap` 核对 spring-expression 6.1.6）：{@code SpelExpressionParser.parseExpression(String)}
     * **没有内部表达式缓存**（{@code InternalSpelExpressionParser} 只缓存了正则 {@code patternCache}），
     * 因此每请求都会重新构建 AST。表达式集合来自注解、数量有限且稳定，按字符串缓存即可。
     * 缓存的是**解析结果**（不可变、线程安全），求值上下文仍是每请求新建（含参数绑定）。</p>
     */
    private static final Map<String, Expression> EXPRESSION_CACHE = new ConcurrentHashMap<>(8);

    private final WorkspacePermissionChecker checker;

    public WorkspacePermissionAspect(WorkspacePermissionChecker checker) {
        this.checker = checker;
    }

    @Around("@annotation(workspacePermission)")
    public Object around(ProceedingJoinPoint joinPoint, WorkspacePermission workspacePermission) throws Throwable {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            // 未登录：正常链路上 AuthenticationEntryPoint 已拦，这里兜底拒绝
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        Long workspaceId = resolveWorkspaceId(joinPoint, workspacePermission);
        if (workspaceId == null) {
            log.warn("{} 无法确定目标空间（workspaceIdExpr 未写且 token 无 ws_id），按拒绝处理: {}",
                    workspacePermission.value(), joinPoint.getSignature().toShortString());
            throw new BizException(ErrorCode.FORBIDDEN, "无法确定目标工作空间");
        }

        if (!checker.has(userId, workspaceId, workspacePermission.value())) {
            String message = StringUtils.hasText(workspacePermission.message())
                    ? workspacePermission.message()
                    : "您在当前工作空间没有该操作权限";
            log.info("空间维度权限不足: userId={} workspaceId={} permission={} method={}",
                    userId, workspaceId, workspacePermission.value(), joinPoint.getSignature().toShortString());
            throw new BizException(ErrorCode.FORBIDDEN, message);
        }
        return joinPoint.proceed();
    }

    /**
     * 解析目标空间 ID：注解表达式优先，其次当前 token 的 ws_id。
     */
    private Long resolveWorkspaceId(ProceedingJoinPoint joinPoint, WorkspacePermission annotation) {
        String expr = annotation.workspaceIdExpr();
        if (!StringUtils.hasText(expr)) {
            return UserContext.getWorkspaceId();
        }
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.addPropertyAccessor(new ReflectivePropertyAccessor());
        Object[] args = joinPoint.getArgs();
        String[] paramNames = PARAMETER_NAME_DISCOVERER.getParameterNames(method);
        if (paramNames == null) {
            // 编译期未保留参数名（-parameters 缺失）→ 表达式必然解析失败，明确报错而不是静默兜底
            throw new BizException(ErrorCode.SYSTEM_ERROR,
                    "无法解析方法参数名，请确认编译开启 -parameters（根 POM maven-compiler-plugin）");
        }
        for (int i = 0; i < paramNames.length; i++) {
            context.setVariable(paramNames[i], args[i]);
        }
        // 表达式按字符串缓存（解析一次，复用多次），求值上下文仍每请求新建
        Expression expression = EXPRESSION_CACHE.computeIfAbsent(expr, SPEL_PARSER::parseExpression);
        Object value = expression.getValue(context);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.valueOf(value.toString());
        } catch (NumberFormatException e) {
            throw new BizException(ErrorCode.PARAM_ERROR, "目标工作空间参数类型非法: " + expr);
        }
    }
}
