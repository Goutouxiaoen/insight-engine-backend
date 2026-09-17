package com.insightengine.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 模型厂商实体，对应表 {@code ie_model_vendor}（DB.md §5.3.1）。
 *
 * <p>厂商 = 一个"能调用的模型服务端点"（通义 DashScope / 智谱 / OpenAI 兼容服务 / 本地 Ollama），
 * 由 {@code baseUrl} + {@code apiKeySecretId} 描述。**平台级目录**：本表无租户/空间归属列
 * （2026-09-17 裁决路线 A：平台/组织管理员接入，所有业务方共用；BYOK 不在本期，PROGRESS §三）。</p>
 */
@Data
@TableName("ie_model_vendor")
public class ModelVendor {

    /** 主键（BIGSERIAL 自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 厂商编码（唯一，如 qwen / zhipu / ollama；创建后不可修改） */
    private String code;

    /** 厂商名称（展示用，如「通义千问」） */
    private String name;

    /** 接口基址（OpenAI 兼容模式，如 https://dashscope.aliyuncs.com/compatible-mode/v1） */
    private String baseUrl;

    /** 关联密钥 ID（ie_secret.id）；无鉴权的本地模型（如 Ollama）可为空 */
    private Long apiKeySecretId;

    /** 能力类型：CHAT / EMBEDDING / RERANK */
    private String type;

    /** 是否启用：1 启用 / 0 停用 */
    private Integer enabled;

    /** 扩展配置（JSONB 字符串，如超时、额外请求头） */
    private String config;

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
