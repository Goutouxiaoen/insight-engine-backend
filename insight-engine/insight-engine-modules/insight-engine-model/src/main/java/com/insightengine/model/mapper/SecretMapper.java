package com.insightengine.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.insightengine.model.entity.SecretRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 密钥表 Mapper（{@code ie_secret}，DB.md §5.11.4）。
 *
 * <p>仅使用 MyBatis-Plus 通用 CRUD：密钥表不参与业务查询联表，避免明文/密文被无意带出到其它查询结果里。</p>
 */
@Mapper
public interface SecretMapper extends BaseMapper<SecretRecord> {
}
