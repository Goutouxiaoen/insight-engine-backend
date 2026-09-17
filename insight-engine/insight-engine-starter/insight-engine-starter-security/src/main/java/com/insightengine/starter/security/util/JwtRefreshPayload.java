package com.insightengine.starter.security.util;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/**
 * 刷新令牌解析后的载荷值对象。
 *
 * <p>刷新令牌携带 {@code jti}（JWT ID），用于**一次性轮换与重放检测**
 * （TD ADR-10 / UMS review）：服务端按用户记录当前有效 jti，
 * refresh 时旧 jti 作废并签发新对；若旧 jti 被再次使用则视为泄露，吊销该用户全部会话。</p>
 *
 * <p><b>{@code workspaceId}（2026-09-17 新增）</b>：refresh 也必须携带"当前所在空间"，
 * 否则刷新时只能重算（拿默认空间）→ 会把用户**悄悄切回默认空间**（BE-20260916-01 同源问题）。
 * 刷新链路的正确口径是「沿用旧令牌的 {@code ws_id}」，见 {@code AuthTokenIssuer} 与 IF §3 口径表。</p>
 *
 * @see JwtUtil#createRefreshToken(Long, String, Long)
 */
@Data
@AllArgsConstructor
public class JwtRefreshPayload implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户 ID（JWT sub） */
    private Long userId;

    /** 令牌唯一 ID（JWT jti），用于轮换与重放检测 */
    private String jti;

    /** 当前工作空间 ID（JWT ws_id），刷新时沿用；组织级管理员可为空 */
    private Long workspaceId;
}
