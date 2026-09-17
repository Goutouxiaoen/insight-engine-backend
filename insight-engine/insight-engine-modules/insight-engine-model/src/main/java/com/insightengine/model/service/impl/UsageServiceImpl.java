package com.insightengine.model.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.insightengine.common.core.PageResult;
import com.insightengine.model.constant.ModelConstants;
import com.insightengine.model.dto.request.UsagePageQuery;
import com.insightengine.model.dto.response.UsageRecordVO;
import com.insightengine.model.entity.Model;
import com.insightengine.model.entity.UsageRecord;
import com.insightengine.model.mapper.ModelMapper;
import com.insightengine.model.mapper.UsageRecordMapper;
import com.insightengine.model.service.UsageService;
import com.insightengine.starter.web.context.UserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 用量查询服务实现（IF §7.8）。
 *
 * <h3>可见范围规则（本轮定稿，已回写 IF §7.8）</h3>
 * <p>实测权限授予：`model:usage:read` 由 <b>super_admin / org_admin / ws_admin</b> 三者持有
 * —— 若只按"有权限即看全量"，**空间管理员就能看到全平台所有空间的用量**（跨空间信息泄露）。
 * 故实现为：</p>
 * <ul>
 *   <li>调用者持 <b>`org:write`</b>（组织级能力，判据取自 token 的 perms —— 与 IF §5.4
 *       "组织级可查看组织内全部空间"同一口径）→ **看全量**；</li>
 *   <li>否则（如 `ws_admin`）→ **强制追加 `scope_type=WORKSPACE AND scope_id=当前空间`**；</li>
 *   <li>当前空间缺失（组织级成员无 `ws_id`）且又非组织级 → 返回空页（而不是"回退看全量"，避免越权）。</li>
 * </ul>
 *
 * <p>时间范围按**自然日闭区间**换算：{@code start 00:00:00} ~ {@code end 次日 00:00:00}（左闭右开），
 * 这样"结束日当天"的用量能被包含进来。</p>
 */
@Service
public class UsageServiceImpl implements UsageService {

    private static final Logger log = LoggerFactory.getLogger(UsageServiceImpl.class);

    /** 组织级能力权限码（判据与 IF §5.4 一致） */
    private static final String PERM_ORG_WRITE = "org:write";

    private final UsageRecordMapper usageRecordMapper;
    private final ModelMapper modelMapper;

    public UsageServiceImpl(UsageRecordMapper usageRecordMapper, ModelMapper modelMapper) {
        this.usageRecordMapper = usageRecordMapper;
        this.modelMapper = modelMapper;
    }

    @Override
    public PageResult<UsageRecordVO> page(UsagePageQuery query) {
        int pageNum = query.getSafePageNum();
        int pageSize = query.getSafePageSize();

        LambdaQueryWrapper<UsageRecord> wrapper = new LambdaQueryWrapper<>();
        // 只查模型用量（同一张表还承载 TOOL/AGENT/KB 等业务类型）
        wrapper.eq(UsageRecord::getBizType, ModelConstants.USAGE_BIZ_MODEL);
        if (query.getModelId() != null) {
            wrapper.eq(UsageRecord::getRefId, query.getModelId());
        }
        LocalDate start = query.getStart();
        if (start != null) {
            wrapper.ge(UsageRecord::getCreatedAt, start.atStartOfDay());
        }
        LocalDate end = query.getEnd();
        if (end != null) {
            // 闭区间：< 次日 00:00
            wrapper.lt(UsageRecord::getCreatedAt, end.plusDays(1).atStartOfDay());
        }
        applyVisibilityScope(wrapper);
        wrapper.orderByDesc(UsageRecord::getId);

        IPage<UsageRecord> page = usageRecordMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<UsageRecord> records = page.getRecords();
        Map<Long, Model> modelMap = loadModels(records);
        List<UsageRecordVO> vos = records.stream().map(r -> toVO(r, modelMap)).toList();
        return PageResult.of(vos, page.getTotal(), pageNum, pageSize);
    }

    /* ==================== 私有方法 ==================== */

    /**
     * 可见范围收敛（详见类注释）。
     */
    private void applyVisibilityScope(LambdaQueryWrapper<UsageRecord> wrapper) {
        if (callerIsOrgLevel()) {
            return;
        }
        Long workspaceId = UserContext.getWorkspaceId();
        if (workspaceId == null) {
            // 非组织级又没有当前空间：只能看到"零条"，绝不回退为全量
            log.warn("非组织级调用者缺少当前空间（ws_id），用量查询返回空集以防越权");
            wrapper.eq(UsageRecord::getId, -1L);
            return;
        }
        wrapper.eq(UsageRecord::getScopeType, ModelConstants.USAGE_SCOPE_WORKSPACE);
        wrapper.eq(UsageRecord::getScopeId, workspaceId);
    }

    /**
     * 调用者是否组织级：以 token 的 {@code perms} 是否含 {@code org:write} 为判据。
     *
     * <p>为什么用 token 权限而不是查库：`org:*` 是**组织级能力**（不属于任何空间），
     * token 的 perms 正是"用户级能力"的载体（IF §3.0），此处语义完全吻合，且免一次跨库查询。</p>
     */
    private boolean callerIsOrgLevel() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> PERM_ORG_WRITE.equals(a.getAuthority()));
    }

    /**
     * 批量加载本次页内的模型（一次 IN 查询，避免 N+1）。
     */
    private Map<Long, Model> loadModels(List<UsageRecord> records) {
        List<Long> modelIds = records.stream()
                .map(UsageRecord::getRefId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (modelIds.isEmpty()) {
            return Map.of();
        }
        return modelMapper.selectBatchIds(modelIds).stream()
                .collect(Collectors.toMap(Model::getId, Function.identity(), (a, b) -> a));
    }

    private UsageRecordVO toVO(UsageRecord record, Map<Long, Model> modelMap) {
        UsageRecordVO vo = new UsageRecordVO();
        vo.setId(record.getId());
        vo.setModelId(record.getRefId());
        Model model = record.getRefId() == null ? null : modelMap.get(record.getRefId());
        if (model != null) {
            vo.setModelCode(model.getCode());
            vo.setModelName(model.getDisplayName());
        }
        vo.setScopeType(record.getScopeType());
        vo.setScopeId(record.getScopeId());
        vo.setTokens(record.getQuantity());
        vo.setCost(record.getCost());
        vo.setTraceId(record.getTraceId());
        vo.setCreatedAt(record.getCreatedAt());
        return vo;
    }
}
