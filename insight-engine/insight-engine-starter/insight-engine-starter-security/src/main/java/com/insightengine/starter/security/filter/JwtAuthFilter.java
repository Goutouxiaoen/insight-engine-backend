package com.insightengine.starter.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insightengine.common.core.ErrorCode;
import com.insightengine.common.core.Result;
import com.insightengine.starter.security.blacklist.TokenBlacklistService;
import com.insightengine.starter.security.util.JwtPayload;
import com.insightengine.starter.security.util.JwtUtil;
import com.insightengine.starter.web.context.LoginUser;
import com.insightengine.starter.web.context.UserContext;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * JWT 认证过滤器（挂在 Spring Security 过滤链中）。
 *
 * <p>职责：从 {@code Authorization: Bearer <token>} 头解析访问令牌，校验通过后：</p>
 * <ul>
 *   <li>将权限编码列表转为 {@link SimpleGrantedAuthority} 写入 SecurityContext，
 *       支撑 {@code @PreAuthorize("hasAuthority('kb:read')")} 方法级权限；</li>
 *   <li>同步填充 {@link UserContext}，供数据权限/审计等业务代码读取当前用户。</li>
 * </ul>
 *
 * <p>设计要点：</p>
 * <ul>
 *   <li>本过滤器只负责「有 token 时的身份建立」，不负责「无 token 的拒绝」——
 *       后者由 Spring Security 的授权规则（{@code anyRequest().authenticated()}）
 *       与 EntryPoint 统一处理，职责单一；</li>
 *   <li>token 过期返回 {@code 2007}、其他非法返回 {@code 2001}，
 *       与 IF 附录 A 对齐，便于前端区分「需刷新」与「需重新登录」；</li>
 *   <li>{@link UserContext} 的清理由 starter-web 的 {@code UserContextFilter} 兜底，
 *       本过滤器不做 finally 清理，避免过早清空导致下游读不到上下文。</li>
 * </ul>
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    /** Bearer 前缀 */
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    /** 黑名单服务（可选）：未配置时为 null，退化为无状态 JWT 校验 */
    private final TokenBlacklistService blacklistService;

    public JwtAuthFilter(JwtUtil jwtUtil, ObjectMapper objectMapper, TokenBlacklistService blacklistService) {
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
        this.blacklistService = blacklistService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        // 无 Authorization 头或非 Bearer 前缀：不建立身份，交由授权规则决定是否放行
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }
        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        // 黑名单优先于签名校验（TD §8.3 网关校验顺序：黑名单 → 签名 → 过期）：
        // 已登出的 token 即便未过期也必须拒绝，否则「主动登出」形同虚设
        if (blacklistService != null && blacklistService.isBlacklisted(token)) {
            writeUnauthorized(response, ErrorCode.UNAUTHORIZED);
            return;
        }
        try {
            JwtPayload payload = jwtUtil.parseAccessToken(token);
            authenticate(request, payload);
            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException e) {
            // 令牌过期：明确返回 2007，前端据此触发刷新流程而非直接登出
            writeUnauthorized(response, ErrorCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            // 签名非法 / 类型不符 / 格式错误：视为未登录
            writeUnauthorized(response, ErrorCode.UNAUTHORIZED);
        }
    }

    /**
     * 建立认证上下文并填充用户上下文。
     */
    private void authenticate(HttpServletRequest request, JwtPayload payload) {
        // 权限编码 → 权限对象；空列表时 token 仍视为「已认证」但无任何权限
        List<SimpleGrantedAuthority> authorities = payload.getPermissions().stream()
                .map(SimpleGrantedAuthority::new)
                .toList();

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(payload.getUserId(), null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 同步填充用户上下文（数据权限/审计用）
        UserContext.set(new LoginUser(payload.getUserId(), payload.getTenantId(),
                payload.getWorkspaceId(), payload.getRoles()));
    }

    /**
     * 写出统一格式的 401 响应，不进入后续过滤链。
     */
    private void writeUnauthorized(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getHttpStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(Result.fail(errorCode)));
    }
}
