package com.insightengine.model.service;

import com.insightengine.model.dto.request.RoutePolicyCreateRequest;
import com.insightengine.model.dto.request.RoutePolicyStatusRequest;
import com.insightengine.model.dto.request.RoutePolicyUpdateRequest;
import com.insightengine.model.dto.response.RoutePolicyVO;

import java.util.List;

/**
 * 模型路由策略服务（IF §7.4）。
 *
 * <p>策略是**平台/组织级**资源（权限 `model:route:read/write` 仅超管与组织管理员持有），
 * 因此只做第一层动作门控，不做空间维度判定。</p>
 */
public interface RoutePolicyService {

    /**
     * 策略列表（按优先级升序 = 匹配顺序，便于前端直接展示"路由顺序"）。
     */
    List<RoutePolicyVO> list();

    /**
     * 创建策略（rules 结构在写入时校验）。
     *
     * @return 新策略 ID
     */
    Long create(RoutePolicyCreateRequest request);

    /**
     * 更新策略（仅非空字段；rules 传入即整体替换并重新校验）。
     */
    void update(Long id, RoutePolicyUpdateRequest request);

    /**
     * 启用/停用策略。
     */
    void updateStatus(Long id, RoutePolicyStatusRequest request);
}
