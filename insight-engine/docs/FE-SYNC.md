# FE-SYNC — 后端 → 前端 联调就绪信号（跨端同步协议）

> **本文档是**：后端写给前端的"公告板" —— 哪个模块能联调了、前端提的问题答复到哪一步了。
> **何时看**：前端每轮对话开工必读；后端模块状态变化 / 答复前端问题后必更新。
> **不负责**：前端提的问题（在**前端仓库** `docs/BE-ISSUES.md`）、后端内部进度（→ `PROGRESS.md`）。

> 本文件 = **前后端跨仓库协作协议 + 后端给前端的进度投影**。
> 前端每轮对话**开工必读**；后端每次模块状态变化 / 答复前端问题后**必须更新**。
> 相关：后端进度真相源 `docs/PROGRESS.md`｜前端信箱 `D:\JavaProject\insight-engine-web\docs\BE-ISSUES.md`｜接口契约 `docs/IF.md`（唯一事实源）。

---

## 0. 协议总览（先读这段）

| 方向 | 文件（路径写死，禁止改名/挪位置） | 谁写 | 谁读 | 更新时机 |
| --- | --- | --- | --- | --- |
| FE → BE | 前端 `docs/BE-ISSUES.md` | 前端 | 后端 | 前端发现「后端能力欠缺 / 接口对不上 / 契约缺口」时写入；后端开工必读 |
| BE → FE | 本文件 `docs/FE-SYNC.md` | 后端 | 前端 | 后端模块就绪 / 契约变更 / 答复前端诉求时更新；前端开工必读 |

**条目状态机**：`🟡 待后端答复` → `🔵 后端处理中` → `✅ 已答复/已修复（附证据）` → `🟢 前端已验证闭环`

**三条铁律**
1. 后端更新 `PROGRESS.md` 模块看板时，**必须同步本文件 §1**（禁止只改一边，这是 2026-09-09 之前 gap 的直接成因）。
2. 后端答复前端条目后，**必须双写**：本文件 §3 登记摘要 + 前端 `BE-ISSUES.md` 该条目回填「后端答复 + 证据」并改状态（前端不必来回跳仓库找答案）。
3. 前端切真联调前，必须在本文件 §1 看到该模块 `✅` + curl 证据；否则只能 mock。**后端没实现的功能，前端不许声称联调**。

---

## 1. 模块联调就绪清单（BE → FE）

> 状态：⚪ 未开始 / 🔵 开发中 / ✅ 已就绪（前端可切真联调）
> 证据列格式：`curl 命令摘要 + 响应 code + traceId + 日期`；无证据一律写「未验证」。

