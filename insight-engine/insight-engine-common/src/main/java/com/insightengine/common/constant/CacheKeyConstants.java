package com.insightengine.common.constant;

/**
 * Redis 缓存键契约（TD §6.1 缓存键表）。
 *
 * <p>为什么下沉到 common：认证域的 Redis 键是「写入方（UMS 登录/刷新/登出/锁定）」与
 * 「读取方（各业务服务 starter-redis 的登录态/黑名单校验、workspace 切换空间换签）」
 * 共享的跨进程契约。若各服务各写一份字面量，一旦改名就会出现「A 写 key、B 查不到」
 * 的静默失效（登录态永远判定失效），与 {@link JwtClaimConstants} 同一类 drift 风险，
 * 故集中于此单点定义。</p>
 *
 * <p>命名规范（TD §6.1）：{@code ie:{域}:{对象}:{标识}}，冒号结尾表示后接标识拼接。</p>
 */
public final class CacheKeyConstants {

    private CacheKeyConstants() {
        // 工具类禁止实例化
    }

    /* ============ 认证域（ie:auth:*，TD §6.1） ============ */

    /** 登录失败计数键：{@code ie:auth:login-fail:{account}}，TTL=锁定窗口（30min） */
    public static final String AUTH_LOGIN_FAIL = "ie:auth:login-fail:";

    /** 账号锁定键：{@code ie:auth:lock:{account}}，TTL=锁定时长（30min） */
    public static final String AUTH_LOGIN_LOCK = "ie:auth:lock:";

    /** 登录态键：{@code ie:auth:token:{userId}}，值=当前有效 access token 的 SHA-256 摘要，TTL=access 有效期（2h） */
    public static final String AUTH_TOKEN = "ie:auth:token:";

    /** refresh 会话键：{@code ie:auth:refresh:{userId}}，值=当前有效 refresh token 的 jti 摘要，TTL=refresh 有效期（7d） */
    public static final String AUTH_REFRESH = "ie:auth:refresh:";

    /** 登出黑名单键：{@code ie:auth:blacklist:{tokenHash}}，TTL=token 剩余有效期 */
    public static final String AUTH_BLACKLIST = "ie:auth:blacklist:";
}
