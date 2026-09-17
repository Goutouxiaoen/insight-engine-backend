package com.insightengine.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.insightengine.model.support.JsonbTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 模型路由策略实体，对应表 {@code ie_route_policy}（DB.md §5.3.3）。
 *
 * <p>作用：把"逻辑模型名（如 {@code auto}）"翻译成"具体用哪个模型"——同一逻辑名可按优先级、
 * 匹配条件、目标序列（主/备）落到不同物理模型上。</p>
 *
 * <p>{@code rules} 是 {@code jsonb}，用 {@link JsonbTypeHandler} 以 String 形式读写（原因见该类注释）。</p>
 */
@Data
@TableName("ie_route_policy")
public class RoutePolicy {

    /** 主键（BIGSERIAL 自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 策略名称（如「默认路由」） */
    private String name;

    /** 规则（JSONB：strategy / fallback / rules[].match / rules[].targets） */
    @TableField(value = "rules", typeHandler = JsonbTypeHandler.class)
    private String rules;

    /** 优先级（**数值越小越先匹配**，见 IF §7.4） */
    private Integer priority;

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
