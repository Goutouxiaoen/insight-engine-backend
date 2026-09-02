package com.insightengine.ums.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求体（IF §3.1）。
 *
 * <p>{@code account} 可为邮箱或手机号；{@code password} 为明文，仅用于本次请求
 * 传输，服务端校验后即丢弃，不落库、不打印（日志脱敏）。</p>
 */
@Data
public class LoginRequest {

    /** 登录账号（邮箱或手机号） */
    @NotBlank(message = "账号不能为空")
    private String account;

    /** 明文密码 */
    @NotBlank(message = "密码不能为空")
    private String password;
}
