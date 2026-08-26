# 智擎 AI（InsightEngine）—— 数据库设计文档（DB）

> 版本：v1.0（MVP）
> 撰写日期：2026-08-26
> 关联文档：PRD（需求）、TD（技术方案 §5 数据库设计）、IF（接口）
> 本文档是数据库结构的唯一权威说明，与 `init.sql` 严格一一对应，已实机建表验证通过。

---

## 目录

- 1. 文档信息与范围
- 2. 数据库概览
- 3. 命名与通用字段规范
- 4. 表结构总览
- 5. 表详细设计（按业务域分组）
- 6. 表关系说明（ER）
- 7. 索引设计汇总
- 8. 种子数据说明
- 附录 A：通用审计字段约定

---

## 1. 文档信息与范围

| 项 | 内容 |
|----|------|
| 数据库引擎 | PostgreSQL 15 + PGVector 扩展 |
| 数据库名 | `insight_engine` |
| 字符集 / 排序 | UTF-8 / en_US.utf8（容器默认） |
| 表数量 | 35 张（业务表 32 + 关联表 3） |
| 字段总数 | 356 个（全部带 `COMMENT ON COLUMN` 说明） |
| 索引数量 | 69 个（含主键、唯一、部分唯一、向量、普通） |
| 建表脚本 | 工程根目录 `init.sql`（PG 容器首次初始化自动执行） |
| 交付目标 | 供后端 ORM 映射、DBA 运维、接口文档生成器读取元数据 |

### 1.1 连接信息（本机开发环境）

| 项 | 值 |
|----|----|
| 主机 | `127.0.0.1`（容器名 `postgres`，走 compose 内部网络） |
| 端口 | `5433`（宿主映射；容器内 `5432`，见 TD §18.2.3） |
| 数据库 | `insight_engine` |
| 用户 / 密码 | `insight` / `insight123` |
| 容器名 | `insight-postgres` |

> 微服务内部连接走 compose 内部网络：`postgres:5432`，账号同上（见 TD §18.2.4）。

---

## 2. 数据库概览

### 2.1 扩展

| 扩展 | 用途 |
|------|------|
| `vector` | PGVector 向量类型与 `vector_cosine_ops` 操作符类，支撑 `ie_chunk` 的语义检索 |

### 2.2 主键策略

全部业务表主键采用 `BIGSERIAL`（自增），理由：索引体积小、写入顺序性好、避免 UUID 主键的索引碎片（TD §5.1）。

### 2.3 时间与金额约定

| 类型 | 约定 |
|------|------|
| 时间 | `TIMESTAMP`（不带时区），统一存 UTC，展示层转换（TD §5.1） |
| 金额 | `DECIMAL(18,6)`，单位「元」，禁浮点（精度安全） |
| JSON | `JSONB`，仅用于非检索字段 |

---

## 3. 命名与通用字段规范

| 规则 | 说明 |
|------|------|
| 表前缀 | `ie_`（InsightEngine 缩写） |
| 字段命名 | 蛇形（snake_case），实体类驼峰由 MyBatis-Plus 自动映射 |
| 主键 | `id BIGSERIAL PRIMARY KEY` |
| 逻辑删除 | `deleted SMALLINT`（0 正常 / 1 删除），MyBatis-Plus 全局 `logic-delete-field: deleted` |
| 唯一约束 | 统一用「部分唯一索引 `WHERE deleted = 0`」，规避逻辑删除后重建同名记录的唯一键冲突（TD §5.5） |
| 审计字段 | `created_at / updated_at / created_by / updated_by`（详见附录 A） |
| 租户字段 | `tenant_id`，数据隔离第一维度（MVP 单租户=1，V1.0 多租户） |

---

## 4. 表结构总览

> 按 TD §5.2 建表顺序分 11 组，共 35 张表。

| 组 | 业务域 | 表 | 用途 |
|----|--------|----|------|
| 1 | 账号与组织 | ie_user | 用户账号 |
| 1 | 账号与组织 | ie_organization | 组织（一级容器） |
| 1 | 账号与组织 | ie_workspace | 工作空间（二级隔离） |
| 1 | 账号与组织 | ie_member | 成员-角色-空间关系 |
| 2 | 权限 | ie_role | 角色 |
| 2 | 权限 | ie_permission | 权限字典 |
| 2 | 权限 | ie_role_permission | 角色-权限关联 |
| 3 | 模型 | ie_model_vendor | 模型厂商 |
| 3 | 模型 | ie_model | 模型 |
| 3 | 模型 | ie_route_policy | 模型路由策略 |
| 4 | Prompt | ie_prompt_template | Prompt 模板 |
| 4 | Prompt | ie_prompt_example | Few-shot 示例 |
| 4 | Prompt | ie_prompt_debug_record | Prompt 调试记录 |
| 5 | 知识库 | ie_knowledge_base | 知识库 |
| 5 | 知识库 | ie_document | 文档 |
| 5 | 知识库 | ie_chunk | 文档分片（向量） |
| 6 | Agent | ie_agent | Agent |
| 6 | Agent | ie_agent_tool | Agent-工具关联 |
| 6 | Agent | ie_agent_kb | Agent-知识库关联 |
| 6 | Agent | ie_agent_invocation | Agent 调用记录 |
| 7 | 工具 | ie_tool | 工具 |
| 7 | 工具 | ie_tool_invocation | 工具调用记录 |
| 8 | 对话 | ie_conv | 会话 |
| 8 | 对话 | ie_message | 消息 |
| 9 | 计费 | ie_quota | 配额 |
| 9 | 计费 | ie_usage_record | 用量明细 |
| 9 | 计费 | ie_bill | 账单 |
| 9 | 计费 | ie_bill_item | 账单明细 |
| 10 | 审计通知 | ie_audit_log | 审计日志 |
| 10 | 审计通知 | ie_notification_channel | 通知渠道 |
| 10 | 审计通知 | ie_notification_template | 通知模板 |
| 10 | 审计通知 | ie_notification_record | 通知记录 |
| 11 | 系统 | ie_dict | 字典类型 |
| 11 | 系统 | ie_dict_item | 字典项 |
| 11 | 系统 | ie_sys_config | 系统配置 |

