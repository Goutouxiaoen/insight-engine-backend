package com.insightengine.workspace.support;

import com.insightengine.common.constant.CacheKeyConstants;
import com.insightengine.starter.security.workspace.WorkspacePermissionChecker;
import com.insightengine.workspace.constant.WorkspaceConstants;
import com.insightengine.workspace.mapper.RoleMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * 空间维度权限判定器（workspace 侧实现，TD §7.5 第二层鉴权）。
 *
 * <h3>判定链路</h3>
 * <pre>
 * (userId, workspaceId) → ie_member（该空间成员关系）→ ie_role → ie_role_permission → ie_permission
 * 不是该空间成员 → 无任何权限（返回空集合）
 * </pre>
 *
 * <h3>缓存</h3>
 * <p>键 {@code ie:ws:user-perm:{workspaceId}:{userId}}（{@link CacheKeyConstants#WS_USER_PERM}），
 * 值 = 权限码逗号拼接，TTL 10min。避免每次请求打三张表。</p>
 * <ul>
 *   <li><b>精确失效</b>：成员增删 / 改成员角色 / 删除空间时由 {@link #evict}/ {@link #evictWorkspace} 主动删除；</li>
 *   <li><b>TTL 兜底</b>：角色授权变更发生在 UMS（跨服务），故保留 10min TTL（最长 10 分钟后生效），
 *       与 TD §6.1 其他权限缓存的取舍一致。</li>
 * </ul>
 *
 * <h3>与 token 的区别（务必分清）</h3>
 * <p>本类返回的是"这个人**在这个空间**里的权限"；token 里的 {@code perms} 是"这个人**跨所有空间**的并集"。
 * 前者用于第二层判定与前端按钮门控，后者用于 {@code @PreAuthorize} 动作门控——**两者不可互相替代**。</p>
 */
@Component
public class WorkspacePermissionCheckerImpl implements WorkspacePermissionChecker {

    private final RoleMapper roleMapper;
    private final StringRedisTemplate stringRedisTemplate;

    public WorkspacePermissionCheckerImpl(RoleMapper roleMapper, StringRedisTemplate stringRedisTemplate) {
        this.roleMapper = roleMapper;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean has(Long userId, Long workspaceId, String permissionCode) {
        if (userId == null || workspaceId == null || permissionCode == null) {
            return false;
        }
        return permissionsOf(userId, workspaceId).contains(permissionCode);
    }

    @Override
    public List<String> permissionsOf(Long userId, Long workspaceId) {
        if (userId == null || workspaceId == null) {
            return List.of();
        }
        String key = cacheKey(workspaceId, userId);
        String cached = stringRedisTemplate.opsForValue().get(key);
        if (cached != null) {
            return cached.isEmpty() ? List.of() : Arrays.asList(cached.split(","));
        }
        List<String> permissions = roleMapper.selectPermissionCodesByUserAndWorkspace(userId, workspaceId);
        // 空集合也写入缓存（值为空串）——否则"反复查一个非成员"会持续打库
        stringRedisTemplate.opsForValue().set(key, String.join(",", permissions),
                Duration.ofSeconds(WorkspaceConstants.WS_PERM_CACHE_TTL_SECONDS));
        return permissions;
    }

    /**
     * 失效某成员在某空间的权限缓存（成员增删 / 改角色后调用）。
     */
    public void evict(Long userId, Long workspaceId) {
        if (userId != null && workspaceId != null) {
            stringRedisTemplate.delete(cacheKey(workspaceId, userId));
        }
    }

    /**
     * 失效某空间的全部成员权限缓存（删除空间 / 批量变更后调用）。
     *
     * <p>实现说明：单机 MVP 用 {@code keys} 模式删除；数据量大时应改为
     * SCAN + 批量删除，或直接把"空间成员权限"整体挂在一个 Hash 里按空间整体删（TD §6.1 维护注记）。</p>
     */
    public void evictWorkspace(Long workspaceId) {
        if (workspaceId == null) {
            return;
        }
        var keys = stringRedisTemplate.keys(CacheKeyConstants.WS_USER_PERM + workspaceId + ":*");
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }

    private String cacheKey(Long workspaceId, Long userId) {
        return CacheKeyConstants.WS_USER_PERM + workspaceId + ":" + userId;
    }
}
