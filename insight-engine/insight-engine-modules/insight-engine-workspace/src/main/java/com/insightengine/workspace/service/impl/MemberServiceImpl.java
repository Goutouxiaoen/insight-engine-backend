package com.insightengine.workspace.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.insightengine.common.core.BizException;
import com.insightengine.common.core.ErrorCode;
import com.insightengine.common.core.PageResult;
import com.insightengine.workspace.dto.request.MemberInviteRequest;
import com.insightengine.workspace.dto.request.MemberPageQuery;
import com.insightengine.workspace.dto.request.MemberRoleUpdateRequest;
import com.insightengine.workspace.dto.response.MemberVO;
import com.insightengine.workspace.entity.Member;
import com.insightengine.workspace.entity.Workspace;
import com.insightengine.workspace.mapper.MemberMapper;
import com.insightengine.workspace.mapper.RoleMapper;
import com.insightengine.workspace.mapper.UserRefMapper;
import com.insightengine.workspace.mapper.WorkspaceMapper;
import com.insightengine.workspace.service.MemberService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 空间成员服务实现。
 *
 * <p>安全要点：</p>
 * <ul>
 *   <li>添加成员前校验「用户已注册」与「角色存在」，防孤儿 member（PROGRESS §6.1 高价值收尾项同源问题）；</li>
 *   <li>禁止移除自己 / 修改自己的空间角色，避免管理员误操作导致空间失管或自我提权。</li>
 * </ul>
 */
@Service
public class MemberServiceImpl implements MemberService {

    private final MemberMapper memberMapper;
    private final WorkspaceMapper workspaceMapper;
    private final RoleMapper roleMapper;
    private final UserRefMapper userRefMapper;

    public MemberServiceImpl(MemberMapper memberMapper,
                             WorkspaceMapper workspaceMapper,
                             RoleMapper roleMapper,
                             UserRefMapper userRefMapper) {
        this.memberMapper = memberMapper;
        this.workspaceMapper = workspaceMapper;
        this.roleMapper = roleMapper;
        this.userRefMapper = userRefMapper;
    }

    /**
     * 成员分页（联表查询用户昵称/邮箱与角色名）。
     */
    @Override
    public PageResult<MemberVO> page(MemberPageQuery query) {
        requireWorkspace(query.getWorkspaceId());
        int pageNum = query.getSafePageNum();
        int pageSize = query.getSafePageSize();

        IPage<MemberVO> page = memberMapper.selectMemberPage(
                new Page<>(pageNum, pageSize), query.getWorkspaceId());
        return PageResult.of(page.getRecords(), page.getTotal(), pageNum, pageSize);
    }

    /**
     * 添加成员：空间存在 → 邮箱对应用户已注册 → 非重复成员 → 角色存在 → 落成员关系。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long invite(MemberInviteRequest request) {
        Workspace workspace = requireWorkspace(request.getWorkspaceId());

        String email = request.getEmail().trim();
        Long userId = userRefMapper.selectIdByEmail(email);
        if (userId == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "该邮箱未注册平台账号");
        }

        Long existCount = memberMapper.selectCount(new LambdaQueryWrapper<Member>()
                .eq(Member::getUserId, userId)
                .eq(Member::getWorkspaceId, request.getWorkspaceId()));
        if (existCount != null && existCount > 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "该用户已是空间成员");
        }

        if (roleMapper.selectById(request.getRoleId()) == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "角色不存在");
        }

        Member member = new Member();
        member.setTenantId(workspace.getTenantId());
        member.setOrgId(workspace.getOrgId());
        member.setWorkspaceId(workspace.getId());
        member.setUserId(userId);
        member.setRoleId(request.getRoleId());
        member.setJoinedAt(LocalDateTime.now(ZoneOffset.UTC));
        memberMapper.insert(member);
        return member.getId();
    }

    /**
     * 移除成员：成员存在 + 非本人 → 逻辑删除成员关系。
     */
    @Override
    public void remove(Long id, Long operatorUserId) {
        Member member = requireMember(id);
        if (member.getUserId().equals(operatorUserId)) {
            throw new BizException(ErrorCode.OPERATION_NOT_ALLOWED, "不允许移除自己");
        }
        memberMapper.deleteById(id);
    }

    /**
     * 修改成员空间角色：成员存在 + 非本人 + 角色存在 → 更新 role_id。
     */
    @Override
    public void updateRole(Long id, MemberRoleUpdateRequest request, Long operatorUserId) {
        Member member = requireMember(id);
        if (member.getUserId().equals(operatorUserId)) {
            throw new BizException(ErrorCode.OPERATION_NOT_ALLOWED, "不允许修改自己的空间角色");
        }
        if (roleMapper.selectById(request.getRoleId()) == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "角色不存在");
        }
        Member update = new Member();
        update.setId(id);
        update.setRoleId(request.getRoleId());
        memberMapper.updateById(update);
    }

    /* ==================== 私有方法 ==================== */

    /**
     * 查询工作空间，不存在抛 1004（成员列表/添加前先确认空间存在，避免对无效空间做操作）。
     */
    private Workspace requireWorkspace(Long id) {
        Workspace workspace = workspaceMapper.selectById(id);
        if (workspace == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "工作空间不存在");
        }
        return workspace;
    }

    /**
     * 查询成员关系，不存在抛 1004。
     */
    private Member requireMember(Long id) {
        Member member = memberMapper.selectById(id);
        if (member == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "成员不存在");
        }
        return member;
    }
}