---

## 5. 表详细设计

> 说明：字段「可空」列——`否` 表示 `NOT NULL`；「默认值」列——`—` 表示无默认值。
> 每张表均含通用审计字段（见附录 A），下表不再逐行重复展开其说明，仅以「审计字段」标注。

### 5.1 账号与组织（组 1）

#### 5.1.1 ie_user — 用户表

**表说明**：平台账号主体，单租户（MVP）下所有用户归属于同一租户。

| 字段 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | 否 | 自增 | 主键 |
| tenant_id | BIGINT | 否 | — | 租户 ID（MVP 单租户=1） |
| email | VARCHAR(128) | 是 | — | 邮箱，登录账号之一（唯一索引 uk_user_email） |
| phone | VARCHAR(32) | 是 | — | 手机号，登录账号之一 |
| password_hash | VARCHAR(255) | 是 | — | 密码 BCrypt 密文，strength=10，绝不存明文 |
| nickname | VARCHAR(64) | 是 | — | 昵称 |
| avatar | VARCHAR(255) | 是 | — | 头像 URL（MinIO） |
| status | SMALLINT | 是 | 1 | 账号状态：1 正常 / 0 禁用 |
| last_login_at | TIMESTAMP | 是 | — | 最近登录时间 |
| 审计字段 | — | — | — | created_at / updated_at / created_by / updated_by / deleted |

**索引**：`uk_user_email(email) WHERE deleted=0`（唯一）、`idx_user_tenant(tenant_id)`。

#### 5.1.2 ie_organization — 组织表

**表说明**：租户内的一级容器，对应一个企业/部门，下辖多个工作空间。

| 字段 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | 否 | 自增 | 主键 |
| tenant_id | BIGINT | 否 | — | 租户 ID |
| name | VARCHAR(128) | 否 | — | 组织名称 |
| code | VARCHAR(64) | 否 | — | 组织唯一编码（同租户内唯一） |
| owner_id | BIGINT | 否 | — | 所有者 user_id |
| plan_id | BIGINT | 是 | — | 套餐 ID（V1.0 起用） |
| status | SMALLINT | 是 | 1 | 状态：1 正常 / 0 禁用 |
| 审计字段 | — | — | — | — |

**索引**：`uk_org_code_tenant(tenant_id, code) WHERE deleted=0`（唯一）。

#### 5.1.3 ie_workspace — 工作空间表

**表说明**：租户内二级隔离单元，对应业务部门/项目，是资源（KB/Agent/工具）的归属边界。

| 字段 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | 否 | 自增 | 主键 |
| tenant_id | BIGINT | 否 | — | 租户 ID |
| org_id | BIGINT | 否 | — | 所属组织 ID |
| name | VARCHAR(128) | 否 | — | 工作空间名称 |
| code | VARCHAR(64) | 否 | — | 编码（同组织内唯一，uk_ws_code_org） |
| plan_id | BIGINT | 是 | — | 工作空间级套餐 ID |
| max_apps | INT | 是 | 10 | 最大应用数上限（配额约束） |
| max_kb_size_mb | INT | 是 | 1024 | 知识库容量上限（MB） |
| status | SMALLINT | 是 | 1 | 状态：1 正常 / 0 禁用 |
| 审计字段 | — | — | — | — |

**索引**：`uk_ws_code_org(org_id, code) WHERE deleted=0`（唯一）。

#### 5.1.4 ie_member — 成员关系表

**表说明**：用户与角色在组织/工作空间维度的关联，一个用户可在不同空间拥有不同角色。

| 字段 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | 否 | 自增 | 主键 |
| tenant_id | BIGINT | 否 | — | 租户 ID |
| org_id | BIGINT | 否 | — | 所属组织 ID |
| workspace_id | BIGINT | 是 | — | 所属工作空间 ID；组织级管理员可为空 |
| user_id | BIGINT | 否 | — | 用户 ID |
| role_id | BIGINT | 否 | — | 角色 ID（决定该成员权限） |
| joined_at | TIMESTAMP | 否 | NOW() | 加入时间 |
| 审计字段 | — | — | — | — |

**索引**：`idx_member_user(user_id)`、`idx_member_ws(workspace_id)`。

### 5.2 权限（组 2）

#### 5.2.1 ie_role — 角色表

**表说明**：RBAC 核心，聚合一组权限；`tenant_id=0` 表示平台内置角色。

| 字段 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | 否 | 自增 | 主键 |
| tenant_id | BIGINT | 否 | 0 | 租户 ID；0 = 平台内置 |
| code | VARCHAR(64) | 否 | — | 角色编码（如 super_admin/ws_admin） |
| name | VARCHAR(128) | 是 | — | 角色名称 |
| scope | VARCHAR(16) | 是 | — | 数据范围：ALL/ORG/WS/SELF |
| builtin | SMALLINT | 是 | 0 | 是否内置：1 内置（禁删）/ 0 自定义 |
| description | TEXT | 是 | — | 角色描述 |
| 审计字段 | — | — | — | — |

