package com.insightengine.ums.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 角色授权请求体（IF §6.4）。
 *
 * <p>{@code permissionIds} 为完整的目标权限集合（非增量），服务端采用「先删后插」语义，
 * 保证结果与请求一致、幂等可重试。</p>
 */
@Data
public class RolePermissionUpdateRequest {

    /** 目标权限 ID 列表（完整覆盖） */
    @NotNull(message = "权限列表不能为空")
    private List<Long> permissionIds;
}
