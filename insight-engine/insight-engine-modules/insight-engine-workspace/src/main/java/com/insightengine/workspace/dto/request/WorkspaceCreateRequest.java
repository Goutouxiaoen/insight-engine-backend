package com.insightengine.workspace.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建工作空间请求体（IF §5.3）。
 *
 * <p>配额字段（{@code maxApps} / {@code maxKbSizeMb}）可选，未传时由 DB 默认值兜底
 * （{@code 10} / {@code 1024}）。</p>
 */
@Data
public class WorkspaceCreateRequest {

    /** 所属组织 ID */
    @NotNull(message = "组织不能为空")
    private Long orgId;

    /** 工作空间名称 */
    @NotBlank(message = "空间名称不能为空")
    @Size(max = 128, message = "空间名称长度不能超过 128 个字符")
    private String name;

    /** 空间编码（同组织内唯一；创建后不可修改，前端一致） */
    @NotBlank(message = "空间编码不能为空")
    @Pattern(regexp = "^[a-z][a-z0-9-]{1,63}$", message = "编码须以小写字母开头，2~64 位字母/数字/中划线")
    private String code;

    /** 最大应用数上限（可选，上限 1000） */
    @Min(value = 1, message = "应用上限至少为 1")
    @Max(value = 1000, message = "应用上限不能超过 1000")
    private Integer maxApps;

    /** 知识库容量上限 MB（可选，上限 1048576 = 1TB） */
    @Min(value = 1, message = "知识库容量至少为 1 MB")
    @Max(value = 1048576, message = "知识库容量不能超过 1048576 MB")
    private Integer maxKbSizeMb;
}
