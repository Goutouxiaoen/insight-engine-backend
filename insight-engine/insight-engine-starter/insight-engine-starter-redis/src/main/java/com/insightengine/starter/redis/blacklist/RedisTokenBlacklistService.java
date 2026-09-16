package com.insightengine.starter.redis.blacklist;

import com.insightengine.common.constant.CacheKeyConstants;
import com.insightengine.starter.security.blacklist.TokenBlacklistService;
import com.insightengine.starter.security.util.TokenDigestUtil;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

/**
 * 基于 Redis 的 Token 黑名单实现（TD ADR-10）。
 *
 * <p>登出时把 token 加入黑名单（{@code ie:auth:blacklist:{tokenHash}}），各服务的认证过滤器
 * 据此拒绝已失效 token，实现「主动登出全局生效」——网关只校验签名，业务服务才是黑名单的
 * 实际执行点，故必须是所有服务共享一份实现，不能只在 UMS 生效。</p>
 *
 * <p>设计要点：</p>
 * <ul>
 *   <li>key 用 token 的 SHA-256 摘要而非明文，避免 token 泄露在 Redis 键中；</li>
 *   <li>TTL 设为 token 剩余有效期，到期自动清除——黑名单条目不会无限膨胀，
 *       且 token 过期后本就失效，无需继续保留。</li>
 * </ul>
 */
public class RedisTokenBlacklistService implements TokenBlacklistService {

    private final StringRedisTemplate stringRedisTemplate;

    public RedisTokenBlacklistService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean isBlacklisted(String token) {
        Boolean exists = stringRedisTemplate.hasKey(blacklistKey(token));
        return Boolean.TRUE.equals(exists);
    }

    @Override
    public void blacklist(String token, long ttlSeconds) {
        // 值存 "1" 占位即可，命中判断只依赖 key 是否存在
        stringRedisTemplate.opsForValue().set(blacklistKey(token), "1", Duration.ofSeconds(ttlSeconds));
    }

    /**
     * 生成黑名单 key：前缀 + token 的 SHA-256 摘要。
     */
    private String blacklistKey(String token) {
        return CacheKeyConstants.AUTH_BLACKLIST + TokenDigestUtil.sha256Hex(token);
    }
}
