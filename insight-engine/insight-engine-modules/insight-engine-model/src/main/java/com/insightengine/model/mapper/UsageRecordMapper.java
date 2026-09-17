package com.insightengine.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.insightengine.model.entity.UsageRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用量明细 Mapper（{@code ie_usage_record}）。
 *
 * <p>只增不改：本 Mapper 仅用 {@code insert}（查询侧由 §7.8 用量查询接口使用条件构造器）。</p>
 */
@Mapper
public interface UsageRecordMapper extends BaseMapper<UsageRecord> {
}
