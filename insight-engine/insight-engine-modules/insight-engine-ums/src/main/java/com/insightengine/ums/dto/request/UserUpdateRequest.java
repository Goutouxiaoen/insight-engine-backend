package com.insightengine.ums.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新用户请求体（IF §4.3）。
 *
 * <p>仅允许更新昵称/手机号；邮箱与角色变更不在本接口范围（邮箱变更属高危操作，
 * 需独立流程；角色变更走成员接口），避免本接口权限边界过宽。</p>
 */
@Data
public class UserUpdateRequest {

    /** 昵称（可选） */
    @Size(max = 64, message = "昵称长度不能超过 64 个字符")
    private String nickname;

    /** 手机号（可选，空串视为清空） */
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
}
