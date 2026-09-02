package com.insightengine.ums.controller;

import com.insightengine.common.core.Result;
import com.insightengine.ums.dto.request.LoginRequest;
import com.insightengine.ums.dto.request.RefreshRequest;
import com.insightengine.ums.dto.request.RegisterRequest;
import com.insightengine.ums.dto.response.LoginResponse;
import com.insightengine.ums.dto.response.UserInfoVO;
import com.insightengine.ums.service.AuthService;
import com.insightengine.starter.web.context.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口（IF §3）。
 *
 * <p>统一前缀 {@code /auth}（IF §1.1 认证类接口前缀）。登录/注册/刷新为白名单
 * （starter-security 已放行）；登出/当前用户需携带有效访问令牌。</p>
 */
@Tag(name = "认证管理", description = "登录、刷新令牌、登出、注册、当前用户信息")
@RestController
@RequestMapping("/auth")
public class AuthController {

    /** Bearer 前缀长度，用于从 Authorization 头提取纯 token */
    private static final int BEARER_PREFIX_LENGTH = "Bearer ".length();

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 登录（IF §3.1）。
     */
    @Operation(summary = "账号密码登录", description = "支持邮箱/手机号登录，密码错误 5 次锁定 30 分钟")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    /**
     * 刷新令牌（IF §3.2）。
     */
    @Operation(summary = "刷新令牌", description = "用 refreshToken 换取新的令牌对")
    @PostMapping("/refresh")
    public Result<LoginResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return Result.ok(authService.refresh(request));
    }

    /**
     * 登出（IF §3.3）。
     *
     * <p>从 Authorization 头取当前 access token 加入黑名单。该接口需认证通过
     * （不在白名单），故能走到此处说明 token 有效。</p>
     */
    @Operation(summary = "登出", description = "当前 access token 加入黑名单，立即失效")
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String authorization) {
        String token = authorization.substring(BEARER_PREFIX_LENGTH).trim();
        authService.logout(token);
        return Result.ok();
    }

    /**
     * 注册（IF §3.4，MVP 开放）。
     */
    @Operation(summary = "注册", description = "MVP 开放注册，新用户挂默认工作空间 + end_user 角色")
    @PostMapping("/register")
    public Result<Long> register(@Valid @RequestBody RegisterRequest request) {
        return Result.ok(authService.register(request));
    }

    /**
     * 当前用户信息（IF §3.5）。
     *
     * <p>用户 ID 从认证上下文读取（JWT 已解析并填充 {@link UserContext}），
     * 不信任客户端传参，杜绝水平越权。</p>
     */
    @Operation(summary = "当前用户信息", description = "返回当前登录用户信息 + 角色 + 工作空间")
    @GetMapping("/me")
    public Result<UserInfoVO> me() {
        Long userId = UserContext.getUserId();
        return Result.ok(authService.currentUser(userId));
    }
}
