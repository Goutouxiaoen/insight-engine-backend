package com.insightengine.ums.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 工作空间只读查询 Mapper。
 *
 * <p>说明（MVP 临时方案）：{@code /auth/me} 需返回 {@code workspaceName}（IF §3.5），
 * 而 workspace 服务尚未实现。因 init.sql 中 {@code ie_workspace} 与 UMS 同库、
 * 且此处仅只读名称，故 MVP 阶段由 UMS 直接查询该表；待 workspace 服务落地后
 * 改走 Feign 调用（TD §3.2 服务间只能通过 api 契约调用），此处查询届时移除。</p>
 */
@Mapper
public interface WorkspaceMapper {

    /**
     * 查询工作空间名称。
     */
    @Select("SELECT name FROM ie_workspace WHERE id = #{id} AND deleted = 0")
    String selectNameById(@Param("id") Long id);
}
