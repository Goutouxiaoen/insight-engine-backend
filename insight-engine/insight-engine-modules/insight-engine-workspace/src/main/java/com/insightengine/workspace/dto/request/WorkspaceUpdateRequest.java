package com.insightengine.workspace.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新工作空间请求体（IF §5.3）。
 *
 * <p>字段全部可选：仅更新传入的非空字段。{@code code} / {@code orgId} 不可修改
 * （编码是空间在组织内的稳定标识，前端「创建后不可修改」提示一致）。</p>
 */
@Data
public class WorkspaceUpdateRequest {

    /** 工作空间名称（可选） */
    @Size(max = 128, message = "空间名称长度不能超过 128 个字符")
    private String name;

    /** 最大应用数上限（可选） */
    @Min(value = 1, message = "应用上限至少为 1")
    @Max(value = 1000, message = "应用上限不能超过 1000")
    private Integer maxApps;

    /** 知识库容量上限 MB（可选） */
    @Min(value = 1, message = "知识库容量至少为 1 MB")
    @Max(value = 1048576, message = "知识库容量不能超过 1048576 MB")
    private Integer maxKbSizeMb;
}
