package com.insightengine.model.service;

import com.insightengine.common.core.PageResult;
import com.insightengine.model.dto.request.VendorCreateRequest;
import com.insightengine.model.dto.request.VendorPageQuery;
import com.insightengine.model.dto.request.VendorUpdateRequest;
import com.insightengine.model.dto.response.VendorVO;

/**
 * 模型厂商服务（IF §7.1）。
 *
 * <p>厂商 = 模型服务的接入端点，是"平台能调用哪些大模型"的入口。写入时 {@code apiKey} 加密落
 * {@code ie_secret}；读取一律只回掩码（IF §7.1 安全约束）。</p>
 */
public interface ModelVendorService {

    /**
     * 厂商分页（编码/名称模糊 + 类型/启停过滤）。
     */
    PageResult<VendorVO> page(VendorPageQuery query);

    /**
     * 创建厂商（编码唯一；带 {@code apiKey} 时加密落密文库）。
     *
     * @return 新厂商 ID
     */
    Long create(VendorCreateRequest request);

    /**
     * 更新厂商（{@code apiKey} 留空表示不修改密钥）。
     */
    void update(Long id, VendorUpdateRequest request);

    /**
     * 删除厂商（逻辑删除；密钥记录保留以留审计痕迹）。
     *
     * <p>注意：本接口**不级联**删除该厂商下的模型（{@code ie_model}）与路由策略引用；
     * 是否允许删除"仍被模型引用的厂商"由实现按业务规则决定（当前实现：存在启用中的模型时拒绝，见实现说明）。</p>
     */
    void delete(Long id);
}
