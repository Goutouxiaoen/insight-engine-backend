package com.insightengine.model.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 模型视图对象（IF §7.2）。
 *
 * <p><b>金额口径（IF §2.5，务必遵守）</b>：单价是**字符串**（单位「元」、6 位小数，如 {@code "0.001200"}），
 * 不是 JSON 数字 —— 金额用浮点会在下游出现精度误差（TD §5.x「金额用 DECIMAL，不用浮点」）。
 * 故这里显式用 {@link ToStringSerializer} 序列化，且保留 DB 的 scale=6。</p>
 *
 * <p>{@code vendorCode}/{@code vendorName} 为冗余字段，便于前端列表直接展示（避免再查一次厂商）。</p>
 */
@Data
public class ModelVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 模型 ID */
    private Long id;

    /** 所属厂商 ID */
    private Long vendorId;

    /** 所属厂商编码（冗余展示） */
    private String vendorCode;

    /** 所属厂商名称（冗余展示） */
    private String vendorName;

    /** 模型编码（对厂商 API 的模型名） */
    private String code;

    /** 展示名称 */
    private String displayName;

    /** 能力类型：CHAT / EMBEDDING / RERANK */
    private String type;

    /** 上下文窗口 */
    private Integer contextWindow;

    /** 输入单价（元 / 1K token，字符串，6 位小数） */
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal inputPricePer1k;

    /** 输出单价（元 / 1K token，字符串，6 位小数） */
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal outputPricePer1k;

    /** 是否启用：1 启用 / 0 停用 */
    private Integer enabled;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
