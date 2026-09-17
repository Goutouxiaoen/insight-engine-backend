package com.insightengine.workspace.dto.response;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 「我在某个空间的权限」视图对象（IF §5.7）。
 *
 * <p>为什么需要这个接口：token 里的 {@code perms} 是"用户跨空间的能力并集"，
 * 只能回答"我这类动作能不能做"，**回答不了"在这个空间我能不能做"**。
 * 前端如果只用 token 做按钮门控，就会出现「在 A 空间有权限、切到 B 空间按钮还在、点下去 403」。
 * 因此按钮门控必须用本接口返回的**当前空间权限**，做到"显示与后端判定同源"。</p>
 */
@Data
public class WorkspacePermissionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工作空间 ID */
    private Long workspaceId;

    /** 我在该空间内的角色编码（如 ws_admin / end_user；非成员时为空集合） */
    private List<String> roles;

    /** 我在该空间内的权限编码（第二层判定的判据，非成员时为空集合） */
    private List<String> permissions;
}
