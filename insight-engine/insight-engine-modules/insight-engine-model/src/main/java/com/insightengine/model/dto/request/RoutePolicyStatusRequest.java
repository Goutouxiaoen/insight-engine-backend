package com.insightengine.model.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 路由策略启停请求体（IF §7.4 {@code PUT /api/v1/model/route/{id}/status}）。
 */
@Data
public class RoutePolicyStatusRequest {

    /** 是否启用：1 启用 / 0 停用 */
    @NotNull(message = "enabled 不能为空")
    @Min(value = 0, message = "enabled 只能是 0 或 1")
    @Max(value = 1, message = "enabled 只能是 0 或 1")
    private Integer enabled;
}
