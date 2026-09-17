package com.insightengine.model.support;

import com.insightengine.common.core.BizException;
import com.insightengine.common.core.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * OpenAI 兼容协议的模型调用客户端（通义 Token Plan / Ollama / 智谱等"兼容模式"都走这里）。
 *
 * <h3>为什么用 JDK 自带的 {@link HttpClient}</h3>
 * <ul>
 *   <li>我们的服务是 **Servlet 栈**（spring-web），引 WebClient 要拖进 WebFlux 整套；
 *       JDK 11+ 的 HttpClient 支持**流式读取响应体**（{@link HttpResponse.BodyHandlers#ofLines()}），
 *       正好满足"边收边转发"的 SSE 需求；</li>
 *   <li>零新增依赖 —— 外部依赖越少，升级与排障面越小。</li>
 * </ul>
 *
 * <h3>两种调用</h3>
 * <ul>
 *   <li>{@link #complete}：非流式，一次性拿完整响应体（JSON 字符串）；</li>
 *   <li>{@link #stream}：流式，**逐行**回调（回调里自行解析 {@code data:} 行），配合 Servlet 输出流实现透传。</li>
 * </ul>
 *
 * <h3>密钥纪律</h3>
 * <p>API Key 只作为请求头参数传入，**绝不写日志**；日志里只出现 baseUrl 与模型名。</p>
 */
@Component
public class OpenAiChatClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiChatClient.class);

    /** 建连超时（连接阶段） */
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);

    /** 非流式整体超时（模型生成较慢，给足 120s） */
    private static final Duration COMPLETE_TIMEOUT = Duration.ofSeconds(120);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /**
     * 非流式调用。
     *
     * @param baseUrl    厂商基址（如 {@code https://.../compatible-mode/v1}）
     * @param apiKey     明文 Key（仅本次调用内存中存在）
     * @param bodyJson   上游请求体（已由服务层组装成 OpenAI 兼容格式）
     * @return 上游原始响应体（JSON 字符串；**不做解析**，交给服务层按契约映射）
     */
    public String complete(String baseUrl, String apiKey, String bodyJson) {
        HttpResponse<String> response = send(baseUrl, apiKey, bodyJson,
                HttpResponse.BodyHandlers.ofString(), COMPLETE_TIMEOUT);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw upstreamError(response.statusCode(), response.body());
        }
        return response.body();
    }

    /**
     * 流式调用：逐行回调上游 SSE 行（含空行；由服务层负责解析与事件映射）。
     *
     * <p>⚠️ **不设整体超时**：流式响应可能持续数分钟，整体超时会中途掐断；连接超时仍由客户端统一控制。
     * 上游异常（非 2xx）在**拿到响应头时**即可判定，此时抛业务异常而不会进入流式回调。</p>
     *
     * @param lineConsumer 每收到一行调用一次（同步执行，用完即弃）
     */
    public void stream(String baseUrl, String apiKey, String bodyJson, Consumer<String> lineConsumer) {
        HttpRequest request = buildRequest(baseUrl, apiKey, bodyJson, null);
        try {
            HttpResponse<Stream<String>> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofLines());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                // 非流式错误体：把流收敛成字符串以便读取错误详情（错误体很小）
                String body;
                try (Stream<String> lines = response.body()) {
                    body = String.join("", lines.toList());
                }
                throw upstreamError(response.statusCode(), body);
            }
            try (Stream<String> lines = response.body()) {
                lines.forEach(lineConsumer);
            }
        } catch (BizException e) {
            throw e;
        } catch (HttpTimeoutException e) {
            log.error("调用模型超时（流式）: baseUrl={}", baseUrl, e);
            throw new BizException(ErrorCode.MODEL_TIMEOUT, "模型调用超时（上游响应过慢）");
        } catch (IOException e) {
            log.error("调用模型失败（IO）: baseUrl={}", baseUrl, e);
            throw new BizException(ErrorCode.MODEL_CALL_FAIL, "调用模型服务失败（网络异常）");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException(ErrorCode.MODEL_CALL_FAIL, "调用模型服务被中断");
        }
    }

    private HttpResponse<String> send(String baseUrl, String apiKey, String bodyJson,
                                      HttpResponse.BodyHandler<String> handler, Duration timeout) {
        HttpRequest request = buildRequest(baseUrl, apiKey, bodyJson, timeout);
        try {
            return httpClient.send(request, handler);
        } catch (HttpTimeoutException e) {
            log.error("调用模型超时: baseUrl={} timeout={}s", baseUrl, timeout == null ? "-" : timeout.toSeconds(), e);
            throw new BizException(ErrorCode.MODEL_TIMEOUT, "模型调用超时（上游响应过慢）");
        } catch (IOException e) {
            log.error("调用模型失败（IO）: baseUrl={}", baseUrl, e);
            throw new BizException(ErrorCode.MODEL_CALL_FAIL, "调用模型服务失败（网络异常）");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException(ErrorCode.MODEL_CALL_FAIL, "调用模型服务被中断");
        }
    }

    private HttpRequest buildRequest(String baseUrl, String apiKey, String bodyJson, Duration timeout) {
        String url = baseUrl.endsWith("/") ? baseUrl + "chat/completions" : baseUrl + "/chat/completions";
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream, application/json")
                .POST(HttpRequest.BodyPublishers.ofString(bodyJson, java.nio.charset.StandardCharsets.UTF_8));
        if (apiKey != null && !apiKey.isBlank()) {
            builder.header("Authorization", "Bearer " + apiKey);
        }
        if (timeout != null) {
            builder.timeout(timeout);
        }
        return builder.build();
    }

    /**
     * 上游报错转业务异常（**错误体透传**，便于调用方看到厂商原文，如余额/模型名/限流提示）。
     *
     * <p>状态码 → 平台错误码（3xxx 段）映射，让前端能按码段做差异化提示：</p>
     * <ul>
     *   <li>{@code 401/403} → {@code 3005 密钥错误}（"Key 无效"与"Key 用错入口"都落这里，错误体里有厂商原话）；</li>
     *   <li>{@code 404} → {@code 3001 模型不存在}（如模型名不在该套餐/接入点）；</li>
     *   <li>{@code 429} → {@code 3003 模型限流}；</li>
     *   <li>{@code 408/504} → {@code 3002 模型调用超时}；</li>
     *   <li>其它 → {@code 3004 模型调用失败}。</li>
     * </ul>
     */
    private BizException upstreamError(int statusCode, String body) {
        log.warn("模型服务返回非 2xx: status={} body={}", statusCode,
                body == null ? "" : body.substring(0, Math.min(300, body.length())));
        ErrorCode errorCode = switch (statusCode) {
            case 401, 403 -> ErrorCode.MODEL_KEY_ERROR;
            case 404 -> ErrorCode.MODEL_NOT_FOUND;
            case 429 -> ErrorCode.MODEL_RATE_LIMIT;
            case 408, 504 -> ErrorCode.MODEL_TIMEOUT;
            default -> ErrorCode.MODEL_CALL_FAIL;
        };
        return new BizException(errorCode,
                "模型服务调用失败（HTTP " + statusCode + "）：" + abbreviate(body));
    }

    private String abbreviate(String body) {
        if (body == null) {
            return "";
        }
        String oneLine = body.replaceAll("\\s+", " ").trim();
        return oneLine.length() <= 300 ? oneLine : oneLine.substring(0, 300) + "...";
    }
}
