package com.insightengine.workspace.controller;

import com.insightengine.common.core.Result;
import com.insightengine.workspace.dto.request.OrgCreateRequest;
import com.insightengine.workspace.dto.response.OrgVO;
import com.insightengine.workspace.service.OrgService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 组织管理接口（IF §5.1 / §5.2）。
 *
 * <p>统一前缀 {@code /api/v1/org}；读写分别用 {@code org:create} / {@code org:read} 方法级权限
 * （TD §7.4）。</p>
 */
@Tag(name = "组织管理", description = "组织创建、组织详情")
@RestController
@RequestMapping("/api/v1/org")
public class OrgController {

    private final OrgService orgService;

    public OrgController(OrgService orgService) {
        this.orgService = orgService;
    }

    /**
     * 创建组织（IF §5.1，权限 org:create）。
     */
    @Operation(summary = "创建组织", description = "编码同租户内唯一，创建者即所有者")
    @PostMapping
    @PreAuthorize("hasAuthority('org:create')")
    public Result<Long> create(@Valid @RequestBody OrgCreateRequest request) {
        return Result.ok(orgService.create(request));
    }

    /**
     * 组织详情（IF §5.2，权限 org:read）。
     */
    @Operation(summary = "组织详情", description = "按 ID 查询组织基本信息")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('org:read')")
    public Result<OrgVO> detail(@PathVariable("id") Long id) {
        return Result.ok(orgService.detail(id));
    }
}
