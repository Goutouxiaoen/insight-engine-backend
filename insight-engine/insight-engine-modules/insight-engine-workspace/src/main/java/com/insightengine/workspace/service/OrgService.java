package com.insightengine.workspace.service;

import com.insightengine.workspace.dto.request.OrgCreateRequest;
import com.insightengine.workspace.dto.response.OrgVO;

/**
 * 组织服务（IF §5.1 / §5.2）。
 */
public interface OrgService {

    /**
     * 创建组织，创建者即所有者。
     *
     * @return 新组织 ID
     */
    Long create(OrgCreateRequest request);

    /**
     * 组织详情。
     *
     * @param id 组织 ID
     */
    OrgVO detail(Long id);
}
