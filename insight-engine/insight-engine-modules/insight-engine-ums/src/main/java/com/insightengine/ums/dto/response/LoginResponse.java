package com.insightengine.ums.dto.response;

import lombok.Data;

import java.io.Serializable;

/**
 * 登录/刷新成功响应体（IF §3.1）。
 *
 * <p>返回访问令牌 + 刷新令牌 + 有效期（秒）+ 基础用户信息。
 * {@code expiresIn} 取访问令牌有效期，前端据此提前刷新（TD §7.2：2h）。</p>
 */
@Data
public class LoginResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 访问令牌（JWT） */
    private String token;

    /** 刷新令牌（JWT） */
    private String refreshToken;

    /** 访问令牌有效期（秒） */
    private long expiresIn;

    /** 基础用户信息 */
    private UserInfoVO user;
}
