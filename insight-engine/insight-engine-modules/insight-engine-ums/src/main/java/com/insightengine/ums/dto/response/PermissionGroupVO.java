package com.insightengine.ums.dto.response;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 权限树分组节点（IF §6.3）。
 *
 * <p>按 {@code resource} 字段把权限聚合为一组（如 kb / model:vendor），
 * 组内 {@code children} 为该资源下的具体权限点列表。</p>
 */
@Data
public class PermissionGroupVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 资源类型（如 kb / member / model:vendor） */
    private String resource;

    /** 资源显示名（如 知识库 / 成员 / 模型厂商） */
    private String name;

    /** 该资源下的权限点列表 */
    private List<PermissionNodeVO> children;
}
