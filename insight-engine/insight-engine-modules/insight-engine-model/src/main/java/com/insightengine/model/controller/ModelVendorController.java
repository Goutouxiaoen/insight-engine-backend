package com.insightengine.model.controller;

import com.insightengine.common.core.PageResult;
import com.insightengine.common.core.Result;
import com.insightengine.model.dto.request.VendorCreateRequest;
import com.insightengine.model.dto.request.VendorPageQuery;
import com.insightengine.model.dto.request.VendorUpdateRequest;
import com.insightengine.model.dto.response.VendorVO;
import com.insightengine.model.service.ModelVendorService;
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
 * 模型厂商管理接口（IF §7.1）。
 *
 * <p>统一前缀 {@code /api/v1/model/vendor}；读 {@code model:vendor:read}、写 {@code model:vendor:write}。</p>
 *
 * <p><b>权限分域说明</b>：厂商接入属**组织级/平台级**动作（模型目录为平台级，所有业务方共用，
 * 2026-09-17 裁决路线 A），与"在哪个空间"无关 → **只做第一层动作门控，不加空间维度校验**
 * （与 {@code ws:create}/{@code ws:delete} 同处理，见 IF §5.6 分域说明）。</p>
 */
@Tag(name = "模型厂商管理", description = "厂商分页、创建、更新、删除（API Key 加密存储，读取只回掩码）")
@RestController
@RequestMapping("/api/v1/model/vendor")
public class ModelVendorController {

    private final ModelVendorService vendorService;

    public ModelVendorController(ModelVendorService vendorService) {
        this.vendorService = vendorService;
    }

    /**
     * 厂商分页（IF §7.1，权限 {@code model:vendor:read}）。
     */
    @Operation(summary = "模型厂商分页", description = "支持编码/名称模糊、能力类型与启停过滤；密钥只回掩码")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('model:vendor:read')")
    public Result<PageResult<VendorVO>> page(@Valid VendorPageQuery query) {
        return Result.ok(vendorService.page(query));
    }

    /**
     * 创建厂商（IF §7.1，权限 {@code model:vendor:write}）。
     */
    @Operation(summary = "创建模型厂商",
            description = "code 唯一且创建后不可改；apiKey 明文入参，服务端 AES-256-GCM 加密后落 ie_secret（无鉴权本地模型可留空）")
    @PostMapping
    @PreAuthorize("hasAuthority('model:vendor:write')")
    public Result<Long> create(@Valid @RequestBody VendorCreateRequest request) {
        return Result.ok(vendorService.create(request));
    }

    /**
     * 更新厂商（IF §7.1，权限 {@code model:vendor:write}）。
     */
    @Operation(summary = "更新模型厂商", description = "仅更新非空字段；apiKey 留空表示不修改密钥（不会把掩码写成密钥）")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('model:vendor:write')")
    public Result<Void> update(@PathVariable("id") Long id,
                              @Valid @RequestBody VendorUpdateRequest request) {
        vendorService.update(id, request);
        return Result.ok();
    }

    /**
     * 删除厂商（IF §7.1，权限 {@code model:vendor:write}）。
     */
    @Operation(summary = "删除模型厂商", description = "逻辑删除；密钥记录保留以留审计痕迹")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('model:vendor:write')")
    public Result<Void> delete(@PathVariable("id") Long id) {
        vendorService.delete(id);
        return Result.ok();
    }
}
