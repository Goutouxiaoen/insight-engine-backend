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
}
