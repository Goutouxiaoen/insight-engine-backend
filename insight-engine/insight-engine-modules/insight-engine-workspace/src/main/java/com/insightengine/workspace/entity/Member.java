package com.insightengine.workspace.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 成员关系实体，对应表 {@code ie_member}（DB.md §5.1.4）。
 *
 * <p>用户与角色在组织 / 工作空间维度的关联：一个用户可在不同空间拥有不同角色。
 * 本表既是「空间成员管理」的数据源，也是「切换空间可切换范围」的服务端约束依据
 * （PROGRESS §三 2026-09-09 裁决：不新增 {@code ws:switch} 权限码，范围由成员关系强约束）。</p>
 *
 * <p>{@code workspaceId} 可空——组织级管理员（如 org_admin）不挂具体空间。</p>
 */
@Data
@TableName("ie_member")
public class Member {

    /** 主键（BIGSERIAL 自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户 ID */
    private Long tenantId;

    /** 所属组织 ID */
    private Long orgId;

    /** 所属工作空间 ID；组织级管理员可为空 */
    private Long workspaceId;

    /** 用户 ID */
    private Long userId;

    /** 角色 ID */
    private Long roleId;

    /** 加入时间 */
    private LocalDateTime joinedAt;

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