**索引**：`uk_role_code_tenant(tenant_id, code) WHERE deleted=0`（唯一）。

#### 5.2.2 ie_permission — 权限表

**表说明**：权限字典，编码「资源:动作」（如 `kb:read`），RBAC 最小授权单元。

| 字段 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | 否 | 自增 | 主键 |
| code | VARCHAR(64) | 否 | — | 权限编码（如 kb:read、model:vendor:write），全局唯一 |
| name | VARCHAR(128) | 是 | — | 权限名称（前端展示） |
| resource | VARCHAR(32) | 是 | — | 资源类型（kb / member / model:vendor） |
| action | VARCHAR(32) | 是 | — | 动作（read/write/create/update/delete） |
| scope | VARCHAR(16) | 是 | — | 数据范围（ALL/ORG/WS/SELF），运行时 ABAC 校验 |
| description | TEXT | 是 | — | 权限描述 |
| created_at / updated_at / deleted | — | — | — | 系统字典，无 created_by/updated_by |

**索引**：`uk_permission_code(code) WHERE deleted=0`（唯一）。

#### 5.2.3 ie_role_permission — 角色-权限关联表

**表说明**：角色与权限多对多关系，联合主键去重（无审计/逻辑删除字段）。

| 字段 | 类型 | 可空 | 说明 |
|------|------|------|------|
| role_id | BIGINT | 否 | 角色 ID |
| permission_id | BIGINT | 否 | 权限 ID |

**主键**：`(role_id, permission_id)` 联合主键。

### 5.3 模型（组 3）

#### 5.3.1 ie_model_vendor — 模型厂商表

**表说明**：大模型服务提供商接入配置（通义/Ollama/智谱/OpenAI 兼容协议）。

| 字段 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | 否 | 自增 | 主键 |
| code | VARCHAR(64) | 否 | — | 厂商编码（qwen/openai/ollama/zhipu），唯一 |
| name | VARCHAR(128) | 是 | — | 厂商名称 |
| base_url | VARCHAR(255) | 是 | — | API Base URL |
| api_key_secret_id | BIGINT | 是 | — | 密钥 ID；API Key 不入库明文 |
| type | VARCHAR(16) | 是 | — | 能力类型：CHAT/EMBEDDING/RERANK |
| enabled | SMALLINT | 是 | 1 | 是否启用 |
| config | JSONB | 是 | — | 厂商扩展配置 |
| 审计字段 | — | — | — | — |

**索引**：`uk_vendor_code(code) WHERE deleted=0`（唯一）。

#### 5.3.2 ie_model — 模型表

**表说明**：厂商下具体模型（qwen-plus/text-embedding-v3 等），含计价与上下文窗口。

| 字段 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | 否 | 自增 | 主键 |
| vendor_id | BIGINT | 否 | — | 所属厂商 ID |
| code | VARCHAR(128) | 否 | — | 模型编码（同厂商内唯一） |
| display_name | VARCHAR(128) | 是 | — | 模型展示名 |
| type | VARCHAR(16) | 是 | — | CHAT/EMBEDDING/RERANK |
| context_window | INT | 是 | — | 上下文窗口（token） |
| input_price_per_1k | DECIMAL(18,6) | 是 | — | 输入单价（元/千 token） |
| output_price_per_1k | DECIMAL(18,6) | 是 | — | 输出单价（元/千 token） |
| enabled | SMALLINT | 是 | 1 | 是否启用 |
| 审计字段 | — | — | — | — |

**索引**：`uk_model_vendor_code(vendor_id, code) WHERE deleted=0`（唯一）。

#### 5.3.3 ie_route_policy — 模型路由策略表

**表说明**：按优先级匹配的路由规则（加权/优先级/成本优化等，DSL 见 PRD §12.4.5）。

| 字段 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | 否 | 自增 | 主键 |
| name | VARCHAR(128) | 是 | — | 策略名称 |
| rules | JSONB | 是 | — | 路由 DSL（strategy/fallback/匹配规则） |
| priority | INT | 是 | — | 匹配优先级（越大越先） |
| enabled | SMALLINT | 是 | 1 | 是否启用 |
| 审计字段 | — | — | — | — |

### 5.4 Prompt（组 4）

#### 5.4.1 ie_prompt_template — Prompt 模板表

**表说明**：可复用提示词模板，支持 `{{变量}}` 插值与版本化。

| 字段 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | 否 | 自增 | 主键 |
| workspace_id | BIGINT | 否 | — | 所属工作空间 ID |
| app_id | BIGINT | 是 | — | 关联应用 ID（MVP 阶段 App=Agent） |
| name | VARCHAR(128) | 否 | — | 模板名称 |
| content | TEXT | 否 | — | 模板内容（含 {{变量}}） |
| variables | JSONB | 是 | — | 变量名列表 |
| version | INT | 是 | 1 | 版本号（每次更新 +1） |
| status | SMALLINT | 是 | 1 | 状态：1 启用 / 0 停用 |
| created_by | BIGINT | 是 | — | 创建人 user_id |
| created_at / updated_at / updated_by / deleted | — | — | — | — |

**索引**：`idx_prompt_ws(workspace_id)`。

#### 5.4.2 ie_prompt_example — Prompt Few-shot 示例表

**表说明**：模板附带的多轮示例，用于少样本引导。

