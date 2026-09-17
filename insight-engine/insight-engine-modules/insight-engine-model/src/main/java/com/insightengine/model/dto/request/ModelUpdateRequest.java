package com.insightengine.model.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 更新模型请求体（IF §7.3）。仅更新传入的非空字段。
 *
 * <p>{@code vendorId} 与 {@code code} 不可修改：它们共同构成唯一键，且 {@code code} 是对厂商 API 的模型名
 * —— "换模型"应新建一条记录（历史用量按 modelId 归属，改 code 会让旧用量记录指向一个语义已变的模型）。</p>
 */
@Data
public class ModelUpdateRequest {

    /** 展示名称 */
    @Size(max = 128, message = "展示名称长度不能超过 128 个字符")
    private String displayName;

    /** 能力类型：CHAT / EMBEDDING / RERANK */
    @Pattern(regexp = "^$|^(CHAT|EMBEDDING|RERANK)$", message = "能力类型必须是 CHAT/EMBEDDING/RERANK 之一")
    private String type;

    /** 上下文窗口 */
    @Min(value = 1, message = "上下文窗口必须为正数")
    private Integer contextWindow;

    /** 输入单价（元 / 1K token） */
    @DecimalMin(value = "0", message = "输入单价不能为负")
    private BigDecimal inputPricePer1k;

    /** 输出单价（元 / 1K token） */
    @DecimalMin(value = "0", message = "输出单价不能为负")
    private BigDecimal outputPricePer1k;

    /** 是否启用：1 启用 / 0 停用 */
    private Integer enabled;
}
