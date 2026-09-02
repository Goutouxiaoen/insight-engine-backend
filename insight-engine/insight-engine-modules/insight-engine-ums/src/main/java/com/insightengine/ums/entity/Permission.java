package com.insightengine.ums.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 权限实体，对应表 {@code ie_permission}。
 *
 * <p>RBAC 最小授权单元，编码「资源:动作」（如 kb:read），全局唯一（PRD §12.2.4）。
 * 注意：本表无 created_by/updated_by 审计列（见 init.sql），故实体也不声明这两个字段。</p>
 */
@Data
@TableName("ie_permission")
public class Permission {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 权限编码（如 kb:read、model:vendor:write） */
    private String code;

    /** 权限名称（前端展示） */
    private String name;

    /** 资源类型（kb / member / model:vendor 等） */
    private String resource;

    /** 动作（read/write/create/update/delete） */
    private String action;

    /** 数据范围：ALL/ORG/WS/SELF */
    private String scope;

    /** 权限描述 */
    private String description;

    /** 创建时间（UTC） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间（UTC） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 逻辑删除标记 */
    private Integer deleted;
}
