package com.insightengine.workspace.support;

import com.insightengine.common.core.BizException;
import com.insightengine.common.core.ErrorCode;
import com.insightengine.starter.web.context.UserContext;
import com.insightengine.workspace.constant.WorkspaceConstants;

/**
 * 租户/组织归属守卫（2026-09-17 code review Y2 收口）。
 *
 * <h3>为什么需要</h3>
 * <p>按主键取到资源就操作，是"水平越权"的经典入口：多租户下，A 租户的组织级用户只要拿到 B 租户的空间 id，
 * 就能更新/删除它。当前 MVP 单租户（且 {@code ws:delete} 仅超管持有）不暴露，但**多租户/多组织前必须补**，
 * 且属于"越早补越便宜"的一类约束（晚补要回头逐个接口补）。</p>
 *
 * <h3>失败口径</h3>
 * <p>跨租户一律按 <b>1004「不存在」</b>处理，而不是 403：403 等于告诉调用者"这个 id 存在，只是不给你操作"，
 * 反而泄露了其它租户的资源存在性（探测面）。</p>
 *
 * <p>注：跨租户的**平台超管**能力（scope=ALL 跨租户运维）留待多租户阶段按 {@code ie_role.scope} 放行（TD §7.5）。</p>
 */
public final class TenantGuard {

    private TenantGuard() {
        // 工具类禁止实例化
    }

    /**
     * 当前请求所属租户；上下文缺失时兜底默认租户（MVP 单租户，与 UMS 注册口径一致）。
     */
    public static Long currentTenantId() {
        Long tenantId = UserContext.getTenantId();
        return tenantId != null ? tenantId : WorkspaceConstants.DEFAULT_TENANT_ID;
    }

    /**
     * 校验资源租户归属，不一致抛 1004（口径：按"不存在"处理，不泄露存在性）。
     *
     * @param resourceTenantId 资源的 tenant_id（为空视为不校验，兼容历史数据）
     * @param resourceName     资源中文名（用于提示，如"工作空间"）
     */
    public static void assertSameTenant(Long resourceTenantId, String resourceName) {
        if (resourceTenantId == null) {
            return;
        }
        if (!resourceTenantId.equals(currentTenantId())) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, resourceName + "不存在");
        }
    }
}
