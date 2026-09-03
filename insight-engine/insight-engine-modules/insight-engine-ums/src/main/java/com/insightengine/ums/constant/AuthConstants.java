package com.insightengine.ums.constant;

/**
 * UMS 认证与用户相关常量。
 *
 * <p>集中管理登录锁定阈值、Redis Key 前缀、默认角色等，避免魔法数字/字符串散落。
 * Key 命名遵循 TD §6.1 规范 {@code ie:auth:*}/{@code ie:user:*}}。</p>
 */
public final class AuthConstants {

    private AuthConstants() {
        // 工具类禁止实例化
    }

    /* ============ 登录安全策略（PRD §12.1.5） ============ */

    /** 连续密码错误达到该次数后锁定账号 */
    public static final int MAX_LOGIN_FAIL_COUNT = 5;

    /** 账号锁定时长（秒）：30 分钟 */
    public static final long LOGIN_LOCK_SECONDS = 30 * 60L;

    /* ============ Redis Key 前缀（TD §6.1） ============ */

    /** 登录失败计数 Key：ie:auth:login-fail:{account}，TTL=锁定窗口 */
    public static final String KEY_LOGIN_FAIL = "ie:auth:login-fail:";

    /** 账号锁定 Key：ie:auth:lock:{account}，TTL=锁定时长 */
    public static final String KEY_LOGIN_LOCK = "ie:auth:lock:";

    /** 登录态 Key：ie:auth:token:{userId}，值=access token 摘要，TTL=2h */
    public static final String KEY_AUTH_TOKEN = "ie:auth:token:";

    /** refresh 会话 Key：ie:auth:refresh:{userId}，值=当前有效 refresh token 的 jti 摘要，TTL=7d */
    public static final String KEY_AUTH_REFRESH = "ie:auth:refresh:";

    /** 登出黑名单 Key：ie:auth:blacklist:{tokenHash}，TTL=token 剩余有效期 */
    public static final String KEY_AUTH_BLACKLIST = "ie:auth:blacklist:";

    /* ============ 注册默认值（MVP 单租户） ============ */

    /** 默认租户 ID（init.sql 种子，MVP 单租户） */
    public static final Long DEFAULT_TENANT_ID = 1L;

    /** 默认组织 ID（init.sql 种子数据） */
    public static final Long DEFAULT_ORG_ID = 1L;

    /** 默认工作空间 ID（init.sql 种子数据，注册用户挂到默认空间） */
    public static final Long DEFAULT_WORKSPACE_ID = 1L;

    /** 注册用户默认角色编码：end_user（init.sql 预置角色 id=5） */
    public static final String DEFAULT_ROLE_CODE = "end_user";

    /* ============ 账号状态（与 Constants 一致，此处做 UMS 语义化命名） ============ */

    /** 账号正常 */
    public static final int ACCOUNT_NORMAL = 1;

    /** 账号禁用 */
    public static final int ACCOUNT_DISABLED = 0;
}
