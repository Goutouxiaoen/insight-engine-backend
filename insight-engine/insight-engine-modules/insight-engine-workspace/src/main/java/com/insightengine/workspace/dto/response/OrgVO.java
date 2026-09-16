package com.insightengine.workspace.dto.response;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 组织信息（IF §5.1 创建响应 / §5.2 详情）。
 */
@Data
public class OrgVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 组织 ID */
    private Long id;

    /** 租户 ID */
    private Long tenantId;

    /** 组织名称 */
    private String name;

    /** 组织编码 */
    private String code;

    /** 所有者 user_id */
    private Long ownerId;

    /** 状态：1 正常 / 0 禁用 */
    private Integer status;

    /** 创建时间（ISO-8601 UTC，IF §2.5） */
    private LocalDateTime createdAt;
}
