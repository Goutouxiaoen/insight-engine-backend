package com.insightengine.gateway.security;

import com.insightengine.gateway.config.GatewaySecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 网关侧 JWT 访问令牌解析器。
 *
 * <p>为什么网关要单独实现一份解析而不是复用 starter-security 的 {@code JwtUtil}：
 * starter-security 是 Servlet 栈（基于 {@code HttpSecurity}），网关为 WebFlux
 * 响应式栈，引入会破坏运行模型。此处仅以 JJWT 复刻「校验 access 令牌」这一最小能力，
 * Claim 命名与 UMS 签发侧保持一致（JWT 是双端共享契约），避免两边 drift。</p>
 *
 * <p>异常语义（与 UMS 的 {@code JwtUtil.parseAccessToken} 一致，供过滤器区分 2001/2007）：</p>
 * <ul>
 *   <li>{@link io.jsonwebtoken.ExpiredJwtException}：令牌过期 → 2007；</li>
 *   <li>其他 {@link JwtException} / {@link IllegalArgumentException}：签名非法、
 *       类型不符、格式错误 → 2001。</li>
 * </ul>
 */
public class GatewayJwtParser {

    /** 令牌类型 Claim 名（与 UMS JwtUtil 常量一致） */
    private static final String CLAIM_TYPE = "type";
    /** 访问令牌类型值（与 UMS JwtUtil 常量一致） */
    private static final String TYPE_ACCESS = "access";
    /** 租户 ID Claim 名（TD §7.2） */
    private static final String CLAIM_TENANT_ID = "tenant_id";
    /** 工作空间 ID Claim 名（TD §7.2） */
    private static final String CLAIM_WS_ID = "ws_id";
    /** 角色编码列表 Claim 名（TD §7.2） */
    private static final String CLAIM_ROLES = "roles";

    /** 签名密钥（由配置属性在构造时派生） */
    private final SecretKey secretKey;

    /**
     * @param properties 网关安全配置（jwt-secret），调用方已保证非空且 >= 32 字节（fail-fast）
     */
    public GatewayJwtParser(GatewaySecurityProperties properties) {
        // HS256 要求密钥 >= 256 bit（32 字节），不足抛 WeakKeyException（配置装配层已前置校验）
        this.secretKey = Keys.hmacShaKeyFor(properties.getJwtSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 解析并校验访问令牌。
     *
     * <p>JJWT {@code parseSignedClaims} 同时校验签名与 exp（过期抛
     * {@code ExpiredJwtException}）。此处额外校验 {@code type=access}，
     * 防止客户端拿 refresh 令牌冒充 access 令牌（令牌混淆攻击，见 UMS JwtUtil 注释）。</p>
     *
     * @param token 原始访问令牌字符串
     * @return 强类型载荷
     * @throws io.jsonwebtoken.ExpiredJwtException 令牌已过期
     * @throws JwtException                       签名非法或类型不是 access
     */
    public GatewayJwtPayload parseAccessToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        if (!TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw new JwtException("非法的访问令牌类型");
        }
        Long userId = Long.valueOf(claims.getSubject());
        Long tenantId = toLong(claims.get(CLAIM_TENANT_ID));
        Long workspaceId = toLong(claims.get(CLAIM_WS_ID));
        List<String> roles = toStringList(claims.get(CLAIM_ROLES));
        return new GatewayJwtPayload(userId, tenantId, workspaceId, roles);
    }

    /**
     * 数字 Claim 安全转 Long：Jackson 会把 JSON 小整数反序列化为 {@code Integer}，
     * 直接强转会抛 ClassCastException，统一经 {@code Number} 转换规避。
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
     * Claim 安全转字符串列表：缺失或类型不符返回空列表，防御下游 NPE。
     */
    @SuppressWarnings("unchecked")
    private List<String> toStringList(Object value) {
        if (value instanceof List<?> list) {
            return (List<String>) list;
        }
        return List.of();
    }
}
