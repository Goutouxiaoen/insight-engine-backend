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

    /* ============ 聊天补全（IF §7.5） ============ */

    /**
     * 逻辑模型名 {@code auto}：交给路由策略选具体模型。
     *
     * <p>MVP 说明：路由策略（IF §7.4）尚未实现，当前 {@code auto} 的语义 = **取第一个启用中的 CHAT 模型**；
     * 等 §7.4 落地后由路由策略接管（此时本常量语义不变，只是选择逻辑变复杂）。</p>
     */
    public static final String LOGICAL_MODEL_AUTO = "auto";

    /* ============ SSE 事件（IF §2.6 是心跳的单一事实源） ============ */

    /** 业务事件：增量内容（delta） */
    public static final String SSE_EVENT_MESSAGE = "message";

    /** 业务事件：错误（流中出错时推送，随后关闭连接） */
    public static final String SSE_EVENT_ERROR = "error";

    /** 业务事件：结束（携带 usage 汇总） */
    public static final String SSE_EVENT_FINISH = "finish";

    /** 通用心跳事件：**所有** stream=true 接口都有，15s 一次，不可关闭（IF §2.6） */
    public static final String SSE_EVENT_HEARTBEAT = "heartbeat";

    /** 心跳周期（毫秒）——IF §2.6 规定 15s，不可关闭 */
    public static final long SSE_HEARTBEAT_INTERVAL_MS = 15_000L;

    /** SSE 默认内容类型（UTF-8，避免中文乱码） */
    public static final String SSE_CONTENT_TYPE = "text/event-stream;charset=UTF-8";

    /* ============ 用量计量（DB.md §5.9.2 ie_usage_record 的取值口径） ============ */

    /** 计量维度：租户（token 无 ws_id 时的退化维度） */
    public static final String USAGE_SCOPE_TENANT = "TENANT";

    /** 计量维度：工作空间（默认维度——空间是资源与计费的容器） */
    public static final String USAGE_SCOPE_WORKSPACE = "WORKSPACE";

    /** 业务类型：模型调用（biz_type 取值之一，见 DB.md §5.9.2） */
    public static final String USAGE_BIZ_MODEL = "MODEL";
}
