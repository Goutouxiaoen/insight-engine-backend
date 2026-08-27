package com.insightengine.starter.mybatis.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusPropertiesCustomizer;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * starter-mybatis 自动配置类。
 *
 * <p>引入本 starter 后，Spring Boot 通过
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}
 * 自动加载本配置，为业务服务装配 MyBatis-Plus 的通用能力（TD §3.1 / §5.1）。</p>
 *
 * <p>职责：</p>
 * <ul>
 *   <li>全局逻辑删除配置：{@code deleted} 字段（0 正常 / 1 已删除），
 *      与 init.sql 各业务表的逻辑删除列保持一致，避免每个实体重复标注；</li>
 *   <li>分页插件：{@link PaginationInnerInterceptor}，方言 PostgreSQL，配合
 *      {@link com.insightengine.common.core.PageQuery} 完成分页查询；</li>
 *   <li>审计字段自动填充：由 {@link MybatisMetaObjectHandler} 负责
 *      {@code created_at / updated_at / created_by / updated_by}。</li>
 * </ul>
 *
 * <p>设计要点：逻辑删除采用「全局配置」而非逐实体 {@code @TableLogic} 注解，
 * 避免新表漏配导致数据被物理删除；统一约定所有含 {@code deleted} 列的表都走逻辑删除。</p>
 */
@Configuration
public class MybatisAutoConfiguration {

    /** 逻辑删除字段名（与 init.sql 一致） */
    private static final String LOGIC_DELETE_FIELD = "deleted";
    /** 逻辑删除标记值：已删除 */
    private static final String LOGIC_DELETE_VALUE = "1";
    /** 逻辑未删除标记值：正常 */
    private static final String LOGIC_NOT_DELETE_VALUE = "0";

    /**
     * 全局逻辑删除配置。
     * <p>通过 {@link MybatisPlusPropertiesCustomizer} 修改默认配置，
     * 而非在 application.yml 硬编码，保证任何引入本 starter 的服务自动获得一致行为。</p>
     */
    @Bean
    public MybatisPlusPropertiesCustomizer logicDeleteCustomizer() {
        return properties -> {
            GlobalConfig.DbConfig dbConfig = properties.getGlobalConfig().getDbConfig();
            dbConfig.setLogicDeleteField(LOGIC_DELETE_FIELD);
            dbConfig.setLogicDeleteValue(LOGIC_DELETE_VALUE);
            dbConfig.setLogicNotDeleteValue(LOGIC_NOT_DELETE_VALUE);
        };
    }

    /**
     * MyBatis-Plus 拦截器：注册分页插件。
     * <p>分页方言 PostgreSQL，{@code maxLimit} 与 {@link com.insightengine.common.core.PageQuery#MAX_PAGE_SIZE}
     * 保持一致（上限 100），防止绕过应用层校验的恶意超大分页拖垮数据库。</p>
     */
    @Bean
    @ConditionalOnMissingBean(MybatisPlusInterceptor.class)
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.POSTGRE_SQL);
        // 单页上限 100，与 PageQuery.MAX_PAGE_SIZE 对齐，双保险拦截非法分页
        pagination.setMaxLimit(100L);
        interceptor.addInnerInterceptor(pagination);
        return interceptor;
    }

    /**
     * 审计字段自动填充处理器。
     */
    @Bean
    @ConditionalOnMissingBean(MybatisMetaObjectHandler.class)
    public MybatisMetaObjectHandler mybatisMetaObjectHandler() {
        return new MybatisMetaObjectHandler();
    }
}
