package com.insightengine.workspace.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 添加空间成员请求体（IF §5.6 {@code POST /api/v1/member/invite}）。
 *
 * <p>MVP 语义为「把已注册用户直接加入空间」：邮箱须对应平台已注册用户（前端提示
 * 「已注册成员邮箱」），未注册返回 1001 业务提示；重复加入同样 1001。</p>
 */
@Data
public class MemberInviteRequest {

    /** 工作空间 ID */
    @NotNull(message = "工作空间不能为空")
    private Long workspaceId;

    /** 成员邮箱（须为已注册用户） */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    /** 空间内角色 ID */
    @NotNull(message = "空间角色不能为空")
    private Long roleId;
}
