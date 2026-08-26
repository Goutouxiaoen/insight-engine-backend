# 项目进度追踪（PROGRESS）

> 本文件是「开发者」与「AI」之间的**共享进度真相源**，是跨对话记忆的唯一可靠载体。
> **铁律：每次对话结束前必须更新本文件；每次新对话开始必须先读本文件。**

---

## 一、总体状态

| 项     | 值                      |
| ----- | ---------------------- |
| 当前阶段  | 阶段 2：基础设施层（已完成，待进入阶段 3） |
| 当前里程碑 | M2：基础设施（docker-compose + init.sql） |
| 当前任务  | 中间件编排 + 数据库初始化脚本（已实机验证） |
| 整体完成度 | 约 20%（阶段 1+2 完成，进入阶段 3 UMS） |

---

## 二、模块进度看板

> 状态图例：✅ 完成 / 🔵 进行中 / ⚪ 未开始 / ⛔ 阻塞

| 模块                                        | 状态    | 完成度  | 关键产物                                  | 备注                                     |
| ----------------------------------------- | ----- | ---- | ------------------------------------- | -------------------------------------- |
| 产品/技术/接口文档                                | ✅ 完成  | 100% | PRD/TD/IF                             | 已定稿                                    |
| 协作指导文档                                    | ✅ 完成  | 100% | DEVGUIDE.md                           | 已定稿                                    |
| 环境准备（JDK/Maven/Docker/Node）               | ✅ 完成  | 100% | 本机 JDK21/Maven3.9.9/Docker29/Node24   | 全部就绪，Docker 引擎已启动                      |
| Git 仓库初始化                                 | ✅ 完成  | 100% | .gitignore                            | master 分支，已有 6 次提交，无远程                 |
| 工程骨架（父POM/BOM/common/api/starter/modules） | ✅ 完成  | 100% | 父POM/BOM/common/api/8个starter/12个模块占位 | `mvn clean install -DskipTests` 全量编译通过 |
| 基础设施（docker-compose/init.sql）             | ✅ 完成 | 100% | docker-compose.yml / init.sql / prometheus.yml | 7 中间件实机启动全部 healthy；PG 建表 35 表全注释验证通过 |
| UMS 认证服务                                  | ⚪ 未开始 | 0%   |                                       |                                        |
| gateway 网关                                | ⚪ 未开始 | 0%   |                                       |                                        |
| workspace 工作空间                            | ⚪ 未开始 | 0%   |                                       |                                        |
| model 模型网关                                | ⚪ 未开始 | 0%   |                                       |                                        |
| kb 知识库                                    | ⚪ 未开始 | 0%   |                                       |                                        |
| tool 工具市场                                 | ⚪ 未开始 | 0%   |                                       |                                        |
| agent Agent编排                             | ⚪ 未开始 | 0%   |                                       |                                        |
| conv 对话服务                                 | ⚪ 未开始 | 0%   |                                       |                                        |
| billing 计费                                | ⚪ 未开始 | 0%   |                                       |                                        |
| obs 监控审计                                  | ⚪ 未开始 | 0%   |                                       |                                        |
| notify 通知                                 | ⚪ 未开始 | 0%   |                                       |                                        |
| 前端 admin                                  | ⚪ 未开始 | 0%   |                                       |                                        |
| 前端 chat                                   | ⚪ 未开始 | 0%   |                                       |                                        |
| docker-compose 全量编排                       | ⚪ 未开始 | 0%   |                                       |                                        |

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
- [ ] 待处理：`UserContextFilter` 无条件信任明文身份头（X-User-Id/X-Tenant-Id/X-Roles），存在水平+垂直越权面（review 红级问题）。既定决策见 TD ADR-5（网关下发明文头、服务不解析 JWT），**留待阶段 3 落地 UMS 时补齐**，至少一项：网络层端口隔离 / 代码层 HMAC 签名（X-User-Sign）/ 最低成本 IP 网段校验

### Review 🟡 建议修（随对应阶段推进修复，本次不处理）

- [ ] 🟡 `ie_usage_record` 缺幂等唯一键（违背 TD §12.5，MQ 重投会重复计量）→ billing 阶段补 `event_id` 列 + 唯一索引
- [ ] 🟡 中间件密码明文且全服务同密码 `insight123`（docker-compose.yml）→ 生产改 `.env`/secrets 分离 + 密码差异化
- [ ] 🟡 `ie_chunk.embedding` 维度硬编码 `vector(1024)`（切换 768 维本地模型会失败）→ 模型网关阶段约束 Embedding 维度统一 1024
- [ ] 🟡 `ie_message."references"` / `ie_audit_log."before"/"after"` 双引号关键字列名（ORM 映射有坑）→ 建议改 `refs`/`before_data`/`after_data` 并同步 PRD
- [ ] 🟡 `ie_user.phone` 无唯一索引（手机号登录歧义）→ 补 `uk_user_phone` 部分唯一索引
- [ ] 🟡 `ie_agent_invocation` 缺 `status`/`error_msg`（无法记录失败调用）→ 补列对齐 `ie_tool_invocation`
- [ ] 🟡 权限编码二级/三级混用（`kb:read` vs `model:vendor:write`）→ 阶段 3 约定统一编码规范

### Review 🟢 可选优化（低优先级，择机处理）

- [ ] 🟢 无 Schema 迁移机制（init.sql 一次性执行）→ 阶段 3 起引入 Flyway，转 `V1__init.sql`
- [ ] 🟢 `ie_user` 缺「email / phone 至少其一」CHECK 约束
- [ ] 🟢 部分容器 healthcheck 缺 `start_period`（除 nacos）

---

## 六、下一步计划（Top 3）

1. 启动 UMS 认证服务（第一个可运行微服务，验证分层规范落地）
2. 阶段 3 落地时补齐 UserContextFilter 越权面（HMAC 签名 / IP 网段校验）
3. 阶段 3 完成后接入 Nacos 注册/配置中心，打通服务发现

---

## 七、最近一次对话摘要

- 日期：2026-08-26
- 内容：阶段 2 基础设施层——① 分支策略定为 GitHub Flow，切出 `feature/infra-docker-compose`；② 编写 `docker-compose.yml`（7 中间件）、`init.sql`（35 表 + 全字段 COMMENT 注释 + 种子数据）、`prometheus.yml`、`DB.md`（数据库设计文档）；③ 端口核实全部空闲；④ 用 DaoCloud 镜像加速器绕过 Docker Hub 直连失败，评估本机已有镜像（rabbitmq 4.2 / nacos 3.1.1 / minio latest 因版本与 TD 锁定不符均不复用），拉取 6 个缺失镜像 + pgvector；⑤ 7 个中间件全部实机启动且 healthcheck 全部 healthy；PG 实机建表验证：35 表 / 356 字段全注释 / 69 索引 / 种子数据齐全；⑥ 架构师 review：🔴 种子数据显式 id 未重置序列（已修 + 实机验证不冲突），🟡 7 项 + 🟢 3 项已记入「五、待解决问题」。
- 下一步：进入阶段 3，启动 UMS 认证服务
