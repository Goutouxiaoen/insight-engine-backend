package com.insightengine.workspace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.insightengine.workspace.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色 Mapper（只读引用）。
 *
 * <p>角色的写职责在 UMS（IF §6）；本 Mapper 只提供 Workspace 侧需要的两类查询：</p>
 * <ul>
 *   <li>角色存在性校验（添加成员 / 改成员角色前，防孤儿 member，见 PROGRESS §6.1 教训）；</li>
 *   <li>按「用户 + 工作空间」展开角色与权限编码——切换空间换签 JWT 时写入 roles / perms Claim，
 *       必须按目标空间维度取，不能用全量角色（否则切到 A 空间却带着 B 空间的权限）。</li>
 * </ul>
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    /**
     * 查询用户在指定工作空间内的角色编码列表。
     */
    @Select("""
            SELECT DISTINCT r.code
            FROM ie_role r
            JOIN ie_member m ON m.role_id = r.id AND m.deleted = 0
            WHERE m.user_id = #{userId}
              AND m.workspace_id = #{workspaceId}
              AND r.deleted = 0
            """)
    List<String> selectRoleCodesByUserAndWorkspace(@Param("userId") Long userId,
                                                   @Param("workspaceId") Long workspaceId);

    /**
     * 查询用户在指定工作空间内的权限编码列表（按角色展开，去重）。
     */
    @Select("""
            SELECT DISTINCT p.code
            FROM ie_permission p
            JOIN ie_role_permission rp ON rp.permission_id = p.id
            JOIN ie_role r ON r.id = rp.role_id AND r.deleted = 0
            JOIN ie_member m ON m.role_id = r.id AND m.deleted = 0
            WHERE m.user_id = #{userId}
              AND m.workspace_id = #{workspaceId}
              AND p.deleted = 0
            ORDER BY p.code
            """)
    List<String> selectPermissionCodesByUserAndWorkspace(@Param("userId") Long userId,
                                                         @Param("workspaceId") Long workspaceId);
}
