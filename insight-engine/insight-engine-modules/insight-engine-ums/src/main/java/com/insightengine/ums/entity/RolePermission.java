package com.insightengine.ums.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 角色-权限关联实体，对应表 {@code ie_role_permission}。
 *
 * <p>多对多关系表，联合主键 (role_id, permission_id)（PRD §12.2 / init.sql）。</p>
 *
 * <p>设计要点：本表无审计字段、无逻辑删除列（关系随角色删除一并清理），
 * 故实体极简，仅两个关联字段；写入/删除由 RoleService 在事务内批量处理。</p>
 */
@Data
@TableName("ie_role_permission")
public class RolePermission {

    /** 角色 ID */
    private Long roleId;

    /** 权限 ID */
    private Long permissionId;
}
