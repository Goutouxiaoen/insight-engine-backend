package com.insightengine.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.insightengine.model.entity.Model;
import org.apache.ibatis.annotations.Mapper;

/**
 * 模型 Mapper（{@code ie_model}）。
 *
 * <p>条件构造器即可满足分页/过滤需求（厂商、类型、关键字），不写自定义 SQL。</p>
 */
@Mapper
public interface ModelMapper extends BaseMapper<Model> {
}
