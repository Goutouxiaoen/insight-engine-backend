# 项目进度追踪（PROGRESS）

> 本文件是「开发者」与「AI」之间的**共享进度真相源**，是跨对话记忆的唯一可靠载体。
> **铁律：每次对话结束前必须更新本文件；每次新对话开始必须先读本文件。**
>
> **文档口径（2026-09-02 起执行）**：
> - 「§五 当前阻塞 / 待解决问题」= **必须修且尚未解决**的项，阻塞对应交付收口；已办结项不在此滞留。
> - 「§六 后续待办」= **建议修（🟡）/ 可选优化（🟢）**，属后续要完成的任务池，按处理阶段归类。
> - 文档只写「项目任务视角」的进展与待办，不再粘贴 review 输出格式标题。

---

## 一、总体状态

| 项     | 值                                  |
| ----- | ---------------------------------- |
| 当前阶段  | 阶段 4：gateway 网关 —— 全链路冒烟 8/8 通过，修复+文档已提交并推送 feature/gateway（fe912aa，与 master 同步），待 PR 合入 master 后收口 |
| 当前里程碑 | M4：gateway 网关                       |
| 当前任务  | gateway 阶段收口：`feature/gateway`（fe912aa，含冒烟修复与文档）PR 合入 master → 路由按 TD §8.3 细分（workspace 等服务接入前置，禁 `/api/v1/**` 全量 fallback）→ 接入 Nacos 路由改 `lb://`（§六 6.4 / §七 Top1/2） |
| 整体完成度 | 约 35%（阶段 1-3 完成并已全部合入 master；gateway 代码 + 全链路冒烟 8/8 通过、3 修复与文档已提交分支待 PR 合入；UMS 收尾项在 §6.1 待办池；联调环境由本机 Docker 迁移至云服务器推进中） |

---

## 二、模块进度看板

> 状态图例：✅ 完成 / 🔵 进行中 / ⚪ 未开始 / ⛔ 阻塞

| 模块                                        | 状态    | 完成度  | 关键产物                                           | 备注                                     |
| ----------------------------------------- | ----- | ---- | ---------------------------------------------- | -------------------------------------- |
| 产品/技术/接口文档                                | ✅ 完成  | 100% | PRD/TD/IF                                      | 已定稿                                    |
| 协作指导文档                                    | ✅ 完成  | 100% | DEVGUIDE.md                                    | 已定稿                                    |
| 环境准备（JDK/Maven/Docker/Node）               | 🔵 进行中 | 80%  | 本机 JDK21/Maven3.9.9/Node24 + 云服务器 Docker | 本机 Docker Desktop 因未检测到虚拟化无法启动 → 中间件承载迁移至腾讯云轻量服务器（见 §三 2026-09-06）；本机 JDK/Maven/Node 不受影响   |
| Git 仓库初始化                                 | ✅ 完成  | 100% | .gitignore                                     | master 主干 + GitHub Flow；PR #1 已合入，远程无远程额外分支                |
| 工程骨架（父POM/BOM/common/api/starter/modules） | ✅ 完成  | 100% | 父POM/BOM/common/api/8个starter/12个模块占位          | `mvn clean install -DskipTests` 全量编译通过 |
| 基础设施（docker-compose/init.sql）             | 🔵 进行中 | 70%  | docker-compose.yml / init.sql / prometheus.yml | compose/init.sql 定义完成；本机 7 中间件曾实机启动 healthy；现迁移至云服务器 39.106.110.214（Ubuntu 24.04）部署 PG/Redis，按 compose 逐项对齐重建中（见 §四 2026-09-06 踩坑） |
| UMS 认证服务                                  | ✅ 完成  | 100% | 认证5+用户5+角色权限6 接口 / JWT / RBAC / 黑名单 / 登录锁定 / Knife4j | 功能实机验证通过；UMS-1（双身份源收敛）+ UMS-2（refresh 轮换撤销）回归验证通过，PR #2 已合入 master，阶段正式收口；遗留 UMS 收尾优化项转 §6.1 待办池（不阻塞） |
| gateway 网关                                | 🔵 进行中 | 85%  | 骨架/路由/CORS + AuthGlobalFilter（JWT+防伪造头+sk-分流）+ fail-fast 配置 | 全链路冒烟 8/8 通过（2026-09-08，云 PG/Redis，UMS 7101+gateway 7000 经 7000 网关验证）；3 修复（pom repackage / AccessDenied→403 / charset）+ 文档已提交 feature/gateway（fe912aa，已 merge master 并推送远程）；待 PR 合入 master → 路由收窄 + 接入 Nacos（见 §六 6.4 / §七） |
| workspace 工作空间                            | ⚪ 未开始 | 0%   |                                                |                                        |
| model 模型网关                                | ⚪ 未开始 | 0%   |                                                |                                        |
| kb 知识库                                    | ⚪ 未开始 | 0%   |                                                |                                        |
| tool 工具市场                                 | ⚪ 未开始 | 0%   |                                                |                                        |
| agent Agent编排                             | ⚪ 未开始 | 0%   |                                                |                                        |
| conv 对话服务                                 | ⚪ 未开始 | 0%   |                                                |                                        |
| billing 计费                                | ⚪ 未开始 | 0%   |                                                |                                        |
| obs 监控审计                                  | ⚪ 未开始 | 0%   |                                                |                                        |
| notify 通知                                 | ⚪ 未开始 | 0%   |                                                |                                        |
| 前端 admin（控制台）                         | ⚪ 未开始 | 0%   | FEGUIDE v1.0 + FE-PROGRESS 骨架 | 指导文档已产出，前端工程建于独立仓库 D:\JavaProject\insight-engine-web\（接口契约仍以本仓库 IF.md 为唯一事实源）；定位：控制台（管理+开发一体，角色收缩菜单）+ 对话门户独立 SPA 后置；下一步 P0 脚手架（FEGUIDE §4.2） |
| 前端 chat                                   | ⚪ 未开始 | 0%   |                                                |                                        |
| docker-compose 全量编排                       | ⚪ 未开始 | 0%   |                                                |                                        |

