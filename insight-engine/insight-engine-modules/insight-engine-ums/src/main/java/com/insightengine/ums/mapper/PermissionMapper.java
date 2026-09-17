package com.insightengine.ums.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.insightengine.common.constant.AuthQuerySql;
import com.insightengine.ums.entity.Permission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 权限 Mapper。
 */
@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {

    /**
     * 查询用户拥有的权限编码列表（按角色展开）。
     *
     * <p>链路：user → member → role → role_permission → permission（PRD §12.2.3）。
     * 登录时调用，结果写入 JWT 的 {@code perms} Claim，供 {@code @PreAuthorize} 方法级权限校验。</p>
     *
     * <p><b>SQL 口径来源</b>：{@link AuthQuerySql#SELECT_PERMISSION_CODES_BY_USER}——
     * 与 workspace「切换空间」共用同一份字面量（"登录 / 刷新 / 切换"三个签发入口必须同口径，
     * IF §3 口径表；2026-09-17 BE-20260916-01 教训）。</p>
     */
    @Select(AuthQuerySql.SELECT_PERMISSION_CODES_BY_USER)
    List<String> selectPermissionCodesByUserId(@Param("userId") Long userId);
}
