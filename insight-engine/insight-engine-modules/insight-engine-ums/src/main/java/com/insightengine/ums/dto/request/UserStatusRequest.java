package com.insightengine.ums.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 启用/禁用用户请求体（IF §4.4）。
 *
 * <p>{@code status} 取值 0（禁用）/1（启用），用 JSR-303 范围约束拒绝非法值。</p>
 */
@Data
public class UserStatusRequest {

    /** 目标状态：0 禁用 / 1 启用 */
    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态值非法")
    @Max(value = 1, message = "状态值非法")
    private Integer status;
}
