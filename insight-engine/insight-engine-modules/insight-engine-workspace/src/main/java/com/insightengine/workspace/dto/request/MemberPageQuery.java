package com.insightengine.workspace.dto.request;

import com.insightengine.common.core.PageQuery;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 空间成员分页查询参数（IF §5.6）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MemberPageQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /** 工作空间 ID */
    @NotNull(message = "工作空间不能为空")
    private Long workspaceId;
}
