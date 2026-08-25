# 智擎 AI（InsightEngine）—— 接口设计文档（IF）

> 版本：v1.0（MVP）
> 撰写日期：2026-08-25
> 关联文档：PRD、TD（技术方案）
> 本文档定义全部 HTTP 接口契约，是前后端联调、Knife4j 文档、自动化测试的唯一依据。所有接口均以 RESTful 风格设计，统一前缀 `/api/v1`，认证类接口前缀 `/auth`。

---

## 目录

- 1. 文档信息与范围
- 2. 通用约定
- 3. 认证接口（auth）
- 4. 用户接口（user）
- 5. 组织与工作空间接口（org / workspace）
- 6. 角色与权限接口（role / permission）
- 7. 模型网关接口（model）
- 8. Prompt 接口（prompt）
- 9. 知识库接口（kb）
- 10. Agent 接口（agent）
- 11. 工具接口（tool）
- 12. 对话接口（conv）
- 13. OpenAPI 与 API Key 接口（openapi / apikey）
- 14. 计费接口（billing）
- 15. 监控与审计接口（obs / audit）
- 16. 通知接口（notify）
- 17. 系统设置接口（system）
- 附录 A：错误码总表
- 附录 B：鉴权头约定

---

## 1. 文档信息与范围

| 项 | 内容 |
|----|------|
| Base URL | `http://localhost:7000`（网关） |
| 接口前缀 | 业务：`/api/v1`，认证：`/auth` |
| 数据格式 | JSON（请求/响应），SSE（流式） |
| 编码 | UTF-8 |
| 鉴权 | 管理端接口用 JWT（`Authorization: Bearer <jwt>`）；OpenAPI 用 API Key（`Authorization: Bearer sk-ins-xxx`） |
| 文档入口 | Knife4j：`http://localhost:7000/doc.html` |

### 1.1 接口清单概览

| 模块 | 接口数（MVP） | 说明 |
|------|--------------|------|
| auth | 5 | 登录/刷新/登出/注册/用户信息 |
| user | 5 | 用户 CRUD + 密码 |
| org / workspace | 8 | 组织、空间、成员 |
| role / permission | 6 | 角色、权限、分配 |
| model | 10 | 厂商、模型、路由、调用 |
| prompt | 6 | 模板、示例、调试 |
| kb | 10 | 知识库、文档、检索 |
| agent | 9 | Agent、版本、调用 |
| tool | 7 | 工具 CRUD、调用 |
| conv | 7 | 会话、消息、流式 |
| openapi / apikey | 6 | API Key、开放接口 |
| billing | 6 | 配额、用量、账单 |
| obs / audit | 6 | 指标、调用链、审计 |
| notify | 4 | 渠道、模板、记录 |
| system | 4 | 字典、配置、公告 |
| **合计** | **~99** | |

---

## 2. 通用约定

### 2.1 通用请求头

| Header | 必填 | 说明 |
|--------|------|------|
| `Authorization` | 是（除认证接口） | `Bearer <token>` |
| `Content-Type` | 是 | `application/json` |
| `X-Trace-Id` | 否 | 链路追踪 ID，缺省由网关生成 |
| `X-Request-Id` | 否 | 幂等键，创建类接口建议传 |

### 2.2 通用响应体

```json
{
  "code": 0,
  "message": "ok",
  "data": { },
  "traceId": "ab12cd34...",
  "ts": 1724567890123
}
```

### 2.3 分页约定

请求（Query 参数）：

| 参数 | 类型 | 默认 | 说明 |
|------|------|------|------|
| pageNum | int | 1 | 页码 |
| pageSize | int | 10 | 每页条数，上限 100 |

响应（`data` 内）：

```json
{
  "records": [ ],
  "total": 120,
  "pageNum": 1,
  "pageSize": 10
}
```

### 2.4 鉴权约定

- 管理端：JWT（`Authorization: Bearer <jwt>`）
- OpenAPI：API Key（`Authorization: Bearer sk-ins-xxx`），网关按前缀 `sk-` 分流到 API Key 校验
- 未登录：`code=2001`；无权限：`code=2006`

### 2.5 时间与金额

- 时间：ISO-8601，`2026-08-25T10:30:00Z`（UTC）
- 金额：字符串，单位「元」，6 位小数，如 `"0.001200"`

---

## 3. 认证接口（auth）

### 3.1 登录

`POST /auth/login`

**请求体**：

```json
{
  "account": "admin@example.com",
  "password": "Admin@123"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| account | string | 是 | 邮箱或手机号 |
| password | string | 是 | 明文密码 |

**响应**：

```json
{
  "code": 0,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "expiresIn": 7200,
    "user": {
      "id": 10001,
      "nickname": "管理员",
      "email": "admin@example.com",
      "avatar": "http://.../avatar.png",
      "roles": ["super_admin"]
    }
  }
}
```

**错误码**：`2001` 账号不存在、`2002` 密码错误、`2003` 账号锁定、`2004` 账号禁用

**curl**：

```bash
curl -X POST http://localhost:7000/auth/login \
  -H "Content-Type: application/json" \
  -d '{"account":"admin@example.com","password":"Admin@123"}'
