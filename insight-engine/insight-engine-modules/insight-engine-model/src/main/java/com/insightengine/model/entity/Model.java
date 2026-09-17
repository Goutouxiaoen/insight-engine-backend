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
 * 模型实体，对应表 {@code ie_model}（DB.md §5.3.2）。
 *
 * <p>模型 = 厂商下的一个具体模型（如 {@code qwen-plus} / {@code text-embedding-v3}），
 * 带上下文窗口与单价（计价用）。唯一约束 {@code uk_model_vendor_code(vendor_id, code)}。</p>
 */
@Data
@TableName("ie_model")
public class Model {

    /** 主键（BIGSERIAL 自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属厂商 ID（ie_model_vendor.id） */
    private Long vendorId;

    /** 模型编码（同厂商内唯一，如 qwen-plus；创建后不可修改） */
    private String code;

    /** 展示名称（如「通义千问 Plus」） */
    private String displayName;

    /** 能力类型：CHAT / EMBEDDING / RERANK */
    private String type;

    /** 上下文窗口（token 数；EMBEDDING/RERANK 可为空） */
    private Integer contextWindow;

    /**
     * 输入单价（元 / 1K token，DECIMAL(18,6)）。
     *
     * <p><b>必须显式声明列名</b>：MyBatis-Plus 的驼峰→下划线转换按"大写字母前插下划线"规则处理，
     * {@code inputPricePer1k} 会被转成 {@code input_price_per1k}，而库里是 {@code input_price_per_1k}
     * （数字前也要下划线）→ 不显式指定就报 {@code column "input_price_per1k" does not exist}
     * （2026-09-17 冒烟实测踩到）。</p>
     */
    @TableField("input_price_per_1k")
    private BigDecimal inputPricePer1k;

    /** 输出单价（元 / 1K token，DECIMAL(18,6)）；列名显式声明原因同上 */
    @TableField("output_price_per_1k")
    private BigDecimal outputPricePer1k;

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