---

## 三、关键技术决策记录（增量追加）

- [2026-08-25] 选定产品方向：企业级 AI Agent 编排与知识中枢平台（对标 Dify/Coze/FastGPT）
- [2026-08-25] 主库选 PostgreSQL（含 PGVector）而非 MySQL，见 TD ADR-2
- [2026-08-25] AI 框架 Spring AI 为主 + LangChain4j 为辅，见 TD ADR-3
- [2026-08-25] 工作流自研状态机，不引 Flowable，见 TD ADR-4
- [2026-08-25] ✅ 已决策：RabbitMQ 锁定 `3.13-management`，不复用本机 4.2；宿主端口映射 5673/15673（容器内仍 5672/15672），微服务走内部网络，见 TD ADR-11/ADR-12、TD §18.2
- [2026-08-25] ✅ 已决策：所有中间件宿主端口统一加偏移（PG 5433/Redis 6380/Nacos 8850/MinIO 9010/Prom 9091/Grafana 3001），避开本机占用，见 TD §18.2.3
- [2026-08-25] ✅ 已决策：JDK 复用本机 21，编译用 `--release 17` 产出 17 字节码，满足 MVP 的 Java17 目标，免装 JDK17
- [2026-08-25] ✅ 已决策：工作区根目录 `d:/CodexProject/` 作为「多工程容器」，智擎项目整体收进子目录 `insight-engine/`（项目完全自包含，docs 也移入 `insight-engine/docs/`），便于 IDEA 单独打开工程进行 review；未来 `CodexProject/` 下可并列多个工程
- [2026-08-25] ✅ 已决策：groupId=`com.insightengine`，版本 `1.0.0-SNAPSHOT`；编译目标 Java 17（`maven.compiler.release=17`）
- [2026-08-25] ✅ 备注：阶段 1 骨架未涉及入参校验（无 Controller），但 `starter-web` 的 `GlobalExceptionHandler` 已预留 `MethodArgumentNotValidException` / `ConstraintViolationException` 两类校验异常处理，**完整入参校验（@Valid/JSR-303 + 业务规则校验）在阶段 3 UMS 认证服务中落地**（与 `starter-security` 配合）
- [2026-08-26] ✅ 已决策：分支策略采用 **GitHub Flow**（master 稳定主干 + `feature/xxx` 功能分支），不建 develop/release/hotfix 多分支（Git Flow 对单人 MVP 过重），不按人名建分支（分支应表达"做什么"而非"谁在做"）；阶段 2 已从 master 切出 `feature/infra-docker-compose`
- [2026-08-26] ✅ 已核实：本项目规划的全部宿主映射端口（PG 5433 / Redis 6380 / RabbitMQ 5673+15673 / Nacos 8850+9850 / MinIO 9010+9011 / Prom 9091 / Grafana 3001）本机**均空闲无冲突**，无需调整 TD §18.2.3 端口表
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

