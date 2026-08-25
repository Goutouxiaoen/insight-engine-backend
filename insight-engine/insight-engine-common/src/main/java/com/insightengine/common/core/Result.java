package com.insightengine.common.core;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应体。
 *
 * <p>所有 HTTP 接口（网关、业务服务、OpenAPI）对外返回的结构保持一致（TD §4.1 / IF §2.2）：</p>
 * <pre>
 * { "code": 0, "message": "ok", "data": {...}, "traceId": "...", "ts": 1724567890123 }
 * </pre>
 *
 * <p>设计要点：</p>
 * <ul>
 *   <li>{@code code == 0} 表示成功，非 0 表示失败，前端据此映射提示文案；</li>
 *   <li>{@code traceId} 用于全链路排查，由 starter-web 的 TraceFilter 统一回填；</li>
 *   <li>{@code ts} 采用 {@code System.currentTimeMillis()} 而非构造时赋值，
 *       保证每次序列化反映真实响应时刻。</li>
 * </ul>
 *
 * @param <T> 业务数据类型
 */
@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 业务码：0 成功，非 0 失败（见 {@link ErrorCode}） */
    private int code;

    /** 提示信息 */
    private String message;

    /** 业务数据，失败时为 null */
    private T data;

    /** 链路追踪 ID */
    private String traceId;

    /** 响应时间戳（毫秒） */
    private long ts;

    /**
     * 私有构造：强制通过静态工厂方法创建，避免调用方直接 new 出状态不一致的对象。
     */
    private Result() {
        this.ts = System.currentTimeMillis();
    }

    /**
     * 成功响应（无数据）。
     */
    public static <T> Result<T> ok() {
        return ok(null);
    }

    /**
     * 成功响应（携带数据）。
     *
     * @param data 业务数据，可为 null
     */
    public static <T> Result<T> ok(T data) {
        Result<T> result = new Result<>();
        result.code = ErrorCode.SUCCESS.getCode();
        result.message = ErrorCode.SUCCESS.getMessage();
        result.data = data;
        return result;
    }

    /**
     * 失败响应（使用错误码枚举的默认提示信息）。
     *
     * @param errorCode 错误码枚举
     */
    public static <T> Result<T> fail(ErrorCode errorCode) {
        return fail(errorCode, errorCode.getMessage());
    }

    /**
     * 失败响应（自定义提示信息，覆盖枚举默认文案）。
     *
     * @param errorCode 错误码枚举
     * @param message   自定义提示信息（如携带具体字段校验错误）
     */
    public static <T> Result<T> fail(ErrorCode errorCode, String message) {
        Result<T> result = new Result<>();
        result.code = errorCode.getCode();
        result.message = message;
        return result;
    }

    /**
     * 失败响应（直接使用错误码数值与文案，供异常处理器等已知码值场景使用）。
     *
     * @param code    业务错误码
     * @param message 提示信息
     */
    public static <T> Result<T> fail(int code, String message) {
        Result<T> result = new Result<>();
        result.code = code;
        result.message = message;
        return result;
    }

    /**
     * 判断当前响应是否成功。
     */
    public boolean isSuccess() {
        return this.code == ErrorCode.SUCCESS.getCode();
    }
}
