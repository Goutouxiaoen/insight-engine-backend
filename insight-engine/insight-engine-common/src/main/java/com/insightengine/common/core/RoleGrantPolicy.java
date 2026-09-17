package com.insightengine.common.core;

/**
 * 角色授予策略（"谁能在哪里授予什么角色"），跨服务单点定义。
 *
 * <h3>为什么必须有这条规则</h3>
 * <p>「能授予角色」等于「能分配权限」——如果只看 {@code roleId} 是否存在，那么任何持有
 * {@code member:create}/{@code member:update}（空间管理员）或 {@code user:create}（组织管理员）的人，
 * 都能把 {@code super_admin} 这个内置平台角色授予自己（或自己的小号），**变相完成提权**。
 * 这是 RBAC 最经典、也最容易被漏掉的一条越权面（"授权接口本身就是提权接口"）。</p>
 *
 * <h3>规则（按授予位置分档，依据 {@code ie_role.scope}，init.sql 种子）</h3>
 * <pre>
 * scope  ALL  = 平台级（super_admin）  → 仅平台超管可授予；空间/组织接口一律拒绝
 * scope  ORG  = 组织级（org_admin）    → 组织级接口可授予；空间接口拒绝（超出空间权能边界）
 * scope  WS   = 空间级（ws_admin/app_developer）→ 空间/组织接口均可授予
 * scope  SELF = 自身级（end_user）     → 空间/组织接口均可授予
 * </pre>
 *
 * <p>「空间接口只能授予 WS/SELF」的理由：空间管理员的权能边界就是**这一个空间**，
 * 允许其授予组织级/平台级角色，等于让空间管理员越过自己的边界去动组织（水平越权升格为垂直越权）。</p>
 *
 * <p>本类只做纯判定（不查库、不依赖 Spring），DB 读取留在各服务（UMS / workspace）——
 * 规则只有一份，避免"两处各写一套阈值"再次漂移。</p>
 */
public final class RoleGrantPolicy {

    private RoleGrantPolicy() {
        // 工具类禁止实例化
    }

    /** 角色数据范围：平台级（{@code ie_role.scope}） */
    public static final String SCOPE_ALL = "ALL";

    /** 角色数据范围：组织级 */
    public static final String SCOPE_ORG = "ORG";

    /** 角色数据范围：空间级 */
    public static final String SCOPE_WS = "WS";

    /** 角色数据范围：自身级 */
    public static final String SCOPE_SELF = "SELF";

    /** 平台内置角色编码：超级管理员（init.sql 种子 id=1，scope=ALL） */
    public static final String ROLE_SUPER_ADMIN = "super_admin";

    /** 平台内置角色租户 ID：0 表示所有租户可见的平台内置角色 */
    public static final long PLATFORM_TENANT_ID = 0L;

    /**
     * 空间内成员管理接口（{@code /api/v1/member/**}）是否允许授予该角色的作用域。
     *
     * <p>只允许 WS / SELF：空间管理员的权能边界就是这一个空间。</p>
     */
    public static boolean grantableWithinWorkspace(String scope) {
        return SCOPE_WS.equals(scope) || SCOPE_SELF.equals(scope);
    }

    /**
     * 组织级接口（{@code /api/v1/user/**}）是否允许授予该角色的作用域（在授予者是超管的前提下另需校验 ALL）。
     *
     * <p>组织级管理员可授予组织级及以下（ORG/WS/SELF）；ALL（平台级）需另由超管校验。</p>
     */
    public static boolean grantableWithinOrg(String scope) {
        return !SCOPE_ALL.equals(scope);
    }

    /**
     * 角色是否属于该租户（平台内置角色 tenant_id=0 对所有租户可见）。
     *
     * <p>防止把**其它租户的自定义角色**挂给本租户成员（跨租户引用既脏数据也可能是越权入口）。</p>
     */
    public static boolean belongsToTenant(Long roleTenantId, Long tenantId) {
        if (roleTenantId == null) {
            return true;
        }
        return roleTenantId == PLATFORM_TENANT_ID || roleTenantId.equals(tenantId);
    }

    /**
     * 是否为平台级（ALL）角色。
     */
    public static boolean isPlatformScope(String scope) {
        return SCOPE_ALL.equals(scope);
    }
}
