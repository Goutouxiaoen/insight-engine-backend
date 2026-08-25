package com.insightengine.starter.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Security 配置属性（前缀 {@code insight.security}）。
 *
 * <p>由业务服务在 {@code application.yml} / 环境变量 / 配置中心注入，**不设代码默认值**——
 * 若字段为空或为开发占位密钥，{@code SecurityAutoConfiguration} 启动即 fail-fast 拒绝，
 * 防止「带默认密钥裸奔上线」（密钥泄露 = 可离线伪造任意 JWT，等于绕过整个认证体系）。</p>
 *
 * <p>字段说明：</p>
 * <ul>
 *   <li>{@code jwtSecret}：HS256 签名密钥，要求 {@code >= 32} 字节（256 bit）；
 *       本地开发可用 yml 占位符默认值，生产必须经环境变量 {@code INSIGHT_SECURITY_JWT_SECRET} 注入；</li>
 *   <li>{@code accessTokenTtlSeconds}：访问令牌有效期（默认 2h，TD §7.2）；</li>
 *   <li>{@code refreshTokenTtlSeconds}：刷新令牌有效期（默认 7d，TD §7.2）。</li>
 * </ul>
 */
@Data
@ConfigurationProperties(prefix = "insight.security")
public class SecurityProperties {

    /** JWT 签名密钥（无默认值：启动时校验非空、长度 >= 32，prod 下禁用开发占位密钥） */
    private String jwtSecret;

    /** 访问令牌有效期（秒），默认 2 小时 */
    private long accessTokenTtlSeconds = 7200L;

    /** 刷新令牌有效期（秒），默认 7 天 */
    private long refreshTokenTtlSeconds = 604800L;
}
