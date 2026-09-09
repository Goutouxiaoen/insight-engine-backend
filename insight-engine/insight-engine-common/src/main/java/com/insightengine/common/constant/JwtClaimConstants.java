package com.insightengine.common.constant;

/**
 * JWT Claim 名称常量（TD §7.2）。
 *
 * <p>为什么下沉到 common：JWT 是「签发方（UMS）与校验方（gateway）」共享的跨进程契约，
 * 此前两端各自声明一份字面量（{@code starter-security.JwtUtil} 与
 * {@code gateway.GatewayJwtParser}），仅靠注释约束「防 drift」，改名时极易漏改一端
 * 导致令牌解析静默失败（PROGRESS §6.4）。集中于此由两端共同引用，编译期即保证一致。</p>
 *
 * <p>约定：常量命名全大写 + 下划线；值与 TD §7.2 载荷设计一一对应，不得随意更改。</p>
 */
public final class JwtClaimConstants {

    private JwtClaimConstants() {
        // 工具类禁止实例化
    }

    /** 令牌类型 Claim 名（用于区分访问/刷新令牌，防令牌混淆攻击） */
    public static final String CLAIM_TYPE = "type";

    /** 令牌类型值：访问令牌 */
    public static final String TYPE_ACCESS = "access";

    /** 令牌类型值：刷新令牌 */
    public static final String TYPE_REFRESH = "refresh";

    /** 租户 ID Claim 名（TD §7.2） */
    public static final String CLAIM_TENANT_ID = "tenant_id";

    /** 工作空间 ID Claim 名（TD §7.2，组织级管理员可为空） */
    public static final String CLAIM_WS_ID = "ws_id";

    /** 角色编码列表 Claim 名（TD §7.2） */
    public static final String CLAIM_ROLES = "roles";

    /** 权限编码列表 Claim 名（登录时由角色展开，@PreAuthorize 无状态鉴权依据） */
    public static final String CLAIM_PERMISSIONS = "perms";
}
