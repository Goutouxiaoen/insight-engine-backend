package com.insightengine.starter.security.util;

import com.insightengine.starter.security.config.SecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * JWT 签发与解析工具。
 *
 * <p>基于 JJWT 0.12（TD §2.1）实现 HS256 对称签名。职责：</p>
 * <ul>
 *   <li>{@link #createAccessToken}：签发访问令牌，载荷含用户身份 + 角色 + 权限（TD §7.2）；</li>
 *   <li>{@link #createRefreshToken}：签发刷新令牌，仅含用户 ID 与令牌类型，最小化载荷；</li>
 *   <li>{@link #parseAccessToken} / {@link #parseRefreshToken}：解析并校验签名与过期，
 *       类型不符或篡改直接抛 {@link JwtException}，由认证过滤器统一拦截。</li>
 * </ul>
 *
 * <p>设计要点：</p>
 * <ul>
 *   <li>访问/刷新令牌用 {@code type} Claim 区分，防止刷新令牌被当访问令牌使用（令牌混淆攻击）；</li>
 *   <li>权限编码在「登录时」展开写入访问令牌，因为 JWT 无状态、服务端不每次查库，
 *       这是方法级权限 {@code @PreAuthorize} 能工作的前提；</li>
 *   <li>数字 Claim 读取统一走 {@link #toLong}，规避 Jackson 把小整数反序列化为
 *       {@code Integer} 导致 {@code Long} 强转失败的问题。</li>
 * </ul>
 */
public class JwtUtil {

    /** 令牌类型 Claim 名 */
    private static final String CLAIM_TYPE = "type";
    /** 令牌类型值：访问令牌 */
    private static final String TYPE_ACCESS = "access";
    /** 令牌类型值：刷新令牌 */
    private static final String TYPE_REFRESH = "refresh";
    /** 租户 ID Claim 名（TD §7.2） */
    private static final String CLAIM_TENANT_ID = "tenant_id";
    /** 工作空间 ID Claim 名（TD §7.2） */
    private static final String CLAIM_WS_ID = "ws_id";
    /** 角色编码列表 Claim 名（TD §7.2） */
    private static final String CLAIM_ROLES = "roles";
    /** 权限编码列表 Claim 名 */
    private static final String CLAIM_PERMISSIONS = "perms";

    /** 签名密钥 */
    private final SecretKey secretKey;
    /** 访问令牌有效期（毫秒） */
    private final long accessTtlMillis;
    /** 刷新令牌有效期（毫秒） */
    private final long refreshTtlMillis;

    /**
     * 构造时由配置属性派生签名密钥与有效期。
     */
    public JwtUtil(SecurityProperties properties) {
        // HS256 要求密钥至少 256 bit（32 字节），不足会抛 WeakKeyException
        this.secretKey = Keys.hmacShaKeyFor(properties.getJwtSecret().getBytes(StandardCharsets.UTF_8));
        this.accessTtlMillis = properties.getAccessTokenTtlSeconds() * 1000L;
        this.refreshTtlMillis = properties.getRefreshTokenTtlSeconds() * 1000L;
    }

    /**
     * 签发访问令牌。
     *
     * @param userId      用户 ID
     * @param tenantId    租户 ID
     * @param workspaceId 当前工作空间 ID（可为 null）
     * @param roles       角色编码列表
     * @param permissions 权限编码列表
     */
    public String createAccessToken(Long userId, Long tenantId, Long workspaceId,
                                    List<String> roles, List<String> permissions) {
        Date now = new Date();
        var builder = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .claim(CLAIM_TENANT_ID, tenantId)
                .claim(CLAIM_ROLES, roles == null ? List.of() : roles)
                .claim(CLAIM_PERMISSIONS, permissions == null ? List.of() : permissions)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessTtlMillis));
        // workspaceId 可能为空（组织级管理员），JJWT 会忽略 null Claim，故条件写入
        if (workspaceId != null) {
            builder.claim(CLAIM_WS_ID, workspaceId);
        }
        return builder.signWith(secretKey).compact();
    }

    /**
     * 签发刷新令牌（仅含用户 ID + 类型，载荷最小化）。
     */
    public String createRefreshToken(Long userId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_TYPE, TYPE_REFRESH)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + refreshTtlMillis))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 解析并校验访问令牌，返回强类型载荷。
     *
     * @throws JwtException 签名非法、令牌过期、或类型不是 access 时抛出
     */
    public JwtPayload parseAccessToken(String token) {
        Claims claims = parse(token);
        if (!TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw new JwtException("非法的访问令牌类型");
        }
        Long userId = Long.valueOf(claims.getSubject());
        Long tenantId = toLong(claims.get(CLAIM_TENANT_ID));
        Long workspaceId = toLong(claims.get(CLAIM_WS_ID));
        List<String> roles = toStringList(claims.get(CLAIM_ROLES));
        List<String> permissions = toStringList(claims.get(CLAIM_PERMISSIONS));
        return new JwtPayload(userId, tenantId, workspaceId, roles, permissions);
    }

    /**
     * 解析并校验刷新令牌，返回用户 ID。
     *
     * @throws JwtException 签名非法、令牌过期、或类型不是 refresh 时抛出
     */
    public Long parseRefreshToken(String token) {
        Claims claims = parse(token);
        if (!TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw new JwtException("非法的刷新令牌类型");
        }
        return Long.valueOf(claims.getSubject());
    }

    /**
     * 访问令牌有效期（秒），供登录接口返回 {@code expiresIn} 字段（IF §3.1）。
     */
    public long getAccessTtlSeconds() {
        return accessTtlMillis / 1000L;
    }

    /**
     * 计算令牌剩余有效期（秒）。
     *
     * <p>登出时用于设置黑名单 TTL（TD ADR-10：黑名单存活 = token 剩余有效期）。
     * 令牌已过期返回 0，避免负数 TTL 导致 Redis 写入异常。</p>
     */
    public long getRemainingSeconds(String token) {
        Claims claims = parse(token);
        long remainingMillis = claims.getExpiration().getTime() - System.currentTimeMillis();
        return remainingMillis <= 0 ? 0L : remainingMillis / 1000L;
    }

    /**
     * 解析并校验签名与过期。
     * <p>JJWT 的 {@code parseSignedClaims} 会同时校验签名与 exp，
     * 过期抛 {@code ExpiredJwtException}，签名非法抛 {@code SignatureException}。</p>
     */
    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 数字 Claim 安全转 Long。
     * <p>Jackson 反序列化 JSON 数字时，小整数会变成 {@code Integer}，
     * 直接 {@code (Long) claims.get(...)} 会抛 ClassCastException，故统一走 Number 转换。</p>
     */
    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }

    /**
     * Claim 安全转字符串列表；缺失或类型不符返回空列表，防御下游 NPE。
     */
    @SuppressWarnings("unchecked")
    private List<String> toStringList(Object value) {
        if (value instanceof List<?> list) {
            return (List<String>) list;
        }
        return List.of();
    }
}