| 字段 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | 否 | 自增 | 主键 |
| template_id | BIGINT | 否 | — | 所属模板 ID |
| role | VARCHAR(16) | 否 | — | 消息角色：user / assistant |
| content | TEXT | 是 | — | 示例内容 |
| ord | INT | 是 | 0 | 示例排序号 |
| created_at | TIMESTAMP | 否 | NOW() | 创建时间 |
| deleted | SMALLINT | 否 | 0 | 逻辑删除 |

**索引**：`idx_example_template(template_id)`。

#### 5.4.3 ie_prompt_debug_record — Prompt 调试记录表

**表说明**：调试工作台的调用留痕（只增不改，无逻辑删除）。

| 字段 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | 否 | 自增 | 主键 |
| template_id | BIGINT | 是 | — | 模板 ID |
| user_id | BIGINT | 是 | — | 调试人 user_id |
| request | JSONB | 是 | — | 请求快照 |
| response | JSONB | 是 | — | 响应快照 |
| tokens | INT | 是 | — | 消耗 token 数 |
| cost | DECIMAL(18,6) | 是 | — | 成本（元） |
| latency_ms | INT | 是 | — | 耗时（毫秒） |
| created_at | TIMESTAMP | 否 | NOW() | 创建时间 |

**索引**：`idx_debug_template(template_id)`。

### 5.5 知识库（组 5）

#### 5.5.1 ie_knowledge_base — 知识库表

**表说明**：RAG 引擎的文档容器，定义向量化模型与切片策略。

| 字段 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | 否 | 自增 | 主键 |
| workspace_id | BIGINT | 否 | — | 所属工作空间 ID |
| name | VARCHAR(128) | 否 | — | 知识库名称 |
| embedding_model_id | BIGINT | 是 | — | 向量化模型 ID（type=EMBEDDING） |
| chunk_size | INT | 是 | 1000 | 切片大小（字符） |
| chunk_overlap | INT | 是 | 200 | 切片重叠（字符） |
| slice_strategy | VARCHAR(32) | 是 | MARKDOWN_HEADER | 切片策略 |
| status | SMALLINT | 是 | 1 | 状态：1 启用 / 0 禁用 |
| created_by | BIGINT | 是 | — | 创建人 |
| created_at / updated_at / updated_by / deleted | — | — | — | — |

**索引**：`idx_kb_ws(workspace_id)`。

#### 5.5.2 ie_document — 文档表

**表说明**：知识库内上传的文档，异步解析切片后写入 ie_chunk。

| 字段 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | 否 | 自增 | 主键 |
| kb_id | BIGINT | 否 | — | 所属知识库 ID |
| name | VARCHAR(255) | 否 | — | 文档名（含扩展名） |
| source_type | VARCHAR(16) | 是 | — | 源类型：pdf/docx/md/txt/csv/html |
| source_url | TEXT | 是 | — | 源文件存储地址（MinIO） |
| status | VARCHAR(16) | 是 | PENDING | PENDING/PROCESSING/SUCCESS/FAILED |
| total_chunks | INT | 是 | 0 | 切片总数 |
| total_tokens | INT | 是 | 0 | 向量化 token 总数 |
| total_chars | INT | 是 | 0 | 正文字符总数 |
| error_msg | TEXT | 是 | — | 解析失败原因 |
| created_by | BIGINT | 是 | — | 上传人 |
| created_at / updated_at / updated_by / deleted | — | — | — | — |

**索引**：`idx_doc_kb(kb_id)`。

#### 5.5.3 ie_chunk — 文档分片表（向量）

**表说明**：检索最小单元，含正文、元数据与向量。**物理删除**（删除文档即删向量，见 IF §9.5），故无 `deleted` 字段。

| 字段 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | 否 | 自增 | 主键 |
| doc_id | BIGINT | 否 | — | 所属文档 ID |
| kb_id | BIGINT | 否 | — | 所属知识库 ID |
| idx | INT | 是 | — | 文档内分片序号（doc_id+idx 幂等键） |
| content | TEXT | 是 | — | 分片正文 |
| char_count | INT | 是 | — | 分片字符数 |
| token_count | INT | 是 | — | 分片 token 数 |
| metadata | JSONB | 是 | — | 分片元数据（来源页/标题等） |
| embedding | vector(1024) | 是 | — | 文本向量（text-embedding-v3 维度 1024） |
| created_at | TIMESTAMP | 否 | NOW() | 创建时间 |

**索引**：`idx_chunk_doc(doc_id)`、`idx_chunk_kb(kb_id)`、`idx_chunk_embedding USING ivfflat(embedding vector_cosine_ops) WITH(lists=100)`（向量，MVP 用 ivfflat，>100 万 chunk 迁 hnsw）。

### 5.6 Agent（组 6）

#### 5.6.1 ie_agent — Agent 表

**表说明**：由 Prompt+工具+知识库组成的可执行单元，业务价值容器。

| 字段 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | 否 | 自增 | 主键 |
| workspace_id | BIGINT | 否 | — | 所属工作空间 ID |
| app_id | BIGINT | 是 | — | 关联应用 ID（MVP 阶段 App=Agent） |
| name | VARCHAR(128) | 否 | — | Agent 名称 |
| description | TEXT | 是 | — | Agent 描述 |
| system_prompt | TEXT | 是 | — | 系统提示词 |
| model_id | BIGINT | 是 | — | 默认模型 ID |
| strategy | VARCHAR(16) | 是 | REACT | 策略：REACT/FUNCTION_CALL/PLAN |
| max_iter | INT | 是 | 5 | ReAct 最大迭代次数 |
| timeout_ms | INT | 是 | 60000 | 全局超时（毫秒） |
| status | SMALLINT | 是 | 1 | 状态：1 启用 / 0 禁用 |
| version | INT | 是 | 1 | 当前版本号 |
| created_by | BIGINT | 是 | — | 创建人 |
| created_at / updated_at / updated_by / deleted | — | — | — | — |

