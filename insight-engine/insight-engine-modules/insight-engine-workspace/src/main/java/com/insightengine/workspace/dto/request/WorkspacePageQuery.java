package com.insightengine.workspace.dto.request;

import com.insightengine.common.core.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工作空间分页查询参数（IF §5.4）。
 *
 * <p>{@code orgId} 可选：不传时按当前登录用户可见范围查询（组织级管理员看组织内全部，
 * 其他用户看自己所属空间）。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WorkspacePageQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /** 组织 ID（可选） */
    private Long orgId;
}
