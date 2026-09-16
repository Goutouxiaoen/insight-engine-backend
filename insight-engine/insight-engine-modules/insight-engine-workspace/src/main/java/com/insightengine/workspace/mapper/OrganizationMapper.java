package com.insightengine.workspace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.insightengine.workspace.entity.Organization;
import org.apache.ibatis.annotations.Mapper;

/**
 * 组织 Mapper。
 *
 * <p>继承 {@link BaseMapper} 获得通用 CRUD（编码唯一性走
 * {@code LambdaQueryWrapper} 查询 + init.sql 的 {@code uk_org_code_tenant} 唯一索引兜底）。</p>
 */
@Mapper
public interface OrganizationMapper extends BaseMapper<Organization> {
}
