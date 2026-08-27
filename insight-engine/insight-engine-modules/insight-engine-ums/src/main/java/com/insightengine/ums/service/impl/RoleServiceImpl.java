package com.insightengine.ums.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.insightengine.common.core.BizException;
import com.insightengine.common.core.ErrorCode;
import com.insightengine.ums.constant.AuthConstants;
import com.insightengine.ums.dto.request.RoleCreateRequest;
import com.insightengine.ums.dto.request.RolePermissionUpdateRequest;
import com.insightengine.ums.dto.response.RoleVO;
import com.insightengine.ums.entity.Role;
import com.insightengine.ums.mapper.RoleMapper;
import com.insightengine.ums.mapper.RolePermissionMapper;
import com.insightengine.ums.service.RoleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * 角色服务实现。
 *
 * <p>删除保护：{@code builtin=1} 的内置角色禁止删除（IF §6.6 返回 1003），
 * 防止误删预置角色导致权限体系崩溃。授权采用「先删后插」保证结果与请求完全一致。</p>
 */
@Service
public class RoleServiceImpl implements RoleService {

    /** 内置角色标记（init.sql builtin=1，禁删） */
    private static final int BUILTIN = 1;

    private final RoleMapper roleMapper;
    private final RolePermissionMapper rolePermissionMapper;

    public RoleServiceImpl(RoleMapper roleMapper, RolePermissionMapper rolePermissionMapper) {
        this.roleMapper = roleMapper;
        this.rolePermissionMapper = rolePermissionMapper;
    }

    /**
     * 角色列表：返回全部角色（含内置与自定义），列表不含权限明细。
     */
    @Override
    public List<RoleVO> list() {
        List<Role> roles = roleMapper.selectList(
                new LambdaQueryWrapper<Role>().orderByAsc(Role::getId));
        return roles.stream().map(role -> toVO(role, null)).toList();
    }

    /**
     * 创建角色：编码唯一校验 → 落库 → 可选授权。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(RoleCreateRequest request) {
        String code = request.getCode().trim();
        // 编码唯一性校验（DB 唯一索引 uk_role_code_tenant 兜底）
        Long existCount = roleMapper.selectCount(
                new LambdaQueryWrapper<Role>().eq(Role::getCode, code));
        if (existCount != null && existCount > 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "角色编码已存在");
        }

        Role role = new Role();
        role.setTenantId(AuthConstants.DEFAULT_TENANT_ID);
        role.setCode(code);
        role.setName(request.getName().trim());
        role.setScope(request.getScope());
        role.setDescription(request.getDescription());
        role.setBuiltin(0);
        roleMapper.insert(role);

        if (!CollectionUtils.isEmpty(request.getPermissionIds())) {
            rolePermissionMapper.batchInsert(role.getId(), request.getPermissionIds());
        }
        return role.getId();
    }

    /**
     * 角色详情：含已授权权限 ID 列表。
     */
    @Override
    public RoleVO detail(Long id) {
        Role role = requireRole(id);
        List<Long> permissionIds = rolePermissionMapper.selectPermissionIdsByRoleId(id);
        return toVO(role, permissionIds);
    }

    /**
     * 删除角色：内置角色拒绝删除。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Role role = requireRole(id);
        if (role.getBuiltin() != null && role.getBuiltin() == BUILTIN) {
            // IF §6.6：builtin=1 禁止删除，返回 1003（不允许的操作）
            throw new BizException(ErrorCode.OPERATION_NOT_ALLOWED, "内置角色禁止删除");
        }
        roleMapper.deleteById(id);
        // 清理该角色的权限关联，避免产生孤儿数据
        rolePermissionMapper.deleteByRoleId(id);
    }

    /**
     * 角色授权：先删后插，权限集合与请求完全一致（幂等）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(Long id, RolePermissionUpdateRequest request) {
        requireRole(id);
        rolePermissionMapper.deleteByRoleId(id);
        if (!CollectionUtils.isEmpty(request.getPermissionIds())) {
            rolePermissionMapper.batchInsert(id, request.getPermissionIds());
        }
    }

    /* ==================== 私有方法 ==================== */

    /**
     * 查询角色，不存在抛 1004。
     */
    private Role requireRole(Long id) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "角色不存在");
        }
        return role;
    }

    /**
     * 实体转 VO。
     *
     * @param permissionIds 权限 ID 列表（列表场景传 null，详情场景传实际值）
     */
    private RoleVO toVO(Role role, List<Long> permissionIds) {
        RoleVO vo = new RoleVO();
        vo.setId(role.getId());
        vo.setCode(role.getCode());
        vo.setName(role.getName());
        vo.setScope(role.getScope());
        vo.setBuiltin(role.getBuiltin());
        vo.setDescription(role.getDescription());
        vo.setPermissionIds(permissionIds);
        return vo;
    }
}
