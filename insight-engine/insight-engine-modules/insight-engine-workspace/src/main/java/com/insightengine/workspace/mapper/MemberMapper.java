package com.insightengine.workspace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.insightengine.workspace.dto.response.MemberVO;
import com.insightengine.workspace.entity.Member;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 成员关系 Mapper。
 *
 * <p>成员列表需要展示「昵称 / 邮箱 / 角色名」，这些字段不在 {@code ie_member} 上，
 * 故用一条联表查询一次取回，避免 N+1（IF §5.6 成员列表）。</p>
 *
 * <p>注意：自定义 SQL 不受 MyBatis-Plus 全局逻辑删除配置影响，故显式写
 * {@code m.deleted = 0} 等条件。</p>
 */
@Mapper
public interface MemberMapper extends BaseMapper<Member> {

    /**
     * 成员分页（联表 ie_user / ie_role）。
     *
     * @param page        分页参数（由 starter-mybatis 的分页插件拦截改写 SQL）
     * @param workspaceId 工作空间 ID
     */
    @Select("""
            SELECT m.id,
                   m.workspace_id,
                   m.user_id,
                   m.role_id,
                   m.joined_at,
                   u.nickname AS nickname,
                   u.email    AS email,
                   r.name     AS role_name
            FROM ie_member m
            JOIN ie_user u ON u.id = m.user_id AND u.deleted = 0
            LEFT JOIN ie_role r ON r.id = m.role_id AND r.deleted = 0
            WHERE m.deleted = 0
              AND m.workspace_id = #{workspaceId}
            ORDER BY m.id DESC
            """)
    IPage<MemberVO> selectMemberPage(IPage<MemberVO> page, @Param("workspaceId") Long workspaceId);
}
