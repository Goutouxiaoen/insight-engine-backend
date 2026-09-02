package com.insightengine.ums.controller;

import com.insightengine.common.core.Result;
import com.insightengine.ums.dto.request.RoleCreateRequest;
import com.insightengine.ums.dto.request.RolePermissionUpdateRequest;
import com.insightengine.ums.dto.response.RoleVO;
import com.insightengine.ums.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 角色管理接口（IF §6）。
 *
 * <p>读操作用 {@code role:read}，写操作用 {@code role:write}（IF §6 权限约定）。</p>
 */
@Tag(name = "角色管理", description = "角色列表、创建、详情、删除、授权")
@RestController
@RequestMapping("/api/v1/role")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    /**
     * 角色列表（IF §6.1）。
     */
    @Operation(summary = "角色列表", description = "返回全部角色（含内置角色），不含权限明细")
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('role:read')")
    public Result<List<RoleVO>> list() {
        return Result.ok(roleService.list());
    }

    /**
     * 创建角色（IF §6.2）。
     */
    @Operation(summary = "创建角色", description = "角色编码唯一，可同时授权权限")
    @PostMapping
    @PreAuthorize("hasAuthority('role:write')")
    public Result<Long> create(@Valid @RequestBody RoleCreateRequest request) {
        return Result.ok(roleService.create(request));
    }

    /**
     * 角色详情（IF §6.5）。
     */
    @Operation(summary = "角色详情", description = "含已授权权限 ID 列表")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('role:read')")
    public Result<RoleVO> detail(@PathVariable("id") Long id) {
        return Result.ok(roleService.detail(id));
    }

    /**
     * 删除角色（IF §6.6）。
     */
    @Operation(summary = "删除角色", description = "内置角色（builtin=1）禁止删除，返回 1003")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('role:write')")
    public Result<Void> delete(@PathVariable("id") Long id) {
        roleService.delete(id);
        return Result.ok();
    }

    /**
     * 角色授权（IF §6.4）。
     */
    @Operation(summary = "角色授权", description = "先删后插，完整覆盖权限集合（幂等）")
    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('role:write')")
    public Result<Void> assignPermissions(@PathVariable("id") Long id,
                                          @Valid @RequestBody RolePermissionUpdateRequest request) {
        roleService.assignPermissions(id, request);
        return Result.ok();
    }
}
