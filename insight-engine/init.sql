-- =============================================================================
-- 智擎 AI（InsightEngine）—— 数据库初始化脚本
--
-- 说明：
--   1. 本脚本由 PostgreSQL 容器「首次初始化」时自动执行（挂载到
--      /docker-entrypoint-initdb.d/），在 POSTGRES_DB=insight_engine 库内建表。
--      已存在数据卷时不会重跑，需手工 psql 执行或重建数据卷。
--   2. 表结构遵循 docs/TD.md §5（命名/字段/索引规范）与 docs/PRD.md §14（核心表）。
--      建表顺序遵循 TD §5.2（先主表后从表、先无外键后有外键）。
--   3. 统一规范（TD §5.1）：
--      - 表前缀 ie_，字段蛇形，主键 BIGSERIAL（自增，索引性能优于 UUID）
--      - 审计字段 created_at / updated_at / created_by / updated_by
--      - 逻辑删除 deleted（0 正常 / 1 删除），唯一索引用部分索引 WHERE deleted=0
--        （规避逻辑删除后唯一索引冲突，见 TD §5.5）
--      - 金额 DECIMAL(18,6)，时间 TIMESTAMP 不带时区（统一 UTC），JSON 用 JSONB
--   4. 每张表均通过 COMMENT ON 写入表说明与字段说明，便于 DBA/工具（pgAdmin/
--      DBeaver）与接口文档生成器读取元数据。
-- =============================================================================

-- PGVector 扩展（必须在 ie_chunk 建表前创建，见 TD §5.4）
CREATE EXTENSION IF NOT EXISTS vector;

-- =============================================================================
-- 组 1：账号与组织（ie_user / ie_organization / ie_workspace / ie_member）
-- =============================================================================

-- 用户表
CREATE TABLE ie_user (
    id            BIGSERIAL PRIMARY KEY,
    tenant_id     BIGINT       NOT NULL,             -- 租户（MVP 单租户=1，V1.0 多租户）
    email         VARCHAR(128),
    phone         VARCHAR(32),
    password_hash VARCHAR(255),                      -- BCrypt 密文（TD §16.1，绝不存明文）
    nickname      VARCHAR(64),
    avatar        VARCHAR(255),
    status        SMALLINT     DEFAULT 1,            -- 1 正常 / 0 禁用
    last_login_at TIMESTAMP,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by    BIGINT,
    updated_by    BIGINT,
    deleted       SMALLINT     NOT NULL DEFAULT 0    -- 逻辑删除 0/1
);
COMMENT ON TABLE  ie_user                    IS '用户表：平台账号主体，单租户（MVP）下所有用户归属于同一租户';
COMMENT ON COLUMN ie_user.id                 IS '主键，自增';
COMMENT ON COLUMN ie_user.tenant_id          IS '租户 ID，数据隔离第一维度；MVP 单租户固定为 1';
COMMENT ON COLUMN ie_user.email              IS '邮箱，登录账号之一（与 phone 二选一，唯一索引见 uk_user_email）';
COMMENT ON COLUMN ie_user.phone              IS '手机号，登录账号之一';
COMMENT ON COLUMN ie_user.password_hash      IS '密码 BCrypt 密文，strength=10，绝不存明文（TD §16.1）';
COMMENT ON COLUMN ie_user.nickname           IS '昵称';
COMMENT ON COLUMN ie_user.avatar             IS '头像 URL（MinIO 对象地址）';
COMMENT ON COLUMN ie_user.status             IS '账号状态：1 正常 / 0 禁用';
COMMENT ON COLUMN ie_user.last_login_at      IS '最近登录时间';
COMMENT ON COLUMN ie_user.created_at         IS '创建时间（UTC）';
COMMENT ON COLUMN ie_user.updated_at         IS '更新时间（UTC）';
COMMENT ON COLUMN ie_user.created_by         IS '创建人 user_id';
COMMENT ON COLUMN ie_user.updated_by         IS '最后修改人 user_id';
COMMENT ON COLUMN ie_user.deleted            IS '逻辑删除标记：0 正常 / 1 已删除';
-- 邮箱唯一（部分索引，删除后可复用邮箱，见 TD §5.5）
CREATE UNIQUE INDEX uk_user_email ON ie_user (email) WHERE deleted = 0;
CREATE INDEX idx_user_tenant ON ie_user (tenant_id);

-- 组织表
CREATE TABLE ie_organization (
    id         BIGSERIAL PRIMARY KEY,
    tenant_id  BIGINT       NOT NULL,
    name       VARCHAR(128) NOT NULL,
    code       VARCHAR(64)  NOT NULL,                -- 唯一编码（短链）
    owner_id   BIGINT       NOT NULL,                -- 所有者 user_id
    plan_id    BIGINT,                               -- 套餐 id
    status     SMALLINT     DEFAULT 1,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by BIGINT,
    updated_by BIGINT,
    deleted    SMALLINT     NOT NULL DEFAULT 0
);
COMMENT ON TABLE  ie_organization             IS '组织表：租户内的一级容器，对应一个企业/部门，下辖多个工作空间';
COMMENT ON COLUMN ie_organization.id          IS '主键，自增';
COMMENT ON COLUMN ie_organization.tenant_id   IS '租户 ID';
COMMENT ON COLUMN ie_organization.name        IS '组织名称';
COMMENT ON COLUMN ie_organization.code        IS '组织唯一编码（短链，同租户内唯一）';
COMMENT ON COLUMN ie_organization.owner_id    IS '所有者 user_id';
COMMENT ON COLUMN ie_organization.plan_id     IS '套餐 ID（关联套餐，V1.0 起用）';
COMMENT ON COLUMN ie_organization.status      IS '状态：1 正常 / 0 禁用';
COMMENT ON COLUMN ie_organization.created_at  IS '创建时间（UTC）';
COMMENT ON COLUMN ie_organization.updated_at  IS '更新时间（UTC）';
COMMENT ON COLUMN ie_organization.created_by  IS '创建人 user_id';
COMMENT ON COLUMN ie_organization.updated_by  IS '最后修改人 user_id';
COMMENT ON COLUMN ie_organization.deleted     IS '逻辑删除标记：0 正常 / 1 已删除';
CREATE UNIQUE INDEX uk_org_code_tenant ON ie_organization (tenant_id, code) WHERE deleted = 0;

-- 工作空间表（租户内二级隔离，对应部门/项目）
CREATE TABLE ie_workspace (
    id             BIGSERIAL PRIMARY KEY,
    tenant_id      BIGINT       NOT NULL,
    org_id         BIGINT       NOT NULL,
    name           VARCHAR(128) NOT NULL,
    code           VARCHAR(64)  NOT NULL,
    plan_id        BIGINT,
    max_apps       INT          DEFAULT 10,          -- 最大应用数
    max_kb_size_mb INT          DEFAULT 1024,        -- 知识库容量上限(MB)
    status         SMALLINT     DEFAULT 1,
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by     BIGINT,
    updated_by     BIGINT,
    deleted        SMALLINT     NOT NULL DEFAULT 0
);
COMMENT ON TABLE  ie_workspace                  IS '工作空间表：租户内二级隔离单元，对应业务部门/项目，是资源（KB/Agent/工具）归属边界';
COMMENT ON COLUMN ie_workspace.id               IS '主键，自增';
COMMENT ON COLUMN ie_workspace.tenant_id        IS '租户 ID';
COMMENT ON COLUMN ie_workspace.org_id           IS '所属组织 ID';
COMMENT ON COLUMN ie_workspace.name             IS '工作空间名称';
COMMENT ON COLUMN ie_workspace.code             IS '工作空间编码（同组织内唯一，见 uk_ws_code_org）';
COMMENT ON COLUMN ie_workspace.plan_id          IS '工作空间级套餐 ID';
COMMENT ON COLUMN ie_workspace.max_apps         IS '最大应用数上限（配额约束）';
COMMENT ON COLUMN ie_workspace.max_kb_size_mb   IS '知识库容量上限（MB）';
COMMENT ON COLUMN ie_workspace.status           IS '状态：1 正常 / 0 禁用';
COMMENT ON COLUMN ie_workspace.created_at       IS '创建时间（UTC）';
COMMENT ON COLUMN ie_workspace.updated_at       IS '更新时间（UTC）';
COMMENT ON COLUMN ie_workspace.created_by       IS '创建人 user_id';
COMMENT ON COLUMN ie_workspace.updated_by       IS '最后修改人 user_id';
COMMENT ON COLUMN ie_workspace.deleted          IS '逻辑删除标记：0 正常 / 1 已删除';
-- TD §5.3 uk_ws_code_org：同一组织内 code 唯一
CREATE UNIQUE INDEX uk_ws_code_org ON ie_workspace (org_id, code) WHERE deleted = 0;

