package com.insightengine.ums.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改密码请求体（IF §4.5）。
 *
 * <p>需校验旧密码正确后才允许更新（防止越权改密），新密码满足复杂度要求。</p>
 */
@Data
public class PasswordUpdateRequest {

    /** 旧密码（明文） */
    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    /** 新密码（明文） */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 32, message = "密码长度需在 8~32 位之间")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "密码须同时包含字母和数字")
    private String newPassword;
}
