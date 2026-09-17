package com.insightengine.model.controller;

import com.insightengine.common.core.Result;
import com.insightengine.model.constant.ModelConstants;
import com.insightengine.model.dto.request.ChatCompletionRequest;
import com.insightengine.model.dto.response.ChatCompletionVO;
import com.insightengine.model.service.ChatService;
import com.insightengine.model.service.ChatStreamListener;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 聊天补全接口（IF §7.5）——模型网关的核心出口，兼容 OpenAI 协议。
 *
 * <p><b>一个端点两种返回</b>：{@code stream=false} 走统一 {@link Result} JSON 包装；
 * {@code stream=true} 走 SSE（{@code text/event-stream}）逐段推送。因此方法返回 {@code Object}：
 * 非流式返回 {@code Result}（交给 Spring 序列化），流式自行写响应体后返回 {@code null}。</p>
 *
 * <h3>SSE 帧格式（本轮定稿，已回写 IF §7.5）</h3>
 * <pre>
 * event: message
 * data: {"model":"qwen3.7-plus","delta":{"content":"你"}}
 *
 * event: heartbeat            ← IF §2.6：15s 一次、不可关闭
 * data: {"ts":1789634540000}
 *
 * event: finish               ← 收尾（携带 usage 汇总）
 * data: {"model":"...","finishReason":"stop","usage":{"promptTokens":12,"completionTokens":3,"totalTokens":15}}
 *
 * event: error                ← 出错（随后关闭连接）
 * data: {"code":3003,"message":"模型限流"}
 * </pre>
 *
 * <h3>两个容易被忽略但必须做的事</h3>
 * <ul>
 *   <li><b>心跳独立线程</b>：模型"思考"期间可能十几秒不吐字，必须由独立线程按 15s 推心跳，
 *       否则中间代理/浏览器会把"长时间无数据"的连接判为超时并掐断（IF §2.6 之所以规定心跳即为此）；
 *       写帧对 {@link PrintWriter} 加锁，避免与业务线程交叉写坏帧；</li>
 *   <li><b>{@code X-Accel-Buffering: no}</b>：告诉 nginx 类反向代理**不要缓冲**响应，否则"边生成边推送"
 *       会被代理攒成一次性返回，前端看到的就是"卡半天然后刷出全部"。</li>
 * </ul>
 */
@Tag(name = "聊天补全", description = "兼容 OpenAI 协议；支持非流式与 SSE 流式（含 heartbeat 心跳）")
@RestController
@RequestMapping("/api/v1/model")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    /** 心跳线程池：单线程即可（心跳只写少量字节），守护线程避免阻止 JVM 退出 */
    private static final ScheduledExecutorService HEARTBEAT_POOL = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "model-sse-heartbeat");
        thread.setDaemon(true);
        return thread;
    });

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * 聊天补全（IF §7.5，权限 {@code model:list:read}）。
     *
     * <p>权限说明：模型字典目前只有 {@code model:vendor:*}/{@code model:list:*}/{@code model:route:*}/
     * {@code model:usage:read}，没有独立的"调用模型"权限码，故 MVP 复用 {@code model:list:read}；
     * 是否需要新增如 {@code model:chat} 已登记 PROGRESS §6.3 待裁决（新增码需同步权限字典 + 角色授权 + 前端门控）。</p>
     */
    @Operation(summary = "聊天补全",
            description = "stream=false 返回统一 JSON；stream=true 返回 SSE（事件 message/heartbeat/finish/error）")
    @PostMapping("/chat/completions")
    @PreAuthorize("hasAuthority('model:list:read')")
    public Object chatCompletions(@Valid @RequestBody ChatCompletionRequest request,
                                  HttpServletResponse response) throws IOException {
        if (Boolean.TRUE.equals(request.getStream())) {
            streamChat(request, response);
            return null;
        }
        ChatCompletionVO result = chatService.complete(request);
        return Result.ok(result);
    }

    /* ==================== 流式实现 ==================== */

    private void streamChat(ChatCompletionRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(ModelConstants.SSE_CONTENT_TYPE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        // 禁用 nginx 类代理的响应缓冲，保证"边生成边推送"
        response.setHeader("X-Accel-Buffering", "no");

        PrintWriter writer = response.getWriter();
        ScheduledFuture<?> heartbeat = HEARTBEAT_POOL.scheduleAtFixedRate(
                () -> writeEvent(writer, ModelConstants.SSE_EVENT_HEARTBEAT,
                        "{\"ts\":" + System.currentTimeMillis() + "}"),
                ModelConstants.SSE_HEARTBEAT_INTERVAL_MS / 1000,
                ModelConstants.SSE_HEARTBEAT_INTERVAL_MS / 1000,
                TimeUnit.SECONDS);
        try {
            chatService.stream(request, new ChatStreamListener() {

                @Override
                public void onDelta(String model, String content) {
                    writeEvent(writer, ModelConstants.SSE_EVENT_MESSAGE,
                            "{\"model\":" + quote(model) + ",\"delta\":{\"content\":" + quote(content) + "}}");
                }

                @Override
                public void onFinish(String model, String finishReason, ChatCompletionVO.Usage usage) {
                    StringBuilder data = new StringBuilder("{\"model\":").append(quote(model))
                            .append(",\"finishReason\":").append(quote(finishReason));
                    if (usage != null) {
                        data.append(",\"usage\":{\"promptTokens\":").append(usage.getPromptTokens())
                                .append(",\"completionTokens\":").append(usage.getCompletionTokens())
                                .append(",\"totalTokens\":").append(usage.getTotalTokens()).append("}");
                    }
                    data.append("}");
                    writeEvent(writer, ModelConstants.SSE_EVENT_FINISH, data.toString());
                }

                @Override
                public void onError(int code, String message) {
                    writeEvent(writer, ModelConstants.SSE_EVENT_ERROR,
                            "{\"code\":" + code + ",\"message\":" + quote(message) + "}");
                }
            });
        } finally {
            heartbeat.cancel(false);
        }
    }

    /**
     * 写一个 SSE 帧并立即 flush。
     *
     * <p>对 writer 加锁：心跳线程与业务线程会并发写同一个输出流，不加锁可能把两个帧的字节交叉写坏。</p>
     */
    private void writeEvent(PrintWriter writer, String event, String dataJson) {
        synchronized (writer) {
            try {
                writer.write("event: " + event + "\n");
                writer.write("data: " + dataJson + "\n\n");
                writer.flush();
            } catch (Exception e) {
                // 调用方提前断开连接时写会失败：属正常情况，不升级为错误日志（避免刷屏）
                log.debug("SSE 写帧失败（可能调用方已断开）: event={}", event);
            }
        }
    }

    /**
     * JSON 字符串字面量转义（内容里可能有引号/换行/反斜杠）。
     *
     * <p>用最朴素的转义规则而非引 Jackson：此处只需保证 SSE 帧是合法 JSON 一行，避免为一个字符串
     * 引入依赖注入与异常分支。</p>
     */
    private String quote(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder(value.length() + 2);
        sb.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
        return sb.toString();
    }
}
