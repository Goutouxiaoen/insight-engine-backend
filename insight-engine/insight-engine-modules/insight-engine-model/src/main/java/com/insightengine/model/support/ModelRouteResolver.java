package com.insightengine.model.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insightengine.common.core.BizException;
import com.insightengine.common.core.ErrorCode;
import com.insightengine.model.constant.ModelConstants;
import com.insightengine.model.entity.Model;
import com.insightengine.model.entity.RoutePolicy;
import com.insightengine.model.mapper.ModelMapper;
import com.insightengine.model.mapper.RoutePolicyMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 模型路由解析器（IF §7.4）：把逻辑模型名 {@code auto} 翻译成**有序的具体模型列表**（主 + 备）。
 *
 * <h3>rules 结构（本轮定稿，已回写 IF §7.4）</h3>
 * <pre>
 * {
 *   "strategy": "PRIORITY",                                  // 可选，默认 PRIORITY（MVP 仅支持）
 *   "fallback": true,                                        // 可选，默认 true（主失败则试备；**仅非流式生效**）
 *   "rules": [                                               // 有序：先匹配先生效
 *     { "match": { "tenantId": 1, "workspaceId": 15 },       // match 可空/缺省 = 匹配全部
 *       "targets": [ { "modelId": 3 }, { "modelId": 8 } ] }  // targets 有序：第 1 个为主，其余为备
 *   ]
 * }
 * </pre>
 *
 * <h3>解析顺序</h3>
 * <ol>
 *   <li>取启用中的策略，按 {@code priority 升序、id 升序}；</li>
 *   <li>逐条策略逐条规则匹配 {@code match}（缺省=全匹配）；</li>
 *   <li>命中后按 {@code targets} 顺序过滤出**存在、启用、类型为 CHAT** 的模型，返回有序 modelId 列表；</li>
 *   <li>全部未命中 → 返回空列表（由调用方决定兜底行为）。</li>
 * </ol>
 *
 * <p><b>写入口径校验</b>：{@link #validate} 在写入时解析一遍，拒绝"字段写错但能入库、调用时才炸"的策略。
 * 其中 {@code tenantTier} 这类**当前落不了地的 match 键**明确报错并提示（IF §7.4 示例曾用它，
 * 但租户套餐字段尚未落地，见 PROGRESS §6.3）——宁可写入失败，也不要"配了以为生效"。</p>
 */
@Component
public class ModelRouteResolver {

    private static final Logger log = LoggerFactory.getLogger(ModelRouteResolver.class);

    /** 支持的 match 键（其余一律拒绝，避免"配了不生效"） */
    private static final Set<String> SUPPORTED_MATCH_KEYS = Set.of("tenantId", "workspaceId");

    private static final String STRATEGY_PRIORITY = "PRIORITY";

    private final RoutePolicyMapper routePolicyMapper;
    private final ModelMapper modelMapper;
    private final ObjectMapper objectMapper;

    public ModelRouteResolver(RoutePolicyMapper routePolicyMapper,
                              ModelMapper modelMapper,
                              ObjectMapper objectMapper) {
        this.routePolicyMapper = routePolicyMapper;
        this.modelMapper = modelMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 解析逻辑模型 {@code auto} 的目标模型（有序：主 + 备）。
     *
     * @return 命中结果；{@link RouteDecision#modelIds()} 为空表示未命中任何策略（调用方自行兜底）
     */
    public RouteDecision resolve(Long tenantId, Long workspaceId) {
        List<RoutePolicy> policies = routePolicyMapper.selectList(new LambdaQueryWrapper<RoutePolicy>()
                .eq(RoutePolicy::getEnabled, 1)
                .orderByAsc(RoutePolicy::getPriority)
                .orderByAsc(RoutePolicy::getId));
        for (RoutePolicy policy : policies) {
            RouteDecision decision = matchPolicy(policy, tenantId, workspaceId);
            if (!decision.modelIds().isEmpty()) {
                log.info("路由命中：policyId={} name={} priority={} fallback={} targets={}",
                        policy.getId(), policy.getName(), policy.getPriority(),
                        decision.fallback(), decision.modelIds());
                return decision;
            }
        }
        return new RouteDecision(List.of(), true);
    }

    /**
     * 路由决策：有序目标模型（首个为主、其余为备）+ 是否允许降级。
     */
    public record RouteDecision(List<Long> modelIds, boolean fallback) {
    }

    /**
     * 校验 rules 结构（写入口径，IF §7.4）。
     */
    public void validate(JsonNode rules) {
        JsonNode rulesNode = parseAndValidateShape(rules);
        for (JsonNode rule : rulesNode) {
            validateMatch(rule.path("match"));
        }
    }

    /**
     * 序列化为入库字符串（同时再校验一次，避免绕过）。
     */
    public String writeValue(JsonNode rules) {
        validate(rules);
        try {
            return objectMapper.writeValueAsString(rules);
        } catch (Exception e) {
            throw new BizException(ErrorCode.PARAM_ERROR, "规则序列化失败");
        }
    }

    /**
     * 读取时反序列化为 JsonNode（入库异常时按"无规则"处理并告警，不阻断列表接口）。
     */
    public JsonNode readValue(String rulesJson) {
        if (!StringUtils.hasText(rulesJson)) {
            return null;
        }
        try {
            return objectMapper.readTree(rulesJson);
        } catch (Exception e) {
            log.warn("路由策略 rules 不是合法 JSON，按空规则处理：{}",
                    rulesJson.substring(0, Math.min(120, rulesJson.length())));
            return null;
        }
    }

    /* ==================== 内部实现 ==================== */

    private RouteDecision matchPolicy(RoutePolicy policy, Long tenantId, Long workspaceId) {
        JsonNode root;
        try {
            root = objectMapper.readTree(policy.getRules());
        } catch (Exception e) {
            log.warn("跳过无法解析的路由策略：policyId={}", policy.getId());
            return new RouteDecision(List.of(), true);
        }
        if (root == null || !root.isObject()) {
            return new RouteDecision(List.of(), true);
        }
        String strategy = root.path("strategy").asText(STRATEGY_PRIORITY);
        if (!STRATEGY_PRIORITY.equalsIgnoreCase(strategy)) {
            // MVP 只实现 PRIORITY；未知策略跳过并告警（而不是静默当成命中）
            log.warn("跳过未支持的路由策略类型：policyId={} strategy={}", policy.getId(), strategy);
            return new RouteDecision(List.of(), true);
        }
        boolean fallback = root.path("fallback").asBoolean(true);
        JsonNode rulesNode = root.path("rules");
        if (!rulesNode.isArray()) {
            return new RouteDecision(List.of(), fallback);
        }
        List<Long> orderedTargets = new ArrayList<>();
        for (JsonNode rule : rulesNode) {
            if (!matches(rule.path("match"), tenantId, workspaceId)) {
                continue;
            }
            JsonNode targets = rule.path("targets");
            if (targets.isArray()) {
                for (JsonNode target : targets) {
                    if (target.path("modelId").isNumber()) {
                        orderedTargets.add(target.path("modelId").asLong());
                    }
                }
            }
            // 命中一条规则即停止（"先匹配先生效"）
            break;
        }
        if (orderedTargets.isEmpty()) {
            return new RouteDecision(List.of(), fallback);
        }
        return new RouteDecision(filterUsableModels(orderedTargets), fallback);
    }

    /**
     * 过滤出可用模型（存在 + 启用 + CHAT），并保持配置顺序（主/备次序不能乱）。
     */
    private List<Long> filterUsableModels(List<Long> orderedTargetIds) {
        List<Model> models = modelMapper.selectList(new LambdaQueryWrapper<Model>()
                .in(Model::getId, orderedTargetIds)
                .eq(Model::getEnabled, 1));
        var usableMap = models.stream()
                .filter(m -> ModelConstants.TYPE_CHAT.equals(m.getType()))
                .collect(Collectors.toMap(Model::getId, m -> m, (a, b) -> a));
        List<Long> result = new ArrayList<>();
        for (Long id : orderedTargetIds) {
            if (usableMap.containsKey(id)) {
                if (!result.contains(id)) {
                    result.add(id);
                }
            } else {
                log.warn("路由目标模型不可用（不存在/已停用/非 CHAT），跳过：modelId={}", id);
            }
        }
        return result;
    }

    /**
     * match 匹配：缺省（无 match 或空对象）= 全匹配；给定键则须与上下文一致。
     */
    private boolean matches(JsonNode match, Long tenantId, Long workspaceId) {
        if (match.isMissingNode() || match.isNull() || !match.isObject() || match.isEmpty()) {
            return true;
        }
        if (match.hasNonNull("tenantId")) {
            long expectedTenant = match.path("tenantId").asLong();
            if (tenantId == null || tenantId != expectedTenant) {
                return false;
            }
        }
        if (match.hasNonNull("workspaceId")) {
            long expectedWorkspace = match.path("workspaceId").asLong();
            if (workspaceId == null || workspaceId != expectedWorkspace) {
                return false;
            }
        }
        return true;
    }

    /**
     * 结构校验：strategy / rules / targets / modelId。
     */
    private JsonNode parseAndValidateShape(JsonNode rules) {
        if (rules == null || !rules.isObject()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "rules 必须是 JSON 对象");
        }
        String strategy = rules.path("strategy").asText(STRATEGY_PRIORITY);
        if (!STRATEGY_PRIORITY.equalsIgnoreCase(strategy)) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    "暂不支持的路由策略类型：" + strategy + "（当前仅支持 PRIORITY）");
        }
        JsonNode rulesNode = rules.path("rules");
        if (!rulesNode.isArray() || rulesNode.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "rules.rules 必须是非空数组");
        }
        for (JsonNode rule : rulesNode) {
            JsonNode targets = rule.path("targets");
            if (!targets.isArray() || targets.isEmpty()) {
                throw new BizException(ErrorCode.PARAM_ERROR, "每条规则必须配置非空 targets");
            }
            for (JsonNode target : targets) {
                if (!target.path("modelId").isNumber() || target.path("modelId").asLong() <= 0) {
                    throw new BizException(ErrorCode.PARAM_ERROR, "targets 中的 modelId 必须是正整数");
                }
            }
        }
        return rulesNode;
    }

    private void validateMatch(JsonNode match) {
        if (match.isMissingNode() || match.isNull()) {
            return;
        }
        if (!match.isObject()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "match 必须是 JSON 对象");
        }
        match.fieldNames().forEachRemaining(key -> {
            if (!SUPPORTED_MATCH_KEYS.contains(key)) {
                throw new BizException(ErrorCode.PARAM_ERROR,
                        "暂不支持匹配条件：" + key + "（当前仅支持 tenantId / workspaceId；"
                                + "tenantTier 等待租户套餐字段落地，见 PROGRESS §6.3）");
            }
        });
    }
}
