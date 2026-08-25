package com.insightengine.starter.web.filter;

import cn.hutool.core.util.StrUtil;
import com.insightengine.common.constant.Constants;
import com.insightengine.starter.web.context.LoginUser;
import com.insightengine.starter.web.context.UserContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 用户上下文过滤器。
 *
 * <p>职责（TD §4.5 / ADR-5）：网关已解析 JWT 并通过明文头下发用户身份，
 * 本过滤器从请求头解析这些信息，组装 {@link LoginUser} 写入 {@link UserContext}，
 * 供业务代码（数据权限、审计）使用。业务服务因此无需重复解析 JWT、也无需持有 secret。</p>
 *
 * <p>若请求缺少用户头（如白名单接口、内部调用），则不填充上下文，
 * 此时 {@code UserContext.get()} 返回 {@code null}，由业务自行判断。</p>
 */
public class UserContextFilter extends OncePerRequestFilter {

    /** 角色编码分隔符（网关按逗号拼接多个角色） */
    private static final String ROLE_SEPARATOR = ",";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            LoginUser loginUser = parse(request);
            if (loginUser != null) {
                UserContext.set(loginUser);
            }
            filterChain.doFilter(request, response);
        } finally {
            // 与 TraceFilter 同理，请求结束必须清理，防止线程复用时上下文串号
            UserContext.clear();
        }
    }

    /**
     * 从请求头解析登录用户；若关键头缺失则返回 {@code null}。
     * <p>userId 缺失视为未携带身份（匿名/白名单请求），不做兜底填充。</p>
     */
    private LoginUser parse(HttpServletRequest request) {
        String userIdStr = request.getHeader(Constants.HEADER_USER_ID);
        if (StrUtil.isBlank(userIdStr)) {
            return null;
        }

        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(parseLong(userIdStr));
        loginUser.setTenantId(parseLong(request.getHeader(Constants.HEADER_TENANT_ID)));
        loginUser.setWorkspaceId(parseLong(request.getHeader(Constants.HEADER_WORKSPACE_ID)));
        loginUser.setRoles(parseRoles(request.getHeader(Constants.HEADER_ROLES)));
        return loginUser;
    }

    /**
     * 解析角色编码列表；空头返回空列表，避免下游 NPE。
     */
    private List<String> parseRoles(String rolesHeader) {
        if (StrUtil.isBlank(rolesHeader)) {
            return Collections.emptyList();
        }
        return Arrays.stream(rolesHeader.split(ROLE_SEPARATOR))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .toList();
    }

    /**
     * 安全解析 Long；非法值返回 {@code null}（网关下发异常时降级，不抛异常中断请求）。
     */
    private Long parseLong(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException e) {
            // 网关下发的头理论上是可信的，防御性容错：异常值按缺失处理
            return null;
        }
    }
}
