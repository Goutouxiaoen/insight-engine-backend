# 项目进度追踪（PROGRESS）

> 本文件是「开发者」与「AI」之间的**共享进度真相源**，是跨对话记忆的唯一可靠载体。
> **铁律：每次对话结束前必须更新本文件；每次新对话开始必须先读本文件。**

---

## 一、总体状态

| 项     | 值                                  |
| ----- | ---------------------------------- |
| 当前阶段  | 阶段 3：UMS 认证服务（第一个完整微服务，已实机验证登录闭环） |
| 当前里程碑 | M3：UMS 认证服务                      |
| 当前任务  | UMS 认证 + 用户 + 角色/权限（已实机验证登录/me/角色/权限树） |
| 整体完成度 | 约 27%（阶段 1+2 完成，UMS 完成，待 gateway/workspace） |

---

## 二、模块进度看板

> 状态图例：✅ 完成 / 🔵 进行中 / ⚪ 未开始 / ⛔ 阻塞

| 模块                                        | 状态    | 完成度  | 关键产物                                           | 备注                                     |
| ----------------------------------------- | ----- | ---- | ---------------------------------------------- | -------------------------------------- |
| 产品/技术/接口文档                                | ✅ 完成  | 100% | PRD/TD/IF                                      | 已定稿                                    |
| 协作指导文档                                    | ✅ 完成  | 100% | DEVGUIDE.md                                    | 已定稿                                    |
| 环境准备（JDK/Maven/Docker/Node）               | ✅ 完成  | 100% | 本机 JDK21/Maven3.9.9/Docker29/Node24            | 全部就绪，Docker 引擎已启动                      |
| Git 仓库初始化                                 | ✅ 完成  | 100% | .gitignore                                     | master 分支，已有 6 次提交，无远程                 |
| 工程骨架（父POM/BOM/common/api/starter/modules） | ✅ 完成  | 100% | 父POM/BOM/common/api/8个starter/12个模块占位          | `mvn clean install -DskipTests` 全量编译通过 |
| 基础设施（docker-compose/init.sql）             | ✅ 完成  | 100% | docker-compose.yml / init.sql / prometheus.yml | 7 中间件实机启动全部 healthy；PG 建表 35 表全注释验证通过  |
| UMS 认证服务                                  | ✅ 完成  | 100% | 认证5+用户5+角色权限6 接口 / JWT / RBAC / 黑名单 / 登录锁定 / Knife4j | 已实机验证登录/me/角色列表/权限树，文档页 200 |
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

---

## 五、当前阻塞 / 待解决问题

- [x] 已解决：本机宿主端口占用核实完毕，本项目规划的宿主映射端口（PG/Redis/RabbitMQ/Nacos/MinIO/Prom/Grafana）全部空闲无冲突
- [x] 已解决：Docker Hub 无法直连 → 改用 DaoCloud 镜像加速器，7 个中间件镜像全部拉取并实机启动，healthcheck 全部 healthy
- [x] 已解决：PG 实机建表验证通过（35 表 / 356 字段全注释 / 69 索引 / 种子数据齐全，管理员密码 hash 与 Admin@123 匹配）
- [x] 已解决（review 🔴）：种子数据显式 id 未重置自增序列 → 已在 init.sql 末尾对 7 张表补 setval 重置，实机验证序列同步正确、自增插入不冲突

### Review 🔴 必须修（UMS 阶段 3，2026-08-26 review 产出，下次对话优先处理）

