package com.insightengine.model.service;

import com.insightengine.common.core.PageResult;
import com.insightengine.model.dto.request.UsagePageQuery;
import com.insightengine.model.dto.response.UsageRecordVO;

/**
 * 模型用量查询服务（IF §7.8）。
 *
 * <p><b>可见范围</b>（重要，见实现说明）：`model:usage:read` 权限**空间管理员也持有**，
 * 因此不能"有权限就看全平台"——实现按调用者是否为组织级（持 `org:write`）区分：
 * 组织级看全量；否则**强制限定在自己当前空间**。</p>
 */
public interface UsageService {

    /**
     * 用量分页（模型/时间范围过滤 + 可见范围收敛）。
     */
    PageResult<UsageRecordVO> page(UsagePageQuery query);
}