-- 成员关系表（用户-角色-空间关联；org 级管理员不挂 workspace，故 workspace_id 可空）
CREATE TABLE ie_member (
    id           BIGSERIAL PRIMARY KEY,
    tenant_id    BIGINT    NOT NULL,
    org_id       BIGINT    NOT NULL,
    workspace_id BIGINT,                             -- 可空：org 级管理员不挂 workspace
    user_id      BIGINT    NOT NULL,
    role_id      BIGINT    NOT NULL,
    joined_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by   BIGINT,
    updated_by   BIGINT,
    deleted      SMALLINT  NOT NULL DEFAULT 0
);
COMMENT ON TABLE  ie_member                IS '成员关系表：用户与角色在组织/工作空间维度的关联，一个用户可在不同空间拥有不同角色';
COMMENT ON COLUMN ie_member.id             IS '主键，自增';
COMMENT ON COLUMN ie_member.tenant_id      IS '租户 ID';
COMMENT ON COLUMN ie_member.org_id         IS '所属组织 ID';
COMMENT ON COLUMN ie_member.workspace_id   IS '所属工作空间 ID；组织级管理员可为空（不挂具体空间）';
COMMENT ON COLUMN ie_member.user_id        IS '用户 ID';
COMMENT ON COLUMN ie_member.role_id        IS '角色 ID（决定该成员在对应空间内的权限）';
COMMENT ON COLUMN ie_member.joined_at      IS '加入时间';
COMMENT ON COLUMN ie_member.created_at     IS '创建时间（UTC）';
COMMENT ON COLUMN ie_member.updated_at     IS '更新时间（UTC）';
COMMENT ON COLUMN ie_member.created_by     IS '创建人 user_id';
COMMENT ON COLUMN ie_member.updated_by     IS '最后修改人 user_id';
COMMENT ON COLUMN ie_member.deleted        IS '逻辑删除标记：0 正常 / 1 已删除';
CREATE INDEX idx_member_user ON ie_member (user_id);   -- TD §5.3：按用户查所属空间
CREATE INDEX idx_member_ws   ON ie_member (workspace_id);

-- =============================================================================
-- 组 2：权限（ie_role / ie_permission / ie_role_permission）
-- =============================================================================

-- 角色表（tenant_id=0 表示平台内置角色）
CREATE TABLE ie_role (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT       NOT NULL DEFAULT 0,    -- 0 = 平台内置
    code        VARCHAR(64)  NOT NULL,
    name        VARCHAR(128),
    scope       VARCHAR(16),                         -- ALL/ORG/WS/SELF
    builtin     SMALLINT     DEFAULT 0,              -- 1 内置（禁删）
    description TEXT,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by  BIGINT,
    updated_by  BIGINT,
    deleted     SMALLINT     NOT NULL DEFAULT 0
);
COMMENT ON TABLE  ie_role              IS '角色表：RBAC 核心，聚合一组权限；tenant_id=0 表示平台内置角色（所有租户可见）';
COMMENT ON COLUMN ie_role.id           IS '主键，自增';
COMMENT ON COLUMN ie_role.tenant_id    IS '租户 ID；0 表示平台内置角色';
COMMENT ON COLUMN ie_role.code         IS '角色编码（如 super_admin/ws_admin）';
COMMENT ON COLUMN ie_role.name         IS '角色名称';
COMMENT ON COLUMN ie_role.scope        IS '角色数据范围：ALL/ORG/WS/SELF（ABAC 数据行级过滤依据）';
COMMENT ON COLUMN ie_role.builtin      IS '是否内置：1 内置（禁止删除）/ 0 自定义';
COMMENT ON COLUMN ie_role.description  IS '角色描述';
COMMENT ON COLUMN ie_role.created_at   IS '创建时间（UTC）';
COMMENT ON COLUMN ie_role.updated_at   IS '更新时间（UTC）';
COMMENT ON COLUMN ie_role.created_by   IS '创建人 user_id';
COMMENT ON COLUMN ie_role.updated_by   IS '最后修改人 user_id';
COMMENT ON COLUMN ie_role.deleted      IS '逻辑删除标记：0 正常 / 1 已删除';
CREATE UNIQUE INDEX uk_role_code_tenant ON ie_role (tenant_id, code) WHERE deleted = 0;

-- 权限表（编码「资源:动作」，如 kb:read；见 PRD §12.2.5）
CREATE TABLE ie_permission (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(64)  NOT NULL,               -- 权限编码，如 kb:read
    name        VARCHAR(128),
    resource    VARCHAR(32),                         -- 资源类型（kb / model:vendor ...）
    action      VARCHAR(32),                         -- 动作（read/write/create/...）
    scope       VARCHAR(16),                         -- ALL/ORG/WS/SELF（数据范围，运行时 ABAC 再校验）
    description TEXT,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted     SMALLINT     NOT NULL DEFAULT 0
);
COMMENT ON TABLE  ie_permission            IS '权限表：权限字典，编码「资源:动作」（如 kb:read），是 RBAC 的最小授权单元';
COMMENT ON COLUMN ie_permission.id         IS '主键，自增';
COMMENT ON COLUMN ie_permission.code       IS '权限编码（如 kb:read、model:vendor:write），全局唯一';
COMMENT ON COLUMN ie_permission.name       IS '权限名称（前端展示）';
COMMENT ON COLUMN ie_permission.resource   IS '资源类型（kb / member / model:vendor 等）';
COMMENT ON COLUMN ie_permission.action     IS '动作（read/write/create/update/delete）';
COMMENT ON COLUMN ie_permission.scope      IS '数据范围（ALL/ORG/WS/SELF），运行时由 ABAC 二次校验';
COMMENT ON COLUMN ie_permission.description IS '权限描述';
COMMENT ON COLUMN ie_permission.created_at IS '创建时间（UTC）';
COMMENT ON COLUMN ie_permission.updated_at IS '更新时间（UTC）';
COMMENT ON COLUMN ie_permission.deleted    IS '逻辑删除标记：0 正常 / 1 已删除';
CREATE UNIQUE INDEX uk_permission_code ON ie_permission (code) WHERE deleted = 0;

-- 角色-权限关联（多对多关系表，不加审计/逻辑删除）
CREATE TABLE ie_role_permission (
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id)
);
COMMENT ON TABLE  ie_role_permission                 IS '角色-权限关联表：角色与权限多对多关系，联合主键去重';
COMMENT ON COLUMN ie_role_permission.role_id         IS '角色 ID';
COMMENT ON COLUMN ie_role_permission.permission_id   IS '权限 ID';

-- =============================================================================
-- 组 3：模型（ie_model_vendor / ie_model / ie_route_policy）
-- =============================================================================

-- 模型厂商表
CREATE TABLE ie_model_vendor (
    id                 BIGSERIAL PRIMARY KEY,
    code               VARCHAR(64)  NOT NULL,        -- qwen / openai / ollama / zhipu
    name               VARCHAR(128),
    base_url           VARCHAR(255),
    api_key_secret_id  BIGINT,                       -- 密钥 id，密钥不入库明文（TD §16.1）
    type               VARCHAR(16),                  -- CHAT / EMBEDDING / RERANK
    enabled            SMALLINT     DEFAULT 1,
    config             JSONB,
    created_at         TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by         BIGINT,
    updated_by         BIGINT,
    deleted            SMALLINT     NOT NULL DEFAULT 0
);
COMMENT ON TABLE  ie_model_vendor                  IS '模型厂商表：大模型服务提供商接入配置（通义/Ollama/智谱/OpenAI 兼容协议）';
COMMENT ON COLUMN ie_model_vendor.id               IS '主键，自增';
COMMENT ON COLUMN ie_model_vendor.code             IS '厂商编码（qwen/openai/ollama/zhipu），唯一';
COMMENT ON COLUMN ie_model_vendor.name             IS '厂商名称';
COMMENT ON COLUMN ie_model_vendor.base_url         IS 'API Base URL';
COMMENT ON COLUMN ie_model_vendor.api_key_secret_id IS '密钥 ID，关联密钥管理；API Key 不入库明文（TD §16.1）';
COMMENT ON COLUMN ie_model_vendor.type             IS '能力类型：CHAT/EMBEDDING/RERANK';
COMMENT ON COLUMN ie_model_vendor.enabled          IS '是否启用：1 启用 / 0 禁用';
COMMENT ON COLUMN ie_model_vendor.config           IS '厂商扩展配置（JSONB，如超时、代理等）';
COMMENT ON COLUMN ie_model_vendor.created_at       IS '创建时间（UTC）';
COMMENT ON COLUMN ie_model_vendor.updated_at       IS '更新时间（UTC）';
COMMENT ON COLUMN ie_model_vendor.created_by       IS '创建人 user_id';
COMMENT ON COLUMN ie_model_vendor.updated_by       IS '最后修改人 user_id';
COMMENT ON COLUMN ie_model_vendor.deleted          IS '逻辑删除标记：0 正常 / 1 已删除';
CREATE UNIQUE INDEX uk_vendor_code ON ie_model_vendor (code) WHERE deleted = 0;

