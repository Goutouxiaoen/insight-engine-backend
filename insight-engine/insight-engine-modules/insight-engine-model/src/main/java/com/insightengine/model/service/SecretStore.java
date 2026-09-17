package com.insightengine.model.service;

/**
 * 敏感凭据存取（{@code ie_secret}，DB.md §5.11.4 / TD §16.1）。
 *
 * <p>把"加密 + 落库 + 掩码"三件事收口在一处，业务侧只见 {@code secretId}：
 * **明文只在 save 入参与 readPlain 出参上出现，绝不进入实体 VO、日志、审计**。</p>
 *
 * @see com.insightengine.model.support.SecretCipher
 */
public interface SecretStore {

    /**
     * 保存密钥（新建或更新同一行）。
     *
     * @param existingSecretId 已存在的密钥 ID；为空则新建一行（更新场景传 vendor 上已有的 secretId，
     *                         保持引用稳定、不产生重复行）
     * @param name             展示名
     * @param secretType       密钥类型（{@code ModelConstants.SECRET_TYPE_*}）
     * @param plainText        明文（仅在本次调用内存中存在）
     * @return 密钥 ID（供 {@code ie_model_vendor.api_key_secret_id} 引用）
     */
    Long save(Long existingSecretId, String name, String secretType, String plainText);

    /**
     * 读取明文（**仅内部调用**，如发起厂商 API 请求时）。
     *
     * @return 明文；记录不存在时返回 null
     */
    String readPlain(Long secretId);

    /**
     * 读取掩码提示（可安全回传前端）。
     */
    String maskedHint(Long secretId);
}
