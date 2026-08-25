package com.insightengine.starter.web.context;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 登录用户上下文值对象。
 *
 * <p>由网关解析 JWT 后，通过明文请求头下发（TD §4.5 / ADR-5：业务服务不重复解析 JWT，
 * 只信任内网可信的明文头），本对象承载一次请求内当前用户的身份快照。</p>
 *
 * <p>字段来源（对应 {@code Constants.HEADER_*} 请求头）：</p>
 * <ul>
 *   <li>{@code userId}：{@code X-User-Id}</li>
 *   <li>{@code tenantId}：{@code X-Tenant-Id}</li>
 *   <li>{@code workspaceId}：{@code X-Workspace-Id}</li>
 *   <li>{@code roles}：{@code X-Roles}（逗号分隔的角色编码）</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户 ID */
    private Long userId;

    /** 租户 ID */
    private Long tenantId;

    /** 当前工作空间 ID（可能为空，如组织级管理员操作） */
    private Long workspaceId;

    /** 角色编码列表 */
    private List<String> roles;

    /**
     * 获取角色列表的防御性返回：为 null 时降级为空列表，避免调用方 NPE。
     */
    public List<String> getRoles() {
        return roles == null ? Collections.emptyList() : roles;
    }

    /**
     * 判断当前用户是否持有指定角色编码。
     *
     * @param roleCode 角色编码，如 {@code super_admin}
     */
    public boolean hasRole(String roleCode) {
        return getRoles().contains(roleCode);
    }
}
