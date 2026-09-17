package com.insightengine.model.service;

import com.insightengine.model.dto.request.ChatCompletionRequest;
import com.insightengine.model.dto.response.ChatCompletionVO;

/**
 * 聊天补全服务（IF §7.5，模型网关的核心出口）。
 *
 * <p>职责链：**解析模型（含 {@code auto}）→ 取厂商基址与解密后的 Key → 转调上游 → 映射响应/流式事件 →
 * 写用量记录（{@code ie_usage_record}）**。</p>
 */
public interface ChatService {

    /**
     * 非流式补全（{@code stream=false}）。
     */
    ChatCompletionVO complete(ChatCompletionRequest request);

    /**
     * 流式补全（{@code stream=true}）：逐段回调 {@link ChatStreamListener}，结束时回调 usage。
     */
    void stream(ChatCompletionRequest request, ChatStreamListener listener);
}
