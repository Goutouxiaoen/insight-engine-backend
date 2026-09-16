package com.insightengine.workspace.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工作空间实体，对应表 {@code ie_workspace}（DB.md §5.1.3）。
 *
 * <p>工作空间是租户内的二级隔离单元（对应部门/项目），也是知识库 / Agent / 工具等资源的
 * 归属边界（PRD §12.1.2）。</p>
 */
@Data
@TableName("ie_workspace")
public class Workspace {

    /** 主键（BIGSERIAL 自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户 ID */
    private Long tenantId;

    /** 所属组织 ID */
    private Long orgId;

    /** 工作空间名称 */
    private String name;

    /** 工作空间编码（同组织内唯一，见 uk_ws_code_org；创建后不可修改） */
    private String code;

    /** 工作空间级套餐 ID */
    private Long planId;

    /** 最大应用数上限（配额约束） */
    private Integer maxApps;

    /** 知识库容量上限（MB） */
    private Integer maxKbSizeMb;

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