| 模块 | 接口前缀 | 状态 | 前端可联调 | 证据（curl + traceId + 日期） | 备注 |
| --- | --- | --- | --- | --- | --- |
| UMS 认证 | `/auth/**`、`/api/v1/user\|role\|permission/**` | ✅ 已就绪 | 是 | 2026-09-08 全链路冒烟 8/8（登录/me/refresh 轮换/logout 撤销/401/403/2006），见 PROGRESS §六 6.4 | 前端 P1/P2 已真联调；register 已修复 |
| gateway 网关 | 前端统一入口 `:7000` | ✅ 已就绪（冒烟级） | 是 | **2026-09-09 Nacos `lb://` 冒烟 8/8**（gateway 经 Nacos 服务发现转发 UMS，寻址方式对前端无感）：login/me/user/page/role/list 200（code=0）/ 无 token 401-2001 / 坏 token 401-2001 / 错口令 401-2002 / `/doc.html` 200；另有同日直连冒烟 9/9（登录/转发/2001/2001/2007/`/doc.html`/sk-2001/2006/404，`X-Trace-Id` 单值） | 路由已按 TD §8.3 收窄（user/role/permission 专属前缀）；TraceGlobalFilter(-200)、错误响应 charset=UTF-8、JWT Claim 常量下沉 common 已落地；Nacos 云容器已部署（39.106.110.214:8848，1:1 端口）+ gateway/UMS 接入代码完成（`fail-fast:false`、路由改 `lb://insight-engine-ums`），**注册实机验证通过（2026-09-09：Nacos 实例 UP、`lb://` 转发冒烟 8/8）**。**PR #5（`f84ffd2`）已合入 master**；**2026-09-16 新增 workspace 路由**：`/api/v1/org\|workspace\|member/**` → `lb://insight-engine-workspace`；**同日 `lb://` 服务发现双服务复验通过**（云端 Nacos 名单 UMS/workspace healthy、gateway 不注册；经网关 login code=0 / `org/1` 200 / `workspace/page` 200 / `user/page` 200 / `kb/x` 404） |
| workspace 工作空间 | `/api/v1/org\|workspace\|member/**` | ✅ 已就绪 | **是**（可从 mock 清单移除） | **2026-09-16 实机冒烟 28/28**（UMS 7101 + workspace 7102，云 PG/Redis）：登录 200 → 组织详情 200（`traceId=null` 成功态，错误态如 `929767985c9b43d291e5110f5e22f663`）→ 空间创建 200（id=3）/ 更新 200 / 分页 200（名与配额更新生效）→ 成员自动挂 ws_admin、重复邀请 1001（`traceId=0aec7d43efbd4bcd82427ab4c0eedf91`）/ 添加 200 / 改角色 200 / 移除 200 → 非成员切换 403-2006 / 移除后不可见 → 切换空间 200（新 token）→ **旧 token 立即 401-2001** → `/auth/me` 新 token 返回 `wsId=3 wsName=Smoke WS v2` → 删除当前空间 403-1003（`traceId=929767985c9b43d291e5110f5e22f663`）/ 删除目标空间 200 / 删后成员接口 404-1004（`traceId=7c27a17e77984dbbbaf2d2f6f0698244`）/ 非法编码 1001（`traceId=032ddde1ca00448c9b375195676f86fc`）；**另经网关 :7000 复验**：`/api/v1/org/1`、`/api/v1/workspace/page`、`/api/v1/member/page` 转 workspace 均 200（`X-Trace-Id` 单值），未接入前缀 `/api/v1/kb/x` 网关 404，无 token 401-2001 | 组织/空间/成员 **12** 个端点（含 §5.7 `GET /api/v1/workspace/{id}/my-permissions`）；`/auth/me` 工作空间语义修正为「当前空间（JWT ws_id）」；**新增 `DELETE /api/v1/workspace/{id}`**；⚠️ 添加成员路径为 `/api/v1/member/invite`（前端当前请求 `/api/v1/member`，需同步，见 §2）。**2026-09-17 收口**：空间维度鉴权（第二层）生效 + 角色授予收紧（只能授 `scope=WS/SELF`）；冒烟 `insight-engine/scripts/smoke-workspace-permission.ps1` **17 项断言 ALL PASS** + 跨租户实测 `1004`（证据见 §2 `[2026-09-17]` 三条）；⬜ 前端待办：空间内门控改用 `my-permissions`、角色下拉过滤 `WS/SELF` |
| model 模型网关 | `/api/v1/model/**` | 🔵 开发中 | **部分可联调：厂商 CRUD**（模型/路由/chat 仍 mock） | **2026-09-17 厂商 CRUD 实机冒烟 12/12**（`scripts/smoke-model-vendor.ps1`，隔离实例 :17101/:17103，云 PG/Redis）：`POST /vendor`（带 apiKey）→ 200；分页回 `maskedHint=sk-****7890` + `hasApiKey=true` 且**响应/分页均不含明文**；`PUT` 不带 apiKey → 密钥不变；带新 apiKey → 掩码轮换；重复 code → `1001`；未知 id → `1004`；`DELETE` → 200 后不再列出。DB 核对：`ie_secret` 为密文（AES-256-GCM/v1），明文泄露 0 行 | `/api/v1/model/vendor/page`(read) / `POST`、`PUT /{id}`、`DELETE /{id}`(write)。**契约要点**：① `apiKey` 明文入参、服务端加密，**读取只回 `maskedHint`（如 `sk-****1a2b`）**，永不下发明文；② `PUT` 的 `apiKey` **留空 = 不改密钥**（前端别用掩码值回填提交）；③ 厂商编码 `code` 唯一且**不可修改**；④ `hasApiKey=false` 表示无鉴权的本地模型（如 Ollama）。⬜ 模型/路由策略/chat completions(SSE) 未实现，**不得声称可联调** | **阶段 6（2026-09-17 判定：有条件进入）**：IF §7.1~§7.8 契约完整、权限码 `model:*` 已在字典；但**开工前先补 DB 缺口**（`ie_secret` 密钥表缺 → 待裁决"新建 or 复用 `ie_sys_config`"；厂商/模型种子为空）→ 清单见 `PROGRESS §七 3`。（2026-09-17 更正：路由策略表**已存在**，名为 `ie_route_policy`，原先"表缺失"的登记系误判，已撤回。）**前端现在可先做模型管理页 UI（照 IF §7 契约 + mock），等 §1 置 ✅ 再切真** |
| kb 知识库 | `/api/v1/kb/**` | ⚪ 未开始 | 否（mock） | — | 上传 + 状态轮询 |
| tool 工具市场 | `/api/v1/tool/**` | ⚪ 未开始 | 否（mock） | — | JSON Schema |
| agent 编排 | `/api/v1/agent/**` | ⚪ 未开始 | 否（mock） | — | ★SSE 最复杂 |
| conv 对话服务 | `/api/v1/conv/**` | ⚪ 未开始 | 否（mock） | — | SSE |
| billing 计费 | `/api/v1/billing/**` | ⚪ 未开始 | 否（mock） | — | 金额字符串 |
| obs 监控审计 | `/api/v1/audit/**` | ⚪ 未开始 | 否（mock） | — | span 甘特 |
| notify 通知 | `/api/v1/notify/**` | ⚪ 未开始 | 否（mock） | — | |

