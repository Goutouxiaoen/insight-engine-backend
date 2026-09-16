package com.insightengine.workspace.dto.response;

import lombok.Data;

import java.io.Serializable;

/**
 * 切换工作空间响应（IF §5.5）。
 *
 * <p>返回重签后的新令牌对：前端 {@code setTokens(token, refreshToken)} 后，后续请求
 * 携带的 JWT 即带新 {@code ws_id} 与按目标空间展开的 {@code roles}/{@code perms}。</p>
 *
 * <p>语义（与 UMS 登录/刷新一致，TD §7.2 / §6.1）：</p>
 * <ul>
 *   <li>access token 载荷最小必要身份 + 目标空间角色/权限；</li>
 *   <li>refresh token <b>一次性轮换</b>（新 jti 覆盖服务端会话），旧 refresh 立即失效；</li>
 *   <li>服务端登录态摘要同步覆盖，因此换签前的旧 access token 立即失效（单会话语义）。</li>
 * </ul>
 */
@Data
public class WorkspaceSwitchVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 新访问令牌 */
    private String token;

    /** 新刷新令牌（轮换后） */
    private String refreshToken;

    /** 访问令牌有效期（秒） */
    private Long expiresIn;
}
