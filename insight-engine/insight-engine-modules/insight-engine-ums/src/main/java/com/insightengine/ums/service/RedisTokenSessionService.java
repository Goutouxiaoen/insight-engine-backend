package com.insightengine.ums.service;

import com.insightengine.starter.security.session.TokenSessionService;
import com.insightengine.ums.constant.AuthConstants;
import com.insightengine.ums.util.TokenDigestUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 基于 Redis 的登录态服务实现（TD §6.1）。
 *
 * <p>登录态缓存 {@code ie:auth:token:{userId}} 的语义：值为该用户「最新登录/刷新」签发
 * access token 的 SHA-256 摘要。登录/刷新时写入、改密/禁用/登出时删除；</p>
 *
 * <p>认证过滤器每次请求都会调用 {@link #isActive}，只有「缓存存在 且 摘要等于当前 token」
 * 才放行。由此实现：</p>
 * <ul>
 *   <li>改密/禁用删掉缓存后，旧 token 签名再合法也因查不到缓存被拒（踢人/强制重新登录）；</li>
 *   <li>用户换设备/重新登录覆盖缓存后，旧 token 摘要不匹配同样被拒（单会话语义）。</li>
 * </ul>
 */
@Service
public class RedisTokenSessionService implements TokenSessionService {

    private final StringRedisTemplate stringRedisTemplate;

    public RedisTokenSessionService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean isActive(Long userId, String token) {
        String cachedDigest = stringRedisTemplate.opsForValue().get(AuthConstants.KEY_AUTH_TOKEN + userId);
        // 缓存被删（改密/禁用/登出）或已是别的 token 的摘要（重新登录后旧 token）→ 判定失效
        return cachedDigest != null && cachedDigest.equals(TokenDigestUtil.sha256Hex(token));
    }
}
