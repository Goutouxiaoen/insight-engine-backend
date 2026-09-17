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
 * <p>角色的写职责在 UMS（IF §6）；本 Mapper 提供 Workspace 侧需要的两类查询：</p>
 * <ul>
 *   <li>角色存在性校验（添加成员 / 改成员角色前，防孤儿 member，见 PROGRESS §6.1 教训）；</li>
 *   <li><b>按「用户」维度展开角色与权限编码</b> —— 切换空间换签 JWT 时写入 roles / perms Claim。</li>
 * </ul>
 *
 * <h3>⚠️ 这里为什么<b>不带</b> workspace 过滤（2026-09-17 修正，BE-20260916-01）</h3>
 * <p>JWT 里 `roles` / `perms` 的语义是「<b>这个人是谁、有哪些能力</b>」，与「当前站在哪个空间」无关：</p>
 * <ul>
 *   <li>`ie_member.workspace_id` <b>可空</b> = 组织级成员（如 org_admin / super_admin 不挂具体空间），
 *       若按空间过滤，这部分成员记录会被整体丢掉；</li>
 *   <li>组织级能力（`org:*`、`ws:create`、`ws:delete`）本就不属于任何空间，
 *       按空间过滤必然把它们筛掉 → 表现为「一切空间就降级」（删除按钮消失、`ws:delete` 丢失）。</li>
 * </ul>
 * <p>因此必须与 UMS 登录口径<b>完全一致</b>（UMS 的 `RoleMapper.selectRoleCodesByUserId` /
 * `PermissionMapper.selectPermissionCodesByUserId` 均不按空间过滤）。
 * 空间维度能做什么（「在 A 空间能建、在 B 空间不能建」）<b>不由 token 承载</b>，
 * 而是由服务端按「当前 ws_id + 成员关系」二次判定（TD §7.5 DataScope / PROGRESS §6.3 待办）。</p>
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    /**
     * 查询用户拥有的角色编码列表（用户维度，跨空间聚合；与 UMS 登录口径一致）。
     */
    @Select("""
            SELECT DISTINCT r.code
            FROM ie_role r
            JOIN ie_member m ON m.role_id = r.id AND m.deleted = 0
            WHERE m.user_id = #{userId}
              AND r.deleted = 0
            """)
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);

    /**
     * 查询用户拥有的权限编码列表（按角色展开、去重；与 UMS 登录口径一致）。
     */
    @Select("""
            SELECT DISTINCT p.code
            FROM ie_permission p
            JOIN ie_role_permission rp ON rp.permission_id = p.id
            JOIN ie_role r ON r.id = rp.role_id AND r.deleted = 0
            JOIN ie_member m ON m.role_id = r.id AND m.deleted = 0
            WHERE m.user_id = #{userId}
              AND p.deleted = 0
            ORDER BY p.code
            """)
    List<String> selectPermissionCodesByUserId(@Param("userId") Long userId);
}