---

## 2. 契约变更记录（IF.md 相关，增量追加）

> 后端改 `IF.md` 或接口实际行为与 IF 有偏差时，在此登记，前端据此同步 API 封装与类型。

- `[2026-09-09] GET /api/v1/role/{id}` → `permissionIds` 明确为扁平 `Long` 数组、不含父级/分组 → 前端可直接用于勾选回显 → 已同步 IF §6.5。
- `[2026-09-09] 权限编码规范` → 统一 `资源路径:动作`（末段固定为动作），二级 `kb:read` / 三级 `model:vendor:write` 均合法 → 前端改用权限树 `resource` 字段分组，勿对 `code` 做 `startsWith` 前缀匹配 → 已同步 IF §6.7。
- `[2026-09-09] SSE 新增 `heartbeat` 事件` → 每 15s、`data={"ts":<epochMillis>}`、不可关闭 → 前端可据此调整读超时 → 已同步 IF §10（Agent 调用 SSE 事件表）。
- `[2026-09-09] 网关响应头 `X-Trace-Id` 保证单值` → 所有命中路由（含错误响应）均回写**唯一** `X-Trace-Id`，前端可直接读取用于报障定位；**未匹配路由**（如路径拼写错误、未接入的服务前缀）由网关直接 404，响应**不带** `X-Trace-Id` 且为 Spring 默认错误体（无 `code` 字段）→ 前端对未知路径按 HTTP 404 兜底，勿依赖 `code`。
- `[2026-09-09] SSE 心跳升为通用约定（IF §2.6）` → 新增 **IF §2.6「SSE 流式通用约定」**作为心跳单一事实源：**所有 `stream=true` 接口均含 `heartbeat`（15s / 不可关闭 / `data={"ts":<epochMillis>}`）**；§7.5 / §8.3 / §10.3 / §12.4 / §13.6 统一改为引用 §2.6，其中 **§12.4 括号枚举已补上 `heartbeat`**（原枚举遗漏，最易误导）→ 前端 `sse.ts` 读超时保护按「任意事件即重置」统一实现，无需按端点差异化 → 已同步 IF §2.6/§7.5/§8.3/§10.3/§12.4/§13.6。
- `[2026-09-16] 新增删除空间端点 DELETE /api/v1/workspace/{id}` → workspace 模块交付时补充的删除能力（权限码 `ws:delete` 已在权限字典中）。语义：逻辑删除空间**及其全部成员关系**；**不允许删除当前所处空间**（`1003`，提示先切换）。前端「删除」按钮可直接对接 → 已同步 IF §5.3。
- `[2026-09-16] 添加成员路径以 IF 为准：POST /api/v1/member/invite` → IF §5.6 定义为 `/api/v1/member/invite`，而前端 `api/member.ts` 当前请求 `POST /api/v1/member`（旧 mock 口径）。后端按 IF 实现（`/invite`），**请前端把该调用改为 `/api/v1/member/invite`**（请求体 `{workspaceId, email, roleId}` 不变）。未注册邮箱 / 重复加入仍返回 `1001`（与 mock 一致）。
- `[2026-09-16] GET /auth/me 的 workspaceId / workspaceName 语义明确为「当前工作空间」（以 JWT ws_id 为准）` → **切换空间后 `ensureMe()` 会返回新空间**（此前实现固定返回成员关系中最早的空间，切换后不刷新，属实现与语义不符，已修正）；令牌无 `ws_id`（组织级管理员）时回退最早所属空间 → 已同步 IF §3.5。
- `[2026-09-16] 空间列表可见范围收敛` → `GET /api/v1/workspace/page` 对**组织级管理员及以上（持 `org:write`）**返回组织内全部空间；其他用户**仅返回自己所属空间**（按 `ie_member` 反查）。前端无需改动（管理员视角不变）；普通用户「空间切换」列表即为其可切换范围，与服务端切换校验同源 → 已同步 IF §5.4。
- `[2026-09-17] 切换空间的 JWT 权限口径修正（重要，影响前端按钮门控）` → `POST /api/v1/workspace/switch` 重签后的 `roles`/`perms` **按用户维度取全量，与登录（`/auth/login`）完全同口径**，**只有 `ws_id` 变化**。此前实现"按目标空间重新展开"会把组织级/平台级能力（`org:*`、`ws:create`、`ws:delete`）筛掉 → 表现为「切换后新建/删除按钮消失、菜单还在、`org:write` 可见范围缩水」。**修复后前端可这样断言**：切换前后 `perms` 集合必须完全相同（只允许 `ws_id` 变）→ 已同步 IF §5.5、FEATURES 2.5。
- `[2026-09-17] 新增「我在该空间的权限」接口 + 空间维度鉴权生效（前端按钮门控需改判据）` → ① 新增 `GET /api/v1/workspace/{id}/my-permissions`（IF §5.7），返回 `{workspaceId, roles, permissions}` = **你在该空间内的真实权限**（非成员 `403/2006`）。**前端应改用它做按钮门控**：token 里的 `perms` 是"跨空间并集"，用它渲染会出现「切到某空间后按钮还在、点下去 403」。② 部分接口已加**第二层校验**（同一 token，在不同空间判定不同）：`member/page`、`member/invite`、`member/{id}` 移除、`member/{id}/role`、`workspace/{id}` 更新 → 跨空间越权调用统一返回 **`403/2006`「您在当前工作空间没有该操作权限」**（注意：这不是 `1003`，`1003` 表示"业务规则不允许"）。③ `ws:create` / `ws:delete` 属**组织级**，不做空间判定（仍只看 token 权限码）。
- `[2026-09-17] 角色授予收紧（角色下拉需过滤 + 新增 1003/1004 提示场景）` → ① **空间成员管理**（`POST /member/invite`、`PUT /member/{id}/role`）**只能选择 `scope=WS/SELF` 的角色**（如 `ws_admin`/`app_developer`/`end_user` 及自定义空间级角色）；选 `org_admin`/`super_admin` 会被后端拒 `1003`「不允许授予该角色：超出工作空间管理范围」→ 前端下拉应直接隐藏（或禁用+提示），避免"能选但必失败"。② **建号** `POST /api/v1/user` 的 `roleId` 同样受限：非超管只能选 `WS/SELF`（越界同样 `1003`）；`roleId` 不存在 → `1001`「角色不存在」（此前无校验）。③ 跨租户资源一律按"不存在"返回 `1004`（多租户前端无需特判）。
- `[2026-09-17] 答复 BE-20260917-01：空间角色授予限制「未生效」= 运行实例未重启，非契约问题（前端按 IF §5.6 过滤正确，请保持）` → 前端实测 `member/invite` 传 `roleId=1/2` 得 200，原因是**所打实例启动早于修复**（实例 `:7102` 启动 14:28:42；`RoleGrantPolicy.java` 写入 14:35:19、`MemberServiceImpl` 接线 14:36:33、jar 构建 14:37:07、提交 `cf6580c` 14:43）。**member 侧无超管豁免**（`MemberServiceImpl.java:191-197` 只有 scope + 租户两条校验）→ **IF §5.6 无需修改**；超管豁免仅存在于 UMS 建号接口（IF §4.2 已写明"非超管只能选 WS/SELF"）。**实测复现**：含修复的新实例上同一请求 → **`403 / 1003`**「不允许授予该角色：超出工作空间管理范围」；`scripts/smoke-workspace-permission.ps1` 17 项断言 ALL PASS。**请前端在 workspace 服务重启后复测 `roleId=1/2 → 403/1003`、`roleId=3/4/5 → 200`，然后转 🟢**。
  **2026-09-17 后续核实（后端 16:2x 实测）**：开发者重启了服务，但**只重启了 UMS**（PID 10340→37232@16:17:30），**`workspace :7102` 仍是 14:28:42 的旧进程**（`gateway :7000` 同样未重启）→ 复测 `roleId=1` **仍 200**（因撞到"邮箱未注册"后修正为已注册用户，得 `code=0`）→ **不是代码问题，是 workspace 未重启**。IDEA 的输出目录即 `target/classes`（`MemberServiceImpl.class` 时间戳 14:37:07，已含修复）→ **重启 workspace（建议连 gateway）后再复测即可**。另：UMS 重启后登录已恢复（`code=0`，此前 16:14 的 9999 已消失）。
