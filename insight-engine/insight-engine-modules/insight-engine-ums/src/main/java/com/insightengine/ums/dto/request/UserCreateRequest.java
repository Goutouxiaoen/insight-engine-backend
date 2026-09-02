package com.insightengine.ums.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建用户请求体（IF §4.2，管理员操作）。
 *
 * <p>与注册接口的差异：管理员可指定 {@code roleId}，且手机号可填。
 * 邮箱唯一性、密码复杂度在 Service 层二次校验（DB 唯一索引兜底）。</p>
 */
@Data
public class UserCreateRequest {

    /** 邮箱 */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    /** 昵称 */
    @NotBlank(message = "昵称不能为空")
    @Size(max = 64, message = "昵称长度不能超过 64 个字符")
    private String nickname;

    /** 明文密码 */
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 32, message = "密码长度需在 8~32 位之间")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "密码须同时包含字母和数字")
    private String password;

    /** 手机号（可选） */
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 角色 ID */
    @NotNull(message = "角色不能为空")
    private Long roleId;
}
