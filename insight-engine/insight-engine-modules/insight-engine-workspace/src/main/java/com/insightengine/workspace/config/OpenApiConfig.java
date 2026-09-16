package com.insightengine.workspace.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / OpenAPI3 文档配置。
 *
 * <p>为 Workspace 服务装配接口文档元信息（IF §1.1 文档入口 Knife4j）。
 * 启动后访问：</p>
 * <ul>
 *   <li>文档页面：{@code http://localhost:7102/doc.html}</li>
 *   <li>OpenAPI JSON：{@code http://localhost:7102/v3/api-docs}</li>
 * </ul>
 *
 * <p>设计要点：Knife4j 依赖 springdoc-openapi 自动扫描 {@code @RestController} 生成文档；
 * 本类仅补充文档标题/描述/版本等元信息，接口分组靠 Controller 的 {@code @Tag} 注解完成。</p>
 */
@Configuration
public class OpenApiConfig {

    /**
     * 文档元信息。
     */
    @Bean
    public OpenAPI workspaceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("智擎 AI - Workspace 工作空间与组织服务")
                        .description("组织创建/详情、工作空间创建/更新/删除/分页/切换、成员分页/添加/移除/改角色接口")
                        .version("1.0.0")
                        .contact(new Contact().name("InsightEngine").email("admin@example.com")));
    }
}