- `[2026-09-17] `POST /auth/refresh` 行为修正：刷新不再重置"当前空间"` → 此前 refresh 会把 `ws_id` 重算为**默认空间**（切空间后每 2h 被静默弹回默认空间）；现改为**沿用当前 `ws_id`**。**前端可感知的变化**：切换空间后长时间挂着再刷新，空间不再跳回；若前端此前对"刷新后空间会变"做过防御（重新调 switch 或重读 `/auth/me`），可移除。**三入口口径已统一**（登录 / 刷新 / 切换空间的 `roles`+`perms` 完全一致，仅 `ws_id` 语义不同）→ 详见 IF §3.0 口径表；断言脚本 `insight-engine/scripts/smoke-auth-claims.ps1`（全 ASCII、口令走参数）可在联调前自跑。
- `[2026-09-17] `DELETE /api/v1/workspace/{id}` 的权限顺序说明` → 该接口需 `ws:delete`，而 `ws:delete` 属**组织级**权限（`ws_admin` 的 27 条里不含）→ **无组织级权限的账号会先被 `2006` 拦截**，走不到「不允许删除当前空间 → `1003`」的业务校验。这是预期行为：前端对删除按钮的门控应按 `ws:delete`（组织级）显示，而非 `ws:write`。

---

## 3. 对前端 BE-ISSUES 的答复（后端必填）

