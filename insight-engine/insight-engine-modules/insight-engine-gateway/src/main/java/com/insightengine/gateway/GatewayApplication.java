package com.insightengine.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 智擎 AI API 网关启动类。
 *
 * <p>统一流量入口（PRD §9.2 端口 7000），基于 Spring Cloud Gateway（WebFlux），
 * 职责见 {@code pom.xml} 注释：路由转发、JWT/API Key 鉴权、跨域、traceId 透传。</p>
 *
 * <p>注意：本模块为响应式栈，不引入任何 Servlet 栈的 starter（web/security/mybatis/redis），
 * 鉴权过滤器以 {@code GlobalFilter} 实现（TD §8.3）。</p>
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
