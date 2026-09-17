package com.insightengine.model.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 路由策略视图对象（IF §7.4）。
 *
 * <p>{@code rules} 以**结构化 JSON**（非字符串）返回，前端可直接渲染规则编辑器。</p>
 */
@Data
public class RoutePolicyVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 策略 ID */
    private Long id;

    /** 策略名称 */
    private String name;

    /** 优先级（数值越小越先匹配） */
    private Integer priority;

    /** 规则（结构化 JSON） */
    private JsonNode rules;

    /** 是否启用：1 启用 / 0 停用 */
    private Integer enabled;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
