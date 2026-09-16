package com.insightengine.workspace.dto.response;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工作空间列表项（IF §5.4）。
 *
 * <p>字段与前端 {@code WorkspaceItem} 类型一一对应（orgId / name / code / maxApps /
 * maxKbSizeMb / status / createdAt）。</p>
 */
@Data
public class WorkspaceVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工作空间 ID */
    private Long id;

    /** 所属组织 ID */
    private Long orgId;

    /** 空间名称 */
    private String name;

    /** 空间编码 */
    private String code;

    /** 最大应用数上限 */
    private Integer maxApps;

    /** 知识库容量上限（MB） */
    private Integer maxKbSizeMb;

    /** 状态：1 正常 / 0 停用 */
    private Integer status;

    /** 创建时间（ISO-8601 UTC，IF §2.5） */
    private LocalDateTime createdAt;
}
