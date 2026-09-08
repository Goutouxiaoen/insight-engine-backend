package com.insightengine.gateway.config;

import com.insightengine.gateway.security.GatewayJwtParser;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * 网关认证装配。
 *
 * <p>职责：启用 {@link GatewaySecurityProperties} 配置绑定，并装配 JWT 解析器 Bean，
 * 同时执行与 UMS {@code SecurityAutoConfiguration} 一致的 fail-fast 密钥校验——
 * 网关与 UMS 共享同一 HS256 密钥（经环境变量 {@code INSIGHT_SECURITY_JWT_SECRET} 注入），
 * 任何一端带空/弱/占位密钥启动都会立刻失败，杜绝「生产密钥可预测」风险。</p>
 */
@Configuration
@EnableConfigurationProperties(GatewaySecurityProperties.class)
public class GatewayAuthConfiguration {

    /**
     * JWT 解析器 Bean，初始化即做密钥 fail-fast 校验（TD §16.1 / 对齐 UMS starter）：
     * <ol>
     *   <li>未配置（null/空白）→ 拒绝启动；</li>
     *   <li>长度不足 32 字节 → 拒绝启动（HS256 要求 >= 256 bit）；</li>
     *   <li>prod profile 下含 {@code change-me} 开发占位标记 → 拒绝启动。</li>
     * </ol>
     */
    @Bean
    @ConditionalOnMissingBean(GatewayJwtParser.class)
    public GatewayJwtParser gatewayJwtParser(GatewaySecurityProperties properties, Environment environment) {
        String secret = properties.getJwtSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("insight.gateway.jwt-secret 未配置，拒绝启动。"
                    + "请通过环境变量 INSIGHT_SECURITY_JWT_SECRET 注入 HS256 密钥（>= 32 字节，与 UMS 同源）");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("insight.gateway.jwt-secret 长度不足 32 字节（HS256 要求 >= 256 bit），拒绝启动");
        }
        if (Arrays.asList(environment.getActiveProfiles()).contains("prod")
                && secret.contains("change-me")) {
            throw new IllegalStateException("prod 环境禁止使用开发占位 JWT 密钥（含 change-me 标记），"
                    + "请通过环境变量 INSIGHT_SECURITY_JWT_SECRET 注入独立随机密钥后重启");
        }
        return new GatewayJwtParser(properties);
    }
}
