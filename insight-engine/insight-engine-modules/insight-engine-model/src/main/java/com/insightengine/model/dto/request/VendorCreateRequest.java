package com.insightengine.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建模型厂商请求体（IF §7.1）。
 *
 * <p><b>安全要点</b>：{@code apiKey} 是**明文入参**（HTTPS 传输），服务端加密后落
 * {@code ie_secret}；任何读取接口都不会回传明文（只回 {@code maskedHint}）。
 * 无鉴权的本地模型（如 Ollama）可不填 {@code apiKey}。</p>
 */
@Data
public class VendorCreateRequest {

    /** 厂商编码（唯一，创建后不可改），如 qwen / zhipu / ollama */
    @NotBlank(message = "厂商编码不能为空")
    @Size(max = 64, message = "厂商编码长度不能超过 64 个字符")
    @Pattern(regexp = "^[a-z][a-z0-9_-]*$", message = "厂商编码须以小写字母开头，仅含小写字母/数字/下划线/中划线")
    private String code;

    /** 厂商名称（展示用） */
    @NotBlank(message = "厂商名称不能为空")
    @Size(max = 128, message = "厂商名称长度不能超过 128 个字符")
    private String name;

    /** 接口基址（OpenAI 兼容模式） */
    @NotBlank(message = "接口基址不能为空")
    @Size(max = 255, message = "接口基址长度不能超过 255 个字符")
    private String baseUrl;

    /** API Key（明文入参；本地无鉴权模型可留空） */
    @Size(max = 512, message = "API Key 长度不能超过 512 个字符")
    private String apiKey;

    /** 能力类型：CHAT / EMBEDDING / RERANK */
    @NotBlank(message = "能力类型不能为空")
    @Pattern(regexp = "^(CHAT|EMBEDDING|RERANK)$", message = "能力类型必须是 CHAT/EMBEDDING/RERANK 之一")
    private String type;

    /** 扩展配置（JSON 字符串，可空） */
    private String config;
}
