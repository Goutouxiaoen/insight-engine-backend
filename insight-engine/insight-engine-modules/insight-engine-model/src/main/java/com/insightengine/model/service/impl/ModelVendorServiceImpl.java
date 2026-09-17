package com.insightengine.model.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.insightengine.common.core.BizException;
import com.insightengine.common.core.ErrorCode;
import com.insightengine.common.core.PageResult;
import com.insightengine.model.constant.ModelConstants;
import com.insightengine.model.dto.request.VendorCreateRequest;
import com.insightengine.model.dto.request.VendorPageQuery;
import com.insightengine.model.dto.request.VendorUpdateRequest;
import com.insightengine.model.dto.response.VendorVO;
import com.insightengine.model.entity.ModelVendor;
import com.insightengine.model.mapper.ModelVendorMapper;
import com.insightengine.model.service.ModelVendorService;
import com.insightengine.model.service.SecretStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 模型厂商服务实现（IF §7.1）。
 *
 * <p>安全与一致性要点：</p>
 * <ul>
 *   <li><b>明文不落地</b>：{@code apiKey} 只在内存中传给 {@link SecretStore}，加密后写 {@code ie_secret}，
 *       厂商表只存 {@code apiKeySecretId}；</li>
 *   <li><b>更新不误伤密钥</b>：{@code apiKey} 留空 = 不修改（前端回显的是掩码，若照原样提交会把密钥写成假值）；</li>
 *   <li><b>先写密钥再写厂商</b>：同一事务内先落密文、再挂引用，避免出现"厂商指向不存在的密钥"的悬空引用；</li>
 *   <li><b>不删密钥记录</b>：删除厂商只逻辑删除厂商行，密钥行保留（审计需要；密钥本身已加密）。</li>
 * </ul>
 */
@Service
public class ModelVendorServiceImpl implements ModelVendorService {

    private final ModelVendorMapper vendorMapper;
    private final SecretStore secretStore;

    public ModelVendorServiceImpl(ModelVendorMapper vendorMapper, SecretStore secretStore) {
        this.vendorMapper = vendorMapper;
        this.secretStore = secretStore;
    }

    @Override
    public PageResult<VendorVO> page(VendorPageQuery query) {
        int pageNum = query.getSafePageNum();
        int pageSize = query.getSafePageSize();

        LambdaQueryWrapper<ModelVendor> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like(ModelVendor::getCode, keyword)
                    .or().like(ModelVendor::getName, keyword));
        }
        if (StringUtils.hasText(query.getType())) {
            wrapper.eq(ModelVendor::getType, query.getType().trim());
        }
        if (query.getEnabled() != null) {
            wrapper.eq(ModelVendor::getEnabled, query.getEnabled());
        }
        wrapper.orderByAsc(ModelVendor::getId);

        IPage<ModelVendor> page = vendorMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<VendorVO> records = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(records, page.getTotal(), pageNum, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(VendorCreateRequest request) {
        String code = request.getCode().trim();
        Long existCount = vendorMapper.selectCount(
                new LambdaQueryWrapper<ModelVendor>().eq(ModelVendor::getCode, code));
        if (existCount != null && existCount > 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "厂商编码已存在");
        }

        ModelVendor vendor = new ModelVendor();
        vendor.setCode(code);
        vendor.setName(request.getName().trim());
        vendor.setBaseUrl(request.getBaseUrl().trim());
        vendor.setType(request.getType().trim());
        vendor.setEnabled(1);
        vendor.setConfig(request.getConfig());
        // 先落密文、再挂引用（同事务）：apiKey 为空则视为"无鉴权的本地模型"（如 Ollama）
        if (StringUtils.hasText(request.getApiKey())) {
            vendor.setApiKeySecretId(secretStore.save(null, buildSecretName(vendor.getName()),
                    ModelConstants.SECRET_TYPE_MODEL_API_KEY, request.getApiKey().trim()));
        }
        vendorMapper.insert(vendor);
        return vendor.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, VendorUpdateRequest request) {
        ModelVendor vendor = requireVendor(id);

        ModelVendor update = new ModelVendor();
        update.setId(id);
        if (StringUtils.hasText(request.getName())) {
            update.setName(request.getName().trim());
        }
        if (StringUtils.hasText(request.getBaseUrl())) {
            update.setBaseUrl(request.getBaseUrl().trim());
        }
        if (StringUtils.hasText(request.getType())) {
            update.setType(request.getType().trim());
        }
        if (request.getEnabled() != null) {
            update.setEnabled(request.getEnabled());
        }
        if (request.getConfig() != null) {
            update.setConfig(request.getConfig());
        }
        // 仅当显式传了新 Key 才动密钥；复用同一行 ie_secret，保持 api_key_secret_id 引用稳定
        if (StringUtils.hasText(request.getApiKey())) {
            String secretName = buildSecretName(
                    StringUtils.hasText(request.getName()) ? request.getName().trim() : vendor.getName());
            update.setApiKeySecretId(secretStore.save(vendor.getApiKeySecretId(), secretName,
                    ModelConstants.SECRET_TYPE_MODEL_API_KEY, request.getApiKey().trim()));
        }
        vendorMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireVendor(id);
        // 逻辑删除厂商；密钥行保留（审计），不做物理清理
        vendorMapper.deleteById(id);
    }

    /* ==================== 私有方法 ==================== */

    /**
     * 密钥展示名：{@code <厂商名>-API Key}（便于在密钥表中识别人工可读的来源）。
     */
    private String buildSecretName(String vendorName) {
        return (StringUtils.hasText(vendorName) ? vendorName : "未命名厂商") + "-API Key";
    }

    private ModelVendor requireVendor(Long id) {
        ModelVendor vendor = vendorMapper.selectById(id);
        if (vendor == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "模型厂商不存在");
        }
        return vendor;
    }

    /**
     * 实体转 VO：**只带掩码，不带明文**。
     */
    private VendorVO toVO(ModelVendor vendor) {
        VendorVO vo = new VendorVO();
        vo.setId(vendor.getId());
        vo.setCode(vendor.getCode());
        vo.setName(vendor.getName());
        vo.setBaseUrl(vendor.getBaseUrl());
        vo.setType(vendor.getType());
        vo.setEnabled(vendor.getEnabled());
        vo.setConfig(vendor.getConfig());
        vo.setHasApiKey(vendor.getApiKeySecretId() != null);
        vo.setMaskedHint(secretStore.maskedHint(vendor.getApiKeySecretId()));
        vo.setCreatedAt(vendor.getCreatedAt());
        vo.setUpdatedAt(vendor.getUpdatedAt());
        return vo;
    }
}
