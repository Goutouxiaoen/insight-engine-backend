package com.insightengine.starter.redis.session;

import com.insightengine.common.constant.CacheKeyConstants;
import com.insightengine.starter.security.session.TokenSessionService;
import com.insightengine.starter.security.util.TokenDigestUtil;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 基于 Redis 的登录态服务实现（TD §6.1）。
 *
 * <p>登录态缓存 {@code ie:auth:token:{userId}} 的语义：值为该用户「最新登录 / 刷新 /
 * 切换空间换签」签发 access token 的 SHA-256 摘要。写入方为 UMS（登录/刷新）与
 * workspace（切换空间换签），读取方为所有引入本 starter 的服务。</p>
 *
 * <p>认证过滤器每次请求都会调用 {@link #isActive}，只有「缓存存在 且 摘要等于当前 token」
 * 才放行。由此实现：</p>
 * <ul>
 *   <li>改密 / 禁用 / 登出删掉缓存后，旧 token 签名再合法也因查不到缓存被拒（踢人）；</li>
 *   <li>用户重新登录 / 切换空间换签覆盖缓存后，旧 token 摘要不匹配同样被拒（单会话语义）。</li>
 * </ul>
 *
 * <p>为什么实现放在 starter-redis 而非 starter-security：starter-security 刻意保持对 Redis 的
 * 零依赖（TD ADR-10，未提供实现的服务退化为纯无状态 JWT 校验）；Redis 版本实现属于
 * 「Redis 能力」，故由 starter-redis 提供并在引入后自动装配（见 RedisAutoConfiguration）。</p>
 */
public class RedisTokenSessionService implements TokenSessionService {

    private final StringRedisTemplate stringRedisTemplate;

    public RedisTokenSessionService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean isActive(Long userId, String token) {
        String cachedDigest = stringRedisTemplate.opsForValue().get(CacheKeyConstants.AUTH_TOKEN + userId);
        // 缓存被删（改密/禁用/登出）或已是别的 token 的摘要（重新登录/切换空间后旧 token）→ 判定失效
        return cachedDigest != null && cachedDigest.equals(TokenDigestUtil.sha256Hex(token));
    }
}
