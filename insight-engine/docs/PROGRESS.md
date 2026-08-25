# 项目进度追踪（PROGRESS）

> 本文件是「开发者」与「AI」之间的**共享进度真相源**，是跨对话记忆的唯一可靠载体。
> **铁律：每次对话结束前必须更新本文件；每次新对话开始必须先读本文件。**

---

## 一、总体状态

| 项     | 值                      |
| ----- | ---------------------- |
| 当前阶段  | 阶段 1：工程骨架（已完成，待进入阶段 2） |
| 当前里程碑 | M1：Maven 多模块工程骨架（已完成）  |
| 当前任务  | 工程骨架搭建 + 编译验证闭环        |
| 整体完成度 | 约 10%（阶段 1 全部完成）       |

---

## 二、模块进度看板

> 状态图例：✅ 完成 / 🔵 进行中 / ⚪ 未开始 / ⛔ 阻塞

| 模块                                        | 状态    | 完成度  | 关键产物                                  | 备注                                     |
| ----------------------------------------- | ----- | ---- | ------------------------------------- | -------------------------------------- |
| 产品/技术/接口文档                                | ✅ 完成  | 100% | PRD/TD/IF                             | 已定稿                                    |
| 协作指导文档                                    | ✅ 完成  | 100% | DEVGUIDE.md                           | 已定稿                                    |
| 环境准备（JDK/Maven/Docker/Node）               | ✅ 完成  | 100% | 本机 JDK21/Maven3.9.9/Docker29/Node24   | 全部就绪，Docker 引擎已启动                      |
| Git 仓库初始化                                 | ✅ 完成  | 100% | .gitignore                            | master 分支，已有 5 次提交，无远程                 |
| 工程骨架（父POM/BOM/common/api/starter/modules） | ✅ 完成  | 100% | 父POM/BOM/common/api/8个starter/12个模块占位 | `mvn clean install -DskipTests` 全量编译通过 |
| 基础设施（docker-compose/init.sql）             | ⚪ 未开始 | 0%   |                                       |                                        |
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

---

## 五、当前阻塞 / 待解决问题

- [ ] 待确认：PostgreSQL/Redis/Nacos/MinIO 本机是否已有其他容器占用对应宿主端口（阶段 2 逐一核实，若有冲突按 TD §18.2.3 端口表调整）

---

## 六、下一步计划（Top 3）

1. 编写 docker-compose.yml + init.sql 基础设施（阶段 2）
2. 核实本机中间件宿主端口占用情况（PostgreSQL/Redis/Nacos/MinIO）
3. 启动 UMS 认证服务（第一个可运行微服务，验证分层规范落地）

---

## 七、最近一次对话摘要

- 日期：2026-08-25
- 内容：阶段 1 工程骨架闭环并进入收尾——① 目录层级重构为 `insight-engine/` 自包含项目（`d:/CodexProject/` 成为多工程容器）；② 父 POM + BOM + common + api + 8 starter + 12 业务模块占位，`mvn clean install -DskipTests` 全量编译通过；③ 排查并解决 `package-info.java` 在 IDEA 中不被识别的问题，最终回归企业标准：`package-info.java` 保持「包级文档」本分、不写类型锚点，接受骨架阶段 api 模块暂空的过渡显示；④ 清理 21 个 target 构建产物；⑤ 沉淀 3 篇学习笔记（Docker 端口映射 / Maven 多模块工程 / package-info.java 包级元数据）。
- 下一步：进入阶段 2，编写 docker-compose.yml + init.sql 基础设施；阶段 1 代码改动待 git 提交（`git add -A && git commit`）
