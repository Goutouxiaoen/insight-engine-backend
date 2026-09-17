package com.insightengine.model;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Model（模型网关）服务启动类。
 *
 * <p>智擎 AI 第三个业务微服务（PRD §9.2 端口 7103），是整个平台的"发动机"：
 * 向上为 Agent / 知识库 / 对话提供**统一的模型调用出口**（IF §7），向下屏蔽厂商差异
 * （通义 / OpenAI 兼容协议 / Ollama 等）。</p>
 *
 * <p>平台定位（2026-09-17 裁决，路线 A）：模型目录为**平台级**——由平台/组织管理员接入厂商与模型，
 * **所有业务方共用**；用户自带模型（BYOK）不在本期范围（详见 PROGRESS §三）。</p>
 *
 * <p>{@code @MapperScan} 扫描本模块 Mapper 接口，配合 starter-mybatis 的
 * MyBatis-Plus 自动装配完成 ORM 初始化。</p>
 */
@SpringBootApplication
@MapperScan("com.insightengine.model.mapper")
public class ModelApplication {

    public static void main(String[] args) {
        SpringApplication.run(ModelApplication.class, args);
    }
}
