package com.insightengine.starter.security.token;

import java.io.Serializable;

/**
 * 一次签发的令牌对（唯一签发入口 {@link AuthTokenIssuer} 的产物）。
 *
 * <p>把「access + refresh + jti + 两个 TTL」打包返回，避免每个调用方各自拼装：
 * 会话缓存需要 {@code jti} 与 TTL，接口响应需要 token 与 {@code expiresIn}。</p>
 *
 * @param accessToken      访问令牌
 * @param refreshToken     刷新令牌（已携带当前 {@code ws_id}，供下次刷新沿用）
 * @param refreshJti       本次 refresh 的 jti（服务端会话按其摘要记录，用于一次性轮换/重放检测）
 * @param accessTtlSeconds access 有效期（秒），供接口返回 {@code expiresIn}
 * @param refreshTtlSeconds refresh 有效期（秒），供会话缓存设置 TTL
 */
public record IssuedTokens(String accessToken,
                           String refreshToken,
                           String refreshJti,
                           long accessTtlSeconds,
                           long refreshTtlSeconds) implements Serializable {
}