- [ ] 🔴 **禁用/改密后 token 不失效 —— 踢人机制形同虚设**
  - 定位：`UserServiceImpl.updateStatus:138-140` 与 `updatePassword:161` 仅执行 `delete(ie:auth:token:{userId})`；而 `JwtAuthFilter.doFilterInternal:78-81` 只查黑名单 `isBlacklisted(token)`、**从不读该缓存**；`AuthServiceImpl.cacheToken:302-307` 写入的缓存无任何消费方（死代码），且存的是完整 token 明文（与"存摘要"注释自相矛盾）。
  - 影响：禁用用户/改密后，已签发的 access token（最长 2h）仍可正常访问，注释宣称的"强制重新登录"完全不成立；Redis 中的明文 token 也是泄露面。
  - 修复（推荐方案 B）：
    - A：`JwtAuthFilter` 增加"校验 `ie:auth:token:{userId}` 存在且摘要匹配"，踢人靠删缓存（改动最小）；
    - B（推荐）：JWT 引入 `jti` + `ver`（会话版本号）Claim，禁用/改密时 `ver`+1 写 Redis，过滤器比对版本，无状态友好、可扩展；
    - C：改密/禁用时枚举该用户全部会话 token 加入黑名单（需维护 userId→tokenHash 集合）。
  - 附带：`cacheToken` 一律改存 SHA-256 摘要（与 `RedisTokenBlacklistService` 对齐），杜绝明文 token 落 Redis。

- [ ] 🔴 **JWT 密钥硬编码且可预测**
  - 定位：`SecurityProperties.jwtSecret:26` 代码内默认值 + `application.yml:41` 明文写死同一开发密钥 `insight-engine-dev-secret-key-change-me-in-prod-2026-08`。
  - 影响：HS256 对称密钥若以默认值上线，攻击者可离线伪造任意 userId/roles/perms 的 JWT，等于完全绕过认证与授权。
  - 修复：删除代码内默认值；生产 profile 下启动校验（jwtSecret 为空或含 `change-me` 直接 fail-fast 拒绝启动）；密钥仅经环境变量/配置中心注入。

- [ ] 🔴 **双身份源并存 —— UserContextFilter 无条件信任明文身份头（承自骨架阶段 review 红级问题）**
  - 定位：UMS 同时依赖 `starter-web`（自动装配 `UserContextFilter`，`WebAutoConfiguration:56-60`，从 `X-User-Id/X-Tenant-Id/X-Roles` 明文头解析并 `UserContext.set`，order=HIGHEST+1 先执行）与 `starter-security`（`JwtAuthFilter:98-112` 解析 JWT 覆盖 `UserContext`）。
  - 影响：需认证接口虽被 JwtAuthFilter 覆盖，但白名单接口（login/register/refresh）及未来新增接口中，伪造的明文头身份会直接进入 `UserContext`，构成水平+垂直越权面；两套身份源并存，安全依赖"filter 顺序"这一脆弱前提。
  - 修复（二选一）：
    - 方案 A（推荐，UMS 走 JWT 解析）：给 `UserContextFilter` 加条件装配开关（如 `insight.web.trust-gateway-headers=false`），仅"网关下发明文头"方案的服务开启；
    - 方案 B（走 TD ADR-5 明文头方案）：加 HMAC 签名头（X-User-Sign）校验 / IP 网段校验兜底。

### Review 🟡 建议修（随对应阶段推进修复，本次不处理）