-- 模型表
CREATE TABLE ie_model (
    id                  BIGSERIAL PRIMARY KEY,
    vendor_id           BIGINT        NOT NULL,
    code                VARCHAR(128)  NOT NULL,      -- qwen-plus / text-embedding-v3
    display_name        VARCHAR(128),
    type                VARCHAR(16),                  -- CHAT / EMBEDDING / RERANK
    context_window      INT,
    input_price_per_1k  DECIMAL(18,6),                -- 输入单价（元/1k token）
    output_price_per_1k DECIMAL(18,6),                -- 输出单价（元/1k token）
    enabled             SMALLINT      DEFAULT 1,
    created_at          TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP     NOT NULL DEFAULT NOW(),
    created_by          BIGINT,
    updated_by          BIGINT,
    deleted             SMALLINT      NOT NULL DEFAULT 0
);
COMMENT ON TABLE  ie_model                       IS '模型表：厂商下具体模型（qwen-plus/text-embedding-v3 等），含计价与上下文窗口';
COMMENT ON COLUMN ie_model.id                    IS '主键，自增';
COMMENT ON COLUMN ie_model.vendor_id             IS '所属厂商 ID';
COMMENT ON COLUMN ie_model.code                  IS '模型编码（如 qwen-plus），同厂商内唯一';
COMMENT ON COLUMN ie_model.display_name          IS '模型展示名';
COMMENT ON COLUMN ie_model.type                  IS '能力类型：CHAT/EMBEDDING/RERANK';
COMMENT ON COLUMN ie_model.context_window        IS '上下文窗口大小（token 数）';
COMMENT ON COLUMN ie_model.input_price_per_1k    IS '输入单价（元/千 token，DECIMAL(18,6) 保精度）';
COMMENT ON COLUMN ie_model.output_price_per_1k   IS '输出单价（元/千 token）';
COMMENT ON COLUMN ie_model.enabled               IS '是否启用：1 启用 / 0 禁用';
COMMENT ON COLUMN ie_model.created_at            IS '创建时间（UTC）';
COMMENT ON COLUMN ie_model.updated_at            IS '更新时间（UTC）';
COMMENT ON COLUMN ie_model.created_by            IS '创建人 user_id';
COMMENT ON COLUMN ie_model.updated_by            IS '最后修改人 user_id';
COMMENT ON COLUMN ie_model.deleted               IS '逻辑删除标记：0 正常 / 1 已删除';
-- TD §5.3 uk_model_vendor_code：同厂商下模型 code 唯一
CREATE UNIQUE INDEX uk_model_vendor_code ON ie_model (vendor_id, code) WHERE deleted = 0;

-- 模型路由策略表
CREATE TABLE ie_route_policy (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(128),
    rules      JSONB,                               -- 路由 DSL（PRD §12.4.5）
    priority   INT,
    enabled    SMALLINT  DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by BIGINT,
    updated_by BIGINT,
    deleted    SMALLINT  NOT NULL DEFAULT 0
);
COMMENT ON TABLE  ie_route_policy             IS '模型路由策略表：按优先级匹配的路由规则（加权/优先级/成本优化等，DSL 见 PRD §12.4.5）';
COMMENT ON COLUMN ie_route_policy.id          IS '主键，自增';
COMMENT ON COLUMN ie_route_policy.name        IS '策略名称';
COMMENT ON COLUMN ie_route_policy.rules       IS '路由 DSL（JSONB，含 strategy/fallback/匹配规则与目标模型）';
COMMENT ON COLUMN ie_route_policy.priority    IS '匹配优先级（数值越大越先匹配）';
COMMENT ON COLUMN ie_route_policy.enabled     IS '是否启用：1 启用 / 0 禁用';
COMMENT ON COLUMN ie_route_policy.created_at  IS '创建时间（UTC）';
COMMENT ON COLUMN ie_route_policy.updated_at  IS '更新时间（UTC）';
COMMENT ON COLUMN ie_route_policy.created_by  IS '创建人 user_id';
COMMENT ON COLUMN ie_route_policy.updated_by  IS '最后修改人 user_id';
COMMENT ON COLUMN ie_route_policy.deleted     IS '逻辑删除标记：0 正常 / 1 已删除';

-- =============================================================================
-- 组 4：Prompt（ie_prompt_template / ie_prompt_example / ie_prompt_debug_record）
-- =============================================================================

-- Prompt 模板表
CREATE TABLE ie_prompt_template (
    id           BIGSERIAL PRIMARY KEY,
    workspace_id BIGINT       NOT NULL,
    app_id       BIGINT,
    name         VARCHAR(128) NOT NULL,
    content      TEXT         NOT NULL,              -- 含 {{变量}} 插值
    variables    JSONB,                              -- 变量名列表
    version      INT          DEFAULT 1,
    status       SMALLINT     DEFAULT 1,
    created_by   BIGINT,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by   BIGINT,
    deleted      SMALLINT     NOT NULL DEFAULT 0
);
COMMENT ON TABLE  ie_prompt_template               IS 'Prompt 模板表：可复用提示词模板，支持 {{变量}} 插值与版本化';
COMMENT ON COLUMN ie_prompt_template.id            IS '主键，自增';
COMMENT ON COLUMN ie_prompt_template.workspace_id  IS '所属工作空间 ID';
COMMENT ON COLUMN ie_prompt_template.app_id        IS '关联应用 ID（MVP 阶段 App=Agent，可空）';
COMMENT ON COLUMN ie_prompt_template.name          IS '模板名称';
COMMENT ON COLUMN ie_prompt_template.content       IS '模板内容（含 {{变量}} 占位符）';
COMMENT ON COLUMN ie_prompt_template.variables     IS '变量名列表（JSONB 数组）';
COMMENT ON COLUMN ie_prompt_template.version       IS '版本号（每次更新 +1）';
COMMENT ON COLUMN ie_prompt_template.status        IS '状态：1 启用 / 0 停用';
COMMENT ON COLUMN ie_prompt_template.created_by    IS '创建人 user_id';
COMMENT ON COLUMN ie_prompt_template.created_at    IS '创建时间（UTC）';
COMMENT ON COLUMN ie_prompt_template.updated_at    IS '更新时间（UTC）';
COMMENT ON COLUMN ie_prompt_template.updated_by    IS '最后修改人 user_id';
COMMENT ON COLUMN ie_prompt_template.deleted       IS '逻辑删除标记：0 正常 / 1 已删除';
CREATE INDEX idx_prompt_ws ON ie_prompt_template (workspace_id);

-- Prompt Few-shot 示例表
CREATE TABLE ie_prompt_example (
    id          BIGSERIAL PRIMARY KEY,
    template_id BIGINT       NOT NULL,
    role        VARCHAR(16)  NOT NULL,               -- user / assistant
    content     TEXT,
    ord         INT          DEFAULT 0,              -- 示例顺序
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted     SMALLINT     NOT NULL DEFAULT 0
);
COMMENT ON TABLE  ie_prompt_example             IS 'Prompt Few-shot 示例表：模板附带的多轮示例，用于少样本引导';
COMMENT ON COLUMN ie_prompt_example.id          IS '主键，自增';
COMMENT ON COLUMN ie_prompt_example.template_id IS '所属模板 ID';
COMMENT ON COLUMN ie_prompt_example.role        IS '消息角色：user / assistant';
COMMENT ON COLUMN ie_prompt_example.content     IS '示例内容';
COMMENT ON COLUMN ie_prompt_example.ord         IS '示例排序号';
COMMENT ON COLUMN ie_prompt_example.created_at  IS '创建时间（UTC）';
COMMENT ON COLUMN ie_prompt_example.deleted     IS '逻辑删除标记：0 正常 / 1 已删除';
CREATE INDEX idx_example_template ON ie_prompt_example (template_id);

