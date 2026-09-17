package com.insightengine.model.dto.request;

import com.insightengine.common.core.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 厂商分页查询参数（IF §7.1）。
 *
 * <p>全部条件可选：{@code keyword} 模糊匹配编码/名称，{@code type} 按能力类型过滤，
 * {@code enabled} 按启停状态过滤。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class VendorPageQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /** 关键字（匹配厂商编码或名称） */
    private String keyword;

    /** 能力类型：CHAT / EMBEDDING / RERANK */
    private String type;

    /** 是否启用：1 启用 / 0 停用 */
    private Integer enabled;
}
