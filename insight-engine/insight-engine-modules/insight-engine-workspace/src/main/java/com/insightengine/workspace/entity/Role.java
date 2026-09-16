package com.insightengine.workspace.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 角色实体（只读引用），对应表 {@code ie_role}（DB.md §5.2.1）。
 *
 * <p>说明：角色的「写」职责属于 UMS（IF §6 角色与权限接口）；Workspace 侧只在
 * 「添加成员 / 修改成员角色」时校验 {@code roleId} 存在性并按编码解析内置角色
 * （如创建空间后把创建者挂为 {@code ws_admin}），故此处仅定义最小字段集。</p>
 */
@Data
@TableName("ie_role")
public class Role {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户 ID（0 表示全局内置角色） */
    private Long tenantId;

    /** 角色编码（如 ws_admin） */
    private String code;

    /** 角色名称 */
    private String name;

    /** 数据范围：ALL / ORG / WS / SELF */
    private String scope;

    /** 是否内置：1 内置（禁删） / 0 自定义 */
    private Integer builtin;

    /** 逻辑删除标记：0 正常 / 1 已删除 */
    private Integer deleted;
}
