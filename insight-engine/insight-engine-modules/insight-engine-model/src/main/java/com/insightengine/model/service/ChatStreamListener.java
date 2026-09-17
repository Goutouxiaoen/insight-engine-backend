package com.insightengine.model.service;

import com.insightengine.model.dto.response.ChatCompletionVO;

/**
 * 流式输出的**回调契约**：服务层负责解析上游 SSE 并映射成平台事件，传输层（HTTP/SSE 写回）由调用方决定。
 *
 * <p>这样分层的原因：服务层不该知道 {@code HttpServletResponse}；测试时可用一个收集器当监听器，
 * 不必起 HTTP 服务。</p>
 */
public interface ChatStreamListener {

    /**
     * 增量内容（映射为 SSE 的 {@code message} 事件）。
     */
    void onDelta(String model, String content);

    /**
     * 流结束（映射为 SSE 的 {@code finish} 事件，携带 usage 汇总）。
     */
    void onFinish(String model, String finishReason, ChatCompletionVO.Usage usage);

    /**
     * 流中出错（映射为 SSE 的 {@code error} 事件，随后关闭连接）。
     */
    void onError(int code, String message);
}
