package com.insightengine.starter.security.token;

import com.insightengine.starter.security.util.JwtUtil;

import java.util.List;
import java.util.UUID;

/**
 * 令牌签发的<b>唯一入口</b>（token 载荷口径的单一实现点）。
 *
 * <h3>为什么需要它</h3>
 * <p>系统里有三个"签发 token"的入口：<b>登录</b>（UMS）、<b>刷新</b>（UMS）、<b>切换空间</b>（workspace）。
 * 三个入口若各自 new/拼装，就会出现"同一语义三种口径"——2026-09-17 的 BE-20260916-01 正是如此
 * （登录按用户维度算权限，切换却按空间维度算，导致切完空间丢组织级能力）。</p>
 *
 * <p>收口后，三个入口都必须调用 {@link #issue}，于是"载荷里放什么、refresh 里带不带 {@code ws_id}
 * 一律照这里来"，<b>结构上不可能再漂移</b>；口径表见 IF §3。</p>
 *
 * <h3>口径规则（改之前必读）</h3>
 * <ul>
 *   <li><b>roles / permissions</b>：按「用户」维度全量（跨空间聚合），调用方从
 *       {@link com.insightengine.common.constant.AuthQuerySql} 的两条权威 SQL 查询；
 *       <b>禁止按空间裁剪</b>（身份 ≠ 上下文）；</li>
 *   <li><b>workspaceId</b>：当前上下文。登录 = 默认空间；<b>刷新 = 沿用旧令牌的 ws_id</b>（不重算，
 *       否则会把用户悄悄切回默认空间）；切换空间 = 目标空间；</li>
 *   <li><b>refresh token 也携带 ws_id</b>：这样"刷新沿用当前空间"无需额外存储即可实现；</li>
 *   <li>需要可为空的 {@code tenantId}（异常数据兜底）由调用方决定，本类只负责装载。</li>
 * </ul>
 */
public class AuthTokenIssuer {

    private final JwtUtil jwtUtil;

    public AuthTokenIssuer(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /**
     * 签发一对令牌（access + refresh）。
     *
     * @param userId      用户 ID
     * @param tenantId    租户 ID（可空）
     * @param workspaceId 当前工作空间 ID（登录=默认空间；刷新=沿用旧令牌；切换=目标空间）
     * @param roles       角色编码（用户维度全量）
     * @param permissions 权限编码（用户维度全量）
     */
    public IssuedTokens issue(Long userId, Long tenantId, Long workspaceId,
                              List<String> roles, List<String> permissions) {
        String accessToken = jwtUtil.createAccessToken(userId, tenantId, workspaceId, roles, permissions);
        // jti：refresh 一次性轮换 / 重放检测的唯一凭据（调用方需把摘要写入会话缓存）
        String refreshJti = UUID.randomUUID().toString().replace("-", "");
        String refreshToken = jwtUtil.createRefreshToken(userId, refreshJti, workspaceId);
        return new IssuedTokens(accessToken, refreshToken, refreshJti,
                jwtUtil.getAccessTtlSeconds(), jwtUtil.getRefreshTtlSeconds());
    }
}