- [ ] 🟡 `ie_usage_record` 缺幂等唯一键（违背 TD §12.5，MQ 重投会重复计量）→ billing 阶段补 `event_id` 列 + 唯一索引
- [ ] 🟡 中间件密码明文且全服务同密码 `insight123`（docker-compose.yml）→ 生产改 `.env`/secrets 分离 + 密码差异化
- [ ] 🟡 `ie_chunk.embedding` 维度硬编码 `vector(1024)`（切换 768 维本地模型会失败）→ 模型网关阶段约束 Embedding 维度统一 1024
- [ ] 🟡 `ie_message."references"` / `ie_audit_log."before"/"after"` 双引号关键字列名（ORM 映射有坑）→ 建议改 `refs`/`before_data`/`after_data` 并同步 PRD
- [ ] 🟡 `ie_user.phone` 无唯一索引（手机号登录歧义）→ 补 `uk_user_phone` 部分唯一索引
- [ ] 🟡 `ie_agent_invocation` 缺 `status`/`error_msg`（无法记录失败调用）→ 补列对齐 `ie_tool_invocation`
- [ ] 🟡 权限编码二级/三级混用（`kb:read` vs `model:vendor:write`）→ 阶段 3 约定统一编码规范
- [ ] 🟡 refresh token 无法主动失效（`AuthServiceImpl.logout:145-157` 只黑名单 access token；refresh token 7d 无黑名单/无轮换/无 jti）→ 窃取后 7 天内可无限刷新、登出无法终止；建议 refresh 一次性轮换（每次 refresh 旧 token 作废、签发新 token 对）或纳入黑名单
- [ ] 🟡 删除角色未检查成员引用（`RoleServiceImpl.delete:92-103` 只逻辑删 role + 删 role_permission，未处理 `ie_member.role_id`）→ 删除后该角色成员登录时 `selectRoleCodesByUserId`（`RoleMapper:28` 的 r.deleted=0）查不到角色，用户角色静默丢失 + 孤儿 member 数据；建议删除前检查引用（有则拒绝 1003 或先迁移）
- [ ] 🟡 创建用户未校验 roleId 存在（`UserServiceImpl.create:97-104` 直接 set roleId 插入 member）→ 可能写入孤儿 member；建议先 requireRole(roleId) 返回 RESOURCE_NOT_FOUND
- [ ] 🟡 登录失败计数 increment+expire 竞态（`AuthServiceImpl.handleLoginFail:215-228` increment 返回 1 后单独 expire 非原子）→ 首次失败后进程崩溃/Redis 抖动导致 failKey 永不过期；建议 Lua 原子化（INCR+EXPIRE）或 SET NX EX + INCR
- [ ] 🟡 并发"先查后插"唯一索引冲突返回 500（`AuthServiceImpl.register:167-171` / `UserServiceImpl.create:81-85` / `RoleServiceImpl.create:58-62` 均 check-then-act；`GlobalExceptionHandler:85-91` 未捕获 DuplicateKeyException）→ 并发下唯一索引兜底触发后落到 500 系统内部错误而非"邮箱已注册"；建议捕获 DuplicateKeyException 映射 PARAM_ERROR 友好文案
- [ ] 🟡 密码复杂度低于 TD 约定（`RegisterRequest:26` / `UserCreateRequest:32` / `PasswordUpdateRequest:23` 正则 `^(?=.*[A-Za-z])(?=.*\d).+$` 只要求字母+数字，注释宣称"含大小写"但未强制大写）→ 建议改为 `(?=.*[a-z])(?=.*[A-Z])(?=.*\d)`
- [ ] 🟡 application.yml 明文密码（`application.yml:15,25` 数据库/Redis 密码 `insight123` 明文）→ 建议环境变量占位 `${DB_PASSWORD:insight123}` 等
- [ ] 🟡 WorkspaceMapper 跨服务直查 `ie_workspace`（`WorkspaceMapper:21` 违反 TD §3.2 服务边界，MVP 临时方案）→ workspace 服务落地后改走 Feign，避免隐性数据库耦合

### Review 🟢 可选优化（低优先级，择机处理）

- [ ] 🟢 无 Schema 迁移机制（init.sql 一次性执行）→ 阶段 3 起引入 Flyway，转 `V1__init.sql`
- [ ] 🟢 `ie_user` 缺「email / phone 至少其一」CHECK 约束
- [ ] 🟢 部分容器 healthcheck 缺 `start_period`（除 nacos）
- [ ] 🟢 组装用户信息多次查库（`AuthServiceImpl.buildUserInfo:264-268` 登录 4 次/`me` 3 次查询）→ 可合并为 1~2 条联表 SQL 或一次查询复用
- [ ] 🟢 默认角色 ID 每次注册查库（`AuthServiceImpl.resolveDefaultRoleId:315-322` end_user 是预置常量）→ 可启动时加载本地缓存/常量，避免每次注册 selectOne
- [ ] 🟢 logout 重复解析 token（`AuthServiceImpl.logout:146,152` getRemainingSeconds 与 parseAccessToken 各 parse 一次）→ 可一次解析复用
- [ ] 🟢 RoleCreateRequest.scope 未设默认值（`RoleCreateRequest:31` 可选，`RoleServiceImpl` 直接 setScope 若为 null 且表列 NOT NULL 会插入失败）→ 建议默认 SELF
- [ ] 🟢 status=null 跳过禁用检查（`AuthServiceImpl:95,132` 判空后放行）→ 建议改为 `!ACCOUNT_NORMAL.equals(status)` 拦截
- [ ] 🟢 登录无验证码/IP 限流（`CAPTCHA_ERROR(2005)` 已定义未使用）→ MVP 靠账号锁定可接受，V1.0 补 IP 维度限流 + 验证码

