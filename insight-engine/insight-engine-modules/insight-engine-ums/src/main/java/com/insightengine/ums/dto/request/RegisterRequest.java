package com.insightengine.ums.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求体（IF §3.4）。
 *
 * <p>入参校验（JSR-303）：邮箱格式、密码强度（8~32 且含大小写/数字）、昵称长度。
 * 密码复杂度用正则约束，避免弱口令（TD §16.1 安全要求）。</p>
 */
@Data
public class RegisterRequest {

    /** 邮箱 */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    /** 明文密码（至少 8 位，须含字母和数字） */
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 32, message = "密码长度需在 8~32 位之间")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "密码须同时包含字母和数字")
    private String password;

    /** 昵称 */
    @NotBlank(message = "昵称不能为空")
    @Size(max = 64, message = "昵称长度不能超过 64 个字符")
    private String nickname;
}
