package com.insightengine.workspace.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.insightengine.common.core.BizException;
import com.insightengine.common.core.ErrorCode;
import com.insightengine.common.constant.Constants;
import com.insightengine.starter.web.context.UserContext;
import com.insightengine.workspace.constant.WorkspaceConstants;
import com.insightengine.workspace.dto.request.OrgCreateRequest;
import com.insightengine.workspace.dto.response.OrgVO;
import com.insightengine.workspace.entity.Organization;
import com.insightengine.workspace.mapper.OrganizationMapper;
import com.insightengine.workspace.service.OrgService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 组织服务实现。
 */
@Service
public class OrgServiceImpl implements OrgService {

    private final OrganizationMapper organizationMapper;

    public OrgServiceImpl(OrganizationMapper organizationMapper) {
        this.organizationMapper = organizationMapper;
    }

    /**
     * 创建组织：编码同租户内唯一 → 落库（创建者即所有者 + 正常状态）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(OrgCreateRequest request) {
        Long tenantId = currentTenantId();
        String code = request.getCode().trim();

        // 编码唯一性：先查友好提示，DB 唯一索引（uk_org_code_tenant）兜底防并发
        Long existCount = organizationMapper.selectCount(new LambdaQueryWrapper<Organization>()
                .eq(Organization::getTenantId, tenantId)
                .eq(Organization::getCode, code));
        if (existCount != null && existCount > 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "该组织编码已存在");
        }

        Organization org = new Organization();
        org.setTenantId(tenantId);
        org.setName(request.getName().trim());
        org.setCode(code);
        org.setOwnerId(UserContext.getUserId());
        org.setStatus(Constants.STATUS_ENABLED);
        organizationMapper.insert(org);
        return org.getId();
    }

    /**
     * 组织详情。
     */
    @Override
    public OrgVO detail(Long id) {
        Organization org = organizationMapper.selectById(id);
        if (org == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "组织不存在");
        }
        OrgVO vo = new OrgVO();
        vo.setId(org.getId());
        vo.setTenantId(org.getTenantId());
        vo.setName(org.getName());
        vo.setCode(org.getCode());
        vo.setOwnerId(org.getOwnerId());
        vo.setStatus(org.getStatus());
        vo.setCreatedAt(org.getCreatedAt());
        return vo;
    }

    /**
     * 当前租户 ID：取自 JWT 载荷；缺失（异常数据）时兜底 MVP 单租户默认值。
     */
    private Long currentTenantId() {
        Long tenantId = UserContext.getTenantId();
        return tenantId == null ? WorkspaceConstants.DEFAULT_TENANT_ID : tenantId;
    }
}
