package com.insightengine.starter.web.filter;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.insightengine.common.constant.Constants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * 全链路 TraceID 过滤器。
 *
 * <p>职责（TD §4.4）：</p>
 * <ul>
 *   <li>从请求头 {@code X-Trace-Id} 读取上游 traceId，无则新生成，保证整条链路 ID 一致；</li>
 *   <li>将 traceId 写入 {@link MDC}，使 logback 的 {@code %X{traceId}} 能打印到每条日志；</li>
 *   <li>通过响应头回传 traceId，前端可据其反馈问题定位。</li>
 * </ul>
 *
 * <p>继承 {@link OncePerRequestFilter} 保证单请求只执行一次，
 * 即使发生 forward/include 也不重复生成 traceId。</p>
 *
 * <p>finally 中必须 {@code MDC.remove}，否则线程复用时旧 traceId 会污染后续请求的日志。</p>
 */
public class TraceFilter extends OncePerRequestFilter {

    /** MDC 中 traceId 的 key，与 logback pattern 中的 %X{traceId} 对应 */
    private static final String MDC_TRACE_ID = "traceId";

    /** traceId 最大长度，防止超长字符串导致 MDC/响应头/日志膨胀 */
    private static final int MAX_TRACE_ID_LEN = 64;

    /** 合法 traceId 字符集：字母/数字/连字符，杜绝换行、ANSI 等日志注入字符 */
    private static final Pattern TRACE_ID_PATTERN = Pattern.compile("[A-Za-z0-9-]{1,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 优先复用上游（网关/上游服务）透传的 traceId，保证跨服务链路可串联
        String traceId = request.getHeader(Constants.HEADER_TRACE_ID);
        // 请求头完全可控，非法/缺失一律丢弃并重新生成，绝不信任用户输入
        if (traceId == null || !TRACE_ID_PATTERN.matcher(traceId).matches()) {
            traceId = IdUtil.fastSimpleUUID();
        }

        // 写入 MDC，使本次请求内所有日志自动携带 traceId
        MDC.put(MDC_TRACE_ID, traceId);
        // 回填响应头，前端与下游可读取
        response.setHeader(Constants.HEADER_TRACE_ID, traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            // 无论成功失败都必须清理，避免线程池复用导致 traceId 串号
            MDC.remove(MDC_TRACE_ID);
        }
    }
}
