package com.insightengine.workspace.constant;

/**
 * Workspace 服务常量。
 *
 * <p>集中管理默认租户、内置角色编码等，避免魔法值散落（与 UMS {@code AuthConstants} 同风格）。</p>
 */
public final class WorkspaceConstants {

    private WorkspaceConstants() {
        // 工具类禁止实例化
    }

    /* ============ 默认值（MVP 单租户） ============ */

    /** 默认租户 ID（init.sql 种子，MVP 单租户）；上下文缺失时的兜底值 */
    public static final Long DEFAULT_TENANT_ID = 1L;

    /* ============ 内置角色编码（init.sql 预置，builtin=1） ============ */

    /** 工作空间管理员角色编码：创建空间者自动成为该角色成员，保证空间创建后可被切换/管理 */
    public static final String ROLE_WS_ADMIN = "ws_admin";

    /* ============ 组织管理判定 ============ */

    /** 组织级（及以上）管理权限编码：持有者可查看组织内全部空间，否则仅可见自己所属空间 */
    public static final String PERM_ORG_WRITE = "org:write";

    /* ============ 空间维度授权（第二层鉴权，TD §7.5） ============ */

    /**
     * 「用户在某空间的权限」缓存 TTL（秒）。
     *
     * <p>取值理由：成员变更走主动失效（精确），但**角色授权变更发生在 UMS**（跨服务），
     * 主动失效成本高，故用 10min TTL 兜底；与 TD §6.1 其它权限缓存（10min）保持一致。</p>
     */
    public static final long WS_PERM_CACHE_TTL_SECONDS = 600L;

    /** 成员查看权限（空间维度判定点，IF §5.6） */
    public static final String PERM_MEMBER_READ = "member:read";

    /** 成员添加权限（空间维度判定点，IF §5.6） */
    public static final String PERM_MEMBER_CREATE = "member:create";

    /** 成员移除权限（空间维度判定点，IF §5.6） */
    public static final String PERM_MEMBER_DELETE = "member:delete";

    /** 成员改角色权限（空间维度判定点，IF §5.6） */
    public static final String PERM_MEMBER_UPDATE = "member:update";

    /** 空间编辑权限（空间维度判定点，IF §5.3；注意 ws:create / ws:delete 属组织级，不做空间维度判定） */
    public static final String PERM_WS_WRITE = "ws:write";

    /** 空间查看权限（空间维度判定点，IF §5.4 / §5.7） */
    public static final String PERM_WS_READ = "ws:read";
}