> 每条答复格式：`BE-{id} → 结论 → 证据（curl + traceId + 日期）→ 已回填前端 BE-ISSUES.md ✅`
> 未答复的条目**不得从 BE-ISSUES 的未闭环区移走**。

### BE-20260908-01 · `ws:switch` → ✅ 已答复（不新增，沿用 `ws:read` 门控）
- **结论**：**不新增 `ws:switch`**。切换目标范围已由「成员关系」在服务端强约束（只能切到所属空间），独立权限码无额外安全增益、徒增授权维护成本。前端切换按钮继续用 `ws:read` 门控，无需改动。
- **已同步**：IF §6.7、DB.md 种子表说明。
- **证据**：权限字典 ws 组 = read/create/write/delete（init.sql §5，2026-09-09 代码核对）。

### BE-20260908-02 · workspace/member/audit 交付 → ✅ 已交付（workspace / member 就绪；audit 属 obs，未交付）
- **结论**：workspace 模块已实现并**实机冒烟通过**（2026-09-16），`/api/v1/org|workspace|member/**` 全部就绪，前端可按 FE-P3 步骤 0→5 复测并**从 mock 清单移除 `workspace,member`**。
- **证据**：见本文件 §1 workspace 行（冒烟 28/28 + 经网关复验）；代码位置 `insight-engine-modules/insight-engine-workspace`（11 端点 / 端口 7102）。
- **需前端同步两处**：① 添加成员改调 `POST /api/v1/member/invite`（§2）；② 新增删除空间 `DELETE /api/v1/workspace/{id}` 已可用。
- **未交付**：**audit（审计）属 obs 服务**，本轮未实现，前端审计 Tab 需继续 mock（待 obs 阶段）。
- **前置数据**：云端库 `ie_role_permission` 增量 seed 已执行（2026-09-16，见 BE-20260908-03），`ws_admin` 成员管理权限已生效。

