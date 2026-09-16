package com.insightengine.workspace.controller;

import com.insightengine.common.core.PageResult;
import com.insightengine.common.core.Result;
import com.insightengine.starter.web.context.UserContext;
import com.insightengine.workspace.dto.request.WorkspaceCreateRequest;
import com.insightengine.workspace.dto.request.WorkspacePageQuery;
import com.insightengine.workspace.dto.request.WorkspaceSwitchRequest;
import com.insightengine.workspace.dto.request.WorkspaceUpdateRequest;
import com.insightengine.workspace.dto.response.WorkspaceSwitchVO;
import com.insightengine.workspace.dto.response.WorkspaceVO;
import com.insightengine.workspace.service.WorkspaceService;
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

/**
 * 工作空间管理接口（IF §5.3 / §5.4 / §5.5）。
 *
 * <p>统一前缀 {@code /api/v1/workspace}；切换空间用 {@code ws:read} 门控——切换目标范围由服务端
 * 「成员关系」强约束，故不新增 {@code ws:switch} 权限码（PROGRESS §三 2026-09-09 裁决，
 * 答复前端 BE-20260908-01）。</p>
 */
@Tag(name = "工作空间管理", description = "工作空间创建、更新、删除、分页、切换")
@RestController
@RequestMapping("/api/v1/workspace")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    /**
     * 创建工作空间（IF §5.3，权限 ws:create）。
     */
    @Operation(summary = "创建工作空间", description = "编码同组织内唯一；创建者自动成为空间管理员")
    @PostMapping
    @PreAuthorize("hasAuthority('ws:create')")
    public Result<Long> create(@Valid @RequestBody WorkspaceCreateRequest request) {
        return Result.ok(workspaceService.create(request));
    }

    /**
     * 更新工作空间（IF §5.3，权限 ws:write）。
     */
    @Operation(summary = "更新工作空间", description = "可改名称与配额；编码与所属组织不可修改")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ws:write')")
    public Result<Void> update(@PathVariable("id") Long id,
                               @Valid @RequestBody WorkspaceUpdateRequest request) {
        workspaceService.update(id, request);
        return Result.ok();
    }

    /**
     * 删除工作空间（权限 ws:delete）。
     *
     * <p>IF §5.3 只定义创建/更新，删除为本轮按前端交付需求（空间删除按钮，权限码
     * {@code ws:delete} 已存在于权限字典）补充的契约，已登记 FE-SYNC §2。</p>
     */
    @Operation(summary = "删除工作空间", description = "逻辑删除空间及其全部成员关系；不允许删除当前所处空间")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ws:delete')")
    public Result<Void> delete(@PathVariable("id") Long id) {
        workspaceService.delete(id);
        return Result.ok();
    }

    /**
     * 工作空间分页（IF §5.4，权限 ws:read）。
     */
    @Operation(summary = "工作空间分页", description = "组织级管理员可见组织内全部空间，其他用户仅见自己所属空间")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('ws:read')")
    public Result<PageResult<WorkspaceVO>> page(@Valid WorkspacePageQuery query) {
        return Result.ok(workspaceService.page(query));
    }

    /**
     * 切换当前工作空间（IF §5.5，权限 ws:read）。
     *
     * <p>用户 ID 从认证上下文读取（不信任客户端传参），服务端校验成员关系后重签令牌对。</p>
     */
    @Operation(summary = "切换工作空间", description = "校验成员关系后重签 JWT（携带新 ws_id 与目标空间角色/权限），旧令牌立即失效")
    @PostMapping("/switch")
    @PreAuthorize("hasAuthority('ws:read')")
    public Result<WorkspaceSwitchVO> switchWorkspace(@Valid @RequestBody WorkspaceSwitchRequest request) {
        Long userId = UserContext.getUserId();
        return Result.ok(workspaceService.switchWorkspace(userId, request));
    }
}
