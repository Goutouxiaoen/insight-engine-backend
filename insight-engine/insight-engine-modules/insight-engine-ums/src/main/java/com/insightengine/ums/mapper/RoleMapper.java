package com.insightengine.ums.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.insightengine.ums.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色 Mapper。
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    /**
     * 查询用户在当前工作空间（或组织级）下拥有的角色编码列表。
     *
     * <p>通过 member → role 两表关联（PRD §12.1.3）：
     * 成员关系可能挂 workspace（空间级角色），也可能是组织级（workspace_id 为空）。
     * 登录时用户可能尚未确定工作空间，故不按 workspace 过滤，返回其所有角色编码；
     * 具体数据范围在 ABAC 拦截器阶段再按 scope 收敛。</p>
     */
    @Select("""
            SELECT r.code
            FROM ie_role r
            JOIN ie_member m ON m.role_id = r.id AND m.deleted = 0
            WHERE m.user_id = #{userId}
              AND r.deleted = 0
            """)
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);

    /**
     * 查询用户默认工作空间 ID（取第一条成员关系的工作空间）。
     *
     * <p>登录后需要为用户初始化一个默认工作空间上下文（IF §3.1 返回 workspaceId）。
     * MVP 单租户下用户通常只属于一个空间，取最早加入的那条即可；无成员关系返回 null。</p>
     */
    @Select("""
            SELECT m.workspace_id
            FROM ie_member m
            WHERE m.user_id = #{userId}
              AND m.deleted = 0
              AND m.workspace_id IS NOT NULL
            ORDER BY m.id ASC
            LIMIT 1
            """)
    Long selectDefaultWorkspaceIdByUserId(@Param("userId") Long userId);
}
