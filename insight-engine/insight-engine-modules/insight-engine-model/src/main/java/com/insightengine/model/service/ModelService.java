package com.insightengine.model.service;

import com.insightengine.common.core.PageResult;
import com.insightengine.model.dto.request.ModelCreateRequest;
import com.insightengine.model.dto.request.ModelPageQuery;
import com.insightengine.model.dto.request.ModelUpdateRequest;
import com.insightengine.model.dto.response.ModelVO;

/**
 * 模型目录服务（IF §7.2 / §7.3）。
 *
 * <p>模型挂在厂商下；"平台级目录、所有业务方共用"的口径同厂商（PROGRESS §三 2026-09-17 裁决）。</p>
 */
public interface ModelService {

    /**
     * 模型分页（厂商/类型/启停/关键字过滤；返回含厂商编码与名称）。
     */
    PageResult<ModelVO> page(ModelPageQuery query);

    /**
     * 创建模型（厂商必须存在；{@code (vendorId, code)} 唯一）。
     *
     * @return 新模型 ID
     */
    Long create(ModelCreateRequest request);

    /**
     * 更新模型（仅非空字段；厂商与编码不可改）。
     */
    void update(Long id, ModelUpdateRequest request);

    /**
     * 删除模型（逻辑删除）。
     */
    void delete(Long id);
}
