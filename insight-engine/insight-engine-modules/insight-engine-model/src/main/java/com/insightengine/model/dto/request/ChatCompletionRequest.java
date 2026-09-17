package com.insightengine.model.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

/**
 * 聊天补全请求体（IF §7.5，兼容 OpenAI 协议）。
 *
 * <p>字段名与 OpenAI 略有差异（{@code maxTokens} 而非 {@code max_tokens}），**以 IF §7.5 为准**
 * （前端/调用方按 IF 传参）。</p>
 */
@Data
public class ChatCompletionRequest {

    /**
     * 模型：具体模型编码（如 {@code qwen3.7-plus}）或逻辑名 {@code auto}（交给路由策略；
     * 路由未实现前 = 第一个启用中的 CHAT 模型）。
     */
    @NotBlank(message = "model 不能为空")
    private String model;

    /** 消息列表（system / user / assistant） */
    @NotEmpty(message = "messages 不能为空")
    @Valid
    private List<Message> messages;

    /** 是否流式（false/缺省 = 非流式；true = SSE） */
    private Boolean stream;

    /** 采样温度（可空，缺省由厂商决定） */
    private Double temperature;

    /** 最大生成 token 数（可空） */
    private Integer maxTokens;

    /**
     * 单条消息。
     */
    @Data
    public static class Message {

        /** 角色：system / user / assistant */
        @NotBlank(message = "role 不能为空")
        @Pattern(regexp = "^(system|user|assistant)$", message = "role 必须是 system/user/assistant 之一")
        private String role;

        /** 内容（MVP 仅支持纯文本；多模态后续扩展） */
        @NotBlank(message = "content 不能为空")
        private String content;
    }
}