**索引**：`idx_agent_ws(workspace_id)`。

#### 5.6.2 ie_agent_tool — Agent-工具关联表

| 字段 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| agent_id | BIGINT | 否 | — | Agent ID |
| tool_id | BIGINT | 否 | — | 工具 ID |
| enabled | SMALLINT | 是 | 1 | 是否启用该绑定 |

**主键**：`(agent_id, tool_id)`。

#### 5.6.3 ie_agent_kb — Agent-知识库关联表

| 字段 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| agent_id | BIGINT | 否 | — | Agent ID |
| kb_id | BIGINT | 否 | — | 知识库 ID |
| enabled | SMALLINT | 是 | 1 | 是否启用该绑定 |

**主键**：`(agent_id, kb_id)`。

#### 5.6.4 ie_agent_invocation — Agent 调用记录表

**表说明**：每次 Agent 调用的完整留痕（供用量统计与调用链回溯，只增不改）。

| 字段 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | 否 | 自增 | 主键 |
| agent_id | BIGINT | 是 | — | Agent ID |
| user_id | BIGINT | 是 | — | 调用人 user_id |
| request | JSONB | 是 | — | 请求快照 |
| response | JSONB | 是 | — | 响应快照 |
| tool_calls | JSONB | 是 | — | 工具调用明细 |
| tokens | INT | 是 | — | 消耗 token 数 |
| cost | DECIMAL(18,6) | 是 | — | 调用成本（元） |
| latency_ms | INT | 是 | — | 调用耗时（毫秒） |
| trace_id | VARCHAR(64) | 是 | — | 链路追踪 ID |
| created_at | TIMESTAMP | 否 | NOW() | 创建时间 |

**索引**：`idx_agent_inv_agent(agent_id)`。

### 5.7 工具（组 7）

#### 5.7.1 ie_tool — 工具表

**表说明**：Agent 可调用的能力单元（HTTP/函数/内置），`workspace_id` 为空表示平台内置。

| 字段 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | 否 | 自增 | 主键 |
| workspace_id | BIGINT | 是 | — | 所属工作空间 ID；NULL = 平台内置 |
| code | VARCHAR(128) | 否 | — | 工具编码（空间内唯一） |
| name | VARCHAR(128) | 是 | — | 工具名称 |
| type | VARCHAR(16) | 是 | — | HTTP/FUNCTION/DB/FILE/BUILTIN |
| description | TEXT | 是 | — | 工具描述（注入 LLM 上下文） |
| schema_json | JSONB | 是 | — | 入参 JSON Schema（Function Calling 协议） |
| config | JSONB | 是 | — | 工具配置（url/method/headers 等） |
| enabled | SMALLINT | 是 | 1 | 是否启用 |
| builtin | SMALLINT | 是 | 0 | 是否内置：1 内置 / 0 自定义 |
| created_by | BIGINT | 是 | — | 创建人 |
| created_at / updated_at / updated_by / deleted | — | — | — | — |

**索引**：`uk_tool_code_ws(workspace_id, code) WHERE deleted=0`（唯一）。

#### 5.7.2 ie_tool_invocation — 工具调用记录表

| 字段 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | 否 | 自增 | 主键 |
| tool_id | BIGINT | 是 | — | 工具 ID |
| args | JSONB | 是 | — | 调用入参 |
| result | JSONB | 是 | — | 调用结果 |
| status | VARCHAR(16) | 是 | — | SUCCESS / FAILED |
| latency_ms | INT | 是 | — | 耗时（毫秒） |
| trace_id | VARCHAR(64) | 是 | — | 链路追踪 ID |
| error_msg | TEXT | 是 | — | 失败原因 |
| created_at | TIMESTAMP | 否 | NOW() | 创建时间 |

**索引**：`idx_tool_inv_tool(tool_id)`。

### 5.8 对话（组 8）

#### 5.8.1 ie_conv — 会话表

**表说明**：用户在某个应用（Agent）下发起的一次多轮对话。

| 字段 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | 否 | 自增 | 主键 |
| workspace_id | BIGINT | 否 | — | 所属工作空间 ID |
| app_id | BIGINT | 否 | — | 关联应用 ID |
| user_id | BIGINT | 否 | — | 会话发起人 user_id |
| title | VARCHAR(255) | 是 | — | 会话标题 |
| status | SMALLINT | 是 | 1 | 状态：1 正常 / 0 归档 |
| created_at | TIMESTAMP | 否 | NOW() | 创建时间 |
| updated_at | TIMESTAMP | 否 | NOW() | 最后活跃时间 |
| deleted | SMALLINT | 否 | 0 | 逻辑删除 |

**索引**：`idx_conv_user(user_id)`、`idx_conv_ws(workspace_id)`。

#### 5.8.2 ie_message — 消息表

**表说明**：会话内单条消息（用户/AI/系统），含工具调用与知识引用溯源。

| 字段 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | 否 | 自增 | 主键 |
| conv_id | BIGINT | 否 | — | 所属会话 ID |
| role | VARCHAR(16) | 否 | — | 消息角色：user/assistant/system |
| content | TEXT | 是 | — | 消息正文 |
| tool_calls | JSONB | 是 | — | 工具调用明细 |
| references | JSONB | 是 | — | 知识引用溯源（chunkId/docName/page） |
| feedback | SMALLINT | 是 | — | 反馈：1 赞 / -1 踩 / 0 取消 |
| latency_ms | INT | 是 | — | 生成耗时（毫秒） |
| tokens | INT | 是 | — | 消耗 token 数 |
| trace_id | VARCHAR(64) | 是 | — | 链路追踪 ID |
| created_at | TIMESTAMP | 否 | NOW() | 创建时间 |

