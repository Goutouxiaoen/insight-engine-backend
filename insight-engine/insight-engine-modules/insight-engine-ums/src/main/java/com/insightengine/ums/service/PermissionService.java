package com.insightengine.ums.service;

import com.insightengine.ums.dto.response.PermissionGroupVO;

import java.util.List;

/**
 * 权限服务接口（IF §6.3）。
 */
public interface PermissionService {

    /**
     * 权限树（按资源分组，供角色授权界面）。
     */
    List<PermissionGroupVO> tree();
}