---

## 四、踩坑记录（增量追加）

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
- [2026-09-08] 坑：`git fetch/push` 直连 github.com 报 443 超时，但 `curl.exe --resolve` 到 140.82.114.4 / 20.27.177.113 可达（DNS 默认解析的 20.205.243.166 被墙）；api.github.com / raw.githubusercontent.com 直连可达可作快速判别 → 规避：git 走可达 IP：`git -c http.curloptResolve="github.com:443:20.27.177.113" -c http.sslBackend=schannel fetch/push/ls-remote`（Windows git 默认 openssl 后端需显式 `-c http.sslBackend=schannel`，否则报 Unsupported SSL backend）；同 IP curl 通 ≠ git 通，实测为准
- [2026-09-08] 坑（协作规范教训，促成 LEARNING 铁律）：违反 GitHub Flow「master 只接受 PR」——在本地把 `feature/ums-security-fix` merge 进 master 并试图直接 push master → 被拦截纠正；正确处理：功能分支 commit → `git push origin feature/xxx` → 网页 PR（base=master）→ Merge → 本地 `checkout master && git pull`；已在 LEARNING.md 新增「Git 常用命令速查（2026-09-08）」固化 8 场景 + 五条铁律
- [2026-09-08] 坑：PowerShell 5.1 `Invoke-RestMethod` 显示中文乱码（PS 对无 charset 响应按 ISO-8859-1 解码）≠ 服务端/DB 数据损坏——判定：curl 存原始字节后用 python 按 utf-8 解析看 `repr`/hex，字节合法即纯客户端解码问题；修复走服务端 charset（见 §三 2026-09-08）或脚本改字节安全读取

---

## 五、当前阻塞 / 待解决问题（= 必须修，未完成前阻塞交付收口）

> 本节只保留**尚未解决**的必须修项；已办结项已移入 §三（决策/修复留档）/ §四（踩坑）/ §八（对话摘要），不再滞留于此。

> 当前无代码级必须修项。UMS 收尾 UMS-1/UMS-2 已实机回归验证通过并合入 master（PR #2，见 §三 2026-09-03 留档），正式关闭。
>
> **gateway 冒烟前置已全部打通（2026-09-08）**：云服务器 39.106.110.214 的 PG/Redis 已由 UMS 连接实机验证可用；UMS 配置已切云（§三 2026-09-08）；曾阻塞 gateway 冒烟的 3 项（master 缺 `JwtRefreshPayload` / ums/gateway pom 无 `repackage` / `@PreAuthorize` 拒绝误报 500）均已修复并随 `feature/gateway` 推送远程。§五无遗留阻塞。

---

## 六、后续待办与优化池（🟡 建议修 = 后续要完成 / 🟢 可选优化 = 低优先级择机）

> 本节任务**不阻塞当前交付**，按处理阶段归类；完成一条勾一条。

### 6.1 UMS 服务收尾（🟡，UMS 主线已收口，随后续阶段择机）

> **高价值优先组**（原 §七 Top2，已核对代码均未落地，保持待办）：phone 唯一索引、roleId 前置校验、授权集合去重校验、`DuplicateKey`/`HttpMessageNotReadableException`(1002) 友好映射、`Result` 成功响应 traceId 回填（末项在 §6.2）。建议 gateway 阶段收尾后作为独立任务优先处理。

