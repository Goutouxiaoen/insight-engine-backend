package com.insightengine.ums.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.insightengine.common.core.BizException;
import com.insightengine.common.core.ErrorCode;
import com.insightengine.common.core.PageResult;
import com.insightengine.ums.constant.AuthConstants;
import com.insightengine.ums.dto.request.PasswordUpdateRequest;
import com.insightengine.ums.dto.request.UserCreateRequest;
import com.insightengine.ums.dto.request.UserPageQuery;
import com.insightengine.ums.dto.request.UserStatusRequest;
import com.insightengine.ums.dto.request.UserUpdateRequest;
import com.insightengine.ums.dto.response.UserPageVO;
import com.insightengine.ums.entity.Member;
import com.insightengine.ums.entity.User;
import com.insightengine.ums.mapper.MemberMapper;
import com.insightengine.ums.mapper.UserMapper;
import com.insightengine.ums.service.UserService;
import org.springframework.data.redis.core.StringRedisTemplate;
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
    private final StringRedisTemplate stringRedisTemplate;

    public UserServiceImpl(UserMapper userMapper,
                           MemberMapper memberMapper,
                           PasswordEncoder passwordEncoder,
                           StringRedisTemplate stringRedisTemplate) {
        this.userMapper = userMapper;
        this.memberMapper = memberMapper;
        this.passwordEncoder = passwordEncoder;
        this.stringRedisTemplate = stringRedisTemplate;
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
            stringRedisTemplate.delete(AuthConstants.KEY_AUTH_TOKEN + id);
            stringRedisTemplate.delete(AuthConstants.KEY_AUTH_REFRESH + id);
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
        stringRedisTemplate.delete(AuthConstants.KEY_AUTH_TOKEN + userId);
        stringRedisTemplate.delete(AuthConstants.KEY_AUTH_REFRESH + userId);
    }

    /* ==================== 私有方法 ==================== */

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
