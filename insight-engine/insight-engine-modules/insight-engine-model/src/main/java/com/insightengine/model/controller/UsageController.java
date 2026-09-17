package com.insightengine.model.controller;

import com.insightengine.common.core.PageResult;
import com.insightengine.common.core.Result;
import com.insightengine.model.dto.request.UsagePageQuery;
import com.insightengine.model.dto.response.UsageRecordVO;
import com.insightengine.model.service.UsageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 模型用量查询接口（IF §7.8）。
 *
 * <p>统一前缀 {@code /api/v1/model/usage}；权限 {@code model:usage:read}。</p>
 *
 * <p><b>可见范围</b>：该权限 `ws_admin` 也持有，故服务端按调用者是否为组织级收敛范围
 * （组织级=全量；否则仅当前空间）——详见 {@code UsageServiceImpl} 类注释与 IF §7.8。</p>
 */
@Tag(name = "模型用量查询", description = "按模型/时间范围分页查询用量明细（可见范围按调用者组织级能力收敛）")
@RestController
@RequestMapping("/api/v1/model/usage")
public class UsageController {

    private final UsageService usageService;

    public UsageController(UsageService usageService) {
        this.usageService = usageService;
    }

    /**
     * 用量分页（IF §7.8，权限 {@code model:usage:read}）。
     */
    @Operation(summary = "模型用量分页",
            description = "modelId/start/end 可选（日期闭区间）；cost 为字符串或 null（套餐制无价目）；非组织级调用者只能看当前空间")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('model:usage:read')")
    public Result<PageResult<UsageRecordVO>> page(@Valid UsagePageQuery query) {
        return Result.ok(usageService.page(query));
    }
}
