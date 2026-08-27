package com.insightengine.ums.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.insightengine.ums.entity.RolePermission;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色-权限关联 Mapper。
 *
 * <p>关系表无自增主键、无逻辑删除，故不使用 BaseMapper 的通用 CRUD 的按 ID 操作，
 * 仅提供按角色维度批量增删查，保证「角色授权」操作的事务原子性。</p>
 */
@Mapper
public interface RolePermissionMapper extends BaseMapper<RolePermission> {

    /**
     * 删除角色的全部权限关联（角色授权前先清空，再批量插入，保证幂等）。
     */
    @Delete("DELETE FROM ie_role_permission WHERE role_id = #{roleId}")
    int deleteByRoleId(@Param("roleId") Long roleId);

    /**
     * 查询角色已关联的权限 ID 列表。
     */
    @Select("SELECT permission_id FROM ie_role_permission WHERE role_id = #{roleId}")
    List<Long> selectPermissionIdsByRoleId(@Param("roleId") Long roleId);

    /**
     * 批量插入角色-权限关联。
     *
     * <p>用 {@code <script>} 动态拼接 VALUES，一次 INSERT 完成批量写入，
     * 避免逐条 INSERT 的性能损耗与多次网络往返。</p>
     */
    @Insert("""
            <script>
            INSERT INTO ie_role_permission (role_id, permission_id) VALUES
            <foreach collection="permissionIds" item="pid" separator=",">
                (#{roleId}, #{pid})
            </foreach>
            </script>
            """)
    int batchInsert(@Param("roleId") Long roleId, @Param("permissionIds") List<Long> permissionIds);
}
