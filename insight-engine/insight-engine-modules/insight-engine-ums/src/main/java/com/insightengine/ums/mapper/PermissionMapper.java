package com.insightengine.ums.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.insightengine.ums.entity.Permission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 权限 Mapper。
 */
@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {

    /**
     * 查询用户拥有的权限编码列表（按角色展开）。
     *
     * <p>链路：user → member → role → role_permission → permission（PRD §12.2.3）。
     * 登录时调用，结果写入 JWT 的 perms Claim，供 {@code @PreAuthorize} 方法级权限校验。</p>
     *
     * <p>使用 DISTINCT 去重——用户可能拥有多个角色，角色间权限可能重叠。</p>
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