-- Prompt 调试记录（只增不改的日志表，不加逻辑删除）
CREATE TABLE ie_prompt_debug_record (
    id          BIGSERIAL PRIMARY KEY,
    template_id BIGINT,
    user_id     BIGINT,
    request     JSONB,
    response    JSONB,
    tokens      INT,
    cost        DECIMAL(18,6),
    latency_ms  INT,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
COMMENT ON TABLE  ie_prompt_debug_record              IS 'Prompt 调试记录表：调试工作台的调用留痕（只增不改）';
COMMENT ON COLUMN ie_prompt_debug_record.id           IS '主键，自增';
COMMENT ON COLUMN ie_prompt_debug_record.template_id  IS '模板 ID';
COMMENT ON COLUMN ie_prompt_debug_record.user_id      IS '调试人 user_id';
COMMENT ON COLUMN ie_prompt_debug_record.request      IS '调试请求快照（JSONB）';
COMMENT ON COLUMN ie_prompt_debug_record.response     IS '调试响应快照（JSONB）';
COMMENT ON COLUMN ie_prompt_debug_record.tokens       IS '本次调试消耗 token 数';
COMMENT ON COLUMN ie_prompt_debug_record.cost         IS '本次调试成本（元）';
COMMENT ON COLUMN ie_prompt_debug_record.latency_ms   IS '调用耗时（毫秒）';
COMMENT ON COLUMN ie_prompt_debug_record.created_at   IS '创建时间（UTC）';
CREATE INDEX idx_debug_template ON ie_prompt_debug_record (template_id);

-- =============================================================================
-- 组 5：知识库（ie_knowledge_base / ie_document / ie_chunk）
-- =============================================================================

-- 知识库表
CREATE TABLE ie_knowledge_base (
    id                 BIGSERIAL PRIMARY KEY,
    workspace_id       BIGINT       NOT NULL,
    name               VARCHAR(128) NOT NULL,
    embedding_model_id BIGINT,                      -- 向量化模型
    chunk_size         INT          DEFAULT 1000,   -- 切片大小
    chunk_overlap      INT          DEFAULT 200,    -- 切片重叠
    slice_strategy     VARCHAR(32)  DEFAULT 'MARKDOWN_HEADER',  -- 切片策略
    status             SMALLINT     DEFAULT 1,
    created_by         BIGINT,
    created_at         TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by         BIGINT,
    deleted            SMALLINT     NOT NULL DEFAULT 0
);
COMMENT ON TABLE  ie_knowledge_base                    IS '知识库表：RAG 引擎的文档容器，定义向量化模型与切片策略';
COMMENT ON COLUMN ie_knowledge_base.id                 IS '主键，自增';
COMMENT ON COLUMN ie_knowledge_base.workspace_id       IS '所属工作空间 ID';
COMMENT ON COLUMN ie_knowledge_base.name               IS '知识库名称';
COMMENT ON COLUMN ie_knowledge_base.embedding_model_id IS '向量化模型 ID（关联 ie_model，type=EMBEDDING）';
COMMENT ON COLUMN ie_knowledge_base.chunk_size         IS '切片大小（字符数，默认 1000）';
COMMENT ON COLUMN ie_knowledge_base.chunk_overlap      IS '切片重叠（字符数，默认 200）';
COMMENT ON COLUMN ie_knowledge_base.slice_strategy     IS '切片策略：MARKDOWN_HEADER/SENTENCE/FIXED';
COMMENT ON COLUMN ie_knowledge_base.status             IS '状态：1 启用 / 0 禁用';
COMMENT ON COLUMN ie_knowledge_base.created_by         IS '创建人 user_id';
COMMENT ON COLUMN ie_knowledge_base.created_at         IS '创建时间（UTC）';
COMMENT ON COLUMN ie_knowledge_base.updated_at         IS '更新时间（UTC）';
COMMENT ON COLUMN ie_knowledge_base.updated_by         IS '最后修改人 user_id';
COMMENT ON COLUMN ie_knowledge_base.deleted            IS '逻辑删除标记：0 正常 / 1 已删除';
CREATE INDEX idx_kb_ws ON ie_knowledge_base (workspace_id);

-- 文档表
CREATE TABLE ie_document (
    id           BIGSERIAL PRIMARY KEY,
    kb_id        BIGINT       NOT NULL,
    name         VARCHAR(255) NOT NULL,
    source_type  VARCHAR(16),                        -- pdf/docx/md/txt/csv/html
    source_url   TEXT,                               -- MinIO 对象地址
    status       VARCHAR(16)  DEFAULT 'PENDING',     -- PENDING/PROCESSING/SUCCESS/FAILED
    total_chunks INT          DEFAULT 0,
    total_tokens INT          DEFAULT 0,
    total_chars  INT          DEFAULT 0,
    error_msg    TEXT,
    created_by   BIGINT,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by   BIGINT,
    deleted      SMALLINT     NOT NULL DEFAULT 0
);
COMMENT ON TABLE  ie_document               IS '文档表：知识库内上传的文档，异步解析切片后写入 ie_chunk';
COMMENT ON COLUMN ie_document.id            IS '主键，自增';
COMMENT ON COLUMN ie_document.kb_id         IS '所属知识库 ID';
COMMENT ON COLUMN ie_document.name          IS '文档名（含扩展名）';
COMMENT ON COLUMN ie_document.source_type   IS '源文件类型：pdf/docx/md/txt/csv/html';
COMMENT ON COLUMN ie_document.source_url    IS '源文件存储地址（MinIO 对象 URL）';
COMMENT ON COLUMN ie_document.status        IS '解析状态：PENDING/PROCESSING/SUCCESS/FAILED';
COMMENT ON COLUMN ie_document.total_chunks  IS '切片总数';
COMMENT ON COLUMN ie_document.total_tokens  IS '向量化 token 总数';
COMMENT ON COLUMN ie_document.total_chars   IS '正文字符总数';
COMMENT ON COLUMN ie_document.error_msg     IS '解析失败原因';
COMMENT ON COLUMN ie_document.created_by    IS '上传人 user_id';
COMMENT ON COLUMN ie_document.created_at    IS '创建时间（UTC）';
COMMENT ON COLUMN ie_document.updated_at    IS '更新时间（UTC）';
COMMENT ON COLUMN ie_document.updated_by    IS '最后修改人 user_id';
COMMENT ON COLUMN ie_document.deleted       IS '逻辑删除标记：0 正常 / 1 已删除';
CREATE INDEX idx_doc_kb ON ie_document (kb_id);       -- TD §5.3：按 KB 查文档

-- 分片表（向量表，物理删除，不加 deleted：删除文档即删向量，见 IF §9.5）
CREATE TABLE ie_chunk (
    id         BIGSERIAL PRIMARY KEY,
    doc_id     BIGINT NOT NULL,
    kb_id      BIGINT NOT NULL,
    idx        INT,                                  -- 文档内分片序号（幂等键 doc_id+idx）
    content    TEXT,
    char_count INT,
    token_count INT,
    metadata   JSONB,                                -- 来源页/标题等非检索字段
    embedding  vector(1024),                         -- text-embedding-v3 维度 1024（TD §5.4）
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
COMMENT ON TABLE  ie_chunk             IS '文档分片表：检索最小单元，含正文、元数据与向量（PGVector 存储）';
COMMENT ON COLUMN ie_chunk.id          IS '主键，自增';
COMMENT ON COLUMN ie_chunk.doc_id       IS '所属文档 ID';
COMMENT ON COLUMN ie_chunk.kb_id        IS '所属知识库 ID';
COMMENT ON COLUMN ie_chunk.idx          IS '文档内分片序号（doc_id+idx 为幂等键）';
COMMENT ON COLUMN ie_chunk.content      IS '分片正文';
COMMENT ON COLUMN ie_chunk.char_count   IS '分片字符数';
COMMENT ON COLUMN ie_chunk.token_count  IS '分片 token 数';
COMMENT ON COLUMN ie_chunk.metadata     IS '分片元数据（JSONB，来源页/标题等，用于引用溯源与过滤）';
COMMENT ON COLUMN ie_chunk.embedding    IS '文本向量（text-embedding-v3 维度 1024）';
COMMENT ON COLUMN ie_chunk.created_at   IS '创建时间（UTC）';
CREATE INDEX idx_chunk_doc ON ie_chunk (doc_id);      -- TD §5.3：按 doc 查 chunk
CREATE INDEX idx_chunk_kb  ON ie_chunk (kb_id);       -- TD §5.3：按 KB 查 chunk
-- 向量索引：MVP 用 ivfflat（构建快、省内存），>100 万 chunk 迁 hnsw（TD §5.4 / ADR-8）
CREATE INDEX idx_chunk_embedding ON ie_chunk USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

-- =============================================================================
-- 组 6：Agent（ie_agent / ie_agent_tool / ie_agent_kb / ie_agent_invocation）
-- =============================================================================

-- Agent 表
CREATE TABLE ie_agent (
    id            BIGSERIAL PRIMARY KEY,
    workspace_id  BIGINT       NOT NULL,
    app_id        BIGINT,
    name          VARCHAR(128) NOT NULL,
    description   TEXT,
    system_prompt TEXT,
    model_id      BIGINT,
    strategy      VARCHAR(16)  DEFAULT 'REACT',      -- REACT / FUNCTION_CALL / PLAN
    max_iter      INT          DEFAULT 5,            -- ReAct 最大迭代
    timeout_ms    INT          DEFAULT 60000,        -- 全局超时
    status        SMALLINT     DEFAULT 1,
    version       INT          DEFAULT 1,            -- 当前版本号
    created_by    BIGINT,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by    BIGINT,
    deleted       SMALLINT     NOT NULL DEFAULT 0
);
COMMENT ON TABLE  ie_agent               IS 'Agent 表：由 Prompt+工具+知识库组成的可执行单元，业务价值容器';
COMMENT ON COLUMN ie_agent.id            IS '主键，自增';
COMMENT ON COLUMN ie_agent.workspace_id  IS '所属工作空间 ID';
COMMENT ON COLUMN ie_agent.app_id        IS '关联应用 ID（MVP 阶段 App=Agent，可空）';
COMMENT ON COLUMN ie_agent.name          IS 'Agent 名称';
COMMENT ON COLUMN ie_agent.description   IS 'Agent 描述';
COMMENT ON COLUMN ie_agent.system_prompt IS '系统提示词';
COMMENT ON COLUMN ie_agent.model_id      IS '默认模型 ID';
COMMENT ON COLUMN ie_agent.strategy      IS '执行策略：REACT/FUNCTION_CALL/PLAN';
COMMENT ON COLUMN ie_agent.max_iter      IS 'ReAct 最大迭代次数（默认 5）';
COMMENT ON COLUMN ie_agent.timeout_ms    IS '全局超时（毫秒，默认 60000）';
COMMENT ON COLUMN ie_agent.status        IS '状态：1 启用 / 0 禁用';
COMMENT ON COLUMN ie_agent.version       IS '当前版本号';
COMMENT ON COLUMN ie_agent.created_by    IS '创建人 user_id';
COMMENT ON COLUMN ie_agent.created_at    IS '创建时间（UTC）';
COMMENT ON COLUMN ie_agent.updated_at    IS '更新时间（UTC）';
COMMENT ON COLUMN ie_agent.updated_by    IS '最后修改人 user_id';
COMMENT ON COLUMN ie_agent.deleted       IS '逻辑删除标记：0 正常 / 1 已删除';
CREATE INDEX idx_agent_ws ON ie_agent (workspace_id);

-- Agent-工具关联（多对多）
CREATE TABLE ie_agent_tool (
    agent_id BIGINT   NOT NULL,
    tool_id  BIGINT   NOT NULL,
    enabled  SMALLINT DEFAULT 1,
    PRIMARY KEY (agent_id, tool_id)
);
COMMENT ON TABLE  ie_agent_tool          IS 'Agent-工具关联表：Agent 绑定的可用工具集（多对多）';
COMMENT ON COLUMN ie_agent_tool.agent_id IS 'Agent ID';
COMMENT ON COLUMN ie_agent_tool.tool_id  IS '工具 ID';
COMMENT ON COLUMN ie_agent_tool.enabled  IS '是否启用该绑定：1 启用 / 0 禁用';

-- Agent-知识库关联（多对多）
CREATE TABLE ie_agent_kb (
    agent_id BIGINT   NOT NULL,
    kb_id    BIGINT   NOT NULL,
    enabled  SMALLINT DEFAULT 1,
    PRIMARY KEY (agent_id, kb_id)
);
COMMENT ON TABLE  ie_agent_kb          IS 'Agent-知识库关联表：Agent 绑定的知识库集（多对多）';
COMMENT ON COLUMN ie_agent_kb.agent_id IS 'Agent ID';
COMMENT ON COLUMN ie_agent_kb.kb_id    IS '知识库 ID';
COMMENT ON COLUMN ie_agent_kb.enabled  IS '是否启用该绑定：1 启用 / 0 禁用';

-- Agent 调用记录（只增不改，供用量/调用链回溯）
CREATE TABLE ie_agent_invocation (
    id          BIGSERIAL PRIMARY KEY,
    agent_id    BIGINT,
    user_id     BIGINT,
    request     JSONB,
    response    JSONB,
    tool_calls  JSONB,
    tokens      INT,
    cost        DECIMAL(18,6),
    latency_ms  INT,
    trace_id    VARCHAR(64),
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
COMMENT ON TABLE  ie_agent_invocation               IS 'Agent 调用记录表：每次 Agent 调用的完整留痕（供用量统计与调用链回溯）';
COMMENT ON COLUMN ie_agent_invocation.id            IS '主键，自增';
COMMENT ON COLUMN ie_agent_invocation.agent_id      IS 'Agent ID';
COMMENT ON COLUMN ie_agent_invocation.user_id       IS '调用人 user_id';
COMMENT ON COLUMN ie_agent_invocation.request       IS '请求快照（JSONB）';
COMMENT ON COLUMN ie_agent_invocation.response      IS '响应快照（JSONB）';
COMMENT ON COLUMN ie_agent_invocation.tool_calls    IS '工具调用明细（JSONB）';
COMMENT ON COLUMN ie_agent_invocation.tokens        IS '消耗 token 数';
COMMENT ON COLUMN ie_agent_invocation.cost          IS '调用成本（元）';
COMMENT ON COLUMN ie_agent_invocation.latency_ms    IS '调用耗时（毫秒）';
COMMENT ON COLUMN ie_agent_invocation.trace_id      IS '链路追踪 ID';
COMMENT ON COLUMN ie_agent_invocation.created_at    IS '创建时间（UTC）';
CREATE INDEX idx_agent_inv_agent ON ie_agent_invocation (agent_id);

-- =============================================================================
-- 组 7：工具（ie_tool / ie_tool_invocation）
-- =============================================================================

-- 工具表（workspace_id 为 NULL 表示平台内置工具）
CREATE TABLE ie_tool (
    id           BIGSERIAL PRIMARY KEY,
    workspace_id BIGINT,                             -- NULL = 内置（平台级）
    code         VARCHAR(128) NOT NULL,
    name         VARCHAR(128),
    type         VARCHAR(16),                        -- HTTP/FUNCTION/DB/FILE/BUILTIN
    description  TEXT,
    schema_json  JSONB,                              -- 参数 JSON Schema
    config       JSONB,                              -- 工具配置（url/headers 等）
    enabled      SMALLINT     DEFAULT 1,
    builtin      SMALLINT     DEFAULT 0,
    created_by   BIGINT,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by   BIGINT,
    deleted      SMALLINT     NOT NULL DEFAULT 0
);
COMMENT ON TABLE  ie_tool               IS '工具表：Agent 可调用的能力单元（HTTP/函数/内置），workspace_id 为空表示平台内置';
COMMENT ON COLUMN ie_tool.id            IS '主键，自增';
COMMENT ON COLUMN ie_tool.workspace_id  IS '所属工作空间 ID；NULL 表示平台内置工具';
COMMENT ON COLUMN ie_tool.code          IS '工具编码（如 leave.query），空间内唯一';
COMMENT ON COLUMN ie_tool.name          IS '工具名称';
COMMENT ON COLUMN ie_tool.type          IS '工具类型：HTTP/FUNCTION/DB/FILE/BUILTIN';
COMMENT ON COLUMN ie_tool.description   IS '工具描述（注入 LLM 上下文）';
COMMENT ON COLUMN ie_tool.schema_json   IS '入参 JSON Schema（Function Calling 协议）';
COMMENT ON COLUMN ie_tool.config        IS '工具配置（JSONB：url/method/headers/响应提取等）';
COMMENT ON COLUMN ie_tool.enabled       IS '是否启用：1 启用 / 0 禁用';
COMMENT ON COLUMN ie_tool.builtin       IS '是否内置：1 内置 / 0 自定义';
COMMENT ON COLUMN ie_tool.created_by    IS '创建人 user_id';
COMMENT ON COLUMN ie_tool.created_at    IS '创建时间（UTC）';
COMMENT ON COLUMN ie_tool.updated_at    IS '更新时间（UTC）';
COMMENT ON COLUMN ie_tool.updated_by    IS '最后修改人 user_id';
COMMENT ON COLUMN ie_tool.deleted       IS '逻辑删除标记：0 正常 / 1 已删除';
CREATE UNIQUE INDEX uk_tool_code_ws ON ie_tool (workspace_id, code) WHERE deleted = 0;

-- 工具调用记录（只增不改）
CREATE TABLE ie_tool_invocation (
    id          BIGSERIAL PRIMARY KEY,
    tool_id     BIGINT,
    args        JSONB,
    result      JSONB,
    status      VARCHAR(16),                         -- SUCCESS / FAILED
    latency_ms  INT,
    trace_id    VARCHAR(64),
    error_msg   TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
COMMENT ON TABLE  ie_tool_invocation              IS '工具调用记录表：工具调用留痕（参数/结果/耗时/状态）';
COMMENT ON COLUMN ie_tool_invocation.id           IS '主键，自增';
COMMENT ON COLUMN ie_tool_invocation.tool_id      IS '工具 ID';
COMMENT ON COLUMN ie_tool_invocation.args         IS '调用入参（JSONB）';
COMMENT ON COLUMN ie_tool_invocation.result       IS '调用结果（JSONB）';
COMMENT ON COLUMN ie_tool_invocation.status       IS '调用状态：SUCCESS/FAILED';
COMMENT ON COLUMN ie_tool_invocation.latency_ms   IS '调用耗时（毫秒）';
COMMENT ON COLUMN ie_tool_invocation.trace_id     IS '链路追踪 ID';
COMMENT ON COLUMN ie_tool_invocation.error_msg    IS '失败原因';
COMMENT ON COLUMN ie_tool_invocation.created_at   IS '创建时间（UTC）';
CREATE INDEX idx_tool_inv_tool ON ie_tool_invocation (tool_id);

-- =============================================================================
-- 组 8：对话（ie_conv / ie_message）
-- =============================================================================

-- 会话表
CREATE TABLE ie_conv (
    id           BIGSERIAL PRIMARY KEY,
    workspace_id BIGINT       NOT NULL,
    app_id       BIGINT       NOT NULL,
    user_id      BIGINT       NOT NULL,
    title        VARCHAR(255),
    status       SMALLINT     DEFAULT 1,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted      SMALLINT     NOT NULL DEFAULT 0
);
COMMENT ON TABLE  ie_conv              IS '会话表：用户在某个应用（Agent）下发起的一次多轮对话';
COMMENT ON COLUMN ie_conv.id           IS '主键，自增';
COMMENT ON COLUMN ie_conv.workspace_id IS '所属工作空间 ID';
COMMENT ON COLUMN ie_conv.app_id       IS '关联应用 ID';
COMMENT ON COLUMN ie_conv.user_id      IS '会话发起人 user_id';
COMMENT ON COLUMN ie_conv.title        IS '会话标题';
COMMENT ON COLUMN ie_conv.status       IS '状态：1 正常 / 0 归档';
COMMENT ON COLUMN ie_conv.created_at   IS '创建时间（UTC）';
COMMENT ON COLUMN ie_conv.updated_at   IS '最后活跃时间（UTC）';
COMMENT ON COLUMN ie_conv.deleted      IS '逻辑删除标记：0 正常 / 1 已删除';
CREATE INDEX idx_conv_user ON ie_conv (user_id);
CREATE INDEX idx_conv_ws   ON ie_conv (workspace_id);

-- 消息表（"references" 为 PG 非保留关键字，故加双引号作列名，与 PRD §14.5 一致）
CREATE TABLE ie_message (
    id          BIGSERIAL PRIMARY KEY,
    conv_id     BIGINT       NOT NULL,
    role        VARCHAR(16)  NOT NULL,               -- user / assistant / system
    content     TEXT,
    tool_calls  JSONB,
    "references" JSONB,                              -- 引用溯源（chunkId/docName/page）
    feedback    SMALLINT,                            -- 1 赞 / -1 踩 / 0 取消
    latency_ms  INT,
    tokens      INT,
    trace_id    VARCHAR(64),
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);
COMMENT ON TABLE  ie_message               IS '消息表：会话内单条消息（用户/AI/系统），含工具调用与知识引用溯源';
COMMENT ON COLUMN ie_message.id            IS '主键，自增';
COMMENT ON COLUMN ie_message.conv_id       IS '所属会话 ID';
COMMENT ON COLUMN ie_message.role          IS '消息角色：user/assistant/system';
COMMENT ON COLUMN ie_message.content       IS '消息正文';
COMMENT ON COLUMN ie_message.tool_calls    IS '工具调用明细（JSONB）';
COMMENT ON COLUMN ie_message."references"  IS '知识引用溯源（JSONB：chunkId/docName/page 等）';
COMMENT ON COLUMN ie_message.feedback      IS '用户反馈：1 赞 / -1 踩 / 0 取消';
COMMENT ON COLUMN ie_message.latency_ms    IS '生成耗时（毫秒）';
COMMENT ON COLUMN ie_message.tokens        IS '消耗 token 数';
COMMENT ON COLUMN ie_message.trace_id      IS '链路追踪 ID';
COMMENT ON COLUMN ie_message.created_at    IS '创建时间（UTC）';
CREATE INDEX idx_msg_conv ON ie_message (conv_id);    -- TD §5.3：按会话查消息

-- =============================================================================
-- 组 9：计费（ie_quota / ie_usage_record / ie_bill / ie_bill_item）
-- =============================================================================

-- 配额表
CREATE TABLE ie_quota (
    id          BIGSERIAL PRIMARY KEY,
    scope_type  VARCHAR(16),                         -- TENANT/WORKSPACE/USER
    scope_id    BIGINT,
    type        VARCHAR(32),                         -- 配额类型（如 TOKEN_MONTH）
    limit_value BIGINT,
    used_value  BIGINT       DEFAULT 0,
    cycle       VARCHAR(16)  DEFAULT 'MONTH',        -- 周期（MONTH/DAY/NONE）
    reset_at    TIMESTAMP,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted     SMALLINT     NOT NULL DEFAULT 0
);
COMMENT ON TABLE  ie_quota              IS '配额表：按租户/空间/用户维度的资源上限与已用量（超限拒绝，见 IF 附录 A 8001）';
COMMENT ON COLUMN ie_quota.id           IS '主键，自增';
COMMENT ON COLUMN ie_quota.scope_type   IS '配额维度：TENANT/WORKSPACE/USER';
COMMENT ON COLUMN ie_quota.scope_id     IS '配额对象 ID（对应 scope_type 的实体 ID）';
COMMENT ON COLUMN ie_quota.type         IS '配额类型（如 TOKEN_MONTH/APP_COUNT）';
COMMENT ON COLUMN ie_quota.limit_value  IS '配额上限';
COMMENT ON COLUMN ie_quota.used_value   IS '已用量';
COMMENT ON COLUMN ie_quota.cycle        IS '重置周期：MONTH/DAY/NONE';
COMMENT ON COLUMN ie_quota.reset_at     IS '下次重置时间';
COMMENT ON COLUMN ie_quota.created_at   IS '创建时间（UTC）';
COMMENT ON COLUMN ie_quota.updated_at   IS '更新时间（UTC）';
COMMENT ON COLUMN ie_quota.deleted      IS '逻辑删除标记：0 正常 / 1 已删除';
CREATE UNIQUE INDEX uk_quota_scope_type ON ie_quota (scope_type, scope_id, type) WHERE deleted = 0;

-- 用量明细表（只增不改）
CREATE TABLE ie_usage_record (
    id         BIGSERIAL PRIMARY KEY,
    scope_type VARCHAR(16),
    scope_id   BIGINT,
    biz_type   VARCHAR(32),                          -- MODEL/TOOL/AGENT/KB
    ref_id     BIGINT,
    quantity   BIGINT,                               -- token 数或调用次数
    cost       DECIMAL(18,6),
    trace_id   VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
COMMENT ON TABLE  ie_usage_record              IS '用量明细表：模型/工具/Agent/检索调用的计量流水（异步 MQ 上报，只增不改）';
COMMENT ON COLUMN ie_usage_record.id           IS '主键，自增';
COMMENT ON COLUMN ie_usage_record.scope_type   IS '计量维度：TENANT/WORKSPACE/USER';
COMMENT ON COLUMN ie_usage_record.scope_id     IS '计量对象 ID';
COMMENT ON COLUMN ie_usage_record.biz_type     IS '业务类型：MODEL/TOOL/AGENT/KB';
COMMENT ON COLUMN ie_usage_record.ref_id       IS '业务引用 ID（模型/工具/Agent ID）';
COMMENT ON COLUMN ie_usage_record.quantity     IS '用量（token 数或调用次数）';
COMMENT ON COLUMN ie_usage_record.cost         IS '费用（元）';
COMMENT ON COLUMN ie_usage_record.trace_id     IS '链路追踪 ID';
COMMENT ON COLUMN ie_usage_record.created_at   IS '创建时间（UTC）';
CREATE INDEX idx_usage_scope_time ON ie_usage_record (scope_id, created_at);  -- TD §5.3

-- 账单表
CREATE TABLE ie_bill (
    id         BIGSERIAL PRIMARY KEY,
    scope_type VARCHAR(16),
    scope_id   BIGINT,
    period     VARCHAR(16),                          -- 账期（如 2026-08）
    total_cost DECIMAL(18,6),
    file_url   VARCHAR(255),
    status     VARCHAR(16)  DEFAULT 'GENERATED',
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted    SMALLINT     NOT NULL DEFAULT 0
);
COMMENT ON TABLE  ie_bill             IS '账单表：按账期聚合的账单，含 EasyExcel 导出文件地址';
COMMENT ON COLUMN ie_bill.id          IS '主键，自增';
COMMENT ON COLUMN ie_bill.scope_type  IS '计费维度：TENANT/WORKSPACE';
COMMENT ON COLUMN ie_bill.scope_id    IS '计费对象 ID';
COMMENT ON COLUMN ie_bill.period      IS '账期（如 2026-08）';
COMMENT ON COLUMN ie_bill.total_cost  IS '账单总费用（元）';
COMMENT ON COLUMN ie_bill.file_url    IS '账单文件地址（MinIO）';
COMMENT ON COLUMN ie_bill.status      IS '账单状态：GENERATING/GENERATED';
COMMENT ON COLUMN ie_bill.created_at  IS '创建时间（UTC）';
COMMENT ON COLUMN ie_bill.updated_at  IS '更新时间（UTC）';
COMMENT ON COLUMN ie_bill.deleted     IS '逻辑删除标记：0 正常 / 1 已删除';

-- 账单明细表
CREATE TABLE ie_bill_item (
    id       BIGSERIAL PRIMARY KEY,
    bill_id  BIGINT NOT NULL,
    biz_type VARCHAR(32),
    ref_id   BIGINT,
    quantity BIGINT,
    cost     DECIMAL(18,6)
);
COMMENT ON TABLE  ie_bill_item            IS '账单明细表：账单下的费用分项（按业务类型聚合）';
COMMENT ON COLUMN ie_bill_item.id         IS '主键，自增';
COMMENT ON COLUMN ie_bill_item.bill_id    IS '所属账单 ID';
COMMENT ON COLUMN ie_bill_item.biz_type   IS '业务类型：MODEL/TOOL/AGENT/KB';
COMMENT ON COLUMN ie_bill_item.ref_id     IS '业务引用 ID';
COMMENT ON COLUMN ie_bill_item.quantity   IS '用量（token 数或调用次数）';
COMMENT ON COLUMN ie_bill_item.cost       IS '费用（元）';
CREATE INDEX idx_bill_item_bill ON ie_bill_item (bill_id);

-- =============================================================================
-- 组 10：审计与通知（ie_audit_log / ie_notification_*）
-- =============================================================================

-- 审计日志（只增不改；before/after 为 PG 非保留关键字，加双引号，与 PRD §14.6 一致）
CREATE TABLE ie_audit_log (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT,
    user_id     BIGINT,
    action      VARCHAR(64),                         -- CREATE/UPDATE/DELETE/LOGIN...
    resource    VARCHAR(64),
    resource_id BIGINT,
    "before"    JSONB,
    "after"     JSONB,
    ip          VARCHAR(64),
    ua          VARCHAR(255),
    trace_id    VARCHAR(64),
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
COMMENT ON TABLE  ie_audit_log              IS '审计日志表：全量操作留痕（登录/增删改/调用），只增不改，90 天保留';
COMMENT ON COLUMN ie_audit_log.id           IS '主键，自增';
COMMENT ON COLUMN ie_audit_log.tenant_id    IS '租户 ID';
COMMENT ON COLUMN ie_audit_log.user_id      IS '操作人 user_id';
COMMENT ON COLUMN ie_audit_log.action       IS '动作：CREATE/UPDATE/DELETE/LOGIN/INVOKE 等';
COMMENT ON COLUMN ie_audit_log.resource     IS '资源类型（kb/agent/tool/user 等）';
COMMENT ON COLUMN ie_audit_log.resource_id  IS '资源 ID';
COMMENT ON COLUMN ie_audit_log."before"     IS '变更前快照（JSONB）';
COMMENT ON COLUMN ie_audit_log."after"      IS '变更后快照（JSONB）';
COMMENT ON COLUMN ie_audit_log.ip           IS '操作来源 IP';
COMMENT ON COLUMN ie_audit_log.ua           IS '操作来源 User-Agent';
COMMENT ON COLUMN ie_audit_log.trace_id     IS '链路追踪 ID';
COMMENT ON COLUMN ie_audit_log.created_at   IS '创建时间（UTC）';
CREATE INDEX idx_audit_time ON ie_audit_log (created_at);  -- TD §5.3：按时间查审计
CREATE INDEX idx_audit_user ON ie_audit_log (user_id);

-- 通知渠道表
CREATE TABLE ie_notification_channel (
    id           BIGSERIAL PRIMARY KEY,
    workspace_id BIGINT,
    type         VARCHAR(16),                        -- WEBHOOK/EMAIL/INBOX
    config       JSONB,
    enabled      SMALLINT  DEFAULT 1,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by   BIGINT,
    updated_by   BIGINT,
    deleted      SMALLINT  NOT NULL DEFAULT 0
);
COMMENT ON TABLE  ie_notification_channel             IS '通知渠道表：通知投递通道配置（Webhook/邮件/站内信）';
COMMENT ON COLUMN ie_notification_channel.id          IS '主键，自增';
COMMENT ON COLUMN ie_notification_channel.workspace_id IS '所属工作空间 ID';
COMMENT ON COLUMN ie_notification_channel.type        IS '渠道类型：WEBHOOK/EMAIL/INBOX';
COMMENT ON COLUMN ie_notification_channel.config      IS '渠道配置（JSONB，如钉钉机器人 URL）';
COMMENT ON COLUMN ie_notification_channel.enabled     IS '是否启用：1 启用 / 0 禁用';
COMMENT ON COLUMN ie_notification_channel.created_at  IS '创建时间（UTC）';
COMMENT ON COLUMN ie_notification_channel.updated_at  IS '更新时间（UTC）';
COMMENT ON COLUMN ie_notification_channel.created_by  IS '创建人 user_id';
COMMENT ON COLUMN ie_notification_channel.updated_by  IS '最后修改人 user_id';
COMMENT ON COLUMN ie_notification_channel.deleted     IS '逻辑删除标记：0 正常 / 1 已删除';

-- 通知模板表
CREATE TABLE ie_notification_template (
    id           BIGSERIAL PRIMARY KEY,
    code         VARCHAR(64) NOT NULL,
    name         VARCHAR(128),
    channel_type VARCHAR(16),
    content      TEXT,                               -- 含 {{变量}} 插值
    vars         JSONB,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted      SMALLINT  NOT NULL DEFAULT 0
);
COMMENT ON TABLE  ie_notification_template               IS '通知模板表：通知内容模板，支持 {{变量}} 插值';
COMMENT ON COLUMN ie_notification_template.id            IS '主键，自增';
COMMENT ON COLUMN ie_notification_template.code          IS '模板编码（如 quota_exhausted），唯一';
COMMENT ON COLUMN ie_notification_template.name          IS '模板名称';
COMMENT ON COLUMN ie_notification_template.channel_type  IS '适用渠道类型';
COMMENT ON COLUMN ie_notification_template.content       IS '模板内容（含 {{变量}} 占位符）';
COMMENT ON COLUMN ie_notification_template.vars          IS '模板变量定义（JSONB）';
COMMENT ON COLUMN ie_notification_template.created_at    IS '创建时间（UTC）';
COMMENT ON COLUMN ie_notification_template.updated_at    IS '更新时间（UTC）';
COMMENT ON COLUMN ie_notification_template.deleted       IS '逻辑删除标记：0 正常 / 1 已删除';
CREATE UNIQUE INDEX uk_notify_tpl_code ON ie_notification_template (code) WHERE deleted = 0;

-- 通知记录表
CREATE TABLE ie_notification_record (
    id          BIGSERIAL PRIMARY KEY,
    target_type VARCHAR(16),
    target_id   BIGINT,
    channel     VARCHAR(16),
    status      VARCHAR(16),                         -- PENDING/SUCCESS/FAILED
    retry_count INT         DEFAULT 0,
    payload     JSONB,
    error_msg   TEXT,
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW()
);
COMMENT ON TABLE  ie_notification_record               IS '通知记录表：通知投递流水（待发/成功/失败 + 重试次数）';
COMMENT ON COLUMN ie_notification_record.id            IS '主键，自增';
COMMENT ON COLUMN ie_notification_record.target_type   IS '接收对象类型（USER/WORKSPACE）';
COMMENT ON COLUMN ie_notification_record.target_id     IS '接收对象 ID';
COMMENT ON COLUMN ie_notification_record.channel       IS '投递渠道';
COMMENT ON COLUMN ie_notification_record.status        IS '投递状态：PENDING/SUCCESS/FAILED';
COMMENT ON COLUMN ie_notification_record.retry_count   IS '重试次数';
COMMENT ON COLUMN ie_notification_record.payload       IS '投递内容（JSONB）';
COMMENT ON COLUMN ie_notification_record.error_msg     IS '失败原因';
COMMENT ON COLUMN ie_notification_record.created_at    IS '创建时间（UTC）';

-- =============================================================================
-- 组 11：系统（ie_dict / ie_dict_item / ie_sys_config）
-- =============================================================================

-- 字典类型表
CREATE TABLE ie_dict (
    id         BIGSERIAL PRIMARY KEY,
    code       VARCHAR(64) NOT NULL,
    name       VARCHAR(128),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted    SMALLINT  NOT NULL DEFAULT 0
);
COMMENT ON TABLE  ie_dict            IS '字典类型表：业务字典分类（如模型类型、切片策略）';
COMMENT ON COLUMN ie_dict.id         IS '主键，自增';
COMMENT ON COLUMN ie_dict.code       IS '字典编码，唯一';
COMMENT ON COLUMN ie_dict.name       IS '字典名称';
COMMENT ON COLUMN ie_dict.created_at IS '创建时间（UTC）';
COMMENT ON COLUMN ie_dict.updated_at IS '更新时间（UTC）';
COMMENT ON COLUMN ie_dict.deleted    IS '逻辑删除标记：0 正常 / 1 已删除';
CREATE UNIQUE INDEX uk_dict_code ON ie_dict (code) WHERE deleted = 0;

-- 字典项表
CREATE TABLE ie_dict_item (
    id         BIGSERIAL PRIMARY KEY,
    dict_id    BIGINT      NOT NULL,
    label      VARCHAR(128),
    value      VARCHAR(128),
    sort       INT         DEFAULT 0,
    enabled    SMALLINT    DEFAULT 1,
    created_at TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP   NOT NULL DEFAULT NOW(),
    deleted    SMALLINT    NOT NULL DEFAULT 0
);
COMMENT ON TABLE  ie_dict_item            IS '字典项表：字典下的具体键值项';
COMMENT ON COLUMN ie_dict_item.id         IS '主键，自增';
COMMENT ON COLUMN ie_dict_item.dict_id    IS '所属字典类型 ID';
COMMENT ON COLUMN ie_dict_item.label      IS '字典项显示名';
COMMENT ON COLUMN ie_dict_item.value      IS '字典项值';
COMMENT ON COLUMN ie_dict_item.sort       IS '排序号';
COMMENT ON COLUMN ie_dict_item.enabled    IS '是否启用：1 启用 / 0 禁用';
COMMENT ON COLUMN ie_dict_item.created_at IS '创建时间（UTC）';
COMMENT ON COLUMN ie_dict_item.updated_at IS '更新时间（UTC）';
COMMENT ON COLUMN ie_dict_item.deleted    IS '逻辑删除标记：0 正常 / 1 已删除';
CREATE INDEX idx_dict_item_dict ON ie_dict_item (dict_id);

-- 系统配置表
CREATE TABLE ie_sys_config (
    id           BIGSERIAL PRIMARY KEY,
    config_key   VARCHAR(128) NOT NULL,
    config_value TEXT,
    description  VARCHAR(255),
    created_at   TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP   NOT NULL DEFAULT NOW(),
    deleted      SMALLINT    NOT NULL DEFAULT 0
);
COMMENT ON TABLE  ie_sys_config               IS '系统配置表：平台级开关与参数（如文档大小上限、默认模型）';
COMMENT ON COLUMN ie_sys_config.id            IS '主键，自增';
COMMENT ON COLUMN ie_sys_config.config_key    IS '配置键，唯一';
COMMENT ON COLUMN ie_sys_config.config_value  IS '配置值';
COMMENT ON COLUMN ie_sys_config.description   IS '配置说明';
COMMENT ON COLUMN ie_sys_config.created_at    IS '创建时间（UTC）';
COMMENT ON COLUMN ie_sys_config.updated_at    IS '更新时间（UTC）';
COMMENT ON COLUMN ie_sys_config.deleted       IS '逻辑删除标记：0 正常 / 1 已删除';
CREATE UNIQUE INDEX uk_sys_config_key ON ie_sys_config (config_key) WHERE deleted = 0;

-- =============================================================================
-- 种子数据（TD §18.4：管理员账号、权限字典、内置工具）
-- =============================================================================

-- 1) 管理员账号（明文 Admin@123，hash 由 BCryptPasswordEncoder strength=10 生成，$2b$ 兼容 Spring Security）
INSERT INTO ie_user (id, tenant_id, email, nickname, password_hash, status, created_by, updated_by)
VALUES (1, 1, 'admin@example.com', '管理员',
        '$2b$10$JBXZOWMQgFhQ4493ulfWjOB5NJFFrAZEclOHBWIE9vz87fVj7CLzO', 1, 0, 0);

-- 2) 默认组织 + 默认工作空间（管理员登录后的组织/空间上下文，见 IF §3.5 /auth/me）
INSERT INTO ie_organization (id, tenant_id, name, code, owner_id, status, created_by, updated_by)
VALUES (1, 1, '智擎科技', 'zhiqing', 1, 1, 1, 1);

INSERT INTO ie_workspace (id, tenant_id, org_id, name, code, status, created_by, updated_by)
VALUES (1, 1, 1, '默认空间', 'default', 1, 1, 1);

-- 3) 预置角色（PRD §12.2.2，builtin=1 禁删）
INSERT INTO ie_role (id, tenant_id, code, name, scope, builtin, description) VALUES
(1, 0, 'super_admin',   '超级管理员',   'ALL',  1, '平台超级管理员，拥有全部权限'),
(2, 0, 'org_admin',     '组织管理员',   'ORG',  1, '组织管理员，组织下一切权限'),
(3, 0, 'ws_admin',      '工作空间管理员', 'WS', 1, '工作空间管理员，空间内一切权限'),
(4, 0, 'app_developer', '应用开发者',   'WS',   1, '应用开发者，可管理知识库/Agent/工具'),
(5, 0, 'end_user',      '业务用户',     'SELF', 1, '业务用户，仅限个人使用');

-- 4) 管理员绑定 super_admin 角色（挂 org=1/ws=1 下，成员关系）
INSERT INTO ie_member (id, tenant_id, org_id, workspace_id, user_id, role_id, created_by, updated_by)
VALUES (1, 1, 1, 1, 1, 1, 1, 1);

-- 5) 权限字典（编码「资源:动作」，resource 列用于按域批量授权；完整动作粒度在阶段 3 UMS 落地时按 @PreAuthorize 细化）
INSERT INTO ie_permission (id, code, name, resource, action, scope) VALUES
-- 账号与权限
(101, 'auth:read',           '认证管理-查看',     'auth',        'read',   'ALL'),
(102, 'auth:write',          '认证管理-操作',     'auth',        'write',  'ALL'),
(103, 'org:read',            '组织-查看',         'org',         'read',   'ALL'),
(104, 'org:create',          '组织-创建',         'org',         'create', 'ALL'),
(105, 'org:write',           '组织-编辑',         'org',         'write',  'ALL'),
(106, 'org:delete',          '组织-删除',         'org',         'delete', 'ALL'),
(107, 'ws:read',             '工作空间-查看',     'ws',          'read',   'ALL'),
(108, 'ws:create',           '工作空间-创建',     'ws',          'create', 'ALL'),
(109, 'ws:write',            '工作空间-编辑',     'ws',          'write',  'ALL'),
(110, 'ws:delete',           '工作空间-删除',     'ws',          'delete', 'ALL'),
(111, 'member:read',         '成员-查看',         'member',      'read',   'ALL'),
(112, 'member:create',       '成员-邀请',         'member',      'create', 'ALL'),
(113, 'member:update',       '成员-修改角色',     'member',      'update', 'ALL'),
(114, 'member:delete',       '成员-移除',         'member',      'delete', 'ALL'),
(115, 'role:read',           '角色-查看',         'role',        'read',   'ALL'),
(116, 'role:write',          '角色-管理',         'role',        'write',  'ALL'),
-- 模型
(117, 'model:vendor:read',   '模型厂商-查看',     'model:vendor','read',   'ALL'),
(118, 'model:vendor:write',  '模型厂商-管理',     'model:vendor','write',  'ALL'),
(119, 'model:list:read',     '模型列表-查看',     'model:list',  'read',   'ALL'),
(120, 'model:list:write',    '模型-管理',         'model:list',  'write',  'ALL'),
(121, 'model:route:read',    '模型路由-查看',     'model:route', 'read',   'ALL'),
(122, 'model:route:write',   '模型路由-管理',     'model:route', 'write',  'ALL'),
(123, 'model:usage:read',    '用量监控-查看',     'model:usage', 'read',   'ALL'),
-- 知识库
(124, 'kb:read',             '知识库-查看',       'kb',          'read',   'ALL'),
(125, 'kb:write',            '知识库-管理',       'kb',          'write',  'ALL'),
(126, 'kb:doc:read',         '文档-查看',         'kb:doc',      'read',   'ALL'),
(127, 'kb:doc:write',        '文档-上传/管理',    'kb:doc',      'write',  'ALL'),
(128, 'kb:retrieval:read',   '检索-测试',         'kb:retrieval','read',   'ALL'),
-- Agent
(129, 'agent:read',          'Agent-查看',        'agent',       'read',   'ALL'),
(130, 'agent:write',         'Agent-管理',        'agent',       'write',  'ALL'),
(131, 'agent:workflow:write','工作流-编排',       'agent:workflow','write', 'ALL'),
(132, 'agent:publish:write', '发布-管理',         'agent:publish','write', 'ALL'),
-- 工具
(133, 'tool:read',           '工具-查看',         'tool',        'read',   'ALL'),
(134, 'tool:write',          '工具-管理',         'tool',        'write',  'ALL'),
(135, 'tool:builtin:read',   '内置工具-查看',     'tool:builtin','read',   'ALL'),
(136, 'tool:http:write',     '自定义HTTP工具-管理','tool:http',  'write',  'ALL'),
-- 对话
(137, 'conv:read',           '对话-查看',         'conv',        'read',   'ALL'),
(138, 'conv:write',          '对话-管理',         'conv',        'write',  'ALL'),
-- 计费
(139, 'billing:quota:read',  '配额-查看',         'billing:quota','read',  'ALL'),
(140, 'billing:quota:write', '配额-管理',         'billing:quota','write', 'ALL'),
(141, 'billing:export:read', '账单导出-查看',     'billing:export','read', 'ALL'),
-- 监控审计
(142, 'obs:metric:read',     '指标-查看',         'obs:metric',  'read',   'ALL'),
(143, 'obs:trace:read',      '调用链-查看',       'obs:trace',   'read',   'ALL'),
(144, 'audit:log:read',      '审计-查看',         'audit:log',   'read',   'ALL'),
(145, 'audit:export:read',   '审计导出-查看',     'audit:export','read',   'ALL'),
-- OpenAPI 与系统
(146, 'api:write',           'OpenAPI-管理',      'api',         'write',  'ALL'),
(147, 'system:read',         '系统设置-查看',     'system',      'read',   'ALL'),
(148, 'system:write',        '系统设置-管理',     'system',      'write',  'ALL');

-- 6) 角色授权：
--    super_admin(id=1) 拥有全部权限（TD §7.5：超管不过滤，此处显式绑定全部便于展示）
INSERT INTO ie_role_permission (role_id, permission_id)
SELECT 1, id FROM ie_permission;

