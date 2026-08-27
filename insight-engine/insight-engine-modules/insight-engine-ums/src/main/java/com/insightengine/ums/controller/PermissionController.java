package com.insightengine.ums.controller;

import com.insightengine.common.core.Result;
import com.insightengine.ums.dto.response.PermissionGroupVO;
import com.insightengine.ums.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 权限接口（IF §6.3）。
 *
 * <p>权限树用于角色授权界面，读权限 {@code role:read}。</p>
 */
@Tag(name = "权限管理", description = "权限树（按资源分组）")
@RestController
@RequestMapping("/api/v1/permission")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    /**
     * 权限树（IF §6.3）。
     */
    @Operation(summary = "权限树", description = "按资源分组返回权限点，供角色授权界面勾选")
    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('role:read')")
    public Result<List<PermissionGroupVO>> tree() {
        return Result.ok(permissionService.tree());
    }
}
