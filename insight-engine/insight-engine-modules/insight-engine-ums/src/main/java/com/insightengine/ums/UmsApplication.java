package com.insightengine.ums;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * UMS（用户与权限）服务启动类。
 *
 * <p>智擎 AI 第一个完整业务微服务（PRD §9.2 端口 7101），提供认证、用户、角色、权限能力。</p>
 *
 * <p>{@code @MapperScan} 扫描本模块 Mapper 接口，配合 starter-mybatis 的
 * MyBatis-Plus 自动装配完成 ORM 初始化。</p>
 */
@SpringBootApplication
@MapperScan("com.insightengine.ums.mapper")
public class UmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(UmsApplication.class, args);
    }
}
