package com.insightengine.ums.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 成员关系实体，对应表 {@code ie_member}。
 *
 * <p>用户与角色在组织/工作空间维度的关联（PRD §12.1.3）：
 * 一个用户可在不同空间拥有不同角色（RBAC 数据隔离的关键）。</p>
 *
 * <p>设计要点：{@code workspaceId} 可空——组织级管理员（如 org_admin）不挂具体空间，
 * 故登录时需容忍该字段为空（JWT 中 ws_id 缺失）。</p>
 */
@Data
@TableName("ie_member")
public class Member {

    /** 主键 */
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
