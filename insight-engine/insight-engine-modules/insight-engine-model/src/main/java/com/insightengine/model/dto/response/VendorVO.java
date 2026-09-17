package com.insightengine.model.dto.response;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 模型厂商视图对象（IF §7.1）。
 *
 * <p><b>密钥安全</b>：只回 {@code maskedHint}（如 {@code sk-****1a2b}），**永不回传明文 API Key**
 * （IF §7.1 安全约束）。{@code hasApiKey} 用于前端区分"已配置密钥"与"无需密钥的本地模型"
 * （如 Ollama），避免用 {@code maskedHint} 是否为空来猜。</p>
 */
@Data
public class VendorVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 厂商 ID */
    private Long id;

    /** 厂商编码（创建后不可改） */
    private String code;

    /** 厂商名称 */
    private String name;

    /** 接口基址 */
    private String baseUrl;

    /** 能力类型：CHAT / EMBEDDING / RERANK */
    private String type;

    /** 是否启用：1 启用 / 0 停用 */
    private Integer enabled;

    /** 是否已配置 API Key（false = 无鉴权的本地模型） */
    private Boolean hasApiKey;

    /** API Key 掩码提示（仅尾四位，可安全回显） */
    private String maskedHint;

    /** 扩展配置（JSON 字符串，供编辑表单回显） */
    private String config;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
