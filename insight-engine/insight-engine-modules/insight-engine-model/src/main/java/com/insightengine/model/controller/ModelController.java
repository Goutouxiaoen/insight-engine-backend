package com.insightengine.model.controller;

import com.insightengine.common.core.PageResult;
import com.insightengine.common.core.Result;
import com.insightengine.model.dto.request.ModelCreateRequest;
import com.insightengine.model.dto.request.ModelPageQuery;
import com.insightengine.model.dto.request.ModelUpdateRequest;
import com.insightengine.model.dto.response.ModelVO;
import com.insightengine.model.service.ModelService;
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
 * 模型目录接口（IF §7.2 / §7.3）。
 *
 * <p>统一前缀 {@code /api/v1/model}；读 {@code model:list:read}、写 {@code model:list:write}。</p>
 *
 * <p>权限分域同厂商接口：模型目录是平台级资源，**只做第一层动作门控**，不加空间维度校验。</p>
 */
@Tag(name = "模型目录管理", description = "模型分页（含厂商冗余）、创建、更新、删除")
@RestController
@RequestMapping("/api/v1/model")
public class ModelController {

    private final ModelService modelService;

    public ModelController(ModelService modelService) {
        this.modelService = modelService;
    }

    /**
     * 模型分页（IF §7.2，权限 {@code model:list:read}）。
     */
    @Operation(summary = "模型分页", description = "按厂商/类型/启停/关键字过滤；单价按 IF §2.5 以字符串返回（元、6 位小数）")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('model:list:read')")
    public Result<PageResult<ModelVO>> page(@Valid ModelPageQuery query) {
        return Result.ok(modelService.page(query));
    }

    /**
     * 创建模型（IF §7.3，权限 {@code model:list:write}）。
     */
    @Operation(summary = "创建模型", description = "厂商须存在；同一厂商下 code 唯一且创建后不可改")
    @PostMapping
    @PreAuthorize("hasAuthority('model:list:write')")
    public Result<Long> create(@Valid @RequestBody ModelCreateRequest request) {
        return Result.ok(modelService.create(request));
    }

    /**
     * 更新模型（IF §7.3，权限 {@code model:list:write}）。
     */
    @Operation(summary = "更新模型", description = "仅更新非空字段；vendorId 与 code 不可修改（换模型请新建）")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('model:list:write')")
    public Result<Void> update(@PathVariable("id") Long id,
                              @Valid @RequestBody ModelUpdateRequest request) {
        modelService.update(id, request);
        return Result.ok();
    }

    /**
     * 删除模型（IF §7.3，权限 {@code model:list:write}）。
     */
    @Operation(summary = "删除模型", description = "逻辑删除；已产生的用量记录不受影响")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('model:list:write')")
    public Result<Void> delete(@PathVariable("id") Long id) {
        modelService.delete(id);
        return Result.ok();
    }
}
