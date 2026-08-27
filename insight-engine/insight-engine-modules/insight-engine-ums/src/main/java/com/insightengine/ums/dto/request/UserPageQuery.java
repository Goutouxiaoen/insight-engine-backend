package com.insightengine.ums.dto.request;

import com.insightengine.common.core.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户分页查询条件（IF §4.1）。
 *
 * <p>继承 {@link PageQuery} 复用分页字段（pageNum/pageSize），
 * 额外提供 {@code keyword} 按昵称/邮箱模糊检索。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserPageQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /** 昵称/邮箱模糊关键字 */
    private String keyword;
}
