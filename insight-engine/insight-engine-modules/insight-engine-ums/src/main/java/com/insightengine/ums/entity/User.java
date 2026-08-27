package com.insightengine.ums.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体，对应表 {@code ie_user}。
 *
 * <p>字段与 init.sql 一一对应（TD §5.1 蛇形转驼峰由 MP 自动映射）。</p>
 *
 * <p>设计要点：</p>
 * <ul>
 *   <li>{@code passwordHash} 存 BCrypt 密文，绝不存明文（TD §16.1），
 *       实体层也不提供明文密码字段，避免误写；</li>
 *   <li>{@code status}：1 正常 / 0 禁用（{@link com.insightengine.common.constant.Constants}）；</li>
 *   <li>{@code deleted} 逻辑删除字段，配合 starter-mybatis 全局逻辑删除配置；</li>
 *   <li>审计字段（createdAt/updatedAt/createdBy/updatedBy）由 MetaObjectHandler 自动填充。</li>
 * </ul>
 */
@Data
@TableName("ie_user")
public class User {

    /** 主键（BIGSERIAL 自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户 ID（MVP 单租户=1） */
    private Long tenantId;

    /** 邮箱（登录账号之一） */
    private String email;

    /** 手机号（登录账号之一） */
    private String phone;

    /** 密码 BCrypt 密文 */
    private String passwordHash;

    /** 昵称 */
    private String nickname;

    /** 头像 URL */
    private String avatar;

    /** 账号状态：1 正常 / 0 禁用 */
    private Integer status;

    /** 最近登录时间 */
    private LocalDateTime lastLoginAt;

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
