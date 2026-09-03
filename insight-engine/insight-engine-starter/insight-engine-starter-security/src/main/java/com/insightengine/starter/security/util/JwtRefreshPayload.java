package com.insightengine.starter.security.util;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/**
 * 刷新令牌解析后的载荷值对象。
 *
 * <p>刷新令牌也携带 {@code jti}（JWT ID），用于**一次性轮换与重放检测**
 * （TD ADR-10 / UMS review）：服务端按用户记录当前有效 jti，
 * refresh 时旧 jti 作废并签发新对；若旧 jti 被再次使用则视为泄露，吊销该用户全部会话。</p>
 *
 * @see JwtUtil#createRefreshToken
 */
@Data
@AllArgsConstructor
public class JwtRefreshPayload implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户 ID（JWT sub） */
    private Long userId;

    /** 令牌唯一 ID（JWT jti），用于轮换与重放检测 */
    private String jti;
}
