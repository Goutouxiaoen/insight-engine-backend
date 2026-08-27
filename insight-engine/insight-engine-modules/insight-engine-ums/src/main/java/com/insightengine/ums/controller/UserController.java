package com.insightengine.ums.controller;

import com.insightengine.common.core.PageResult;
import com.insightengine.common.core.Result;
import com.insightengine.ums.dto.request.PasswordUpdateRequest;
import com.insightengine.ums.dto.request.UserCreateRequest;
import com.insightengine.ums.dto.request.UserPageQuery;
import com.insightengine.ums.dto.request.UserStatusRequest;
import com.insightengine.ums.dto.request.UserUpdateRequest;
import com.insightengine.ums.dto.response.UserPageVO;
import com.insightengine.ums.service.UserService;
import com.insightengine.starter.web.context.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户管理接口（IF §4）。
 *
 * <p>统一前缀 {@code /api/v1/user}。管理类接口用 {@code @PreAuthorize} 做方法级权限
 * （TD §7.4）；改密接口为本人操作，仅需登录态。</p>
 */
@Tag(name = "用户管理", description = "用户分页、创建、更新、启停、修改密码")
@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 用户分页列表（IF §4.1，权限 member:read）。
     */
    @Operation(summary = "用户分页列表", description = "keyword 模糊匹配昵称/邮箱，手机号脱敏输出")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('member:read')")
    public Result<PageResult<UserPageVO>> page(@Valid UserPageQuery query) {
        return Result.ok(userService.page(query));
    }

    /**
     * 创建用户（IF §4.2，权限 member:create）。
     */
    @Operation(summary = "创建用户", description = "管理员创建用户并挂默认工作空间 + 指定角色")
    @PostMapping
    @PreAuthorize("hasAuthority('member:create')")
    public Result<Long> create(@Valid @RequestBody UserCreateRequest request) {
        return Result.ok(userService.create(request));
    }

    /**
     * 更新用户（IF §4.3，权限 member:update）。
     */
    @Operation(summary = "更新用户", description = "更新昵称/手机号")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('member:update')")
    public Result<Void> update(@PathVariable("id") Long id,
                               @Valid @RequestBody UserUpdateRequest request) {
        userService.update(id, request);
        return Result.ok();
    }

    /**
     * 启用/禁用用户（IF §4.4，权限 member:update）。
     */
    @Operation(summary = "启用/禁用用户", description = "禁用后删除登录态，强制重新登录")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('member:update')")
    public Result<Void> updateStatus(@PathVariable("id") Long id,
                                     @Valid @RequestBody UserStatusRequest request) {
        userService.updateStatus(id, request);
        return Result.ok();
    }

    /**
     * 修改密码（IF §4.5，本人操作）。
     *
     * <p>用户 ID 从认证上下文读取，保证只能改自己的密码，杜绝越权改密。</p>
     */
    @Operation(summary = "修改密码", description = "本人操作，校验旧密码后更新，改密后旧登录态失效")
    @PutMapping("/password")
    public Result<Void> updatePassword(@Valid @RequestBody PasswordUpdateRequest request) {
        Long userId = UserContext.getUserId();
        userService.updatePassword(userId, request);
        return Result.ok();
    }
}