--    app_developer(id=4) 拥有 kb/agent/tool/conv 域权限（PRD §12.2.2：kb:* agent:* tool:*）
INSERT INTO ie_role_permission (role_id, permission_id)
SELECT 4, id FROM ie_permission
WHERE resource LIKE 'kb%' OR resource LIKE 'agent%' OR resource LIKE 'tool%' OR resource LIKE 'conv%';

--    说明：org_admin(2)/ws_admin(3)/end_user(5) 的精细化授权依赖阶段 3 UMS 的
--    @PreAuthorize 注解与 ABAC 数据范围实现，届时再精确分配，本阶段仅预置 super_admin 与 app_developer。

-- 7) 内置工具（IF §11.6，6 个；workspace_id=NULL 表示平台级）
INSERT INTO ie_tool (id, workspace_id, code, name, type, description, builtin, enabled) VALUES
(1, NULL, 'current_time', '当前时间',  'BUILTIN', '获取当前时间',        1, 1),
(2, NULL, 'calculator',   '计算器',    'BUILTIN', '执行四则运算',        1, 1),
(3, NULL, 'uuid',         'UUID 生成', 'BUILTIN', '生成随机 UUID',       1, 1),
(4, NULL, 'md5',          'MD5 摘要',  'BUILTIN', '计算字符串 MD5 摘要', 1, 1),
(5, NULL, 'json_parse',   'JSON 解析', 'BUILTIN', '解析 JSON 字符串',    1, 1),
(6, NULL, 'http_get',     'HTTP 请求', 'BUILTIN', '发起 HTTP GET 请求',  1, 1);

