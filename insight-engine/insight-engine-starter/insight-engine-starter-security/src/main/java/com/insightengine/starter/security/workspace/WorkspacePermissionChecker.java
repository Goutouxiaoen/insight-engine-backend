package com.insightengine.starter.security.workspace;

import java.util.List;

/**
 * 空间维度权限判定器（第二层鉴权的 SPI，由业务服务提供实现）。
 *
 * <h3>为什么做成接口 + 由服务实现</h3>
 * <p>{@code starter-security} 是**技术能力**（不依赖业务表、不依赖 Redis）；而"判断某人在某空间有没有某权限"
 * 需要查 {@code ie_member → ie_role → ie_role_permission → ie_permission}（同库只读）。
 * 因此这里只定义契约，实现由用到它的服务提供（当前 workspace；将来 kb/agent 等若跨库则改走 Feign 契约）。</p>
 *
 * <p>未提供实现时，{@code @WorkspacePermission} 不会生效（{@code WorkspacePermissionAspect}
 * 通过 {@code @ConditionalOnBean} 装配）——这是刻意的：**没有判定器就不做空间维度校验**，
 * 而不是静默放行（调用方引入注解即视为声明了需求，服务未实现时应尽快暴露）。</p>
 *
 * @see com.insightengine.common.annotation.WorkspacePermission
 */
public interface WorkspacePermissionChecker {

    /**
     * 该用户在该空间内是否拥有指定权限。
     *
     * @param userId         用户 ID
     * @param workspaceId    工作空间 ID
     * @param permissionCode 权限编码（如 {@code member:create}）
     * @return 拥有返回 true；不是该空间成员 / 无该权限返回 false
     */
    boolean has(Long userId, Long workspaceId, String permissionCode);

    /**
     * 该用户在该空间内的全部权限编码（供"当前空间权限"接口给前端做按钮门控）。
     *
     * @return 权限编码列表（无成员关系时返回空列表，不返回 null）
     */
    List<String> permissionsOf(Long userId, Long workspaceId);

    /**
     * 该用户在该空间内的角色编码（与 {@link #permissionsOf} **取自同一份快照**，
     * 避免"角色实时查库、权限读缓存"造成同一响应内两者不一致，2026-09-17 code review 收口）。
     *
     * @return 角色编码列表（无成员关系时返回空列表，不返回 null）
     */
    List<String> rolesOf(Long userId, Long workspaceId);
}