### BE-20260908-03 · 角色 seed 缺授权 → ✅ 已修复（云端已执行增量 seed）
- **结论**：已补齐 `org_admin`(46) / `ws_admin`(27) / `end_user`(7) 授权（init.sql §6）。`ws_admin` 含成员管理（`member:read/create/update/delete`），成员 Tab 不再走 2006 降级。
- **证据**：init.sql 三条授权（带 `ON CONFLICT DO NOTHING` 可增量重跑）；DB.md 种子表 `ie_role_permission=143`（2026-09-09）。
- **落地状态（2026-09-16 更新）**：实测云端库此前**只有 role 1/4 授权**（成员 Tab 与空间级权限 403 的直接成因）；已执行这三条增量 INSERT，现 `ie_role_permission` = 1:48 / 2:46 / 3:27 / 4:15 / 5:7 = **143**（与 DB.md 一致）。

### BE-20260909-04 · SSE 心跳 → ✅ 已答复（契约已定，实现随 conv/agent）
- **结论**：事件名 `heartbeat`，周期 **15s**，`data = {"ts":<epochMillis>}`，**不可关闭**；已写入 IF §10 SSE 事件表。实现随 conv/agent 流式模块落地。
- **证据**：IF §10（2026-09-09）。

### BE-20260909-05 · `GET /api/v1/role/{id}` 响应示例 → ✅ 已答复
- **结论**：`permissionIds` 为**扁平 `Long` 数组**（元素 = 权限数字 ID），与 `PUT /role/{id}/permissions` 请求体同构、可直接回显勾选态；**不含父级/分组**；列表接口（§6.1）不返回该字段。示例已写入 IF §6.5。
- **证据**：`RoleVO.permissionIds: List<Long>` + `RoleServiceImpl.detail`（2026-09-09）。

### BE-20260909-06 · 权限编码二级/三级混用 → ✅ 已答复（统一规则，零破坏）
- **结论**：统一 **`资源路径:动作`，最后一段固定为动作**；二级 `kb:read`、三级 `model:vendor:write` 均合法。前端按最后一个 `:` 切分，或直接用权限树 `resource` 字段分组；禁止对 `code` 做 `startsWith` 前缀匹配。已写入 IF §6.7。现有编码不变。
- **证据**：IF §6.7（2026-09-09）。

### BE-20260909-07 · SSE `heartbeat` 契约不完整 + §12.4 枚举误导 → ✅ 已答复/已修复
- **结论**：**采纳**。新增 **IF §2.6「SSE 流式通用约定」**作为心跳单一事实源：**所有 `stream=true` 接口均含 `heartbeat`（15s / 不可关闭 / `data={"ts":<epochMillis>}`）**，端点事件表只列业务事件；同步修正 **§12.4 括号枚举（补 `heartbeat`）**，§7.5 / §8.3 / §10.3 / §13.6 统一引用 §2.6。前端读超时保护按「任意事件即重置」统一实现。
- **已同步**：IF §2.6 / §7.5 / §8.3 / §10.3 / §12.4 / §13.6。
- **证据**：IF（2026-09-09 文档核对）。⚠️ 相关流式端点尚未实现，本次仅收敛契约，**不构成可联调**（§1 仍 ⚪）。

