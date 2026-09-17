package com.insightengine.starter.redis.session;

import com.insightengine.common.constant.CacheKeyConstants;
import com.insightengine.starter.security.util.TokenDigestUtil;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

/**
 * 服务端会话缓存（登录态 + refresh 会话）的<b>唯一读写入口</b>。
 *
 * <h3>为什么收口</h3>
 * <p>令牌签发后要落地两个 Redis 键，而"怎么写、写什么、TTL 多长"原本在 UMS（登录/刷新）与
 * workspace（切换空间）各写一份；<b>键名与摘要算法一漂移就会静默失效</b>（登录态永远判定无效）。
 * 现在写入方（UMS 登录/刷新、workspace 切换）、校验方（{@link RedisTokenSessionService}）、
 * 清理方（登出/改密/禁用）共用本类，口径单一。</p>
 *
 * <h3>键语义（TD §6.1，字面量见 {@link CacheKeyConstants}）</h3>
 * <ul>
 *   <li>{@code ie:auth:token:{userId}} = 当前有效 access token 的 SHA-256 摘要，TTL = access 有效期；</li>
 *   <li>{@code ie:auth:refresh:{userId}} = 当前有效 refresh 的 <b>jti</b> 摘要，TTL = refresh 有效期；</li>
 *   <li>只存摘要不存明文：Redis 被拖库也拿不到可直接使用的令牌（TD ADR-10）。</li>
 * </ul>
 */
public class TokenSessionCache {

    private final StringRedisTemplate stringRedisTemplate;

    public TokenSessionCache(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 记录一次签发的会话（覆盖式：重新登录 / 刷新轮换 / 切换空间都走这里）。
     *
     * <p>"覆盖"即单会话语义：写入后，<b>此前签发的 access token 立即失效</b>（摘要不匹配）。</p>
     */
    public void save(Long userId, String accessToken, String refreshJti,
                     long accessTtlSeconds, long refreshTtlSeconds) {
        stringRedisTemplate.opsForValue().set(
                CacheKeyConstants.AUTH_TOKEN + userId,
                TokenDigestUtil.sha256Hex(accessToken),
                Duration.ofSeconds(accessTtlSeconds));
        stringRedisTemplate.opsForValue().set(
                CacheKeyConstants.AUTH_REFRESH + userId,
                TokenDigestUtil.sha256Hex(refreshJti),
                Duration.ofSeconds(refreshTtlSeconds));
    }

    /**
     * 判断某个 jti 是否为「当前有效 refresh」。
     *
     * <p>返回 false 有两种含义：会话键不存在（已登出/改密/禁用被吊销）或 jti 不匹配（旧令牌重放）
     * ——调用方应据此吊销该用户全部会话。</p>
     */
    public boolean matchesRefreshJti(Long userId, String refreshJti) {
        String activeDigest = stringRedisTemplate.opsForValue().get(CacheKeyConstants.AUTH_REFRESH + userId);
        return activeDigest != null && activeDigest.equals(TokenDigestUtil.sha256Hex(refreshJti));
    }

    /**
     * 清除该用户的全部会话（登出 / 改密 / 禁用 / 检出重放时调用）。
     */
    public void clear(Long userId) {
        stringRedisTemplate.delete(CacheKeyConstants.AUTH_TOKEN + userId);
        stringRedisTemplate.delete(CacheKeyConstants.AUTH_REFRESH + userId);
    }
}
