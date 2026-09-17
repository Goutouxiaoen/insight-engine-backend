package com.insightengine.model.dto.request;

import com.insightengine.common.core.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 模型分页查询参数（IF §7.2）。
 *
 * <p>条件可选：{@code vendorId} / {@code type} / {@code enabled} / {@code keyword}（模糊匹配模型编码或展示名）。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ModelPageQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /** 厂商 ID */
    private Long vendorId;

    /** 能力类型：CHAT / EMBEDDING / RERANK */
    private String type;

    /** 是否启用：1 启用 / 0 停用 */
    private Integer enabled;

    /** 关键字（匹配模型编码或展示名） */
    private String keyword;
}