### BE-20260916-01 · 切换空间后 JWT 只按目标空间成员角色展开，丢失组织级角色 → ✅ 已修复（2026-09-17）
- **结论**：**采纳，属我方实现口径错误**，已修复为「切换空间只换 `ws_id`，`roles`/`perms` 按用户维度全量、与登录同口径」。
- **根因**（诚实记录）：`ie_member.workspace_id` **可空**（= 组织级成员），且组织级能力（`org:*`、`ws:create`、`ws:delete`）**不属于任何空间**；
  按空间过滤必然把它们筛掉。更深层是**未与 UMS 既有实现对齐**——UMS 登录用的两个查询本就不带空间过滤，
  而我在 workspace 另写了一套（`...ByUserAndWorkspace`），两套口径并存。
- **证据**（2026-09-17，workspace 独立实例 `:17102` 隔离验证，避免打断 IDEA 中运行的服务）：
  登录 `ws_id=1 roles=super_admin perms=48` → 切换 `ws_id=7 roles=super_admin,ws_admin perms=48`；
  **断言 `perms` 切换前后完全一致 = True**（`ws:delete`/`org:write`/`ws:create` 均在）；
  `DELETE /api/v1/workspace/7`（当前空间）→ **403 / `1003`「不允许删除当前所处的工作空间」**（修复前是 `2006`）。
- **前端需知**：切换后按钮不再消失；可加断言「切换前后 `perms` 相同」；`ws:delete` 属组织级，`ws_admin` 无此权限（走 `2006` 属预期）。
- **遗留（另立）**：「同一用户在不同空间权限不同」属**空间维度授权**，需服务端按当前 `ws_id` 二次判定 + 前端取「当前空间权限」做门控（TD §7.5 / PROGRESS §6.3 待办），本次不涉及。

> 以上答复已双写前端 `BE-ISSUES.md`（状态置 ✅，待前端实测后转 🟢）。

---

## 4. 开场白（全场景唯一一句，复制即用）

```
读 <本仓库根目录>/AGENTS.md，按铁律 0 开工，然后执行：<你的任务>
```

- 后端窗口 → `<本仓库根目录>` = `d:/CodexProject/insight-engine`
- 前端窗口 → `<本仓库根目录>` = `D:/JavaProject/insight-engine-web`
- **不要在开场白里手写「读 FE-SYNC / BE-ISSUES / PROGRESS」**：铁律 0 已强制 AI 读这几份，重复写既冗余又容易漏（2026-09-09 修正：旧版两条模板把文件清单写死，导致"后端先开始"等场景说不清）。
- 正常情况下 `AGENTS.md` 会被工具自动加载，此句仅作兜底。

全场景任务写法见 §5.3。

---

## 5. 日常操作手册（人只需做 3 个动作）

### 5.1 四个文件的分工（先认清）

| 文件 | 在哪 | 是什么 | 谁改 |
| --- | --- | --- | --- |
| `docs/FE-SYNC.md` | **后端仓库** | 后端给前端的「能不能联调」公告板 + 本协议 | 后端 |
| `docs/BE-ISSUES.md` | **前端仓库** | 前端给后端的「问题工单」 | 前端提，后端答复 |
| `AGENTS.md` | 前端仓库根 | 前端 AI 自动加载的规矩 | 极少改 |
| `AGENTS.md` | 后端仓库根 | 后端 AI 自动加载的规矩 | 极少改 |

**一句话**：进度各写各家（后端 `PROGRESS.md` / 前端 `FE-PROGRESS.md`）；**只有跨端的事才走 FE-SYNC 和 BE-ISSUES**。

### 5.2 你每天只做 3 个动作

1. **开对话** → 粘贴一行开场白（见 §4；正常会自动加载，可省）
2. **说任务** → 一句人话，如「开发 FE-P3 工作空间管理页」
3. **收尾** → 说「按 AGENTS.md 收尾铁律收尾」，AI 自动更新文档 + commit

其余（读对方文档、写工单、回填答复、同步状态）全部由 AI 按 AGENTS.md 完成。

### 5.3 全场景起手式（先后顺序无所谓，两端都是异步信箱）

**统一格式**：`读 <本仓库根目录>/AGENTS.md，按铁律 0 开工，然后执行：<下表里的任务>`

**后端窗口（5 种）**

