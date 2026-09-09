package com.insightengine.gateway.filter;

import com.insightengine.common.constant.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 网关全链路 TraceFilter（TD §8.3 过滤器链首项，WebFlux 全局过滤器）。
 *
 * <p>为什么网关要先做 traceId：网关是流量唯一入口，在这里生成/校验 traceId 后
 * 向下游透传，整条链路（网关 → 业务服务 → DB/MQ）才能用同一个 ID 串联日志。
 * 此前网关缺失该能力：请求日志无 traceId，且 {@link AuthGlobalFilter#writeError}
 * 只能透传客户端自带的 {@code X-Trace-Id}，客户端不带时响应 traceId 为 null，
 * 前端报障无法定位（PROGRESS §6.4）。</p>
 *
 * <p>职责：</p>
 * <ul>
 *   <li>读取上游 {@code X-Trace-Id}，缺失或非法则新生成；</li>
 *   <li>重建请求头写入 traceId，保证下游服务（含网关内后续过滤器）可读取；</li>
 *   <li>回写响应头 {@code X-Trace-Id}，前端可据此报障定位。</li>
 * </ul>
 *
 * <p>安全要点：请求头完全由客户端可控，非法/超长/含注入字符（换行、ANSI）一律丢弃重生成，
 * 与 starter-web {@code TraceFilter} 的校验规则保持一致，避免日志注入。</p>
 *
 * <p>执行顺序：{@link #getOrder()} = -200，先于 {@link AuthGlobalFilter}（-100）执行，
 * 保证认证失败时也能拿到 traceId 回填错误响应。</p>
 */
@Slf4j
@Component
public class TraceGlobalFilter implements GlobalFilter, Ordered {

    /** traceId 最大长度，防止超长字符串导致响应头/日志膨胀 */
    private static final int MAX_TRACE_ID_LEN = 64;

    /** 合法 traceId 字符集：字母/数字/连字符，杜绝换行、ANSI 等日志注入字符 */
    private static final Pattern TRACE_ID_PATTERN = Pattern.compile("[A-Za-z0-9-]{1," + MAX_TRACE_ID_LEN + "}");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String traceId = resolveTraceId(request);

        // 重建请求头（覆盖客户端传入的非法值），下游服务与后续过滤器据此取到可信 traceId
        ServerHttpRequest mutatedRequest = request.mutate()
                .header(Constants.HEADER_TRACE_ID, traceId)
                .build();
        // 响应头回传，前端报障可直接引用。
        // 必须在 beforeCommit 阶段写：直接 set 会被 NettyRoutingFilter 合并上游响应头时追加一份，
        // 导致 X-Trace-Id 出现两个值（上游 starter-web TraceFilter 也会回传该头）；提交前 set 可覆盖为单值。
        exchange.getResponse().beforeCommit(() -> {
            exchange.getResponse().getHeaders().set(Constants.HEADER_TRACE_ID, traceId);
            return Mono.empty();
        });

        log.info("[gateway] traceId={} {} {}", traceId, request.getMethod(), request.getPath().value());
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    @Override
    public int getOrder() {
        return -200;
    }

    /**
     * 取可信 traceId：优先复用上游透传值，非法或缺失则新生成。
     * <p>不使用 MDC：WebFlux 响应式线程模型下 ThreadLocal 无法跨调度传递，
     * 故以「请求头透传 + 日志显式带 traceId」替代，避免误以为 MDC 生效。</p>
     */
    private String resolveTraceId(ServerHttpRequest request) {
        String traceId = request.getHeaders().getFirst(Constants.HEADER_TRACE_ID);
        if (StringUtils.hasText(traceId) && TRACE_ID_PATTERN.matcher(traceId).matches()) {
            return traceId;
        }
        return UUID.randomUUID().toString().replace("-", "");
    }
}
