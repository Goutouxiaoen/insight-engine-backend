package com.insightengine.starter.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insightengine.starter.security.blacklist.TokenBlacklistService;
import com.insightengine.starter.security.filter.JwtAuthFilter;
import com.insightengine.starter.security.handler.RestAccessDeniedHandler;
import com.insightengine.starter.security.handler.RestAuthenticationEntryPoint;
import com.insightengine.starter.security.session.TokenSessionService;
import com.insightengine.starter.security.util.JwtUtil;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * starter-security 自动配置类。
 *
 * <p>为引入本 starter 的服务装配无状态 JWT 认证体系（TD §7.3）：</p>
 * <ul>
 *   <li>{@link SecurityFilterChain}：关闭 CSRF、无状态 Session、放行认证白名单、
 *       其余全部需认证，并前置 {@link JwtAuthFilter}；</li>
 *   <li>{@link JwtUtil}：JWT 签发/解析（HS256）；</li>
 *   <li>{@link PasswordEncoder}：BCrypt，strength=10（TD §16.1）；</li>
 *   <li>{@link RestAuthenticationEntryPoint} / {@link RestAccessDeniedHandler}：
 *       未认证/无权限统一转 Result 结构。</li>
 * </ul>
 *
 * <p>设计要点：</p>
 * <ul>
 *   <li>白名单集中管理（{@code /auth/login}、{@code /auth/register}、{@code /auth/refresh}
 *       以及 Knife4j 文档路径），非白名单一律需有效访问令牌；</li>
 *   <li>开启 {@code @EnableMethodSecurity}，业务服务用 {@code @PreAuthorize}
 *       做方法级权限校验（TD §7.4）；</li>
 *   <li>{@code @ConditionalOnMissingBean} 允许业务侧自定义覆盖，保持 starter 可扩展。</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityAutoConfiguration {

    /**
     * 密码编码器：BCrypt，强度 10（TD §16.1）。
     * <p>每次编码自带随机盐，故同一明文多次编码结果不同，只能比对不能解密。</p>
     */
    @Bean
    @ConditionalOnMissingBean(PasswordEncoder.class)
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    /**
     * JWT 工具：由配置属性派生签名密钥与有效期。
     *
     * <p>fail-fast 启动校验：HS256 密钥一旦带默认值/开发占位值上线，
     * 攻击者可离线伪造任意 userId/roles/perms 的 JWT（等于绕过整个认证体系），
     * 故在 Bean 初始化期强制校验，而非留给运行时。</p>
     */
    @Bean
    @ConditionalOnMissingBean(JwtUtil.class)
    public JwtUtil jwtUtil(SecurityProperties properties, Environment environment) {
        String secret = properties.getJwtSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("insight.security.jwt-secret 未配置，拒绝启动。"
                    + "请通过环境变量 INSIGHT_SECURITY_JWT_SECRET 或配置中心注入 HS256 密钥（>= 32 字节）");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("insight.security.jwt-secret 长度不足 32 字节（HS256 要求 >= 256 bit），拒绝启动");
        }
        if (Arrays.asList(environment.getActiveProfiles()).contains("prod")
                && secret.contains("change-me")) {
            throw new IllegalStateException("prod 环境禁止使用开发占位 JWT 密钥（含 change-me 标记），"
                    + "请通过环境变量 INSIGHT_SECURITY_JWT_SECRET 注入独立随机密钥后重启");
        }
        return new JwtUtil(properties);
    }

    /**
     * 未认证处理器。
     */
    @Bean
    @ConditionalOnMissingBean(RestAuthenticationEntryPoint.class)
    public RestAuthenticationEntryPoint restAuthenticationEntryPoint(ObjectMapper objectMapper) {
        return new RestAuthenticationEntryPoint(objectMapper);
    }

    /**
     * 无权限处理器。
     */
    @Bean
    @ConditionalOnMissingBean(RestAccessDeniedHandler.class)
    public RestAccessDeniedHandler restAccessDeniedHandler(ObjectMapper objectMapper) {
        return new RestAccessDeniedHandler(objectMapper);
    }

    /**
     * Security 过滤链：无状态 + JWT。
     *
     * <p>白名单路径在此集中放行；{@code /auth/logout}、{@code /auth/me} 等需携带
     * 有效访问令牌（不在白名单内），保证「登出需登录态」「查当前用户需登录态」。</p>
     */
    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtUtil jwtUtil,
                                                   ObjectMapper objectMapper,
                                                   ObjectProvider<TokenBlacklistService> blacklistProvider,
                                                   ObjectProvider<TokenSessionService> sessionProvider,
                                                   RestAuthenticationEntryPoint entryPoint,
                                                   RestAccessDeniedHandler accessDeniedHandler) throws Exception {
        http.csrf(csrf -> csrf.disable())
                // 无状态：不创建、不使用 HttpSession，认证完全依赖 JWT（TD §7.3）
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 认证白名单（登录/注册/刷新 + Knife4j 文档）
                        .requestMatchers("/auth/login", "/auth/register", "/auth/refresh",
                                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-resources/**",
                                "/webjars/**", "/doc.html").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(handler -> handler
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                // 前置 JWT 认证过滤器（在用户名密码过滤器之前执行）；
                // 黑名单/登录态服务为可选依赖，未提供时退化为纯无状态 JWT 校验
                .addFilterBefore(new JwtAuthFilter(jwtUtil, objectMapper,
                                blacklistProvider.getIfAvailable(), sessionProvider.getIfAvailable()),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
