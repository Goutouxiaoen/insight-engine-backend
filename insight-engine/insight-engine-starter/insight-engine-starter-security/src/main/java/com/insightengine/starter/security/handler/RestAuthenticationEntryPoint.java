package com.insightengine.starter.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insightengine.common.core.ErrorCode;
import com.insightengine.common.core.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 未认证访问受保护资源的统一入口。
 *
 * <p>当请求未携带有效 token 却访问 {@code anyRequest().authenticated()} 的接口时，
 * Spring Security 默认会跳转登录页或返回空 401，与统一响应体契约不符。
 * 本处理器把「未认证」统一转为 {@code Result} 结构（code=2001），
 * 保证前端拿到的错误格式与业务异常完全一致（IF §2.2）。</p>
 */
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(ErrorCode.UNAUTHORIZED.getHttpStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(Result.fail(ErrorCode.UNAUTHORIZED)));
    }
}
