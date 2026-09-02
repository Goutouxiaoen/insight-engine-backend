package com.insightengine.ums.service;

import com.insightengine.ums.dto.request.RoleCreateRequest;
import com.insightengine.ums.dto.request.RolePermissionUpdateRequest;
import com.insightengine.ums.dto.response.RoleVO;

import java.util.List;

/**
 * 角色服务接口（IF §6）。
 *
 * <p>覆盖角色列表/创建/详情/删除/授权五个接口。</p>
 */
public interface RoleService {

    /**
     * 角色列表（IF §6.1，含内置角色）。
     */
    List<RoleVO> list();

    /**
     * 创建角色（IF §6.2，可同时授权）。
     *
     * @return 新角色 ID
     */
    Long create(RoleCreateRequest request);

    /**
     * 角色详情（IF §6.5，含权限 ID 列表）。
     */
    RoleVO detail(Long id);

    /**
     * 删除角色（IF §6.6）：内置角色禁止删除，返回 1003。
     */
    void delete(Long id);

    /**
     * 角色授权（IF §6.4）：先删后插，完整覆盖权限集合。
     */
    void assignPermissions(Long id, RolePermissionUpdateRequest request);
}
