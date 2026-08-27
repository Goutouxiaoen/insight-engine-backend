package com.insightengine.starter.security.blacklist;

/**
 * Token 黑名单服务接口（可选能力）。
 *
 * <p>用于「主动登出/踢人」场景（TD ADR-10：JWT 存 Redis 摘要 + 黑名单，支持主动失效）。
 * starter-security 只定义接口、不绑定实现：</p>
 * <ul>
 *   <li>引入 Redis 的服务（如 UMS）提供 {@code RedisTokenBlacklistService} 实现；</li>
 *   <li>未提供实现的服务，JWT 校验退化为「仅签名 + 过期校验」（无状态 JWT）。</li>
 * </ul>
 *
 * <p>这样 starter-security 保持对 Redis 的零依赖，可被任何业务服务复用。</p>
 */
public interface TokenBlacklistService {

    /**
     * 判断令牌是否已被加入黑名单。
     *
     * @param token 完整令牌原文
     * @return true 表示已登出/失效，应拒绝
     */
    boolean isBlacklisted(String token);

    /**
     * 将令牌加入黑名单。
     *
     * @param token      完整令牌原文
     * @param ttlSeconds 黑名单存活时长（秒），应等于令牌剩余有效期，到期自动清除避免无限膨胀
     */
    void blacklist(String token, long ttlSeconds);
}
