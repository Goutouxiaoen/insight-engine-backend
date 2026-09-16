package com.insightengine.workspace.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 修改成员空间角色请求体（IF §5.6 {@code PUT /api/v1/member/{id}/role}）。
 */
@Data
public class MemberRoleUpdateRequest {

    /** 新角色 ID */
    @NotNull(message = "角色不能为空")
    private Long roleId;
}
