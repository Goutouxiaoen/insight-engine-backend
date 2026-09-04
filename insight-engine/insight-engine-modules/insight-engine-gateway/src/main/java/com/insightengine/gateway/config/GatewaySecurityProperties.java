package com.insightengine.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 网关安全配置属性（前缀 {@code insight.gateway}）。
 *
 * <p>网关在入口统一校验 JWT 签名（TD §8.3 AuthGlobalFilter），因此必须持有与签发方（UMS）
 * 相同的 HS256 密钥。{@code jwtSecret} **不设代码默认值**，由启动装配处 fail-fast 校验：
 * 为空或不足 32 字节拒绝启动，防止「网关带占位密钥裸奔上线」——
 * 网关密钥泄露意味着攻击者可离线伪造任意用户身份的 token，等于绕过整个认证体系。</p>
 *
 * <p>配置来源与 UMS 对齐：本机开发用 yml 占位默认值，生产统一经环境变量
 * {@code INSIGHT_SECURITY_JWT_SECRET} 注入（与 UMS 同一变量，保证同一密钥双端可验）。</p>
 */
@Data
@ConfigurationProperties(prefix = "insight.gateway")
public class GatewaySecurityProperties {

    /** JWT 签名密钥（无默认值：启动时校验非空、长度 >= 32 字节，prod 下禁用开发占位密钥） */
    private String jwtSecret;
}
