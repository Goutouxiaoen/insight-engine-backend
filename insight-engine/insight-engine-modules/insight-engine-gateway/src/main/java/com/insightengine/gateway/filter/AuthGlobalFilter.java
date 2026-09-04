package com.insightengine.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insightengine.common.constant.Constants;
import com.insightengine.common.core.ErrorCode;
import com.insightengine.common.core.Result;
import com.insightengine.gateway.security.GatewayJwtParser;
import com.insightengine.gateway.security.GatewayJwtPayload;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 网关统一认证过滤器（TD §8.3 AuthGlobalFilter，WebFlux 全局过滤器）。
 *
 * <p>为什么认证要在网关做：网关是流量唯一入口，在这里统一校验令牌可实现
 * 「一次校验、全局生效」，避免每个业务服务重复解析 JWT（ADR-5 网关校验模式）。
 * 认证通过后网关把身份写入标准明文头（X-User-Id / X-Tenant-Id / X-Workspace-Id / X-Roles）
 * 下发下游——业务服务默认自校验 JWT（保留原 Authorization 头），
 * 需要走「信任网关头」模式的服务再按开关读取（见 PROGRESS 2026-09-03 认证模型定案）。</p>
 *
 * <p>安全要点：</p>
 * <ul>
 *   <li><b>防客户端伪造身份头</b>：X-User-Id 等明文头是「内网可信」约定，客户端可随意伪造，
 *       故在放行/转发前一律先清除同名头，再由网关基于签名校验结果重新写入，
 *       保证下游拿到的身份头只可能来自网关；</li>
 *   <li><b>错误码对齐 IF 附录 A</b>：token 过期 → 2007，签名非法/未带凭证 → 2001；
 *       网关只做「认证」（401），「授权」（403）交给业务服务按权限码校验；</li>
 *   <li><b>白名单</b>：登录/注册/刷新与 Knife4j 文档资源无需认证，其余路径一律要求有效令牌。</li>
 * </ul>
 *
 * <p>执行顺序：{@link #getOrder()} = -100，位于路由转发过滤器之前；
 * 后续若补充网关 TraceFilter（生成/透传 traceId），其 order 应取更小值（如 -200）保证最先执行。</p>
 */
@Slf4j
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    /** 认证白名单（与 UMS starter-security 放行路径一致：登录/注册/刷新 + Knife4j 文档资源） */
    private static final List<String> WHITELIST_PATTERNS = List.of(
            "/auth/login", "/auth/register", "/auth/refresh",
            "/v3/api-docs/**", "/swagger-ui/**", "/swagger-resources/**",
            "/webjars/**", "/doc.html", "/favicon.ico");

    /** 网关下发的用户身份明文头（TD §4.5），客户端伪造时必须清除后重建 */
    private static final List<String> IDENTITY_HEADERS = List.of(
            Constants.HEADER_USER_ID, Constants.HEADER_TENANT_ID,
            Constants.HEADER_WORKSPACE_ID, Constants.HEADER_ROLES);

    /** Authorization 请求头名 */
    private static final String AUTH_HEADER = "Authorization";
    /** Bearer 前缀（管理端 JWT） */
    private static final String BEARER_PREFIX = "Bearer ";
    /** API Key 前缀（IF §2.4：OpenAPI 通道 sk-ins-xxx） */
    private static final String API_KEY_PREFIX = "sk-";

    /** 兜底响应：Result 序列化极端失败时返回的固定 500（防吞异常且保证可解析） */
    private static final byte[] FALLBACK_ERROR_BYTES =
            "{\"code\":9999,\"message\":\"系统内部错误\"}".getBytes(StandardCharsets.UTF_8);

    private final GatewayJwtParser jwtParser;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public AuthGlobalFilter(GatewayJwtParser jwtParser, ObjectMapper objectMapper) {
        this.jwtParser = jwtParser;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // 白名单（登录/注册/刷新/文档）：无需认证，仅清除客户端伪造的身份头后放行，
        // 避免伪造头在「业务服务信任网关头」开启时造成越权
        if (isWhitelisted(path)) {
            return chain.filter(exchange.mutate().request(stripIdentityHeaders(request)).build());
        }

        String token = resolveBearerToken(request);
        // 无凭证 / 非 Bearer 格式：未登录
        if (token == null) {
            return writeError(exchange, ErrorCode.UNAUTHORIZED);
        }

        // API Key 通道（sk-ins-xxx）：MVP 阶段 API Key 由工作空间签发、校验依赖密钥存储
        //（workspace/conv 阶段落地），当前一律视为无效凭证，接入点见 TD §7.6
        if (token.startsWith(API_KEY_PREFIX)) {
            log.warn("[gateway] API Key 通道未启用，拒绝访问 path={}", path);
            return writeError(exchange, ErrorCode.UNAUTHORIZED);
        }

        // JWT 校验：签名/过期/类型由 GatewayJwtParser 统一判定
        final GatewayJwtPayload payload;
        try {
            payload = jwtParser.parseAccessToken(token);
        } catch (ExpiredJwtException e) {
            // token 过期：前端据此触发刷新流程而非直接登出（IF 附录 A：2007）
            return writeError(exchange, ErrorCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            // 签名非法 / 类型不符 / 格式错误：视为未登录
            log.warn("[gateway] JWT 校验失败 path={}, reason={}", path, e.getMessage());
            return writeError(exchange, ErrorCode.UNAUTHORIZED);
        }

        // 校验通过：清掉伪造头 → 基于可信载荷重建身份头 → 放行（保留原 Authorization 供服务自校验）
        ServerHttpRequest authedRequest = rewriteIdentityHeaders(request, payload);
        return chain.filter(exchange.mutate().request(authedRequest).build());
    }

    /**
     * 过滤器执行顺序：在路由转发（NettyRoutingFilter）之前执行。
     * 预留 -200 给未来的 TraceFilter，保证 traceId 先生成、认证日志可关联链路。
     */
    @Override
    public int getOrder() {
        return -100;
    }

    /**
     * 路径是否命中认证白名单。
     * <p>用 Ant 通配符匹配，与 UMS starter 的 {@code requestMatchers("/v3/api-docs/**")} 语义一致。</p>
     */
    private boolean isWhitelisted(String path) {
        return WHITELIST_PATTERNS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    /**
     * 从 {@code Authorization: Bearer <token>} 头提取令牌；
     * 头缺失或非 Bearer 前缀返回 {@code null}（视为未携带凭证）。
     */
    private String resolveBearerToken(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst(AUTH_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return authHeader.substring(BEARER_PREFIX.length()).trim();
    }

    /**
     * 移除客户端可能伪造的全部身份头。
     */
    private ServerHttpRequest stripIdentityHeaders(ServerHttpRequest request) {
        return request.mutate()
                .headers(headers -> IDENTITY_HEADERS.forEach(headers::remove))
                .build();
    }

    /**
     * 重建可信身份头：先清空旧的（防伪造），再写入由签名 JWT 解析出的身份。
     * <p>tenantId/workspaceId 可为 null（组织级管理员 token 不含 ws_id），此时不下发对应头；
     * roles 为空也不下发空头，下游 {@code UserContextFilter} 按缺失处理为空角色。</p>
     */
    private ServerHttpRequest rewriteIdentityHeaders(ServerHttpRequest request, GatewayJwtPayload payload) {
        return request.mutate()
                .headers(headers -> {
                    IDENTITY_HEADERS.forEach(headers::remove);
                    headers.set(Constants.HEADER_USER_ID, String.valueOf(payload.userId()));
                    if (payload.tenantId() != null) {
                        headers.set(Constants.HEADER_TENANT_ID, String.valueOf(payload.tenantId()));
                    }
                    if (payload.workspaceId() != null) {
                        headers.set(Constants.HEADER_WORKSPACE_ID, String.valueOf(payload.workspaceId()));
                    }
                    if (!payload.roles().isEmpty()) {
                        // 多个角色按逗号拼接（下游 UserContextFilter 按逗号拆分，见其 ROLE_SEPARATOR）
                        headers.set(Constants.HEADER_ROLES, String.join(",", payload.roles()));
                    }
                })
                .build();
    }

    /**
     * 写出统一格式的错误响应（JSON + Result 结构），并直接结束请求不进入路由。
     * <p>错误响应的 traceId 回填请求头携带的 X-Trace-Id，保证前端/日志可按链路定位
     * （网关自身 TraceFilter 落地前，透传客户端已携带的 traceId）。</p>
     */
    private Mono<Void> writeError(ServerWebExchange exchange, ErrorCode errorCode) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.valueOf(errorCode.getHttpStatus()));
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Result<Void> result = Result.fail(errorCode);
        String traceId = exchange.getRequest().getHeaders().getFirst(Constants.HEADER_TRACE_ID);
        result.setTraceId(StringUtils.hasText(traceId) ? traceId : null);

        // 先赋兜底值：try 序列化成功会覆盖；catch 中可能抛未捕获异常（日志/状态码写入失败），
        // 若最后才赋值会被编译器判定「可能未初始化」，故以声明默认值消除该不可达分支的不确定性
        byte[] body = FALLBACK_ERROR_BYTES;
        try {
            body = objectMapper.writeValueAsBytes(result);
        } catch (JsonProcessingException e) {
            // 序列化固定结构的 Result 失败属极端异常：记录后返回固定 500，不吞异常
            log.error("[gateway] 序列化认证失败响应异常 traceId={}", traceId, e);
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        DataBuffer buffer = response.bufferFactory().wrap(body);
        return response.writeWith(Mono.just(buffer));
    }
}
