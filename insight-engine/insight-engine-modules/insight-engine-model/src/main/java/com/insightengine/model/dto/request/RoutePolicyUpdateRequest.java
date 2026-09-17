package com.insightengine.model.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新路由策略请求体（IF §7.4）。仅更新非空字段；启停另有专用接口（{@code PUT /route/{id}/status}）。
 */
@Data
public class RoutePolicyUpdateRequest {

    /** 策略名称 */
    @Size(max = 128, message = "策略名称长度不能超过 128 个字符")
    private String name;

    /** 优先级（数值越小越先匹配） */
    @Min(value = 1, message = "优先级最小为 1")
    private Integer priority;

    /** 规则（JSON 对象）；传入即整体替换并重新校验 */
    private JsonNode rules;
}
