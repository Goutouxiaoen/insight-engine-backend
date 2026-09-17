# 项目进度追踪（PROGRESS）

> **本文档是**：项目当前状态看板 —— 做到哪了、每个模块什么进度、卡在哪、下一步做什么。
> **何时看**：每次对话**开始必读**、**结束必更新**（日常最高频打开的一份）。
> **不负责**：实现细节（→ `FEATURES.md`）、接口契约（→ `IF.md`）。
>
> 本文件是「开发者」与「AI」之间的**共享进度真相源**，是跨对话记忆的唯一可靠载体。
> **铁律：每次对话结束前必须更新本文件；每次新对话开始必须先读本文件。**
>
> **找文件看这里**：AI 入口 `AGENTS.md` 在**仓库根目录**（不在 `docs/`）；`docs/` 下真相源清单见 `DEVGUIDE.md` 附录 A；跨端同步协议见 `docs/FE-SYNC.md`；前端工单在 `D:\JavaProject\insight-engine-web\docs\BE-ISSUES.md`。
>
> **文档口径（2026-09-02 起执行）**：
> - 「§五 当前阻塞 / 待解决问题」= **必须修且尚未解决**的项，阻塞对应交付收口；已办结项不在此滞留。
> - 「§六 后续待办」= **建议修（🟡）/ 可选优化（🟢）**，属后续要完成的任务池，按处理阶段归类。
> - 文档只写「项目任务视角」的进展与待办，不再粘贴 review 输出格式标题。

---

## 一、总体状态

| 项     | 值                                  |
| ----- | ---------------------------------- |
| 当前阶段  | **阶段 5：workspace 工作空间 —— 模块已实现并实机冒烟 28/28（2026-09-16，未合并）**；阶段 4 已收口：**PR #5（`f84ffd2`）已合入 master**（路由按 §8.3 收窄 + TraceGlobalFilter + charset + JWT 常量下沉，复跑冒烟 9/9）；**云上 Nacos 2.3.2 已部署（healthy，8848/9848 1:1 端口，自报 39.106.110.214:8848）+ UMS/gateway Nacos 接入代码完成、全量 BUILD SUCCESS（路由已改 `lb://insight-engine-ums`）**，**P2-2/P2-3 注册与 lb:// 转发验证通过（冒烟 8/8，2026-09-09 安全组放行后）**；遗留 🔴 云凭据明文已进 master 历史（§五，闸门已失守，待口令轮换止血） |
| 当前里程碑 | M5：workspace 工作空间                  |
| 当前任务  | **workspace 模块（阶段 5，兑现 BE-20260908-02）——2026-09-16 实现完成并实机冒烟通过**：11 个端点（组织 2 + 空间 5 + 成员 4）全部落地，`mvn -DskipTests package` 全量 **BUILD SUCCESS**；**实机冒烟 28/28**（UMS 7101 + workspace 7102，云 PG/Redis，经 SSH 隧道绕开安全组）：空间创建/更新/分页、成员自动挂 ws_admin/添加/改角色/移除、**切换空间换签（旧 token 立即 401-2001）**、`/auth/me` 返回新空间、删除保护 1003、删后成员接口 404、非法编码 1001；**另经网关 :7000 复验** workspace 三条前缀路由 200、未接入前缀 404、无 token 401-2001。同时：安全会话实现下沉 starter-redis（`CacheKeyConstants` 统一 Redis 键契约）、`/auth/me` 工作空间语义修正为「当前空间」、**云端库补齐角色授权增量 seed（`ie_role_permission`=143）**。**下一步：`feature/workspace` 推送 + PR 合入；Nacos `lb://` 实机复验需安全组放行 8848/9848 或改用 `docker-compose.cloud.yml`**（提交：`d446ca7`，53 files） |
| 整体完成度 | 约 46%（阶段 1-4 已完成并合入 master（gateway 冒烟 9/9 + Nacos `lb://` 8/8）；**阶段 5 workspace 模块实现完成并实机冒烟 28/28（2026-09-16）**，未合并；公共层沉淀 CacheKeyConstants + starter-redis 提供登录态/黑名单实现；`/auth/me` 当前空间语义修正；云端库角色授权 seed 补齐 143；⬜ 待办：workspace PR 合并 + `lb://` 复验、RabbitMQ/MinIO/Prom/Grafana 迁云、§五 a) 口令轮换、UMS 收尾项在 §6.1 待办池） |

---

## 二、模块进度看板

> 状态图例：✅ 完成 / 🔵 进行中 / ⚪ 未开始 / ⛔ 阻塞

| 模块                                        | 状态    | 完成度  | 关键产物                                           | 备注                                     |
| ----------------------------------------- | ----- | ---- | ---------------------------------------------- | -------------------------------------- |
| 产品/技术/接口文档                                | ✅ 完成  | 100% | PRD/TD/IF                                      | 已定稿                                    |
| 协作指导文档                                    | ✅ 完成  | 100% | DEVGUIDE.md                                    | 已定稿                                    |
| 环境准备（JDK/Maven/Docker/Node）               | ✅ 完成  | 100% | 本机 JDK21.0.10/Maven3.9.9/Node24.13.0 + 云服务器 Docker（39.106.110.214） | ✅ 云端 PG/Redis 已部署并经 UMS 全链路冒烟实机验证可用（2026-09-08）；✅ 云上 Nacos 2.3.2 容器已部署（2026-09-09，healthy）；2026-09-09 复核本机 JDK/Maven/Node 正常。**口径说明（2026-09-16 判定）**：本机 `docker` 命令不存在属**有意接受**——本机 Docker Desktop 无法启动（虚拟化未开），承载方案已定为云服务器，故本机不承载中间件、「中间件是否全部就位」计入「基础设施」行不在此重复计算 → 本行判为完成 |
| Git 仓库初始化                                 | ✅ 完成  | 100% | .gitignore + GitHub 工作流（PR/分支）                 | master 主干 + GitHub Flow；PR #1/#2/#3 均已合入 master；远程现含 master + feature/ums-security-fix + feature/gateway 分支 |
| 工程骨架（父POM/BOM/common/api/starter/modules） | ✅ 完成  | 100% | 父POM/BOM/common/api/8个starter/12个模块占位          | `mvn clean install -DskipTests` 全量编译通过 |
| 基础设施（docker-compose/init.sql）             | 🔵 进行中 | 92%  | docker-compose.yml / init.sql / prometheus.yml | compose/init.sql 定义完成；✅ 云服务器（39.106.110.214）PG/Redis 已按 compose 逐项对齐重建完成（卷 + appendonly + unless-stopped），并经 UMS 冒烟实机验证（2026-09-08 8/8 通过）；✅ Nacos 容器迁云完成（2026-09-09：1:1 端口 8848/9848 + 公网 IP 自报 + healthy）；2026-09-09 补齐 init.sql 角色 seed 授权（org_admin/ws_admin/end_user，带 `ON CONFLICT` 可增量重跑）；✅ **2026-09-16 云端库增量 seed 已执行**（role 2/3/5 授权补齐，`ie_role_permission`=143，与 DB.md 一致）；⬜ 待办：**RabbitMQ / MinIO / Prom / Grafana 容器迁云**（compose 已写好，云上仅 postgres/redis/nacos 3 容器在跑）+ **compose 与云上实跑漂移（P1-2：compose 的 nacos 服务缺 `NACOS_SERVER_IP`）** + §五 凭据收敛（口令轮换） |
| UMS 认证服务                                  | ✅ 完成  | 100% | 认证5+用户5+角色权限6 接口 / JWT / RBAC / 黑名单 / 登录锁定 / Knife4j | 功能实机验证通过；UMS-1（双身份源收敛）+ UMS-2（refresh 轮换撤销）回归验证通过，PR #2 已合入 master，阶段正式收口；遗留 UMS 收尾优化项转 §6.1 待办池（不阻塞） |
| gateway 网关                                | 🔵 进行中 | 95%  | 骨架/路由/CORS + AuthGlobalFilter（JWT+防伪造头+sk-分流）+ TraceGlobalFilter + 常量下沉 | 全链路冒烟 8/8（2026-09-08，云 PG/Redis，UMS 7101 + gateway 7000）；3 修复 + 文档已提交 feature/gateway（fe912aa）；2026-09-09 路由按 §8.3 收窄（`/auth/**`+`/api/v1/user|role|permission/**`）+ TraceGlobalFilter(-200) + 错误响应 charset + JWT Claim 常量下沉 common，**复跑冒烟 9/9 通过**（含 `X-Trace-Id` 单值修复、2007 过期 token、`/doc.html` 白名单、`sk-` 分流）；**PR #5（`f84ffd2`）已合入 master**；✅ Nacos 接入闭环（2026-09-09：路由改 `lb://insight-engine-ums` + `fail-fast:false`；实例 UP、`lb://` 转发冒烟 8/8 通过）；**2026-09-16 新增 workspace 路由 + `lb://` 双服务复验通过**（UMS/workspace 注册 healthy、经网关 login/org/workspace-page/user-page 200、无路由 404）+ `register-enabled:false`（P2-1）。⬜ 剩余均为生产向项：API Key（`sk-`）通道、Sentinel 限流、CORS 白名单、未匹配路由 404 统一体、Nginx 入口 |
| workspace 工作空间                            | 🔵 进行中 | 98%  | 组织/空间/成员 12 端点 + 切换空间换签 + 网关路由接入 + 空间维度鉴权 | **2026-09-17 工程收口完成**（代码/冒烟/文档/审查闭环），⬜ 仅剩：**PR 合并**（`feature/workspace` 领先 master **20 commits**，gh CLI 未安装 → 待开发者点 PR）+ 前端把空间内门控改用 `my-permissions`。历史：2026-09-16 实现完成、**实机冒烟 28/28**（含经网关复验）；前端 BE-20260908-02 已切真联调（FE-SYNC §1 ✅）；**`lb://` 服务发现复验通过**；**2026-09-17 空间维度授权（第二层鉴权）**：`@WorkspacePermission` + `my-permissions`，冒烟 **17 项断言 ALL PASS**；同日 code review 收口 4 项 🟡 + 1 项 🟢 + 2 处同源授予缺口（角色授予越权/租户归属/踢权语义/SpEL 缓存）。**阶段 5 内的「数据权限拦截器（DataScope）」为显式延后项**（口径见 §七）。❌ 不含 audit（属 obs 服务，仍在 mock） |
| model 模型网关                                | ⚪ 未开始 | 0%   |                                                |                                        |
| kb 知识库                                    | ⚪ 未开始 | 0%   |                                                |                                        |
| tool 工具市场                                 | ⚪ 未开始 | 0%   |                                                |                                        |
| agent Agent编排                             | ⚪ 未开始 | 0%   |                                                |                                        |
| conv 对话服务                                 | ⚪ 未开始 | 0%   |                                                |                                        |
| billing 计费                                | ⚪ 未开始 | 0%   |                                                |                                        |
| obs 监控审计                                  | ⚪ 未开始 | 0%   |                                                |                                        |
| notify 通知                                 | ⚪ 未开始 | 0%   |                                                |                                        |
| 前端 admin（控制台）                         | 🔵 进行中 | 45%  | FEGUIDE v1.1 + FE-PROGRESS（P0/P1 ✅ 100%、P2 ~90%） | 独立仓库 D:\JavaProject\insight-engine-web\（接口契约仍以本仓库 IF.md 为唯一事实源）；P0 脚手架 + P1 认证闭环已 commit `4908c45` 并全链路实测；P2 组织与人员前端收口（workspace/member/audit 因后端未实现走 mock）；**跨端同步见 `docs/FE-SYNC.md`，前端诉求见前端 `docs/BE-ISSUES.md`** |
| 前端 chat                                   | ⚪ 未开始 | 0%   |                                                |                                        |
| docker-compose 全量编排                       | ⚪ 未开始 | 0%   |                                                |                                        |

---

## 三、关键技术决策记录（增量追加）

