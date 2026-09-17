package com.insightengine.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.insightengine.model.entity.ModelVendor;
import org.apache.ibatis.annotations.Mapper;

/**
 * 模型厂商 Mapper（{@code ie_model_vendor}）。
 *
 * <p>查询条件简单（编码唯一、名称/类型过滤），统一用 MyBatis-Plus 条件构造器，
 * 不写自定义 SQL —— 少一处 SQL 就少一处口径漂移（AGENTS 铁律 6 精神）。</p>
 */
@Mapper
public interface ModelVendorMapper extends BaseMapper<ModelVendor> {
}
