package com.insightengine.workspace.service;

import com.insightengine.common.core.PageResult;
import com.insightengine.workspace.dto.request.WorkspaceCreateRequest;
import com.insightengine.workspace.dto.request.WorkspacePageQuery;
import com.insightengine.workspace.dto.request.WorkspaceSwitchRequest;
import com.insightengine.workspace.dto.request.WorkspaceUpdateRequest;
import com.insightengine.workspace.dto.response.WorkspaceSwitchVO;
import com.insightengine.workspace.dto.response.WorkspaceVO;

/**
 * 工作空间服务（IF §5.3 / §5.4 / §5.5）。
 */
public interface WorkspaceService {

    /**
     * 创建工作空间（创建者自动成为空间管理员成员）。
     *
     * @return 新空间 ID
     */
    Long create(WorkspaceCreateRequest request);

    /**
     * 更新工作空间（名称 / 配额；编码与组织不可改）。
     */
    void update(Long id, WorkspaceUpdateRequest request);

    /**
     * 删除工作空间（逻辑删除空间与其全部成员关系）。
     */
    void delete(Long id);

    /**
     * 工作空间分页（按可见范围收敛，见实现说明）。
     */
    PageResult<WorkspaceVO> page(WorkspacePageQuery query);

    /**
     * 切换当前工作空间：校验成员关系后重签令牌对（IF §5.5）。
     *
     * @param userId 当前登录用户 ID（取自认证上下文，不信任客户端传参）
     */
    WorkspaceSwitchVO switchWorkspace(Long userId, WorkspaceSwitchRequest request);
}
