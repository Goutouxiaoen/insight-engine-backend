package com.insightengine.model.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.insightengine.common.core.BizException;
import com.insightengine.common.core.ErrorCode;
import com.insightengine.common.core.PageResult;
import com.insightengine.model.dto.request.ModelCreateRequest;
import com.insightengine.model.dto.request.ModelPageQuery;
import com.insightengine.model.dto.request.ModelUpdateRequest;
import com.insightengine.model.dto.response.ModelVO;
import com.insightengine.model.entity.Model;
import com.insightengine.model.entity.ModelVendor;
import com.insightengine.model.mapper.ModelMapper;
import com.insightengine.model.mapper.ModelVendorMapper;
import com.insightengine.model.service.ModelService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 模型目录服务实现（IF §7.2 / §7.3）。
 *
 * <p>要点：</p>
 * <ul>
 *   <li><b>外键有效性</b>：创建时校验 {@code vendorId} 存在，避免"模型指向不存在的厂商"（孤儿数据）；</li>
 *   <li><b>唯一性</b>：{@code (vendorId, code)} 业务层先查一次（DB 唯一索引 {@code uk_model_vendor_code} 兜底）；</li>
 *   <li><b>厂商信息冗余回显</b>：分页时批量取涉及的厂商（一次 IN 查询，避免 N+1）；</li>
 *   <li><b>不可改字段</b>：{@code vendorId}/{@code code} 不允许更新 —— 历史用量按 {@code modelId} 归属，
 *       改 code 会让旧数据指向一个"语义已变"的模型。</li>
 * </ul>
 */
@Service
public class ModelServiceImpl implements ModelService {

    private final ModelMapper modelMapper;
    private final ModelVendorMapper vendorMapper;

    public ModelServiceImpl(ModelMapper modelMapper, ModelVendorMapper vendorMapper) {
        this.modelMapper = modelMapper;
        this.vendorMapper = vendorMapper;
    }

    @Override
    public PageResult<ModelVO> page(ModelPageQuery query) {
        int pageNum = query.getSafePageNum();
        int pageSize = query.getSafePageSize();

        LambdaQueryWrapper<Model> wrapper = new LambdaQueryWrapper<>();
        if (query.getVendorId() != null) {
            wrapper.eq(Model::getVendorId, query.getVendorId());
        }
        if (StringUtils.hasText(query.getType())) {
            wrapper.eq(Model::getType, query.getType().trim());
        }
        if (query.getEnabled() != null) {
            wrapper.eq(Model::getEnabled, query.getEnabled());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like(Model::getCode, keyword)
                    .or().like(Model::getDisplayName, keyword));
        }
        wrapper.orderByAsc(Model::getVendorId).orderByAsc(Model::getId);

        IPage<Model> page = modelMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<Model> records = page.getRecords();

        // 批量取厂商（一次 IN 查询，避免逐行查引发出 N+1）
        Map<Long, ModelVendor> vendorMap = loadVendors(records);
        List<ModelVO> vos = records.stream().map(m -> toVO(m, vendorMap)).toList();
        return PageResult.of(vos, page.getTotal(), pageNum, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ModelCreateRequest request) {
        ModelVendor vendor = vendorMapper.selectById(request.getVendorId());
        if (vendor == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "厂商不存在");
        }
        String code = request.getCode().trim();
        Long existCount = modelMapper.selectCount(new LambdaQueryWrapper<Model>()
                .eq(Model::getVendorId, request.getVendorId())
                .eq(Model::getCode, code));
        if (existCount != null && existCount > 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "同一厂商下模型编码已存在");
        }

        Model model = new Model();
        model.setVendorId(request.getVendorId());
        model.setCode(code);
        model.setDisplayName(StringUtils.hasText(request.getDisplayName())
                ? request.getDisplayName().trim() : code);
        model.setType(request.getType().trim());
        model.setContextWindow(request.getContextWindow());
        model.setInputPricePer1k(request.getInputPricePer1k());
        model.setOutputPricePer1k(request.getOutputPricePer1k());
        model.setEnabled(1);
        modelMapper.insert(model);
        return model.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ModelUpdateRequest request) {
        requireModel(id);
        Model update = new Model();
        update.setId(id);
        if (StringUtils.hasText(request.getDisplayName())) {
            update.setDisplayName(request.getDisplayName().trim());
        }
        if (StringUtils.hasText(request.getType())) {
            update.setType(request.getType().trim());
        }
        if (request.getContextWindow() != null) {
            update.setContextWindow(request.getContextWindow());
        }
        if (request.getInputPricePer1k() != null) {
            update.setInputPricePer1k(request.getInputPricePer1k());
        }
        if (request.getOutputPricePer1k() != null) {
            update.setOutputPricePer1k(request.getOutputPricePer1k());
        }
        if (request.getEnabled() != null) {
            update.setEnabled(request.getEnabled());
        }
        modelMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireModel(id);
        modelMapper.deleteById(id);
    }

    /* ==================== 私有方法 ==================== */

    private Model requireModel(Long id) {
        Model model = modelMapper.selectById(id);
        if (model == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "模型不存在");
        }
        return model;
    }

    /**
     * 批量加载当前页涉及的厂商（去重后一次查询）。
     */
    private Map<Long, ModelVendor> loadVendors(List<Model> models) {
        List<Long> vendorIds = models.stream().map(Model::getVendorId).distinct().toList();
        if (vendorIds.isEmpty()) {
            return Map.of();
        }
        return vendorMapper.selectBatchIds(vendorIds).stream()
                .collect(Collectors.toMap(ModelVendor::getId, Function.identity(), (a, b) -> a));
    }

    private ModelVO toVO(Model model, Map<Long, ModelVendor> vendorMap) {
        ModelVO vo = new ModelVO();
        vo.setId(model.getId());
        vo.setVendorId(model.getVendorId());
        ModelVendor vendor = vendorMap.get(model.getVendorId());
        if (vendor != null) {
            vo.setVendorCode(vendor.getCode());
            vo.setVendorName(vendor.getName());
        }
        vo.setCode(model.getCode());
        vo.setDisplayName(model.getDisplayName());
        vo.setType(model.getType());
        vo.setContextWindow(model.getContextWindow());
        vo.setInputPricePer1k(model.getInputPricePer1k());
        vo.setOutputPricePer1k(model.getOutputPricePer1k());
        vo.setEnabled(model.getEnabled());
        vo.setCreatedAt(model.getCreatedAt());
        vo.setUpdatedAt(model.getUpdatedAt());
        return vo;
    }
}