```

### 3.2 刷新令牌

`POST /auth/refresh`

**请求体**：

```json
{ "refreshToken": "eyJhbGciOiJIUzI1NiJ9..." }
```

**响应**：同登录的 `data`（不含 user 也可，含 user 更佳）。

### 3.3 登出

`POST /auth/logout`

**请求头**：`Authorization: Bearer <token>`

**响应**：`{"code":0,"data":null}`

**说明**：服务端将 token 加入 Redis 黑名单。

### 3.4 注册（MVP 开放，V1.0 由管理员邀请）

`POST /auth/register`

**请求体**：

```json
{
  "email": "user@example.com",
  "password": "User@123",
  "nickname": "张三"
}
```

**响应**：`data` 为用户 id。

### 3.5 当前用户信息

`GET /auth/me`

**响应**：

```json
{
  "code": 0,
  "data": {
    "id": 10001,
    "nickname": "管理员",
    "email": "admin@example.com",
    "avatar": "...",
    "roles": ["super_admin"],
    "tenantId": 1,
    "workspaceId": 1,
    "workspaceName": "默认空间"
  }
}
```

---

## 4. 用户接口（user）

### 4.1 用户分页列表

`GET /api/v1/user/page?pageNum=1&pageSize=10&keyword=张`

| Query | 类型 | 说明 |
|-------|------|------|
| keyword | string | 昵称/邮箱模糊 |

**响应 `data.records[]`**：

```json
{
  "id": 10001,
  "nickname": "张三",
  "email": "zhangsan@example.com",
  "phone": "138****1234",
  "status": 1,
  "createdAt": "2026-08-01T10:00:00Z"
}
```

### 4.2 创建用户（管理员）

`POST /api/v1/user`

**请求体**：

```json
{
  "email": "new@example.com",
  "nickname": "新用户",
  "password": "New@123",
  "phone": "13800001234",
  "roleId": 3
}
```

**权限**：`member:create`

### 4.3 更新用户

`PUT /api/v1/user/{id}`

**请求体**：`{ "nickname": "新昵称", "phone": "..." }`

### 4.4 启用/禁用用户

`PUT /api/v1/user/{id}/status`

**请求体**：`{ "status": 0 }`（0 禁用 1 启用）

### 4.5 修改密码

`PUT /api/v1/user/password`

**请求体**：

```json
{ "oldPassword": "Admin@123", "newPassword": "Admin@456" }
```

---

## 5. 组织与工作空间接口

### 5.1 创建组织

`POST /api/v1/org`

**请求体**：

```json
{ "name": "智擎科技", "code": "zhiqing" }
```

**权限**：`org:create`

### 5.2 组织详情

`GET /api/v1/org/{id}`

### 5.3 创建/更新工作空间

`POST /api/v1/workspace` / `PUT /api/v1/workspace/{id}`

**请求体**：

```json
{
  "orgId": 1,
  "name": "研发部",
  "code": "rd",
  "maxApps": 10,
  "maxKbSizeMb": 1024
}
```

**权限**：`ws:create`

### 5.4 工作空间列表

`GET /api/v1/workspace/page?orgId=1&pageNum=1&pageSize=10`

### 5.5 切换当前工作空间

`POST /api/v1/workspace/switch`

**请求体**：`{ "workspaceId": 2 }`

**响应**：返回新 token（重签 JWT 携带新 ws_id）。

### 5.6 成员管理

- `GET /api/v1/member/page?workspaceId=1&pageNum=1&pageSize=10` — 成员列表
- `POST /api/v1/member/invite` — 邀请成员

```json
{ "workspaceId": 1, "email": "user@example.com", "roleId": 4 }
```

- `DELETE /api/v1/member/{id}` — 移除成员
- `PUT /api/v1/member/{id}/role` — 修改角色

```json
{ "roleId": 5 }
```

**权限**：`member:read` / `member:create` / `member:delete` / `member:update`

---

## 6. 角色与权限接口

### 6.1 角色列表

`GET /api/v1/role/list`

**响应 `data`**：

```json
[
  { "id": 1, "code": "super_admin", "name": "超级管理员", "builtin": 1 },
  { "id": 2, "code": "org_admin", "name": "组织管理员", "builtin": 1 },
  { "id": 3, "code": "ws_admin", "name": "工作空间管理员", "builtin": 1 },
  { "id": 4, "code": "app_developer", "name": "应用开发者", "builtin": 1 },
  { "id": 5, "code": "end_user", "name": "业务用户", "builtin": 1 }
]
```

### 6.2 创建角色

`POST /api/v1/role`

**请求体**：

```json
{
  "code": "hr_operator",
  "name": "HR 运营",
  "permissionIds": [101, 102]
}
```

### 6.3 权限树（用于角色授权界面）

`GET /api/v1/permission/tree`

**响应 `data`**（按 resource 分组树）：

```json
[
  {
    "resource": "kb",
    "name": "知识库",
    "children": [
      { "id": 101, "code": "kb:read", "name": "查看知识库" },
      { "id": 102, "code": "kb:write", "name": "编辑知识库" }
    ]
  }
]
```

### 6.4 角色授权

`PUT /api/v1/role/{id}/permissions`

**请求体**：`{ "permissionIds": [101, 102, 103] }`

### 6.5 角色详情

`GET /api/v1/role/{id}`

### 6.6 删除角色

`DELETE /api/v1/role/{id}`（`builtin=1` 禁止删除，返回 `1003`）

---

## 7. 模型网关接口（model）

### 7.1 模型厂商 CRUD

- `GET /api/v1/model/vendor/page` — 厂商分页
- `POST /api/v1/model/vendor` — 创建厂商

```json
{
  "code": "qwen",
  "name": "通义千问",
  "baseUrl": "https://dashscope.aliyuncs.com/compatible-mode/v1",
  "apiKey": "sk-xxxx",
  "type": "CHAT"
}
```

- `PUT /api/v1/model/vendor/{id}` — 更新
- `DELETE /api/v1/model/vendor/{id}` — 删除

**权限**：`model:vendor:write`

### 7.2 模型列表

`GET /api/v1/model/page?vendorId=1&type=CHAT&pageNum=1&pageSize=10`

**响应 `data.records[]`**：

```json
{
  "id": 1,
  "vendorId": 1,
  "code": "qwen-plus",
  "displayName": "通义千问 Plus",
  "type": "CHAT",
  "contextWindow": 131072,
  "inputPricePer1k": "0.000800",
  "outputPricePer1k": "0.002000",
  "enabled": 1
}
```

### 7.3 创建/更新/删除模型

`POST /api/v1/model` / `PUT /api/v1/model/{id}` / `DELETE /api/v1/model/{id}`

### 7.4 模型路由策略

- `GET /api/v1/model/route/list` — 策略列表
- `POST /api/v1/model/route` — 创建策略

```json
{
  "name": "默认路由",
  "priority": 1,
  "rules": {
    "strategy": "PRIORITY",
    "fallback": true,
    "rules": [
      { "match": {"tenantTier": "PRO"}, "targets": [{"modelId": 1}, {"modelId": 2}] }
    ]
  }
}
```

- `PUT /api/v1/model/route/{id}` — 更新
- `PUT /api/v1/model/route/{id}/status` — 启用/禁用

### 7.5 聊天补全（核心，兼容 OpenAI 协议）

`POST /api/v1/model/chat/completions`

**请求体**：

```json
{
  "model": "auto",
  "messages": [
    {"role": "system", "content": "你是企业助手"},
    {"role": "user", "content": "你好"}
  ],
  "stream": false,
  "temperature": 0.7,
  "maxTokens": 1024
}
```

**响应（非流式）**：

```json
{
  "code": 0,
  "data": {
    "id": "cmpl-abc",
    "model": "qwen-plus",
    "choices": [
      { "index": 0, "message": {"role": "assistant", "content": "你好！"}, "finishReason": "stop" }
    ],
    "usage": { "promptTokens": 12, "completionTokens": 3, "totalTokens": 15 }
  }
}
```

**流式（stream=true）**：SSE，事件 `message`（delta）/ `error` / `finish`（含 usage）。

**curl**：

```bash
curl -X POST http://localhost:7000/api/v1/model/chat/completions \
  -H "Authorization: Bearer <jwt>" \
  -H "Content-Type: application/json" \
  -d '{"model":"auto","messages":[{"role":"user","content":"你好"}],"stream":false}'
