package com.insightengine.ums.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.insightengine.common.core.BizException;
import com.insightengine.common.core.ErrorCode;
import com.insightengine.starter.security.blacklist.TokenBlacklistService;
import com.insightengine.starter.security.util.JwtUtil;
import com.insightengine.ums.constant.AuthConstants;
import com.insightengine.ums.dto.request.LoginRequest;
import com.insightengine.ums.dto.request.RefreshRequest;
import com.insightengine.ums.dto.request.RegisterRequest;
import com.insightengine.ums.dto.response.LoginResponse;
import com.insightengine.ums.dto.response.UserInfoVO;
import com.insightengine.ums.entity.Member;
import com.insightengine.ums.entity.Role;
import com.insightengine.ums.entity.User;
import com.insightengine.ums.mapper.MemberMapper;
import com.insightengine.ums.mapper.PermissionMapper;
import com.insightengine.ums.mapper.RoleMapper;
import com.insightengine.ums.mapper.UserMapper;
import com.insightengine.ums.mapper.WorkspaceMapper;
import com.insightengine.ums.service.AuthService;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * 认证服务实现。
 *
 * <p>登录安全策略（PRD §12.1.5）：连续密码错误 {@value AuthConstants#MAX_LOGIN_FAIL_COUNT} 次
 * 锁定 {@value AuthConstants#LOGIN_LOCK_SECONDS} 秒（30 分钟）。失败计数与锁状态存 Redis
 * （Key 见 {@link AuthConstants}），因锁定是「临时态」，无需落库。</p>
 */
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final MemberMapper memberMapper;
    private final WorkspaceMapper workspaceMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate stringRedisTemplate;
    private final TokenBlacklistService tokenBlacklistService;

    public AuthServiceImpl(UserMapper userMapper,
                           RoleMapper roleMapper,
                           PermissionMapper permissionMapper,
                           MemberMapper memberMapper,
                           WorkspaceMapper workspaceMapper,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil,
                           StringRedisTemplate stringRedisTemplate,
                           TokenBlacklistService tokenBlacklistService) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.memberMapper = memberMapper;
        this.workspaceMapper = workspaceMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.stringRedisTemplate = stringRedisTemplate;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    /**
     * 登录：锁定检查 → 账号/状态校验 → 密码校验 → 签发令牌。
     */
    @Override
    public LoginResponse login(LoginRequest request) {
        String account = request.getAccount().trim();

        // 1. 锁定检查：命中锁 key 直接拒绝，避免无谓的查库与密码比对
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(AuthConstants.KEY_LOGIN_LOCK + account))) {
            throw new BizException(ErrorCode.ACCOUNT_LOCKED);
        }

        // 2. 查用户：不存在返回 2001（IF §3.1 错误码）
        User user = userMapper.selectByAccount(account);
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "账号或密码错误");
        }

        // 3. 状态校验：禁用返回 2004（IF §3.1）
        if (user.getStatus() != null && user.getStatus() == AuthConstants.ACCOUNT_DISABLED) {
            throw new BizException(ErrorCode.ACCOUNT_DISABLED);
        }

        // 4. 密码校验：失败递增计数，达阈值锁定（PRD §12.1.5）
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            handleLoginFail(account);
            throw new BizException(ErrorCode.PASSWORD_ERROR);
        }

        // 5. 成功：清空失败计数，更新最近登录时间，签发令牌
        stringRedisTemplate.delete(AuthConstants.KEY_LOGIN_FAIL + account);
        updateLastLoginAt(user.getId());

        LoginResponse response = buildLoginResponse(user);
        // 登录态写 Redis（TD §6.1：ie:auth:token:{userId} 存 access 摘要），支持主动踢人
        cacheToken(user.getId(), response.getToken());
        return response;
    }

    /**
     * 刷新：校验 refresh token 有效性后重签令牌对。
     */
    @Override
    public LoginResponse refresh(RefreshRequest request) {
        Long userId;
        try {
            userId = jwtUtil.parseRefreshToken(request.getRefreshToken());
        } catch (JwtException | IllegalArgumentException e) {
            // 刷新令牌非法/过期：要求重新登录（2001）
            throw new BizException(ErrorCode.UNAUTHORIZED, "刷新令牌无效或已过期");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "用户不存在");
        }
        if (user.getStatus() != null && user.getStatus() == AuthConstants.ACCOUNT_DISABLED) {
            throw new BizException(ErrorCode.ACCOUNT_DISABLED);
        }

        LoginResponse response = buildLoginResponse(user);
        cacheToken(user.getId(), response.getToken());
        return response;
    }

    /**
     * 登出：token 加黑名单 + 删除登录态缓存。
     */
    @Override
    public void logout(String accessToken) {
        long remainingSeconds = jwtUtil.getRemainingSeconds(accessToken);
        if (remainingSeconds > 0) {
            tokenBlacklistService.blacklist(accessToken, remainingSeconds);
        }
        // 删除登录态：即便剩余有效期已为 0，也确保缓存不残留
        try {
            Long userId = jwtUtil.parseAccessToken(accessToken).getUserId();
            stringRedisTemplate.delete(AuthConstants.KEY_AUTH_TOKEN + userId);
        } catch (JwtException e) {
            // token 已不可解析，登录态本就失效，忽略即可（登出幂等）
        }
    }

    /**
     * 注册：创建用户 + 挂到默认工作空间（end_user 角色）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long register(RegisterRequest request) {
        String email = request.getEmail().trim();
        // 邮箱唯一性校验：DB 唯一索引兜底，这里先友好提示避免抛约束异常
        Long existCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (existCount != null && existCount > 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "该邮箱已被注册");
        }

        User user = new User();
        user.setTenantId(AuthConstants.DEFAULT_TENANT_ID);
        user.setEmail(email);
        user.setNickname(request.getNickname().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setStatus(AuthConstants.ACCOUNT_NORMAL);
        userMapper.insert(user);

        // 注册用户挂到默认工作空间，赋予默认角色 end_user（MVP 开放注册，PRD §12.1.4）
        Long endUserRoleId = resolveDefaultRoleId();
        Member member = new Member();
        member.setTenantId(AuthConstants.DEFAULT_TENANT_ID);
        member.setOrgId(AuthConstants.DEFAULT_ORG_ID);
        member.setWorkspaceId(AuthConstants.DEFAULT_WORKSPACE_ID);
        member.setUserId(user.getId());
        member.setRoleId(endUserRoleId);
        member.setJoinedAt(LocalDateTime.now(ZoneOffset.UTC));
        memberMapper.insert(member);

        return user.getId();
    }

    /**
     * 当前用户信息：用户 + 角色 + 默认工作空间。
     */
    @Override
    public UserInfoVO currentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "用户不存在");
        }
        return buildUserInfo(user);
    }

    /* ==================== 私有方法 ==================== */

    /**
     * 处理登录失败：计数 +1，达到阈值则锁定。
     *
     * <p>计数与锁共用同一个 30 分钟窗口：每失败一次重置计数 key 的 TTL 为 30 分钟，
     * 保证「连续」语义（窗口内无失败则计数自然过期清零）。达到阈值时另设锁 key。</p>
     */
    private void handleLoginFail(String account) {
        String failKey = AuthConstants.KEY_LOGIN_FAIL + account;
        Long failCount = stringRedisTemplate.opsForValue().increment(failKey);
        // 首次失败才设置窗口 TTL；后续失败沿用已有窗口，避免频繁重置过期时间
        if (failCount != null && failCount == 1L) {
            stringRedisTemplate.expire(failKey, Duration.ofSeconds(AuthConstants.LOGIN_LOCK_SECONDS));
        }
        if (failCount != null && failCount >= AuthConstants.MAX_LOGIN_FAIL_COUNT) {
            stringRedisTemplate.opsForValue().set(
                    AuthConstants.KEY_LOGIN_LOCK + account, "1",
                    Duration.ofSeconds(AuthConstants.LOGIN_LOCK_SECONDS));
            // 触发锁定后清空计数，锁定解除后从零重新累计
            stringRedisTemplate.delete(failKey);
        }
    }

    /**
     * 更新最近登录时间。
     */
    private void updateLastLoginAt(Long userId) {
        User update = new User();
        update.setId(userId);
        update.setLastLoginAt(LocalDateTime.now(ZoneOffset.UTC));
        userMapper.updateById(update);
    }

    /**
     * 组装登录响应：查角色/权限/工作空间，签发令牌。
     */
    private LoginResponse buildLoginResponse(User user) {
        List<String> roles = roleMapper.selectRoleCodesByUserId(user.getId());
        List<String> permissions = permissionMapper.selectPermissionCodesByUserId(user.getId());
        Long workspaceId = roleMapper.selectDefaultWorkspaceIdByUserId(user.getId());

        String accessToken = jwtUtil.createAccessToken(
                user.getId(), user.getTenantId(), workspaceId, roles, permissions);
        String refreshToken = jwtUtil.createRefreshToken(user.getId());

        LoginResponse response = new LoginResponse();
        response.setToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setExpiresIn(jwtUtil.getAccessTtlSeconds());
        response.setUser(buildUserInfo(user, roles, workspaceId));
        return response;
    }

    /**
     * 组装用户信息（含角色与工作空间）。
     */
    private UserInfoVO buildUserInfo(User user) {
        List<String> roles = roleMapper.selectRoleCodesByUserId(user.getId());
        Long workspaceId = roleMapper.selectDefaultWorkspaceIdByUserId(user.getId());
        return buildUserInfo(user, roles, workspaceId);
    }

    /**
     * 组装用户信息（复用已查到的角色/工作空间，避免重复查询）。
     */
    private UserInfoVO buildUserInfo(User user, List<String> roles, Long workspaceId) {
        UserInfoVO vo = new UserInfoVO();
        vo.setId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setPhone(maskPhone(user.getPhone()));
        vo.setAvatar(user.getAvatar());
        vo.setRoles(roles);
        vo.setTenantId(user.getTenantId());
        vo.setWorkspaceId(workspaceId);
        vo.setWorkspaceName(workspaceId == null ? null : workspaceMapper.selectNameById(workspaceId));
        return vo;
    }

    /**
     * 手机号脱敏（IF §4.1：138****1234）。
     *
     * <p>保留前 3 后 4 位，中间以星号替代；非 11 位或为空直接返回原值（防御异常数据）。</p>
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    /**
     * 写登录态缓存（TTL = access 有效期）。
     */
    private void cacheToken(Long userId, String accessToken) {
        stringRedisTemplate.opsForValue().set(
                AuthConstants.KEY_AUTH_TOKEN + userId,
                accessToken,
                Duration.ofSeconds(jwtUtil.getAccessTtlSeconds()));
    }

    /**
     * 解析默认角色 end_user 的 ID。
     *
     * <p>注册需给用户赋默认角色。角色编码 end_user 为 init.sql 预置（builtin），
     * 直接按编码查询。若查询失败（种子数据缺失）则抛系统异常，不静默降级。</p>
     */
    private Long resolveDefaultRoleId() {
        Role role = roleMapper.selectOne(
                new LambdaQueryWrapper<Role>().eq(Role::getCode, AuthConstants.DEFAULT_ROLE_CODE));
        if (role == null) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "默认角色配置缺失");
        }
        return role.getId();
    }
}
