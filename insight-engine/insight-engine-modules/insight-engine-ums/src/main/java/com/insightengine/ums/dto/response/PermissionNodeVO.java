package com.insightengine.ums.dto.response;

import lombok.Data;

import java.io.Serializable;

/**
 * 权限树叶子节点（IF §6.3）。
 *
 * <p>权限按「资源 resource」分组后，组内每一项即一个具体权限点
 * （id + code + name），供前端角色授权界面勾选。</p>
 */
@Data
public class PermissionNodeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 权限 ID */
    private Long id;

    /** 权限编码（如 kb:read） */
    private String code;

    /** 权限名称（如 查看知识库） */
    private String name;
}
