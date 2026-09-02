package com.insightengine.ums.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / OpenAPI3 文档配置。
 *
 * <p>为 UMS 服务装配接口文档元信息（IF §1.1 文档入口 Knife4j）。
 * 启动后访问：</p>
 * <ul>
 *   <li>文档页面：{@code http://localhost:7101/doc.html}</li>
 *   <li>OpenAPI JSON：{@code http://localhost:7101/v3/api-docs}</li>
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
    public OpenAPI umsOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("智擎 AI - UMS 用户与权限服务")
                        .description("认证（登录/刷新/登出/注册/当前用户）、用户管理、角色管理、权限树接口")
                        .version("1.0.0")
                        .contact(new Contact().name("InsightEngine").email("admin@example.com")));
    }
}
