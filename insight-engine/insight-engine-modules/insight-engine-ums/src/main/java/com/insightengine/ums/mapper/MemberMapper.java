package com.insightengine.ums.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.insightengine.ums.entity.Member;
import org.apache.ibatis.annotations.Mapper;

/**
 * 成员关系 Mapper。
 *
 * <p>继承 {@link BaseMapper} 获得通用 CRUD。注册时创建用户后写一条 member 关系，
 * 把新用户挂到默认工作空间并赋予默认角色（end_user）。</p>
 */
@Mapper
public interface MemberMapper extends BaseMapper<Member> {
}
