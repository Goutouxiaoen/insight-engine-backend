package com.insightengine.workspace.dto.response;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 空间成员列表项（IF §5.6）。
 *
 * <p>{@code nickname} / {@code email} / {@code roleName} 来自联表查询（{@code ie_user} /
 * {@code ie_role}），前端「成员」抽屉直接展示，无需二次请求。</p>
 */
@Data
public class MemberVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 成员关系 ID（移除 / 改角色接口的路径参数） */
    private Long id;

    /** 所属工作空间 ID */
    private Long workspaceId;

    /** 用户 ID */
    private Long userId;

    /** 用户昵称 */
    private String nickname;

    /** 用户邮箱 */
    private String email;

    /** 空间内角色 ID */
    private Long roleId;

    /** 空间内角色名称 */
    private String roleName;

    /** 加入时间（ISO-8601 UTC，IF §2.5） */
    private LocalDateTime joinedAt;
}