- [ ] `ie_user.phone` 加部分唯一索引（`init.sql` 补 `uk_user_phone`，`DB.md` 同步）——手机号也是登录账号（IF §3.1），当前无唯一约束存在串号登录歧义
- [ ] 邮箱大小写归一：注册/创建/登录/唯一性查询统一 `lower(trim)`，防 `A@x.com` 与 `a@x.com` 注册成双账号
- [ ] 创建用户前校验 `roleId` 存在（`UserServiceImpl.create` 前置 `requireRole`），防孤儿 member
- [ ] 角色授权/创建：`permissionIds` 先去重 + 校验有效性（`RoleServiceImpl.assignPermissions/create`；`batchInsert` 改 `ON CONFLICT DO NOTHING`），防联合主键冲突与垃圾关联
- [ ] 删除角色前检查 `ie_member` 引用（`RoleServiceImpl.delete:92-103`）：被引用返回 1003 或级联清理，防用户角色静默丢失 + 孤儿数据
- [ ] `GlobalExceptionHandler` 补 `DuplicateKeyException`（并发注册/创建/角色唯一冲突 → 1001 友好文案）与 `HttpMessageNotReadableException`（body 解析错误 → 1002），不再一律 500
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
- [ ] 权限编码二级/三级混用统一规范（`kb:read` vs `model:vendor:write`）——各模块开工前约定编码体系
- [ ] `WorkspaceMapper` 直查 `ie_workspace` 改走 Feign（workspace 服务落地后，TD §3.2 服务边界）
- [ ] DataScope 行级数据权限拦截器（TD §7.5）——多租户/V1.0 前必须完成，覆盖全部业务列表查询

### 6.4 gateway / Nacos / 部署阶段（🟡）

- [x] **认证模型定案**：ADR-5（网关校验 JWT 下发明文头）vs 当前「服务自校验 JWT」双轨矛盾 → 已裁决（2026-09-03）：网关校验 JWT + 下发头，但服务端通过 `insight.web.trust-gateway-headers` 开关决定是否信任，双轨共存、默认自校验，见 §三 2026-09-03 留档
- [x] gateway 模块骨架 + 路由 + 全局 Cors（2026-09-03 完成，`feature/gateway` 分支）：POM 补 jjwt/可执行插件、`GatewayApplication`、`application.yml`（端口 7000、UMS 路由 `/auth/**`+`/api/v1/**`+文档路径、globalcors），`mvn install` 编译通过
- [x] gateway AuthGlobalFilter：JWT 校验 + 明文头注入（防客户端伪造头）+ `sk-` API Key 分流 + 错误转 Result（TD §8.3）——代码完成并已提交 f45a226（2026-09-04），编译通过，待联调验证
- [x] **gateway 全链路冒烟**（2026-09-08 完成，云 PG/Redis + UMS 7101 + gateway 7000）：登录/me/refresh 200、register 400 业务码、无 token/坏 token 401、权限不足 403（修复后）、logout 后旧 token 401（撤销生效）——8/8 通过；3 修复见 §三 2026-09-08；`/doc.html` 白名单、`sk-` 分流、过期 token 2007 本次未覆盖，随路由收窄/Nacos 轮补
- [ ] **gateway 路由按 TD §8.3 细分**（workspace 等服务接入前置，P18）：冒烟期 `/api/v1/**` 全量直连 UMS 收窄为服务专属前缀（`/auth/**`、`/api/v1/user|role/**` → ums；`/api/v1/org|workspace|member/**` → workspace；`/api/v1/kb/**` → kb…），禁止全量 fallback 或 fallback 排在细分之前；收窄与新增路由同一次提交
- [ ] 服务接入 Nacos 注册/配置中心（冒烟期 UMS 路由为直连 localhost:7101，Nacos 接入后改 `lb://insight-engine-ums`）
- [ ] 中间件与应用密码差异化：`insight123` / `application.yml` 明文密码改 `.env`/secrets + 环境变量占位注入
- [ ] 引入 Flyway schema 迁移（替代一次性 init.sql）
- [ ] 部分容器 healthcheck 补 `start_period`（🟢）

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

1. **gateway 阶段 PR 合入 master 收口**：`feature/gateway`（fe912aa，含冒烟修复 d437202 + 文档 a84a574/fad5940，已 merge master 并推送远程）提 PR（base=master）→ Merge 后 gateway 主线收口
2. **gateway 路由按 TD §8.3 细分 + 接入 Nacos**：冒烟期 `/api/v1/**` 全量直连 UMS 收窄为服务专属前缀（workspace 等服务接入前置，见 §六 6.4）；Nacos 就绪后路由改 `lb://insight-engine-ums`（云服务器补充 nacos 容器）
3. 完成 §6.1 高价值 UMS 收尾项（phone 唯一索引、roleId 校验、授权去重、DuplicateKey/1002、Result traceId 回填）——独立收尾任务，不阻塞 gateway

---

## 八、最近一次对话摘要

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
