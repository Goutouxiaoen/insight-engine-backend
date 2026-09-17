package com.insightengine.model.constant;

/**
 * Model 服务常量。
 *
 * <p>集中管理密钥类型、厂商/模型类型、掩码规则与平台级租户口径，避免魔法值散落
 * （与 UMS {@code AuthConstants} / workspace {@code WorkspaceConstants} 同风格）。</p>
 */
public final class ModelConstants {

    private ModelConstants() {
        // 工具类禁止实例化
    }

    /* ============ 平台级口径（2026-09-17 裁决：模型目录平台级、所有业务方共用，PROGRESS §三） ============ */

    /**
     * 平台级租户 ID：{@code 0} 表示全租户共用。
     *
     * <p>与 {@code ie_role.tenant_id = 0}（平台内置角色）**同口径**（AGENTS 铁律 6：同一语义全系统一个口径）。</p>
     */
    public static final Long PLATFORM_TENANT_ID = 0L;

    /* ============ 密钥类型（ie_secret.secret_type） ============ */

    /** 模型厂商 API Key（当前唯一使用方；后续通知 webhook token 等复用同一张表） */
    public static final String SECRET_TYPE_MODEL_API_KEY = "MODEL_API_KEY";

    /* ============ 厂商 / 模型能力类型（ie_model_vendor.type / ie_model.type） ============ */

    /** 聊天补全 */
    public static final String TYPE_CHAT = "CHAT";

    /** 向量化 */
    public static final String TYPE_EMBEDDING = "EMBEDDING";

    /** 重排 */
    public static final String TYPE_RERANK = "RERANK";

    /* ============ 掩码规则（回显给前端，明文永不出库） ============ */

    /** 掩码保留的前缀长度（如 {@code sk-}） */
    public static final int MASK_PREFIX_LEN = 3;

    /** 掩码保留的后缀长度（便于人工识别是哪把 Key） */
    public static final int MASK_SUFFIX_LEN = 4;

    /** 掩码占位串 */
    public static final String MASK_MIDDLE = "****";
}