> 注：`references` 为 PG 非保留关键字，DDL 中以双引号 `"references"` 作列名（与 PRD §14.5 一致）。

**索引**：`idx_msg_conv(conv_id)`。

### 5.9 计费（组 9）

#### 5.9.1 ie_quota — 配额表

**表说明**：按租户/空间/用户维度的资源上限与已用量（超限拒绝，见 IF 附录 A 8001）。

| 字段 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | 否 | 自增 | 主键 |
| scope_type | VARCHAR(16) | 是 | — | 配额维度：TENANT/WORKSPACE/USER |
| scope_id | BIGINT | 是 | — | 配额对象 ID |
| type | VARCHAR(32) | 是 | — | 配额类型（TOKEN_MONTH/APP_COUNT） |
| limit_value | BIGINT | 是 | — | 配额上限 |
| used_value | BIGINT | 是 | 0 | 已用量 |
| cycle | VARCHAR(16) | 是 | MONTH | 重置周期：MONTH/DAY/NONE |
| reset_at | TIMESTAMP | 是 | — | 下次重置时间 |
| created_at / updated_at / deleted | — | — | — | — |

**索引**：`uk_quota_scope_type(scope_type, scope_id, type) WHERE deleted=0`（唯一）。

#### 5.9.2 ie_usage_record — 用量明细表

**表说明**：模型/工具/Agent/检索调用的计量流水（异步 MQ 上报，只增不改）。

| 字段 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | 否 | 自增 | 主键 |
| scope_type | VARCHAR(16) | 是 | — | 计量维度：TENANT/WORKSPACE/USER |
| scope_id | BIGINT | 是 | — | 计量对象 ID |
| biz_type | VARCHAR(32) | 是 | — | 业务类型：MODEL/TOOL/AGENT/KB |
| ref_id | BIGINT | 是 | — | 业务引用 ID |
| quantity | BIGINT | 是 | — | 用量（token 数或调用次数） |
| cost | DECIMAL(18,6) | 是 | — | 费用（元） |
| trace_id | VARCHAR(64) | 是 | — | 链路追踪 ID |
| created_at | TIMESTAMP | 否 | NOW() | 创建时间 |

**索引**：`idx_usage_scope_time(scope_id, created_at)`。

#### 5.9.3 ie_bill — 账单表

**表说明**：按账期聚合的账单，含 EasyExcel 导出文件地址。

| 字段 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | 否 | 自增 | 主键 |
| scope_type | VARCHAR(16) | 是 | — | 计费维度：TENANT/WORKSPACE |
| scope_id | BIGINT | 是 | — | 计费对象 ID |
| period | VARCHAR(16) | 是 | — | 账期（如 2026-08） |
| total_cost | DECIMAL(18,6) | 是 | — | 账单总费用（元） |
| file_url | VARCHAR(255) | 是 | — | 账单文件地址（MinIO） |
| status | VARCHAR(16) | 是 | GENERATED | 状态：GENERATING/GENERATED |
| created_at / updated_at / deleted | — | — | — | — |

#### 5.9.4 ie_bill_item — 账单明细表

| 字段 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | 否 | 自增 | 主键 |
| bill_id | BIGINT | 否 | — | 所属账单 ID |
| biz_type | VARCHAR(32) | 是 | — | 业务类型：MODEL/TOOL/AGENT/KB |
| ref_id | BIGINT | 是 | — | 业务引用 ID |
| quantity | BIGINT | 是 | — | 用量 |
| cost | DECIMAL(18,6) | 是 | — | 费用（元） |

**索引**：`idx_bill_item_bill(bill_id)`。

### 5.10 审计与通知（组 10）

#### 5.10.1 ie_audit_log — 审计日志表

**表说明**：全量操作留痕（登录/增删改/调用），只增不改，90 天保留。

| 字段 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | 否 | 自增 | 主键 |
| tenant_id | BIGINT | 是 | — | 租户 ID |
| user_id | BIGINT | 是 | — | 操作人 user_id |
| action | VARCHAR(64) | 是 | — | 动作：CREATE/UPDATE/DELETE/LOGIN/INVOKE |
| resource | VARCHAR(64) | 是 | — | 资源类型（kb/agent/tool/user） |
| resource_id | BIGINT | 是 | — | 资源 ID |
| before | JSONB | 是 | — | 变更前快照 |
| after | JSONB | 是 | — | 变更后快照 |
| ip | VARCHAR(64) | 是 | — | 操作来源 IP |
| ua | VARCHAR(255) | 是 | — | 操作来源 User-Agent |
| trace_id | VARCHAR(64) | 是 | — | 链路追踪 ID |
| created_at | TIMESTAMP | 否 | NOW() | 创建时间 |

> 注：`before`/`after` 为 PG 非保留关键字，DDL 中以双引号作列名（与 PRD §14.6 一致）。

**索引**：`idx_audit_time(created_at)`、`idx_audit_user(user_id)`。

#### 5.10.2 ie_notification_channel — 通知渠道表

| 字段 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | 否 | 自增 | 主键 |
| workspace_id | BIGINT | 是 | — | 所属工作空间 ID |
| type | VARCHAR(16) | 是 | — | WEBHOOK/EMAIL/INBOX |
| config | JSONB | 是 | — | 渠道配置（如钉钉机器人 URL） |
| enabled | SMALLINT | 是 | 1 | 是否启用 |
| created_at / updated_at / created_by / updated_by / deleted | — | — | — | — |

