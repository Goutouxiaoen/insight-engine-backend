package com.insightengine.ums.dto.response;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 用户信息视图对象（登录/当前用户返回，IF §3.1 / §3.5）。
 *
 * <p>{@code phone} 输出前已脱敏（IF §4.1：138****1234），
 * 绝不把明文手机号/密码散列返回给客户端（TD §16.3 输出脱敏）。</p>
 */
@Data
public class UserInfoVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户 ID */
    private Long id;

    /** 昵称 */
    private String nickname;

    /** 邮箱 */
    private String email;

    /** 手机号（脱敏后） */
    private String phone;

    /** 头像 URL */
    private String avatar;

    /** 角色编码列表 */
    private List<String> roles;

    /** 租户 ID */
    private Long tenantId;

    /** 当前工作空间 ID */
    private Long workspaceId;

    /** 当前工作空间名称 */
    private String workspaceName;
}
