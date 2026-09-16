package com.insightengine.workspace.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 组织实体，对应表 {@code ie_organization}（DB.md §5.1.2）。
 *
 * <p>组织是租户内的一级容器，下辖多个工作空间（PRD §12.1.2）。MVP 单租户下仅一条组织记录。</p>
 */
@Data
@TableName("ie_organization")
public class Organization {

    /** 主键（BIGSERIAL 自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户 ID */
    private Long tenantId;

    /** 组织名称 */
    private String name;

    /** 组织唯一编码（同租户内唯一，见 uk_org_code_tenant） */
    private String code;

    /** 所有者 user_id */
    private Long ownerId;

    /** 套餐 ID（V1.0 起用） */
    private Long planId;

    /** 状态：1 正常 / 0 禁用 */
    private Integer status;

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

    /** 逻辑删除标记：0 正常 / 1 已删除 */
    private Integer deleted;
}