- [2026-08-25] 选定产品方向：企业级 AI Agent 编排与知识中枢平台（对标 Dify/Coze/FastGPT）
- [2026-08-25] 主库选 PostgreSQL（含 PGVector）而非 MySQL，见 TD ADR-2
- [2026-08-25] AI 框架 Spring AI 为主 + LangChain4j 为辅，见 TD ADR-3
- [2026-08-25] 工作流自研状态机，不引 Flowable，见 TD ADR-4
- [2026-08-25] ✅ 已决策：RabbitMQ 锁定 `3.13-management`，不复用本机 4.2；宿主端口映射 5673/15673（容器内仍 5672/15672），微服务走内部网络，见 TD ADR-11/ADR-12、TD §18.2
- [2026-08-25] ✅ 已决策：所有中间件宿主端口统一加偏移（PG 5433/Redis 6380/Nacos 8850/MinIO 9010/Prom 9091/Grafana 3001），避开本机占用，见 TD §18.2.3（⚠️ **2026-09-09 修正：Nacos 例外，改 1:1 8848/9848，见本节下方 P2-1 决策**）
- [2026-08-25] ✅ 已决策：JDK 复用本机 21，编译用 `--release 17` 产出 17 字节码，满足 MVP 的 Java17 目标，免装 JDK17
- [2026-08-25] ✅ 已决策：工作区根目录 `d:/CodexProject/` 作为「多工程容器」，智擎项目整体收进子目录 `insight-engine/`（项目完全自包含，docs 也移入 `insight-engine/docs/`），便于 IDEA 单独打开工程进行 review；未来 `CodexProject/` 下可并列多个工程
- [2026-08-25] ✅ 已决策：groupId=`com.insightengine`，版本 `1.0.0-SNAPSHOT`；编译目标 Java 17（`maven.compiler.release=17`）
- [2026-08-25] ✅ 备注：阶段 1 骨架未涉及入参校验（无 Controller），但 `starter-web` 的 `GlobalExceptionHandler` 已预留 `MethodArgumentNotValidException` / `ConstraintViolationException` 两类校验异常处理，**完整入参校验（@Valid/JSR-303 + 业务规则校验）在阶段 3 UMS 认证服务中落地**（与 `starter-security` 配合）
- [2026-08-26] ✅ 已决策：分支策略采用 **GitHub Flow**（master 稳定主干 + `feature/xxx` 功能分支），不建 develop/release/hotfix 多分支（Git Flow 对单人 MVP 过重），不按人名建分支（分支应表达"做什么"而非"谁在做"）；阶段 2 已从 master 切出 `feature/infra-docker-compose`
- [2026-08-26] ✅ 已核实：本项目规划的全部宿主映射端口（PG 5433 / Redis 6380 / RabbitMQ 5673+15673 / Nacos 8850+9850 / MinIO 9010+9011 / Prom 9091 / Grafana 3001）本机**均空闲无冲突**，无需调整 TD §18.2.3 端口表（⚠️ **2026-09-09 修正：Nacos 除外——2.x 自报地址，宿主须 1:1 8848/9848**）
- [2026-08-26] ✅ 已决策：镜像拉取走 DaoCloud 加速器（`docker.m.daocloud.io`），以 `docker pull <加速器前缀>/<镜像>` + `docker tag` 回标准名的方式绕过 Docker Hub 直连失败；`docker-compose.yml` 保持标准镜像名（可移植），本机 daemon 不改配置
- [2026-08-26] ✅ 已决策：本机已有镜像 rabbitmq `4.2-management-alpine`、nacos `v3.1.1`、minio `latest` **均不复用**——版本与 TD 锁定（RabbitMQ 3.13 / Nacos 2.3.2 / MinIO RELEASE.2024）不符，复用会引入不可控版本差异，统一按 TD 拉取锁定版本
- [2026-08-26] ✅ 已产出：`docs/DB.md` 数据库设计文档（35 表 / 356 字段全注释 / 69 索引 / ER 关系 / 种子数据，与 init.sql 一一对应）；`docs/LEARNING.md` 沉淀「PG 自增主键与序列」笔记（MySQL→PG 对照讲解，含原理/类比/面试点/踩坑）
- [2026-08-26] ✅ 已决策：阶段 3 从 master 切出 `feature/ums-auth` 分支，实现完整 UMS（auth 5 + user 5 + role/permission 6 接口）
- [2026-08-26] ✅ 已决策：三个 starter 骨架（mybatis/redis/security）在 UMS 阶段一并实现——UMS 要连 PG 查表、要存登录态/黑名单、要签发校验 JWT，三个骨架缺一不可，属于 TD 既定分层而非越界
- [2026-08-26] ✅ 已决策：starter-security 通过「可选 TokenBlacklistService 接口 + ObjectProvider 注入」支持登出黑名单（TD ADR-10），starter 本身不依赖 Redis，未提供实现的服务退化为纯无状态 JWT 校验
- [2026-08-26] ✅ 已决策：JWT 载荷在 TD §7.2 基础上扩展 `perms` Claim（登录时由角色展开的权限编码列表），这是 @PreAuthorize 方法级权限能无状态工作的前提
- [2026-08-26] ✅ 已决策：注册用户默认挂 org=1/ws=1、角色 end_user（MVP 单租户开放注册）；/auth/me 的 workspaceName 由 UMS 直查 ie_workspace（同库只读临时方案），待 workspace 服务落地改走 Feign
- [2026-08-26] ✅ 已决策：登录失败锁定（5 次/30 分钟）用 Redis 计数实现（PRD §12.1.5），因锁定是临时态无需落库
- [2026-08-26] ✅ 已决策：UMS 接入 Knife4j 4.5.0（TD §2.1 选型，BOM 已锁版本），文档入口 http://localhost:7101/doc.html；Controller 统一加 @Tag/@Operation 注解分组
- [2026-08-26] ✅ 已产出：`docs/FEATURES.md` 功能模块实现清单（定位：每完成一个模块沉淀「实现哪些功能 + 如何实现」，后续模块持续追加）
- [2026-09-02] ✅ 已决策并实施（登录态踢人，红级修复①）：采用方案 A「单会话 + 单 key 摘要」——starter-security 新增可选 `TokenSessionService`，`JwtAuthFilter` 在签名校验后校验 `ie:auth:token:{userId}` 存在且摘要匹配，改密/禁用删缓存即 401；UMS 提供 `RedisTokenSessionService` 实现，`cacheToken` 改存 SHA-256 摘要（不落明文）。TD §6.1 本就是单 key 设计，多设备并存（jti+ver）留待需要时演进（见 §六 6.6）
- [2026-09-02] ✅ 已决策并实施（JWT 密钥加固，红级修复②）：`SecurityProperties.jwtSecret` 删除代码内默认值；`SecurityAutoConfiguration.jwtUtil` Bean 初始化 fail-fast（为空/不足 32 字节拒绝启动；prod profile 下含 `change-me` 拒绝启动）；`application.yml` 密钥改 `${INSIGHT_SECURITY_JWT_SECRET:本地开发默认值}` 环境变量注入。**部署纪律：非本地环境必须注入独立随机密钥并启用 prod profile**
- [2026-09-02] ✅ 已决策并实施（UMS 收尾 UMS-1 双身份源，方案 A）：`UserContextFilter` 改为条件装配，默认**关闭**（`insight.web.trust-gateway-headers=true` 才注册）；身份只信 `JwtAuthFilter` 解析的 JWT；`UserContext` 的 finally 清理职责移交 `JwtAuthFilter`（原依赖 `UserContextFilter` 兜底）。与 TD ADR-5（网关明文头）的最终裁决保留到 gateway 阶段（见 §六 6.4），届时服务走网关方案则把开关打开即可
- [2026-09-02] ✅ 已决策并实施（UMS 收尾 UMS-2 refresh 无撤销/无轮换）：refresh token 增加 `jti`；UMS 新增 refresh 会话缓存 `ie:auth:refresh:{userId}`（存 jti 摘要，TTL=7d）；`refresh()` 校验 jti 匹配后**一次性轮换**（旧 jti 作废、签发新对），旧 jti 重放视为泄露 → 吊销该用户全部会话；`logout`/改密/禁用删除 refresh 会话 key（refresh 不再无限续期）。**注意：不能用 access 登录态 key 存在性做 refresh 兜底（其 TTL=2h 会误伤超 2h 未活动的正常刷新）**
- [2026-09-03] ✅ 已办结（UMS 阶段正式收口）：UMS-1/UMS-2 安全收尾在分支 `feature/ums-security-fix` 实机回归验证通过（UMS-1：关闭 `UserContextFilter` 明文头后登录/me/角色/权限全链路正常；UMS-2：登录→刷新轮换→旧 refresh 重放被拒→登出/改密/禁用后 refresh 均失效），PR #2 已合入 master 且本地与远程同步。UMS 模块看板置 ✅ 100%，遗留收尾优化项（phone 唯一索引/roleId 校验/授权去重等）留在 §6.1 待办池随后续阶段择机处理
- [2026-09-03] ✅ 已决策：gateway 网关对前端提供的总入口仍走 **ADR-5 网关校验 JWT + 下发明文头** 路径（TD §8.3 既定），但**不强制业务服务信任明文头**——通过开关 `insight.web.trust-gateway-headers` 保持「服务自校验 JWT」与「网关下发头」双轨共存：新服务默认自校验（安全优先），需要降本时开网关头信任。裁决依据见 §六 6.4 首条，联动 UMS-1 方案 A（见 §三 2026-09-02 记录）
- [2026-09-04] ✅ 已提交 gateway 网关全部代码至 `feature/gateway`（f45a226）：`pom.xml`（jjwt + spring-boot 可执行插件）、`GatewayApplication`、`application.yml`（端口 7000、UMS 路由直连 localhost:7101、globalcors、`insight.gateway.jwt-secret` 占位）、`GatewaySecurityProperties`+`GatewayAuthConfiguration`（fail-fast 密钥校验，与 UMS 同源同 env 注入）、`GatewayJwtParser`/`GatewayJwtPayload`（WebFlux 侧复刻 UMS 校验，区分过期/非法）、`AuthGlobalFilter`（白名单放行、防伪造头清除重建、JWT/`sk-` 分流、2001/2007 错误码对齐 IF 附录 A）；同时 DEVGUIDE 增 P17 约束「部署/容器命令必须与 docker-compose.yml 逐项对齐」；`mvn install` 编译通过
- [2026-09-06] ✅ 已决策（联调环境迁移云服务器）：本机 Docker Desktop 因**未检测到虚拟化**无法启动 → 中间件承载迁移至腾讯云轻量服务器（39.106.110.214 / Ubuntu 24.04 / 2C1G30G）；云上容器**以「与 docker-compose.yml 完全一致」为铁律**（容器名 `insight-postgres`/`insight-redis`、`restart unless-stopped`、命名卷 `pg_data`/`redis_data`、Redis `--appendonly yes`、端口 5433/6380）；网络暂用默认 bridge（PG/Redis 互不访问无影响），微服务进容器阶段再统一走 compose 自定义网络（TD §18.2），不在手工阶段造同名网络避免"同名异网"
- [2026-09-08] ✅ 已决策并实施（文档同步①前端基线对齐）：TD §2.2 前端技术栈与前端仓库 FEGUIDE §3.1 定案对齐（Vue 3.5 / Vite 7 / TS 5.9(strict) / Arco 2.x / Tailwind 4 / Pinia 3 / Router 4 / Axios 1 / fetch-event-source / @vueuse / markdown-it+DOMPurify / ECharts 5 / MSW 2 / pnpm 工程链），并在 TD 声明「FEGUIDE §3.1 为前端侧唯一事实源」；DEVGUIDE §8.1/§8.4/D1 目录图/P7/P13 模板同步独立仓库 `D:\JavaProject\insight-engine-web\` 口径（控制台+对话门户二分、契约驱动+Mock 先行）
- [2026-09-08] ✅ 已决策并实施（文档同步②真相源补全）：DEVGUIDE 附录 A 与 D1 目录图补 `DB.md`（数据库设计，与 init.sql 对应）与 `FEATURES.md`（功能模块实现清单）——两者 2026-08-26 即确立真相源地位但清单漏列，铁律 3 同步「5 份 → 7 份」；TD §6.1 缓存键表补 2 个已上线键：`ie:auth:lock:{account}`（30min 登录失败锁定）与 `ie:auth:refresh:{userId}`（7d refresh 会话 / jti 一次性轮换），后续服务照 TD §6 开发不再漏
- [2026-09-08] ✅ 已决策（路由收窄纪律，防路由抢占）：TD §8.3 补注——冒烟期 `/api/v1/**` 全量直连 UMS **仅限单服务阶段**，workspace 等后续服务接入必须先把通配收窄为服务专属前缀（与 IF.md 章节一一对应），收窄与新增路由同一次提交完成；同时 DEVGUIDE 附录 B 新增 **P18 场景约束**（网关路由/新服务接入：必读 TD §8.3、前缀无交集、同步核对 routes 谓词/AuthGlobalFilter 白名单/globalcors 三处）
- [2026-09-08] ✅ 已决策并实施（gateway 冒烟前置修复①UMS 配置切云）：`ums/application.yml` datasource/redis 指向云服务器（39.106.110.214:5433/6380），两处改动生效并实机冒烟通过；经 `feature/gateway` `d437202` 承载
- [2026-09-08] ✅ 已决策并实施（gateway 冒烟前置修复②权限拒绝语义）：starter-security 新增 `SecurityExceptionHandlerAdvice`（`@Order(HIGHEST_PRECEDENCE)` 的 `@RestControllerAdvice`）——`@PreAuthorize` 抛出的 `AccessDeniedException` 在 DispatcherServlet 层统一转 **403/code=2006**；此前 `RestAccessDeniedHandler` 注释声称覆盖该方法级场景但异常传不到（被 starter-web 的 `Exception` 兜底误报 500）
- [2026-09-08] ✅ 已决策并实施（gateway 冒烟前置修复③中文乱码根治）：根因 = PowerShell 5.1 等老客户端对不带 `charset` 的 `application/json` 按 ISO-8859-1 解码（服务端字节实测为合法 UTF-8「默认空间」）；修复 = starter-web `WebAutoConfiguration` 新增 WebMvcConfigurer，给 Jackson 转换器设默认 `UTF-8`（响应头带 `charset=UTF-8`），实测 PS 解码正常
- [2026-09-09] ✅ 已决策并实施（gateway 待办收口四件）：① 路由按 TD §8.3 收窄为 `/auth/**` + `/api/v1/user|role|permission/**`（含文档路径；其余 `/api/v1/xxx` 网关层 404，不误转 UMS）；② 新增 `TraceGlobalFilter`（order=-200，校验/生成 `X-Trace-Id`，正则 `[A-Za-z0-9-]{1,64}` 防日志注入 → 请求头透传 + 响应头回写；WebFlux 不用 MDC，改请求头+日志显式带 traceId）；③ `AuthGlobalFilter.writeError` 补 `charset=UTF-8`；④ 新增 `common.constant.JwtClaimConstants`，UMS `JwtUtil` 与 gateway `GatewayJwtParser` 共享引用消除双份字面量。common/starter-security/gateway 三模块 `mvn install` 编译通过（§六 6.4）
- [2026-09-09] ✅ 已决策并实施（init.sql 角色 seed 授权，答复 BE-20260908-03）：补 `org_admin`(46)/`ws_admin`(27)/`end_user`(7) 授权，`ws_admin` 含成员管理（`member:read/create/update/delete`）；三条 INSERT 带 `ON CONFLICT DO NOTHING` 可对已初始化库增量重跑；DB.md 同步（`ie_role_permission`=143）。**云端已初始化库需执行增量 seed 才生效（未重建库场景）**
- [2026-09-09] ✅ 已决策（权限编码规范 + `ws:switch`，答复 BE-20260909-06/01）：① 权限编码统一「`资源路径:动作`，最后一段固定为动作」，二级/三级并存合法，前端按最后一个 `:` 切分或用权限树 `resource` 分组、禁用 `startsWith`（写入 IF §6.7，零破坏）；② **不新增 `ws:switch`**——切换范围由成员关系服务端约束，独立权限码无安全增益，前端沿用 `ws:read` 门控
- [2026-09-09] ✅ 已答复（前端契约两项）：① `GET /api/v1/role/{id}` 的 `permissionIds` 为扁平 `Long` 数组、不含父级/分组、列表接口不返回（写入 IF §6.5，答复 BE-20260909-05）；② SSE 新增 `heartbeat` 事件（15s、`data={"ts":<epochMillis>}`、不可关闭，写入 IF §10，答复 BE-20260909-04）
- [2026-09-09] ✅ 已修复并实测（`X-Trace-Id` 响应头重复）：冒烟复跑抓到经网关转发到 UMS 的响应出现**两个** `X-Trace-Id`（网关 set 的值 + 上游 starter-web `TraceFilter` 回传值，被 `NettyRoutingFilter` 合并追加）→ `TraceGlobalFilter` 改为在 `exchange.getResponse().beforeCommit(...)` 回调内 `headers().set(...)`（提交前覆盖为单值）。实测转发 200 / 无 token 401 / 坏 token 401 / 过期 401 / `sk-` 401 / end_user 403 六类路径 `X-Trace-Id` 计数均为 1
- [2026-09-09] 🟡 新发现（未匹配路由不经过 GlobalFilter）：`/api/v1/nonexistent` 由网关路由层直接 404，**不进入 GlobalFilter 链**（GlobalFilter 仅在路由命中后执行）→ 该响应无 `X-Trace-Id` 且为 Spring 默认错误体（含 `requestId`，非 IF §2 统一 `Result`）。路由收窄语义正确（未误转 UMS），但统一错误格式/traceId 覆盖存在缺口，记入 §6.4 待办
- [2026-09-09] ✅ 已决策并实施（SSE 心跳升为通用约定，答复 BE-20260909-07）：新增 **IF §2.6「SSE 流式通用约定」**作为心跳**单一事实源**——所有 `stream=true` 接口均含 `heartbeat`（15s / 不可关闭 / `data={"ts":<epochMillis>}`），端点事件表只列业务事件、不再重复声明；修正 **§12.4 括号枚举遗漏（补 `heartbeat`）**，§7.5 / §8.3 / §10.3 / §13.6 统一引用 §2.6。前端 `sse.ts` 读超时保护按「任意事件即重置」统一实现，消除按端点漂移。**上述流式端点尚未实现，本次仅收敛契约，不构成可联调**
- [2026-09-09] ✅ 已决策并实施（凭据纪律硬约束落地，补规范缺口）：复盘「弱口令明文进 master」根因——**不是 `.gitignore` 漏规则**（`.env` / `.env.*` / `application-local.yml` / `*.secret` 早已就位），而是**事前硬约束从未存在**：P2 第 6 条只约束「不改配置项」、P5 第 4 项属事后 Review、P17 反而要求「与含明文的 compose 逐项对齐」，`AGENTS.md` 至 2026-09-09 才建立且无凭据条款。→ 本次落地：① `AGENTS.md` 新增**铁律 5：凭据纪律**（禁止明文入库 / 占位符 + `.env` 注入 / 新增配置项三件套 / 提交前自检 / 历史泄露按已泄露处理）；② `DEVGUIDE.md` 附录 B 新增 **P19 场景约束：配置与凭据**；③ **修正 P17**——对齐范围明确为「结构（镜像 tag / 容器名 / 端口映射 / 命名卷 / 环境变量名 / 持久化参数）」，**凭据值一律占位符、禁止抄明文**，消除「文档说取 `.env`、compose 写明文」的自相矛盾；④ 附录 B 标题 / 索引 / 正文引用同步为 `P1~P19`；⑤ DEVGUIDE 铁律 3 真相源份数与表格、附录 A 对齐（7 份 → 11 份，补 `ARCHITECTURE.md` / `LEARNING.md`）。**代码侧配置占位化（§五 b）已于同日第三批完成，见下条**

- [2026-09-09] ✅ 已实施（凭据占位化落地，§五 b / §6.4 收口，第三批）：① `docker-compose.yml` 口令 6 处改 `${VAR:?必填提示}`（PG / Redis `--requirepass` + healthcheck `-a` / RabbitMQ / MinIO / Grafana）；② `ums/application.yml` 4 处改 `${INSIGHT_PG_*}` / `${INSIGHT_REDIS_*}`——**不给默认值**，未注入即 fail-fast（避免「忘配 → 静默用空口令连库」）；③ 新增 `.env.example`（入库模板：变量名 + 用途 + 两套注入路径指引）+ `.env`（真实值，已被 gitignore）；④ 新增 `application-local.example.yml`（入库模板）+ `application-local.yml`（真实值，已被 gitignore）——**关键事实：Spring Boot 不会自动读 `.env`**，应用侧只能走 profile 覆盖 / 环境变量，已写入 TD **§18.2.6**；⑤ `.gitignore` 补 `!.env.example`（原 `.env.*` 会把模板一起忽略，模板提不上去）；⑥ 同步 DEVGUIDE **P19**（两套注入路径 / gitignore 例外 / 自检命令只搜已跟踪文件的局限）、TD §18.2.4 应用侧注释、DB §1.1。自检：通用凭据模式 `git grep -niE "(password|passwd|secret|token|apikey)" -- ':!*.md' ':!*.example'` 与公网 IP 扫描均 **无输出**；`git check-ignore` 确认 `.env` / `application-local.yml` 被忽略、两个 `.example` 模板**不被忽略**。**未做：口令轮换（§五 a）——占位化只防「以后泄露」，不能回收已泄露历史**
- [2026-09-09] ✅ 已决策并实施（云上 Nacos 2.3.2 部署，P2-1）：云服务器（39.106.110.214）`docker run` 启动 `insight-nacos`（MODE=standalone / NACOS_AUTH_ENABLE=false / JVM 256m-128m / 命名卷 nacos_data、nacos_logs / readiness healthcheck / restart unless-stopped，与 docker-compose.yml nacos 服务逐项对齐）。**关键修正：宿主端口由 8850/9850 偏移改为 1:1 8848/9848**——Nacos 2.x 服务端向客户端「自报 ip:port」并据此推导 gRPC 端口（=主端口+1000），偏移映射会使客户端按自报 `ip:9848` 连接失败；`NACOS_SERVER_IP=39.106.110.214` + `JAVA_OPT_EXT=-Dnacos.inetutils.ip-address=39.106.110.214` 强制服务端自报公网 IP，`cluster/nodes` 已确认 `39.106.110.214:8848` UP（grpcReportEnabled）。docker-compose.yml / TD §18.2.3 / PRD / ARCHITECTURE / `.env.example` 同步 1:1 口径
- [2026-09-09] ✅ 已决策并实施（UMS/gateway 接入 Nacos，P2-2 代码侧）：ums/gateway 各引入 `insight-engine-starter-nacos`；starter-nacos 补 `spring-cloud-starter-loadbalancer`（nacos-discovery 2.x 不再传递引入，`lb://` 必需）；两服务 discovery/config 加 `server-addr: ${NACOS_ADDR:127.0.0.1:8848}` 与 **`fail-fast: false`**（默认 true 会在 Nacos 不可达时阻断启动——实测 UMS 报 `NacosException: Client not connected, current status:STARTING` 后整体关闭）；`spring.config.import-check.enabled: false`；gateway UMS 路由由 `http://localhost:7101` 改 **`lb://insight-engine-ums`**。全量 `mvn -DskipTests package` **BUILD SUCCESS**
- [2026-09-09] 🟡 阻塞（P2-2 注册实机验证，**已解除**）：本机 → 云 8848/9848（TCP）**不可达**（22/5433/6380 可达）；云主机侧 ufw/firewalld 均 inactive、iptables policy ACCEPT、docker-proxy 正常监听 0.0.0.0:8848/9848 → 判定为**阿里云安全组入方向未放行 8848/9848**（实例 i-2ze6ks9wv7i0fxjkq8c6）。**2026-09-09 控制台放行后解除**：本机 `-DNACOS_ADDR=39.106.110.214:8848` 启 UMS + gateway 复验通过（实例 UP + `lb://` 冒烟 8/8，见 §6.4 / §八）
- [2026-09-09] ✅ 已更正（云端供应商口径）：此前文档把 39.106.110.214 记为「腾讯云轻量服务器」，实机判定为**阿里云 ECS（cn-beijing，实例 i-2ze6kswv7i0fxjkq8c6）**——依据：主机名 `iZ2ze6ks9wv7i0fxjkq8c6Z`（iZ 前缀）、元数据服务 100.100.100.200。历史记录保留原表述，后续统一按「阿里云 ECS」口径

- [2026-09-16] ✅ 已决策并实施（workspace 模块落地，阶段 5，兑现 BE-20260908-02）：新建 `insight-engine-workspace`（端口 7102，模块此前仅 pom 空壳），实现 IF §5 全部 11 个端点（组织创建/详情、空间创建/更新/删除/分页/切换、成员分页/添加/移除/改角色），关键决策：
  - **空间列表可见范围收敛**：持 `org:write`（组织级管理员及以上）见组织内全部空间；其他用户仅见自己所属空间（按 `ie_member` 反查）。避免普通用户窥见他人空间，且与切换空间的服务端成员关系校验同源；
  - **创建者自动挂 `ws_admin` 成员**：成员关系既是可见范围也是切换前提，不落成员关系会导致「新建空间自己都看不到、切不过去」；
  - **删除空间级联逻辑删除其成员关系**（对齐前端确认文案），并**禁止删除当前所处空间**（`1003`），防止当前令牌 `ws_id` 指向已删空间；
  - **切换空间 = 校验成员关系 → 按目标空间重展开 roles/perms → 重签 access + 轮换 refresh → 覆盖登录态**；旧 access token 立即失效（单会话语义，与 UMS 登录/刷新一致）；
  - **操作保护**：不允许移除自己、不允许修改自己的空间角色（`1003`）；
  - **添加/改角色前校验 `roleId` 存在**，防孤儿成员关系（与 §6.1 UMS 同源教训）。
- [2026-09-16] ✅ 已决策并实施（安全会话实现下沉 starter-redis + Redis 键契约入 common）：workspace 需要「登录态校验 / 登出黑名单」才能保证切换空间与踢人在全平台一致生效，而原实现只在 UMS 内部（`RedisTokenSessionService` / `RedisTokenBlacklistService` / `TokenDigestUtil`）。→ ① 新增 `common.constant.CacheKeyConstants` 作为 `ie:auth:*` 键的**跨服务唯一契约**（与 `JwtClaimConstants` 同模式，UMS `AuthConstants` 改为引用它）；② 两个 Redis 实现 + `TokenDigestUtil` 下沉到 **starter-redis/starter-security** 并由 `RedisAutoConfiguration` 以 `@ConditionalOnMissingBean` 装配（starter-security 仍保持对 Redis 零依赖的设计不变）；③ 删除 UMS 内的重复实现。**目的：避免「A 服务写 key、B 服务查不到」的静默失效与多处字面量漂移**
- [2026-09-16] ✅ 已决策并实施（应用侧配置改「写死 + 口令自动加载」，**取代 profile / 环境变量注入**）：起因是 IDEA 直启连续踩坑（缺 `local` profile → 占位符绑定失败；Nacos 地址有默认值 → 静默连 `127.0.0.1`）。新口径：① **非敏感项写死** `application.yml`（PG/Redis/Nacos 地址、库名、账号、端口）并加备注；② **口令只放** `application-local.yml`（gitignore），由 `spring.config.import: optional:classpath:application-local.yml` **自动加载**（无需 profile / 环境变量 / IDE 配置）；③ `application.yml` **故意不写 `password` 键**（导入文件优先级更低，会被覆盖）；④ 生产用 `SPRING_DATASOURCE_PASSWORD` / `SPRING_DATA_REDIS_PASSWORD` 覆盖。**已废弃**：`.run/*.run.xml` 共享运行配置、gateway 的 local 文件（已删除）。**实测**：三服务零参数启动成功，经网关 login code=0 / workspace-page / org / member 全 200，验证后已停服
- [2026-09-16] ✅ 已决策并修正（`/auth/me` 当前工作空间语义）：`workspaceId`/`workspaceName` 改以 JWT `ws_id` 为准（无 `ws_id` 时回退「成员关系中最早的空间」）。原实现固定返回最早所属空间，导致 workspace 切换空间后 `ensureMe()` 仍显示旧空间，**实现与 IF §3.5/§5.5 语义不符**；已写入 IF §3.5 并登记 FE-SYNC §2
- [2026-09-16] ✅ 已核实并补执行（云端库角色授权增量 seed）：实测云端 `ie_role_permission` 只有 role 1(48)/4(15)，**role 2/3/5 的授权从未增量执行**（BE-20260908-03 的「落地提醒」未落实）——这正是「`ws_admin` 成员 Tab 403/2006」与「切换空间后新 token 全 403」的直接成因（非代码缺陷）。→ 执行 init.sql §6 三条幂等 INSERT 后为 1:48 / 2:46 / 3:27 / 4:15 / 5:7 = **143**（与 DB.md 一致）。**教训：seed 变更必须同时执行到已初始化库，否则代码正确也会表现为权限缺失**

- [2026-09-17] ✅ 已修复（切换空间权限口径错误，答复前端 BE-20260916-01）：**切换空间只改 `ws_id`（上下文），`roles`/`perms` 按「用户」维度取全量、与 UMS 登录同口径**。根因见 §四 同日条目；代码改动 `workspace/RoleMapper`（删掉 `...ByUserAndWorkspace` 两个按空间过滤的查询，改为与 UMS 同口径的 `selectRoleCodesByUserId` / `selectPermissionCodesByUserId`）+ `WorkspaceServiceImpl.switchWorkspace`；`IF §5.5/§5.3`、`FEATURES 2.5`、`FE-SYNC §2/§3` 同步。**证据（2026-09-17，workspace 独立实例 `:17102` 隔离验证，不打断 IDEA 中运行的实例）**：登录 `ws_id=1 perms=48` → 切换 `ws_id=7 perms=48`（`ws:delete`/`org:write`/`ws:create` 保留），**断言 perms 切换前后一致 = True**；`DELETE /workspace/7`（当前空间）→ **403/1003**（修复前 2006）。**遗留另立**：空间维度授权（同一用户不同空间权限不同）需服务端二次判定 + 前端「当前空间权限」接口，转 §6.3 待办

- [2026-09-17] ✅ 已决策并实施（**口径收口三件套**，治本 BE-20260916-01 这一"类"问题，呼应 AGENTS 铁律 6）：把"token 里放什么"从**靠人记**改为**结构性保证**——
  ① `common.constant.AuthQuerySql`：`roles` / `perms` 两条查询的**唯一字面量**（UMS `RoleMapper/PermissionMapper` 与 workspace `RoleMapper` 全部改为 `@Select(AuthQuerySql.XXX)`）→ SQL 只有一份，改一次全系统同步；
  ② `starter-security.token.AuthTokenIssuer` + `IssuedTokens`：**唯一令牌签发入口**，登录 / 刷新 / 切换空间三者都调它（access + refresh 一次成型，refresh **携带 `ws_id`**）；
  ③ `starter-redis.session.TokenSessionCache`：登录态与 refresh 会话的**唯一读写入口**（`save` / `matchesRefreshJti` / `clear`），取代原先 UMS 与 workspace 各自拼 Redis 键的写法；
  ④ **顺带修掉 refresh 的 `ws_id` 重置问题**（方案 A：刷新沿用当前空间，见 §6.1）；
  ⑤ 清理死配置：`AuthConstants` 中三个键常量随收口下沉后已无引用，删除并留注；
  ⑥ **断言**：新增 `scripts/smoke-auth-claims.ps1`（全 ASCII —— PS 5.1 读无 BOM 的 UTF-8 会乱码；口令走参数不落库），覆盖 5 类/10 项断言。
  **验证（隔离实例 :17101/:17102，不打断 IDEA 中运行的服务）**：`RESULT: ALL PASS` —— 登录/刷新/切换三入口 `roles`+`perms` 一致、**刷新前后 `ws_id` 不变**、**切换后刷新仍停留在目标空间**、切换后旧 token `401/2001`、删当前空间 `403/1003`、清理返回 200

- [2026-09-17] ✅ 已决策并实施（**空间维度授权轻量版落地**，TD §7.5 的 A 层）：回答"同一用户在不同空间权限不同"这个真实需求，**且不污染 token 口径**——
  **关键取舍**：① 该需求**不由 token 承载**（token 只承载用户级能力），而是在**服务端二次判定**；② 判定器做成 `starter-security` 的 SPI（`WorkspacePermissionChecker`），实现放业务侧（当前 workspace；将来跨库再改 Feign）——保持 starter 不依赖业务表；
  ③ 判定入口两种形态：目标空间在方法参数里 → 注解 `@WorkspacePermission(value, workspaceIdExpr)`（SpEL，需 `-parameters`，已在根 POM 开启）；目标空间需先查库才知道（如按 `memberId` 反查）→ 服务内**显式调用**判定器（检查点可见、易审计）；
  ④ 缓存 `ie:ws:user-perm:{wsId}:{userId}`（10min）：成员变更**主动失效**，角色授权变更在 UMS（跨服务）靠 TTL 兜底；
  ⑤ 前端门控改由 `GET /api/v1/workspace/{id}/my-permissions` 提供"当前空间权限"，**做到显示与后端判定同源**（否则会出现"按钮在、点了 403"）；
  ⑥ 权限分域纪律：`ws:create`/`ws:delete` 属**组织级**，不做空间判定；`member:*`/`ws:write`/`ws:read` 及后续 `kb:*` 等属**空间级**。
  **验证**：`scripts/smoke-workspace-permission.ps1` 全绿（同 token 在 W1 放行、在 W2 `403/2006`；`my-permissions` 在两空间分别返回 27/7 条）

- [2026-09-17] ✅ 已决策并实施（**空间维度授权的 4 项 🟡 + 1 项 🟢 code review 收口**，同时修掉 2 处同源缺口）：
  **背景**：第二层鉴权落地后，另一轮 code review（DEVGUIDE §7.4 方法论）提出 Y1–Y4 + 1 项 🟢，本轮**逐条核实后全部修复**（其中 Y4 用 `javap` 核对 spring-expression 6.1.6 字节码确认「`SpelExpressionParser.parseExpression(String)` 无内部缓存」，建议成立）。
  **① Y1 角色授予越权（最严重，授权接口本身就是提权接口）**：新增 `common.RoleGrantPolicy` 单点规则（**空间接口只能授 WS/SELF；ORG/ALL 越界**）+ 租户归属校验；`MemberServiceImpl.assertRoleGrantable` 接入 `invite`/`updateRole`。
  **同源缺口一并修**：`UMS UserServiceImpl.create` 此前连角色存在性都没校验（roleId 写错造孤儿成员）→ 补齐，且因该接口门控是 `member:create`（**空间管理员也持有**），非超管只能授 WS/SELF（否则 ws_admin 一步变 org_admin）。
  **② Y2 租户归属**：新增 `workspace.support.TenantGuard`（跨租户一律 **1004「不存在」**，不泄露存在性），`update/delete/myPermissions` + `MemberServiceImpl.requireWorkspace` 全部收口。
  **③ Y3 踢权语义**：把缓存失效**提到数据库变更之前**并纳入新接口 `WorkspacePermissionCacheInvalidator`，失效失败 → **9999「操作已取消」**（fail-closed：宁可不改库，也不留"库里没了、缓存还在"的窗口）——比原建议的"降级告警 + 依赖 TTL"更安全。
  **④ Y4 + 🟢**：SpEL `Expression` 按表达式缓存；`myPermissions` 的 roles/permissions 改为**同一份快照**（缓存值 `角色|权限`，旧格式自动兼容回源）；切面显式 `@Order(Ordered.LOWEST_PRECEDENCE)`；Service 改为依赖接口而非实现类。
  **规则性结论（可复用）**：**「能授予」=「能分配权限」——所有"带 roleId/permissionIds 参数的接口"与"权限校验接口"同级危险，必须做等级/子集约束**；以及**撤销类操作的正确顺序是"先失效缓存、再改库"**（顺序错了就没有正确性，只有窗口大小）。
  **验证**：`scripts/smoke-workspace-permission.ps1` 扩展为 17 项断言 **ALL PASS**（同一 token 在 W1 放行 / W2 `403/2006`；空间侧授 super_admin、org_admin → `1003`、授 app_developer → 200；建号侧 roleId 1/2 → `1003`、5 → 200、999 → `1001`、超管授 ALL → 200）；跨租户空间实测（`tenant_id=99`）→ `DELETE` **404/1004**、`my-permissions` **404/1004**、`PUT` 被第二层先拦 `403/2006`。
  **本轮新登记（未修，需裁决）**：**Y5 权限授予侧同源问题**（`role:permissions` 可授 `auth:write`/`system:write` → org_admin 自升平台级）；**Y6 门控语义错配**（UMS 用户管理用空间级 `member:create` 承载组织级动作）。理由：均属权限模型/跨端契约裁决，不是顺手可改。

---

## 四、踩坑记录（增量追加）

- [2026-09-17] 【**"结论误判"的方法论教训**：按记忆中的名字搜表 → 误报"表缺失"】排查阶段 6 准入时，我用 `Select-String "ie_model_route"` 搜 `init.sql` / `DB.md` 无果，就写入 §七"**路由策略表缺失，需裁决补表**"。实际**表存在**，只是真名叫 **`ie_route_policy`**（`init.sql:301` + `DB.md §5.3.3` + 云端库已建，列 `name/priority/rules` 与 IF §7.4 完全一致）——**是我拿一个想象中的表名去验证"存在性"**，正中铁律 2 的反面（"不认记忆和推测"）。
  **正确做法（以后按此执行）**：判断"某个东西在不在"，先取**全量清单**再比对——表 → `select table_name from information_schema.tables`（本次实测 35 张表一眼就能看到 `ie_route_policy`）；接口 → 搜 Controller 的 `@*Mapping`；配置项 → 搜 `@ConfigurationProperties`。**"搜不到"只能证明"这个名字不存在"，不能证明"这个东西不存在"**；跨名字的结论必须换至少两种检索口径复核后才可写入文档（本条与铁律 6 同源：同一语义可能有多套命名）。
  **善后**：已撤回 §七 该结论、同步 FE-SYNC §1，并把"路由表无需裁决"写回准入清单（真实待办只剩 `ie_secret` + 种子 + 幂等键）。

- [2026-08-25] 坑：本机已存在 `rabbitmq:4.2` 容器占用宿主 5672/15672 → 规避：项目独立起 3.13，宿主端口映射 5673/15673，容器内端口不变
- [2026-08-25] 坑：PowerShell `Get-Content` 读 UTF-8 中文文件在控制台显示乱码（GBK 编码）→ 规避：勿凭控制台乱码判定文件损坏，用 `read_file` 核对文件真实内容
- [2026-08-25] 坑：Maven POM 的 XML 注释里写 `--release 17`（两个连字符）导致 POM 无法解析（XML 注释中 `--` 非法）→ 规避：注释中避免出现连续连字符，改写成 `release=17`
- [2026-08-25] 坑：BOM 只锁 `hutool-all` 版本，但 starter-web 用了 `hutool-core` 导致版本缺失报错 → 规避：BOM 中同时锁定 `hutool-all` 与 `hutool-core` 同版本
- [2026-08-25] 坑：`insight-engine-api` 模块的 `package-info.java` 在 IDEA 中显示为灰色带 `.java` 后缀、不被识别为 Java 文件 → 根因：`package-info.java` 里没有任何 Java 类型声明（只有 `package` + Javadoc），IDEA 的「内容类型检测」无法判定它是 Java 文件 → 最终决策（企业标准）：**`package-info.java` 保持「包级文档」本分（只含 `package` + Javadoc），不写类型锚点 hack**；该显示异常是骨架阶段「api 模块暂空」的无害过渡现象，阶段 3 写入真实 Feign 接口/DTO 后自动消失
- [2026-08-25] 技术结论：实测 javac **允许** `package-info.java` 内含类型声明（不报"类型声明不允许"，只报与外部同名类型"重复"）；但企业标准仍**禁止**这么做——`package-info.java` 语义是「包级元数据/文档」专用文件，写类型会被 code review 打回、SonarQube/Checkstyle 告警
- [2026-08-25] 协作教训：用户**直接判断**"在 package-info.java 里写一个类就能被识别"方向正确（javac 确实允许），我凭"javac 会禁止"的错误认知反驳是错的——应实测验证而非先入为主；但最终「是否该这么做」要回到企业规范判断：javac 允许 ≠ 规范推荐
- [2026-08-25] 坑：`DEVGUIDE.md` 被 `LEARNING.md` 内容覆盖（两文件内容完全相同，指导手册内容丢失）→ 规避：① 从 git 历史 `git checkout HEAD -- docs/DEVGUIDE.md` 无损恢复；② 覆盖后已将 DEVGUIDE 内 27 处旧路径 `d:/CodexProject/docs/` 统一改为 `d:/CodexProject/insight-engine/docs/`；③ 教训：写文件前先读原文件确认，同名文件操作需谨慎，所有文档以 git 为兜底
- [2026-08-26] 坑：Docker Hub（registry-1.docker.io:443）无法直连（Docker Desktop 无 HTTPS 代理）→ 规避：改用 DaoCloud 镜像加速器（`docker.m.daocloud.io`，实测可达，返回 401 即服务正常），以 `docker pull docker.m.daocloud.io/pgvector/pgvector:pg15` 拉取后 `docker tag` 回标准名，成功实机建表；其余中间件镜像同样走该加速器
- [2026-08-26] 坑：`init.sql` 种子数据显式指定 `id`，不推进 `BIGSERIAL` 序列 → 应用层首次自增插入与已有主键冲突 → 规避：`init.sql` 末尾对 7 张种子表补 `setval(pg_get_serial_sequence(...))` 重置到 `MAX(id)`，实机验证自增插入正常
- [2026-09-04] 坑：本机 **Docker Desktop 无法启动**（报告未检测到虚拟化支持，WSL2/Hyper-V 后端依赖 BIOS 开启虚拟化）→ 规避：本机不再承担 Docker 负载，改用腾讯云轻量服务器（39.106.110.214）承载中间件；本机 JDK/Maven 继续用于本地跑应用
- [2026-09-04] 坑：云服务器（腾讯云 Ubuntu）`docker pull` Docker Hub 镜像报 **i/o timeout**（国内直连 Docker Hub 不通）→ 规避：daemon 配置 `registry-mirrors`（`https://docker.m.daocloud.io` 优先 + `https://mirror.ccs.tencentyun.com` 腾讯云内网），与 §三 2026-08-26 本机 DaoCloud 经验同源；兜底方案「DaoCloud 前缀拉取 + `docker tag` 回标准名」
- [2026-09-06] 坑（协作教训，促成 P17）：手动 `docker run` 与 `docker-compose.yml` 不一致埋三处隐患——① `--restart always` vs compose `unless-stopped`（`always` 无视手动 stop 强行拉起）；② PG 未挂 `pg_data` 卷 / Redis 未挂 `redis_data` 卷且未开 `--appendonly yes`（删容器/重启即丢数据，登录态、黑名单、锁定计数全在 Redis 运行态）；③ 容器名与网络同 compose 不一致（bridge 无容器名 DNS）→ 规避：DEVGUIDE 附录 B 新增 P17 约束「容器/部署命令必须与 docker-compose.yml 逐项对齐」；删除旧容器后按 compose 对齐重建（逻辑备份 → `docker rm -f` → 新 `docker run` 挂卷开 appendonly → 持久化重启验证）；Docker 不支持对已存在容器补挂卷/改启动命令，此类修正必须重建
- [2026-09-08] 坑：`git fetch/push` 报 `Failed to connect to 127.0.0.1 port 7890` → 根因：全局 git 代理（`C:/Users/admin/.gitconfig` 的 http.proxy/https.proxy）指向 Clash Verge 7890，但当时仅有 `clash-verge-service.exe` 后台服务在跑、代理内核未启动（7890 无监听）；实测直连 github.com 可通（HTTP 200）→ 处理：`git config --global --unset http.proxy && git config --global --unset https.proxy` 摘除全局代理走直连，`git ls-remote` 验证通过；若日后直连劣化需恢复代理：`git config --global http.proxy http://127.0.0.1:7890`（https 同）
- [2026-09-08] 坑：IDEA 运行服务加载 `target/classes` 里的旧配置（UMS `src/main/resources/application.yml` 已改云地址 39.106.110.214，但未 Rebuild → `target/classes/application.yml` 仍是 localhost 指向）→ 登录/注册全部 500 且快速失败（本机无 PG/Redis，连接拒绝）→ 排查要点：DB/Redis/表/密码均验证正常后，对比 `target/classes/application.yml` 与 `src` 是否一致即定位（src/target 漂移）→ 规避：改 resources/依赖代码后必须 `Build → Rebuild Project` 再 Run，不能只 Run（IDEA 增量编译不保证拷 resources）；长期建议把 `target/` 加入 gitignore 心智——产物永远可能比源码旧
- [2026-09-08] 坑：`git fetch/push` 直连 github.com 报 443 超时，但 `curl.exe --resolve` 到 140.82.114.4 / 20.27.177.113 可达（DNS 默认解析的 20.205.243.166 被墙）；api.github.com / raw.githubusercontent.com 直连可达可作快速判别 → 规避：git 走可达 IP：`git -c http.curloptResolve="github.com:443:20.27.177.113" -c http.sslBackend=schannel fetch/push/ls-remote`（Windows git 默认 openssl 后端需显式 `-c http.sslBackend=schannel`，否则报 Unsupported SSL backend）；同 IP curl 通 ≠ git 通，实测为准
- [2026-09-08] 坑（协作规范教训，促成 LEARNING 铁律）：违反 GitHub Flow「master 只接受 PR」——在本地把 `feature/ums-security-fix` merge 进 master 并试图直接 push master → 被拦截纠正；正确处理：功能分支 commit → `git push origin feature/xxx` → 网页 PR（base=master）→ Merge → 本地 `checkout master && git pull`；已在 LEARNING.md 新增「Git 常用命令速查（2026-09-08）」固化 8 场景 + 五条铁律
- [2026-09-08] 坑：PowerShell 5.1 `Invoke-RestMethod` 显示中文乱码（PS 对无 charset 响应按 ISO-8859-1 解码）≠ 服务端/DB 数据损坏——判定：curl 存原始字节后用 python 按 utf-8 解析看 `repr`/hex，字节合法即纯客户端解码问题；修复走服务端 charset（见 §三 2026-09-08）或脚本改字节安全读取
- [2026-09-09] 坑：网关响应头 `X-Trace-Id` 出现两个值——根因是网关 GlobalFilter 直接 `getResponse().getHeaders().set(...)` 的时机早于 `NettyRoutingFilter` 回写上游响应头，上游 starter-web `TraceFilter` 也回传同名头，被**合并追加**而非覆盖 → 规避：网关回写响应头必须在 `beforeCommit` 回调内 `set`（响应提交前最后一刻覆盖），已验证转发/错误各路径均为单值
- [2026-09-09] 坑：PowerShell 5.1 向 `curl.exe` 传 `-d '{"k":"v"}'` 时双引号被吞（native 参数传递规则），服务端收到 `{account:...}` → Jackson 报 `Unexpected character ('a')` 500 → 规避：请求体写入临时文件用 `-d "@file"`，勿在 PS 里内联带引号 JSON
- [2026-09-09] 协作教训（促成铁律 5 / P19）：凭据明文能进 master，根因是**规范缺口**而非工具缺失——`.gitignore` 早已能拦 `.env`，但 `docker-compose.yml` / `ums/application.yml` 本身必须入库，明文写死在文件里就必然被提交；**凡是要入库的配置文件，只能写占位符**。P17 原先「与 compose 逐项对齐」的口径还会反向推动明文扩散（compose 是明文，命令就得抄明文），已修正为「对齐结构、凭据取占位符」
- [2026-09-09] 坑：**Nacos 2.x 宿主端口偏移映射（8850/9850）会让 gRPC 连不上**——Nacos 2.x 服务端向客户端「自报 ip:port」（容器内主端口），2.x 客户端据此推导 gRPC 端口（=主端口+1000）建长连接，**推导基准是「自报地址」而非客户端填写的 server-addr** → 宿主暴露 9850、自报却是 `:9848` 必失败 → 规避：Nacos 宿主端口与容器内端口 1:1（8848/9848），偏移仅适用于不自报端口的中间件（PG/Redis/RabbitMQ 等）
- [2026-09-09] 坑：**nacos-discovery/config 的 `fail-fast` 默认 true 会阻断启动**——Nacos 不可达时 UMS 报 `NacosException: Client not connected, current status:STARTING`（注册阶段抛错，端口已起后整体关闭）→ 规避：联调/容错期显式 `spring.cloud.nacos.*.fail-fast: false`（Nacos 未就绪时服务先起），生产再评估恢复 true
- [2026-09-09] 坑：**`lb://` 路由缺 LoadBalancer 依赖**——`spring-cloud-starter-alibaba-nacos-discovery` 不传递引入 `spring-cloud-loadbalancer`，网关 `lb://` URI 会因无负载均衡器不可用 → 规避：显式引 `spring-cloud-starter-loadbalancer`（已放 starter-nacos 统一提供）
- [2026-09-09] 协作/判定沉淀：远程中间件「连不上」的排查链路 = ① 本机到目标端口 TCP 可达性（`Test-NetConnection`）→ ② 云主机侧（ss 监听 / docker-proxy / ufw / firewalld / iptables policy）→ ③ 云安全组入方向。**主机全放行 ≠ 公网可达**，安全组与主机防火墙是两层，勿只查一层就下结论

- [2026-09-17] 协作教训（想当然，非文档缺失——答复 BE-20260916-01）：实现 workspace「切换空间」时我把 `roles`/`perms` **按目标空间重算**，导致一切空间就丢掉组织级/平台级能力（`org:*`/`ws:create`/`ws:delete`），连锁产生三个现象：① 按钮消失但菜单还在（`perms` 与 `/auth/me` 的 `roles` 两套口径打架）；② `IF §5.3` 承诺的「删除当前空间 → `1003`」被 `2006` 抢先拦截（`ws_admin` 无 `ws:delete`）；③ `org:write` 驱动的可见范围缩水。**根因三段**：(a) 套用了"多租户 token 只带当前租户权限"的通用假设，**没回项目自己的模型核对**（`ie_member.workspace_id` 可空 = 组织级成员；`ie_role.scope` 分 ALL/ORG/WS/SELF）；(b) **没复用 UMS 既有口径**——UMS 登录的两个查询本就不带空间过滤，我却另写一套 `...ByUserAndWorkspace`，同一身份两种算法；(c) 还把错方向写进了 `IF §5.5`（"按目标空间维度重新展开"）并配了句自洽理由，文档与代码一起错，更难发现。→ 规避：**① 换签口径必须与登录同源（能复用就复用，不复用也要逐字段对齐）；② 冒烟必须断言「切换前后 perms 相同、仅 ws_id 变化」（本次已补）；③ 写契约时先分清「身份能力」与「请求上下文」，能力不进空间**
- [2026-09-16] 坑：**gateway 关闭时抛 `ERR_NACOS_DEREGISTER ... Client not connected, current status:STARTING` → 根因不是网络，而是 `NACOS_ADDR` 没注入**：gateway `application.yml` 的 Nacos 地址写成 `${NACOS_ADDR:127.0.0.1:8848}`（**有默认值 → 不 fail-fast**），IDEA 里没配 profile/环境变量时静默回落本机 127.0.0.1 → 启动期刷 `Server check fail ... 127.0.0.1:9848`、关闭期抛上面那条（**报错出现在 shutdown 线程，属症状非根因**）、`lb://` 路由不可用。**与 UMS/workspace 的差异**：它们的 `datasource`/`redis` 占位符无默认值 → 缺变量直接启动失败（报错显眼）；gateway 这类"有默认值"的配置则**静默降级**，更难发现。→ 规避（当日定稿）：① **Nacos 地址直接写死在 `application.yml`**（不再依赖 `NACOS_ADDR` 注入，彻底消除静默降级）；② gateway 设 `register-enabled: false`（纯消费方，不注册）；③ 三服务统一"非敏感写死 + 口令 auto-import"的配置方式（见上一条），IDEA/终端/机器怎么换都一样。**实测**：修复后启动 1.84s、`127.0.0.1` 相关报错 **0 条**、关闭时 `ERR_NACOS_DEREGISTER` **0 条**（见 §六 6.4 `lb://` 复验）
- [2026-09-16] 坑：**「昨天能连、今天连不上」的云库 = 本机公网 IP 变了**（联通动态 IP：`123.138.150.18` → `123.139.53.41`），而云安全组是「按来源 IP 放行 5433/6380/8848」→ 新 IP 不在名单里，连接被挡在云主机**之外**。**排查口径（可复用）**：主机侧全绿（`ufw inactive` / `INPUT ACCEPT` / 端口 `0.0.0.0` 监听 / 容器 Up）时，问题必在**安全组那一层**（云控制台）；用 `auth.log` 的历史 SSH 来源 IP 可反证本机 IP 是否变过。→ 规避：开发期连库走 **SSH 隧道**（只依赖 22，IP 变了也不影响）；不要把库端口长期按 IP 放行（IP 一变即失效，且容易顺手开成 `0.0.0.0/0`）
- [2026-09-16] 坑：**本机 → 云主机 5433/6380/8848 全部不可达（22 可达）**，服务启动后登录一律 500（`Unable to connect to Redis`）→ 排查链路复用 §四既有沉淀（本机 TCP → 云主机 `ss`/docker-proxy → 主机防火墙 → 云安全组）：云上容器均 `Up 6 days` 且 `0.0.0.0:5433/6380/8848` 正常监听，本机 `Test-NetConnection` 对三端口全 False、仅 22 True → 判定为**安全组已收敛（或本机出口 IP 变化）** → 临时用 **SSH 隧道**（paramiko `direct-tcpip`，本地 5433/6380 → 云 `127.0.0.1` 同端口）完成冒烟，服务侧用命令行覆盖 `--spring.datasource.url/--spring.data.redis.host` 指到 `127.0.0.1`，**不改受版本控制的配置**。验证后已停进程、关隧道
- [2026-09-16] 坑：**代码正确但权限为空** —— 切换空间后新 token 调 workspace 接口全 403/2006，一度怀疑 roles/perms 查询写错；实际是**云端库 `ie_role_permission` 缺 role 2/3/5 的增量 seed**（`ws_admin` 一条授权都没有）。定位方法：看 MyBatis SQL 日志中权限查询 `Total: 0` + 直接查库 `group by role_id` 核对 → 规避：**凡 seed/授权变更，必须同步对已初始化库执行增量 SQL**，否则表现为「权限缺失」而非「代码报错」，极难反查
- [2026-09-16] 坑：**PowerShell 5.1 读取「UTF-8 无 BOM」脚本时中文乱码**，导致引号配对错乱、脚本语法报错（`Unexpected token`）→ 规避：冒烟/临时脚本一律用**纯 ASCII**（或在 PS 中以 `-Encoding UTF8` 且带 BOM 写入）。与 §四 2026-08-25「勿凭控制台乱码判定文件损坏」同源问题的另一面：**写入侧也要注意编码**
- [2026-09-16] 坑：**IDEA 直接点 main 绿三角启动 → `local` profile 未激活 → 启动即失败**，报错形态为
  `Failed to bind properties under 'spring.data.redis.port' to int: Value: "${INSIGHT_REDIS_PORT}" ... NumberFormatException`
  ——**根因不是代码/模块问题**：`application-local.yml` 在 `src` 与 `target/classes` 都在，只是没有任何属性源提供这些变量；`redis.host`（String）会静默留成字面量，只有 `port`（int）在绑定期报错，故**报错位置具有误导性**（看起来像配置写错，实为 profile 未激活）。
  → **规避（当日改为更彻底的方案）**：**取消 profile 依赖** —— `application.yml` 非敏感项（PG/Redis/Nacos 地址、库名、账号）**直接写死 + 备注**；口令放同目录 `application-local.yml`（gitignore）由 `spring.config.import: optional:...` **自动加载** → **直接点 main 运行即可，无需任何 IDE 配置**（实测三服务零参数启动成功 + 网关全链路 200）。此前"配 `Active profiles=local` + `.run/` 共享配置"的做法已废弃并删除（概念多、易踩坑）。
  📌 与 §四 2026-09-09「IDEA 加载 target/classes 旧配置」同族：**IDEA 侧配置问题优先查 profile / src-target 漂移，勿先怀疑代码**
- [2026-09-16] 协作教训（并发编辑同一分支）：本仓库 `feature/docs-sync-security` 工作区被**两场对话并行编辑**——`LEARNING.md` 出现另一场对话的「Nacos 客户端负载均衡」大段笔记及后续 4 行改动，且**长期未提交**，与本对话改动混在同一工作区。规避：收尾提交前必须 `git diff` 辨别归属，**只提交本对话产物、勿把他人进行中的改动一并提交**；建议大文件（LEARNING）各写各章节并及时提交，避免同一文件长期脏区叠加导致提交边界模糊

---

## 五、当前阻塞 / 待解决问题（= 必须修，未完成前阻塞交付收口）

> 本节只保留**尚未解决**的必须修项；已办结项已移入 §三（决策/修复留档）/ §四（踩坑）/ §八（对话摘要），不再滞留于此。

> 当前无代码级必须修项。UMS 收尾 UMS-1/UMS-2 已实机回归验证通过并合入 master（PR #2，见 §三 2026-09-03 留档），正式关闭。
>
> **gateway 冒烟前置已全部打通（2026-09-08）**：云服务器 39.106.110.214 的 PG/Redis 已由 UMS 连接实机验证可用；UMS 配置已切云（§三 2026-09-08）；曾阻塞 gateway 冒烟的 3 项（master 缺 `JwtRefreshPayload` / ums/gateway pom 无 `repackage` / `@PreAuthorize` 拒绝误报 500）均已修复并随 `feature/gateway` 推送远程。§五无遗留**代码级**阻塞；下方**部署安全**项仍需收敛。

### 云服务器凭据安全收敛（部署安全，2026-09-08 review 新增 / 2026-09-09 核实扩面）

- [ ] 🟡 **打包产物会把本机口令带进 jar（2026-09-17 实测新增）**
  - **现象 [已核实]**：`jar tf insight-engine-ums-1.0.0-SNAPSHOT.jar` 输出含 **`BOOT-INF/classes/application-local.yml`** —— 因 `application.yml:20` 用的是 `spring.config.import: optional:classpath:application-local.yml`，**classpath 来源会被 Maven 打进 jar**，于是本机 PG/Redis 口令随产物走。
  - **影响面**：任何"把本机构建的 jar 拿去云上部署 / 发给他人 / 传网盘"的动作 = 口令泄露（且与云库同口令）；对 CI 构建无害（CI 无该文件，`optional:` 直接跳过）。
  - **修法（一行，三服务 `application.yml` 同改）**：`optional:classpath:application-local.yml` → **`optional:file:./application-local.yml`**（从**进程当前目录**读，jar 外），同时把 `application-local.yml` 从 `src/main/resources/` 挪到**服务模块根目录**（或保留位置但不再被打包）。改完需同步 `application-local.yml` 头注释、`.env.example`、TD §18.2.6 / DEVGUIDE P19 的措辞。
  - **为何未立即改**：属"配置位置变更 + 3 服务 + 文档联动"的独立任务（非顺手改，符合铁律 3），已登记待办；**在此之前：本机 jar 不要外发/上传，部署用 CI 或云上重建**。

- [ ] 🔴 **云服务器凭据明文已进入 master 历史（闸门已失守），止血只能靠口令轮换**
  - **闸门结论修正 [已核实 2026-09-09]**：原判「`d437202` 尚未进 master → master 是最后一道闸门」**已失效**——PR #5（`f84ffd2`）已把 `feature/gateway` 合入 master，`git merge-base --is-ancestor d437202 master` 现返回 0，即 `d437202`（`ums/application.yml` 的公网 IP + 弱口令明文）**已是 master 祖先**。→ 明文已进主干历史，改文件/改文档都无法回收。
  - 暴露面 [已核实]：弱口令明文同时存在于 master 历史的 `docker-compose.yml`（`ed8bfec` 引入，PG/Redis/RabbitMQ/MinIO 共 5 处）与 `ums/application.yml`；文档明文：`DEVGUIDE.md:1120,1122`、`DB.md:48`、`TD.md:1110,1127`、`LEARNING.md:4706`。
  - **本次已做（2026-09-09）**：文档明文改占位/引用 `.env`（`TD.md`/`DEVGUIDE.md`/`DB.md`/`LEARNING.md`，减少新增暴露面）；`.ssh_run.py`、`.remote_cmd.sh` 加 `.gitignore`（防误提交）；**同日第三批：代码侧配置占位化完成（见 b），`git grep` 明文归零**。**注意：这不等于消除泄露**——历史版本仍在，且口令本体未变。
  - **未做（唯一止血手段）**：口令轮换。当前处于学习/开发期，为本地联调方便**暂缓**；进入正式联调或对外部署前必须完成（届时旧口令按已泄露处理，轮换后历史残留即作废）。
  - 影响 [静态推断]：若云安全组对 5433/6380 放开公网（本机直连云库的前提），任何互联网来源可用已知口令连 PG（拖库）/ Redis（读写 key，视配置可能 RDB 落盘）。安全组来源是否已限制**需人工确认**（云控制台规则 / 云上 `ss -lntp`）。
  - 修复清单（分层）：
    - a) 🔴 **口令轮换（治本）**：云 PG/Redis（后续 RabbitMQ/MinIO）改强随机口令；**未完成前本项不闭环**。
    - b) ✅ **配置外置（2026-09-09 已完成）**：`docker-compose.yml` 5 处口令 + `ums/application.yml` 4 处（PG/Redis 地址与口令）全部改占位符；新增 `.env.example`（入库模板）/`.env`（真实值，不入库）/`application-local.example.yml` + `application-local.yml`（Spring 侧注入）；`.gitignore` 补 `!.env.example` 例外（原 `.env.*` 会把模板一起忽略）。**关键：Spring 不自动读 `.env`，应用侧须走 `application-local.yml` 或环境变量（TD §18.2.6 / DEVGUIDE P19）**。
    - c) **安全组收敛**：5433/6380 等仅放行固定来源 IP，不对 `0.0.0.0/0` 开放。**2026-09-16 实测已定位根因**：①**本机公网 IP 变了**——由 `123.138.150.18` 变为 `123.139.53.41`（联通动态 IP；证据：云主机 `auth.log` 中 SSH 来源 IP 仅有这两个，旧 IP 26 次 / 新 IP 8 次）；②云主机侧**全绿**（`ufw inactive`、`iptables -S INPUT` 仅 `-P INPUT ACCEPT`、`DOCKER-USER` 空、无 fail2ban、`0.0.0.0:5433/6380/8848` 正常监听、容器 Up 7 days）→ **拦截发生在云安全组（云主机之外那一层）**，且 5433/6380/8848 的来源被限制在旧 IP（22 端口新 IP 仍可达，说明其规则范围不同）。→ 收敛方向正确；**开发期连库统一走 SSH 隧道**（只依赖 22 端口，不受本机 IP 变化影响），不因图方便把库端口对公网放开。
    - d) **存量清理**：文档占位化已完成（2026-09-09 两批）；`docker-compose.yml` 占位化随 b) 完成；历史改写（filter-repo）破坏性大，口令轮换后可不做。
    - e) **规范补缺（2026-09-09 第二批，已完成）**：事前硬约束落地——`AGENTS.md` **铁律 5：凭据纪律** + `DEVGUIDE.md` **P19 场景约束：配置与凭据** + **P17 修正**（对齐范围改为「结构」，凭据值一律占位符）。**作用只是「防止再次发生」，不能替代 a) 轮换止血**；b) 代码侧占位化已于同日第三批完成。

### 云上 Nacos 安全加固（🔴/🟠，2026-09-09 LB 实测发现，2026-09-16 登记）

> 来源：`LEARNING.md`「Nacos 客户端负载均衡」§八 漏洞清单（实测产出，修法详见该节）。原先只落在 LEARNING，为满足「开工四必读只读 PROGRESS/FE-SYNC」的可见性，登记于此。

- [ ] 🔴 **P0-2 公网可达 + 未开鉴权（Nacos 投毒风险）**：`NACOS_AUTH_ENABLE=false` 且 8848/9848 对公网放行 → 无凭据即可读注册名单（读通即写通，可被投毒）。修法：① 开鉴权（`NACOS_AUTH_TOKEN`≥32B base64 + `NACOS_AUTH_IDENTITY_KEY/VALUE`，客户端配 username/password）；② 安全组把 8848/9848 收敛到固定来源 IP；③ 开发期更优：SSH 隧道 `-L 8848:127.0.0.1:8848 -L 9848:127.0.0.1:9848`，yml 保持 `127.0.0.1:8848`（与 §五 a) 口令轮换、c) 安全组收敛同批处理）
- [ ] 🔴 **P0-1 注册 IP 落在虚拟网卡**：UMS/gateway 注册为 `172.18.128.1`（`vEthernet (Default Switch)`），当前通只因网关与 UMS 同机；**服务上云/进容器即失败**（名单 healthy 但请求 502/超时）。修法：`spring.cloud.nacos.discovery.ip` 显式指定 / `spring.cloud.inetutils.preferred-networks|ignored-interfaces` / 容器 `--network host`——新配置项须按铁律 5 三件套（占位符 + `.env.example` + 文档）
- [x] 🟠 **P1-1 `NACOS_ADDR` 注入路径不可追溯** —— **2026-09-16 已修（改走写死，不依赖注入）**：三服务 `application.yml` 的 Nacos 地址**直接写死**为 `39.106.110.214:8848`（含注记），彻底消除"换终端/IDEA/机器静默回落 `127.0.0.1`"的路径问题；**实测**：三服务零参数（无 profile、无环境变量）启动成功，Nacos 报错 0 条，`lb://` 双服务转发全 200。副作用：地址变更需改三处 yml（当前阶段部署固定在云上，可接受）
- [ ] 🟠 **P1-2 compose 与云上 `docker run` 漂移（P17 复发）**：`docker-compose.yml` nacos 服务缺 `NACOS_SERVER_IP`（云上实配有），用它重建 Nacos → 自报容器网段 IP → 服务发现全挂。修法：出 `docker-compose.cloud.yml` 或在 compose 注释指向权威定义
- [x] 🟡 **P2-1 gateway 自身注册进名单** —— **2026-09-16 已修**：gateway `application.yml` 增加 `spring.cloud.nacos.discovery.register-enabled: false`（纯消费方只订阅不注册）。实测：Nacos 名单中只有 `insight-engine-ums` / `insight-engine-workspace`（无 gateway），且关闭时不再抛 `ERR_NACOS_DEREGISTER`
- [x] 🟡 **P2-2 pom 注释与实况矛盾**：已于 2026-09-16 修正（`gateway/pom.xml` 注释改为 `lb://`）
- [x] 🟡 **P2-3 PROGRESS 状态过期**：已于 2026-09-09 修正（§一/§6.4/§七 更新为「验证通过」）

---

## 六、后续待办与优化池（🟡 建议修 = 后续要完成 / 🟢 可选优化 = 低优先级择机）

> 本节任务**不阻塞当前交付**，按处理阶段归类；完成一条勾一条。

### 6.1 UMS 服务收尾（🟡，UMS 主线已收口，随后续阶段择机）

> **高价值优先组**（原 §七 Top2，已核对代码均未落地，保持待办）：phone 唯一索引、roleId 前置校验、授权集合去重校验、`DuplicateKey`/`HttpMessageNotReadableException`(1002) 友好映射、`Result` 成功响应 traceId 回填（末项在 §6.2）。建议 gateway 阶段收尾后作为独立任务优先处理。

- [x] 🟡 **`ws_id` 口径不一致：`/auth/refresh` 会把"当前空间"悄悄重置回默认空间** —— **2026-09-17 已修（选择方案 A：刷新沿用当前 ws_id）**：refresh token 现在**携带 `ws_id`**（`JwtUtil.createRefreshToken(userId, jti, workspaceId)`），刷新时**沿用**旧令牌的空间而非重算默认空间；实现细节与验证见 §三 2026-09-17「口径收口」条目。以下为原登记内容（留档）：
  **原登记（现象 / 证据 / 选项，留档）**：
  - **现象**：切到空间 B 后，access token 过期（2h）或前端主动刷新 → 新 access token 的 `ws_id` **变回默认空间（成员关系中最早加入的那个）**，前端"当前空间"被静默切回；`roles`/`perms` 不受影响（48/48），故很难察觉，直到发现列表/数据又回默认空间视角。
  - **证据（代码，附行号）**：`ums/AuthServiceImpl.java:130` `refresh()` → `:163` `buildLoginResponse(user)` → `:279` `Long workspaceId = roleMapper.selectDefaultWorkspaceIdByUserId(user.getId())`；即刷新链路**重算** ws_id，而不是沿用旧 token 的 `ws_id`。对照：`/auth/me` 用的是 `:307` 的"JWT `ws_id` 优先、缺失才兜底默认空间"（口径正确）。
  - **三个签发入口现状**：登录 = 默认空间（合理，此刻尚无"当前空间"）✅ ｜ 切换空间 = 目标空间 ✅ ｜ **刷新 = 默认空间 ❌（不一致）** —— 与铁律 6「同一语义只能有一种口径」冲突。
  - **裁决选项**：
    - **A（推荐）**：refresh **沿用当前 `ws_id`**。实现二选一：① 把 `ws_id` 写进 refresh token 的 Claim（`JwtUtil.createRefreshToken` 增参）；② 在 refresh 会话里与 jti 摘要一起存 `ws_id`（Redis `ie:auth:refresh:{userId}` 改存 `jti摘要:ws_id`）。代价：改 `starter-security` 的 `JwtUtil` 或 UMS 会话结构，需同步网关解析（网关不解析 refresh，影响面小）。
    - **B**：维持现状，仅在 `IF §3.2` 明确写「刷新后回到默认空间」，由前端每次刷新后重新调用切换接口。**代价**：切空间后每 2h 被弹回一次，体验差。
    - **C**：前端禁止使用 refresh（只用切换换签）→ 不可行：access 2h 过期后无续期手段。
  - **影响面**：`IF §3.2`（refresh 响应）、前端 `auth.ensureMe()` 后的空间一致性、`FE-SYNC §2`。**未裁决前前端只能按"刷新后可能回到默认空间"防御**。
- [ ] `ie_user.phone` 加部分唯一索引（`init.sql` 补 `uk_user_phone`，`DB.md` 同步）——手机号也是登录账号（IF §3.1），当前无唯一约束存在串号登录歧义
- [ ] 邮箱大小写归一：注册/创建/登录/唯一性查询统一 `lower(trim)`，防 `A@x.com` 与 `a@x.com` 注册成双账号
- [ ] 创建用户前校验 `roleId` 存在（`UserServiceImpl.create` 前置 `requireRole`），防孤儿 member
- [ ] 角色授权/创建：`permissionIds` 先去重 + 校验有效性（`RoleServiceImpl.assignPermissions/create`；`batchInsert` 改 `ON CONFLICT DO NOTHING`），防联合主键冲突与垃圾关联
- [ ] 删除角色前检查 `ie_member` 引用（`RoleServiceImpl.delete:92-103`）：被引用返回 1003 或级联清理，防用户角色静默丢失 + 孤儿数据
- [ ] `GlobalExceptionHandler` 补 `DuplicateKeyException`（并发注册/创建/角色唯一冲突 → 1001 友好文案）与 `HttpMessageNotReadableException`（body 解析错误 → 1002），不再一律 500
- [ ] 🟡 路径参数类型不匹配友好化（2026-09-09 冒烟发现）：`/api/v1/role/page` 等非数字段会命中 `/{id}` 路由并因 Long 解析抛 **500/9999**（正确列表端点为 `role/list`）→ 补 `MethodArgumentTypeMismatchException` 等类型转换异常 → 4xx（1002/1004）友好文案
- [x] `GlobalExceptionHandler` 补 `NoResourceFoundException` → **404/1004**（2026-09-08）：未映射路径此前被 `Exception` 兜底吞成 500/9999「系统内部错误」（与已修复的 AccessDenied 误报 500 同类病），前端「组织与人员」页误触 workspace/member 未实现接口时暴露此问题
- [ ] 登录失败计数原子化（`AuthServiceImpl.handleLoginFail` increment+expire 竞态）：改 Lua（INCR+EXPIRE）或 SETNX EX + INCR，防 Redis 抖动导致计数 key 永不过期
- [ ] 密码复杂度补强制大写：`RegisterRequest`/`UserCreateRequest`/`PasswordUpdateRequest` 正则改 `(?=.*[a-z])(?=.*[A-Z])(?=.*\d)`（当前只要求字母+数字，与注释宣称"含大小写"不符）
- [ ] 账号枚举收敛：登录失败统一 2001 语义（勿用 2001/2002 区分账号存在性）；锁定/计数维度从「输入 account 字符串」改「用户维度」（email 被锁不能换 phone 绕过）
- [ ] 禁用/改密后登录态删除失败补偿：先删缓存再更 DB，失败重试/告警，保证踢人必达（`UserServiceImpl.updateStatus/updatePassword`）
- [ ] 超管等高权账号操作保护：禁止自禁用/同级互操作（防超管自锁死后台），并提供管理员解锁入口（清 `ie:auth:lock:*`）
- [ ] 角色/权限变更即时生效：授权变更后按需吊销受影响用户的登录态缓存（缓解 JWT perms 2h 滞后）
- [ ] 敏感操作审计留痕：登录成功/失败、授权变更、启停等输出结构化日志（含 traceId/IP/操作人），obs 服务落地后转 MQ 写 `ie_audit_log`

### 6.2 公共层 / 基础层（🟡，随对应模块或收尾处理）

- [ ] `Result` traceId 统一回填：starter-web 增加 `ResponseBodyAdvice`，成功响应不再 `traceId=null`（IF §2.2 / TD §4.1）
- [ ] `ie_user` 加「email / phone 至少其一」CHECK 约束（🟢，init.sql / DB.md）

### 6.3 后续模块落地跟随（🟡，各模块阶段处理）

- [ ] `ie_usage_record` 补幂等唯一键 `event_id`（billing 阶段，TD §12.5，MQ 重投不重复计量）
- [ ] `ie_chunk.embedding` 维度统一约束 1024（model 网关阶段，切换 768 维本地模型需同步约束）
- [ ] `ie_message."references"`、`ie_audit_log."before"/"after"` 双引号关键字列名 → 改 `refs`/`before_data`/`after_data`（conv/obs 模块对应阶段，同步 PRD/DB）
- [ ] `ie_agent_invocation` 补 `status`/`error_msg` 列（agent 阶段，对齐 `ie_tool_invocation`）
- [x] 权限编码二级/三级混用统一规范（`kb:read` vs `model:vendor:write`）——**2026-09-09 完成**：统一规则「`资源路径:动作`，最后一段固定为动作」，写入 IF §6.7（前端按最后一个 `:` 切分或用权限树 `resource` 字段分组，禁用 `startsWith` 前缀匹配）；现有编码零改动，已答复前端 BE-20260909-06
- [ ] `WorkspaceMapper` 直查 `ie_workspace` 改走 Feign（workspace 服务落地后，TD §3.2 服务边界）——**workspace 已于 2026-09-16 落地，该项仍未做**：`/auth/me` 的 `workspaceName` 目前仍由 UMS 直查 `ie_workspace`（同库只读），需与下一项一并收口
- [ ] `UserRefMapper` 直查 `ie_user`（workspace 侧按邮箱定位已注册用户，IF §5.6 添加成员）同样属 MVP 临时直查；与上一项一起抽象为 `insight-engine-api` Feign 契约（TD §3.2）
- [x] 🟡 **空间维度授权 —— 第①步「轻量版」已完成（2026-09-17）；第②步「行级 DataScope」待做**（由 BE-20260916-01 引出，属 TD §7.5；TD §7.5 已补实现进度表）
  **✅ 落地内容（第①步，2026-09-17）**：`common` 新增 `@WorkspacePermission`（`value` 权限码 + `workspaceIdExpr` SpEL，缺省取 token `ws_id`）；
  `starter-security` 新增 `WorkspacePermissionChecker`（SPI）+ `WorkspacePermissionAspect`（判定失败 `403/2006`，`@ConditionalOnBean` 装配、无实现则不生效）；
  workspace 实现 `WorkspacePermissionCheckerImpl`（`ie_member → ie_role → ie_role_permission → ie_permission`，缓存 `ie:ws:user-perm:{wsId}:{userId}` 10min，成员变更主动失效）；
  校验点：`member/page|invite`（注解）、`member/{id}` 移除与改角色（服务内显式判定，目标空间需按 memberId 反查）、`workspace/{id}` 更新（注解）；**`ws:create`/`ws:delete` 属组织级，不做空间判定**；
  新接口 `GET /api/v1/workspace/{id}/my-permissions`（IF §5.7）供前端门控同源。
  **证据**：`scripts/smoke-workspace-permission.ps1` **全绿**（隔离实例 :17101/:17102）——同一 token（`perms`=27 含 `member:create`）在 W1（ws_admin）invite→**200**、在 W2（end_user）invite→**403/2006**「您在当前工作空间没有该操作权限」；`my-permissions` W1=27 条 / W2=7 条（与 token 并集不同）
  **设计（原计划，留档）**：
  **现状**：token 只承载「用户级能力」（用户维度全量，与登录一致），服务端**尚未**按「当前 `ws_id`」做二次判定，故"在 A 空间能建、在 B 空间不能建"这类需求**目前无法表达**。
  **设计（分两步，先轻后重）**：
  ① **轻量版**：新增 `@workspacePermission("kb:write")`（或 `WorkspacePermissionAspect`），判定 = 当前 `ws_id` 空间内该用户是否拥有该权限（`ie_member`→`ie_role`→`ie_role_permission`→`ie_permission`，走 Redis 缓存 `ie:ws:member:{wsId}` / `ie:role:permissions:{roleId}`，TD §6.1）；同时给前端补「我在这个空间的权限」接口（或在 `/auth/me` 增 `currentWorkspacePerms` 字段），保证**按钮显示与后端判定同源**（否则会出现"按钮在、点了 403"）；
  ② **彻底版**：ABAC/DataScope 行级拦截器（`InnerInterceptor#beforeQuery`）自动追加 `workspace_id = 当前 ws_id`；并按 §6.6 方案 A 把 perms 从 token 移到「角色→权限」缓存（token 只留角色编码），顺带解决权限变更 2h 滞后与 `perms` 膨胀。
  **纪律**：这两步都**不得**回退到"按空间裁剪 token 权限"的老路（已证实会丢组织级/平台级能力，见 §四 2026-09-17）
  **2026-09-17 code review 追加（第①步的收口缺口）—— 4 项 🟡 + 1 项 🟢 已全部收口（2026-09-17 第二轮修复）**：
- [x] 🟡 **成员角色授予无越权约束** —— **已修**：新增 `common.RoleGrantPolicy`（规则单点：`grantableWithinWorkspace` 只放行 WS/SELF、`belongsToTenant` 校验租户归属、`bundled role codes` 常量）；`MemberServiceImpl` 新增 `assertRoleGrantable`（存在性 + scope + 租户，违规 1003），`invite` 与 `updateRole` 均接上。**同源缺口一并修**：`UMS UserServiceImpl.create` 此前连"角色是否存在"都没校验（`roleId` 写错即产生孤儿成员关系）→ 现同样校验，且因**该接口门控是 `member:create`（空间管理员也持有）**，故非超管只能授予 WS/SELF，ORG/ALL 仅超管可授予。证据：冒烟 §9/§10 全绿（空间侧 super_admin/org_admin → 1003、app_developer → 200；建号侧 1/2 → 1003、5 → 200、999 → 1001、超管授 ALL → 200）
- [x] 🟡 **空间操作未校验租户/组织归属** —— **已修**：新增 `workspace.support.TenantGuard`（跨租户按 **1004「不存在」** 处理，不泄露存在性），`WorkspaceServiceImpl.update/delete/myPermissions` 走 `requireWorkspaceInTenant`，`MemberServiceImpl.requireWorkspace` 也补上。证据：造 `tenant_id=99` 的空间实测（管理员 token）→ `DELETE` **404/1004**、「`my-permissions`」**404/1004**；`PUT` 因走第二层鉴权先被拦 **403/2006**（同样拒绝，防线更前）
- [x] 🟡 **权限缓存失效失败会破坏"踢权"语义** —— **已修（选择 fail-closed 而非"降级告警"）**：`evict/evictWorkspace` 移入新接口 `WorkspacePermissionCacheInvalidator` 并**提到数据库变更之前**调用（`remove`/`updateRole`/`invite`/`deleteWorkspace` 四处），失效失败即抛 **9999「权限缓存失效失败，操作已取消」**→ 库未动、权限状态始终一致、无 10min 窗口。理由：撤销类操作宁可失败也不留"库里没了、缓存还在"的窗口（Redis 故障时登录态校验本也已不可用）。注：该失败路径需注入 Redis 故障才能实测，本轮为**静态验证（顺序+异常语义）**
- [x] 🟡 **SpEL 表达式每次请求重新 parse** —— **已修**：`WorkspacePermissionAspect` 增加 `EXPRESSION_CACHE`（`ConcurrentHashMap<String, Expression>` + `computeIfAbsent`），求值上下文仍每请求新建。**实测依据**：`javap` 核对 spring-expression 6.1.6，`SpelExpressionParser.parseExpression(String)` **无内部缓存**（`InternalSpelExpressionParser` 只有正则 `patternCache`），故该建议成立；冒烟全绿说明缓存路径下注解判定行为不变
- [x] 🟢 **三项小问题** —— **已修**：① `myPermissions` 的 `roles` 与 `permissions` 改为**同一份缓存快照**（缓存值 `角色码,角色码|权限码,权限码`，新增 `WorkspacePermissionChecker#rolesOf`；旧格式无 `|` 时自动回源覆盖，向后兼容）；② `WorkspacePermissionAspect` 显式 `@Order(Ordered.LOWEST_PRECEDENCE)`（原依赖隐式默认）；③ 新增 `WorkspacePermissionCacheInvalidator` 接口，`MemberServiceImpl`/`WorkspaceServiceImpl` 改为**依赖接口**（不再注入 `WorkspacePermissionCheckerImpl` 实现类）
- [ ] 🟡 **【本轮新发现 Y5】权限授予侧同源问题**：`PUT /api/v1/role/{id}/permissions`（`RoleServiceImpl.assignPermissions`，门控 `role:write`，**`org_admin` 持有**）可把 `auth:write`/`system:write` 等**平台级权限**授予任意角色（包括自己所在的角色）→ 组织管理员可借"角色授权"把自己的权限升到平台级。当前种子特意把 `auth:write`/`system:write` 排除在 `org_admin` 之外，说明设计意图是"不该有此能力"，但分配接口没挡住。建议二选一：① **按权限域分级**（`auth:*`/`system:*` 等平台级仅超管可授）；② **权限子集原则**（只能授出"授予者自己拥有"的权限，通用且一劳永逸）。未修原因：属权限模型裁决（影响 IF §6 与前端角色授权页），不是顺手可改
- [ ] 🟡 **【本轮新发现 Y6】门控语义错配：空间级权限码承载组织级动作**：UMS 用户管理（建号/改资料/改状态，`UserController:57/67/79`）用 **`member:create`/`member:update`** 门控，而 `member:*` 是**空间级**权限（`ws_admin` 持有）→ 空间管理员可创建平台账号。本轮已在服务层收紧（非超管只能授 WS/SELF 角色、roleId 必须存在），但"ws_admin 能否建号"本身仍是越界。建议：权限字典补 `user:*` 域（或改挂 `org:write`）——**属跨端契约变更**，需同步 FE-SYNC + 前端按钮门控。未修原因：会移除 `ws_admin` 的建号能力，需前端配套
- [ ] DataScope 行级数据权限拦截器（TD §7.5）——多租户/V1.0 前必须完成，覆盖全部业务列表查询。**归属待裁决（2026-09-17 登记）**：DEVGUIDE 把它列在**阶段 5（workspace）**，实际未做；**建议不在阶段 6（model）之前补**（当前仅 `ie_workspace` 一张业务表且已按 tenant/org 收敛，主战场是阶段 7 的 `ie_kb*` 等带 `workspace_id` 的表）→ **建议明确为"阶段 7 落地前完成"**，待开发者确认后同步 DEVGUIDE 阶段 5/7 措辞（避免"阶段 5 未完成就进阶段 6"的口径含糊）
- [x] 🟡 **token 内 `roles`/`perms` 口径不统一（2026-09-17 开发者裁决：方案 B，已实施）** —— 裁决为 **B. 统一为全局**：`switch` 只换 `ws_id`、不重展开 `roles`/`perms`，三个签发入口统一走 `AuthTokenIssuer` + `common.AuthQuerySql`；"按空间隔离权限"的需求**另立为 §6.3「空间维度授权」两步**（服务端二次判定 + 前端「当前空间权限」接口），**不作为 token 口径**。验证：`scripts/smoke-auth-claims.ps1` 全绿。原登记（留档）：：`roles`/`perms` 有**三个签发入口**，其中 **UMS 登录 / 刷新按全局展开**（`RoleMapper.selectRoleCodesByUserId`、`PermissionMapper.selectPermissionCodesByUserId`，仅按 `user_id` 过滤），而 **`workspace/switch` 按目标空间展开**（`RoleMapper.selectRoleCodesByUserAndWorkspace`，带 `m.workspace_id` 条件）→ 登录后是全局权限、切空间后仅本空间权限，**同一语义两套口径**。IF §3.5 已如实标注「两者语义不同」，但**从未裁决**。两条路：**A. 统一为按空间**（登录也按默认空间展开，`org_admin` 无空间时保留全局兜底）——权限隔离更正确，需改 UMS；**B. 统一为全局**（`switch` 只换 `ws_id`、不重展开 `roles`/`perms`）——改动小但切空间无权限隔离。**裁决前不得再扩散第三种口径**（AGENTS.md 铁律 6）

### 6.4 gateway / Nacos / 部署阶段（🟡）

- [x] **认证模型定案**：ADR-5（网关校验 JWT 下发明文头）vs 当前「服务自校验 JWT」双轨矛盾 → 已裁决（2026-09-03）：网关校验 JWT + 下发头，但服务端通过 `insight.web.trust-gateway-headers` 开关决定是否信任，双轨共存、默认自校验，见 §三 2026-09-03 留档
- [x] gateway 模块骨架 + 路由 + 全局 Cors（2026-09-03 完成，`feature/gateway` 分支）：POM 补 jjwt/可执行插件、`GatewayApplication`、`application.yml`（端口 7000、UMS 路由 `/auth/**`+`/api/v1/**`+文档路径、globalcors），`mvn install` 编译通过
- [x] gateway AuthGlobalFilter：JWT 校验 + 明文头注入（防客户端伪造头）+ `sk-` API Key 分流 + 错误转 Result（TD §8.3）——代码完成并已提交 f45a226（2026-09-04），编译通过，待联调验证
- [x] **gateway 全链路冒烟**（2026-09-08 完成，云 PG/Redis + UMS 7101 + gateway 7000）：登录/me/refresh 200、register 400 业务码、无 token/坏 token 401、权限不足 403（修复后）、logout 后旧 token 401（撤销生效）——8/8 通过；3 修复见 §三 2026-09-08；`/doc.html` 白名单、`sk-` 分流、过期 token 2007 本次未覆盖，随路由收窄/Nacos 轮补
- [x] **gateway 复跑冒烟 9/9**（2026-09-09，云 PG/Redis + UMS 7101 + gateway 7000）：登录 200 / 转发 `user/page` 200 / 无 token 401-2001 / 坏 token 401-2001 / **过期 token 401-2007** / **`/doc.html` 200** / **`sk-` 401-2001** / end_user 403-2006 / `/api/v1/nonexistent` 404（路由收窄生效）；全部响应 `Content-Type: application/json;charset=UTF-8`，六类错误/转发路径 `X-Trace-Id` 均为单值（修复见 §三 2026-09-09）
- [ ] 🟡 未匹配路由（`/api/v1/xxx` 无路由）不进入 GlobalFilter 链 → 404 响应无 `X-Trace-Id`、body 为 Spring 默认错误格式（非 IF §2 `Result`）；如需前端统一处理，需改用 WebFilter 或补 WebFlux 错误处理器（2026-09-09 复跑冒烟发现，不阻塞联调）
- [x] **gateway 路由按 TD §8.3 细分**（2026-09-09 完成）：`/api/v1/**` 全量 fallback 已收窄为 `/auth/**` + `/api/v1/user|role|permission/**` → ums（含 `/doc.html`/`/webjars/**`/`/v3/api-docs/**` 文档路径），其余 `/api/v1/xxx` 网关层直接 404 不误转 UMS；后续服务接入按 P18 同批新增专属前缀
- [x] **gateway 新增 workspace 路由**（2026-09-16 完成，P18 同批核对）：`/api/v1/org/**,/api/v1/workspace/**,/api/v1/member/**` → `lb://insight-engine-workspace`（与 UMS 前缀无交集，IF §5 章节一一对应）；同步更新 TD §8.3 路由表；AuthGlobalFilter 白名单无需变更（workspace 无公开端点）、globalcors 仍为 `/**`。**谓词/前缀转发已经网关实机复验**（`/api/v1/org/1`、`/api/v1/workspace/page`、`/api/v1/member/page` 均 200，未接入前缀 404，无 token 401-2001）
- [x] 🟡 **workspace 路由的 `lb://` 服务发现复验** —— **2026-09-16 完成**（安全组放行本机 IP 到 8848/9848 后）：三服务以 `local` profile 启动 → 云端 Nacos 名单确认 `insight-engine-ums(172.20.160.1:7101 healthy)` / `insight-engine-workspace(172.20.160.1:7102 healthy)`，**gateway 不在名单**（`register-enabled: false` 生效）→ 经网关 `:7000` 走 `lb://`：`/auth/login` code=0、`/api/v1/org/1` 200、`/api/v1/workspace/page` 200、`/api/v1/user/page`（UMS）200、`/api/v1/kb/x` 404（无路由）。验证后三服务已停，端口释放
- [ ] 🟡 注册 IP 仍落在本机虚拟网卡（本次实测为 `172.20.160.1`，此前是 `172.18.128.1`，随机器网络变化）——P0-1 未解，服务跨机/进容器部署前必须显式指定 `spring.cloud.nacos.discovery.ip`
- [ ] 🟢 **Nginx 入口层接入（生产向，部署阶段）**：托管前端 `dist` + `location /api/` 反代网关集群（静态上游，方案 A）+ 透传 `X-Forwarded-For/Proto`；接入后前端入口由 `:7000` 改为 **443 同源**（顺带消除跨域，`globalcors` 转生产白名单）。补齐后形成「Nginx（服务端 LB）→ Gateway（客户端 LB）→ 服务」两层 LB 链路；学习笔记见 LEARNING「负载均衡 LB」篇附二
- [x] **云上 Nacos 部署 + UMS/gateway 接入代码**（2026-09-09，P2-1/P2-2 代码侧完成）：容器 healthy、端口 1:1（8848/9848）、自报公网 IP；ums/gateway 已接 starter-nacos + loadbalancer + `fail-fast:false`；gateway 路由已改 `lb://insight-engine-ums`；全量编译 BUILD SUCCESS
- [x] **注册实机验证（P2-2/P2-3，2026-09-09 完成）**：安全组放行后本机 `-DNACOS_ADDR=39.106.110.214:8848` 启 UMS + gateway → Nacos 两实例 UP（172.18.128.1:7101/7000 healthy）→ gateway `lb://insight-engine-ums` 转发全链路冒烟 **8/8 通过**：login/me/user/page/role/list 200（code=0）、无/坏 token 401-2001、错口令 401-2002、`/doc.html` 200
- [ ] 中间件与应用密码差异化 + 明文占位化：**占位化部分 2026-09-09 已完成**（`docker-compose.yml` 5 处 + Grafana 1 处 + `ums/application.yml` 4 处全部改 `${VAR}`；新增 `.env.example`/`.env`/`application-local.example.yml`/`application-local.yml`；`git grep` 明文归零）；**「差异化」（各中间件/应用改用不同强随机口令）仍待随 §五 a) 轮换一并做**。注意 **Spring 不自动读 `.env`**，应用侧注入见 TD §18.2.6。**PR #5 已合入 master，明文已进主干历史，须先轮换口令，详见 §五 2026-09-09 修正**
- [ ] 🟡 铁律 5 自检残留两处「有意保留的明文」待收口（2026-09-09 自检发现，**非本轮占位化范围**）：① `init.sql:999` 种子管理员注释含明文口令（`admin@example.com`，BCrypt hash 已入库，前端 P1 登录即用此账号）；② `gateway/application.yml:37` / `ums/application.yml:46` 的 JWT 本地开发默认值（含 `change-me`，prod profile fail-fast 拦截）。二者均为「本地开发便利」的有意妥协；收口方向：种子账号改「首次登录强制改密」或由部署方注入，JWT 去默认值改强制注入（需同步 `.env.example` + 本地启动说明）
- [ ] 引入 Flyway schema 迁移（替代一次性 init.sql）——**现状与动因（2026-09-16 补记）**：`init.sql` 是「PG 容器首次初始化」脚本（`docker-entrypoint-initdb.d`，仅空库首启执行），且 `CREATE TABLE` 无 `IF NOT EXISTS`，**对有数据的库重跑会直接报错**；因此每次 schema/seed 变更都只能靠人肉挑段执行到云端库——**2026-09-16 实测的「云端 role 2/3/5 授权缺失 → 403」正是这一模式的直接后果**（改动写了、云端没执行，表现为权限缺失而非报错，极难反查）。引入 Flyway 后：变更按 `V{n}__xxx.sql` 递增、由 `flyway_schema_history` 记录执行状态，启动/部署即自动补齐；落地要点见 §6.4（建议与中间件迁云同批，并先对已初始化库做 `baseline`）
- [ ] 部分容器 healthcheck 补 `start_period`（🟢）
- [x] gateway 增 TraceGlobalFilter（order=-200，TD §8.3 过滤器链首项）——**2026-09-09 完成**：读取/校验上游 `X-Trace-Id`（非法/缺失则生成 UUID）→ 重建请求头透传 → 回写响应头；`AuthGlobalFilter` 错误响应必带 traceId
- [x] JWT Claim 常量下沉 common——**2026-09-09 完成**：新增 `common.constant.JwtClaimConstants`（type/access/refresh/tenant_id/ws_id/roles/perms），UMS `JwtUtil` 与 gateway `GatewayJwtParser` 改为共享引用，消除双份字面量
- [x] gateway 错误响应补 `charset=UTF-8`——**2026-09-09 完成**：`AuthGlobalFilter.writeError` 改用 `new MediaType(APPLICATION_JSON, UTF_8)`，与 UMS 侧 charset 修复对齐
- [ ] 🟡 **本机 `mvn package` 产出的 jar 内含 `application-local.yml`（真实云库/Redis 口令）**（2026-09-16 核查发现）：已实测 `insight-engine-ums-1.0.0-SNAPSHOT.jar` 中同时存在 `BOOT-INF/classes/application-local.yml`（647B，含真实凭据）与 `application-local.example.yml`。`target/` 已被 gitignore（**不进 Git**），但只要把本机打的 jar 外传 / 上传服务器 / 当交付物，即等于把口令一起送出去（属 P19 精神范围内的泄露路径，非已泄露）。**修法（择一）**：① 推荐——把 `application-local.yml` 移出 `src/main/resources`（如放工程外或用 `--spring.config.additional-location` / 环境变量注入），jar 不再携带凭据；② 纪律约束——本机 jar 不外传，部署只用「干净源码构建 + 环境变量注入」（§五 a 轮换后此条风险随之收敛）。**注：2026-09-16 冒烟时本机 jar 确实带该文件运行过，未见外泄**
- [ ] API Key（`sk-`）通道落地（`AuthGlobalFilter` 当前一律拒绝 2001）——workspace/conv 阶段开放 OpenAPI 前必须实现 TD §7.6 校验
- [ ] gateway 接入 Sentinel 限流（TD §8.3 RateLimitGlobalFilter，生产前）；CORS 由 `*` 收敛为白名单（生产）
- [ ] refresh 单槽轮换多端语义：并发/多标签刷新可能被判重放并吊销全会话 → 与前端约定 refresh 单飞互斥（IF/前端联调说明）

### 6.5 长期优化池（🟢，MVP 择机处理）

- [ ] 组装用户信息多次查库合并：登录 4~5 次 / `me` 3~4 次查询 → 1~2 条联表 SQL 或引入 TD §6.1 用户/权限缓存
- [ ] `resolveDefaultRoleId` 每次注册查库 → 启动时加载 end_user 角色 ID 常量/缓存
- [ ] `logout` 重复解析 token（`getRemainingSeconds` 与 `parseAccessToken` 各 parse 一次）→ 一次解析复用
- [ ] `RoleCreateRequest.scope` 无默认值且表列可能 NOT NULL → 默认 SELF
- [ ] `status` 判空语义收紧：`!ACCOUNT_NORMAL.equals(status)` 拦截，null/异常值不放行
- [ ] 登录补图形验证码 + IP 维度限流（V1.0；`CAPTCHA_ERROR(2005)` 已定义未使用）
- [ ] 创建类接口接 `X-Request-Id` 幂等（IF §2.1 / TD §13.3）
- [ ] `TokenDigestUtil` 与黑名单内 `sha256Hex` 重复实现 → 抽公共工具
- [ ] 权限树 `RESOURCE_NAMES` 本地映射与 DB 权限字典易 drift → 字典/注释生成

### 6.6 JWT 权限载荷演进（后续迭代，MVP 不处理）

- [ ] **JWT `perms` Claim 膨胀优化**：当前把权限编码全量塞进 token（超管 48 权限，`perms` 约 1.3KB，整条 ~2KB，每次请求全量携带）。MVP 单租户权限量小可接受；权限规模上来后二选一：
  - 方案 A（推荐）：JWT 只存角色编码 + Redis 缓存「角色→权限」映射，兼顾体积与实时性（引入缓存一致性：改权限主动删缓存/短 TTL）
  - 方案 B：权限编码位图压缩（权限表加 `bit_index`），token 最小但可读性差
  - 详见 LEARNING.md「RBAC vs ABAC + 权限进 JWT 的权衡」权衡③详解

---

## 七、下一步计划（Top 3）

1. **workspace 收尾（阶段 5 收口，只差两步）**：① **建 PR 并合入 master**——`feature/workspace` 已推送且**领先 master 20 commits**（`cf6580c` 为最新），本机未安装 `gh`，PR 待开发者点：<https://github.com/Goutouxiaoen/insight-engine-backend/compare/master...feature/workspace>；② **前端把空间内门控改用 `GET /api/v1/workspace/{id}/my-permissions`**（FE-SYNC §2 2026-09-17 条目），并在 `BE-ISSUES.md` 把 `BE-20260916-01` 核到 `🟢`（后端已回填答复 + 证据，2026-09-17）
2. **§五 口令轮换（唯一止血手段，部署前必办）**：云 PG/Redis（后续 RabbitMQ/MinIO）改强随机口令 + 安全组收敛（需云凭据/控制台，见 §五）；文档占位化 + **规范补缺（`AGENTS.md` 铁律 5 / `DEVGUIDE.md` P19 / P17 修正）+ 代码侧配置占位化（§五 b）均已完成**——**未轮换前该项不闭环**
3. **阶段 6「模型网关」准入（2026-09-17 判定：有条件进入；开工前需 3 项裁决 + 若干必办）**：
   - ✅ **已具备**：IF §7.1~§7.8 契约完整（厂商 CRUD / 模型 CRUD / 路由策略 / **chat completions SSE** / embeddings / rerank / 用量查询）；权限码 `model:*` **7 条**已在字典（实测授权：`super_admin` 7 / `org_admin` 7 / `ws_admin` 2 / `end_user` 1 / **`app_developer` 0** → 说明"模型管理=组织级、空间级只读"的分域已天然成立，但**应用开发者调用 chat/embeddings 会 2006**，需按 PRD §12.2.2 复核是否补种子）；`ie_model_vendor` / `ie_model` 两表**云端已建**；`starter-security`（JWT/权限/第二层空间鉴权）、`starter-redis`、`starter-mybatis`、OpenAPI 均可复用；网关路由纪律与 workspace 已示范接入方式；IF §2.6 SSE 心跳契约已定（`heartbeat`/15s）
   - ⬜ **必须补（DB/契约缺口，按铁律 2/6 先裁决再动手）**：
     1. ~~`ie_model_route` 表缺失~~ **【2026-09-17 更正：此项为误判，已撤回】**——路由策略表**已存在**，名为 **`ie_route_policy`**（`init.sql:301`、`DB.md §5.3.3`、`PRD.md:2160`、云端库已建）。列 = `id/name/rules(jsonb)/priority/enabled/created_at/updated_at/created_by/updated_by/deleted`，与 **IF §7.4 的 `{name, priority, rules}` 完全对得上**，无需补表、无需裁决。**真实待办只剩一条小的**：该表**无 seed**（实测 0 行）→ 本章是否需要一条"默认路由"种子（可选，不阻塞）。误判成因见 §四 2026-09-17；
     2. **`ie_secret` 密钥表缺失（成立，需裁决）**：`ie_model_vendor.api_key_secret_id` 已引用它（`init.sql:1150` 注释自认"阶段 3/8 落地时补充"），而 TD §16.1 要求「模型 API Key：**AES-256-GCM 加密存储，KEK 放环境变量/Secret**」。二选一：**(a) 推荐**——新建 `ie_secret`（与已预留的 `api_key_secret_id` 契约一致；字段建议 `id/tenant_id/name/secret_type/cipher_text/ iv_or_nonce /algo/kek_version/enabled/created_by…`，可轮换、可审计）；**(b)** 复用 `ie_sys_config`（`id/config_key/config_value/description`，把密文塞 `config_value`）——**不推荐**：语义错位（系统配置表当密钥库）、无租户维度、无算法/KEK 版本字段，将来轮换与合规都难做。**未裁决前该章只做只读接口，不落密钥写入**；
     3. **厂商/模型种子为空**（实测云端 `ie_model_vendor` = **0 条**）→ 至少补 `qwen`（通义）与 `ollama` 两条 vendor，`apiKey` 走加密表，不写明文；
     4. **用量计入口径**：`ie_usage_record` 已存在，但 §6.3 登记"补 `event_id` 幂等唯一键（billing 阶段）"→ 本章会写用量，建议**在 model 阶段就把幂等键补上**（否则重试/流式中断会重复计量，billing 阶段再补要回改）；
     5. **需开发者提供的输入**：① 通义 DashScope **API Key**（放各服务 `application-local.yml` 或密钥表，**不入库明文**）；② ~~本机/云端是否有 Ollama~~ → **【2026-09-17 已确认：本机/云端均无 Ollama，且不阻塞】** 替代方案（任选，`ie_model_vendor` 表天然多厂商：`code` + `base_url` + `type`，换厂商**不改代码**）：**(a)** 通义 DashScope（计划内首选，OpenAI 兼容模式 + 免费额度 + `text-embedding-v3` 恰好 1024 维，对上 `ie_chunk.embedding` 约束）；**(b)** 其它 OpenAI 兼容云厂商（智谱 GLM / DeepSeek / 硅基流动 / Kimi）——插一条 vendor 记录即可；**(c)** 本机轻量替代（不想装 Ollama）：**LM Studio**（GUI + 兼容端口）/ `llama.cpp server` / vLLM（需 GPU）。**结论：离线本地模型不是阶段 6 的必需品**，其真实价值在 kb（阶段 7）的 embedding/rerank 省钱与断网演示，届时再定；
     6. **权限种子复核**：实测 `app_developer` 的 `model:*` 授权为 **0 条**（`super_admin`/`org_admin` 各 7、`ws_admin` 2、`end_user` 1）→ 若按 PRD §12.2.2「应用开发者可管理知识库/Agent/工具」的语义，其调用 `chat/completions`、`embeddings` 属必需 → 需裁决是否补种（`init.sql` + 云端增量 seed，带 `ON CONFLICT DO NOTHING`）
     7. **★模型归属口径（2026-09-17 开发者提出，必须开工前裁决——直接影响表结构）**：开发者指出平台定位不是"简单和模型对话"，而是**编排智能体/工作流**，因此**用户应当可以配置自己的大模型**。但**实测当前 schema 不支持**：`ie_model_vendor`（`code/base_url/api_key_secret_id/type/enabled/config`）与 `ie_model`（`vendor_id/code/display_name/type/context_window/input_price_per_1k/output_price_per_1k/enabled`）**都没有 `tenant_id` / `workspace_id`** → 现状 = **平台级统一模型目录**（PRD L1921 时序图也是「Admin 配置模型厂商（通义+Ollama）」、L1105 用户故事是"统一接口调用任意大模型，不关心底层厂商"）。
     **两条路线**：**A（一期）** 保持平台级目录：Admin 接入 + 统一调用 + 路由（主备降级/成本能力择模），BYOK 后置；**B（直达目标）** 多级模型库：`ie_model_vendor`/`ie_model` 增 `scope(PLATFORM/TENANT/WORKSPACE)` 或 `workspace_id`（NULL=平台级）+ 空间级接入能力（权限码 `model:vendor:write` 的空间级语义 + 前端页面归属 + 密钥按空间隔离）。
     **建议（折中，不阻塞一期）**：**一期走 A，但建表时先把归属列留出来**（`tenant_id`/`workspace_id` 可空，一期只写 NULL）→ 一期能最快跑通"真调通义 + SSE"，二期做空间级接入时**不必改表结构**。
     **连带修正**：IF §7.4 的示例 JSON 里写了 `{"match": {"tenantTier": "PRO"}}`（按租户分档）——若定位以"Agent/工作流按需选模型 + 主备降级"为主，该示例应改写为 `agentId`/`scene`/`capability` 之类的 match 条件（**待裁决后同步 IF**）。
   - ⬜ **需一并裁决**：**阶段 5 的「数据权限拦截器（DataScope）」归属** —— DEVGUIDE 阶段 5 含此项，现未做；**建议**：不阻塞阶段 6，但**必须在 kb（阶段 7）落地前完成**（理由：现阶段只有 `ie_workspace` 一张业务表且已按 tenant/org 收敛；`ie_kb`/`ie_kb_doc` 等带 `workspace_id` 的表在阶段 7 才出现，那才是它的主战场）。**待开发者确认该归属**（确认后同步 DEVGUIDE 阶段 5/7 与 §6.3）

---

## 八、最近一次对话摘要

- 日期：2026-09-17（workspace 收尾 + 阶段 6 准入判定）
- 内容：**workspace 阶段收尾 + 「模型网关」准入判定** —— ① **收尾三件套**：全量 `mvn -DskipTests package` **BUILD SUCCESS**；`scripts/smoke-workspace-permission.ps1` **17 项断言 ALL PASS** + 跨租户实测（`DELETE`/`my-permissions` → 404/1004）；文档同步（IF §4.2/§5.6/§5.7、TD §6.1/§7.5、PROGRESS §二/§三/§6.3/§七/§八、FE-SYNC §1/§2、FEATURES 4.1、LEARNING）；提交 `15d7504` + `cf6580c` 已推送 `feature/workspace`（领先 master **20 commits**）。② **补齐铁律 1 漏掉的双写**：`BE-20260916-01` 前端侧仍是 `🟡 待后端答复`（`FE-SYNC` 早已答复），本轮回填「后端答复 + 证据 + 状态 ✅」并提示前端核对后改 `🟢`；同时把 `my-permissions` 门控建议与角色授予收紧一并写入该条（避免前端只看 FE-SYNC 漏掉）。③ **准入判定（结论：有条件进入阶段 6）**：契约/权限码/表（`ie_model_vendor`/`ie_model`/**`ie_route_policy`**）/starter 能力/SSE 心跳契约均就绪；**真正必须先办的是 2 项**——**(a)** **【已更正】** 原判"`ie_model_route` 表缺失"**系误判**：路由策略表已存在，名为 **`ie_route_policy`**（`init.sql:301` + `DB.md §5.3.3` + 云端已建，列 `name/priority/rules` 与 IF §7.4 一致），**无需裁决补表**，仅"无 seed"（可选补默认策略）；误判成因 = 按记忆中的表名去搜而没列全表清单 → 已立为 §四 踩坑并给出正确做法（`information_schema.tables` 先列全量再比对）；**(b)** `ie_secret` 密钥表缺失（`api_key_secret_id` 已引用它、TD §16.1 要求 AES-256-GCM）→ 需裁决"新建 `ie_secret`（推荐）or 复用 `ie_sys_config` 存密文（不推荐）"；**(c)** 厂商/模型种子为空（云端实测 0 条）+ 需真实 DashScope Key；**(d)** `ie_usage_record` 的 `event_id` 幂等键建议在 model 阶段就补。④ **另需开发者裁决**：阶段 5 的「DataScope 行级拦截器」归属（建议不阻塞阶段 6、但 kb 前必须做，口径已写入 §七）。⑤ 环境干净：隔离实例已停、测试数据已清、IDEA 三服务未动；工作区仅剩另一对话的 `AGENTS.md`/`LEARNING.md`（未动）。
- 日期：2026-09-17（code review 收口轮）
- 内容：**修完上一轮 code review 的 4 项 🟡 + 1 项 🟢，并额外修掉 2 处同源缺口** —— ① **核实**：Y1（成员角色授予无越权约束）、Y2（空间操作未校验租户归属）、Y3（缓存失效失败破坏踢权语义）读码确认属实；Y4（SpEL 每请求 parse）用 `javap` 核对 spring-expression 6.1.6 **确认 `SpelExpressionParser.parseExpression(String)` 无内部缓存**（只有正则 `patternCache`），建议成立。② **修复**：新增 `common.RoleGrantPolicy`（空间接口只能授 WS/SELF + 租户归属）→ `MemberServiceImpl.assertRoleGrantable`（授 super_admin/org_admin → `1003`）；新增 `workspace.support.TenantGuard`（跨租户按 1004 处理）；缓存失效**前移到改库之前**并抽成 `WorkspacePermissionCacheInvalidator` 接口（失败 → `9999` 操作取消，fail-closed）；SpEL `Expression` 缓存 + 显式 `@Order` + `myPermissions` 角色/权限同快照（缓存值 `角色|权限`，兼容旧格式）+ Service 改依赖接口。③ **同源缺口（本轮新发现并已修）**：`UMS UserServiceImpl.create` 连角色存在性都没校验（造孤儿成员），且因其门控是 **`member:create`（ws_admin 也持有）**，非超管可借建号授 `org_admin` → 现收紧为"非超管只能授 WS/SELF、roleId 必须存在"（1/2 → 1003、5 → 200、999 → 1001）。④ **验证**：`scripts/smoke-workspace-permission.ps1` 由 8 项扩到 **17 项断言 ALL PASS**；跨租户实测 `DELETE`/`my-permissions` → **404/1004**、`PUT` → 被第二层先拦 `403/2006`。⑤ **未修并新登记**：**Y5** `role:permissions` 可授平台级权限（org_admin 自升）；**Y6** UMS 用户管理用空间级权限码承载组织级动作（ws_admin 可建号）——两者均需权限模型/跨端契约裁决。⑥ **收尾**：隔离实例已停、`tenant_id=99` 探针空间已删、临时脚本已清、IDEA 三服务未受影响。

- 日期：2026-09-17（code review 轮）
- 内容：**workspace 增量 code review（按 DEVGUIDE §7.4 方法论）** —— 范围 = `356effd`（token 口径收口）+ `15d7504`（空间维度第二层鉴权）+ 上轮遗留项复核。① **上轮三问题全部闭环**：编译 `mvn compile` **EXIT=0**（WS-1）；UMS/workspace 三个签发入口均改走 `AuthTokenIssuer`+`TokenSessionCache`（WS-2）；6 处 Mapper 引用 `common.AuthQuerySql`（WS-3）；`refresh` 沿用旧令牌 `ws_id`（`AuthServiceImpl:167`）已修。② **第二层鉴权复核通过**：`@WorkspacePermission`/`WorkspacePermissionAspect`/`WorkspacePermissionCheckerImpl` 接线正确，缓存空值语义（`""` vs `null`）正确，evict 覆盖 invite/remove/updateRole/deleteWorkspace；PROGRESS 已载 smoke 全绿（W1 invite 200 / W2 403-2006），**实测证明 aspect 确已装配生效**。③ **新发现 4 项 🟡 + 1 项 🟢（均未在册，已补 §6.3）**：成员角色授予无越权约束（可授 super_admin）、空间操作未校验租户归属、权限缓存 evict 失败破坏踢权语义、SpEL 每请求 parse 未缓存；🟢 为 roles/permissions 缓存口径不一致等。④ 未改任何代码，仅登记待办。
- 日期：2026-09-17（三段）
- 内容：**空间维度授权（第二层鉴权）轻量版落地**（兑现 §6.3 第①步 / TD §7.5 的 A 层）—— ① **目标**：满足"同一用户在不同空间权限不同"（在 A 能建、在 B 不能建），且**不污染 token 口径**（token 只承载用户级能力）；② **实现**：`common.@WorkspacePermission`（value + `workspaceIdExpr` SpEL）→ `starter-security.WorkspacePermissionChecker`（SPI）+ `WorkspacePermissionAspect`（失败 `403/2006`，`@ConditionalOnBean` 装配）→ workspace `WorkspacePermissionCheckerImpl`（查 `ie_member→ie_role→ie_role_permission→ie_permission`，缓存 `ie:ws:user-perm:{wsId}:{userId}` 10min，成员变更主动失效）；根 POM 开 `-parameters`（SpEL 按参数名取值的前提）；③ **校验点**：`member/page|invite`、`workspace/{id}` 更新用注解；`member/{id}` 移除/改角色在服务内显式判定（目标空间需按 memberId 反查）；**`ws:create`/`ws:delete` 属组织级，明确不做空间判定**；④ **新接口** `GET /api/v1/workspace/{id}/my-permissions`（IF §5.7）——前端按钮门控改用它，解决"按钮在、点了 403"；⑤ **证据**：`scripts/smoke-workspace-permission.ps1`（新增、全 ASCII、口令参数化）**ALL PASS** —— 同一 token（perms=27 含 `member:create`）：W1（ws_admin）invite→**200**、W2（end_user）invite→**403/2006**；`my-permissions` W1=27 / W2=7；⑥ **编译**：中途踩到两处低级错误（块注释里写了 `*/` 提前结束注释；漏 import/静态导入），修复后全量 BUILD SUCCESS；⑦ **收尾**：隔离实例已停、测试空间与测试用户已从云库删除、临时脚本已清、IDEA 三服务未受影响；⑧ **文档**：IF §3.0/§5.6/§5.7、TD §6.1 键表/§7.5 进度表、PROGRESS §二/§三/§6.3/§八、FE-SYNC §2。**遗留**：第②步行级 DataScope 拦截器（防漏兜底）仍待做
- 日期：2026-09-17（续）
- 内容：**口径收口三件套落地（治本层）+ 修掉 refresh 的 `ws_id` 重置 + 新增跨入口一致性断言脚本** —— ① **用户决策**："一起解决"（选项上：`roles/perms` 统一为**全局口径**＝方案 B；refresh 的 `ws_id` 统一为**沿用**＝方案 A）；② **实现收口**：新增 `common.AuthQuerySql`（SQL 唯一字面量）、`starter-security.AuthTokenIssuer`+`IssuedTokens`（唯一签发入口）、`starter-redis.TokenSessionCache`（会话唯一读写入口），UMS 登录/刷新/登出/改密/禁用 与 workspace 切换空间**全部改走这三个收口点**；`JwtUtil.createRefreshToken(userId, jti, wsId)` + `JwtRefreshPayload.workspaceId`（refresh 携带空间）；删除 `AuthConstants` 三个已无引用的键常量；③ **修掉第 4 个不一致**：`/auth/refresh` 不再把 `ws_id` 重置回默认空间（`AuthServiceImpl` 改传旧令牌的 `ws_id`）；④ **新增断言**：`scripts/smoke-auth-claims.ps1`（全 ASCII、口令参数化、可按 `-UmsUrl/-WsUrl` 指向直连或网关），覆盖**登录 / 刷新 / 切换 / 切换后刷新 / 删除保护 / 旧令牌失效** 六组共 10 项断言；⑤ **证据**：隔离实例（UMS `:17101` + workspace `:17102`，`register-enabled=false`，**不打断 IDEA 中运行的实例**）→ `RESULT: ALL PASS`（含"切换后刷新 `ws_id` 仍为目标空间"这条本轮修复的关键断言）；⑥ **文档**：`IF §3.0 新增「token 载荷口径表」`（三个签发入口 × 各字段口径 + 三条硬约束）、`IF §3.2` 补刷新语义、`PROGRESS §三/§6.1/§6.3`、`FE-SYNC §2`；⑦ **收尾**：隔离实例已停、测试空间已硬删（云端仅剩种子空间与前端自测残留）、服务端口 7000/7101/7102 仍为用户 IDEA 实例。**下一步：workspace 走 PR 合并 + 前端切真复测**
- 日期：2026-09-17
- 内容：**修复「切换空间权限口径」缺陷（答复 BE-20260916-01）+ 讲清两层鉴权原理 + 沉淀学习笔记** —— ① **问题定性**：切换空间时把 `roles`/`perms` 按目标空间重算 → 丢掉组织级/平台级能力（`org:*`/`ws:create`/`ws:delete`），连锁产生"按钮消失但菜单在"（两套口径打架）、`IF §5.3` 的 `1003` 被 `2006` 抢先拦截、`org:write` 可见范围缩水；② **根因**：想当然套用"多租户 token 只带当前租户权限"，未核对项目模型（`ie_member.workspace_id` 可空 = 组织级成员）、未复用 UMS 既有登录口径（另写了 `...ByUserAndWorkspace`），并把错方向写进 `IF §5.5`；③ **修复**：`RoleMapper` 删掉两个按空间过滤的查询、改用与 UMS 同口径的 `selectRoleCodesByUserId`/`selectPermissionCodesByUserId`；`switchWorkspace` 只改 `ws_id`；④ **证据**（workspace 独立实例 `:17102`，**不打断 IDEA 中运行的服务**）：登录 `perms=48` → 切换 `perms=48`、`ws:delete`/`org:write`/`ws:create` 保留，**断言 perms 前后一致 = True**；`DELETE /workspace/{当前}` → **403/1003**；⑤ **排查副产品**：发现 IDEA 实例与 `java -jar` 实例会抢同一端口（我的 jar 起不来、冒烟打到 IDEA 旧代码 → 断言假失败），**教训：验证前先确认端口上是谁的进程**（`-javaagent`/`TieredStopAtLevel=1` = IDEA）；⑥ **沉淀**：`LEARNING.md` 新增「接口能力层 vs 资源/空间关系层」两层鉴权原理篇（含 `@PreAuthorize` 执行链、`@workspacePermission`/DataScope 两种空间维度判定、权限为何不每次查库、本项目落地路线）；⑦ **遗留**：空间维度授权立为 `§6.3` 待办（轻量 `@workspacePermission` + 前端「当前空间权限」接口 → 彻底 DataScope + perms 移出 token）；⑧ 测试数据已清理（仅保留种子空间与前端自测残留 `fe-*`）
- 日期：2026-09-16
- 内容：**配置方案定稿：应用侧改「写死 + 口令自动加载」，启动不再需要任何 IDE 配置**（同日第三段，响应"不要绕、直接写死"）—— ① **改动**：三服务 `application.yml` 的 PG/Redis/Nacos 地址、库名、账号**直接写死并备注**；口令只放各服务 `application-local.yml`（gitignore），由 `spring.config.import: optional:classpath:application-local.yml` 自动加载；`application.yml` **故意不写 `password` 键**（导入优先级更低会被覆盖）；生产用 `SPRING_DATASOURCE_PASSWORD` / `SPRING_DATA_REDIS_PASSWORD` 覆盖；② **删除**：`.run/*.run.xml`（共享运行配置，profile 方案已废弃）、gateway 的 `application-local(.example).yml`（网关无口令、地址已写死）；③ **文档同步**：TD §18.2.6 重写、DEVGUIDE P19 第 1 条改口径、`.env.example` 应用侧段改为「已不再需要 `INSIGHT_PG_*`/`NACOS_ADDR`」、PROGRESS §三/§四/§五 P1-1 更正；④ **实测**：三服务**零参数**启动全部 `Started ...Application`，经网关 `:7000` 登录 code=0、`workspace/page`/`org/1`/`member/page` 均 200；⑤ **收尾**：三服务已停（端口空）、临时文件已清。**下一步：`feature/workspace` 走 PR 合并 + 前端切真复测（§七 Top1）**
- 日期：2026-09-16
- 内容：**IDEA 启动三连坑排查 + 安全组收敛定位 + Nacos 注入修复 + `lb://` 双服务复验**（同一日第二段）—— ① **云库连不上**：定位为**本机公网 IP 变化**（`123.138.150.18` → `123.139.53.41`，联通动态 IP；云主机 `auth.log` 中 SSH 来源 IP 仅此两个可反证），而安全组把 5433/6380/8848/9848 的来源写死为旧 IP；云主机侧全绿（ufw inactive / INPUT ACCEPT / 端口正常监听 / 容器 Up）→ 拦截在**安全组层**。用户已在控制台改为当前 IP，**复测四端口全通**；② **IDEA 启动失败（`Failed to bind properties under 'spring.data.redis.port'`）**：根因=**未激活 `local` profile**（占位符无属性源；`redis.host` 是 String 会静默留字面量，只有 `port` 是 int 才在绑定期炸，报错位置具误导性）；③ **gateway 启动报 `ERR_NACOS_DEREGISTER / Client not connected, current status:STARTING`**：报错在 **shutdown 线程**=症状，根因是 `NACOS_ADDR` 未注入 → `${NACOS_ADDR:127.0.0.1:8848}` **有默认值故静默降级**；且发现 **UMS/workspace 的 `application-local.yml` 只覆盖 DB/Redis、Nacos 段漏配**（故它们 local 模式同样刷 Nacos 报错）；④ **修复**：三服务补全 Nacos 地址（gateway 新建 local 模板+本地文件）、共享运行配置入库 `insight-engine/.run/`（三服务带 `local` profile，**只写 profile 名，不含口令与 IP**）、gateway `register-enabled:false`（P2-1）；⑤ **复验证据**：gateway 以 local 启动 1.84s、`127.0.0.1`/`Server check fail`/`ERR_NACOS_DEREGISTER` 报错**均为 0 条**；云端 Nacos 名单 `insight-engine-ums(172.20.160.1:7101 healthy)` / `insight-engine-workspace(…7102 healthy)`、**gateway 不在名单**；经网关 `:7000` 走 `lb://` → login code=0、`/api/v1/org/1` 200、`/api/v1/workspace/page` 200、`/api/v1/user/page` 200、`/api/v1/kb/x` 404；**验证后三服务已停、端口释放**；⑥ **新登记**：本机 `mvn package` 的 jar 内含 `application-local.yml`（真实凭据）→ 属泄露路径（未泄露），修法入 §6.4；P0-1 注册 IP 落虚拟网卡仍存（本次为 `172.20.160.1`）；⑦ **提交**：`6490cf5`（.run + TD 更正）、`5b470c1`（jar 凭据路径）、本轮（P1-1/P2-1 结案 + lb:// 复验 + 三连坑）均已推送 `feature/workspace`——**push 偶发 `Connection was reset` 属直连抖动，重试即过**（已附自动重试写法）。**下一步：`feature/workspace` 走 PR 合并 + 前端切真复测（§七 Top1）**
- 日期：2026-09-16
- 内容：**阶段 5 workspace 模块实现 + 实机冒烟 28/28 + 公共层沉淀（兑现 BE-20260908-02）**—— ① **开工四必读**后确认任务=workspace 模块（PROGRESS §七 Top2 / FE-SYNC §1 / 前端 BE-ISSUES 2 条未闭环 / IF §5）；② **模块实现**：`insight-engine-workspace`（7102）11 端点（组织 2 / 空间 5 / 成员 4）+ 切换空间换签；网关按 P18 新增 `/api/v1/org|workspace|member/**` → `lb://insight-engine-workspace`（同批更新 TD §8.3，白名单/CORS 无需变更）；③ **公共层沉淀**：新增 `common.constant.CacheKeyConstants`（`ie:auth:*` 单一口径），`TokenDigestUtil` + 登录态/黑名单 Redis 实现下沉 `starter-security`/`starter-redis` 并自动装配，删除 UMS 内重复实现（跨服务会话一致，避免键漂移静默失效）；④ **UMS 修正**：`/auth/me` 的 `workspaceId` 改取 JWT `ws_id`（切换空间后 `ensureMe()` 才能反映新空间）；⑤ **环境侧**：本机到云 5433/6380/8848 被安全组拦截（22 可达）→ **SSH 隧道 + 命令行覆盖**完成冒烟（不改受版本控制配置，验毕即停）；**云端库补执行角色授权增量 seed**（`ie_role_permission` 1:48/2:46/3:27/4:15/5:7=143，此前 role 2/3/5 缺失，是「ws_admin 权限为空」的根因）；⑥ **证据**：`mvn -DskipTests package` 全量 **BUILD SUCCESS**；**workspace 冒烟 28/28**（创建/更新/分页/成员 CRUD/切换换签/旧 token 401-2001/`/auth/me` 新空间/删除保护 1003/删后 404/校验 1001，错误态 traceId 已留档 FE-SYNC §1）+ **经网关复验**三条前缀路由 200、未接入前缀 404、无 token 401-2001；⑦ **冒烟数据已清理**（本会话新建的 smoke 用户/空间及成员关系已删，库内仅保留种子与既有数据）；⑧ **提交**：`d446ca7`（分支 `feature/workspace`，53 files：workspace 模块 + 公共层下沉 + UMS 修正 + 网关路由 + 6 份文档）——**按 GitHub Flow 仅提交本对话产物，另一场对话在 `docs/LEARNING.md` 的未提交改动已刻意排除**；⑨ **未完成**：推送 + PR 合入、Nacos `lb://` 实机复验（受安全组限制）、audit（属 obs）仍 mock；⑩ **环境提示**：冒烟用服务与 SSH 隧道均已停止（7000/7101/7102/5433/6380 无监听，可随时由 IDEA 手动启动）；`d:/CodexProject/.ssh_run.py` 含明文 root 口令（已 gitignore，建议用后删除）。**下一步：推送 `feature/workspace` 走 PR + 前端接口复测（§七 Top1）**
- 日期：2026-09-16
- 内容：阶段 4→5 交接体检 + 文档完整度收口（收尾三件套）—— ① **体检结论**：阶段 1-4 已完成并入 master（`f84ffd2`）、整体 ~43%；gateway Nacos 接入闭环（实例 UP + `lb://` 冒烟 8/8）；云端 PG/Redis/Nacos healthy、磁盘/swap 已加固；下一步 workspace（阶段 5，兑现 BE-20260908-02）。② **文档完整度修正**：4 处「Nacos 接入后改 lb://」过期前瞻改为现状（`TD §8.3` 注 / `ARCHITECTURE` 路由规则 / `PROGRESS §七 Top2` / `gateway/pom.xml` 注释）；`FE-SYNC` §1 gateway 证据列补 `lb://` 冒烟 8/8（契约无变化，§2 不加条目）。③ **发现并登记**：另一场对话的「Nacos 客户端负载均衡」实测漏洞清单（P0-1 注册 IP 落虚拟网卡 / P0-2 公网 Nacos 未鉴权可投毒 / P1-1 `NACOS_ADDR` 注入路径 / P1-2 compose 漂移 / P2-1 gateway 自注册）原先只落在 `LEARNING.md`，已登记进 **§五「云上 Nacos 安全加固」**（保证开工四必读可见）；`LEARNING` §八 P2-2/P2-3 标记已修。④ **提交**：`fa9c8c6`（LEARNING 笔记/口径）+ `6af1c75`（Nacos 接入 14 files）已推送 `feature/docs-sync-security`（远程 = `6af1c75`）。⑤ **编译证据**：`mvn -DskipTests -pl :insight-engine-gateway -am package` → **BUILD SUCCESS**（5.5s，2026-09-16）；本轮冒烟复用 2026-09-09 `lb://` 8/8（无新代码逻辑）。⑥ **未决**：`LEARNING.md` 存在另一并行对话的 4 行未提交改动（「Nacos 配置逐行拆解」+ P1-1 结案），保留其归属待其自行收尾；§五 口令轮换与 P0-1/P0-2 加固仍为部署前必办。**下一步：新开对话进 workspace 模块（阶段 5）**
- 日期：2026-09-09
- 内容：Nacos 注册实机验证通过（P2-2/P2-3 闭环，2026-09-09 安全组放行后）—— ① 前置：用户在阿里云控制台放行安全组 8848/9848（连通性复测 True）；② 本机以 `-DNACOS_ADDR=39.106.110.214:8848` 启 UMS（7101，profile local 连云库）+ gateway（7000），日志确认 `insight-engine-ums 172.18.128.1:7101` / `insight-engine-gateway 172.18.128.1:7000` register finished，Nacos API 实例列表 healthy（服务目录 count=2）；③ **冒烟 8/8（经 gateway `lb://` 到 UMS）**：login/me/user/page/role/list 200（code=0）、无 token 401-2001、坏 token 401-2001、错口令 401-2002、`/doc.html` 200；④ 注册实例 IP 为本机网卡 172.18.128.1（两服务同机互达即符合预期；跨机/容器部署需配注册为可达 IP，见 LEARNING）；⑤ 附带发现 🟡：`/api/v1/role/page` 这类非数字段会落入 `role/{id}` 路径参数 Long 解析抛 500/9999（正确列表端点为 role/list，正常），记 §6.1；⑥ 回填 PROGRESS §一/§6.4/§七/§八、FE-SYNC gateway 行。**下一步：workspace 模块（§七 Top2）**
- 日期：2026-09-09
- 内容：云上 Nacos 部署 + UMS/gateway Nacos 接入（P2）+ 文档端口口径订正 —— ① **云端现状复核**：PG/Redis 已迁云；P1 运维加固此前已完成（磁盘 vda3 已扩至 49.8G / 40G 可用；swap 2G 生效、swappiness=0）；② **P2-1 Nacos 部署**：`insight-nacos`（2.3.2 standalone）healthy；宿主端口由 8850/9850 偏移改 **1:1 8848/9848**（Nacos 2.x 自报 ip:port、客户端据此推 gRPC +1000，偏移必失败），`NACOS_SERVER_IP` 强制自报公网 IP 已在 `cluster/nodes` 确认；③ **P2-2 代码侧**：ums/gateway 接入 starter-nacos + loadbalancer + `fail-fast:false`，gateway 路由改 `lb://insight-engine-ums`，全量编译 BUILD SUCCESS；④ **阻塞**：本机 → 云 8848/9848 被阿里云安全组入方向拦截（主机侧 ufw/firewalld inactive、iptables ACCEPT、docker-proxy 监听正常，22/5433/6380 可达）→ 需控制台放行后验证注册与 `lb://` 转发（P2-3 冒烟）；⑤ **订正**：TD §18.2.3 / PRD / ARCHITECTURE / LEARNING 端口口径统一 1:1；云端供应商更正为**阿里云 ECS**（此前误记腾讯云轻量）；⑥ 未做：口令轮换（§五 a）、RabbitMQ/MinIO/Prom/Grafana 迁云、workspace 模块（阶段 5）。**下一步：安全组放行后复验 Nacos（§七 Top3）或启动 workspace（§七 Top2）**
- 日期：2026-09-09
- 内容：凭据占位化落地 + 本轮收尾（§五 b / §6.4 收口，第三批）——① `docker-compose.yml` 6 处口令改 `${VAR:?必填提示}`（PG / Redis `--requirepass` + healthcheck `-a` / RabbitMQ / MinIO / Grafana）；② `ums/application.yml` 4 处改 `${INSIGHT_PG_*}` / `${INSIGHT_REDIS_*}`——**不给默认值**，未注入即 fail-fast（避免「忘配 → 静默用空口令连库」）；③ 新增 `.env.example`（入库模板：变量名 + 用途 + 两套注入路径指引）+ `.env`（真实值，已被 gitignore）；④ 新增 `application-local.example.yml`（入库模板）+ `application-local.yml`（真实值，已被 gitignore）——**关键事实：Spring Boot 不会自动读 `.env`**，应用侧只能走 profile 覆盖 / 环境变量，已写入 TD **§18.2.6**；⑤ `.gitignore` 补 `!.env.example`（原 `.env.*` 会把模板一起忽略，模板提不上去）；⑥ 同步 DEVGUIDE **P19**、TD §18.2.4、DB §1.1、PROGRESS §一/§五/§6.4/§七；⑦ 自检：通用凭据模式 `git grep -niE "(password|passwd|secret|token|apikey)" -- ':!*.md' ':!*.example'` 与公网 IP 扫描均**无输出**；`git check-ignore` 确认 `.env` / `application-local.yml` 被忽略、两个 `.example` 模板**不被忽略**。**未做：口令轮换（§五 a）——占位化只防「以后泄露」，不能回收已泄露历史。下一步：workspace 模块（阶段 5）。**
- 日期：2026-09-09
- 内容：凭据纪律硬约束落地（补规范缺口，回应用户「为什么会被提交上去 / 提示词里写了没有」）——① **根因复盘**：明文能进 master **不是 `.gitignore` 漏规则**（`.env` / `.env.*` / `application-local.yml` / `*.secret` 早已就位），而是**事前硬约束从未存在**——P2 第 6 条只约束「不改配置项」、P5 第 4 项属事后 Review、P17 反而要求「与含明文的 compose 逐项对齐」，`AGENTS.md` 至 2026-09-09 才建立且无凭据条款；而 `docker-compose.yml` / `ums/application.yml` 本身必须入库，明文写死在文件里就必然被提交；② **落地**：`AGENTS.md` 新增**铁律 5：凭据纪律**（禁止明文入库 / 占位符 + `.env` 注入 / 新增配置项三件套 / 提交前 `git grep` 自检 / 历史泄露按已泄露处理）；`DEVGUIDE.md` 附录 B 新增 **P19 场景约束：配置与凭据**；③ **修正 P17**：对齐范围明确为「结构（镜像 tag / 容器名 / 端口 / 命名卷 / 环境变量名 / 持久化参数）」，凭据值一律占位符、禁止抄明文；④ **文档口径统一**：附录 B 标题 / 索引 / 正文引用同步 `P1~P19`（含 AGENTS 铁律 3 的 `P1~P18` → `P1~P19`）、DEVGUIDE 铁律 3 真相源份数与表格对齐附录 A（7 份 → 11 份，补 `ARCHITECTURE.md` / `LEARNING.md`）；⑤ **同步** §一 当前任务 / 完成度、§三 决策、§四 踩坑、§五 e) 规范补缺、§6.4 待办池；⑥ **边界**：本次只补规范，**未改代码配置**——`docker-compose.yml` / `ums/application.yml` 仍为明文（§五 b），**轮换止血（a）仍未做**，§五 红级不闭环
- 日期：2026-09-09
- 内容：云凭据安全收口（局部）+ PROGRESS §五 闸门结论修正 —— ① 停掉本机残留 gateway(7000)/UMS(7101) 进程，解除 IDEA 端口占用（保留 IDEA 本体与 mysqld）；② 文档清明文：`TD.md:1110,1127`、`DEVGUIDE.md:1120,1122`、`DB.md:48`、`LEARNING.md:4706` 一律改 `.env` 占位引用（文档不再出现口令本体）；③ **§五 闸门结论修正**：PR #5（`f84ffd2`）已合入 master，`d437202` 已是 master 祖先（`git merge-base --is-ancestor` 返回 0）→ 原判「master 是最后一道闸门」**失效**，明文已进主干历史、改文件无法回收，**止血只能靠口令轮换**（当前学习开发期暂缓，进入联调/部署前必须完成）；④ `.ssh_run.py`/`.remote_cmd.sh` 加 `.gitignore` 防误提交；⑤ 同步修正 §一 当前阶段/任务/完成度、§二 gateway 看板（PR 已合）、§6.4 密码差异化项、§七 Top1/2 中所有「待 PR 合入」过期表述。
- 日期：2026-09-09
- 内容：处理前端工单 BE-20260909-07（SSE 心跳契约不完整 + §12.4 枚举误导）——① **开工四必读**后受理：前端反查 `heartbeat` 全文仅 §10.3 一处，§12.4 括号枚举逐一列举业务事件却漏 `heartbeat`（声称完整最易误导），§7.5/§8.3/§13.6 均未覆盖；② **采纳并收口**：新增 **IF §2.6「SSE 流式通用约定」**（心跳单一事实源：所有 `stream=true` 均含 `heartbeat`，15s/不可关闭/`data={"ts":<epochMillis>}`；端点表只列业务事件；引用 §10.3 同样含 heartbeat），同步修正 §12.4 枚举 + §7.5/§8.3/§10.3/§13.6 引用 §2.6；③ **双写**：前端 `BE-ISSUES.md` 该条回填答复并置 ✅（待前端核对）、`FE-SYNC.md §2 契约变更 + §3 答复`登记；④ **边界声明**：conv/agent/model 流式端点尚未实现（FE-SYNC §1 仍 ⚪），本次仅收敛契约，不构成"可联调"；⑤ 待办：commit 走 PR（受 §五 红级阻塞）
- 日期：2026-09-09
- 内容：gateway 收尾复跑冒烟（`X-Trace-Id` 重复修复验证）——① 复跑前先复现：经网关转发 UMS 的响应出现**两个** `X-Trace-Id`（网关 + 上游 starter-web `TraceFilter` 各回一份，被 `NettyRoutingFilter` 合并追加）→ `TraceGlobalFilter` 改为 `beforeCommit` 内 `headers().set()` 覆盖；② 停网关进程释放 jar 锁 → `mvn -pl gateway -am install` 重编译 → 重启，复跑冒烟 **9/9 通过**（登录 200 / 转发 200 / 无 token 401-2001 / 坏 token 401-2001 / 过期 token 401-2007 / `/doc.html` 200 / `sk-` 401-2001 / end_user 403-2006 / 未匹配路由 404），六类路径 `X-Trace-Id` 计数均为 1；③ 新发现 🟡：未匹配路由不进入 GlobalFilter 链 → 404 无 traceId 且为 Spring 默认错误体（记 §6.4，不阻塞联调）；④ 踩坑：PS 5.1 内联 JSON 传 `curl.exe` 双引号被吞致 500，改临时文件 `-d "@file"`；⑤ 待办：commit + push `feature/gateway`；**PR 合入受 §五 云凭据明文红级阻塞**（需云凭据/控制台做口令轮换 + 安全组收敛）
- 日期：2026-09-09
- 内容：环境 / 基础设施 / gateway 三线推进（计划先行）——① **开工四必读**并复述现状（阶段 4 gateway、整体 ~35%、前端 6 条待答复）；② **环境复核**：本机 JDK 21.0.10 / Maven 3.9.9 / Node 24.13.0 正常，`docker` 命令不存在（本机不承载中间件），云服务器无 SSH 凭据 → 云上操作待提供；③ **gateway 待办收口四件**（路由按 §8.3 收窄 + `TraceGlobalFilter`(-200) + 错误响应 `charset=UTF-8` + JWT Claim 常量下沉 common），common/starter-security/gateway 三模块 `mvn install` 编译通过；④ **init.sql 角色 seed 授权补齐**（org_admin 46 / ws_admin 27 / end_user 7，带 `ON CONFLICT DO NOTHING` 可增量重跑）+ DB.md 同步（143）；⑤ **前端 6 条诉求处理**：答复 BE-01（不新增 `ws:switch`）/ BE-05（role 详情示例）/ BE-06（权限编码规范）/ BE-04（SSE `heartbeat`），修复 BE-03（seed），排期 BE-02（workspace），双写 `BE-ISSUES.md` + `FE-SYNC.md §1/§2/§3`；⑥ **待办**：§五 凭据收敛（红级，PR 前置）与云上 Nacos 需云凭据；本轮工作区改动待复跑冒烟后 commit → PR
- 日期：2026-09-09
- 内容：建立前后端跨端同步机制（治理「进展同步 / 问题拉通 gap」）——① 新增后端根 `AGENTS.md`（AI 新会话自动加载：开工四必读含前端 BE-ISSUES、前端诉求必答双写、证据纪律、收尾三件套），后端首次拥有与前端对等的自动加载入口，不再依赖人工贴提示词；② 新增 `docs/FE-SYNC.md`（跨端协议 + BE→FE 就绪投影：§1 模块联调就绪清单带 curl 证据 / §2 契约变更 / §3 对前端答复登记 / §4 一键开场白）；③ 前端新增 `docs/BE-ISSUES.md`（FE→BE 信箱，迁移前端 FE-PROGRESS 散落诉求 6 条：`ws:switch` 权限码 / workspace 交付跟踪 / seed 授权 / SSE 心跳 / role 详情示例 / 权限编码规范），前端 `AGENTS.md` 升为「开工四必读 + 收尾四件套」；④ 修正 §二 前端看板滞后（0% → P0/P1 100%、P2 ~90%，此前误记未开始）。后续：每轮后端对话按 AGENTS 铁律 1 处理 BE-ISSUES 待答复项。
- 日期：2026-09-08
- 内容：全量代码 review（按 DEVGUIDE §7.4 通用方法论，范围=gateway 全量 + UMS-1/UMS-2 修复 + starter 安全/web 变更）——① 复核通过：`SecurityExceptionHandlerAdvice`（403/2006，陷阱库实例 1 已闭环）、`UserContextFilter` 默认关（UMS-1）、refresh jti 轮换/撤销链（UMS-2，logout/改密/禁用均删 refresh key）实现正确，冒烟已覆盖 401/403/轮换/撤销；② 无新代码级 🔴；③ 新发现 🟡（已入 §6.4）：gateway 缺 TraceFilter（TD §8.3 链首）/ JWT Claim 常量双份 / gateway 错误响应无 charset / sk- 通道待 workspace 阶段 / 限流与 CORS 收敛待生产 / refresh 单槽多端误吊销需前端单飞；④ 部署安全 🔴 前置：UMS yml 云凭据明文已进 git（§五 gateway PR 合入前置，改环境变量注入 + 云库强口令 + 安全组收敛后再合）；⑤ 修正：PROGRESS 工作区版本曾被我以过时内容覆盖（丢失 gateway 冒烟/云环境进展），已 `git checkout e4998c5` 恢复 HEAD 版并在此整合。
- 日期：2026-09-08
- 内容：gateway 冒烟收尾 + 乱码根治 + Git 规范纠偏 + 文档更新（收尾轮）——① **冒烟前置 3 阻塞修复**：a) master 缺 `JwtRefreshPayload`（补交 cde4e71 未合 master）→ 用户推送本地 master 至远程（ls-remote 核实远程 master=09cb33f，已含补交）；b) ums/gateway pom 补 `spring-boot-maven-plugin` 的 `repackage` execution（自定义父 pom 无默认绑定，此前只产普通 jar 无法 `java -jar`）→ fat jar 启动正常；c) `@PreAuthorize` 拒绝误报 500 → starter-security 新增 `SecurityExceptionHandlerAdvice`（403/code=2006）；② **中文乱码根治**：实测服务端字节为合法 UTF-8，根因 = PS 5.1 对无 charset 的 JSON 按 ISO-8859-1 解码 → starter-web 给 Jackson 转换器设默认 UTF-8（响应头带 `charset=UTF-8`），PS 实测解码「默认空间」正常；③ **Git 规范纠偏**：本地 merge master + 直接 push 违反 GitHub Flow「master 只接受 PR」被纠正（见 §四），LEARNING.md 新增「Git 常用命令速查」8 场景 + 五条铁律；④ **收尾**：文档归口 feature/gateway（a84a574 复盘批 + fad5940 Git 速查）、stash 清空、`merge origin/master`（fe912aa 无冲突）并推送远程分支（与 master 一致）；⑤ 本机服务全停（后续 IDEA 界面启动）；6.4 未覆盖冒烟项（doc.html 白名单 / sk- 分流 / 2007 过期 token）随路由收窄 + Nacos 轮补。
- 日期：2026-09-08
- 内容：gateway 全链路实机冒烟（UMS 7101 + gateway 7000 本机双服务，云 PG/Redis）——① **UMS 配置切云**：`application.yml` datasource/redis 指向腾讯云服务器（39.106.110.214），两处改动生效；② **修 3 个阻塞项**：a) `feature/gateway` 缺 `JwtRefreshPayload.java`（PR #3 补交 `cde4e71` 从未合入 master，master 系分支编译必挂）→ cherry-pick 到本分支，**master 仍缺该文件，需在 GitHub 把 PR #3 真正合入**；b) ums/gateway pom 的 `spring-boot-maven-plugin` 缺 `repackage` execution 绑定（自定义父 pom 不带 starter-parent 默认绑定），`package` 只产普通 jar 无法 `java -jar` → 两模块补 `repackage` 后 fat jar 正常；c) 冒烟抓到 **`@PreAuthorize` 拒绝误报 500**：`AccessDeniedException` 在 DispatcherServlet 层被 `GlobalExceptionHandler` 的 Exception 兜底捕获，到不了过滤器链的 `RestAccessDeniedHandler`（其注释声称覆盖该方法级场景但实际失效）→ starter-security 新增 `SecurityExceptionHandlerAdvice`（`@Order(HIGHEST_PRECEDENCE)` 的 `@RestControllerAdvice`，403 + code=2006）并在 `SecurityAutoConfiguration` 注册；③ **冒烟 8 项全过**：注册 400 业务码 / 登录(经网关) 200 含 jti refresh / me 200 / 无 token 401 / 坏 token 401 / 权限不足 403(修复后) / refresh 轮换出新 jti / logout 后旧 token 401（撤销生效，且第 7 步 refresh 已顶掉旧 token 亦为轮换预期）；④ 疑点：响应 `workspaceName`「默认空间」在 PS 客户端显示乱码，疑似 DB 种子数据编码，待查；冒烟脚本留存 `smoke-test.ps1`（临时）。
- 日期：2026-09-08
- 内容：文档复盘 + 同步修复批（Review 只读轮产出 7 项建议后的执行轮）——① **git 代理修复**：全局代理指向 Clash 7890 但内核未启动，摘除全局代理走直连（github 直连实测 200），`git ls-remote` 验证通过（见 §四）；② **前端基线对齐**：TD §2.2 + DEVGUIDE §8.1/§8.4/D1/P7/P13 统一为前端仓库 FEGUIDE v1.0 口径（独立仓库、控制台+对话门户二分、契约驱动+Mock 先行、技术栈基线升级）；③ DEVGUIDE 附录 A/D1 补 DB.md 与 FEATURES.md 真相源条目（铁律 3 改 7 份）；④ TD §6.1 补 `ie:auth:lock` / `ie:auth:refresh` 两个已上线键；⑤ TD §8.3 路由收窄纪律 + DEVGUIDE 新增 P18；⑥ **复盘发现（待开发者确认）**：前端仓库 `insight-engine-admin` 已实质完成 P0 脚手架（src 13 视图域 / tokens.css / request.ts / sse.ts 齐全、dev 7200 正在监听、存在 dist 构建产物）但 FE-PROGRESS 仍记「P0 未开始 0%」且整个工程未 git 提交——前端真相源滞后于实况，建议在前端仓库走 FE-P4 收口（质量自查 + 更新 FE-PROGRESS + commit）；另 FEGUIDE §3.1 基线与实装版本有漂移（实装 pinia 4 / router 5 / vueuse 14 / fetch-event-source 2.0.1，FEGUIDE 写 3/4/13/3.x），TD §2.2 已按「FEGUIDE 为唯一事实源」指针处理不重复维护
- 日期：2026-09-06
- 内容：前端（管理端）启动规划，本次仅产出文档、未建工程代码——① 勘察后端现状与 IF 契约（UMS 16 端点就绪、IF 约 99 端点、PRD §11 信息架构），产出前端开发指导手册 FEGUIDE v1.0 与前端进度真相源 FE-PROGRESS，存放于独立前端仓库 `D:\JavaProject\insight-engine-web\docs\`（与后端仓库分离；接口契约仍以本仓库 IF.md 为唯一事实源）；② 定位裁决：按 B2B SaaS 惯例二分为「控制台（管理+开发一体，角色收缩菜单）+ 对话门户（独立 SPA 后置）」，开发者门户折叠进控制台「API 与集成」，PRD 不改动；③ 技术选型定案：Vue3.5 + Vite7 + TS5.9 + Arco + Pinia3 + Tailwind4 + MSW2 + fetch-event-source（相对 TD §2.2 基线升级，TD 同步待开发者确认）；④ 推进策略：契约驱动 + Mock 先行（MSW），P1/P2 直接真联调 UMS，其余模块后端就绪即切；任务卡 P0~P13 与 Prompt 库（FE-P0~P8）见 FEGUIDE §4/§5。
- 日期：2026-09-06
- 内容：gateway 联调环境迁移至云服务器（腾讯云轻量 39.106.110.214 / Ubuntu 24.04）——① 本机 Docker Desktop 因虚拟化未启用无法启动，改由云服务器承载中间件；② 云上 Docker 拉镜像 i/o timeout → daemon 配 DaoCloud + 腾讯云内网镜像加速器；③ 排障中发现手工 `docker run` 命令与 docker-compose.yml 不一致（restart 策略、数据卷、Redis appendonly、容器名），确认 Docker 不支持对已存在容器补挂卷 → 给出「逻辑备份 → 删旧 → 按 compose 对齐重建（pg_data/redis_data 卷 + appendonly yes + unless-stopped）→ 重启持久化验证」命令集，验收结果待回报；④ 教训已固化：DEVGUIDE 增 P17（容器命令与 compose 对齐）、本文件 §三/§四 留档。注：gateway 全部代码已于 2026-09-04 由开发者提交（f45a226）。
- 日期：2026-09-03
- 内容：启动 gateway 阶段（阶段 4，分支 `feature/gateway`）——① 勘察确认 starter-security 为 Servlet 栈（HttpSecurity），gateway（WebFlux）不可引入，网关侧 JWT 校验自建（jjwt + `insight.gateway.jwt-secret`，密钥与 UMS 同源同 env 注入）；② 完成模块骨架：POM 补 jjwt + spring-boot 插件、`GatewayApplication`、`application.yml`（端口 7000、UMS 路由直连 localhost:7101、globalcors 放行）、`mvn install` 编译通过；③ 待办：AuthGlobalFilter（JWT 校验/头注入/API Key 分流）→ Nacos 接入改 `lb://` → 全链路冒烟需先起 PG/Redis/UMS（7101 当前未运行）。
- 日期：2026-09-03
- 内容：UMS 阶段正式收口 + 启动 gateway 阶段——① 确认 UMS-1/UMS-2 安全收尾已实机回归验证并合入 master（PR #2，工作区干净与远程同步）；② 核对 §6.1 高价值收尾 5 项代码现状（`uk_user_phone` 唯一索引、`UserServiceImpl.create` 前置 roleId 校验、`RoleServiceImpl` 授权去重/校验、`GlobalExceptionHandler` 补 `DuplicateKeyException`/`HttpMessageNotReadableException`、`Result` 成功响应 traceId 回填）均未落地，继续保留待办池并标注「高价值优先组」；③ PROGRESS 更新：UMS 看板 ✅ 100%、gateway 看板 🔵 启动、§五 阻塞清空、§三 追加 2026-09-03 办结留档与「认证模型定案」决策（ADR-5 网关校验+下发头、服务端开关双轨共存、默认自校验）；④ §七 Top3 重排：1 gateway 网关（骨架/路由/AuthGlobalFilter/Cors）→ 2 Nacos 注册接入 → 3 §6.1 高价值收尾独立任务。
- 日期：2026-09-02
- 内容：UMS 安全收尾修复（分支 `feature/ums-security-fix`，UMS-1/UMS-2）——UMS-1 双身份源（方案 A）：`UserContextFilter` 改条件装配默认关闭（`insight.web.trust-gateway-headers=true` 才注册），身份只信 JWT，`UserContext` 清理职责移交 `JwtAuthFilter` finally；UMS-2 refresh 撤销/轮换：refresh token 增 `jti`，新增 refresh 会话 key `ie:auth:refresh:{userId}`（jti 摘要 TTL=7d），`refresh()` 校验匹配后一次性轮换、旧 jti 重放视为泄露吊销全会话，`logout`/改密/禁用连删 refresh 会话 key；新建 `JwtRefreshPayload`、`JwtUtil` 增 `createRefreshToken(userId,jti)`/`getRefreshTtlSeconds()`；编译通过。待回归验证合入 master。
- 日期：2026-09-02
- 内容：规范整理 `docs/PROGRESS.md`（本次）——① 口径调整：§五只放「未解决的必须修项」= 当前阻塞（现 2 项：UMS-1 双身份源并存、UMS-2 refresh token 无轮换/撤销）；② 建议修/可选优化统一收进 §六「后续待办与优化池」，按处理阶段（UMS 收尾 / 公共层 / 后续模块 / gateway部署 / 长期）分组；③ 已办结红级（token 失效、JWT 密钥 fail-fast、种子序列 setval 等）移出阻塞节，留档到 §三 决策 / §四 踩坑；④ 全文清除 "Review 🔴/🟡" 类 review 输出标题；⑤ git 状态确认：master 与 origin/master 同步，PR #1 已合入，工作区仅 docs 3 个文件未提交。
- 日期：2026-09-02
- 内容：修复 UMS 红级①「禁用/改密后 token 不失效」——采用方案 A（单会话 + 单 key 摘要，符合 TD §6.1）：starter-security 新增可选 `TokenSessionService`；`JwtAuthFilter` 签名校验后、建立认证前校验登录态；`SecurityAutoConfiguration` ObjectProvider 可选装配；UMS 新增 `RedisTokenSessionService` 与 `TokenDigestUtil`；`cacheToken` 改存 SHA-256 摘要。语义注意：单会话语义，重新登录/刷新会顶掉旧 token（多设备互踢）。未处理：🔴 双身份源（UMS-1，现列入 §五）。
- 日期：2026-09-02
- 内容：修复 UMS 红级②「JWT 密钥硬编码且可预测」——`SecurityProperties.jwtSecret` 删除代码内默认值；`jwtUtil` Bean fail-fast（空/不足 32 字节拒绝启动；prod+`change-me` 拒绝启动）；`application.yml` 密钥改 `${INSIGHT_SECURITY_JWT_SECRET:本地开发默认值}`。部署纪律：生产必须注入独立随机密钥并启用 prod profile。
- 日期：2026-09-02
- 内容：Git 首次对接 GitHub 全流程实战走通（`github.com/Goutouxiaoen/insight-engine-backend`）——配 remote、代理 443、push master/feature、PR #1 合并 feature/ums-auth → master、改默认分支为 master 并删 main、本地 pull 同步；沉淀「Git 实操全流程复盘」到 `docs/LEARNING.md`。
- 日期：2026-09-02
- 内容：学习沉淀「ThreadLocal 线程隔离与 remove 防串号」到 `docs/LEARNING.md`（原理 / 必须 remove / finally 清理 / 串号踩坑），串联 `UserContext.HOLDER` / `TraceFilter`+MDC / `SecurityContextHolder` 三处 ThreadLocal。
- 日期：2026-08-26
- 内容：阶段 3 UMS 认证服务（第一个完整微服务）——三个 starter 骨架（mybatis/redis/security）+ 认证 5 / 用户 5 / 角色权限 6 接口 + JSR-303 + @PreAuthorize 全落地；实机冒烟：登录 48 权限 JWT、/auth/me、角色列表、权限树 27 组、401 拦截、Knife4j 文档页 200；产出 FEATURES.md。
- 日期：2026-08-26
- 内容：UMS 服务完整代码 review（对照 TD/IF）——产出待修分级清单（3 🔴 / 8 🟡 / 6 🟢）。核心三红：① 禁用/改密后 token 不失效；② JWT 密钥硬编码默认值可预测；③ UserContextFilter 信任明文头与 JwtAuthFilter 双身份源并存。① ② 已修复（见上），③ 列为 §五 UMS-1 阻塞。
