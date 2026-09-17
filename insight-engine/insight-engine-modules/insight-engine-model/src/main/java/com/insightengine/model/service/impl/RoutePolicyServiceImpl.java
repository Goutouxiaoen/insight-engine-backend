package com.insightengine.model.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.insightengine.common.core.BizException;
import com.insightengine.common.core.ErrorCode;
import com.insightengine.model.dto.request.RoutePolicyCreateRequest;
import com.insightengine.model.dto.request.RoutePolicyStatusRequest;
import com.insightengine.model.dto.request.RoutePolicyUpdateRequest;
import com.insightengine.model.dto.response.RoutePolicyVO;
import com.insightengine.model.entity.RoutePolicy;
import com.insightengine.model.mapper.RoutePolicyMapper;
import com.insightengine.model.service.RoutePolicyService;
import com.insightengine.model.support.ModelRouteResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 路由策略服务实现（IF §7.4）。
 *
 * <p>要点：</p>
 * <ul>
 *   <li><b>写入即校验</b>：rules 结构（strategy/rules/targets/modelId/match 键）在保存前解析校验，
 *       避免"配错能入库、调用时才炸"；</li>
 *   <li><b>名称唯一性</b>：策略名重复会让运维难以区分，故做显式查重（表无唯一约束，属业务约束）；</li>
 *   <li><b>排序口径</b>：列表按 {@code priority 升序、id 升序} 返回 —— 与解析顺序**完全一致**，
 *       前端看到的顺序就是实际生效顺序。</li>
 * </ul>
 */
@Service
public class RoutePolicyServiceImpl implements RoutePolicyService {

    private final RoutePolicyMapper routePolicyMapper;
    private final ModelRouteResolver routeResolver;

    public RoutePolicyServiceImpl(RoutePolicyMapper routePolicyMapper, ModelRouteResolver routeResolver) {
        this.routePolicyMapper = routePolicyMapper;
        this.routeResolver = routeResolver;
    }

    @Override
    public List<RoutePolicyVO> list() {
        List<RoutePolicy> policies = routePolicyMapper.selectList(new LambdaQueryWrapper<RoutePolicy>()
                .orderByAsc(RoutePolicy::getPriority)
                .orderByAsc(RoutePolicy::getId));
        return policies.stream().map(this::toVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(RoutePolicyCreateRequest request) {
        String name = request.getName().trim();
        Long exists = routePolicyMapper.selectCount(
                new LambdaQueryWrapper<RoutePolicy>().eq(RoutePolicy::getName, name));
        if (exists != null && exists > 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "策略名称已存在");
        }
        RoutePolicy policy = new RoutePolicy();
        policy.setName(name);
        policy.setPriority(request.getPriority());
        policy.setRules(routeResolver.writeValue(request.getRules()));
        policy.setEnabled(1);
        routePolicyMapper.insert(policy);
        return policy.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, RoutePolicyUpdateRequest request) {
        requirePolicy(id);
        RoutePolicy update = new RoutePolicy();
        update.setId(id);
        if (StringUtils.hasText(request.getName())) {
            String name = request.getName().trim();
            Long exists = routePolicyMapper.selectCount(new LambdaQueryWrapper<RoutePolicy>()
                    .eq(RoutePolicy::getName, name)
                    .ne(RoutePolicy::getId, id));
            if (exists != null && exists > 0) {
                throw new BizException(ErrorCode.PARAM_ERROR, "策略名称已存在");
            }
            update.setName(name);
        }
        if (request.getPriority() != null) {
            update.setPriority(request.getPriority());
        }
        if (request.getRules() != null) {
            update.setRules(routeResolver.writeValue(request.getRules()));
        }
        routePolicyMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, RoutePolicyStatusRequest request) {
        requirePolicy(id);
        RoutePolicy update = new RoutePolicy();
        update.setId(id);
        update.setEnabled(request.getEnabled());
        routePolicyMapper.updateById(update);
    }

    /* ==================== 私有方法 ==================== */

    private RoutePolicy requirePolicy(Long id) {
        RoutePolicy policy = routePolicyMapper.selectById(id);
        if (policy == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "路由策略不存在");
        }
        return policy;
    }

    private RoutePolicyVO toVO(RoutePolicy policy) {
        RoutePolicyVO vo = new RoutePolicyVO();
        vo.setId(policy.getId());
        vo.setName(policy.getName());
        vo.setPriority(policy.getPriority());
        vo.setRules(routeResolver.readValue(policy.getRules()));
        vo.setEnabled(policy.getEnabled());
        vo.setCreatedAt(policy.getCreatedAt());
        vo.setUpdatedAt(policy.getUpdatedAt());
        return vo;
    }
}
