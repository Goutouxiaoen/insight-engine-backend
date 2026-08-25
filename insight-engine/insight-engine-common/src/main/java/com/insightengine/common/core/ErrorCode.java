package com.insightengine.common.core;

import lombok.Getter;

/**
 * 统一错误码枚举。
 *
 * <p>码段规则（PRD §9.4.2 / IF 附录 A）：</p>
 * <ul>
 *   <li>{@code 0}：成功</li>
 *   <li>{@code 1xxx}：通用错误（参数、限流、不允许操作、资源不存在、系统内部）</li>
 *   <li>{@code 2xxx}：认证与用户权限</li>
 *   <li>{@code 3xxx}：模型网关</li>
 *   <li>{@code 4xxx}：知识库</li>
 *   <li>{@code 5xxx}：Agent</li>
 *   <li>{@code 6xxx}：工具</li>
 *   <li>{@code 7xxx}：对话</li>
 *   <li>{@code 8xxx}：计费</li>
 *   <li>{@code 9xxx}：系统</li>
 * </ul>
 *
 * <p>设计要点：错误码是「前端按 code 映射提示文案 + 日志定位」的唯一契约，
 * 因此枚举必须与 IF 附录 A 一一对应，禁止随意增删或复用码值。</p>
 */
@Getter
public enum ErrorCode {

    /* ============ 通用（1xxx） ============ */
    SUCCESS(0, "ok", 200),
    PARAM_ERROR(1001, "参数错误", 400),
    BODY_MISSING(1002, "请求体缺失或格式错误", 400),
    OPERATION_NOT_ALLOWED(1003, "不允许的操作", 403),
    RESOURCE_NOT_FOUND(1004, "资源不存在", 404),
    SYSTEM_ERROR(9999, "系统内部错误", 500),

    /* ============ 认证（2xxx） ============ */
    UNAUTHORIZED(2001, "未登录", 401),
    PASSWORD_ERROR(2002, "密码错误", 401),
    ACCOUNT_LOCKED(2003, "账号已锁定", 403),
    ACCOUNT_DISABLED(2004, "账号已禁用", 403),
    CAPTCHA_ERROR(2005, "验证码错误", 400),
    FORBIDDEN(2006, "无权限", 403),
    TOKEN_EXPIRED(2007, "token 已过期", 401),

    /* ============ 模型网关（3xxx） ============ */
    MODEL_NOT_FOUND(3001, "模型不存在", 404),
    MODEL_TIMEOUT(3002, "模型调用超时", 504),
    MODEL_RATE_LIMIT(3003, "模型限流", 429),
    MODEL_CALL_FAIL(3004, "模型调用失败", 502),
    MODEL_KEY_ERROR(3005, "密钥错误", 500),

    /* ============ 知识库（4xxx） ============ */
    KB_NOT_FOUND(4001, "知识库不存在", 404),
    DOC_PARSE_FAIL(4002, "文档解析失败", 500),
    EMBEDDING_FAIL(4003, "Embedding 失败", 500),
    RETRIEVE_TIMEOUT(4004, "检索超时", 504),
    DOC_FORMAT_UNSUPPORTED(4005, "文档格式不支持", 400),
    DOC_TOO_LARGE(4006, "文档过大", 413),

    /* ============ Agent（5xxx） ============ */
    AGENT_NOT_FOUND(5001, "Agent 不存在", 404),
    AGENT_MAX_ITER(5002, "超过最大迭代次数", 500),
    TOOL_CALL_FAIL(5003, "工具调用失败", 500),
    WORKFLOW_DSL_INVALID(5004, "工作流 DSL 非法", 400),

    /* ============ 工具（6xxx） ============ */
    TOOL_NOT_FOUND(6001, "工具不存在", 404),
    TOOL_TIMEOUT(6002, "工具调用超时", 504),
    TOOL_DISABLED(6003, "工具已禁用", 403),
    TOOL_INTRANET_FORBIDDEN(6004, "内网地址禁止访问", 403),

    /* ============ 对话（7xxx） ============ */
    CONV_NOT_FOUND(7001, "会话不存在", 404),

    /* ============ 计费（8xxx） ============ */
    QUOTA_INSUFFICIENT(8001, "配额不足", 429),
    BALANCE_INSUFFICIENT(8002, "余额不足", 429),
    BILL_GENERATING(8003, "账单生成中", 409),

    /* ============ 系统（9xxx） ============ */
    CONFIG_NOT_FOUND(9001, "配置不存在", 404);

    /** 业务错误码（0 表示成功） */
    private final int code;
    /** 默认提示信息 */
    private final String message;
    /** 对应的 HTTP 状态码（网关/异常处理器据此设置响应状态） */
    private final int httpStatus;

    ErrorCode(int code, String message, int httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