#### 5.10.3 ie_notification_template — 通知模板表

| 字段 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | 否 | 自增 | 主键 |
| code | VARCHAR(64) | 否 | — | 模板编码（唯一） |
| name | VARCHAR(128) | 是 | — | 模板名称 |
| channel_type | VARCHAR(16) | 是 | — | 适用渠道类型 |
| content | TEXT | 是 | — | 模板内容（含 {{变量}}） |
| vars | JSONB | 是 | — | 模板变量定义 |
| created_at / updated_at / deleted | — | — | — | — |

**索引**：`uk_notify_tpl_code(code) WHERE deleted=0`（唯一）。

#### 5.10.4 ie_notification_record — 通知记录表

| 字段 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | 否 | 自增 | 主键 |
| target_type | VARCHAR(16) | 是 | — | 接收对象类型（USER/WORKSPACE） |
| target_id | BIGINT | 是 | — | 接收对象 ID |
| channel | VARCHAR(16) | 是 | — | 投递渠道 |
| status | VARCHAR(16) | 是 | — | PENDING/SUCCESS/FAILED |
| retry_count | INT | 是 | 0 | 重试次数 |
| payload | JSONB | 是 | — | 投递内容 |
| error_msg | TEXT | 是 | — | 失败原因 |
| created_at | TIMESTAMP | 否 | NOW() | 创建时间 |

### 5.11 系统（组 11）

#### 5.11.1 ie_dict — 字典类型表

| 字段 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | 否 | 自增 | 主键 |
| code | VARCHAR(64) | 否 | — | 字典编码（唯一） |
| name | VARCHAR(128) | 是 | — | 字典名称 |
| created_at / updated_at / deleted | — | — | — | — |

**索引**：`uk_dict_code(code) WHERE deleted=0`（唯一）。

#### 5.11.2 ie_dict_item — 字典项表

| 字段 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | 否 | 自增 | 主键 |
| dict_id | BIGINT | 否 | — | 所属字典类型 ID |
| label | VARCHAR(128) | 是 | — | 字典项显示名 |
| value | VARCHAR(128) | 是 | — | 字典项值 |
| sort | INT | 是 | 0 | 排序号 |
| enabled | SMALLINT | 是 | 1 | 是否启用 |
| created_at / updated_at / deleted | — | — | — | — |

**索引**：`idx_dict_item_dict(dict_id)`。

#### 5.11.3 ie_sys_config — 系统配置表

| 字段 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | 否 | 自增 | 主键 |
| config_key | VARCHAR(128) | 否 | — | 配置键（唯一） |
| config_value | TEXT | 是 | — | 配置值 |
| description | VARCHAR(255) | 是 | — | 配置说明 |
| created_at / updated_at / deleted | — | — | — | — |

**索引**：`uk_sys_config_key(config_key) WHERE deleted=0`（唯一）。

---

## 6. 表关系说明（ER）

> 关系均为逻辑外键（未建物理 FK 约束，符合微服务「库内弱约束、应用层保证」的惯例；关联字段命名 `xxx_id` 表达引用关系）。

```
ie_organization ──1:N── ie_workspace ──1:N── ie_member
                                   │
ie_user ──1:N── ie_member          │──1:N── ie_knowledge_base ──1:N── ie_document ──1:N── ie_chunk
                                   │
ie_role ──N:M── ie_permission      │──1:N── ie_agent ──N:M── ie_tool（ie_agent_tool）
  │（ie_role_permission）          │      │
  │                                │      └──N:M── ie_knowledge_base（ie_agent_kb）
  └──1:N── ie_member.role_id       │      └──1:N── ie_agent_invocation
                                   │
                                   ├──1:N── ie_prompt_template ──1:N── ie_prompt_example
                                   ├──1:N── ie_prompt_template ──1:N── ie_prompt_debug_record
                                   ├──1:N── ie_conv ──1:N── ie_message
                                   ├──1:N── ie_tool ──1:N── ie_tool_invocation
                                   └──1:N── ie_usage_record / ie_bill ──1:N── ie_bill_item
```

### 核心关系链

| 关系 | 关联方式 | 说明 |
|------|---------|------|
| 组织 → 工作空间 | `ie_workspace.org_id` | 一级容器下辖二级容器 |
| 工作空间 → 成员 | `ie_member.workspace_id` | 用户以成员身份加入空间 |
| 角色 → 权限 | `ie_role_permission` | 多对多 |
| 成员 → 角色 | `ie_member.role_id` | 用户在空间内的角色 |
| 知识库 → 文档 → 分片 | `ie_document.kb_id` / `ie_chunk.doc_id` | RAG 三级结构 |
| Agent → 工具 | `ie_agent_tool` | 多对多 |
| Agent → 知识库 | `ie_agent_kb` | 多对多 |
| 会话 → 消息 | `ie_message.conv_id` | 一对多 |

---

## 7. 索引设计汇总

> 共 69 个：35 个主键 + 34 个业务索引（唯一 / 部分唯一 / 普通 / 向量）。

