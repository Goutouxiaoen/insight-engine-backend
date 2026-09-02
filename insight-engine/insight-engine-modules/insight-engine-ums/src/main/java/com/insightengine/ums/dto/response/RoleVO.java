package com.insightengine.ums.dto.response;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 角色视图对象（IF §6.1 / §6.5）。
 *
 * <p>列表返回基础字段；详情额外携带 {@code permissionIds} 供授权界面回显。</p>
 */
@Data
public class RoleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 角色 ID */
    private Long id;

    /** 角色编码 */
    private String code;

    /** 角色名称 */
    private String name;

    /** 数据范围：ALL/ORG/WS/SELF */
    private String scope;

    /** 是否内置：1 内置（禁删）/ 0 自定义 */
    private Integer builtin;

    /** 角色描述 */
    private String description;

    /** 关联权限 ID 列表（详情时返回） */
    private List<Long> permissionIds;
}
