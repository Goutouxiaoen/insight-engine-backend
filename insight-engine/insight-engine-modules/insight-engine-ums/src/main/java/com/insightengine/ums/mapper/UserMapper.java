package com.insightengine.ums.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.insightengine.ums.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户 Mapper。
 *
 * <p>继承 {@link BaseMapper} 获得通用 CRUD（逻辑删除已全局生效，自动追加 deleted=0）。</p>
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 按登录账号（邮箱或手机号）查询用户。
     *
     * <p>登录时账号可能为邮箱也可能为手机号（IF §3.1），故用 OR 条件同时匹配两列。
     * 用 {@code LIMIT 1} 兜底——理论上 email/phone 有唯一索引不会重复，但防御性限制避免意外返回多行。</p>
     */
    @Select("SELECT * FROM ie_user WHERE deleted = 0 AND (email = #{account} OR phone = #{account}) LIMIT 1")
    User selectByAccount(@Param("account") String account);
}
