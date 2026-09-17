package com.insightengine.ums.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.insightengine.common.constant.AuthQuerySql;
import com.insightengine.ums.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色 Mapper。
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    /**
     * 查询用户拥有的角色编码列表（<b>用户维度</b>，跨空间聚合）。
     *
     * <p>通过 member → role 两表关联（PRD §12.1.3）：成员关系可能挂 workspace（空间级角色），
     * 也可能是组织级（{@code workspace_id} 为空）。<b>不按 workspace 过滤</b>——
     * JWT 里的 {@code roles} 表示"这个人是谁"，与"当前站在哪个空间"无关
     * （2026-09-17 修正，见 {@link AuthQuerySql} 与 IF §3 口径表）。</p>
     *
     * <p><b>SQL 口径来源</b>：{@link AuthQuerySql#SELECT_ROLE_CODES_BY_USER}，
     * 与 workspace「切换空间」共用同一份字面量。</p>
     */
    @Select(AuthQuerySql.SELECT_ROLE_CODES_BY_USER)
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);

    /**
     * 查询用户默认工作空间 ID（取第一条成员关系的工作空间）。
     *
     * <p>**仅登录入口使用**（此刻尚无"当前空间"概念）：刷新入口必须沿用旧令牌的 {@code ws_id}，
     * 否则用户会被悄悄切回默认空间（IF §3 口径表）。组织级管理员无空间级成员关系时返回 null。</p>
     */
    @Select("""
            SELECT m.workspace_id
            FROM ie_member m
            WHERE m.user_id = #{userId}
              AND m.deleted = 0
              AND m.workspace_id IS NOT NULL
            ORDER BY m.id ASC
            LIMIT 1
            """)
    Long selectDefaultWorkspaceIdByUserId(@Param("userId") Long userId);
}
