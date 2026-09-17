package com.insightengine.model.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新模型厂商请求体（IF §7.1）。仅更新传入的非空字段。
 *
 * <p><b>{@code apiKey} 语义（契约要点）</b>：</p>
 * <ul>
 *   <li>留空/null → <b>不修改密钥</b>（避免前端回显"掩码"再提交时把密钥写成 {@code sk-****1234} 这种假值）；</li>
 *   <li>传新值 → 加密覆盖同一行 {@code ie_secret}（引用不变，见 {@code SecretStore#save}）。</li>
 * </ul>
 *
 * <p>{@code code} 不可修改（唯一键 + 语义标识），故本请求体不提供该字段。</p>
 */
@Data
public class VendorUpdateRequest {

    /** 厂商名称 */
    @Size(max = 128, message = "厂商名称长度不能超过 128 个字符")
    private String name;

    /** 接口基址 */
    @Size(max = 255, message = "接口基址长度不能超过 255 个字符")
    private String baseUrl;

    /** 新的 API Key（留空表示不修改密钥） */
    @Size(max = 512, message = "API Key 长度不能超过 512 个字符")
    private String apiKey;

    /** 能力类型：CHAT / EMBEDDING / RERANK */
    @Pattern(regexp = "^$|^(CHAT|EMBEDDING|RERANK)$", message = "能力类型必须是 CHAT/EMBEDDING/RERANK 之一")
    private String type;

    /** 是否启用：1 启用 / 0 停用 */
    private Integer enabled;

    /** 扩展配置（JSON 字符串） */
    private String config;
}
