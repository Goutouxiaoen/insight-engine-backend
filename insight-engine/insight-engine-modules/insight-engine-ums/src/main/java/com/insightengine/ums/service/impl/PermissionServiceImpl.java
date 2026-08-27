package com.insightengine.ums.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.insightengine.ums.dto.response.PermissionGroupVO;
import com.insightengine.ums.dto.response.PermissionNodeVO;
import com.insightengine.ums.entity.Permission;
import com.insightengine.ums.mapper.PermissionMapper;
import com.insightengine.ums.service.PermissionService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 权限服务实现。
 *
 * <p>权限树（IF §6.3）：按 {@code resource} 字段分组，组内为具体权限点。
 * 使用 {@link LinkedHashMap} 保持资源首次出现顺序，保证前端展示稳定（不随查询顺序抖动）。</p>
 *
 * <p>资源显示名映射：{@code resource} 是权限表的结构化字段（如 kb / model:vendor），
 * 面向角色授权界面需给中文名（如 知识库 / 模型厂商）。此映射仅用于展示，
 * 与权限判定（按 code）无关，故本地维护一份常量映射即可。</p>
 */
@Service
public class PermissionServiceImpl implements PermissionService {

    /** 资源类型 → 中文显示名（与 init.sql 权限字典注释一致） */
    private static final Map<String, String> RESOURCE_NAMES = Map.ofEntries(
            Map.entry("auth", "认证管理"),
            Map.entry("org", "组织"),
            Map.entry("ws", "工作空间"),
            Map.entry("member", "成员"),
            Map.entry("role", "角色"),
            Map.entry("model:vendor", "模型厂商"),
            Map.entry("model:list", "模型列表"),
            Map.entry("model:route", "模型路由"),
            Map.entry("model:usage", "用量监控"),
            Map.entry("kb", "知识库"),
            Map.entry("kb:doc", "文档"),
            Map.entry("kb:retrieval", "检索"),
            Map.entry("agent", "Agent"),
            Map.entry("agent:workflow", "工作流"),
            Map.entry("agent:publish", "发布"),
            Map.entry("tool", "工具"),
            Map.entry("tool:builtin", "内置工具"),
            Map.entry("tool:http", "自定义 HTTP 工具"),
            Map.entry("conv", "对话"),
            Map.entry("billing:quota", "配额"),
            Map.entry("billing:export", "账单导出"),
            Map.entry("obs:metric", "指标"),
            Map.entry("obs:trace", "调用链"),
            Map.entry("audit:log", "审计日志"),
            Map.entry("audit:export", "审计导出"),
            Map.entry("api", "OpenAPI"),
            Map.entry("system", "系统设置"));

    private final PermissionMapper permissionMapper;

    public PermissionServiceImpl(PermissionMapper permissionMapper) {
        this.permissionMapper = permissionMapper;
    }

    /**
     * 权限树：按 resource 分组。
     */
    @Override
    public List<PermissionGroupVO> tree() {
        List<Permission> permissions = permissionMapper.selectList(
                new LambdaQueryWrapper<Permission>().orderByAsc(Permission::getResource)
                        .orderByAsc(Permission::getId));

        // 用 LinkedHashMap 按 resource 保序分组
        Map<String, List<Permission>> grouped = new LinkedHashMap<>();
        for (Permission permission : permissions) {
            grouped.computeIfAbsent(permission.getResource(), k -> new ArrayList<>())
                    .add(permission);
        }

        List<PermissionGroupVO> groups = new ArrayList<>(grouped.size());
        for (Map.Entry<String, List<Permission>> entry : grouped.entrySet()) {
            PermissionGroupVO group = new PermissionGroupVO();
            group.setResource(entry.getKey());
            group.setName(RESOURCE_NAMES.getOrDefault(entry.getKey(), entry.getKey()));
            group.setChildren(entry.getValue().stream().map(this::toNode).toList());
            groups.add(group);
        }
        return groups;
    }

    /**
     * 权限实体转树叶子节点。
     */
    private PermissionNodeVO toNode(Permission permission) {
        PermissionNodeVO node = new PermissionNodeVO();
        node.setId(permission.getId());
        node.setCode(permission.getCode());
        node.setName(permission.getName());
        return node;
    }
}
