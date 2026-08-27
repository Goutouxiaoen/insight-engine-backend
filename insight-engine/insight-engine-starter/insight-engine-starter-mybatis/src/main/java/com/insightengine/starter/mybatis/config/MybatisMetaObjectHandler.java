package com.insightengine.starter.mybatis.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.insightengine.starter.web.context.UserContext;
import org.apache.ibatis.reflection.MetaObject;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * MyBatis-Plus 审计字段自动填充处理器。
 *
 * <p>职责（TD §5.1 审计字段约定）：在插入/更新时自动填充通用审计字段，</p>
 * <ul>
 *   <li>插入：{@code createdAt} / {@code updatedAt}（当前 UTC 时间）、
 *       {@code createdBy} / {@code updatedBy}（当前登录用户 ID）；</li>
 *   <li>更新：{@code updatedAt} / {@code updatedBy}。</li>
 * </ul>
 *
 * <p>设计要点：</p>
 * <ul>
 *   <li>时间统一使用 UTC（{@link ZoneOffset#UTC}），与 TD §5.1「统一 UTC、展示层转换」一致，
 *       避免多时区部署下时间错乱；</li>
 *   <li>操作人 ID 从 {@link UserContext} 读取，未登录（如系统定时任务、初始化）时兜底为 0，
 *       即「系统」；</li>
 *   <li>使用 {@code strictInsertFill} 语义的 {@link MetaObjectHandler#strictInsertFill}，
 *       仅在实体已声明该字段且值为 null 时才填充，绝不覆盖业务显式赋值。</li>
 * </ul>
 */
public class MybatisMetaObjectHandler implements MetaObjectHandler {

    /** 系统操作人兜底 ID：未登录/系统任务时填充 0，表示「系统」 */
    private static final Long SYSTEM_USER_ID = 0L;

    /**
     * 插入时填充审计字段。
     * <p>时间字段用 UTC 当前时间；操作人字段取当前登录用户，未登录兜底 0。</p>
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        Long operatorId = currentOperatorId();
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "createdBy", Long.class, operatorId);
        this.strictInsertFill(metaObject, "updatedBy", Long.class, operatorId);
    }

    /**
     * 更新时填充审计字段。
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now(ZoneOffset.UTC));
        this.strictUpdateFill(metaObject, "updatedBy", Long.class, currentOperatorId());
    }

    /**
     * 读取当前操作人 ID；未登录时返回系统兜底值 0。
     * <p>之所以不返回 null，是因为审计字段通常为 NOT NULL，null 会导致插入失败。</p>
     */
    private Long currentOperatorId() {
        Long userId = UserContext.getUserId();
        return userId == null ? SYSTEM_USER_ID : userId;
    }
}
