package com.insightengine.starter.security.handler;

import com.insightengine.common.core.ErrorCode;
import com.insightengine.common.core.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 方法级权限异常的统一响应（{@link RestControllerAdvice} 层）。
 *
 * <p>背景：{@code @PreAuthorize} 校验失败抛出的 {@link AccessDeniedException} 发生在
 * DispatcherServlet 内部，会先被 {@code @RestControllerAdvice} 捕获，
 * 传不到过滤器链上的 {@link RestAccessDeniedHandler}；若放任 starter-web
 * {@code GlobalExceptionHandler} 的 {@code Exception} 兜底处理，会误报 500。
 * 本 advice 显式拦截该异常，统一返回 HTTP 403 + {@code code=2006 无权限}（TD §4.3）。</p>
 *
 * <p>排序：{@code GlobalExceptionHandler} 未标 {@code @Order}（默认 LOWEST_PRECEDENCE），
 * 本 advice 用最高优先级确保优先匹配 {@link AccessDeniedException}。</p>
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class SecurityExceptionHandlerAdvice {

    /**
     * 已认证但权限不足：返回 403 / code=2006，并回填 traceId 便于链路定位。
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<Void>> handleAccessDenied(AccessDeniedException e, HttpServletRequest request) {
        log.warn("[accessDenied] uri={}, message={}", request.getRequestURI(), e.getMessage());
        Result<Void> result = Result.fail(ErrorCode.FORBIDDEN);
        String traceId = MDC.get("traceId");
        if (traceId != null && !traceId.isBlank()) {
            result.setTraceId(traceId);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
    }
}
