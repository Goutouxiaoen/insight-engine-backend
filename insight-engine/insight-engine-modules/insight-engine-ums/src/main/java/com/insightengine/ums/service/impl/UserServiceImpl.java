package com.insightengine.ums.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.insightengine.common.core.BizException;
import com.insightengine.common.core.ErrorCode;
import com.insightengine.common.core.PageResult;
import com.insightengine.common.core.RoleGrantPolicy;
import com.insightengine.starter.web.context.UserContext;
import com.insightengine.ums.constant.AuthConstants;
import com.insightengine.ums.dto.request.PasswordUpdateRequest;
import com.insightengine.ums.dto.request.UserCreateRequest;
import com.insightengine.ums.dto.request.UserPageQuery;
import com.insightengine.ums.dto.request.UserStatusRequest;
import com.insightengine.ums.dto.request.UserUpdateRequest;
import com.insightengine.ums.dto.response.UserPageVO;
import com.insightengine.ums.entity.Member;
import com.insightengine.ums.entity.Role;
import com.insightengine.ums.entity.User;
import com.insightengine.ums.mapper.MemberMapper;
import com.insightengine.ums.mapper.RoleMapper;
import com.insightengine.ums.mapper.UserMapper;
import com.insightengine.ums.service.UserService;
import com.insightengine.starter.redis.session.TokenSessionCache;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * 用户管理服务实现。
 *
 * <p>改密/禁用后需让已签发 token 失效：通过删除登录态缓存实现「下次请求重新登录」。
 * 详见 {@link #updatePassword} / {@link #updateStatus}。</p>
 */
@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final MemberMapper memberMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenSessionCache tokenSessionCache;
    private final RoleMapper roleMapper;

    public UserServiceImpl(UserMapper userMapper,
                           MemberMapper memberMapper,
                           PasswordEncoder passwordEncoder,
                           TokenSessionCache tokenSessionCache,
                           RoleMapper roleMapper) {
        this.userMapper = userMapper;
        this.memberMapper = memberMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenSessionCache = tokenSessionCache;
        this.roleMapper = roleMapper;
    }

    /**
     * 用户分页：keyword 模糊匹配昵称/邮箱，手机号脱敏输出。
     */
    @Override
    public PageResult<UserPageVO> page(UserPageQuery query) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like(User::getNickname, keyword)
                    .or().like(User::getEmail, keyword));
        }
        wrapper.orderByDesc(User::getId);

        Page<User> page = userMapper.selectPage(
                new Page<>(query.getSafePageNum(), query.getSafePageSize()), wrapper);

        List<UserPageVO> records = page.getRecords().stream().map(this::toPageVO).toList();
        return PageResult.of(records, page.getTotal(), query.getSafePageNum(), query.getSafePageSize());
    }

    /**
     * 创建用户：邮箱唯一 → 密码加密 → 落库 + 挂角色。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(UserCreateRequest request) {
        String email = request.getEmail().trim();
        Long existCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (existCount != null && existCount > 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "该邮箱已被注册");
        }

        assertRoleGrantable(request.getRoleId());

        User user = new User();
        user.setTenantId(AuthConstants.DEFAULT_TENANT_ID);
        user.setEmail(email);
        user.setNickname(request.getNickname().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setPhone(trimToNull(request.getPhone()));
        user.setStatus(AuthConstants.ACCOUNT_NORMAL);
        userMapper.insert(user);

        // 挂成员关系：默认组织 + 默认工作空间 + 指定角色
        Member member = new Member();
        member.setTenantId(AuthConstants.DEFAULT_TENANT_ID);
        member.setOrgId(AuthConstants.DEFAULT_ORG_ID);
        member.setWorkspaceId(AuthConstants.DEFAULT_WORKSPACE_ID);
        member.setUserId(user.getId());
        member.setRoleId(request.getRoleId());
        member.setJoinedAt(LocalDateTime.now(ZoneOffset.UTC));
        memberMapper.insert(member);

        return user.getId();
    }

    /**
     * 更新昵称/手机号（仅更新非空字段）。
     */
    @Override
    public void update(Long id, UserUpdateRequest request) {
        User user = requireUser(id);
        User update = new User();
        update.setId(id);
        if (StringUtils.hasText(request.getNickname())) {
            update.setNickname(request.getNickname().trim());
        }
        if (request.getPhone() != null) {
            update.setPhone(trimToNull(request.getPhone()));
        }
        userMapper.updateById(update);
    }

    /**
     * 启用/禁用：禁用后删除登录态，使已签发 token 下次校验即失效（强制重新登录）。
     */
    @Override
    public void updateStatus(Long id, UserStatusRequest request) {
        requireUser(id);
        User update = new User();
        update.setId(id);
        update.setStatus(request.getStatus());
        userMapper.updateById(update);

        // 禁用时踢下线：删除登录态 + refresh 会话（TD §6.1 主动失效，access/refresh 一并作废）
        if (request.getStatus() == AuthConstants.ACCOUNT_DISABLED) {
            tokenSessionCache.clear(id);
        }
    }

    /**
     * 修改密码：校验旧密码 → 更新新密文 → 删除登录态强制重新登录。
     */
    @Override
    public void updatePassword(Long userId, PasswordUpdateRequest request) {
        User user = requireUser(userId);

        // 旧密码校验失败返回 2002，与登录密码错误一致（IF §4.5）
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BizException(ErrorCode.PASSWORD_ERROR);
        }

        User update = new User();
        update.setId(userId);
        update.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(update);

        // 改密后旧登录态全部失效（access 登录态 + refresh 会话一并作废），强制重新登录
        tokenSessionCache.clear(userId);
    }

    /* ==================== 私有方法 ==================== */

    /**
     * 角色授予校验（2026-09-17 与 workspace 侧 Y1 同源缺口一并收口）。
     *
     * <p>为什么建号接口也必须校验：{@code POST /api/v1/user} 允许直接指定 {@code roleId}，
     * 若不校验，调用者就能建一个高权账号（或自己的小号）完成提权。此前这里连"角色是否存在"都没校验
     * （roleId 写错会产生孤儿成员关系）。</p>
     *
     * <p><b>本接口的门控是 {@code member:create}</b>（权限字典无 {@code user:*} 域，用户管理复用 member 域），
     * 而 {@code member:create} 是**空间管理员也持有**的权限 —— 因此必须按"非超管只能授予空间级/自身级角色"
     * 来收紧，否则 ws_admin 可通过建号授出 {@code org_admin}（scope=ORG），一步从空间管理员升为组织管理员。</p>
     *
     * <p>规则（单点在 {@link RoleGrantPolicy}）：</p>
     * <ol>
     *   <li>角色存在；</li>
     *   <li>角色归属当前租户（平台内置 {@code tenant_id=0} 对所有租户可见）；</li>
     *   <li>作用域：**超管不受限**；非超管只能授予 WS / SELF（组织级 ORG 与平台级 ALL 一律拒绝）。</li>
     * </ol>
     */
    private void assertRoleGrantable(Long roleId) {
        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "角色不存在");
        }
        Long tenantId = UserContext.getTenantId() != null ? UserContext.getTenantId() : AuthConstants.DEFAULT_TENANT_ID;
        if (!RoleGrantPolicy.belongsToTenant(role.getTenantId(), tenantId)) {
            throw new BizException(ErrorCode.OPERATION_NOT_ALLOWED, "该角色不属于当前租户");
        }
        if (!RoleGrantPolicy.grantableWithinWorkspace(role.getScope()) && !callerIsSuperAdmin()) {
            throw new BizException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "不允许授予该角色：仅超级管理员可授予组织级/平台级角色");
        }
    }

    /**
     * 当前调用者是否持有平台超管角色（判定"能否授予平台级角色"）。
     */
    private boolean callerIsSuperAdmin() {
        Long operatorId = UserContext.getUserId();
        if (operatorId == null) {
            return false;
        }
        return roleMapper.selectRoleCodesByUserId(operatorId).contains(RoleGrantPolicy.ROLE_SUPER_ADMIN);
    }

    /**
     * 查询用户，不存在抛 1004（资源不存在）。
     */
    private User requireUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "用户不存在");
        }
        return user;
    }

    /**
     * 实体转分页 VO（手机号脱敏）。
     */
    private UserPageVO toPageVO(User user) {
        UserPageVO vo = new UserPageVO();
        vo.setId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setPhone(maskPhone(user.getPhone()));
        vo.setStatus(user.getStatus());
        vo.setCreatedAt(user.getCreatedAt());
        return vo;
    }

    /**
     * 手机号脱敏（138****1234）。
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    /**
     * 空串转 null，保证可选字段写入 DB 时为空而非空字符串。
     */
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