| 索引名 | 表 | 列 | 类型 | 说明 |
|--------|----|----|------|------|
| uk_user_email | ie_user | email | 部分唯一 | 邮箱唯一（逻辑删除可复用） |
| idx_user_tenant | ie_user | tenant_id | 普通 | 租户过滤 |
| uk_org_code_tenant | ie_organization | tenant_id, code | 部分唯一 | 组织编码唯一 |
| uk_ws_code_org | ie_workspace | org_id, code | 部分唯一 | 空间编码组织内唯一 |
| idx_member_user | ie_member | user_id | 普通 | 按用户查所属空间 |
| idx_member_ws | ie_member | workspace_id | 普通 | 按空间查成员 |
| uk_role_code_tenant | ie_role | tenant_id, code | 部分唯一 | 角色编码唯一 |
| uk_permission_code | ie_permission | code | 部分唯一 | 权限编码唯一 |
| uk_vendor_code | ie_model_vendor | code | 部分唯一 | 厂商编码唯一 |
| uk_model_vendor_code | ie_model | vendor_id, code | 部分唯一 | 同厂商模型编码唯一 |
| idx_prompt_ws | ie_prompt_template | workspace_id | 普通 | 按空间查模板 |
| idx_example_template | ie_prompt_example | template_id | 普通 | 按模板查示例 |
| idx_debug_template | ie_prompt_debug_record | template_id | 普通 | 按模板查调试记录 |
| idx_kb_ws | ie_knowledge_base | workspace_id | 普通 | 按空间查知识库 |
| idx_doc_kb | ie_document | kb_id | 普通 | 按 KB 查文档 |
| idx_chunk_doc | ie_chunk | doc_id | 普通 | 按文档查分片 |
| idx_chunk_kb | ie_chunk | kb_id | 普通 | 按 KB 查分片 |
| idx_chunk_embedding | ie_chunk | embedding | 向量(ivfflat) | 向量余弦检索 |
| idx_agent_ws | ie_agent | workspace_id | 普通 | 按空间查 Agent |
| idx_agent_inv_agent | ie_agent_invocation | agent_id | 普通 | 按 Agent 查调用记录 |
| uk_tool_code_ws | ie_tool | workspace_id, code | 部分唯一 | 工具编码空间内唯一 |
| idx_tool_inv_tool | ie_tool_invocation | tool_id | 普通 | 按工具查调用记录 |
| idx_conv_user | ie_conv | user_id | 普通 | 按用户查会话 |
| idx_conv_ws | ie_conv | workspace_id | 普通 | 按空间查会话 |
| idx_msg_conv | ie_message | conv_id | 普通 | 按会话查消息 |
| uk_quota_scope_type | ie_quota | scope_type, scope_id, type | 部分唯一 | 配额维度唯一 |
| idx_usage_scope_time | ie_usage_record | scope_id, created_at | 普通 | 用量按时间聚合 |
| idx_bill_item_bill | ie_bill_item | bill_id | 普通 | 按账单查明细 |
| idx_audit_time | ie_audit_log | created_at | 普通 | 审计按时间 |
| idx_audit_user | ie_audit_log | user_id | 普通 | 审计按用户 |
| uk_notify_tpl_code | ie_notification_template | code | 部分唯一 | 模板编码唯一 |
| uk_dict_code | ie_dict | code | 部分唯一 | 字典编码唯一 |
| idx_dict_item_dict | ie_dict_item | dict_id | 普通 | 按字典查项 |
| uk_sys_config_key | ie_sys_config | config_key | 部分唯一 | 配置键唯一 |

---

## 8. 种子数据说明

> init.sql 内置的初始数据（TD §18.4：管理员账号、权限字典、内置工具）。

| 表 | 条数 | 内容 |
|----|------|------|
| ie_user | 1 | 管理员 `admin@example.com`（密码 Admin@123，BCrypt 存储） |
| ie_organization | 1 | 智擎科技（code=zhiqing） |
| ie_workspace | 1 | 默认空间（code=default） |
| ie_role | 5 | super_admin / org_admin / ws_admin / app_developer / end_user |
| ie_member | 1 | 管理员绑定 super_admin（org=1 / ws=1） |
| ie_permission | 48 | 权限字典（覆盖账号/模型/知识库/Agent/工具/对话/计费/审计/系统） |
| ie_role_permission | 63 | super_admin=48（全部）+ app_developer=15（kb/agent/tool/conv） |
| ie_tool | 6 | current_time / calculator / uuid / md5 / json_parse / http_get |

> 说明：org_admin / ws_admin / end_user 的精细化授权依赖阶段 3 UMS 的 @PreAuthorize 落地后再分配。

---

## 附录 A：通用审计字段约定

> 所有业务表统一包含以下字段（关系表与只增不改的日志类表除外）。

| 字段 | 类型 | 可空 | 默认值 | 说明 |
|------|------|------|--------|------|
| created_at | TIMESTAMP | 否 | NOW() | 创建时间（UTC） |
| updated_at | TIMESTAMP | 否 | NOW() | 更新时间（UTC） |
| created_by | BIGINT | 是 | — | 创建人 user_id |
| updated_by | BIGINT | 是 | — | 最后修改人 user_id |
| deleted | SMALLINT | 否 | 0 | 逻辑删除标记：0 正常 / 1 已删除 |

**例外**：
- 关系表（ie_role_permission / ie_agent_tool / ie_agent_kb）：仅联合主键 + 必要字段，无审计/逻辑删除。
- 只增不改的日志表（ie_chunk / ie_prompt_debug_record / ie_agent_invocation / ie_tool_invocation / ie_usage_record / ie_bill_item / ie_audit_log / ie_notification_record）：仅 `created_at`，无 `updated_*` / `deleted`。

---

> **DB 文档结束。** 与 `init.sql` 一一对应，实机建表已验证（35 表 / 356 字段全注释 / 69 索引）。
