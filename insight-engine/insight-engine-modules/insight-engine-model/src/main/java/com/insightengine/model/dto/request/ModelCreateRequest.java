package com.insightengine.model.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建模型请求体（IF §7.3）。
 *
 * <p>模型挂在厂商下：{@code (vendorId, code)} 唯一（DB {@code uk_model_vendor_code}），
 * {@code code} 创建后不可修改（它是对厂商 API 的模型名，改它等于换模型）。</p>
 */
@Data
public class ModelCreateRequest {

    /** 所属厂商 ID */
    @NotNull(message = "厂商不能为空")
    private Long vendorId;

    /** 模型编码（对厂商 API 的模型名，如 qwen-plus） */
    @NotBlank(message = "模型编码不能为空")
    @Size(max = 128, message = "模型编码长度不能超过 128 个字符")
    private String code;

    /** 展示名称 */
    @Size(max = 128, message = "展示名称长度不能超过 128 个字符")
    private String displayName;

    /** 能力类型：CHAT / EMBEDDING / RERANK */
    @NotBlank(message = "能力类型不能为空")
    @Pattern(regexp = "^(CHAT|EMBEDDING|RERANK)$", message = "能力类型必须是 CHAT/EMBEDDING/RERANK 之一")
    private String type;

    /** 上下文窗口（token 数，可空） */
    @Min(value = 1, message = "上下文窗口必须为正数")
    private Integer contextWindow;

    /** 输入单价（元 / 1K token） */
    @DecimalMin(value = "0", message = "输入单价不能为负")
    private BigDecimal inputPricePer1k;

    /** 输出单价（元 / 1K token） */
    @DecimalMin(value = "0", message = "输出单价不能为负")
    private BigDecimal outputPricePer1k;
}
