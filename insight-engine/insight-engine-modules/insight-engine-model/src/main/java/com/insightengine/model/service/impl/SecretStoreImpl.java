package com.insightengine.model.service.impl;

import com.insightengine.common.core.BizException;
import com.insightengine.common.core.ErrorCode;
import com.insightengine.model.constant.ModelConstants;
import com.insightengine.model.entity.SecretRecord;
import com.insightengine.model.mapper.SecretMapper;
import com.insightengine.model.service.SecretStore;
import com.insightengine.model.support.SecretCipher;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 敏感凭据存取实现。
 *
 * <p>关键约束（TD §16.1 / IF §7.1）：</p>
 * <ul>
 *   <li>落库只有密文 + IV + 算法 + KEK 版本，<b>明文永不落库</b>；</li>
 *   <li>掩码（{@code maskedHint}）在写入时一并算好，读取路径不必解密 —— 列表接口无需触碰明文；</li>
 *   <li>更新已有密钥时**复用同一行**（保持 {@code vendor.api_key_secret_id} 引用稳定，
 *       也避免"每次改 Key 都留一行旧密文"的堆积）；</li>
 *   <li>本类不打印任何密钥内容（含长度以外的信息）。</li>
 * </ul>
 */
@Service
public class SecretStoreImpl implements SecretStore {

    private final SecretMapper secretMapper;
    private final SecretCipher secretCipher;

    public SecretStoreImpl(SecretMapper secretMapper, SecretCipher secretCipher) {
        this.secretMapper = secretMapper;
        this.secretCipher = secretCipher;
    }

    @Override
    public Long save(Long existingSecretId, String name, String secretType, String plainText) {
        if (!StringUtils.hasText(plainText)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "密钥内容不能为空");
        }
        SecretCipher.Encrypted encrypted = secretCipher.encrypt(plainText);
        String masked = SecretCipher.mask(plainText);

        if (existingSecretId != null) {
            SecretRecord exist = secretMapper.selectById(existingSecretId);
            if (exist == null) {
                throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "密钥记录不存在");
            }
            SecretRecord update = new SecretRecord();
            update.setId(existingSecretId);
            update.setName(name);
            update.setSecretType(secretType);
            update.setCipherText(encrypted.cipherText());
            update.setIv(encrypted.iv());
            update.setAlgo(encrypted.algo());
            update.setKekVersion(encrypted.kekVersion());
            update.setMaskedHint(masked);
            update.setEnabled(1);
            secretMapper.updateById(update);
            return existingSecretId;
        }

        SecretRecord record = new SecretRecord();
        // 平台级密钥：tenant_id=0（全租户共用，2026-09-17 裁决路线 A）
        record.setTenantId(ModelConstants.PLATFORM_TENANT_ID);
        record.setName(name);
        record.setSecretType(secretType);
        record.setCipherText(encrypted.cipherText());
        record.setIv(encrypted.iv());
        record.setAlgo(encrypted.algo());
        record.setKekVersion(encrypted.kekVersion());
        record.setMaskedHint(masked);
        record.setEnabled(1);
        secretMapper.insert(record);
        return record.getId();
    }

    @Override
    public String readPlain(Long secretId) {
        if (secretId == null) {
            return null;
        }
        SecretRecord record = secretMapper.selectById(secretId);
        if (record == null) {
            return null;
        }
        return secretCipher.decrypt(record.getCipherText(), record.getIv());
    }

    @Override
    public String maskedHint(Long secretId) {
        if (secretId == null) {
            return null;
        }
        SecretRecord record = secretMapper.selectById(secretId);
        return record == null ? null : record.getMaskedHint();
    }
}