```

### 7.6 Embedding

`POST /api/v1/model/embeddings`

**请求体**：

```json
{ "model": "text-embedding-v3", "input": ["你好", "世界"] }
```

**响应**：

```json
{
  "code": 0,
  "data": {
    "model": "text-embedding-v3",
    "data": [
      {"index": 0, "embedding": [0.01, -0.02], "tokens": 1},
      {"index": 1, "embedding": [0.03, 0.01], "tokens": 1}
    ],
    "usage": {"totalTokens": 2}
  }
}
```

### 7.7 Rerank

`POST /api/v1/model/rerank`

**请求体**：

```json
{
  "model": "qwen-rerank",
  "query": "年假规定",
  "documents": ["文档片段1", "文档片段2", "文档片段3"],
  "topN": 2
}
```

**响应**：

```json
{
  "code": 0,
  "data": {
    "results": [
      {"index": 2, "score": 0.92},
      {"index": 0, "score": 0.81}
    ]
  }
}
```

### 7.8 模型用量查询

`GET /api/v1/model/usage/page?modelId=1&start=2026-08-01&end=2026-08-25&pageNum=1&pageSize=10`

---

## 8. Prompt 接口（prompt）

### 8.1 模板 CRUD

- `GET /api/v1/prompt/page?keyword=&pageNum=1&pageSize=10` — 分页
- `POST /api/v1/prompt` — 创建

```json
{
  "name": "客服问答",
  "content": "你是{{company}}的客服，请回答：{{question}}",
  "variables": ["company", "question"],
  "appId": 10
}
```

- `GET /api/v1/prompt/{id}` — 详情
- `PUT /api/v1/prompt/{id}` — 更新（版本号 +1）
- `DELETE /api/v1/prompt/{id}` — 删除

### 8.2 示例（Few-shot）管理

`POST /api/v1/prompt/{id}/examples`

**请求体**：

```json
{
  "examples": [
    {"role": "user", "content": "示例问题1"},
    {"role": "assistant", "content": "示例回答1"}
  ]
}
```

### 8.3 调试（流式）

`POST /api/v1/prompt/{id}/debug`

**请求体**：

```json
{
  "variables": {"company": "智擎科技", "question": "你们支持私有化吗"},
  "modelId": 1,
  "temperature": 0.7
}
```

**响应**：SSE 流式，同聊天补全流式格式；同时服务端记录 `prompt_debug_record`。

### 8.4 调试历史

`GET /api/v1/prompt/{id}/debug-records?pageNum=1&pageSize=10`

### 8.5 版本列表

`GET /api/v1/prompt/{id}/versions`

### 8.6 回滚版本

`PUT /api/v1/prompt/{id}/rollback`

**请求体**：`{ "version": 2 }`

---

## 9. 知识库接口（kb）

### 9.1 知识库 CRUD

- `GET /api/v1/kb/page?keyword=&pageNum=1&pageSize=10` — 分页

**响应 `data.records[]`**：

```json
{
  "id": 1,
  "name": "员工手册",
  "embeddingModelId": 10,
  "embeddingModelName": "text-embedding-v3",
  "chunkSize": 1000,
  "chunkOverlap": 200,
  "sliceStrategy": "MARKDOWN_HEADER",
  "docCount": 12,
  "chunkCount": 3450,
  "status": 1,
  "createdAt": "2026-08-01T10:00:00Z"
}
```

- `POST /api/v1/kb` — 创建

```json
{
  "name": "员工手册",
  "embeddingModelId": 10,
  "chunkSize": 1000,
  "chunkOverlap": 200,
  "sliceStrategy": "MARKDOWN_HEADER"
}
```

- `GET /api/v1/kb/{id}` — 详情
- `PUT /api/v1/kb/{id}` — 更新
- `DELETE /api/v1/kb/{id}` — 删除

**权限**：`kb:read` / `kb:write`

### 9.2 文档上传

`POST /api/v1/kb/{kbId}/doc/upload`

**Content-Type**：`multipart/form-data`

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | file | 是 | pdf/docx/md/txt/csv/html，≤50MB |
| metadata | string | 否 | JSON 字符串，自定义元数据 |

**响应**：

```json
{
  "code": 0,
  "data": { "docId": 200, "status": "PENDING" }
}
```

**curl**：

```bash
curl -X POST http://localhost:7000/api/v1/kb/1/doc/upload \
  -H "Authorization: Bearer <jwt>" \
  -F "file=@/path/员工手册.pdf" \
  -F 'metadata={"dept":"HR"}'
