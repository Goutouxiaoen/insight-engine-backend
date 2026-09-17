package com.insightengine.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用量明细实体，对应表 {@code ie_usage_record}（DB.md §5.9.2）。
 *
 * <p>口径（严格按 DB.md，不擅自扩列）：</p>
 * <ul>
 *   <li>{@code scope_type} ∈ {@code TENANT/WORKSPACE/USER}，{@code scope_id} 为对应对象 ID；</li>
 *   <li>{@code biz_type} ∈ {@code MODEL/TOOL/AGENT/KB}；</li>
 *   <li>{@code quantity} = 用量（token 数或调用次数）；模型调用写 **total token**；</li>
 *   <li>{@code cost} = 费用（元）；Token Plan 为套餐制、无单 token 价目 → 当前写 NULL（不编造）；</li>
 *   <li>本表**只增不改**，且**无逻辑删除列**（故实体不定义 {@code deleted}）。</li>
 * </ul>
 *
 * <p>⚠️ 与 DB.md 的一处偏差（已登记）：DB.md 注明"异步 MQ 上报"，但 RabbitMQ 尚未迁云，
 * 当前实现为**同步落库**；等 MQ 就绪后可改为队列上报（写入方接口不变，只换实现）。</p>
 */
@Data
@TableName("ie_usage_record")
public class UsageRecord {

    /** 主键（BIGSERIAL 自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 计量维度：TENANT / WORKSPACE / USER */
    private String scopeType;

    /** 计量对象 ID */
    private Long scopeId;

    /** 业务类型：MODEL / TOOL / AGENT / KB */
    private String bizType;

    /** 业务引用 ID（模型调用 = ie_model.id） */
    private Long refId;

    /** 用量（token 数或调用次数） */
    private Long quantity;

    /** 费用（元）；无价目时为 NULL */
    private BigDecimal cost;

    /** 链路追踪 ID（与日志/响应头 X-Trace-Id 一致，便于对账排障） */
    private String traceId;

    /** 创建时间（UTC，库默认 NOW()） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
