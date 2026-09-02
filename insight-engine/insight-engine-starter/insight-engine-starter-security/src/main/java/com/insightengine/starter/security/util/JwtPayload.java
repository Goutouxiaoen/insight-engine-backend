package com.insightengine.starter.security.util;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * JWT 解析后的载荷值对象。
 *
 * <p>把解析出的零散 Claim 收敛为强类型字段，避免调用方反复做 {@code claims.get(...)}
 * 与类型强转；同时字段与 {@link com.insightengine.starter.web.context.LoginUser} 一一对应，
 * 便于认证过滤器直接填充用户上下文。</p>
 *
 * <p>字段含义见 TD §7.2：{@code userId} 对应 {@code sub}，{@code roles} 为角色编码列表，
 * {@code permissions} 为登录时由角色展开的权限编码列表（供 {@code @PreAuthorize} 校验）。</p>
 */
@Data
@AllArgsConstructor
public class JwtPayload implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户 ID（JWT sub） */
    private Long userId;

    /** 租户 ID */
    private Long tenantId;

    /** 当前工作空间 ID（组织级管理员可能为空） */
    private Long workspaceId;

    /** 角色编码列表 */
    private List<String> roles;

    /** 权限编码列表（登录时展开，用于方法级权限校验） */
    private List<String> permissions;
}
