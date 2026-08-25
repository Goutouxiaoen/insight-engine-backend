package com.insightengine.common.constant;

/**
 * 全局常量。
 *
 * <p>集中管理跨服务共享的请求头、状态值等常量，避免魔法字符串散落各处。</p>
 *
 * <p>约定：常量命名全大写 + 下划线分隔；注释说明其用途与来源（TD 章节）。</p>
 */
public final class Constants {

    private Constants() {
        // 工具类禁止实例化
    }

    /* ============ 请求头（TD §4.4 / §4.5 / IF §2.1） ============ */

    /** 链路追踪 ID 请求头，网关生成并向下游透传 */
    public static final String HEADER_TRACE_ID = "X-Trace-Id";

    /** 幂等键请求头，创建类接口用于去重（IF §2.1） */
    public static final String HEADER_REQUEST_ID = "X-Request-Id";

    /** 网关解析 JWT 后下发的用户 ID 明文头（TD §4.5 ADR-5） */
    public static final String HEADER_USER_ID = "X-User-Id";

    /** 网关下发的租户 ID 明文头 */
    public static final String HEADER_TENANT_ID = "X-Tenant-Id";

    /** 网关下发的当前工作空间 ID 明文头 */
    public static final String HEADER_WORKSPACE_ID = "X-Workspace-Id";

    /** 网关下发的角色编码列表明文头（逗号分隔） */
    public static final String HEADER_ROLES = "X-Roles";

    /* ============ 通用状态值（TD §5.1 逻辑删除 / 通用启用禁用） ============ */

    /** 逻辑删除：正常 */
    public static final int DELETED_NO = 0;

    /** 逻辑删除：已删除 */
    public static final int DELETED_YES = 1;

    /** 通用启用状态：禁用 */
    public static final int STATUS_DISABLED = 0;

    /** 通用启用状态：启用 */
    public static final int STATUS_ENABLED = 1;
}
