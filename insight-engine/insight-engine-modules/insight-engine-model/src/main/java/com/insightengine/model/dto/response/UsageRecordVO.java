package com.insightengine.model.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用量明细视图对象（IF §7.8）。
 *
 * <p>{@code cost} 按 IF §2.5 以**字符串**返回（元、6 位小数）；Token Plan 套餐制下暂为 {@code null}
 * （不编造金额），前端应显示"未定价"而不是 0。</p>
 *
 * <p>{@code modelCode}/{@code modelName} 为冗余字段（批量回填，避免前端逐行再查模型）。</p>
 */
@Data
public class UsageRecordVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用量记录 ID */
    private Long id;

    /** 模型 ID（ie_usage_record.ref_id） */
    private Long modelId;

    /** 模型编码（冗余展示） */
    private String modelCode;

    /** 模型展示名（冗余展示） */
    private String modelName;

    /** 计量维度：TENANT / WORKSPACE / USER */
    private String scopeType;

    /** 计量对象 ID（空间/租户 ID） */
    private Long scopeId;

    /** 用量（token 数） */
    private Long tokens;

    /** 费用（元，字符串 6 位小数）；无价目时为 null */
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal cost;

    /** 链路追踪 ID（对账/排障用） */
    private String traceId;

    /** 发生时间 */
    private LocalDateTime createdAt;
}
