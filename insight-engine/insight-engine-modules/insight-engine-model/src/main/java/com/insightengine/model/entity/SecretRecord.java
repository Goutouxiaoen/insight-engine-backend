package com.insightengine.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 密钥实体，对应表 {@code ie_secret}（DB.md §5.11.4，2026-09-17 阶段 6 准入时建表）。
 *
 * <p>只存**密文**（AES-256-GCM）+ IV + 算法 + KEK 版本，明文永不入库（TD §16.1）。
 * 由 {@code ie_model_vendor.api_key_secret_id} 引用（该外键列首版即预留）。</p>
 */
@Data
@TableName("ie_secret")
public class SecretRecord {

    /** 主键（BIGSERIAL 自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户 ID；0 表示平台级（全租户共用，与 ie_role.tenant_id=0 同口径） */
    private Long tenantId;

    /** 展示名（如「通义千问-默认Key」） */
    private String name;

    /** 密钥类型（ModelConstants.SECRET_TYPE_*） */
    private String secretType;

    /** 密文（AES-256-GCM，Base64） */
    private String cipherText;

    /** GCM IV/Nonce（Base64，每次加密独立生成） */
    private String iv;

    /** 加密算法（AES-256-GCM） */
    private String algo;

    /** 主密钥版本（轮换用） */
    private String kekVersion;

    /** 掩码提示（仅尾四位，供界面识别） */
    private String maskedHint;

    /** 是否启用：1 启用 / 0 停用 */
    private Integer enabled;

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