```

### 9.3 文档列表

`GET /api/v1/kb/{kbId}/doc/page?status=PENDING&pageNum=1&pageSize=10`

**响应 `data.records[]`**：

```json
{
  "id": 200,
  "name": "员工手册.pdf",
  "sourceType": "pdf",
  "status": "SUCCESS",
  "totalChunks": 120,
  "totalTokens": 45000,
  "totalChars": 88000,
  "errorMsg": null,
  "createdAt": "2026-08-01T10:00:00Z"
}
```

**status 枚举**：`PENDING`（待解析）/ `PROCESSING`（解析中）/ `SUCCESS` / `FAILED`

### 9.4 文档详情

`GET /api/v1/kb/{kbId}/doc/{docId}`

### 9.5 删除文档

`DELETE /api/v1/kb/{kbId}/doc/{docId}`（同时删除 chunk 向量）

### 9.6 重新解析

`POST /api/v1/kb/{kbId}/doc/{docId}/reparse`

### 9.7 知识检索（核心）

`POST /api/v1/kb/{kbId}/retrieve`

**请求体**：

```json
{
  "query": "年假是怎么规定的？",
  "topK": 5,
  "scoreThreshold": 0.5,
  "metadataFilter": {"dept": "HR"}
}
```

**响应**：

```json
{
  "code": 0,
  "data": {
    "chunks": [
      {
        "chunkId": 10001,
        "docId": 200,
        "docName": "员工手册.pdf",
        "content": "年假天数根据司龄计算：满1年5天...",
        "score": 0.87,
        "metadata": {"page": 12, "dept": "HR"}
      }
    ],
    "usage": {"embeddingTokens": 5, "rerankTokens": 1024}
  }
}
```

### 9.8 检索测试（多策略对比，供调试）

`POST /api/v1/kb/{kbId}/retrieve/compare`

**请求体**：

```json
{
  "query": "年假规定",
  "strategies": ["VECTOR", "HYBRID", "HYBRID_RERANK"],
  "topK": 5
}
```

**响应**：按策略分组返回结果，用于对比召回效果。

### 9.9 分片列表（供溯源查看）

`GET /api/v1/kb/{kbId}/doc/{docId}/chunks?pageNum=1&pageSize=20`

### 9.10 知识库统计

`GET /api/v1/kb/{kbId}/stats`

**响应**：

```json
{
  "code": 0,
  "data": {
    "docCount": 12,
    "chunkCount": 3450,
    "totalTokens": 1560000,
    "totalChars": 3200000,
    "storageBytes": 8900000,
    "lastIndexedAt": "2026-08-20T10:00:00Z"
  }
}
```

---

## 10. Agent 接口（agent）

### 10.1 Agent CRUD

- `GET /api/v1/agent/page?keyword=&pageNum=1&pageSize=10` — 分页

**响应 `data.records[]`**：

```json
{
  "id": 1,
  "name": "HR 助手",
  "description": "回答 HR 政策问题",
  "modelId": 1,
  "modelName": "qwen-plus",
  "strategy": "REACT",
  "toolCount": 2,
  "kbCount": 1,
  "status": 1,
  "version": 3,
  "liveVersion": 3,
  "updatedAt": "2026-08-20T10:00:00Z"
}
```

- `POST /api/v1/agent` — 创建

```json
{
  "name": "HR 助手",
  "description": "回答 HR 政策问题",
  "systemPrompt": "你是 HR 助手，基于知识库回答，可调用工具查询。",
  "modelId": 1,
  "strategy": "REACT",
  "maxIter": 5,
  "timeoutMs": 60000
}
```

- `GET /api/v1/agent/{id}` — 详情（含工具、知识库关联）
- `PUT /api/v1/agent/{id}` — 更新
- `DELETE /api/v1/agent/{id}` — 删除

**权限**：`agent:read` / `agent:write`

### 10.2 Agent 关联工具/知识库

- `PUT /api/v1/agent/{id}/tools` — 绑定工具

```json
{ "toolIds": [8, 9, 10] }
```

- `PUT /api/v1/agent/{id}/kbs` — 绑定知识库

```json
{ "kbIds": [1] }
```

### 10.3 Agent 调用（核心，流式）

`POST /api/v1/agent/{id}/invoke`

**请求体**：

```json
{
  "input": "帮我查一下张三的年假余额",
  "stream": true,
  "conversationId": 9001,
  "context": {"employeeId": "zhangsan"}
}
```

**流式响应（SSE）**，事件类型：

| event | 说明 | data 示例 |
|-------|------|-----------|
| `message` | 文本增量 | `{"id":"msg-1","delta":"张三","traceId":"..."}` |
| `tool_call` | 工具调用 | `{"name":"leave.query","args":{"userId":"zhangsan"}}` |
| `tool_result` | 工具结果 | `{"name":"leave.query","result":{"remaining":5}}` |
| `reference` | 引用 | `{"docName":"员工手册.pdf","page":8,"content":"..."}` |
| `error` | 错误 | `{"code":5001,"message":"..."}` |
| `finish` | 结束 | `{"usage":{"totalTokens":144},"latencyMs":1320}` |

**非流式（stream=false）**：

```json
{
  "code": 0,
  "data": {
    "id": "inv-xxx",
    "output": "张三剩余年假 5 天。",
    "toolCalls": [
      {"name": "leave.query", "args": {"userId": "zhangsan"}, "result": {"remaining": 5}}
    ],
    "references": [
      {"chunkId": 10001, "docName": "员工手册.pdf", "page": 8}
    ],
    "usage": {"promptTokens": 120, "completionTokens": 24, "totalTokens": 144},
    "latencyMs": 1320
  }
}
```

**curl（流式）**：

```bash
curl -N -X POST http://localhost:7000/api/v1/agent/1/invoke \
  -H "Authorization: Bearer <jwt>" \
  -H "Content-Type: application/json" \
  -d '{"input":"查张三年假","stream":true}'
