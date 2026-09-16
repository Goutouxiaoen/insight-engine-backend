package com.insightengine.workspace.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户只读引用 Mapper。
 *
 * <p>说明（MVP 临时方案，与 UMS 侧 {@code WorkspaceMapper} 同类）：添加空间成员时按邮箱定位
 * 已注册用户（IF §5.6 {@code POST /member/invite} 入参为 email），而用户主数据归 UMS。
 * init.sql 中 {@code ie_user} 与本服务同库，此处仅只读 ID，故 MVP 阶段直接查表；
 * 服务间契约化（Feign + api 模块）落地后改走 UMS 接口（TD §3.2）。</p>
 */
@Mapper
public interface UserRefMapper {

    /**
     * 按邮箱查询未删除用户的 ID（邮箱也是登录账号，IF §3.1）。
     *
     * @return 不存在返回 {@code null}
     */
    @Select("SELECT id FROM ie_user WHERE email = #{email} AND deleted = 0 LIMIT 1")
    Long selectIdByEmail(@Param("email") String email);
}
