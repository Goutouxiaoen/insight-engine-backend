package com.insightengine.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.insightengine.model.entity.RoutePolicy;
import org.apache.ibatis.annotations.Mapper;

/**
 * 路由策略 Mapper（{@code ie_route_policy}）。
 *
 * <p>条件简单（启用 + 优先级排序），统一用条件构造器，不写自定义 SQL。</p>
 */
@Mapper
public interface RoutePolicyMapper extends BaseMapper<RoutePolicy> {
}
