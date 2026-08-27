package com.insightengine.ums.service;

import com.insightengine.ums.dto.request.LoginRequest;
import com.insightengine.ums.dto.request.RefreshRequest;
import com.insightengine.ums.dto.request.RegisterRequest;
import com.insightengine.ums.dto.response.LoginResponse;
import com.insightengine.ums.dto.response.UserInfoVO;

/**
 * 认证服务接口（IF §3）。
 *
 * <p>覆盖登录/刷新/登出/注册/当前用户五个认证闭环接口。</p>
 */
public interface AuthService {

    /**
     * 账号密码登录（IF §3.1）。
     *
     * @param request 登录请求（账号 + 密码）
     * @return 令牌对 + 基础用户信息
     */
    LoginResponse login(LoginRequest request);

    /**
     * 刷新令牌（IF §3.2）。
     *
     * @param request 刷新请求（refreshToken）
     * @return 新的令牌对 + 基础用户信息
     */
    LoginResponse refresh(RefreshRequest request);

    /**
     * 登出（IF §3.3）：将当前 access token 加入黑名单，删除登录态。
     *
     * @param accessToken 当前访问令牌
     */
    void logout(String accessToken);

    /**
     * 注册（IF §3.4）：创建用户并挂到默认工作空间。
     *
     * @param request 注册请求
     * @return 新用户 ID
     */
    Long register(RegisterRequest request);

    /**
     * 当前用户信息（IF §3.5）。
     *
     * @param userId 用户 ID
     * @return 用户信息 + 角色 + 工作空间
     */
    UserInfoVO currentUser(Long userId);
}