| 你要干的事 | `<任务>` 写什么 |
| --- | --- |
| 开发新模块 | `开发 workspace 模块` |
| 处理前端工单 | `处理 BE-ISSUES.md 里的 🟡 条目` |
| 想先看前端要什么 | `汇总 BE-ISSUES.md 里与 <模块> 相关的诉求` |
| 模块做完，通知前端 | `<模块> 已就绪，更新 FE-SYNC §1 并收尾` |
| 只收尾 | `按铁律 4 收尾` |

**前端窗口（5 种）**

| 你要干的事 | `<任务>` 写什么 |
| --- | --- |
| 开发页面 | `开发 FE-P3 工作空间管理页` |
| 接口报错 | `<接口> 报错，按铁律 2 定性，是后端问题写 BE-ISSUES.md` |
| 查某模块能否联调 | `查 FE-SYNC §1，告诉我 <模块> 能否联调` |
| 后端已答复，去验证 | `读 FE-SYNC §3 + BE-ISSUES 答复，验证并闭环` |
| 只收尾 | `按铁律 4 收尾` |

**「后端先开始」怎么办（重点）**

后端**不依赖前端启动**，三种情况都正常：

1. **纯后端开发**（例：做 workspace）→ 直接开，`BE-ISSUES.md` 为空就是正常开工，不必等前端提工单。
2. **想知道前端要什么** → 铁律 0 已强制读 `BE-ISSUES.md`，有工单会自动带出来；也可显式说「汇总与 workspace 相关的诉求」。
3. **做完要通知前端** → 铁律 4 强制同步 `FE-SYNC §1`，前端下次开工自动看到，不需要你私聊前端。

反之「前端先开始」同理：`FE-SYNC §1` 里该模块是 `⚪` 就先 mock，不阻塞。

**两端唯一需要人推进的动作**：说一句话开对话。其余全自动。

### 5.4 状态谁改、什么时候改

| 状态 | 谁改 | 时机 |
| --- | --- | --- |
| 🟡 待后端答复 | 前端 | 写完工单 |
| 🔵 后端处理中 | 后端 | 已受理、未出结论 |
| ✅ 已答复 / 已修复 | 后端 | 修完或答复完，附证据 |
| 🟢 已验证闭环 | 前端 | 实测通过后，条目移入「已闭环」区 |

### 5.5 AI 偷懒时的兜底话术

- 「你没读 `FE-SYNC.md §1`，先读再回答」
- 「这条答复没有证据（curl + traceId + 日期），补上」
- 「按 AGENTS.md 铁律 4 收尾：更新文档、写 BE-ISSUES、commit」
- 「后端没实现的功能不许说已联调，只能写 mock」

### 5.6 文档那么多会不会失控？（维护边界）

现状：后端 `docs/` 10 个 + 前端 `docs/` 3 个 + 2 个 `AGENTS.md`。**但人需要手写的只有 §4 那 1 句提示词。**

| 层级 | 文件 | 谁写 | 你多久动一次 |
| --- | --- | --- | --- |
| 自动加载 | 2 个 `AGENTS.md` | AI | 从不 |
| 每轮必读（3 份） | 本仓库进度源（`PROGRESS.md` / `FE-PROGRESS.md`）+ 对端同步文件（`FE-SYNC.md` / `BE-ISSUES.md`）+ 任务相关 `IF.md` 章节 | AI | 从不 |
| 按需读 | `DEVGUIDE` / `FEGUIDE` / `TD` / `ARCHITECTURE` / `DB` / `PRD` / `FEATURES` / `LEARNING` | AI | 从不 |
| 人写 | **0 份** | — | — |

本次机制**只新增 2 个文件**（`FE-SYNC.md`、`BE-ISSUES.md`），且全部由 AI 维护；没有新增任何「需要人定期更新」的文档。

若仍嫌多，可选精简（**不推荐现在做**）：
- 方案 B：`BE-ISSUES.md` 并入 `FE-PROGRESS.md` 一节、`FE-SYNC.md` 并入 `PROGRESS.md` 一节 → 协作文件 4 → 2。
- 代价：前端要读后端 `PROGRESS.md` 全文；状态机与进度混在一份文件里，AI 更容易改错位置。
- 建议：先按现方案跑 1~2 周，若发现 AI 更新不到位再合并，而不是先合并再暴露问题。
