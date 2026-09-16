package com.insightengine.workspace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.insightengine.workspace.entity.Workspace;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 工作空间 Mapper。
 */
@Mapper
public interface WorkspaceMapper extends BaseMapper<Workspace> {

    /**
     * 查询用户所属（成员关系覆盖）的工作空间 ID 列表。
     *
     * <p>用途：空间列表的可见范围收敛——非组织级管理员只能看到自己所属的空间
     * （与切换空间的服务端约束同源，见 PROGRESS §三 2026-09-09 裁决）。</p>
     */
    @Select("""
            SELECT m.workspace_id
            FROM ie_member m
            WHERE m.user_id = #{userId}
              AND m.deleted = 0
              AND m.workspace_id IS NOT NULL
            """)
    List<Long> selectWorkspaceIdsByUserId(@Param("userId") Long userId);
}
