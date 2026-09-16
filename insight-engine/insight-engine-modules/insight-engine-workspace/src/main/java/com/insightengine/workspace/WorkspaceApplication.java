package com.insightengine.workspace;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Workspace（工作空间与组织）服务启动类。
 *
 * <p>智擎 AI 第二个业务微服务（PRD §9.2 端口 7102），提供组织、工作空间、成员管理能力
 * （IF §5）。工作空间是后续所有业务资源（知识库 / Agent / 工具）的归属边界与数据权限维度。</p>
 *
 * <p>{@code @MapperScan} 扫描本模块 Mapper 接口，配合 starter-mybatis 的
 * MyBatis-Plus 自动装配完成 ORM 初始化。</p>
 */
@SpringBootApplication
@MapperScan("com.insightengine.workspace.mapper")
public class WorkspaceApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkspaceApplication.class, args);
    }
}
