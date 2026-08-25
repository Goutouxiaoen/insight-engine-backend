package com.insightengine.common.core;

import lombok.Getter;

/**
 * 业务异常。
 *
 * <p>业务代码在遇到「可预期」的失败（如参数不合法、资源不存在、配额不足）时，
 * 抛出本异常并携带 {@link ErrorCode}，由 starter-web 的
 * {@code GlobalExceptionHandler} 统一捕获并转换为 {@link Result} 响应。</p>
 *
 * <p>设计要点：</p>
 * <ul>
 *   <li>仅承载业务错误码与提示，不暴露堆栈给客户端，避免信息泄露；</li>
 *   <li>构造时锁定 HTTP 状态码，异常处理器无需再判断码段；</li>
 *   <li>禁止在业务代码中吞掉本异常，也不要用 {@code catch (Exception)} 兜底覆盖它。</li>
 * </ul>
 */
@Getter
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** 业务错误码 */
    private final int code;

    /** 对应的 HTTP 状态码 */
    private final int httpStatus;

    /**
     * 使用错误码枚举的默认提示信息构造。
     *
     * @param errorCode 错误码枚举
     */
    public BizException(ErrorCode errorCode) {
        this(errorCode, errorCode.getMessage());
    }

    /**
     * 使用自定义提示信息构造（覆盖枚举默认文案）。
     *
     * @param errorCode 错误码枚举
     * @param message   自定义提示信息
     */
    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
        this.httpStatus = errorCode.getHttpStatus();
    }

    /**
     * 使用自定义提示信息构造（覆盖枚举默认文案），并保留原始异常作为 cause，
     * 便于日志中追溯根因（客户端仍只看到友好提示）。
     *
     * @param errorCode 错误码枚举
     * @param message   自定义提示信息
     * @param cause     原始异常
     */
    public BizException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.code = errorCode.getCode();
        this.httpStatus = errorCode.getHttpStatus();
    }
}
