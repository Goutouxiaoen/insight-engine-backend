package com.insightengine.starter.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Security 配置属性（前缀 {@code insight.security}）。
 *
 * <p>由业务服务在 {@code application.yml} 中按需覆盖，未配置时使用下方默认值。
 * 之所以抽成配置属性而非硬编码，是让签发密钥与 token 有效期可运维可替换，
 * 生产环境通过环境变量/配置中心注入独立密钥，避免默认密钥泄露。</p>
 *
 * <p>字段说明：</p>
 * <ul>
 *   <li>{@code jwtSecret}：HS256 签名密钥，要求 {@code >= 32} 字节（256 bit）；
 *       默认值仅供本地开发，生产必须覆盖；</li>
 *   <li>{@code accessTokenTtlSeconds}：访问令牌有效期（默认 2h，TD §7.2）；</li>
 *   <li>{@code refreshTokenTtlSeconds}：刷新令牌有效期（默认 7d，TD §7.2）。</li>
 * </ul>
 */
@Data
@ConfigurationProperties(prefix = "insight.security")
public class SecurityProperties {

    /** JWT 签名密钥（开发默认值，生产必须通过配置覆盖） */
    private String jwtSecret = "insight-engine-dev-secret-key-change-me-in-prod-2026-08";

    /** 访问令牌有效期（秒），默认 2 小时 */
    private long accessTokenTtlSeconds = 7200L;

    /** 刷新令牌有效期（秒），默认 7 天 */
    private long refreshTokenTtlSeconds = 604800L;
}
