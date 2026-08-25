package com.insightengine.starter.web.handler;

import cn.hutool.core.util.StrUtil;
import com.insightengine.common.core.BizException;
import com.insightengine.common.core.ErrorCode;
import com.insightengine.common.core.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器。
 *
 * <p>统一把各类异常转换为 {@link Result} 结构，保证前端拿到的错误格式一致（TD §4.3）。
 * 捕获顺序遵循「越具体越靠前」原则，Spring 会优先匹配最精确的异常类型。</p>
 *
 * <p>处理原则（TD §4.3）：</p>
 * <ol>
 *   <li>{@link BizException} → 返回对应业务码与提示；</li>
 *   <li>{@link MethodArgumentNotValidException} → {@code PARAM_ERROR}，携带字段级错误；</li>
 *   <li>{@link ConstraintViolationException} → {@code PARAM_ERROR}；</li>
 *   <li>{@link Exception} → {@code SYSTEM_ERROR}，打印完整堆栈供排查。</li>
 * </ol>
 *
 * <p>说明：{@code AccessDeniedException}（Spring Security 无权限异常）在
 * starter-security 阶段引入 Spring Security 后由该 starter 补充处理，
 * 本 starter 不依赖 Spring Security，保持 starter-web 轻量、可被 gateway（WebFlux）复用。</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常：返回携带的错误码与提示。
     * <p>使用 {@link BizException#getHttpStatus()} 设置 HTTP 状态码，
     * 使 401/404/429 等语义能被客户端与网关正确识别（而非一律 200）。</p>
     */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<Void>> handleBizException(BizException e) {
        log.warn("[bizException] code={}, message={}", e.getCode(), e.getMessage());
        Result<Void> result = Result.fail(e.getCode(), e.getMessage());
        result.setTraceId(currentTraceId());
        return ResponseEntity.status(e.getHttpStatus()).body(result);
    }

    /**
     * 请求体参数校验失败（@RequestBody + @Valid）。
     * <p>汇总所有字段错误，按「字段名: 错误信息」拼成提示，方便前端定位具体字段。</p>
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        log.warn("[paramError] {}", message);
        Result<Void> result = Result.fail(ErrorCode.PARAM_ERROR, message);
        result.setTraceId(currentTraceId());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }

    /**
     * 方法参数校验失败（@RequestParam / @PathVariable + @Validated）。
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraintViolation(ConstraintViolationException e) {
        log.warn("[constraintViolation] {}", e.getMessage());
        Result<Void> result = Result.fail(ErrorCode.PARAM_ERROR, e.getMessage());
        result.setTraceId(currentTraceId());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }

    /**
     * 兜底异常：返回系统内部错误，打印完整堆栈（含 traceId），
     * 但不把堆栈暴露给客户端，避免泄露内部实现细节。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e, HttpServletRequest request) {
        log.error("[systemError] uri={}, traceId={}", request.getRequestURI(), currentTraceId(), e);
        Result<Void> result = Result.fail(ErrorCode.SYSTEM_ERROR);
        result.setTraceId(currentTraceId());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
    }

    /**
     * 格式化单个字段校验错误：字段名 + 校验提示。
     */
    private String formatFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }

    /**
     * 读取当前线程 traceId，用于回填 Result，保证错误响应也能被前端按链路定位。
     */
    private String currentTraceId() {
        String traceId = MDC.get("traceId");
        return StrUtil.isBlank(traceId) ? null : traceId;
    }
}
