package com.insightengine.workspace.support;

import com.insightengine.common.constant.CacheKeyConstants;
import com.insightengine.common.core.BizException;
import com.insightengine.common.core.ErrorCode;
import com.insightengine.starter.security.workspace.WorkspacePermissionCacheInvalidator;
import com.insightengine.starter.security.workspace.WorkspacePermissionChecker;
import com.insightengine.workspace.constant.WorkspaceConstants;
import com.insightengine.workspace.mapper.RoleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * 不是该空间成员 → 角色与权限均为空集合（等价"什么都不能做"）
 * </pre>
 *
 * <h3>缓存（2026-09-17 调整）</h3>
 * <p>键 {@code ie:ws:user-perm:{workspaceId}:{userId}}（{@link CacheKeyConstants#WS_USER_PERM}），
 * 值 = {@code 角色码,角色码|权限码,权限码}（**角色与权限同一份快照**），TTL 10min。</p>
 * <ul>
 *   <li><b>为什么把角色也放进同一份快照</b>：此前 {@code rolesOf} 实时查库、{@code permissionsOf} 读缓存，
 *       同一响应里两者可能来自不同时刻（刚改完角色/刚被移除时会自相矛盾）——code review 收口为"一次读库、一个快照"；</li>
 *   <li><b>精确失效</b>：成员增删 / 改成员角色 / 删除空间时由 {@link #evict}/{@link #evictWorkspace} 主动删除；</li>
 *   <li><b>TTL 兜底</b>：角色授权变更发生在 UMS（跨服务），故保留 10min TTL（最长 10 分钟后生效）。</li>
 * </ul>
 *
 * <h3>失效的失败语义（重要，2026-09-17 code review 收口）</h3>
 * <p>失效失败**必须让操作失败**（抛 9999），而不是"记个日志继续改库"：调用方把失效放在**数据库变更之前**，
 * 因此失败 = 操作整体未生效，权限状态始终与缓存一致；反之（先改库、失效失败）会留下"库里已撤销、缓存仍放行"
 * 的窗口，最长可达 TTL——对"踢人/降权"这类操作是不可接受的安全语义破坏。</p>
 *
 * <h3>与 token 的区别（务必分清）</h3>
 * <p>本类返回的是"这个人**在这个空间**里的角色/权限"；token 里的 {@code roles}/{@code perms} 是
 * "这个人**跨所有空间**的并集"。前者用于第二层判定与前端按钮门控，后者用于 {@code @PreAuthorize}
 * 动作门控——**两者不可互相替代**。</p>
 */
@Component
public class WorkspacePermissionCheckerImpl implements WorkspacePermissionChecker, WorkspacePermissionCacheInvalidator {

    private static final Logger log = LoggerFactory.getLogger(WorkspacePermissionCheckerImpl.class);

    /** 快照分隔符：{@code 角色集合 | 权限集合}（角色码/权限码本身不含 {@code |}） */
    private static final String SNAPSHOT_SEPARATOR = "|";

    /** 分隔符拆分的正则（{@code |} 在正则中是元字符） */
    private static final String SNAPSHOT_SEPARATOR_REGEX = "\\|";

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
        return snapshot(userId, workspaceId).permissions();
    }

    @Override
    public List<String> rolesOf(Long userId, Long workspaceId) {
        if (userId == null || workspaceId == null) {
            return List.of();
        }
        return snapshot(userId, workspaceId).roles();
    }

    /**
     * 失效某成员在某空间的权限缓存（成员增删 / 改角色时调用，须在数据库变更**之前**调用）。
     */
    @Override
    public void evict(Long userId, Long workspaceId) {
        if (userId == null || workspaceId == null) {
            return;
        }
        try {
            stringRedisTemplate.delete(cacheKey(workspaceId, userId));
        } catch (RuntimeException e) {
            log.error("空间权限缓存失效失败（操作将中止，以避免撤销后仍被放行）: userId={} workspaceId={}",
                    userId, workspaceId, e);
            throw new BizException(ErrorCode.SYSTEM_ERROR, "权限缓存失效失败，操作已取消，请稍后重试");
        }
    }

    /**
     * 失效某空间的全部成员权限缓存（删除空间 / 批量变更时调用）。
     *
     * <p>实现说明：单机 MVP 用 {@code keys} 模式删除；数据量大时应改为 SCAN + 批量删除，
     * 或把"空间成员权限"整体挂在一个 Hash 里按空间整体删（TD §6.1 维护注记）。</p>
     */
    @Override
    public void evictWorkspace(Long workspaceId) {
        if (workspaceId == null) {
            return;
        }
        try {
            var keys = stringRedisTemplate.keys(CacheKeyConstants.WS_USER_PERM + workspaceId + ":*");
            if (keys != null && !keys.isEmpty()) {
                stringRedisTemplate.delete(keys);
            }
        } catch (RuntimeException e) {
            log.error("空间权限缓存整空间失效失败（操作将中止）: workspaceId={}", workspaceId, e);
            throw new BizException(ErrorCode.SYSTEM_ERROR, "权限缓存失效失败，操作已取消，请稍后重试");
        }
    }

    /**
     * 读取（或回填）角色+权限快照。
     */
    private Snapshot snapshot(Long userId, Long workspaceId) {
        String key = cacheKey(workspaceId, userId);
        String cached = stringRedisTemplate.opsForValue().get(key);
        if (cached != null && cached.contains(SNAPSHOT_SEPARATOR)) {
            String[] parts = cached.split(SNAPSHOT_SEPARATOR_REGEX, -1);
            return new Snapshot(split(parts[0]), split(parts[1]));
        }
        // 无缓存 / 旧格式（仅权限串，无分隔符）：回源查询并覆盖写回
        List<String> roles = roleMapper.selectRoleCodesByUserAndWorkspace(userId, workspaceId);
        List<String> permissions = roleMapper.selectPermissionCodesByUserAndWorkspace(userId, workspaceId);
        // 空集合也写入缓存（快照为 "|"）——否则"反复查一个非成员"会持续打库
        stringRedisTemplate.opsForValue().set(key,
                String.join(",", roles) + SNAPSHOT_SEPARATOR + String.join(",", permissions),
                Duration.ofSeconds(WorkspaceConstants.WS_PERM_CACHE_TTL_SECONDS));
        return new Snapshot(roles, permissions);
    }

    private List<String> split(String csv) {
        return csv.isEmpty() ? List.of() : Arrays.asList(csv.split(","));
    }

    private String cacheKey(Long workspaceId, Long userId) {
        return CacheKeyConstants.WS_USER_PERM + workspaceId + ":" + userId;
    }

    /**
     * 空间维度权限快照（角色 + 权限，同一时刻读库得到）。
     */
    private record Snapshot(List<String> roles, List<String> permissions) {
    }
}
