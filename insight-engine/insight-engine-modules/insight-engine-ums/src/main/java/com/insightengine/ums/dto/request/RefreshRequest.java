package com.insightengine.ums.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 刷新令牌请求体（IF §3.2）。
 *
 * <p>仅携带刷新令牌；访问令牌过期后由前端调用本接口换取新令牌对。</p>
 */
@Data
public class RefreshRequest {

    /** 刷新令牌 */
    @NotBlank(message = "刷新令牌不能为空")
    private String refreshToken;
}
