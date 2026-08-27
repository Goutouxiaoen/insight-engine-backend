package com.insightengine.starter.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insightengine.common.core.ErrorCode;
import com.insightengine.common.core.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 已认证但无权限的统一入口。
 *
 * <p>方法级权限 {@code @PreAuthorize} 校验失败抛出的 {@link AccessDeniedException}
 * 由本处理器统一转为 {@code Result}（code=2006 无权限），
 * 与 IF 附录 A 的 {@code 2006} 对齐（TD §4.3 捕获顺序第 4 项）。</p>
 */
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(ErrorCode.FORBIDDEN.getHttpStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(Result.fail(ErrorCode.FORBIDDEN)));
    }
}
