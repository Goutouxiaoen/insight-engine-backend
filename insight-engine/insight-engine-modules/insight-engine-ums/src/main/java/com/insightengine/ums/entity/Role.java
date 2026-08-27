package com.insightengine.ums.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色实体，对应表 {@code ie_role}。
 *
 * <p>RBAC 核心实体（PRD §12.2）。{@code tenantId=0} 表示平台内置角色（所有租户可见）。</p>
 *
 * <p>设计要点：</p>
 * <ul>
 *   <li>{@code scope}：数据范围 ALL/ORG/WS/SELF（ABAC 数据行级过滤依据）；</li>
 *   <li>{@code builtin}：1 内置（禁止删除）/ 0 自定义，删除时据此拦截（IF §6.6 返回 1003）。</li>
 * </ul>
 */
@Data
@TableName("ie_role")
public class Role {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户 ID；0 表示平台内置角色 */
    private Long tenantId;

    /** 角色编码（如 super_admin / ws_admin） */
    private String code;

    /** 角色名称 */
    private String name;

    /** 数据范围：ALL/ORG/WS/SELF */
    private String scope;

    /** 是否内置：1 内置（禁删）/ 0 自定义 */
    private Integer builtin;

    /** 角色描述 */
    private String description;

    /** 创建时间（UTC） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间（UTC） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 创建人 */
    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    /** 最后修改人 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;

    /** 逻辑删除标记 */
    private Integer deleted;
}
