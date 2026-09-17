package com.insightengine.ums.constant;

import com.insightengine.common.constant.CacheKeyConstants;

/**
 * UMS 认证与用户相关常量。
 *
 * <p>集中管理登录锁定阈值、Redis Key 前缀、默认角色等，避免魔法数字/字符串散落。
 * Key 命名遵循 TD §6.1 规范 {@code ie:auth:*}/{@code ie:user:*}}。</p>
 *
 * <p>注意：Redis 键前缀的真实值统一定义在 {@link CacheKeyConstants}（跨服务契约），
 * 本类只做「UMS 语义化别名」，避免 UMS / starter-redis / workspace 各写一份字面量而漂移。</p>
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

    /* ============ Redis Key 前缀（TD §6.1，值统一定义在 common CacheKeyConstants） ============ */

    /** 登录失败计数 Key：ie:auth:login-fail:{account}，TTL=锁定窗口 */
    public static final String KEY_LOGIN_FAIL = CacheKeyConstants.AUTH_LOGIN_FAIL;

    /** 账号锁定 Key：ie:auth:lock:{account}，TTL=锁定时长 */
    public static final String KEY_LOGIN_LOCK = CacheKeyConstants.AUTH_LOGIN_LOCK;

    // 说明（2026-09-17）：登录态 / refresh 会话 / 黑名单三个键的读写已收口到
    // starter-redis 的 TokenSessionCache 与 starter-security 的黑名单实现，
    // 不再在 UMS 内直接拼键（避免"同一语义多处实现"漂移），故此处不再保留对应常量。

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
