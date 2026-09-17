package com.insightengine.model.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / OpenAPI3 文档配置。
 *
 * <p>为 Model 服务装配接口文档元信息（IF §1.1 文档入口 Knife4j）。启动后访问：</p>
 * <ul>
 *   <li>文档页面：{@code http://localhost:7103/doc.html}</li>
 *   <li>OpenAPI JSON：{@code http://localhost:7103/v3/api-docs}</li>
 * </ul>
 *
 * <p>说明：网关的 {@code /doc.html} 目前指向 UMS，本服务的文档走自身端口（与 workspace 同策略）。</p>
 */
@Configuration
public class OpenApiConfig {

    /**
     * 文档元信息。
     */
    @Bean
    public OpenAPI modelOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("智擎 AI - Model 模型网关服务")
                        .description("模型厂商接入（含 API Key 加密存储）、模型目录、路由策略、聊天补全/Embedding/Rerank、用量查询接口")
                        .version("1.0.0")
                        .contact(new Contact().name("InsightEngine").email("admin@example.com")));
    }
}
