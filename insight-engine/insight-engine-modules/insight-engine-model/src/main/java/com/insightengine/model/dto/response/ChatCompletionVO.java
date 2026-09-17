package com.insightengine.model.dto.response;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 聊天补全响应（IF §7.5，非流式）。
 *
 * <p>{@code usage} 同时是**计量依据**：服务端据此写 {@code ie_usage_record}（见 {@code ChatService}）。</p>
 */
@Data
public class ChatCompletionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 上游响应 id（透传，便于对账与排障） */
    private String id;

    /** 实际使用的模型编码（{@code auto} 会被解析为具体模型 —— 前端可据此显示"实际用了哪个模型"） */
    private String model;

    /** 候选结果（MVP 固定 1 条） */
    private List<Choice> choices;

    /** 用量 */
    private Usage usage;

    /**
     * 候选结果。
     */
    @Data
    public static class Choice implements Serializable {

        private static final long serialVersionUID = 1L;

        /** 序号（MVP 固定 0） */
        private Integer index;

        /** 助手消息（非流式为完整内容；流式分片见 SSE 的 message 事件） */
        private Message message;

        /** 结束原因（stop / length …） */
        private String finishReason;
    }

    /**
     * 消息（role + content）。
     */
    @Data
    public static class Message implements Serializable {

        private static final long serialVersionUID = 1L;

        private String role;

        private String content;
    }

    /**
     * 用量（token 数）。
     */
    @Data
    public static class Usage implements Serializable {

        private static final long serialVersionUID = 1L;

        /** 输入 token */
        private Integer promptTokens;

        /** 输出 token */
        private Integer completionTokens;

        /** 合计 token（写 ie_usage_record.quantity 用这个） */
        private Integer totalTokens;
    }
}
