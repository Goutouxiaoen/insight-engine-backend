package com.insightengine.common.constant;

/**
 * 认证域 SQL 的<b>权威口径</b>（唯一字面量定义）。
 *
 * <h3>为什么把 SQL 放到 common 当常量</h3>
 * <p>「token 里的 {@code roles} / {@code perms} 怎么算」是**一个语义**，但它在系统里有多个产生点：
 * UMS 登录、UMS 刷新、workspace 切换空间（见 IF §3 口径表）。这些点分散在不同服务、不同时期实现，
 * 各自手写 SQL → **必然漂移**（2026-09-17 BE-20260916-01 就是 workspace 自己写了「按空间过滤」的版本，
 * 导致切换空间丢组织级/平台级能力）。</p>
 *
 * <p>因此把这两条查询的**唯一字面量**放在 common，由各服务 Mapper 通过
 * {@code @Select(AuthQuerySql.SELECT_ROLE_CODES_BY_USER)} 引用：SQL 只写一份，
 * 改一次即全系统同步，**编译期就保证不会漂移**。</p>
 *
 * <h3>口径规则（改之前必读）</h3>
 * <ul>
 *   <li><b>必须按「用户」维度</b>，禁止按 {@code workspace_id} 过滤：
 *       {@code ie_member.workspace_id} 可空 = <b>组织级成员</b>（org_admin / super_admin 不挂具体空间），
 *       按空间过滤会整体丢掉这类记录；组织级能力（{@code org:*}、{@code ws:create}、{@code ws:delete}）本就不属于任何空间。</li>
 *   <li>与空间无关：**切换空间只改 {@code ws_id}（上下文），不重算权限**（身份 ≠ 上下文）。</li>
 *   <li>空间维度的"能不能做"由服务端按当前 {@code ws_id} 二次判定（TD §7.5 DataScope / PROGRESS §6.3）。</li>
 *   <li>用户不存在 / 无任何成员关系时返回空列表（由调用方决定兜底行为，不在此 SQL 里造数据）。</li>
 * </ul>
 */
public final class AuthQuerySql {

    private AuthQuerySql() {
        // 工具类禁止实例化
    }

    /**
     * 用户拥有的角色编码（用户维度、跨空间聚合、去重）。
     * <p>参数：{@code #{userId}}</p>
     */
    public static final String SELECT_ROLE_CODES_BY_USER = """
            SELECT DISTINCT r.code
            FROM ie_role r
            JOIN ie_member m ON m.role_id = r.id AND m.deleted = 0
            WHERE m.user_id = #{userId}
              AND r.deleted = 0
            """;

    /**
     * 用户拥有的权限编码（按角色展开、去重、排序稳定）。
     * <p>参数：{@code #{userId}}</p>
     */
    public static final String SELECT_PERMISSION_CODES_BY_USER = """
            SELECT DISTINCT p.code
            FROM ie_permission p
            JOIN ie_role_permission rp ON rp.permission_id = p.id
            JOIN ie_role r ON r.id = rp.role_id AND r.deleted = 0
            JOIN ie_member m ON m.role_id = r.id AND m.deleted = 0
            WHERE m.user_id = #{userId}
              AND p.deleted = 0
            ORDER BY p.code
            """;

    /* ==========================================================================================
     * 以下两条是「空间维度」查询：**只用于二次判定（@WorkspacePermission / my-permissions）**，
     * ⚠️ 严禁用于 token 签发（token 必须用上面两条「用户维度」的 SQL）。
     *
     * 两者区别（2026-09-17 固化，BE-20260916-01 的教训）：
     *   · token 承载「用户级能力」→ 用户维度、跨空间聚合（含 workspace_id 为空的组织级成员）；
     *   · 「在某个空间里能不能做某事」→ 空间维度，按 (userId, workspaceId) 查成员关系后判定。
     * 曾经把空间维度查询的结果写进 token，导致"一切空间就降级"（丢掉 org 域、ws:create、ws:delete 等组织级能力）。
     * ========================================================================================== */

    /**
     * 用户在某工作空间内的角色编码（空间维度，二次判定用）。
     * <p>参数：{@code #{userId}}、{@code #{workspaceId}}</p>
     */
    public static final String SELECT_ROLE_CODES_BY_USER_AND_WORKSPACE = """
            SELECT DISTINCT r.code
            FROM ie_role r
            JOIN ie_member m ON m.role_id = r.id AND m.deleted = 0
            WHERE m.user_id = #{userId}
              AND m.workspace_id = #{workspaceId}
              AND r.deleted = 0
            """;

    /**
     * 用户在某工作空间内的权限编码（空间维度，二次判定用）。
     * <p>参数：{@code #{userId}}、{@code #{workspaceId}}</p>
     */
    public static final String SELECT_PERMISSION_CODES_BY_USER_AND_WORKSPACE = """
            SELECT DISTINCT p.code
            FROM ie_permission p
            JOIN ie_role_permission rp ON rp.permission_id = p.id
            JOIN ie_role r ON r.id = rp.role_id AND r.deleted = 0
            JOIN ie_member m ON m.role_id = r.id AND m.deleted = 0
            WHERE m.user_id = #{userId}
              AND m.workspace_id = #{workspaceId}
              AND p.deleted = 0
            ORDER BY p.code
            """;
}