```

### 10.4 Agent 版本管理

- `GET /api/v1/agent/{id}/versions` — 版本列表
- `POST /api/v1/agent/{id}/publish` — 发布（将当前草稿发布为 live 版本）

```json
{ "version": 3 }
```

- `POST /api/v1/agent/{id}/rollback` — 回滚

```json
{ "version": 2 }
```

### 10.5 Agent 调用记录

`GET /api/v1/agent/{id}/invocations?start=&end=&pageNum=1&pageSize=10`

### 10.6 工作流 DSL 保存（V1.0）

`PUT /api/v1/agent/{id}/workflow`

**请求体**：`{ "dsl": { "nodes": [], "edges": [] } }`

### 10.7 Agent 启用/禁用

`PUT /api/v1/agent/{id}/status`

```json
{ "status": 0 }
```

---

## 11. 工具接口（tool）

### 11.1 工具列表

`GET /api/v1/tool/page?type=HTTP&builtin=0&keyword=&pageNum=1&pageSize=10`

**响应 `data.records[]`**：

```json
{
  "id": 8,
  "code": "leave.query",
  "name": "年假查询",
  "type": "HTTP",
  "description": "查询员工年假余额",
  "builtin": 0,
  "enabled": 1
}
```

### 11.2 工具详情

`GET /api/v1/tool/{id}`

**响应（含 schema + config）**：

```json
{
  "code": 0,
  "data": {
    "id": 8,
    "code": "leave.query",
    "name": "年假查询",
    "type": "HTTP",
    "description": "查询员工年假余额",
    "schema": {
      "type": "object",
      "properties": {"userId": {"type": "string", "description": "员工工号"}},
      "required": ["userId"]
    },
    "config": {
      "method": "GET",
      "url": "https://hr.internal.example.com/api/leave",
      "headers": {"X-Token": "{{secret.hrToken}}"},
      "query": {"userId": "{{args.userId}}"}
    },
    "builtin": 0,
    "enabled": 1
  }
}
```

### 11.3 创建工具

`POST /api/v1/tool`

**请求体**：同详情（不含 id）

### 11.4 更新/删除工具

`PUT /api/v1/tool/{id}` / `DELETE /api/v1/tool/{id}`

### 11.5 工具调试（测试调用）

`POST /api/v1/tool/{id}/test`

**请求体**：

```json
{ "args": {"userId": "zhangsan"} }
```

**响应**：

```json
{
  "code": 0,
  "data": {
    "result": {"remaining": 5},
    "latencyMs": 120,
    "status": "SUCCESS"
  }
}
```

### 11.6 内置工具列表

`GET /api/v1/tool/builtin`

**响应**：内置 6 个工具（current_time / calculator / uuid / md5 / json_parse / http_get）。

### 11.7 工具调用记录

`GET /api/v1/tool/{id}/invocations?pageNum=1&pageSize=10`

---

## 12. 对话接口（conv）

### 12.1 创建会话

`POST /api/v1/conv`

**请求体**：

```json
{
  "appId": 10,
  "title": "关于年假的咨询"
}
```

**响应**：`{"code":0,"data":{"id":9001}}`

### 12.2 会话列表

`GET /api/v1/conv/page?appId=10&pageNum=1&pageSize=10`

### 12.3 会话详情（含消息）

`GET /api/v1/conv/{id}`

**响应**：

```json
{
  "code": 0,
  "data": {
    "id": 9001,
    "appId": 10,
    "title": "关于年假的咨询",
    "createdAt": "2026-08-25T10:00:00Z",
    "messages": [
      {
        "id": 50001,
        "role": "user",
        "content": "年假怎么算",
        "createdAt": "2026-08-25T10:00:01Z"
      },
      {
        "id": 50002,
        "role": "assistant",
        "content": "年假根据司龄计算...",
        "toolCalls": [],
        "references": [{"docName": "员工手册.pdf", "page": 12}],
        "feedback": null,
        "latencyMs": 1320,
        "tokens": 144,
        "createdAt": "2026-08-25T10:00:03Z"
      }
    ]
  }
}
```

### 12.4 发送消息（核心，流式）

`POST /api/v1/conv/{id}/message`

**请求体**：

```json
{
  "content": "帮我查张三的年假",
  "stream": true
}
```

**响应**：SSE，事件同 Agent 调用（`message` / `tool_call` / `tool_result` / `reference` / `error` / `finish`）。

**说明**：该接口内部会调用 Agent 服务，并把消息持久化到 `ie_message`。

### 12.5 历史消息分页

`GET /api/v1/conv/{id}/messages?pageNum=1&pageSize=20`

### 12.6 反馈

`POST /api/v1/conv/{id}/message/{messageId}/feedback`

**请求体**：`{ "feedback": 1 }`（1 赞 / -1 踩 / 0 取消）

### 12.7 删除会话

`DELETE /api/v1/conv/{id}`

### 12.8 会话重命名

`PUT /api/v1/conv/{id}/title`

**请求体**：`{ "title": "新标题" }`

---

## 13. OpenAPI 与 API Key 接口

### 13.1 API Key 列表

`GET /api/v1/apikey/page?pageNum=1&pageSize=10`

**响应 `data.records[]`**：

```json
{
  "id": 1,
  "name": "生产环境 Key",
  "keyPrefix": "sk-ins-ab12",
  "key": "sk-ins-ab12****",   // 仅创建时返回完整，列表脱敏
  "enabled": 1,
  "rateLimit": 100,
  "createdAt": "2026-08-01T10:00:00Z"
}
```

### 13.2 创建 API Key

`POST /api/v1/apikey`

**请求体**：

```json
{ "name": "生产环境 Key", "rateLimit": 100 }
```

**响应**（完整 key 仅此一次返回）：

```json
{
  "code": 0,
  "data": { "id": 1, "key": "sk-ins-ab12cd34ef56", "keyPrefix": "sk-ins-ab12" }
}
```

### 13.3 启用/禁用/删除 Key

`PUT /api/v1/apikey/{id}/status` / `DELETE /api/v1/apikey/{id}`

### 13.4 OpenAPI：Agent 调用（开放）

`POST /api/v1/openapi/agent/{appCode}/invoke`

**请求头**：`Authorization: Bearer sk-ins-xxx`

**请求体**：

```json
{
  "input": "查张三年假",
  "stream": false
}
```

**响应**：同 Agent 调用非流式。

### 13.5 OpenAPI：知识检索（开放）

`POST /api/v1/openapi/kb/{kbCode}/retrieve`

**请求头**：`Authorization: Bearer sk-ins-xxx`

**请求体**：

```json
{ "query": "年假规定", "topK": 5 }
```

### 13.6 OpenAPI：对话流式（开放，SSE）

`POST /api/v1/openapi/agent/{appCode}/stream`

**说明**：与 13.4 相同，`stream=true` 的 SSE 版本。

---

## 14. 计费接口（billing）

### 14.1 套餐与配额查询

`GET /api/v1/billing/quota?scopeType=WORKSPACE&scopeId=1`

**响应**：

```json
{
  "code": 0,
  "data": [
    {"type": "TOKEN_MONTH", "limit": 1000000, "used": 234000, "cycle": "MONTH", "resetAt": "2026-09-01T00:00:00Z"},
    {"type": "APP_COUNT", "limit": 10, "used": 3, "cycle": "NONE"}
  ]
}
```

### 14.2 调整配额

`PUT /api/v1/billing/quota`

**请求体**：

```json
{
  "scopeType": "WORKSPACE",
  "scopeId": 1,
  "type": "TOKEN_MONTH",
  "limit": 2000000
}
```

**权限**：`billing:quota:write`

### 14.3 用量明细

`GET /api/v1/billing/usage/page?bizType=MODEL&scopeId=1&start=&end=&pageNum=1&pageSize=10`

**响应 `data.records[]`**：

```json
{
  "id": 1,
  "scopeType": "WORKSPACE",
  "scopeId": 1,
  "bizType": "MODEL",
  "refId": 1,
  "quantity": 15000,
  "cost": "0.012000",
  "traceId": "xxx",
  "createdAt": "2026-08-25T10:00:00Z"
}
```

### 14.4 账单列表

`GET /api/v1/billing/bill/page?scopeId=1&pageNum=1&pageSize=10`

**响应 `data.records[]`**：

```json
{
  "id": 1,
  "scopeId": 1,
  "period": "2026-08",
  "totalCost": "128.500000",
  "status": "GENERATED",
  "fileUrl": "http://.../bill-2026-08.xlsx"
}
```

### 14.5 生成账单

`POST /api/v1/billing/bill/generate`

**请求体**：`{ "scopeId": 1, "period": "2026-08" }`

**说明**：异步生成，EasyExcel 输出，完成后回填 `fileUrl`。

### 14.6 下载账单

`GET /api/v1/billing/bill/{id}/download`

**响应**：文件流（`application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`）。

---

## 15. 监控与审计接口（obs / audit）

### 15.1 系统指标概览

`GET /api/v1/obs/metrics/summary`

**响应**：

```json
{
  "code": 0,
  "data": {
    "todayRequests": 12000,
    "todayTokens": 8900000,
    "todayCost": "12.300000",
    "todayErrorRate": 0.8,
    "avgLatencyMs": 320,
    "activeAgents": 8
  }
}
```

### 15.2 指标趋势

`GET /api/v1/obs/metrics/trend?metric=requests&granularity=HOUR&start=&end=`

**响应**：`data` 为时间序列 `[{t:"2026-08-25T10:00:00Z", v:1200}]`

### 15.3 调用链详情

`GET /api/v1/obs/trace/{traceId}`

**响应**：

```json
{
  "code": 0,
  "data": {
    "traceId": "xxx",
    "spans": [
      {"name": "gateway", "startMs": 0, "durationMs": 1400},
      {"name": "agent.invoke", "startMs": 10, "durationMs": 1300},
      {"name": "model.chat", "startMs": 30, "durationMs": 800},
      {"name": "tool.leave.query", "startMs": 400, "durationMs": 120},
      {"name": "kb.retrieve", "startMs": 900, "durationMs": 200}
    ]
  }
}
```

### 15.4 审计日志分页

`GET /api/v1/audit/page?userId=&action=&resource=&start=&end=&pageNum=1&pageSize=10`

**响应 `data.records[]`**：

```json
{
  "id": 1,
  "userId": 10001,
  "userName": "管理员",
  "action": "CREATE",
  "resource": "kb",
  "resourceId": 1,
  "ip": "192.168.1.10",
  "traceId": "xxx",
  "createdAt": "2026-08-25T10:00:00Z"
}
```

### 15.5 审计日志详情

`GET /api/v1/audit/{id}`（含 before/after JSON）

### 15.6 审计日志导出

`GET /api/v1/audit/export?start=&end=`（EasyExcel，文件流）

---

## 16. 通知接口（notify）

### 16.1 通知渠道列表/创建/更新

`GET /api/v1/notify/channel/list` / `POST /api/v1/notify/channel` / `PUT /api/v1/notify/channel/{id}`

**创建请求体（Webhook）**：

```json
{
  "type": "WEBHOOK",
  "name": "钉钉机器人",
  "config": {"url": "https://oapi.dingtalk.com/robot/send?access_token=xxx"}
}
```

### 16.2 模板管理

`GET /api/v1/notify/template/list` / `POST /api/v1/notify/template`

```json
{
  "code": "quota_exhausted",
  "name": "配额耗尽提醒",
  "content": "您的{{type}}配额已用尽，请及时充值。"
}
```

### 16.3 通知记录

`GET /api/v1/notify/record/page?status=&pageNum=1&pageSize=10`

### 16.4 手动触发测试

`POST /api/v1/notify/test`

```json
{ "channelId": 1, "templateCode": "quota_exhausted", "vars": {"type": "Token"} }
```

---

## 17. 系统设置接口（system）

### 17.1 字典管理

- `GET /api/v1/system/dict/page` — 字典类型分页
- `GET /api/v1/system/dict/{code}/items` — 字典项列表
- `POST /api/v1/system/dict` / `POST /api/v1/system/dict/{code}/item` — 创建

### 17.2 系统配置

`GET /api/v1/system/config` / `PUT /api/v1/system/config`

```json
{
  "maxDocSizeMb": 50,
  "defaultModelId": 1,
  "enableRegister": true,
  "enableSensitiveFilter": false
}
```

### 17.3 公告

`GET /api/v1/system/notice/list` / `POST /api/v1/system/notice`

### 17.4 关于（版本信息）

`GET /api/v1/system/about`

**响应**：

```json
{
  "code": 0,
  "data": {
    "version": "1.0.0",
    "buildTime": "2026-08-25T10:00:00Z",
    "techStack": "Spring Boot 3.2 / Spring Cloud Alibaba / Spring AI / Vue 3"
  }
}
```

---

## 附录 A：错误码总表

| 码段 | 错误码 | 含义 | HTTP 状态 |
|------|--------|------|-----------|
| 通用 | 0 | 成功 | 200 |
| 通用 | 1001 | 参数错误 | 400 |
| 通用 | 1002 | 请求体缺失/格式错误 | 400 |
| 通用 | 1003 | 不允许的操作（如删内置） | 403 |
| 通用 | 1004 | 资源不存在 | 404 |
| 通用 | 9999 | 系统内部错误 | 500 |
| 认证 | 2001 | 未登录 | 401 |
| 认证 | 2002 | 密码错误 | 401 |
| 认证 | 2003 | 账号已锁定 | 403 |
| 认证 | 2004 | 账号已禁用 | 403 |
| 认证 | 2005 | 验证码错误 | 400 |
| 认证 | 2006 | 无权限 | 403 |
| 认证 | 2007 | token 已过期 | 401 |
| 模型 | 3001 | 模型不存在 | 404 |
| 模型 | 3002 | 模型调用超时 | 504 |
| 模型 | 3003 | 模型限流 | 429 |
| 模型 | 3004 | 模型调用失败 | 502 |
| 模型 | 3005 | 密钥错误 | 500 |
| 知识库 | 4001 | 知识库不存在 | 404 |
| 知识库 | 4002 | 文档解析失败 | 500 |
| 知识库 | 4003 | Embedding 失败 | 500 |
| 知识库 | 4004 | 检索超时 | 504 |
| 知识库 | 4005 | 文档格式不支持 | 400 |
| 知识库 | 4006 | 文档过大 | 413 |
| Agent | 5001 | Agent 不存在 | 404 |
| Agent | 5002 | 超过最大迭代次数 | 500 |
| Agent | 5003 | 工具调用失败 | 500 |
| Agent | 5004 | 工作流 DSL 非法 | 400 |
| 工具 | 6001 | 工具不存在 | 404 |
| 工具 | 6002 | 工具调用超时 | 504 |
| 工具 | 6003 | 工具已禁用 | 403 |
| 工具 | 6004 | 内网地址禁止访问 | 403 |
| 对话 | 7001 | 会话不存在 | 404 |
| 计费 | 8001 | 配额不足 | 429 |
| 计费 | 8002 | 余额不足 | 429 |
| 计费 | 8003 | 账单生成中 | 409 |
| 系统 | 9001 | 配置不存在 | 404 |

---

## 附录 B：鉴权头约定

| 场景 | Header |
|------|--------|
| 管理端（登录用户） | `Authorization: Bearer <jwt>` |
| OpenAPI（第三方系统） | `Authorization: Bearer sk-ins-xxx` |
| 流式请求 | 同管理端/OpenAPI，另需 `Accept: text/event-stream` |

---

> **IF 结束。**
>
> 三份文档已齐：《PRD》（做什么）→《TD》（怎么做）→《IF》（接口契约）。
>
> 后续可选交付：
> 1. 《OPS-部署运维》：Docker Compose 完整编排、初始化 SQL、启动步骤、常见问题排查
> 2. 直接进入**代码落地**：搭建 Maven 多模块骨架 + 第一个可运行的微服务（建议从 UMS 认证服务开始）
