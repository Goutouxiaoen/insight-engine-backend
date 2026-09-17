package com.insightengine.model.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建路由策略请求体（IF §7.4）。
 *
 * <p>{@code rules} 为 JSONB 结构，服务端**解析并校验**（strategy / rules / targets），而不是原样入库——
 * 否则一条写错的策略会在请求时才炸。</p>
 */
@Data
public class RoutePolicyCreateRequest {

    /** 策略名称 */
    @NotBlank(message = "策略名称不能为空")
    @Size(max = 128, message = "策略名称长度不能超过 128 个字符")
    private String name;

    /** 优先级：**数值越小越先匹配** */
    @NotNull(message = "优先级不能为空")
    @Min(value = 1, message = "优先级最小为 1")
    private Integer priority;

    /** 规则（JSON 对象） */
    @NotNull(message = "规则不能为空")
    private JsonNode rules;
}
