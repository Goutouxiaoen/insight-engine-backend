package com.insightengine.ums.service;

import com.insightengine.starter.security.blacklist.TokenBlacklistService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;

/**
 * 基于 Redis 的 Token 黑名单实现（TD ADR-10）。
 *
 * <p>登出/踢人时把 token 加入黑名单（TD §6.1：{@code ie:auth:blacklist:{tokenHash}}），
 * 认证过滤器据此拒绝已失效 token，实现「主动登出」。</p>
 *
 * <p>设计要点：</p>
 * <ul>
 *   <li>key 用 token 的 SHA-256 摘要而非明文，避免 token 泄露在 Redis 键中
 *       （运维排查 Redis 时也不至于直接拿到可用 token）；</li>
 *   <li>TTL 设为 token 剩余有效期，到期自动清除——黑名单条目不会无限膨胀，
 *       且 token 过期后本就失效，无需继续保留。</li>
 * </ul>
 */
@Service
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
     *
     * <p>用 JDK 自带 {@link MessageDigest} 计算摘要，避免为单个哈希函数引入额外依赖
     * （starter-web 仅带 hutool-core，不含 crypto）。</p>
     */
    private String blacklistKey(String token) {
        return com.insightengine.ums.constant.AuthConstants.KEY_AUTH_BLACKLIST + sha256Hex(token);
    }

    /**
     * 计算字符串的 SHA-256 十六进制摘要。
     */
    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                String s = Integer.toHexString(b & 0xFF);
                if (s.length() == 1) {
                    hex.append('0');
                }
                hex.append(s);
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 是 JDK 标准算法，理论上必然存在；防御性兜底抛运行时异常
            throw new IllegalStateException("SHA-256 算法不可用", e);
        }
    }
}
