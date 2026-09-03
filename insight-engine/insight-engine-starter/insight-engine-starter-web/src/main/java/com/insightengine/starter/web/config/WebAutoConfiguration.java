package com.insightengine.starter.web.config;

import com.insightengine.starter.web.filter.TraceFilter;
import com.insightengine.starter.web.filter.UserContextFilter;
import com.insightengine.starter.web.handler.GlobalExceptionHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * starter-web 自动配置类。
 *
 * <p>引入本 starter 后，Spring Boot 通过
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}
 * 自动加载本配置，无需业务方手动 {@code @Import}。</p>
 *
 * <p>职责：</p>
 * <ul>
 *   <li>注册 {@link TraceFilter}（traceId 生成/透传，最高优先级，最先执行）；</li>
 *   <li>注册 {@link UserContextFilter}（解析网关明文头，填充用户上下文）；</li>
 *   <li>注册 {@link GlobalExceptionHandler}（统一异常转 Result）。</li>
 * </ul>
 *
 * <p>设计要点：</p>
 * <ul>
 *   <li>{@code @ConditionalOnWebApplication(type = SERVLET)}：仅 Servlet 栈生效，
 *       gateway（WebFlux）不会误注册 Servlet Filter；</li>
 *   <li>{@code @ConditionalOnMissingBean}：业务方已有自定义实现时不覆盖。</li>
 * </ul>
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class WebAutoConfiguration {

    /**
     * TraceID 过滤器，最高优先级，确保任何业务逻辑执行前 traceId 已就绪。
     */
    @Bean
    @ConditionalOnMissingBean(TraceFilter.class)
    public FilterRegistrationBean<TraceFilter> traceFilterRegistration() {
        FilterRegistrationBean<TraceFilter> registration = new FilterRegistrationBean<>(new TraceFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        // 匹配所有路径，含静态资源，保证全链路 traceId 覆盖
        registration.addUrlPatterns("/*");
        return registration;
    }

    /**
     * 用户上下文过滤器，紧随 traceId 过滤器之后执行。
     *
     * <p>默认**不注册**：身份必须来自「服务自验 JWT」（方案 A，推荐）。只有走
     * 「网关下发明文头」架构（TD ADR-5）的服务才显式开启
     * {@code insight.web.trust-gateway-headers=true} 信任网关头，
     * 避免业务服务无条件信任客户端可伪造的明文身份头造成越权。</p>
     */
    @Bean
    @ConditionalOnMissingBean(UserContextFilter.class)
    @ConditionalOnProperty(name = "insight.web.trust-gateway-headers", havingValue = "true")
    public FilterRegistrationBean<UserContextFilter> userContextFilterRegistration() {
        FilterRegistrationBean<UserContextFilter> registration = new FilterRegistrationBean<>(new UserContextFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        registration.addUrlPatterns("/*");
        return registration;
    }

    /**
     * 全局异常处理器。
     */
    @Bean
    @ConditionalOnMissingBean(GlobalExceptionHandler.class)
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }
}