-- 8) 重置自增序列（关键！）
--    显式指定 id 的种子插入不会推进 BIGSERIAL 序列，必须将序列同步到当前最大 id，
--    否则应用层首次自增插入会与已有主键冲突（duplicate key value violates unique constraint）。
SELECT setval(pg_get_serial_sequence('ie_user', 'id'),         (SELECT COALESCE(MAX(id), 1) FROM ie_user));
SELECT setval(pg_get_serial_sequence('ie_organization', 'id'), (SELECT COALESCE(MAX(id), 1) FROM ie_organization));
SELECT setval(pg_get_serial_sequence('ie_workspace', 'id'),    (SELECT COALESCE(MAX(id), 1) FROM ie_workspace));
SELECT setval(pg_get_serial_sequence('ie_member', 'id'),       (SELECT COALESCE(MAX(id), 1) FROM ie_member));
SELECT setval(pg_get_serial_sequence('ie_role', 'id'),         (SELECT COALESCE(MAX(id), 1) FROM ie_role));
SELECT setval(pg_get_serial_sequence('ie_permission', 'id'),   (SELECT COALESCE(MAX(id), 1) FROM ie_permission));
SELECT setval(pg_get_serial_sequence('ie_tool', 'id'),         (SELECT COALESCE(MAX(id), 1) FROM ie_tool));

-- =============================================================================
-- 说明（供后续阶段参考，不在本阶段创建）：
--   PRD 中提及但 TD §5.2 未列入 init.sql 的表，留待对应阶段按需补充：
--   - ie_app          应用（MVP 阶段 App=Agent，由 ie_agent 承载；V1.0 拆分再建）
--   - ie_agent_version Agent 版本快照（版本化见 TD §11.6，阶段 5 Agent 落地时补充）
--   - ie_prompt_version Prompt 版本快照（阶段 4 Prompt 工作台落地时补充）
--   - ie_secret / ie_plan 密钥与套餐表（阶段 3/8 落地时补充）
-- =============================================================================
