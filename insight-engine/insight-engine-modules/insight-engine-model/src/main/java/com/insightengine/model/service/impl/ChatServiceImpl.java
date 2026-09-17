package com.insightengine.model.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.insightengine.common.core.BizException;
import com.insightengine.common.core.ErrorCode;
import com.insightengine.model.constant.ModelConstants;
import com.insightengine.model.dto.request.ChatCompletionRequest;
import com.insightengine.model.dto.response.ChatCompletionVO;
import com.insightengine.model.entity.Model;
import com.insightengine.model.entity.ModelVendor;
import com.insightengine.model.entity.UsageRecord;
import com.insightengine.model.mapper.ModelMapper;
import com.insightengine.model.mapper.ModelVendorMapper;
import com.insightengine.model.mapper.UsageRecordMapper;
import com.insightengine.model.service.ChatService;
import com.insightengine.model.service.ChatStreamListener;
import com.insightengine.model.service.SecretStore;
import com.insightengine.model.support.ModelRouteResolver;
import com.insightengine.model.support.OpenAiChatClient;
import com.insightengine.starter.web.context.UserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 聊天补全服务实现（IF §7.5）。
 *
 * <h3>处理链</h3>
 * <pre>
 * 请求 → 解析模型（auto / 具体编码）→ 取厂商 baseUrl + 解密 Key → 组装 OpenAI 兼容请求体
 *      → 调上游（非流式一次性 / 流式逐行）→ 映射为平台响应或 SSE 事件 → 写用量记录
 * </pre>
 *
 * <h3>几个刻意的设计选择</h3>
 * <ul>
 *   <li><b>错误码用 3xxx 段</b>（{@link ErrorCode#MODEL_NOT_FOUND} 等）——该码段本就是为模型网关预留的，
 *       此前一直空置；本轮启用后，前端可按码段做差异化提示（模型不存在 / 限流 / 密钥错 / 超时）；</li>
 *   <li><b>记账失败不影响调用结果</b>：模型已经答完了，若因记账失败抛错，调用方会"花了钱却没拿到结果"。
 *       故记账异常只打 ERROR 日志（并保留 traceId）——这是**有意的取舍**，不是漏了异常处理；</li>
 *   <li><b>流式也要记账</b>：即使中途出错/提前断开，只要上游已回报 usage 就落账（模型确实产出了 token）。</li>
 * </ul>
 *
 * <h3>MVP 边界（已登记，非遗漏）</h3>
 * <ul>
 *   <li>{@code auto} 暂不等价于路由策略：路由（IF §7.4）未实现，当前 = **第一个启用中的 CHAT 模型**；</li>
 *   <li>用量**同步落库**（DB.md 注明的"异步 MQ 上报"待 RabbitMQ 迁云后切换，写入方接口不变）；</li>
 *   <li>同一 {@code code} 若被多个厂商使用（唯一键是 {@code vendorId+code}），当前取 id 最小的启用模型并告警，
 *       待 §7.4 路由策略给出确定性选择规则。</li>
 * </ul>
 */
@Service
public class ChatServiceImpl implements ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatServiceImpl.class);

    /** MDC 中 traceId 的 key（与 starter-web 的 TraceFilter 一致） */
    private static final String MDC_TRACE_ID = "traceId";

    /** OpenAI 协议里流式结束的固定标记（厂商差异，仅作兼容识别） */
    private static final String SSE_DONE_PAYLOAD = "[DONE]";

    /**
     * 允许触发**降级**的错误码（3xxx 段里的"临时性/可转移"故障）。
     *
     * <p>不含 {@code 3001 模型不存在}：那是配置错误，换个模型只会掩盖问题（且路由解析阶段已过滤不可用模型）。</p>
     */
    private static final java.util.Set<Integer> RETRYABLE_ERROR_CODES = java.util.Set.of(
            ErrorCode.MODEL_TIMEOUT.getCode(),
            ErrorCode.MODEL_RATE_LIMIT.getCode(),
            ErrorCode.MODEL_CALL_FAIL.getCode(),
            ErrorCode.MODEL_KEY_ERROR.getCode());

    private final ModelMapper modelMapper;
    private final ModelVendorMapper vendorMapper;
    private final SecretStore secretStore;
    private final OpenAiChatClient chatClient;
    private final UsageRecordMapper usageRecordMapper;
    private final ModelRouteResolver routeResolver;
    private final ObjectMapper objectMapper;

    public ChatServiceImpl(ModelMapper modelMapper,
                           ModelVendorMapper vendorMapper,
                           SecretStore secretStore,
                           OpenAiChatClient chatClient,
                           UsageRecordMapper usageRecordMapper,
                           ModelRouteResolver routeResolver,
                           ObjectMapper objectMapper) {
        this.modelMapper = modelMapper;
        this.vendorMapper = vendorMapper;
        this.secretStore = secretStore;
        this.chatClient = chatClient;
        this.usageRecordMapper = usageRecordMapper;
        this.routeResolver = routeResolver;
        this.objectMapper = objectMapper;
    }

    @Override
    public ChatCompletionVO complete(ChatCompletionRequest request) {
        Candidates candidates = resolveCandidates(request.getModel());
        List<Long> modelIds = candidates.modelIds();
        BizException lastError = null;
        for (int i = 0; i < modelIds.size(); i++) {
            Target target = loadTarget(modelIds.get(i));
            boolean isLast = (i == modelIds.size() - 1);
            try {
                String body = buildUpstreamBody(request, target.modelCode(), false);
                String raw = chatClient.complete(target.baseUrl(), target.apiKey(), body);
                ChatCompletionVO vo = mapResponse(raw, target.modelCode());
                recordUsage(target.modelId(), vo.getUsage());
                return vo;
            } catch (BizException e) {
                // 降级条件：策略允许 fallback + 错误属"可重试类"（超时/限流/调用失败/密钥）+ 后面还有目标
                if (!candidates.fallback() || isLast || !RETRYABLE_ERROR_CODES.contains(e.getCode())) {
                    throw e;
                }
                lastError = e;
                log.warn("主模型调用失败（modelId={}, code={}），按路由策略降级到下一个目标",
                        target.modelId(), e.getCode());
            }
        }
        throw lastError != null ? lastError
                : new BizException(ErrorCode.MODEL_NOT_FOUND, "没有可用的模型目标");
    }

    @Override
    public void stream(ChatCompletionRequest request, ChatStreamListener listener) {
        // 流式只用**路由的第一个目标**：SSE 一旦发出 200 与响应头就无法改换上游，
        // "流中途换模型"会把两段不同模型的输出拼给用户（更糟）。故流式降级**不做**，
        // 已登记 PROGRESS §6.3（如确需，做法应是"首包前探测 + 失败才重试"，属后续增强）。
        Candidates candidates = resolveCandidates(request.getModel());
        Target target = loadTarget(candidates.modelIds().get(0));
        String body = buildUpstreamBody(request, target.modelCode(), true);
        StreamState state = new StreamState();
        try {
            chatClient.stream(target.baseUrl(), target.apiKey(), body,
                    line -> consumeSseLine(line, target.modelCode(), state, listener));
            // 上游正常关闭连接：**恰好发一次**收尾事件，保证前端一定有结束信号
            // （上游可能只给 [DONE]、不给 finish_reason，此时按 "stop" 兜底）
            listener.onFinish(target.modelCode(),
                    StringUtils.hasText(state.finishReason) ? state.finishReason : "stop",
                    state.usage);
        } catch (BizException e) {
            // 流中出错：以 error 事件告知调用方（连接随后关闭），错误码沿用 3xxx 段
            listener.onError(e.getCode(), e.getMessage());
        } finally {
            recordUsage(target.modelId(), state.usage);
        }
    }

    /* ==================== 内部实现 ==================== */

    /**
     * 解析候选模型（有序：主 + 备）与是否允许降级。
     *
     * <p>两种来源：</p>
     * <ul>
     *   <li><b>{@code auto}</b>（IF §7.4）：走路由策略——按 priority 取启用策略、匹配规则、取 targets
     *       （主 + 备）与 {@code fallback} 开关；<b>未命中任何策略</b>时兜底为"第一个启用中的 CHAT 模型"
     *       （保证没配策略也能用，且日志明确说明走了兜底）；</li>
     *   <li><b>具体编码</b>：直接按 code 查，**不降级**（调用方指定了模型，就按它失败即失败）。</li>
     * </ul>
     */
    private Candidates resolveCandidates(String modelOrAuto) {
        if (ModelConstants.LOGICAL_MODEL_AUTO.equalsIgnoreCase(modelOrAuto)) {
            ModelRouteResolver.RouteDecision decision =
                    routeResolver.resolve(UserContext.getTenantId(), UserContext.getWorkspaceId());
            if (!decision.modelIds().isEmpty()) {
                return new Candidates(decision.modelIds(), decision.fallback());
            }
            log.info("auto 未命中任何路由策略，兜底使用第一个启用中的 CHAT 模型");
            List<Model> fallback = modelMapper.selectList(new LambdaQueryWrapper<Model>()
                    .eq(Model::getType, ModelConstants.TYPE_CHAT)
                    .eq(Model::getEnabled, 1)
                    .orderByAsc(Model::getId)
                    .last("LIMIT 1"));
            if (fallback.isEmpty()) {
                throw new BizException(ErrorCode.MODEL_NOT_FOUND, "没有启用中的 CHAT 模型可用");
            }
            return new Candidates(List.of(fallback.get(0).getId()), false);
        }

        List<Model> candidates = modelMapper.selectList(new LambdaQueryWrapper<Model>()
                .eq(Model::getCode, modelOrAuto)
                .eq(Model::getEnabled, 1)
                .orderByAsc(Model::getId)
                .last("LIMIT 2"));
        if (candidates.isEmpty()) {
            throw new BizException(ErrorCode.MODEL_NOT_FOUND, "模型不存在或未启用：" + modelOrAuto);
        }
        if (candidates.size() > 1) {
            log.warn("模型编码 {} 在多个厂商下重复启用，暂取 id 最小者；如需确定性选择请配置路由策略（IF §7.4）",
                    modelOrAuto);
        }
        return new Candidates(List.of(candidates.get(0).getId()), false);
    }

    /**
     * 按 modelId 装载调用目标（模型 + 厂商接入信息 + 解密后的 Key）。
     */
    private Target loadTarget(Long modelId) {
        Model model = modelMapper.selectById(modelId);
        if (model == null || model.getEnabled() == null || model.getEnabled() != 1) {
            throw new BizException(ErrorCode.MODEL_NOT_FOUND, "模型不存在或未启用：id=" + modelId);
        }

        ModelVendor vendor = vendorMapper.selectById(model.getVendorId());
        if (vendor == null || vendor.getEnabled() == null || vendor.getEnabled() != 1) {
            throw new BizException(ErrorCode.MODEL_CALL_FAIL, "模型所属厂商不存在或已停用");
        }
        if (!StringUtils.hasText(vendor.getBaseUrl())) {
            throw new BizException(ErrorCode.MODEL_CALL_FAIL, "厂商未配置接口基址（baseUrl）");
        }
        // Key 可为空（本地无鉴权模型，如 Ollama）；有记录但解不开则明确报错（常见于 KEK 变更）
        String apiKey = null;
        if (vendor.getApiKeySecretId() != null) {
            apiKey = secretStore.readPlain(vendor.getApiKeySecretId());
            if (!StringUtils.hasText(apiKey)) {
                throw new BizException(ErrorCode.MODEL_KEY_ERROR, "厂商密钥读取失败（请检查密钥记录与 KEK 配置）");
            }
        }
        return new Target(model.getId(), model.getCode(), vendor.getBaseUrl(), apiKey);
    }

    /**
     * 组装上游请求体（OpenAI 兼容协议）。
     *
     * <p>注意字段名差异：平台对外用 {@code maxTokens}（IF §7.5），上游用 {@code max_tokens}——
     * 在**这一处**完成转换，避免两个口径散落。</p>
     */
    private String buildUpstreamBody(ChatCompletionRequest request, String modelCode, boolean stream) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", modelCode);
        ArrayNode messages = root.putArray("messages");
        for (ChatCompletionRequest.Message message : request.getMessages()) {
            ObjectNode node = messages.addObject();
            node.put("role", message.getRole());
            node.put("content", message.getContent());
        }
        root.put("stream", stream);
        if (request.getTemperature() != null) {
            root.put("temperature", request.getTemperature());
        }
        if (request.getMaxTokens() != null) {
            root.put("max_tokens", request.getMaxTokens());
        }
        try {
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "组装模型请求体失败", e);
        }
    }

    /**
     * 解析非流式上游响应（OpenAI 兼容结构 → 平台契约）。
     */
    private ChatCompletionVO mapResponse(String raw, String modelCode) {
        try {
            JsonNode root = objectMapper.readTree(raw);
            ChatCompletionVO vo = new ChatCompletionVO();
            vo.setId(root.path("id").asText(null));
            vo.setModel(root.path("model").asText(modelCode));
            JsonNode choicesNode = root.path("choices");
            if (choicesNode.isArray() && !choicesNode.isEmpty()) {
                JsonNode first = choicesNode.get(0);
                ChatCompletionVO.Choice choice = new ChatCompletionVO.Choice();
                choice.setIndex(first.path("index").asInt(0));
                ChatCompletionVO.Message message = new ChatCompletionVO.Message();
                message.setRole(first.path("message").path("role").asText("assistant"));
                message.setContent(first.path("message").path("content").asText(""));
                choice.setMessage(message);
                choice.setFinishReason(first.path("finish_reason").asText(null));
                vo.setChoices(List.of(choice));
            }
            vo.setUsage(mapUsage(root.path("usage")));
            return vo;
        } catch (Exception e) {
            log.error("解析模型响应失败：{}", raw == null ? "" : raw.substring(0, Math.min(300, raw.length())), e);
            throw new BizException(ErrorCode.MODEL_CALL_FAIL, "模型响应格式异常，无法解析");
        }
    }

    /**
     * 解析流式中的一行（上游 SSE）。
     *
     * <p>只处理 {@code data:} 行（忽略 {@code event:}/{@code id:}/{@code :} 注释行——厂商差异，
     * 只认跨厂商稳定的数据行）；{@code [DONE]} 仅作兼容识别，业务收尾一律用平台的 {@code finish} 事件。</p>
     */
    private void consumeSseLine(String line, String modelCode, StreamState state, ChatStreamListener listener) {
        if (line == null || line.isEmpty() || line.startsWith(":")) {
            return;
        }
        if (!line.startsWith("data:")) {
            return;
        }
        String payload = line.substring(5).trim();
        if (payload.isEmpty()) {
            return;
        }
        if (SSE_DONE_PAYLOAD.equals(payload)) {
            state.done = true;
            return;
        }
        try {
            JsonNode node = objectMapper.readTree(payload);
            JsonNode usageNode = node.path("usage");
            if (!usageNode.isMissingNode() && !usageNode.isNull()) {
                state.usage = mapUsage(usageNode);
            }
            JsonNode choices = node.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                JsonNode choice = choices.get(0);
                JsonNode content = choice.path("delta").path("content");
                if (!content.isMissingNode() && !content.isNull() && !content.asText().isEmpty()) {
                    listener.onDelta(modelCode, content.asText());
                }
                JsonNode finishReason = choice.path("finish_reason");
                if (!finishReason.isMissingNode() && !finishReason.isNull()) {
                    state.finishReason = finishReason.asText();
                }
            }
        } catch (Exception e) {
            // 单行解析失败不中断整条流（上游偶尔发心跳/空块），仅告警
            log.warn("忽略无法解析的流式分片：{}", payload.substring(0, Math.min(120, payload.length())));
        }
    }

    private ChatCompletionVO.Usage mapUsage(JsonNode usageNode) {
        if (usageNode == null || usageNode.isMissingNode() || usageNode.isNull()) {
            return null;
        }
        ChatCompletionVO.Usage usage = new ChatCompletionVO.Usage();
        usage.setPromptTokens(usageNode.path("prompt_tokens").isMissingNode()
                ? null : usageNode.path("prompt_tokens").asInt());
        usage.setCompletionTokens(usageNode.path("completion_tokens").isMissingNode()
                ? null : usageNode.path("completion_tokens").asInt());
        usage.setTotalTokens(usageNode.path("total_tokens").isMissingNode()
                ? null : usageNode.path("total_tokens").asInt());
        return usage;
    }

    /**
     * 写用量记录（{@code ie_usage_record}，DB.md §5.9.2 口径）。
     *
     * <p>维度取当前空间（{@code WORKSPACE}）；组织级调用（token 无 {@code ws_id}）退化为 {@code TENANT}。
     * {@code quantity} 写 total token；{@code cost} 留空（Token Plan 套餐制、暂无价目——不编造数字）。</p>
     */
    private void recordUsage(Long modelId, ChatCompletionVO.Usage usage) {
        if (usage == null || usage.getTotalTokens() == null) {
            log.warn("未取得 usage（modelId={}），本次不计入用量流水", modelId);
            return;
        }
        try {
            UsageRecord record = new UsageRecord();
            Long workspaceId = UserContext.getWorkspaceId();
            if (workspaceId != null) {
                record.setScopeType(ModelConstants.USAGE_SCOPE_WORKSPACE);
                record.setScopeId(workspaceId);
            } else {
                record.setScopeType(ModelConstants.USAGE_SCOPE_TENANT);
                record.setScopeId(UserContext.getTenantId());
            }
            record.setBizType(ModelConstants.USAGE_BIZ_MODEL);
            record.setRefId(modelId);
            record.setQuantity(usage.getTotalTokens().longValue());
            record.setTraceId(MDC.get(MDC_TRACE_ID));
            usageRecordMapper.insert(record);
        } catch (Exception e) {
            // 记账失败不影响调用方（模型已产出结果）；但必须 ERROR 告警并保留 traceId 以便补账
            log.error("写用量记录失败（modelId={}, totalTokens={}, traceId={}）",
                    modelId, usage.getTotalTokens(), MDC.get(MDC_TRACE_ID), e);
        }
    }

    /**
     * 调用目标（解析结果）。
     */
    private record Target(Long modelId, String modelCode, String baseUrl, String apiKey) {
    }

    /**
     * 候选目标（有序：首个为主、其余为备）+ 是否允许降级。
     */
    private record Candidates(List<Long> modelIds, boolean fallback) {
    }

    /**
     * 流式过程的可变状态（跨回调累积 usage 与 finish_reason）。
     */
    private static final class StreamState {
        private ChatCompletionVO.Usage usage;
        private String finishReason;
        /** 上游是否发过 [DONE]（仅用于观测厂商行为差异，不作为业务收尾依据） */
        private boolean done;
    }
}
