package com.insightengine.model.support;

import com.insightengine.common.core.BizException;
import com.insightengine.common.core.ErrorCode;
import com.insightengine.model.constant.ModelConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 敏感凭据加解密器（TD §16.1：AES-256-GCM，主密钥 KEK 放环境变量）。
 *
 * <h3>为什么用 GCM 而不是 CBC</h3>
 * <p>GCM 是**认证加密**（AEAD）：除保密外还带完整性校验，密文被篡改会在解密时直接报错，
 * 不会像 CBC 那样"悄悄解出垃圾明文"。代价是**必须每次使用独立 IV**（同一密钥下 IV 复用会彻底
 * 破坏 GCM 安全性），故本类每次加密都随机生成 12 字节 IV 并随密文一起存（{@code ie_secret.iv}）。</p>
 *
 * <h3>KEK 从哪来</h3>
 * <ul>
 *   <li>本机：{@code application-local.yml} 的 {@code insight.secret.kek}（gitignore，不入库）；</li>
 *   <li>生产：环境变量 {@code INSIGHT_SECRET_KEK}（Base64 的 32 字节随机值）。</li>
 * </ul>
 * <p><b>未配置时不会静默降级</b>：读取路径可用，但一旦要加密（接入/更新厂商密钥）会抛
 * 明确的 9999 错误并提示如何生成 KEK —— 宁可用不了，也绝不把明文写进库（AGENTS 铁律 5）。</p>
 *
 * <h3>轮换</h3>
 * <p>{@code ie_secret.kek_version} 记录加密时使用的 KEK 版本；轮换做法是**新增版本**并保留旧 KEK
 * 用于解密历史数据，而不是直接替换（直接替换会让旧密文永久不可解）。</p>
 */
@Component
public class SecretCipher {

    private static final Logger log = LoggerFactory.getLogger(SecretCipher.class);

    /** 算法标识（写入 ie_secret.algo，为将来算法升级留判别依据） */
    public static final String ALGO_AES_GCM = "AES-256-GCM";

    /**
     * JCE **transformation** 串（注意与上面的"算法名"区分）。
     *
     * <p>{@code Cipher.getInstance(...)} 要的是 {@code 算法/模式/填充} 三段式的 transformation，
     * 不是"算法名"：写 {@code "AES-256-GCM"} 会抛 {@code NoSuchAlgorithmException: Cannot find any provider
     * supporting AES-256-GCM}（2026-09-17 冒烟实测踩到）。密钥长度由 {@link SecretKeySpec} 决定，
     * 不需要也不能写进 transformation。</p>
     */
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    /** 当前 KEK 版本（写入 ie_secret.kek_version） */
    public static final String DEFAULT_KEK_VERSION = "v1";

    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BIT = 128;
    private static final int KEY_LENGTH_BYTE = 32;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final String kekRaw;

    public SecretCipher(@Value("${insight.secret.kek:}") String kekRaw) {
        this.kekRaw = kekRaw;
        if (!StringUtils.hasText(kekRaw)) {
            log.warn("insight.secret.kek 未配置：厂商密钥的加密写入将不可用（读取既有密文也需配置）。"
                    + "本机请写入 application-local.yml，生产请注入环境变量 INSIGHT_SECRET_KEK");
        }
    }

    /**
     * 加密明文。每次调用生成独立 IV。
     */
    public Encrypted encrypt(String plainText) {
        if (!StringUtils.hasText(plainText)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "密钥内容不能为空");
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, loadKey(), new GCMParameterSpec(TAG_LENGTH_BIT, iv));
            byte[] cipherBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            Base64.Encoder encoder = Base64.getEncoder();
            return new Encrypted(encoder.encodeToString(cipherBytes), encoder.encodeToString(iv),
                    ALGO_AES_GCM, DEFAULT_KEK_VERSION);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("厂商密钥加密失败（明文长度={}）", plainText.length(), e);
            throw new BizException(ErrorCode.SYSTEM_ERROR, "密钥加密失败，请检查 insight.secret.kek 配置");
        }
    }

    /**
     * 解密（仅内部调用：调用厂商 API 时取出明文）。
     *
     * <p>日志与异常中**绝不回显明文或密文内容**。</p>
     */
    public String decrypt(String cipherTextBase64, String ivBase64) {
        if (!StringUtils.hasText(cipherTextBase64) || !StringUtils.hasText(ivBase64)) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "密钥记录不完整（缺少密文或 IV）");
        }
        try {
            Base64.Decoder decoder = Base64.getDecoder();
            byte[] iv = decoder.decode(ivBase64);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, loadKey(), new GCMParameterSpec(TAG_LENGTH_BIT, iv));
            return new String(cipher.doFinal(decoder.decode(cipherTextBase64)), StandardCharsets.UTF_8);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            // 常见原因：KEK 换过（旧密文解不开）或密文被篡改（GCM 完整性校验失败）
            log.error("厂商密钥解密失败（可能是 KEK 不匹配或密文被篡改）", e);
            throw new BizException(ErrorCode.SYSTEM_ERROR,
                    "密钥解密失败：请确认 insight.secret.kek 与加密时一致（换 KEK 会导致旧密文不可解）");
        }
    }

    /**
     * 生成掩码提示（只留前缀 3 位与后缀 4 位），供界面识别"这是哪把 Key"。
     */
    public static String mask(String plainText) {
        if (!StringUtils.hasText(plainText)) {
            return null;
        }
        int len = plainText.length();
        if (len <= ModelConstants.MASK_PREFIX_LEN + ModelConstants.MASK_SUFFIX_LEN) {
            // 太短则整体掩掉，避免"掩码本身泄露密钥"
            return ModelConstants.MASK_MIDDLE;
        }
        return plainText.substring(0, ModelConstants.MASK_PREFIX_LEN)
                + ModelConstants.MASK_MIDDLE
                + plainText.substring(len - ModelConstants.MASK_SUFFIX_LEN);
    }

    /**
     * 加载并校验 KEK（懒加载：只在真正加解密时校验，避免"没配密钥就连服务都起不来"）。
     */
    private SecretKey loadKey() {
        if (!StringUtils.hasText(kekRaw)) {
            throw new BizException(ErrorCode.SYSTEM_ERROR,
                    "未配置敏感凭据主密钥（insight.secret.kek / 环境变量 INSIGHT_SECRET_KEK）；"
                            + "生成方式：PowerShell 执行 [Convert]::ToBase64String((1..32 | %{ Get-Random -Max 256 }))");
        }
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(kekRaw.trim());
        } catch (IllegalArgumentException e) {
            throw new BizException(ErrorCode.SYSTEM_ERROR,
                    "insight.secret.kek 必须是 Base64 编码的 32 字节随机值");
        }
        if (keyBytes.length != KEY_LENGTH_BYTE) {
            throw new BizException(ErrorCode.SYSTEM_ERROR,
                    "insight.secret.kek 解码后应为 32 字节（当前 " + keyBytes.length + " 字节）");
        }
        return new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * 加密结果（密文 + IV + 算法 + KEK 版本，逐列对应 {@code ie_secret} 表）。
     */
    public record Encrypted(String cipherText, String iv, String algo, String kekVersion) {
    }
}
