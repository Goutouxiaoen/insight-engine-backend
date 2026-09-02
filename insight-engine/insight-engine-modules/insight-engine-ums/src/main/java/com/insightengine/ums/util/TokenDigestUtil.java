package com.insightengine.ums.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Token 摘要工具。
 *
 * <p>登录态缓存与黑名单均要求「只存 SHA-256 摘要、不落明文 token」，
 * 避免 Redis 被拖库时直接泄露可用 token（TD §6.1 / ADR-10）。</p>
 *
 * <p>用 JDK 自带 {@link MessageDigest} 计算摘要，避免为单个哈希函数引入额外依赖
 * （项目 BOM 未引入 hutool-crypto）。</p>
 */
public final class TokenDigestUtil {

    private TokenDigestUtil() {
        // 工具类禁止实例化
    }

    /**
     * 计算字符串的 SHA-256 十六进制摘要（小写）。
     */
    public static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                String s = Integer.toHexString(b & 0xFF);
                if (s.length() == 1) {
                    hex.append('0');
                }
                hex.append(s);
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 是 JDK 标准算法，理论上必然存在；防御性兜底抛运行时异常
            throw new IllegalStateException("SHA-256 算法不可用", e);
        }
    }
}
