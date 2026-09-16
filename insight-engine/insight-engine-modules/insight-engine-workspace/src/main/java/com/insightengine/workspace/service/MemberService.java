package com.insightengine.workspace.service;

import com.insightengine.common.core.PageResult;
import com.insightengine.workspace.dto.request.MemberInviteRequest;
import com.insightengine.workspace.dto.request.MemberPageQuery;
import com.insightengine.workspace.dto.request.MemberRoleUpdateRequest;
import com.insightengine.workspace.dto.response.MemberVO;

/**
 * 空间成员服务（IF §5.6）。
 */
public interface MemberService {

    /**
     * 成员分页（含用户昵称/邮箱与角色名）。
     */
    PageResult<MemberVO> page(MemberPageQuery query);

    /**
     * 添加成员：把已注册用户加入空间并赋予角色。
     *
     * @return 新成员关系 ID
     */
    Long invite(MemberInviteRequest request);

    /**
     * 移除成员（逻辑删除成员关系）。
     *
     * @param operatorUserId 操作人（当前登录用户），用于禁止移除自己
     */
    void remove(Long id, Long operatorUserId);

    /**
     * 修改成员空间角色。
     *
     * @param operatorUserId 操作人（当前登录用户），用于禁止改动自己的角色
     */
    void updateRole(Long id, MemberRoleUpdateRequest request, Long operatorUserId);
}
