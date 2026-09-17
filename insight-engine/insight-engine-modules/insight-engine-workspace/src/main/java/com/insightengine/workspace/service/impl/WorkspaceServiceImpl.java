package com.insightengine.workspace.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.insightengine.common.constant.Constants;
import com.insightengine.common.core.BizException;
import com.insightengine.common.core.ErrorCode;
import com.insightengine.common.core.PageResult;
import com.insightengine.starter.redis.session.TokenSessionCache;
import com.insightengine.starter.security.token.AuthTokenIssuer;
import com.insightengine.starter.security.token.IssuedTokens;
import com.insightengine.starter.security.workspace.WorkspacePermissionCacheInvalidator;
import com.insightengine.starter.security.workspace.WorkspacePermissionChecker;
import com.insightengine.starter.web.context.UserContext;
import com.insightengine.workspace.dto.response.WorkspacePermissionVO;
import com.insightengine.workspace.support.TenantGuard;
import com.insightengine.workspace.constant.WorkspaceConstants;
import com.insightengine.workspace.dto.request.WorkspaceCreateRequest;
import com.insightengine.workspace.dto.request.WorkspacePageQuery;
import com.insightengine.workspace.dto.request.WorkspaceSwitchRequest;
import com.insightengine.workspace.dto.request.WorkspaceUpdateRequest;
import com.insightengine.workspace.dto.response.WorkspaceSwitchVO;
import com.insightengine.workspace.dto.response.WorkspaceVO;
import com.insightengine.workspace.entity.Member;
import com.insightengine.workspace.entity.Organization;
import com.insightengine.workspace.entity.Role;
import com.insightengine.workspace.entity.Workspace;
import com.insightengine.workspace.mapper.MemberMapper;
import com.insightengine.workspace.mapper.OrganizationMapper;
import com.insightengine.workspace.mapper.RoleMapper;
import com.insightengine.workspace.mapper.WorkspaceMapper;
import com.insightengine.workspace.service.WorkspaceService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * 工作空间服务实现。
 *
 * <p>关键设计：</p>
 * <ul>
 *   <li><b>可见范围收敛</b>（{@link #page}）：组织级管理员及以上（持 {@code org:write}）可见组织内
 *       全部空间；其他用户仅可见自己所属空间（按 {@code ie_member} 反查），避免越权看到他人空间；</li>
 *   <li><b>切换空间即换签</b>（{@link #switchWorkspace}）：切换范围由「成员关系」在服务端强约束
 *       （PROGRESS §三 2026-09-09 裁决：不新增 {@code ws:switch} 权限码）；
 *       **只改 {@code ws_id}（当前上下文），{@code roles}/{@code perms} 按用户维度取全量、与登录一致**
 *       （2026-09-17 修正 BE-20260916-01：早期"按目标空间重展开"会丢掉组织级/平台级能力，属错误口径）；</li>
 *   <li><b>会话一致</b>：换签后经 {@link TokenSessionCache} 覆盖 Redis 登录态摘要与 refresh 会话，
 *       使旧 access token 立即失效、新 token 在 UMS 与其他服务同样有效；令牌本身经
 *       {@link AuthTokenIssuer} 统一签发（与 UMS 登录/刷新同一实现，防口径漂移）。</li>
 * </ul>
 */
@Service
public class WorkspaceServiceImpl implements WorkspaceService {

    private final WorkspaceMapper workspaceMapper;
    private final OrganizationMapper organizationMapper;
    private final MemberMapper memberMapper;
    private final RoleMapper roleMapper;
    private final AuthTokenIssuer authTokenIssuer;
    private final TokenSessionCache tokenSessionCache;

    /** 空间维度权限查询（第二层判定 / my-permissions）；依赖接口而非实现类（code review 收口） */
    private final WorkspacePermissionChecker permissionChecker;

    /** 空间维度权限缓存失效（撤销类操作：须在数据库变更**之前**调用，见实现说明） */
    private final WorkspacePermissionCacheInvalidator permissionCache;

    public WorkspaceServiceImpl(WorkspaceMapper workspaceMapper,
                               OrganizationMapper organizationMapper,
                               MemberMapper memberMapper,
                               RoleMapper roleMapper,
                               AuthTokenIssuer authTokenIssuer,
                               TokenSessionCache tokenSessionCache,
                               WorkspacePermissionChecker permissionChecker,
                               WorkspacePermissionCacheInvalidator permissionCache) {
        this.workspaceMapper = workspaceMapper;
        this.organizationMapper = organizationMapper;
        this.memberMapper = memberMapper;
        this.roleMapper = roleMapper;
        this.authTokenIssuer = authTokenIssuer;
        this.tokenSessionCache = tokenSessionCache;
        this.permissionChecker = permissionChecker;
        this.permissionCache = permissionCache;
    }

    /**
     * 创建工作空间：组织存在 + 编码组织内唯一 → 落库 → 创建者挂 {@code ws_admin} 成员。
     *
     * <p>为什么创建者要自动成为成员：切换空间与空间可见范围都以成员关系为准，若创建者不落成员关系，
     * 新建空间将立即「自己都切不过去、列表里也看不到」，与 PRD §12.1.4「工作空间创建 P0」语义不符。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(WorkspaceCreateRequest request) {
        Long tenantId = currentTenantId();
        Long orgId = request.getOrgId();

        Organization org = organizationMapper.selectById(orgId);
        if (org == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "组织不存在");
        }

        String code = request.getCode().trim();
        // 编码唯一性：先查友好提示，DB 唯一索引（uk_ws_code_org）兜底防并发
        Long existCount = workspaceMapper.selectCount(new LambdaQueryWrapper<Workspace>()
                .eq(Workspace::getOrgId, orgId)
                .eq(Workspace::getCode, code));
        if (existCount != null && existCount > 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "该空间编码已存在");
        }

        Workspace workspace = new Workspace();
        workspace.setTenantId(tenantId);
        workspace.setOrgId(orgId);
        workspace.setName(request.getName().trim());
        workspace.setCode(code);
        // 配额未传时交由 DB 默认值兜底（max_apps=10 / max_kb_size_mb=1024）
        workspace.setMaxApps(request.getMaxApps());
        workspace.setMaxKbSizeMb(request.getMaxKbSizeMb());
        workspace.setStatus(Constants.STATUS_ENABLED);
        workspaceMapper.insert(workspace);

        Member member = new Member();
        member.setTenantId(tenantId);
        member.setOrgId(orgId);
        member.setWorkspaceId(workspace.getId());
        member.setUserId(UserContext.getUserId());
        member.setRoleId(requireRoleIdByCode(WorkspaceConstants.ROLE_WS_ADMIN));
        member.setJoinedAt(LocalDateTime.now(ZoneOffset.UTC));
        memberMapper.insert(member);

        return workspace.getId();
    }

    /**
     * 更新工作空间（仅更新传入的非空字段）。
     */
    @Override
    public void update(Long id, WorkspaceUpdateRequest request) {
        requireWorkspaceInTenant(id);
        Workspace update = new Workspace();
        update.setId(id);
        if (StringUtils.hasText(request.getName())) {
            update.setName(request.getName().trim());
        }
        if (request.getMaxApps() != null) {
            update.setMaxApps(request.getMaxApps());
        }
        if (request.getMaxKbSizeMb() != null) {
            update.setMaxKbSizeMb(request.getMaxKbSizeMb());
        }
        workspaceMapper.updateById(update);
    }

    /**
     * 删除工作空间：逻辑删除空间 + 其全部成员关系（前端确认文案「删除后成员将同时移出」）。
     *
     * <p>禁止删除「当前登录所处的空间」：否则当前令牌的 {@code ws_id} 会指向一个已删除空间，
     * 后续所有空间级操作都落在无主上下文上（提示用户先切换到其他空间）。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireWorkspaceInTenant(id);
        if (id.equals(UserContext.getWorkspaceId())) {
            throw new BizException(ErrorCode.OPERATION_NOT_ALLOWED, "不允许删除当前所处的工作空间，请先切换到其他空间");
        }
        // 撤销类操作：先失效权限缓存、再改库（失效失败则整体失败，不留"库里没了、缓存还在"的窗口，Y3）
        permissionCache.evictWorkspace(id);
        workspaceMapper.deleteById(id);
        memberMapper.delete(new LambdaQueryWrapper<Member>().eq(Member::getWorkspaceId, id));
    }

    /**
     * 「我在该空间的权限」（IF §5.7）：供前端按钮门控使用，与后端第二层判定同源。
     *
     * <p>非该空间成员返回 403/2006（不泄露"该空间的权限清单"给非成员）。</p>
     */
    @Override
    public WorkspacePermissionVO myPermissions(Long userId, Long workspaceId) {
        requireWorkspaceInTenant(workspaceId);
        // 角色与权限取自**同一份快照**（2026-09-17 code review：此前角色实时查库、权限读缓存，同一响应可能自相矛盾）
        List<String> roles = permissionChecker.rolesOf(userId, workspaceId);
        List<String> permissions = permissionChecker.permissionsOf(userId, workspaceId);
        if (roles.isEmpty() && permissions.isEmpty()) {
            throw new BizException(ErrorCode.FORBIDDEN, "您不是该工作空间的成员");
        }
        WorkspacePermissionVO vo = new WorkspacePermissionVO();
        vo.setWorkspaceId(workspaceId);
        vo.setRoles(roles);
        vo.setPermissions(permissions);
        return vo;
    }

    /**
     * 工作空间分页：按组织过滤 + 按可见范围收敛。
     */
    @Override
    public PageResult<WorkspaceVO> page(WorkspacePageQuery query) {
        int pageNum = query.getSafePageNum();
        int pageSize = query.getSafePageSize();

        LambdaQueryWrapper<Workspace> wrapper = new LambdaQueryWrapper<>();
        if (query.getOrgId() != null) {
            wrapper.eq(Workspace::getOrgId, query.getOrgId());
        } else {
            wrapper.eq(Workspace::getTenantId, currentTenantId());
        }

        if (!isOrgManager()) {
            List<Long> visibleIds = workspaceMapper.selectWorkspaceIdsByUserId(UserContext.getUserId());
            if (visibleIds.isEmpty()) {
                // 非组织管理员且无任何成员关系：直接返回空页，避免 IN () 语法为空导致全量泄露
                return PageResult.empty(pageNum, pageSize);
            }
            wrapper.in(Workspace::getId, visibleIds);
        }
        wrapper.orderByAsc(Workspace::getId);

        Page<Workspace> page = workspaceMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<WorkspaceVO> records = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(records, page.getTotal(), pageNum, pageSize);
    }

    /**
     * 切换当前工作空间：成员关系校验 → 带上用户全量角色/权限 + 目标空间 ws_id 重签 → 覆盖会话。
     */
    @Override
    public WorkspaceSwitchVO switchWorkspace(Long userId, WorkspaceSwitchRequest request) {
        Long workspaceId = request.getWorkspaceId();
        Workspace workspace = workspaceMapper.selectById(workspaceId);
        if (workspace == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "工作空间不存在");
        }
        if (workspace.getStatus() != null && workspace.getStatus() == Constants.STATUS_DISABLED) {
            throw new BizException(ErrorCode.OPERATION_NOT_ALLOWED, "工作空间已停用");
        }

        // 成员关系强约束：只能切换到自己所属空间（越权切换返回 2006 无权限）
        Member member = memberMapper.selectOne(new LambdaQueryWrapper<Member>()
                .eq(Member::getUserId, userId)
                .eq(Member::getWorkspaceId, workspaceId)
                .orderByAsc(Member::getId)
                .last("LIMIT 1"));
        if (member == null) {
            throw new BizException(ErrorCode.FORBIDDEN, "您不是该工作空间的成员");
        }

        // 角色/权限按「用户」维度取（与 UMS 登录完全同口径），**不按目标空间过滤**（2026-09-17 修正，BE-20260916-01）：
        // 切换空间只是换"当前站在哪个空间"(ws_id)，不是换"我是谁"；按空间过滤会丢掉组织级/平台级能力
        // （org:*、ws:create、ws:delete），表现为"一切空间就降级"（删除按钮消失、1003 变 2006）。
        // 空间维度的"能不能做"由服务端按当前 ws_id + 成员关系二次判定（TD §7.5 / PROGRESS §6.3 待办）。
        List<String> roles = roleMapper.selectRoleCodesByUserId(userId);
        List<String> permissions = roleMapper.selectPermissionCodesByUserId(userId);

        // 令牌经唯一入口签发（与 UMS 登录/刷新同一实现）：access 带新 ws_id，refresh 也带新 ws_id
        // （这样下次刷新会沿用该空间，不会被重置回默认空间）
        IssuedTokens tokens = authTokenIssuer.issue(
                userId, member.getTenantId(), workspaceId, roles, permissions);
        // 会话落地（唯一实现）：覆盖登录态摘要 + refresh 会话 → 切换前的旧 access token 立即失效
        tokenSessionCache.save(userId, tokens.accessToken(), tokens.refreshJti(),
                tokens.accessTtlSeconds(), tokens.refreshTtlSeconds());

        WorkspaceSwitchVO vo = new WorkspaceSwitchVO();
        vo.setToken(tokens.accessToken());
        vo.setRefreshToken(tokens.refreshToken());
        vo.setExpiresIn(tokens.accessTtlSeconds());
        return vo;
    }

    /* ==================== 私有方法 ==================== */

    /**
     * 当前用户是否组织级管理员及以上（持 {@code org:write}）：决定空间列表是否收敛到本人所属空间。
     */
    private boolean isOrgManager() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(WorkspaceConstants.PERM_ORG_WRITE::equals);
    }

    /**
     * 查询工作空间并**校验租户归属**（2026-09-17 code review Y2 收口）：
     * 跨租户的 id 一律按"不存在"处理（1004，不泄露存在性）。
     *
     * <p>MVP 单租户下不暴露；多租户/多组织前必须收口，否则组织级用户拿到别租户的空间 id 即可改/删。</p>
     */
    private Workspace requireWorkspaceInTenant(Long id) {
        Workspace workspace = requireWorkspace(id);
        TenantGuard.assertSameTenant(workspace.getTenantId(), "工作空间");
        return workspace;
    }

    /**
     * 查询工作空间，不存在抛 1004。
     */
    private Workspace requireWorkspace(Long id) {
        Workspace workspace = workspaceMapper.selectById(id);
        if (workspace == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "工作空间不存在");
        }
        return workspace;
    }

    /**
     * 按编码解析内置角色 ID；种子数据缺失时抛系统异常，不静默降级。
     */
    private Long requireRoleIdByCode(String roleCode) {
        Role role = roleMapper.selectOne(new LambdaQueryWrapper<Role>().eq(Role::getCode, roleCode));
        if (role == null) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "内置角色配置缺失：" + roleCode);
        }
        return role.getId();
    }

    /**
     * 当前租户 ID：取自 JWT 载荷；缺失时兜底 MVP 单租户默认值。
     */
    private Long currentTenantId() {
        Long tenantId = UserContext.getTenantId();
        return tenantId == null ? WorkspaceConstants.DEFAULT_TENANT_ID : tenantId;
    }

    /**
     * 实体转列表项 VO。
     */
    private WorkspaceVO toVO(Workspace workspace) {
        WorkspaceVO vo = new WorkspaceVO();
        vo.setId(workspace.getId());
        vo.setOrgId(workspace.getOrgId());
        vo.setName(workspace.getName());
        vo.setCode(workspace.getCode());
        vo.setMaxApps(workspace.getMaxApps());
        vo.setMaxKbSizeMb(workspace.getMaxKbSizeMb());
        vo.setStatus(workspace.getStatus());
        vo.setCreatedAt(workspace.getCreatedAt());
        return vo;
    }
}
