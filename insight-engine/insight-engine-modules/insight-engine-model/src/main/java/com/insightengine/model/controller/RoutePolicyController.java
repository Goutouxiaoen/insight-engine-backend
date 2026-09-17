package com.insightengine.model.controller;

import com.insightengine.common.core.Result;
import com.insightengine.model.dto.request.RoutePolicyCreateRequest;
import com.insightengine.model.dto.request.RoutePolicyStatusRequest;
import com.insightengine.model.dto.request.RoutePolicyUpdateRequest;
import com.insightengine.model.dto.response.RoutePolicyVO;
import com.insightengine.model.service.RoutePolicyService;
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

import java.util.List;

/**
 * 模型路由策略接口（IF §7.4）。
 *
 * <p>统一前缀 {@code /api/v1/model/route}；读 {@code model:route:read}、写 {@code model:route:write}
 * （实测该权限仅 super_admin / org_admin 持有 → **组织级资源**，不做空间维度判定）。</p>
 *
 * <p>返回顺序 = 生效顺序：列表按 {@code priority 升序、id 升序}，与 {@code auto} 解析时的匹配顺序一致。</p>
 */
@Tag(name = "模型路由策略", description = "策略列表 / 创建 / 更新 / 启停（rules 结构写入即校验）")
@RestController
@RequestMapping("/api/v1/model/route")
public class RoutePolicyController {

    private final RoutePolicyService routePolicyService;

    public RoutePolicyController(RoutePolicyService routePolicyService) {
        this.routePolicyService = routePolicyService;
    }

    /**
     * 策略列表（IF §7.4，权限 {@code model:route:read}）。
     */
    @Operation(summary = "路由策略列表", description = "按优先级升序返回（即实际匹配顺序）；rules 以结构化 JSON 返回")
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('model:route:read')")
    public Result<List<RoutePolicyVO>> list() {
        return Result.ok(routePolicyService.list());
    }

    /**
     * 创建策略（IF §7.4，权限 {@code model:route:write}）。
     */
    @Operation(summary = "创建路由策略",
            description = "rules 结构：{strategy:PRIORITY, fallback:true, rules:[{match:{tenantId?,workspaceId?}, targets:[{modelId}]}]}")
    @PostMapping
    @PreAuthorize("hasAuthority('model:route:write')")
    public Result<Long> create(@Valid @RequestBody RoutePolicyCreateRequest request) {
        return Result.ok(routePolicyService.create(request));
    }

    /**
     * 更新策略（IF §7.4，权限 {@code model:route:write}）。
     */
    @Operation(summary = "更新路由策略", description = "仅更新非空字段；rules 传入即整体替换并重新校验")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('model:route:write')")
    public Result<Void> update(@PathVariable("id") Long id,
                              @Valid @RequestBody RoutePolicyUpdateRequest request) {
        routePolicyService.update(id, request);
        return Result.ok();
    }

    /**
     * 启停策略（IF §7.4，权限 {@code model:route:write}）。
     */
    @Operation(summary = "启用/停用路由策略", description = "停用后不再参与 auto 解析")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('model:route:write')")
    public Result<Void> updateStatus(@PathVariable("id") Long id,
                                    @Valid @RequestBody RoutePolicyStatusRequest request) {
        routePolicyService.updateStatus(id, request);
        return Result.ok();
    }
}
