package com.insightengine.workspace.controller;

import com.insightengine.common.annotation.WorkspacePermission;
import com.insightengine.common.core.PageResult;
import com.insightengine.common.core.Result;
import com.insightengine.starter.web.context.UserContext;
import com.insightengine.workspace.dto.request.MemberInviteRequest;
import com.insightengine.workspace.dto.request.MemberPageQuery;
import com.insightengine.workspace.dto.request.MemberRoleUpdateRequest;
import com.insightengine.workspace.dto.response.MemberVO;
import com.insightengine.workspace.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 空间成员管理接口（IF §5.6）。
 *
 * <p>统一前缀 {@code /api/v1/member}；操作人（当前登录用户）从认证上下文读取，用于
 * 「禁止移除自己 / 修改自己的角色」等操作保护。</p>
 *
 * <p><b>两层鉴权</b>（IF §3.0）：{@code @PreAuthorize} 管"动作类别"（判据=token 的 perms，跨空间并集）；
 * {@code @WorkspacePermission} 管"在这个空间能不能做"（判据=该空间的成员关系）。
 * 「移除成员 / 改角色」的目标空间需先按 {@code memberId} 反查成员记录才知道，注解取不到，
 * 故这两处由 {@code MemberServiceImpl} 内显式调用判定器（检查点可见、易审计）。</p>
 */
@Tag(name = "空间成员管理", description = "成员分页、添加成员、移除成员、修改成员角色")
@RestController
@RequestMapping("/api/v1/member")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    /**
     * 成员分页（IF §5.6，权限 member:read）。
     */
    @Operation(summary = "成员分页", description = "返回成员昵称/邮箱/空间角色/加入时间")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('member:read')")
    @WorkspacePermission(value = "member:read", workspaceIdExpr = "#query.workspaceId")
    public Result<PageResult<MemberVO>> page(@Valid MemberPageQuery query) {
        return Result.ok(memberService.page(query));
    }

    /**
     * 添加成员（IF §5.6，权限 member:create）。
     */
    @Operation(summary = "添加成员", description = "邮箱须为已注册用户；重复加入返回 1001")
    @PostMapping("/invite")
    @PreAuthorize("hasAuthority('member:create')")
    @WorkspacePermission(value = "member:create", workspaceIdExpr = "#request.workspaceId")
    public Result<Long> invite(@Valid @RequestBody MemberInviteRequest request) {
        return Result.ok(memberService.invite(request));
    }

    /**
     * 移除成员（IF §5.6，权限 member:delete）。
     */
    @Operation(summary = "移除成员", description = "逻辑删除成员关系；不允许移除自己")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('member:delete')")
    public Result<Void> remove(@PathVariable("id") Long id) {
        memberService.remove(id, UserContext.getUserId());
        return Result.ok();
    }

    /**
     * 修改成员空间角色（IF §5.6，权限 member:update）。
     */
    @Operation(summary = "修改成员空间角色", description = "不允许修改自己的空间角色")
    @PutMapping("/{id}/role")
    @PreAuthorize("hasAuthority('member:update')")
    public Result<Void> updateRole(@PathVariable("id") Long id,
                                   @Valid @RequestBody MemberRoleUpdateRequest request) {
        memberService.updateRole(id, request, UserContext.getUserId());
        return Result.ok();
    }
}
