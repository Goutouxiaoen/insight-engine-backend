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
| 当前阶段  | 阶段 3：UMS 认证服务 —— 功能完成，安全收尾已修复待回归合入 |
| 当前里程碑 | M3：UMS 认证服务                      |
| 当前任务  | 回归验证 UMS 安全收尾（UMS-1/UMS-2）并合入 master → 之后启动 gateway 网关 |
| 整体完成度 | 约 27%（阶段 1+2 完成，UMS 功能完成，安全收尾修复待验证合入，其余模块未开始） |

---

## 二、模块进度看板

> 状态图例：✅ 完成 / 🔵 进行中 / ⚪ 未开始 / ⛔ 阻塞

| 模块                                        | 状态    | 完成度  | 关键产物                                           | 备注                                     |
| ----------------------------------------- | ----- | ---- | ---------------------------------------------- | -------------------------------------- |
| 产品/技术/接口文档                                | ✅ 完成  | 100% | PRD/TD/IF                                      | 已定稿                                    |
| 协作指导文档                                    | ✅ 完成  | 100% | DEVGUIDE.md                                    | 已定稿                                    |
| 环境准备（JDK/Maven/Docker/Node）               | ✅ 完成  | 100% | 本机 JDK21/Maven3.9.9/Docker29/Node24            | 全部就绪，Docker 引擎已启动                      |
| Git 仓库初始化                                 | ✅ 完成  | 100% | .gitignore                                     | master 主干 + GitHub Flow；PR #1 已合入，远程无远程额外分支                |
| 工程骨架（父POM/BOM/common/api/starter/modules） | ✅ 完成  | 100% | 父POM/BOM/common/api/8个starter/12个模块占位          | `mvn clean install -DskipTests` 全量编译通过 |
| 基础设施（docker-compose/init.sql）             | ✅ 完成  | 100% | docker-compose.yml / init.sql / prometheus.yml | 7 中间件实机启动全部 healthy；PG 建表 35 表全注释验证通过  |
| UMS 认证服务                                  | 🔵 收尾中 | 95%   | 认证5+用户5+角色权限6 接口 / JWT / RBAC / 黑名单 / 登录锁定 / Knife4j | 功能已实机验证（登录/me/角色/权限树/文档页 200）；红级修复① token 失效、② JWT 密钥 fail-fast 已合入 master；**遗留 2 项安全收尾阻塞见 §五** |
| gateway 网关                                | ⚪ 未开始 | 0%   |                                                |                                        |
| workspace 工作空间                            | ⚪ 未开始 | 0%   |                                                |                                        |
| model 模型网关                                | ⚪ 未开始 | 0%   |                                                |                                        |
| kb 知识库                                    | ⚪ 未开始 | 0%   |                                                |                                        |
| tool 工具市场                                 | ⚪ 未开始 | 0%   |                                                |                                        |
| agent Agent编排                             | ⚪ 未开始 | 0%   |                                                |                                        |
| conv 对话服务                                 | ⚪ 未开始 | 0%   |                                                |                                        |
| billing 计费                                | ⚪ 未开始 | 0%   |                                                |                                        |
| obs 监控审计                                  | ⚪ 未开始 | 0%   |                                                |                                        |
| notify 通知                                 | ⚪ 未开始 | 0%   |                                                |                                        |
| 前端 admin                                  | ⚪ 未开始 | 0%   |                                                |                                        |
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

---

## 五、当前阻塞 / 待解决问题（= 必须修，未完成前阻塞交付收口）

> 本节只保留**尚未解决**的必须修项；已办结项已移入 §三（决策/修复留档）/ §四（踩坑）/ §八（对话摘要），不再滞留于此。

> 当前无未解决的必须修项。UMS 收尾 UMS-1/UMS-2 代码已修复（见 §三 决策留档与 §八 摘要），待回归验证并合入 master 后正式关闭（见 §七）。

---

## 六、后续待办与优化池（🟡 建议修 = 后续要完成 / 🟢 可选优化 = 低优先级择机）

> 本节任务**不阻塞当前交付**，按处理阶段归类；完成一条勾一条。

### 6.1 UMS 服务收尾（🟡，随 UMS 阶段完成）

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

- [ ] **认证模型定案**：ADR-5（网关校验 JWT 下发明文头）vs 当前「服务自校验 JWT」双轨矛盾，gateway 落地前裁决并与 §五 UMS-1 联动
- [ ] gateway 网关：路由 + AuthGlobalFilter + Cors（TD §8.3）
- [ ] 服务接入 Nacos 注册/配置中心
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

1. **回归验证并合入 UMS 安全收尾**（分支 `feature/ums-security-fix`）：UMS-1（关闭明文头后 UMS 全链路仍正常）+ UMS-2（登录→刷新轮换→旧 refresh 重放被拒→登出/改密/禁用后 refresh 失效）实机验证后合入 master
2. 完成 §6.1 高价值收尾项：`phone` 唯一索引、`roleId` 校验、授权集合去重校验、`DuplicateKey`/`1002` 友好映射、`Result` traceId 回填
3. 实现 gateway 网关（路由 + AuthGlobalFilter + 认证模型定案，联动 UMS-1 开关），并接入 Nacos 注册/配置中心

---

## 八、最近一次对话摘要

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