### TODO：JWT 权限载荷优化（后续迭代，MVP 不处理）

- [ ] 🔧 **JWT `perms` Claim 膨胀优化**：当前把权限编码全量塞进 token（超管 48 权限，`perms` 占约 1.3KB，整条 token ~2KB，每次请求全量携带）。MVP 阶段单租户权限量小可接受；待权限规模上来后，二选一演进：
  - 方案 A（推荐）：JWT 只存角色编码 + Redis 缓存「角色→权限」映射（`ie:auth:role:perms:{roleCode}`），兼顾体积与实时性，但引入缓存一致性（改权限主动删缓存/短 TTL）
  - 方案 B：权限编码位图压缩（权限表加 `bit_index`，bitmap 存 16 进制），token 最小但可读性差
  - 详见 LEARNING.md「RBAC vs ABAC + 权限进 JWT 的权衡」权衡③详解

---

## 六、下一步计划（Top 3）

1. 处理 UMS review 🔴 清单（token 失效 / JWT 密钥 / 明文头越权，详见第五节），修复后回归验证
2. 实现 gateway 网关（Spring Cloud Gateway，路由 + AuthGlobalFilter 校验 JWT 下发明文头）
3. UMS/gateway 完成后接入 Nacos 注册/配置中心，打通服务发现

---

## 七、最近一次对话摘要

- 日期：2026-08-26
- 内容：阶段 3 UMS 认证服务（第一个完整微服务）——① 从 master 切出 `feature/ums-auth`；② 实现三个 starter 骨架：starter-mybatis（MP 装配 + 逻辑删除全局配置 + 审计字段填充）、starter-redis（RedisTemplate JSON 序列化）、starter-security（SecurityFilterChain 无状态 + JWT 签发/解析 + 认证过滤器 + 未认证/无权限统一 Result 处理 + 可选黑名单）；③ 实现 UMS 完整业务：认证 5 接口（登录含 5 次锁定/刷新/登出黑名单/注册/当前用户）、用户 5 接口（分页/创建/更新/启停/改密）、角色 5 + 权限树 1 接口（含内置角色禁删 1003、授权先删后插）；④ 入参 JSR-303 校验 + @PreAuthorize 方法级权限（member:read/role:write 等）全部落地；⑤ 编译通过，实机冒烟验证：登录返回 48 权限的 JWT、/auth/me 返回工作空间、角色列表 5 个、权限树 27 组、未带 token 访问返回 401；⑥ 接入 Knife4j（文档页 200、OpenAPI 15 路径）+ 产出 FEATURES.md 功能模块实现清单。
- 日期：2026-08-26
- 内容：UMS 服务完整代码 review（对照 TD/IF）——产出 3 🔴 / 8 🟡 / 6 🟢 待修清单，已全部写入第五节"当前阻塞/待解决问题"分级子节。核心三红：① 禁用/改密后 token 不失效（踢人机制失效 + cacheToken 存明文死代码）；② JWT 密钥硬编码默认值可预测；③ UserContextFilter 信任明文头与 JwtAuthFilter 双身份源并存（越权面）。
- 下一步：按第五节 🔴 清单修复 UMS，然后实现 gateway 网关，接入 Nacos
