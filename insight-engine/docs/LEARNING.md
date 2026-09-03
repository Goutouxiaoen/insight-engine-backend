# 学习笔记（LEARNING）

> 本文件是你在开发过程中的**个人技能笔记真相源**。
> 每学完一个技术点，让 AI 帮你把"原理 + 项目用法 + 面试追问点"沉淀到本文件。
> 面试前快速翻阅本文件即可复习全部技术点。

---

## 笔记索引

> 按模块/主题组织，学一个补一个。

### 学习进度索引

**已沉淀**：

- [x] Docker 网络模型与端口映射（2026-08-25）
- [x] Maven 多模块工程与依赖管理（2026-08-25）
- [x] Java 包级元数据：package-info.java / 包级注解（2026-08-25）
- [x] Web 安全基础：日志注入 / 越权面 / 白名单校验（2026-08-26）
- [x] Git 提交/拉取标准动作与冲突规避（2026-08-26）
- [x] PostgreSQL 自增主键与序列（BIGSERIAL / sequence / nextval / setval）（2026-08-26）
- [x] Spring Boot 启动流程主线 + 缓存预热钩子（降维记忆版：`run()` 六步骨架 / 三个钩子钉位 / Tomcat 端口开启时点）（2026-09-03）
- [x] Spring Boot 自动装配机制（AutoConfiguration.imports）（2026-08-26；2026-09-03 标注它在 `run()` 的哪一步）
- [x] Spring Security + JWT 认证：无状态 vs 黑名单/登录态（登出/改密/禁用三种失效）+ 关 CSRF 原因 + @PreAuthorize 原理（2026-08-26；2026-09-02 修复「写而不读」断链）
- [x] RBAC vs ABAC + 权限进 JWT 的权衡（2026-08-26）
- [x] JWT 密钥管理与 fail-fast 校验（2026-08-27；2026-09-02 实战修复复盘）
- [x] 微服务身份传递的信任边界（双身份源问题）——含架构现实（gateway 未落地 / UMS 7101 直连）、**无网关 & 有网关断点级调用栈**（精确到文件:行号）、剥头与 HMAC 签名、三种形态对照表、断点自查清单（2026-08-27；2026-09-02 UMS-1 方案 A 落地复盘；2026-09-03 大幅增强链路图与逐行定位）
- [x] Refresh Token 安全：为什么必须轮换 + 一次性 jti 机制（2026-09-02，UMS-2）
- [x] Nacos 服务注册与配置中心（2026-08-26，配置中心待学）
- [x] Redis 三件事：登录失败锁定 + Token 黑名单 + 登录态缓存（防暴力破解 & 主动登出/踢人）（2026-08-26；2026-09-02 登录态缓存链路已接通；2026-09-03 补「单槽」模型、白/黑名单失效模式、数据类型选型与 TTL 粒度限制）
- [x] MDC 日志上下文 + TraceFilter：%X{traceId} 全链路日志串联（2026-09-02）
- [x] ThreadLocal 线程隔离与 remove 防串号（UserContext / MDC / SecurityContextHolder 共同底层）（2026-09-02）
- [x] Git 实操全流程：本地仓库推到 GitHub（首次对接：remote/push/代理443/PR/main默认分支/同步清理）（2026-09-02）

**待学习**：

- [ ] Spring Boot 3 与 Java 17 新特性
- [ ] Spring Cloud Gateway 与过滤器链
- [ ] OpenFeign 服务调用
- [ ] MyBatis-Plus 与数据权限拦截器
- [ ] PostgreSQL + PGVector 向量检索
- [ ] Spring AI 与模型适配器模式
- [ ] LangChain4j 与 Agent
- [ ] ReAct 与 Function Calling 原理
- [ ] RabbitMQ 异步任务与死信
- [ ] Redis 分布式锁（Redisson）/ 缓存穿透·击穿·雪崩
- [ ] Redis 内存淘汰策略（maxmemory-policy）与持久化（RDB/AOF）对「登录态丢失」的影响
- [ ] Redis Pipeline / Lua 脚本（本项目待解决：`INCR`+`EXPIRE` 竞态）
- [ ] Redis 底层编码（SDS / listpack / hashtable / skiplist）
- [ ] Sentinel 限流熔断
- [ ] Micrometer + Prometheus 可观测
- [ ] Docker / Docker Compose 部署
- [ ] Vue 3 + Vite + TypeScript（初步了解）

---

## 笔记正文

> 每学完一个点，在下方按模板追加。

<!--
### 模板：<技术点名称>
- 学于：<日期>
- 关联模块：<项目里的哪个服务>
- 核心原理：<3~5 句话讲清楚>
- 我在项目里怎么用的：<结合实际代码>
- 面试可能追问：<2~3 个问题 + 答案要点>
- 踩坑提醒：<1~2 条>
-->

---

## Docker 网络模型与端口映射

- 学于：2026-08-25
- 关联模块：基础设施（docker-compose 编排 RabbitMQ / PostgreSQL / Redis / Nacos / MinIO 等中间件）
- 来源：TD §18.2、ADR-11 / ADR-12

> 目标：搞懂 Docker 里「容器内端口 vs 宿主端口」两套端口体系，以及微服务之间到底走「内部网络」还是「宿主端口映射」——这是本项目中间件端口偏移（5673/5433 等）决策的根因。

### 直观类比（公寓楼，先建立直觉）

把**你的电脑（宿主机）**想象成一栋**公寓楼**。要装 RabbitMQ 消息中心，就在楼里**租一个独立房间（容器）**，把 RabbitMQ 放进房间里运行。

1. **房间内部的编号是固定的**：RabbitMQ 这个软件天生规定"我只在房间里的 **5672 号**接电话"（写死在镜像里）。在任何电脑、任何项目跑它，它永远在 5672 号接电话——这就是**容器内端口，永远不改**。

2. **大楼和房间是两套编号**：房间内是 5672 号，但大楼有自己对外的一套号。要让大楼外的人（你的 IDE、浏览器）找到这个房间，得在大楼前台登记一个对外接待号。所以 `- "5673:5672"` 的意思是：**"大楼前台挂个牌子：外面的 5673 号窗口 → 请转到房间里的 5672 号"**。左边 5673 = 宿主端口（可改），右边 5672 = 容器内端口（不改）。

3. **房间里的人互相打电话，根本不走前台**：楼里还住了 ums、kb、agent 等服务（各占一个房间），Docker 给楼里装了一条**内部电话线（内部网络）**。房间之间互相联系，**直接拨对方房间号就行，根本不去前台排队**。所以业务服务找 RabbitMQ 直接写 `rabbitmq:5672`（房间名 + 房间内编号），跟前台挂的 5673 号牌子一点关系都没有。

4. **三条结论用类比翻译**：
   
   - "本机 4.2 占 5672 不影响服务间通信" → 它占的是**前台 5672 窗口**，而服务间走**内部电话线**，不碰前台。
   - "宿主端口改 5673 避开冲突" → 前台 5672 被占了，我们另挂一个"5673 窗口 → 自己房间 5672"，互不干扰；前台窗口只给本机 IDE/浏览器调试用。
   - "版本锁 3.13 不复用 4.2" → 4.2 是**别人项目的房间**，进去用哪天就乱了，且 4.2 的"电话接口规则"（breaking changes）跟我们客户端对不上；自己单独租一个 3.13 房间，隔离、可复现、版本自己说了算。

> 一句话记忆：**容器之间走内部电话线（用容器内端口 5672）；只有你本机想连进去时，才走前台窗口（用宿主端口 5673）。**

### 核心原理

1. **每个容器有独立的网络命名空间**：容器内的进程只监听"容器内端口"，这个端口号由镜像内程序决定（如 RabbitMQ 固定监听 5672/15672），换任何宿主机、换任何项目都不应改它。

2. **容器与宿主机是两套端口体系**：`docker-compose.yml` 里 `ports: - "5673:5672"` 冒号左边是"宿主端口"，右边是"容器内端口"，两者完全独立、可以不同。映射的作用只是：让宿主机（及外部）能通过"宿主机 IP:宿主端口"转发到"容器:容器内端口"。

3. **docker-compose 会创建一个默认的内部网络**：同一 compose 文件里的所有服务自动加入这个网络，彼此能**直接通过「服务名 + 容器内端口」互相访问**（如 `rabbitmq:5672`），走的是 Docker 内部 bridge 网桥，**根本不经过宿主的任何端口映射**。

4. **关键结论（本项目决策的根因）**：宿主端口映射**只服务"宿主机直连"场景**（本机 IDE 直连调试、浏览器打开管理界面）；**微服务之间的通信完全走内部网络，与宿主端口无关**。所以本机已有 `rabbitmq:4.2` 占用宿主 5672/15672，**完全不影响我们服务间通信**，只要把我们项目自己的宿主映射端口改成 5673/15673 就彻底避开。

5. **容器名的 DNS 解析**：compose 内部网络内置 DNS，`rabbitmq` 这个服务名会被解析为容器 IP，因此业务服务配置里写 `host: rabbitmq`（服务名）+ `port: 5672`（容器内端口）即可。

### 我在项目里怎么用的

- docker-compose 里 RabbitMQ 写法（TD §18.2.4）：

```yaml
services:
  rabbitmq:
    image: rabbitmq:3.13-management      # 锁定 3.13，不复用本机 4.2
    container_name: insight-rabbitmq
    ports:
      - "5673:5672"      # 宿主 5673 → 容器 5672（AMQP，本机 IDE 直连用）
      - "15673:15672"    # 宿主 15673 → 容器 15672（管理界面）
```

- 微服务内部连接（走内部网络，用服务名 + 容器内端口）：

```yaml
spring:
  rabbitmq:
    host: rabbitmq     # compose 服务名
    port: 5672         # 容器内端口（不是 5673！）
```

- 本机 IDE 直连调试（不打容器、直接跑本地 jar）：

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5673         # 走宿主映射端口
```

- 同理，所有中间件宿主端口统一加偏移：PG 5433、Redis 6380、Nacos 8850/9850、MinIO 9010/9011、Prom 9091、Grafana 3001（TD §18.2.3），容器内端口全部保持不变。

- 版本锁定 3.13 的理由：项目环境隔离 + 可复现；RabbitMQ 4.0 有 breaking changes（移除 classic mirroring 等 deprecated 特性），不引入不确定性；本项目只用基础交换机/队列/死信，3.13 足够。

### 面试可能追问

- **Q1：`- "5673:5672"` 里两个数字分别是什么？能只改左边吗？**
  
  - 答：左边 5673 是宿主端口（本机对外暴露），右边 5672 是容器内进程监听的端口。容器内端口由镜像内程序固定，永远不改；宿主机端口可随意改，只要避开本机已占用端口即可。改映射不影响容器内程序，也不影响容器间通信。

- **Q2：两个容器（比如我们的 `ums` 服务和 `rabbitmq`）不在同一个 compose 文件里，还能用服务名互访吗？**
  
  - 答：默认不行。compose 会为每个项目创建一个隔离网络，不同 compose 文件（或不同项目）之间网络默认隔离，服务名 DNS 不互通。需要跨 compose 通信时，要显式 `docker network create` 创建共享网络，两边都 `networks: - 该网络` 加入；或走宿主机端口映射 + `host.docker.internal` / 宿主机 IP。

- **Q3：为什么你选 `rabbitmq:3.13-management` 而不是复用本机已有的 4.2 容器？**
  
  - 答：三点——① 环境隔离，本项目自带 compose 能一键拉起独立环境，不依赖也不污染本机其他项目的容器；② 版本受控可复现，任何人 `docker compose up` 得到的行为一致；③ RabbitMQ 4.0 有 breaking changes（移除 classic queue mirroring 等），而 Spring AMQP 3.1.x（Boot 3.2 对应）官方对齐 3.13，锁 3.13 规避不确定性。

- **Q4（进阶）：容器间走内部网络和走宿主端口映射，性能/隔离上有区别吗？**
  
  - 答：有。内部网络是 bridge 网桥直连，不经过宿主机端口 NAT 转发，路径更短、无端口冲突风险、默认不暴露到公网更安全；宿主端口映射则会把服务暴露到宿主机所有网卡（可用 `127.0.0.1:5673:5672` 绑定回环收紧）。所以"服务间通信走内部网络、宿主端口仅用于调试"是更干净也更安全的做法。

### 踩坑提醒

1. **坑：误把宿主端口写进服务间连接串（写成 `rabbitmq:5673`）**
   
   - 现象：容器内服务启动连 MQ 失败/超时，而本机 IDE 直连却正常。
   - 规避：牢记「容器内通信用 **服务名 + 容器内端口（5672）**，宿主机调试才用 **localhost + 宿主端口（5673）**」。用不同 profile 隔离：`application.yml` 走内部网络，`application-local.yml` 走 localhost + 宿主端口，绝不混用。

2. **坑：只改 compose 宿主端口，忘了本机 IDE 调试的 local profile 同步改**
   
   - 现象：改完 compose 宿主端口后，本地直连调试仍连旧端口，导致连不上或连到本机其他容器（如连错到本机 4.2）。
   - 规避：端口变更时，把「docker-compose 的 ports」「本地 local profile 的连接配置」「文档端口表（TD §18.2.3）」三处一起改，形成固定清单，避免只改一半。

3. **坑：版本不锁定（写 `rabbitmq:latest` 或复用本机旧容器）导致不可复现 / 协议不兼容**
   
   - 现象：换台机器或过了几周 `latest` 漂移到 4.x，触发 breaking changes（classic queue mirroring 被移除、部分特性行为变化），项目莫名报错、难以排查。
   - 规避：镜像**精确锁版本 tag**（`rabbitmq:3.13-management`），并让客户端版本与服务端对齐（Boot 3.2 → Spring AMQP 3.1.x → RabbitMQ 3.13）。中间件一律锁版本，杜绝 `latest`。

---

## 智擎 AI 工程架构与模块全景（Maven 多模块工程与依赖管理）

- 学于：2026-08-25
- 关联模块：整个 `insight-engine/` 工程骨架（阶段 1）
- 来源：TD §3、PRD §9.3

> 目标：把"一大堆模块"一次性讲清楚——**为什么这么分、每一层是干嘛的、谁依赖谁**。这是理解后续所有开发的地基。

### 直观类比（先建立直觉）

把整个工程想象成一家**公司大楼**，Maven 多模块就是"按职责分楼层、分部门"：

1. **父 POM（`pom.xml`）= 公司总部规章制度**：规定统一的"公司名（groupId）"、"产品版本（version）"、全局规章制度（统一 Java 版本、统一编码、统一第三方依赖版本）。它不是"干活的部门"，而是**给所有部门定规矩的顶层**。

2. **BOM（`insight-engine-dependencies`）= 公司的《统一采购目录》**：把所有要买的"零件"（依赖）的**型号（版本号）**列成一张表，各部门要用零件时**只报零件名、不报型号**，型号统一由采购目录定，避免"A 部门买 1.0、B 部门买 2.0"打架。

3. **common = 公司公共物品间**：放大家都要用的"通用工具"（统一返回格式 `Result`、错误码 `ErrorCode`、异常 `BizException`、分页 `PageResult`）。谁都能来取，但它**不依赖任何具体业务部门**。

4. **api = 公司《跨部门协作协议》**：规定 A 部门找 B 部门办事时，要填什么表单（DTO）、走哪个流程（Feign 接口）。目的是**解耦**——A 不用知道 B 内部怎么实现，只看协议。

5. **starter 系列 = 公司《标准能力包》**：把某个技术能力（Web 异常处理、MyBatis、Security、Redis…）**打包成一个"即插即用"的组件**，业务部门要哪项能力，引入对应的 starter 就自动生效，不用重复配置。

6. **modules = 公司各业务部门**：ums（用户）、model（模型）、kb（知识库）… 每个是一个**独立可运行**的微服务，是真正"干活、赚钱"的地方。

> 一句话记忆：**父 POM 定规矩，BOM 定版本，common 放公共件，api 定协作协议，starter 打包技术能力，modules 是干活的业务部门。**

### 核心原理（Maven 多模块的关键机制）

1. **父子继承（`<parent>`）**：子模块的 pom 里声明 `<parent>` 指向父 POM，就能**自动继承**父 POM 的 `groupId`、`version`、`properties`、`dependencyManagement`、`pluginManagement`。所以子模块的 pom 可以写得很短——只写自己独有的东西（artifactId + 自己用的依赖）。

2. **聚合（`<modules>`）**：父 POM 里用 `<modules>` 列出所有子模块，作用是**"一次构建全部"**——在父目录跑 `mvn clean install`，Maven 会按依赖顺序自动把所有子模块都构建一遍（拓扑排序）。

3. **`dependencyManagement` vs `dependencies` 的本质区别**：
   
   - `<dependencyManagement>`：**只声明版本，不真正引入**。写在这里的依赖，子模块要**再写一次**（不写版本号）才会真正引入。这是"版本统一管理"的核心。
   - `<dependencies>`：**真正引入依赖**，会传递到编译/运行 classpath。

4. **BOM（Bill Of Materials）的本质**：就是一个**只包含 `<dependencyManagement>` 的 pom**（`packaging=pom`），被别人用 `<scope>import</scope>` 引入后，它里面管理的所有版本号就"注入"到引入方。本项目父 POM import 了 Spring Boot/Cloud/Alibaba 三个官方 BOM + 自己的 `insight-engine-dependencies` BOM，四层版本统一。

5. **`packaging` 的三种形态**：
   
   - `pom`：聚合/父模块，**不产出 jar**，只是"组织者"（父 POM、BOM、starter 聚合、modules 聚合）。
   - `jar`：普通库/服务，产出 `.jar`。
   - `war`：Web 应用（本项目不用）。

6. **依赖方向单向、禁止循环（TD §3.2 铁律）**：
   
   ```
   modules/* ──> api ──> common
   modules/* ──> starter/* ──> common
   ```
   
   底层（common）**永远不依赖**上层（业务），否则形成循环依赖，Maven 会直接报错。这是分层架构不被"污染"的保证。

### 我在项目里怎么用的（每个模块干什么）

#### 第一层：组织/管理型模块（不产出业务代码）

| 模块                                 | packaging | 作用                                                     |
| ---------------------------------- | --------- | ------------------------------------------------------ |
| `insight-engine`（父 POM）            | pom       | 聚合全部子模块；统一 Java 17 / UTF-8；import 四个 BOM；统一编译插件配置      |
| `insight-engine-dependencies`（BOM） | pom       | 统一内部模块 + 三方补充依赖（MyBatis-Plus/JJWT/Knife4j/Hutool…）的版本号 |
| `insight-engine-starter`           | pom       | **聚合 8 个 starter**（本身无代码，只是父目录）                        |
| `insight-engine-modules`           | pom       | **聚合 gateway + 11 个业务服务**（本身无代码，只是父目录）                 |

> 关键理解：`insight-engine-starter` 和 `insight-engine-modules` 这两个目录里**只有 pom.xml、没有 src/**，它们不是"服务"，是"文件夹式的分组"。IDEA 里看到它们嵌套子模块是正常现象。

#### 第二层：公共基础模块（被所有人依赖）

| 模块                      | 关键类                                                                                | 作用                                                 |
| ----------------------- | ---------------------------------------------------------------------------------- | -------------------------------------------------- |
| `insight-engine-common` | `Result` / `ErrorCode` / `BizException` / `PageQuery` / `PageResult` / `Constants` | 纯 POJO，**零框架依赖**。统一响应体、错误码（IF 附录 A）、业务异常、分页封装、全局常量 |

#### 第三层：契约模块

| 模块                   | 作用                                                              |
| -------------------- | --------------------------------------------------------------- |
| `insight-engine-api` | Feign 接口 + 共享 DTO。**服务间只能通过这里调用**，禁止直接引对方内部类（阶段 3 起逐步填充各业务域的契约） |

#### 第四层：8 个技术能力 starter（即插即用）

| starter                 | 提供的能力                                                                                | 当前状态         |
| ----------------------- | ------------------------------------------------------------------------------------ | ------------ |
| `starter-web`           | **统一响应/异常/TraceID/用户上下文**（TraceFilter、UserContextFilter、GlobalExceptionHandler、自动装配） | ✅ 阶段 1 已实现   |
| `starter-mybatis`       | MyBatis-Plus、逻辑删除、审计填充、数据权限拦截器                                                       | 骨架（阶段 5 实现）  |
| `starter-security`      | Spring Security + JWT、方法级权限                                                          | 骨架（阶段 3 实现）  |
| `starter-redis`         | RedisTemplate、分布式锁、缓存防护                                                              | 骨架           |
| `starter-nacos`         | 服务注册 + 配置中心                                                                          | 骨架           |
| `starter-ai`            | Spring AI + LangChain4j 装配                                                           | 骨架（阶段 6 实现）  |
| `starter-mq`            | RabbitMQ 信封/确认/重试死信                                                                  | 骨架（阶段 7 实现）  |
| `starter-observability` | Micrometer + Prometheus                                                              | 骨架（阶段 11 实现） |

> 关键理解：starter 是"**能力包**"不是"服务"。业务服务（ums/kb…）要什么能力就 `依赖` 哪个 starter，能力自动生效。例如 ums 需要 Web + 异常处理 + 安全，就引 `starter-web` + `starter-security`。

#### 第五层：业务服务（modules，真正可运行的微服务）

| 服务          | 端口   | 一句话职责                 | 对应开发阶段       |
| ----------- | ---- | --------------------- | ------------ |
| `gateway`   | 7000 | 统一入口：路由、鉴权、限流、跨域      | 阶段 4         |
| `ums`       | 7101 | 用户、角色、权限、登录 JWT（RBAC） | 阶段 3 ★第一个微服务 |
| `workspace` | 7102 | 组织、工作空间、成员            | 阶段 5         |
| `model`     | 7103 | 模型网关：厂商/模型/路由/流式/计量   | 阶段 6 ★核心     |
| `kb`        | 7104 | 知识库：文档解析、切片、向量检索      | 阶段 7         |
| `agent`     | 7105 | Agent：ReAct、工具调用、版本   | 阶段 9 ★核心     |
| `tool`      | 7106 | 工具市场：内置/HTTP 工具       | 阶段 8         |
| `conv`      | 7107 | 对话：会话、消息、流式输出         | 阶段 10        |
| `billing`   | 7108 | 计费：配额、用量、账单           | 阶段 11        |
| `obs`       | 7109 | 监控审计：指标、调用链、审计日志      | 阶段 11        |
| `notify`    | 7110 | 通知：渠道、模板、投递           | 阶段 11        |

> 端口规律：网关 7000，业务服务 7101~7110（PRD §9.2）。这些都是**宿主端口**，容器内端口另有一套映射（见 Docker 笔记）。

### 面试可能追问

- **Q1：为什么要把依赖版本集中到 BOM，而不是各模块自己写版本号？**
  
  - 答：① 避免版本冲突——多个模块各自写 Spring Boot 版本，很容易出现 A 用 3.2、B 用 3.3 导致的类冲突/行为不一致；② 统一升级——升级一个依赖版本，只需改 BOM 一处，全工程生效；③ 一致性——保证所有模块跑在同一套版本底座上，可复现。

- **Q2：`dependencyManagement` 和 `dependencies` 有什么区别？什么时候用哪个？**
  
  - 答：`dependencyManagement` 只**声明版本、不真正引入**，子模块仍需在 `<dependencies>` 里写一次（省略 version）才会生效；`dependencies` 是**真正引入**。父 POM 用前者统一版本，子模块用后者声明自己实际要用的依赖。

- **Q3：为什么要把公共代码拆成 common / api / starter 三层，而不是都塞进一个 common？**
  
  - 答：职责不同——`common` 是零框架依赖的纯 POJO（被最广泛复用，甚至网关 WebFlux 栈也能用）；`api` 是服务间 Feign 契约（带 Spring Cloud 依赖，用于解耦服务调用）；`starter` 是带自动装配的技术能力包（"引了即用"）。拆开能控制依赖传播范围，避免"引一个工具类却被迫引入整个 Spring Web/Security 全家桶"。

- **Q4：为什么 `common` 不能依赖 Spring Web？**
  
  - 答：因为 `gateway` 是 WebFlux（响应式）栈、业务服务是 WebMvc（Servlet）栈，两者的 Web 体系不同。common 若依赖了 Servlet 栈的 Spring Web，就会污染 gateway，导致依赖冲突。保持 common 纯 POJO，让它在两种栈下都能安全复用。

### 踩坑提醒

1. **坑：XML 注释里写了 `--`（两个连字符）导致 POM 解析失败**
   
   - 现象：`mvn` 报 `Non-parseable POM ... in comment after two dashes (--)`，整个工程构建不起来。
   - 规避：XML 注释内**禁止出现连续两个连字符 `--`**（XML 规范规定注释中不能含 `--`）。写 `--release`、`--spring` 这类词时，改写为 `release=17` 或空格分隔。

2. **坑：BOM 只锁了 `hutool-all`，子模块用 `hutool-core` 报版本缺失**
   
   - 现象：`dependencies.dependency.version for cn.hutool:hutool-core is missing`。
   - 规避：BOM 里要**把同组下用到的每个 artifactId 都锁一遍**（`hutool-all` 和 `hutool-core` 都要），不能只锁"全家桶"就以为子模块引任意子包都有版本。

3. **坑：把聚合模块（`insight-engine-starter`/`insight-engine-modules`）当成"服务"，在 IDEA 里找启动类找不到**
   
   - 现象：在这两个目录里找 `main` 方法找不到，以为工程没生成完整。
   - 规避：记住这两个是 `packaging=pom` 的**分组目录**（只有 pom.xml、无 src），真正的启动类在它的子模块里（且阶段 1 骨架阶段还没写启动类，阶段 3 UMS 才有）。

---

## Java 包级元数据：package-info.java / 包级注解

- 学于：2026-08-25
- 关联模块：`insight-engine-api`（跨服务契约层）的 `package-info.java`
- 来源：实际开发踩坑（IDEA 对空包 package-info 的识别问题）

> 目标：讲清楚一个**很多人用了多年 Java 却从没注意过的冷知识**——`package-info.java` 到底是干嘛的、为什么它这么"特殊"、有什么规范约束。这次是踩了 IDEA 的坑才被迫搞懂的。

### 直观类比（先建立直觉）

把 Java 的"包（package）"想象成**一个文件夹/部门**，里面的 `.java` 文件是这个部门的"员工"（每个员工 = 一个类/接口）。

问题是：**部门本身**（不是员工）如果要写一句话简介、贴一张标签，写在哪？

Java 的答案就是 `package-info.java`——它是**专门给"部门（包）"写简介和贴标签的文件**。它不是"员工"，是"部门的门牌/简介牌"。

| 对象        | 对应文件                    | 能写什么                        |
| --------- | ----------------------- | --------------------------- |
| 员工（类/接口）  | `Xxx.java`              | 类的字段、方法、逻辑                  |
| **部门（包）** | **`package-info.java`** | **包的简介（Javadoc）+ 包的标签（注解）** |

> 一句话记忆：**类用 `Xxx.java` 描述自己，包用 `package-info.java` 描述自己。一个包只能有一个 `package-info.java`，就像一个人只有一个身份证。**

### 核心原理（它到底特殊在哪）

1. **文件名是"保留名"**：`package-info.java` 和 `module-info.java` 一样，是 Java 编译器（javac）**认名字**的特殊文件。你把它放在某个包目录下，javac 就知道"哦，这是描述这个包的元数据文件"，而不是普通的类。

2. **它天生只能承载两种东西**（这是约定，也是它的本分）：
   
   - **包级 Javadoc**：`package-info.java` 顶部的 `/** ... */` 注释，`javadoc` 工具会把它生成为"包说明页"（类似每个类的说明页，但对象是整个包）。
   - **包级注解**：写在 `package xxx;` 语句上面的注解，用来给"整个包"打标签。

3. **编译产物很特殊**：`package-info.java` 编译后生成 `package-info.class`，但这个 class **是"空壳"**——它没有任何方法、字段、业务字节码，只负责在 class 文件里保存包级注解，供程序运行时通过反射读取。

4. **包级注解是它最有价值的功能**：这是普通 `.java` 文件**做不到**的事。因为注解要么打在类上、方法上、字段上，要么打在"包"上——而"打在包上"的唯一载体，就是 `package-info.java`。

### 三种标准用法（都来自真实项目）

**① 包级 Javadoc（最基础）**——JDK 源码 `java.util` 包就有：

```java
/**
 * Contains the collections framework, ...
 * 中文：包含集合框架、日期时间、随机数等工具类
 */
package java.util;
```

**② 包级注解（Spring 大量使用）**——`org.springframework.util` 包：

```java
@NonNullApi      // 整个包的方法参数/返回值默认非空
@NonNullFields   // 整个包的字段默认非空
package org.springframework.util;
```

这样声明后，该包下所有类的空值检查策略就统一了，不用每个类重复写 `@NonNull`。

**③ 包级版权/license 声明**——企业项目常用：

```java
/**
 * Copyright (C) 2026 InsightEngine. All rights reserved.
 */
package com.insightengine.api;
```

### 我在项目里怎么用的

`insight-engine-api` 模块的 `package-info.java` 用的是**① 包级 Javadoc**——用它说明 api 模块（契约层）的职责和约定：

```java
/**
 * 跨服务契约层（api）。
 *
 * <p>本包承载 OpenFeign 客户端接口与共享 DTO（TD §3.2 / §8.2），
 * 是服务间调用的唯一契约。...</p>
 */
package com.insightengine.api;
```

**踩坑全过程**（这段经历本身就是最好的学习材料）：

1. 骨架阶段 api 模块只有 `package-info.java`（无任何类/接口），IDEA 把它显示成灰色 + 带 `.java` 后缀、不识别为 Java 文件；
2. 一开始我以为是 IDEA 缓存问题 → 删 `.idea` 重建 → 没用；
3. 用户提出"在 package-info.java 里写个类让它完整" → 我凭"javac 会禁止"的错误认知反驳 → **实测证明 javac 其实允许**；
4. 但最终回到企业规范判断：**javac 允许 ≠ 规范推荐**，`package-info.java` 里写类型是"为工具而污染代码"，最终保持它只含 `package` + Javadoc 的纯粹写法。

### 面试可能追问

- **Q1：`package-info.java` 和普通 `.java` 文件有什么本质区别？**
  
  - 答：① 文件名是 javac 认的"保留名"，一个包只能有一个；② 它承载的是**包级**元数据（包级 Javadoc + 包级注解），普通文件承载的是**类级**内容；③ 编译产物 `package-info.class` 是"空壳"，只存包级注解，无业务字节码。

- **Q2：包级注解（package-level annotation）有什么实际用途？能举例子吗？**
  
  - 答：核心价值是"对整个包统一声明"。典型例子是 Spring 的 `@NonNullApi`——在 `package-info.java` 上打一次，整个包的方法参数/返回值就默认非空，省去每个方法重复写 `@NonNull`，也统一了团队的 null 约定。另一个例子是 `@Deprecated`、`@SuppressWarnings` 也能打在包上（虽然少见）。

- **Q3：为什么说"javac 允许 package-info.java 里写类型，但规范不推荐"？**
  
  - 答：javac 把 `package-info.java` 当普通源文件解析，技术上确实能编译类型声明；但 `package-info.java` 的**语义本分**是包级元数据，往里塞类会让"说明书"和"零件"混在一起，破坏单一职责，也会让 code review、静态分析工具（SonarQube/Checkstyle）告警。工程规范追求的是"每个文件职责清晰"，而不是"javac 能编过就行"。

### 踩坑提醒

1. **坑：把包级文档写在某个类的注释里，而不是 package-info.java**
   
   - 现象：包里有多个类，每个类头顶都写一段"本包是干嘛的"重复说明，既不统一也难维护。
   - 规避：包的整体说明统一放 `package-info.java` 顶部 Javadoc，类的说明才放各自的类头，职责分开。

2. **坑：给包打注解时，注解写在 import 语句后面 / package 语句下面**
   
   - 现象：包级注解无效或编译报错。
   - 规避：包级注解必须写在 `package xxx;` 语句**正上方**（紧贴），顺序是：`/** 文档 */` → 注解 → `package 语句`。写错位置注解就变成打在"类"上而不是"包"上。

3. **坑：IDE 里 `package-info.java` 显示灰色/带后缀，误以为文件坏了去"修"它**
   
   - 现象：新建的包只有 `package-info.java`（尚无任何类）时，IDEA 可能不把它识别为 Java 文件，显示灰色带后缀。
   - 规避：这是"空包"的过渡现象，无害；等包里有了真实的类/接口，IDEA 自动恢复正常。**不要**为了 IDEA 显示去在 `package-info.java` 里塞类或加"类型锚点"这种 hack——那是为工具而污染代码。

---

## 微服务间通信：Nacos 服务发现 vs 传输网络

- 学于：2026-08-26
- 关联模块：服务治理（Nacos 注册中心 + OpenFeign 服务调用）
- 来源：TD §8

> 目标：回答一个很多人绕晕的问题——"微服务还没部署到 Docker，在宿主机上直接跑，它们之间是怎么互相找到对方的？"答案是：**靠 Nacos，不靠 Docker 网络**。

### 直观类比（延续公寓楼：总机查号台）

上一节讲的是"电话线怎么铺"（传输网络），这一节讲"怎么查到对方的分机号"（服务发现）——**两者是两码事**。

延续公寓楼的比喻，现在楼里住的不只是中间件，还有你的各个微服务（ums、agent、conv…），它们是楼里的不同**部门/住户**。

1. **部门入驻时主动登记（服务注册）**
   每个部门搬进来（服务启动）时，会主动跑到大楼前台说："我是客服部，现在在 3 楼 301，分机号 7101。"前台把这些记在一本**通讯录**里。
   ——这个"前台 + 通讯录"就是 **Nacos**。

2. **部门之间办事先查号（服务发现）**
   销售部（agent）要找客服部（ums）办事，它**不需要背下客服部的分机号**，只要拨通前台问一句："客服部现在在几号？"前台查通讯录，告诉他"3 楼 301，分机 7101"，销售部再直接打过去。
   ——这一步"查号"就是**服务发现**。

3. **查号机制和电话线怎么铺，是两回事（两层拆解）**
   
   - "怎么查到对方号码" = **发现层** = Nacos，跟 Docker 无关
   - "查到号码后怎么把电话打通" = **传输层** = 网络（宿主机网络 or Docker 内部网络）

> 一句话记忆：**Nacos 是"查号台"（告诉你对方在哪），网络是"电话线"（帮你把话送过去）。查号永远靠 Nacos；电话线在宿主机是 localhost、在 Docker 是内部网络。**

### 核心原理（两层拆解）

| 层次  | 回答的问题             | 技术    | 依赖 Docker 吗 |
| --- | ----------------- | ----- | ----------- |
| 发现层 | 我要调的服务 IP 和端口是多少？ | Nacos | ❌ 无关        |
| 传输层 | 拿到地址后数据怎么送过去？     | 网络    | ✅ 有关        |

**宿主机本地跑（不打容器）**：所有服务是宿主机上的普通 Java 进程，共享同一个 localhost。

- 注册：`insight-engine-ums = 127.0.0.1:7101`（服务启动时自己报到 Nacos）
- 调用：conv 要调 agent → 拿服务名 `insight-engine-agent` 去 Nacos 查 → 拿到 `127.0.0.1:7105` → HTTP 直连

**容器部署（打容器）**：每个服务在独立容器里，有各自的内网 IP。

- 注册：`insight-engine-ums = 172.17.0.x:8081`（容器 IP）
- 调用：**一样走 Nacos 查号**，只是查到的是容器地址，走 Docker 内部网络送过去

**结论**：发现层（Nacos）两种场景**完全一样**，变的只是传输层（地址 + 网络）。这就是为什么"微服务还没部署到 Docker，宿主机直接跑也能互相通信"——因为通信靠 Nacos 查号 + localhost 直连，跟 Docker 没关系。

### 我在项目里怎么用的

- 服务注册（application.yml）：

```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_ADDR:127.0.0.1:8850}   # 本地连宿主映射端口 8850
```

- 服务调用（OpenFeign，只写服务名不写 IP）：

```java
@FeignClient(name = "insight-engine-kb")   // 名字 = 注册到 Nacos 的服务名
public interface KbClient {
    @PostMapping("/api/v1/kb/{kbId}/retrieve")
    Result<RetrieveResult> retrieve(...);
}
```

- 网关路由（用 lb:// 走负载均衡）：

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: ums
          uri: lb://insight-engine-ums    # lb:// 表示"去 Nacos 查这个服务名"
```

- 中间件（RabbitMQ/Redis/PG）**不注册 Nacos**，所以只能写死地址：本地 `localhost:5673`、容器 `rabbitmq:5672`（呼应上一节笔记）。

### 面试可能追问

- **Q1：微服务在本地开发时（不打容器）怎么互相调用？**
  
  - 答：靠 Nacos 服务发现 + OpenFeign。每个服务启动时把 `服务名 → IP:端口` 注册到 Nacos；调用方在代码里只写服务名（`@FeignClient`），运行时 OpenFeign 拿服务名去 Nacos 查实例列表，用 LoadBalancer 选一个，HTTP 直连。全程走 localhost，跟 Docker 无关。

- **Q2：容器化后，服务注册到 Nacos 的 IP 是容器内网 IP，会不会访问不到？怎么解决？**
  
  - 答：会，这是经典坑。容器 IP（`172.17.0.x`）只在 Docker 网络内可达，宿主机或其他网络访问不到。解决三选一：① `spring.cloud.nacos.discovery.ip` 显式指定注册为宿主机可达 IP；② 指定注册网卡 `network-interface`；③ 容器用 `network_mode: host`（简单但牺牲隔离）。

- **Q3：Nacos（服务发现）和 OpenFeign（服务调用）到底各负责什么？**
  
  - 答：Nacos 只负责"谁在哪"——维护 `服务名 → 实例地址列表` 的映射 + 健康检查（踢掉不健康实例），它**不传业务数据**；OpenFeign 负责"按名字发 HTTP 请求"——底层用 LoadBalancer 从 Nacos 拿到的实例列表里挑一个，发起真实 HTTP 调用。两者配合才实现"按服务名调用"。

### 踩坑提醒

1. **坑：本地跑时 NACOS_ADDR 配成容器内端口（8848）而不是宿主映射端口（8850）**
   
   - 现象：服务启动报"连接 Nacos 超时"，注册不上去。
   - 规避：本地直连 Nacos 用 `localhost + 宿主映射端口（8850）`；只有容器里的服务才用 `服务名 nacos + 容器内端口（8848）`。和 RabbitMQ 一个道理。

2. **坑：容器化后没指定注册 IP，注册成容器内网 IP，导致服务互调失败**
   
   - 现象：Nacos 控制台能看到实例，但调用超时/连不上。
   - 规避：显式配置注册 IP 或网卡，确保注册出去的是"其他服务可达"的地址。

3. **坑：Feign 的 name、网关 lb:// 后的名字、Nacos 里的服务名三者不一致**
   
   - 现象：`Load balancer does not have available server for client: xxx` 找不到实例。
   - 规避：三处名字必须完全一致（通常都等于 `spring.application.name`），大小写也严格区分。

---

## Web 安全基础：日志注入、越权面与白名单校验

- 学于：2026-08-26
- 关联模块：`starter-web` 的 `TraceFilter` 与 `UserContextFilter`
- 来源：TD ADR-5

> 目标：讲透三条安全常识——**日志注入、越权面、白名单校验思想**。它们共用一条主线：**永远不要无条件信任客户端可控的输入。**

### 直观类比（门口签到本 + 员工证）

**类比①：TraceFilter = 门口签到本**

前台让访客在签到本写名字，然后**原样照抄**到公告栏（日志）和回执单（响应头）。如果前台不检查，访客可以：

- 名字里塞"换行符" → 在公告栏上**多写一行假消息**（日志注入）
- 名字里塞"终端控制码" → 让监控屏**花屏 / 清屏 / 隐藏关键行**（ANSI 注入）
- 写个几百万字的超长名字 → 前台抄到手断、公告栏被撑爆（资源耗尽 DoS）

修复 = 前台立规矩：名字只能由「字母 + 数字 + 连字符」组成、最多 64 字符，不合规就现场**重发一个新工牌**（`fastSimpleUUID()`）。

**类比②：UserContextFilter = 员工证**

为了省事，决定：大门口保安（网关）验过身份证后，给每人发一张**手写纸条**（明文 header）写着"张三，管理员"，大楼内部各部门（业务服务）看到纸条就信，不再查身份证。

风险：如果有人**不走大门、翻墙/走后门直接到部门门口**（绕过网关直连业务服务），他就能**自己伪造纸条**："我是老板，超级管理员"——部门一看就信了 → **越权**。

### 核心原理

**1. 不可信输入（一切的前提）**

请求头、URL 参数、请求体……凡是"从外部进来"的数据，都是**客户端完全可控**的（任何人拿个脚本就能随便填）。所以 `X-Trace-Id`、`X-User-Id` 这些 header 的值，本质上和用户输入一样"脏"，**不能直接信任**。

**2. 日志注入（Log Injection）**

- `%0d%0a` 是 URL 编码的 `\r\n`（回车换行）。traceId 里含它，日志就"换行"，攻击者可**伪造一行假日志**，误导运维/审计、把真实攻击埋在伪造的海量日志里绕过告警。
- **ANSI 转义序列**（如 `\u001b[31m` 变红、`\u001b[2J` 清屏、`\u001b[8m` 隐藏文字）：运维用 `tail`/`cat` 看日志时，终端被操纵。
- 本质：**把"日志内容"变成了"日志结构/终端指令"**。

**3. 资源耗尽（DoS）**

超长字符串被原样塞进 `MDC`、响应头、日志 → 内存暴涨、每条日志都带超长串（磁盘打爆）、响应头超大（带宽浪费）。

**4. 白名单 vs 黑名单校验（关键思想）**

| 方式      | 做法                 | 问题                      |
| ------- | ------------------ | ----------------------- |
| 黑名单     | 列出"不允许的字符"，逐个过滤    | 控制字符成千上万，**容易漏**，而且难枚举全 |
| **白名单** | 只允许"明确安全的字符集"，其余全拒 | **从根上杜绝**，安全、简单、可预测     |

结论：**校验一律用白名单，禁用黑名单**。`[A-Za-z0-9-]{1,64}` 就是白名单——只放行字母/数字/连字符，`\r\n`、ANSI、超长串天然进不来。

**5. 越权面（水平 + 垂直）**

- **水平越权**：伪装成别人，访问别人的数据（伪造 `X-User-Id: 2` 看别人的知识库）。
- **垂直越权**：普通用户伪装成管理员（伪造 `X-Roles: super_admin`）。
- 根因：把"**我是谁**"的决定权交给了客户端——只要客户端能绕过可信边界（网关），就能任意指定身份。

### 我在项目里怎么用的

**① `TraceFilter`（白名单校验 + 非法重生成）**

```java
private static final Pattern TRACE_ID_PATTERN = Pattern.compile("[A-Za-z0-9-]{1,64}");

String traceId = request.getHeader(Constants.HEADER_TRACE_ID);
// 请求头完全可控，非法/缺失一律丢弃并重新生成，绝不信任用户输入
if (traceId == null || !TRACE_ID_PATTERN.matcher(traceId).matches()) {
    traceId = IdUtil.fastSimpleUUID();   // 只含字母数字，天然安全
}
MDC.put("traceId", traceId);
response.setHeader(Constants.HEADER_TRACE_ID, traceId);
try { filterChain.doFilter(request, response); }
finally { MDC.remove("traceId"); }   // 防线程复用串号
```

**② `UserContextFilter`（解析明文身份头）**

`UserContextFilter` 是「走 TD ADR-5 网关明文头方案」时用的过滤器，解析 `X-User-Id`/`X-Tenant-Id`/`X-Roles` 组装 `LoginUser`。**2026-09-02 起默认不再注册**（`@ConditionalOnProperty(insight.web.trust-gateway-headers=true)` 才装配）——服务身份只信 `JwtAuthFilter` 解析的 JWT，防止业务服务无条件信任客户端可伪造的明文头造成越权。`UserContext` 的清理职责也随之下移到 `JwtAuthFilter` 的 finally（写入方负责清理，防线程串号）。走网关方案的服务显式开开关即可（完整拆解与方案对比见《微服务身份传递的信任边界》）。

越权面的防护手段（三选一）：

| 方案          | 做法                                | 代价              |
| ----------- | --------------------------------- | --------------- |
| 网络层端口隔离     | 业务服务端口（7101~7110）不对外暴露，只放网关访问     | 运维成本            |
| 代码层 HMAC 签名 | 网关下发 `X-User-Sign` 签名头，业务服务共享密钥验签 | 加一层签名逻辑         |
| IP 网段校验     | 业务服务只信任来自网关 IP 的请求                | 最便宜，网关 IP 变了要维护 |

### 面试可能追问

- **Q1：什么是日志注入？如何防护？**
  
  - 答：攻击者在可写进日志的字段（如 traceId、用户名）里注入 `\r\n` 换行或 ANSI 转义序列，伪造假日志行、操纵终端显示、绕过日志审计。防护：**对写日志的字段做白名单校验**（只允许安全字符集），或写日志前做转义/清洗，绝不原样输出用户可控内容。

- **Q2：输入校验用白名单还是黑名单？为什么？**
  
  - 答：优先白名单。黑名单要枚举"所有非法字符"，控制字符种类多、容易漏；白名单只声明"哪些是安全的"，其余一律拒绝，从根上杜绝、可预测、可测试。本项目 traceId 就用 `[A-Za-z0-9-]{1,64}` 白名单。

- **Q3：微服务里"网关解析 JWT 后下发明文身份头"有什么风险？如何缓解？**
  
  - 答：风险是业务服务若被绕过网关直连，攻击者可伪造 `X-User-Id`/`X-Roles` 头实现水平/垂直越权。缓解三选一：① 网络层端口隔离（业务服务不对外暴露）；② 身份头加 HMAC 签名（`X-User-Sign`），业务服务验签；③ 最低成本 IP 网段校验（只信网关 IP）。

### 踩坑提醒

1. **坑：用黑名单过滤控制字符（只挡了 `\r\n`，漏掉 ANSI 转义）**
   
   - 现象：日志仍能被 ANSI 序列操纵，或换行过滤不彻底。
   - 规避：**直接上白名单正则**，只允许字母数字等安全字符，不枚举"非法字符"。

2. **坑：业务服务端口直接暴露，绕过网关后伪造身份头越权**
   
   - 现象：内网被攻破或端口误暴露后，攻击者直连业务服务伪造 `X-Roles: super_admin`。
   - 规避：业务服务端口只对内网/网关开放；身份头加 HMAC 签名或 IP 校验，不要"无条件信任明文头"。

3. **坑：日志/响应里回显用户可控输入未做任何过滤**
   
   - 现象：不仅是 traceId，用户名、搜索词等写进日志时，同样存在日志注入面。
   - 规避：凡是"会进日志/响应头的用户输入"，统一做白名单校验或转义，养成"外部输入不可信"的条件反射。

---

## Git 三世界模型与本项目分支策略（GitHub Flow 视角）

- 学于：2026-08-26（2026-09-02 **重写**）
- 关联模块：整个项目的版本管理
- 来源：DEVGUIDE §9.1-§9.2；本节重写——原版命名错位（把 `dev` 当 GitHub Flow 的 `master` 用，又把 `dev-xuy` 用 Git Flow 命名），导致「git push origin dev-xuy 是不是推到远程 dev」一类根本性误解；本文按本项目实际 GitHub Flow 重写，配套给 IDEA 端标准动作

> 目标：彻底搞懂 Git **三个对象**（远程分支 / 本地分支 / 远程跟踪分支快照）的对应关系；明确本项目用 **GitHub Flow**（不用 develop），给出 IDEA2026 标准动作；附带澄清 3 个最致命的命名错觉。

### 一、破一个最常犯的误解（先看再学）

很多人下意识认为「远程的 dev 和本地的 dev 是同一分支的镜像，所以远程的 dev-xuy 和本地的 dev-xuy 也是镜像」。**这个直觉错得离谱**：

- 「远程 dev」是远程仓库里**叫 dev 的那条独立分支**；
- 「本地 dev」是你本地**也叫 dev 的另一条独立分支**；
- 它俩**同名，但不是同一个对象**——通过 push/pull 同步数据；
- **「远程 dev-xuy」和「远程 dev」没有任何从属关系**——它们是两条名字不同、内容独立的分支；
- 「本地 dev-xuy」只跟「远程 dev-xuy」才是「镜像关系」（同名+同步）。

> 一句话记忆：**「同名 ≠ 同一对象」，「不同名 ≠ 没关系」**。所有 Git 误解的根源都来自对名字的过度脑补。

### 二、Git 三个世界（核心心智模型）

Git 有三个独立存在的分支空间，永远不要把它们混为一谈：

| 空间 | 路径 | 比喻 | 何时变化 | 谁来改 |
|---|---|---|---|---|
| **远程分支** | 远程仓库里 `refs/heads/xxx` | 公司文件柜 | 只有 push 后 | 你 push 之后 |
| **本地分支** | 本地 `.git/refs/heads/xxx` | 桌上正在写的文件 | 你 commit / checkout / merge 时 | 你自己 |
| **远程跟踪分支（快照）** | 本地 `.git/refs/remotes/origin/xxx` | 桌上贴的便签 | 只有 fetch / pull / push 时被刷新 | git 自动 |

**三个铁律**（解决"到底推哪个"的全部困惑）：

1. **`git commit` 只改"桌上文件"（本地分支），便签（快照）纹丝不动。**
2. **`git push` 推的是"桌上文件"（本地分支）**——成功后 git 顺手把便签刷新成最新。
3. **便签只在 `fetch` / `pull` / `push` 三个动作时被动刷新，平时永远静止。**

### 三、本项目用哪种分支模型（GitHub Flow）

按 DEVGUIDE §9.1 定稿策略——**GitHub Flow**（适合单人 MVP / 持续部署）：

```
远程 origin：
  master（永久稳定主干，唯一发布线）
  ├── feature/infra-docker-compose（阶段 2，已合并归档）
  └── feature/ums-auth（当前 UMS 阶段）
```

**为什么不用 develop / dev**：

- 单人 MVP 没有「多人并行特性排队等发版」的诉求；
- develop 在这里只是冗余中间层——你每次合到 develop，最终还是要合到 master；
- GitHub Flow = 一个主干走到底，少一层 = 少一处出错；
- 之前笔记里把 `dev` 当 GitHub Flow 的 `master` 用、又用 Git Flow 命名 `dev-xuy`——这是任何标准模型都不存在的混乱混搭，已废弃。

### 四、标准动作（按本项目 GitHub Flow）

#### ① 每天开工前：拉最新主干

```bash
git checkout master
git pull origin master          # 远程 master → 本地 master（保持同步）
git checkout feature/ums-auth   # 切回你的功能分支
git merge master                # 把最新 master 合到功能分支（提前化解冲突）
```

> 等价简写：`git checkout feature/ums-auth && git pull origin master`（pull 后会自动 merge 到当前分支）。

#### ② 开发中：小步、频繁提交

```bash
git add .
git commit -m "feat(ums): 完成登录接口"   # 一个小功能一个 commit
```

> 遵循 DEVGUIDE §9.2：每个任务一个 commit；类型 `feat` / `fix` / `docs` / `refactor`。

#### ③ 收工推送前：先拉后推（减少冲突的关键）

```bash
git checkout feature/ums-auth
git pull origin master          # 拉最新（这是「后推前的拉」，关键）
git push origin feature/ums-auth # 推本地 feature/ums-auth 到远程 feature/ums-auth
```

> 这条 push 命令**永远是「本地同名分支 → 远程同名分支」**——和 master 分支毫无关系！

#### ④ 合入主干：GitHub 网页提 PR

```
GitHub → New Pull Request：
  base: master  ←  compare: feature/ums-auth
  → Review → Merge pull request → Delete branch（远程 feature 删除）
```

#### ⑤ 清理本地分支

```bash
git branch -d feature/ums-auth                  # 本地删除（PR 合入后才能 -d，不能 -D）
git remote prune origin                         # 清理本地对已删除远程分支的快照
```

### 五、消除三个常见误解（必看，否则一辈子绕晕）

**误解 1：「`git push origin dev-xuy` 是把代码推到远程 dev 分支」**

- **错！** 这是把**本地 dev-xuy** 推到**远程 dev-xuy**（同名推送，独立的远程分支）。
- 跟远程 dev 分支**没有任何关系**——远程 dev 只能通过 `git push origin develop` 或合入 develop 才更新。
- 这是命名混乱最容易导致的错觉，也是原笔记导致的核心误解。

**误解 2：「本地 master 和 origin/master 是同一个东西」**

- **错！** 本地 master 是**本地独立分支**，origin/master 是**本地保存的远程快照**；
- 它俩通过 `git pull origin master`（= fetch + merge）保持大致一致，但**是两个独立对象**；
- 类比：本地 master 像「你复印了一份公司 master」；origin/master 像「你桌上贴的便签，写着公司 master 上次长啥样」。

**误解 3：「`git pull` 等于同步所有远程分支」**

- **错！** `git pull origin <branch>` 只同步**一个分支**——把指定远程分支的最新 commit 拉下来并合到当前分支；
- 想只更新快照不合并，用 `git fetch --all`（只刷新 origin/xxx 便签，不动工作分支）。

### 六、Git Flow 对照（如果你将来进用 Git Flow 的公司）

Git Flow 多一条永久分支 `develop`（开发集线器）：

```
远程 origin（Git Flow）：
  master（永久，只存发布版，每次合并打 tag v1.0、v1.1）
  develop（永久，日常开发集线器，所有 feature 先合到这里）
  release/x.y.z（临时，发版前修 bug）
  hotfix/x.y.z（临时，线上紧急修复，从 master 切）
```

**功能分支做完的标准动作**（与 GitHub Flow 关键区别）：

```bash
# ① 开工：从 develop 切功能分支
git checkout develop
git pull origin develop
git checkout -b feature/ums-auth

# ② 写代码、提交
git add .
git commit -m "feat(ums): xxx"

# ③ 功能完工：先把 develop 最新合到功能分支
git checkout feature/ums-auth
git pull origin develop

# ④ 把功能合回 develop（不是 push feature/ums-auth！）
git checkout develop
git merge feature/ums-auth
git push origin develop             # 推本地 develop，不是 feature

# ⑤ 删除功能分支
git branch -d feature/ums-auth
git push origin --delete feature/ums-auth
```

**GitHub Flow vs Git Flow 关键区别**：

| | GitHub Flow（本项目） | Git Flow |
|---|---|---|
| 永久分支 | master 一条 | master + develop |
| feature 从哪切 | master | develop |
| feature 合到哪 | master（PR） | develop |
| release 分支 | 无（用 tag） | 有 |
| 适用 | 持续部署 / 单人 MVP | 定期发版 / 多版本并行 / 多人协作 |

### 七、冲突的产生与解决

**冲突的本质**：两个分支改了同一文件的同一行，Git 无法自动判断保留谁。

**典型场景**：你 push 之前没拉最新；别人已经推了新提交覆盖同一行。

**解决套路**：

```bash
# 推送被拒：rejected ... non-fast-forward
git pull origin feature/ums-auth   # 把远程同名分支合进来（可能产生冲突）
# IDEA 里三栏对比：<<<<<<< HEAD / ======== / >>>>>>> origin/feature/ums-auth
# 手动选择保留的代码，删除标记符
git add .
git commit -m "merge: 解决冲突"
git push origin feature/ums-auth
```

> 核心原则：**冲突在「合并」动作时才产生**——git merge、git pull、PR merge 都可能。

### 八、面试可能追问

- **Q1：`git fetch` 和 `git pull` 的区别？**
  
  - 答：`fetch` 只把远程更新下载到本地快照 `origin/xxx`，**不动工作分支**；`pull` = `fetch` + `merge`，把远程合进**当前分支**。安全起见可以先 `fetch` 看 diff 再决定是否 `merge`。

- **Q2：`git merge` 和 `git rebase` 的区别？**
  
  - 答：`merge` 保留两个分支历史，产生合并提交；`rebase` 把你的提交"搬家"到目标分支顶端，历史是一条直线更干净，但会**改写提交哈希**，**公共分支慎用**（会坑队友）。本项目用 merge 为主。

- **Q3：合并冲突怎么解决？**
  
  - 答：找到 `<<<<<<<` / `=======` / `>>>>>>>` 标记，手动保留想要的内容、删除标记符 → `git add` → `git commit`；或用 IDEA 三栏对比工具。解决冲突就是「人工告诉 Git 最终该长什么样」。

- **Q4：为什么 push 前要先 pull？**
  
  - 答：远程已被别人推进新提交，你本地落后，直接 push 会报 `non-fast-forward` 被拒绝。先 pull 把远程最新合进来（解决可能冲突），再 push 才成功。

- **Q5：「远程 dev-xuy」和「远程 dev」什么关系？**
  
  - 答：**没有从属关系，是两条独立的远程分支**。dev-xuy 是某次个人功能分支被 push 到远程的副本，dev 是另一条独立分支。两者通过 push/pull 与各自对应的本地分支同步，**彼此互不影响**。这是 Git 命名错觉最常踩的坑。

- **Q6：本项目为什么用 GitHub Flow 不用 Git Flow？**
  
  - 答：单人 MVP，无多版本并行需求，develop 是冗余中间层。GitHub Flow 单主干 + feature 分支，PR 合 master，简单高效。多人多版本公司级项目才用 Git Flow。

### 九、踩坑提醒

1. **坑：把「git push origin <本地分支名」理解为"推到某个共享主线"**
   
   - 现象：以为 `git push origin dev-xuy` 是把代码推到远程 dev；于是纠结"远程有没有 dev / 要不要建 dev"。
   - 规避：牢记 **`push` 永远是「本地同名分支 → 远程同名分支」**，跟任何"主线"无关。功能要做合入主干，永远走 PR/MR，不是直接 push 主干。

2. **坑：长期不合并主干，最后一次性大冲突**
   
   - 现象：feature 分支偏离 master/develop 太久，merge 时满屏冲突。
   - 规避：每天开工先 `git pull origin <master或develop>`，把最新主干合到功能分支，冲突化整为零。

3. **坑：直接在 master 上开发并提交**
   
   - 现象：污染主干，无法 review，误操作难回滚。
   - 规避：永远只在 feature/xxx 上开发，master 只接受 PR。

4. **坑：攒一大堆改动才提交一次**
   
   - 现象：冲突面巨大、难排查、回滚只能整块回退。
   - 规避：小步提交，一个功能/一个任务一个 commit。

5. **坑：push 被拒（non-fast-forward）就懵了**
   
   - 现象：报 `rejected ... non-fast-forward` 后不知道怎么处理。
   - 规避：这是"本地落后于远程"的正常提示，`git pull origin <分支>`（可能产生冲突）→ 解决冲突 → 再 `git push`。

---

## Git 实操全流程复盘：从零把本地仓库推到 GitHub（IDEA 2026）

- 学于：2026-09-02（本次实战：本地 `insight-engine` 仓库 → 推到 `github.com/Goutouxiaoen/insight-engine-backend.git`）
- 关联模块：版本管理实战（本工程首次接远程）
- 来源：2026-09-02 实际完整走通一轮（含 5 个坑）

> 目标：把「本地已有完整 git 历史、远程是空仓库」的**首次对接全流程**沉淀下来——每步做什么、为什么、对应哪个 git 命令、IDEA 里点哪里、会踩哪些坑。看完这份就能独立把任何本地仓库推上 GitHub。

### 第 0 步：看清初始状态（先诊断再动手）

对接前先跑三条命令搞清楚自己站在哪：

```bash
git branch -a          # 本地有哪些分支（含当前分支 *）
git remote -v          # 配没配远程（空 = 还没配，这就是推不上去的 99% 原因）
git status --short     # 工作区干不干净（有未提交改动先 commit/stash）
```

> 这次实战的初始状态：本地有 `master` + `feature/infra-docker-compose` + `feature/ums-auth` 三条分支、`git remote -v` 为空、有未提交改动。**远程仓库是空壳（GitHub 网页新建、只勾了 README）**——这是最典型的第一天场景。

### 第 1 步：先把工作区变干净（Commit 或 Stash）

推送前必须保证工作区没有"半截子改动"，否则 IDEA 的 Push 会拒绝或推不干净。

```bash
git add .
git commit -m "feat(ums): 本次工作内容摘要"
```

**IDEA**：`Git → Commit...`（Ctrl+K）→ 勾选全部改动（Modified + Untracked）→ 写 message → Commit。

> ⚠️ **坑 1：Commit Message 别粘贴外部文字**。这次我把"预期会出现的两条 git log 文字"发给用户当参考，用户直接整段粘进了 Commit Message 框，导致 `ba212bf` 这条 commit 的备注变成一长串废文字（内容无误、纯粹难看）。教训：**commit message 只写一句话描述，不要粘贴任何预览性文本**。

### 第 2 步：添加远程仓库（Add Remote）

```bash
git remote add origin https://github.com/Goutouxiaoen/insight-engine-backend.git
git remote -v   # 验证：应出现 fetch/push 两行
```

**IDEA**：`Git → Manage Remotes → +` → Name=`origin`，URL=上面 → OK。

> 概念回顾（呼应《三世界模型》）：这一步是在本地仓库"登记文件柜地址"。`origin` 只是 URL 的昵称。**没配 remote，push 一定失败**——git 不知道往哪推。

### 第 3 步：推 master（第一次对接的真正起点）

```bash
git push -u origin master
# -u = --set-upstream：告诉 git「本地 master 以后默认跟踪远程 master」，只首次推需要
```

**IDEA**：切到 master（右下角分支名）→ `Git → Push`（Ctrl+Shift+K）→ Push → 提示 Set upstream 就选上。

> 为什么先推 master：master 是所有协作模型的共同主干，先让远程有主干，后续分支才有"基准"。

### ⚠️ 第 3.5 步（本次最大坑）：443 连不上 —— 网络代理问题

推送时极可能报：

```
Failed to connect to github.com port 443 after 21000 ms: Couldn't connect to server
```

**诊断（先别乱猜）**：

```bash
ping github.com                 # 通 = DNS 正常
Test-NetConnection github.com -Port 443   # 不通 = 443 被墙（中国大陆典型）
netstat -ano | findstr "7890 1080 10809"  # 本机有没有代理在监听？
git config --global --get http.proxy      # git 配没配代理
```

**本次结论**：Clash 代理开着（127.0.0.1:7890）但 **git 没走它** → 直连被墙。解法（永久生效）：

```bash
git config --global http.proxy http://127.0.0.1:7890
git config --global https.proxy http://127.0.0.1:7890
```

**IDEA 也要配**（如果 IDEA 内置 git 不走命令行配置）：`File → Settings → Appearance & Behavior → System Settings → HTTP Proxy → Manual → Host=127.0.0.1 Port=7890`。

> 判断经验：**ping 通但 443 连不上 = 八成是本机代理没让 git 走**；有代理就配 `http.proxy`，没代理就换 SSH（`git@github.com:` 协议走 22 端口）。本次走 SSH 也通（22 端口实测可连），但配代理最省事。

### 第 4 步：推功能分支（feature/ums-auth）

```bash
git push -u origin feature/ums-auth
```

**IDEA**：右下角切到 feature/ums-auth → `Git → Push` → Set upstream。

> 概念回顾（《三世界模型》§五误解 1）：**push 永远是「本地同名分支 → 远程同名分支」**。`git push origin feature/ums-auth` 只是给远程新增一条叫 feature/ums-auth 的独立分支，**跟 master 没有从属关系，更不是"推到 dev"**。

### 第 5 步：GitHub 网页提 PR 合并到主干

推送后 GitHub 仓库首页会出现黄色提示条：`feature/ums-auth had recent pushes → [Compare & pull request]`。

**注意区分两条黄条**：`master had recent pushes` 那条**忽略**（master 是主干不是被合入对象）；点 **feature/ums-auth 那条**。

1. 点 `Compare & pull request`
2. 确认页面自动填好：**base = master**（合入目标）、**compare = feature/ums-auth**（来源）
3. `Create pull request` → 写一句描述 → `Merge pull request` → `Confirm merge`

> 概念回顾：PR 的本质 = 「请审查人把功能分支并入主干」。**master 只接受 PR，不直接 push**（《三世界模型》踩坑 3）。Merge 后远程 master 前进若干 commit，与 feature 分支内容对齐。

### ⚠️ 第 5.5 步（GitHub 特有坑）：远程多了个 main 分支

新建远程仓库时若勾选了 "Add a README"，GitHub 会**自动建 `main` 分支**（含一个 README commit），于是远程出现 main + master 两条平行分支，页面提示 "master is 14 commits ahead of, 1 commit behind main"。

**删 main 的正确姿势**（直接删会报 `You can't delete the default branch`）：

```
① 先改默认分支：仓库 Settings → General → 拉到 Default branch → main 改成 master → Update
   让 GitHub 输入仓库名确认（insight-engine-backend）
② 再删 main：仓库 Branches 页 → main 行 → Delete
```

> 判断经验：**GitHub 默认分支名是 main（新建仓库自动生成）；你的团队约定若用 master，要把默认分支改过来再删掉多余的**。本项目用 master 作主干（GitHub Flow），main 是空壳，删掉后远程干净：只剩 master + feature/xxx。

### 第 6 步：本地同步（把合并后的新 master 拉回来）

PR 合并后远程 master 已前进，本地 master 还停在旧位置：

```bash
git checkout master
git pull origin master        # 远程新 master → 本地 master
git log --oneline master -3   # 验证：应看到 Merge pull request #1 ... 等新提交
```

**IDEA**：切到 master → `Git → Pull`（Ctrl+T）。

> 概念回顾：pull = fetch（更新快照 `origin/master`）+ merge（快照合进当前分支）。**多人协作/合并后，本地 master 落后于远程时执行这一步**。

### 第 7 步（可选）：清理已合并的功能分支

PR 合入 master 后功能分支使命完成，可清理（保留也无害，只是仓库变乱）：

```bash
git branch -d feature/ums-auth              # 本地删（-d 安全检查：已合并才让删）
git push origin --delete feature/ums-auth   # 远程删
```

### 全流程对照总表

| 步骤 | 一句话做什么 | git 命令 | 对应《三世界》概念 |
| --- | --- | --- | --- |
| 0 | 诊断现状 | `git branch -a` / `git remote -v` | — |
| 1 | 工作区变干净 | `git add . && git commit` | 铁律 1：commit 只改本地 |
| 2 | 登记远程地址 | `git remote add origin <url>` | 认领文件柜 |
| 3 | 上传主干 | `git push -u origin master` | 铁律 2：本地→远程 |
| 3.5 | 网络被墙→配代理 | `git config --global http.proxy` | 网络层（与 git 无关） |
| 4 | 上传功能分支 | `git push -u origin feature/xxx` | §五误解1：同名推送 |
| 5 | 功能并入主干 | GitHub PR（base=master） | master 只接受 PR |
| 5.5 | 删多余的 main | Settings→默认分支→master；删 main | GitHub 默认分支机制 |
| 6 | 拉回新主干 | `git pull origin master` | pull = fetch+merge |
| 7 | 清理功能分支 | `git branch -d` + `push --delete` | feature 短命 |

### 本次 5 个坑速记

1. **没配 remote 就想 push** → 报 `remote origin not found` / 或 IDEA 提示无 remote。先 `git remote -v` 确认。
2. **443 连不上**（ping 通但连不上）→ 本机代理没给 git 用，配 `git config --global http.proxy http://127.0.0.1:<端口>`。
3. **远程多了 main 删不掉** → GitHub 默认分支是 main，先改默认分支为 master 再删。
4. **Commit Message 粘贴外部文字** → commit 备注变一长串废字（无害但难看），message 只写一句话。
5. **cherry-pick 自己刚提交的 commit** → IDEA 误操作（`cherry-pick ba212bf` 对自己）→ 提示 empty → skip。无残留、无害，但要知道它在干嘛、怎么确认没残留（`git status` 看有没有 rebase/cherry 状态）。

### 面试可能追问

- **Q1：`git push -u` 的 `-u` 是干嘛的？**
  
  - 答：`--set-upstream`，设置"本地分支跟踪远程分支"的上游关系。设置后后续直接 `git push` / `git pull` 无需再带 `origin 分支名`。首次推送某条新分支时必须加。

- **Q2：为什么第一次对接要先推 master？**
  
  - 答：master 是所有协作模型的共同主干。先让远程存在主干分支，后续的 feature 分支、PR 才有"合入基准"（base）。GitHub 空仓库只有自动生成的 main，没有 master，你的 master 是第一条实质分支。

- **Q3：ping 通 github.com 但 push 报 443 超时，说明什么？**
  
  - 答：DNS 解析正常（ping 通），但 HTTPS（443 端口）的 TCP 连接被网络阻断——典型于大陆网络访问 GitHub。解决：让 git 走本机代理（`git config http.proxy`），或改用 SSH 协议（22 端口）推送。

---

## PostgreSQL 自增主键：BIGSERIAL / 序列（sequence）/ nextval / setval

- 学于：2026-08-26
- 关联模块：`init.sql`（数据库初始化脚本，35 张表 + 种子数据）
- 来源：实际开发踩坑（种子数据显式 id 未重置序列）

> 目标：从 MySQL 背景彻底搞懂 PG 的"自增"——为什么它不像 MySQL 那样"自动"，为什么显式插 id 后会撞号。核心差异就一句：**MySQL 把计数器藏在表里（无名字），PG 把计数器做成独立对象「序列」（有名字）。**

### 直观类比（银行取号机）

把自增主键想成**银行取号机**：

- 正常自增 = 按一下取号机，机器"咔"吐一个号（`nextval` 自动 +1）；
- 种子数据显式写 id = **不按取号机**，自己手写"1 号"小票交柜台；
- 结果：取号机不知道你手写了 1 号，它下一个还是吐"1 号" → 两张 1 号**撞号**。

修复 `setval` = 手动把取号机计数器拨到"1 号已经发过了"，下次才吐 2 号。

### 核心原理

1. **`BIGSERIAL` 不是数据类型，是语法糖**。你写 `id BIGSERIAL`，PG 背后展开成三件事：
   
   ```sql
   CREATE SEQUENCE ie_user_id_seq;                          -- ① 造一个计数器对象（序列）
   CREATE TABLE ie_user (id BIGINT NOT NULL DEFAULT nextval('ie_user_id_seq'));  -- ② 列默认值 = 取号
   ALTER SEQUENCE ie_user_id_seq OWNED BY ie_user.id;       -- ③ 计数器挂靠到 id 列
   ```

2. **序列（sequence）= 有名字的独立计数器对象**。它内部记一个数字 `last_value`，独立于表存在，可单独 `SELECT`/`setval`。每张 `BIGSERIAL` 表自动配一个 `表名_列名_seq`。

3. **两个动作控制序列**：
   
   - `nextval('seq')`：取号，返回当前值并 +1；
   - `setval('seq', N)`：手动拨计数器到 N（默认 `is_called=true`，下次 nextval 返回 N+1）。

4. **关键差异（bug 根因）**：显式 `INSERT` 指定 id 时**绕过**列的 `nextval` 默认值，序列不动。MySQL InnoDB 的 `AUTO_INCREMENT` 显式插入后会**自动**调到 `max+1`，PG **不会**，必须手动 `setval`。

### 我在项目里怎么用的

阶段 2 `init.sql` 种子数据为了固定 id（管理员=1、角色=1~5、权限=101~148），显式写了 id，导致序列停在初始值，阶段 3 自增插入必撞号。修复——末尾对 7 张表逐一重置：

```sql
SELECT setval(
  pg_get_serial_sequence('ie_user', 'id'),     -- 动态解析序列名，不硬编码
  (SELECT COALESCE(MAX(id), 1) FROM ie_user)   -- 拨到最大 id；空表用 1 兜底
);
-- 同理：ie_organization / ie_workspace / ie_member / ie_role / ie_permission / ie_tool
```

实机验证：7 表序列 `last_value` 全等于各自 `MAX(id)`，自增插入返回 `id=2`（不再撞 1）。

### 面试可能追问

- **Q1：`SERIAL`/`BIGSERIAL` 是类型吗？** 答：不是，语法糖。等价于 `BIGINT` 列 + 自动建序列 + 列默认值 `nextval`，真正自增靠序列对象。
- **Q2：为什么显式插 id 后序列不推进？** 答：序列是独立对象，只在 `nextval`/`setval` 时变；显式 id 绕过默认值，序列不动。这是与 MySQL 的关键差异。
- **Q3：批量导入带 id 的种子数据有几种安全做法？** 答：① 插完 `setval` 重置到 `MAX(id)`（最通用）；② PG 10+ `INSERT ... OVERRIDING SYSTEM VALUE`；③ 种子不写 id，用业务键（code）做唯一约束，id 全交序列。
- **Q4：PG 删表后序列会残留吗？** 答：会。若序列没被 `OWNED BY` 关联，`DROP TABLE` 不删序列，留"孤儿序列"；`BIGSERIAL` 或手动 `OWNED BY` 才会级联删。

### 踩坑提醒

1. **坑：显式插 id 忘 setval → 后续自增主键冲突**
   
   - 现象：种子数据导入正常，应用一插新记录就报 `duplicate key`（序列还停在旧值）。
   - 规避：凡显式指定自增主键的批量导入，末尾必须 `setval` 把序列重置到 `MAX(id)`。

2. **坑：空表 `MAX(id)=NULL` 导致 setval 报错**
   
   - 现象：对空表执行 `setval(seq, (SELECT MAX(id) FROM t))` 时 `MAX(id)` 为 NULL，导致 setval 报错。
   - 规避：务必写成 `COALESCE(MAX(id), 1)`。

3. **坑：硬编码序列名脆弱**
   
   - 现象：表名/列名一改，写死的序列名（如 `public.t_user_id_seq`）就对不上，脚本失效。
   - 规避：用 `pg_get_serial_sequence('表','列')` 动态解析序列名，改名也不破。

---

## Redis 三件事：登录失败锁定 + Token 黑名单 + 登录态缓存（防暴力破解 & 主动登出/踢人）

- 学于：2026-08-26（2026-09-02 重写：三条链路放一起，补「登录后缓存了什么」「踢人怎么实现」「登出和黑名单的关系」）
- 关联模块：`AuthServiceImpl` + `LoginAttemptService` + `RedisTokenBlacklistService` + `JwtAuthFilter` + `UserServiceImpl` + `AuthConstants`
- 来源：PRD §12.1.5、TD §6.1、TD ADR-10

> 目标：把 UMS 用 Redis 干的**三件事**放在一起讲透。核心心法一句话——**「写一个标记 → 校验时读这个标记」，写而不读 = 白写。**

### 先看一张总表（三条机制放一起对比）

| 机制          | Redis Key                                       | 谁在写       | 谁在读                                            | 粒度        | 现在通不通              |
| ----------- | ----------------------------------------------- | --------- | ---------------------------------------------- | --------- | ------------------ |
| ① 登录失败锁定    | `ie:auth:login-fail:{账号}` / `ie:auth:lock:{账号}` | 登录密码错时写   | 登录开头查                                          | 按账号       | ✅ 通                |
| ② Token 黑名单 | `ie:auth:blacklist:{token摘要}`                   | 登出时写      | 每次请求 `JwtAuthFilter` 查                         | 按单个 token | ✅ 通                |
| ③ 登录态缓存     | `ie:auth:token:{userId}`                        | 登录/刷新成功时写 | 每次请求 `JwtAuthFilter` 经 `TokenSessionService` 查 | 按用户       | ✅ 通（2026-09-02 修复） |

> 这张表是本章的「地图」：三条链路「写→读」现在都闭环了。**修复前的教训**：③曾缺「读」——登录写了缓存、改密/禁用删了缓存，但校验时从没读它，所以「踢人」实际**没拦住**（写而不读=白写）。修复就是补上「校验时读」这一环，详见链路③。

### 心法：正常请求是「携带 token 校验」，那这三样 Redis 是干嘛的？

**先回答你的疑问**：正常请求的主线是「携带 token → `JwtAuthFilter` 验签名+过期」，身份验证本身靠 token 自带信息（无状态），**不需要查数据库**。但为了让无状态 JWT「可撤销/可记录」，校验时要在 Redis 上做**两次查标记**：先查黑名单（登出过没），再查登录态缓存（被踢/改密没）——这两次都是「打补丁式」的额外查询，身份主体仍来自 token。

那 Redis 这三样是干嘛的？——是给这条主线**打补丁**，补「无状态做不到」的三件事：

- 无状态做不到「撤销一个已发出的 token」（token 没到期就是有效）→ 引入**黑名单**标记，校验时多查一步。
- 无状态做不到「防暴力破解」（攻击者拿不同密码一直试）→ 引入**失败计数**标记，登录时计数。
- 无状态做不到「踢人立刻下线」→ 引入**登录态缓存**标记。

> 一句话：**凡是「要让已签发 token 提前失效」或「要记录登录行为」的诉求，都得在 Redis 留个标记，并在校验时去读它。写而不读 = 白写——第 ③ 条曾因此「断」过，修复后三环齐全（写→读→删）。**

### 直观类比（先建立直觉）

**类比①：登录失败锁定 = 门禁刷卡机**

门禁卡刷错 5 次，机器就把你**锁在门外 30 分钟**（哪怕你后来想起了正确密码，也进不去，得等解封）。计数器要能「连续记错几次」，且「锁定到期自动解封」——这正好对应 Redis 的「计数 + TTL 过期」两个能力。

**类比②：Token 黑名单 = 门禁卡挂失名单**

你丢了门禁卡（登出），物业不是去改门禁的密码（改 JWT 密钥），而是把你的**卡号（token 摘要）**写进一本「挂失名单（黑名单）」。下次有人刷这张卡，保安先查挂失名单——**名单里有的，直接拦下**，根本不用验卡真伪。

**类比③：登录态缓存 = 门禁系统里「谁还持卡在线」的登记表**

物业想「踢人下线」（比如员工离职），只要在登记表里删掉这个人的记录，他手里的卡下次就刷不了。这个登记表就是 `ie:auth:token:{userId}`。

### 核心原理

#### 1. 登录失败锁定 ≠ 限流（先分清）

| 对比项  | 登录失败锁定（本项目已实现）     | 限流 Rate Limit（本项目未做） |
| ---- | ------------------ | -------------------- |
| 目的   | 防止暴力破解密码（**安全**）   | 防止请求量过大打垮服务（**稳定性**） |
| 触发条件 | 连续**失败 N 次**（业务事件） | 单位时间**请求次数超阈值**      |
| 计数维度 | 按「账号」记失败次数         | 按「接口/IP/用户」记调用次数     |
| 失败动作 | 锁定账号 30 分钟         | 拒绝请求或排队              |
| 清零时机 | 登录成功 或 窗口过期        | 时间窗口滚动               |

> 一句话：**锁定是「你做错事惩罚你」，限流是「你来太频繁拦你」。**

#### 2. 登录失败锁定的三个 Redis 操作

PRD §12.1.5 需求：**连续密码错 5 次 → 锁定 30 分钟**。拆成三个 Redis 能力：

| Redis 命令         | 项目代码                                         | 作用                  |
| ---------------- | -------------------------------------------- | ------------------- |
| `INCR`           | `opsForValue().increment(failKey)`           | 原子自增失败次数            |
| `EXPIRE`         | `expire(failKey, 30min)`                     | 设滑动窗口 TTL（只在第一次失败设） |
| `EXISTS` / `SET` | `hasKey(lockKey)` / `set(lockKey,"1",30min)` | 检查/设置锁定标记           |

**为什么必须用 `INCR` 而不是「get→+1→set」三步？**

因为 `get→+1→set` 是**非原子**的，两个并发请求会读到同一个旧值，各自 +1 后写回，**丢失一次计数**（本应 5 次锁定，结果只记 4 次）。`INCR` 是 Redis 单条原子命令，Redis 单线程串行执行，读+加+写不可分割，天然并发安全。

#### 3. 为什么拆成「计数 key」和「锁 key」两个 key？

因为两者**生命周期语义不同**：

- 计数 key（`ie:auth:login-fail:{account}`）：需要「滑动窗口」——TTL 只在**第一次失败**时设，窗口起点固定，避免攻击者「每 29 分钟试 4 次」永不锁定
- 锁 key（`ie:auth:lock:{account}`）：需要「固定 30 分钟」——从**锁定那一刻**起算满 30 分钟

若混在一个 key 里，TTL 语义会互相干扰。拆开各自职责单一。

#### 4. Token 黑名单的设计（TD ADR-10）

JWT 无状态，服务端无法「撤销」已签发的 token。登出后 token 本应失效，靠什么实现？——**黑名单**。

- 登出时：把 token 加入黑名单，TTL = token **剩余有效期**（token 过期后本就失效，无需继续保留，避免黑名单无限膨胀）
- 校验时：黑名单检查**优先于签名校验**（顺序：黑名单 → 签名 → 过期），已登出的 token 即便未过期也必须拒绝

#### 5. 摘要（SHA-256）的三个特性，分别对应什么用途

登录态黑名单、登录态缓存**都不存 token 明文，只存 SHA-256 摘要**。为什么摘要能替代原文？因为它有三个特性，各自解决一个问题：

| 特性     | 含义                     | 在本项目解决的问题                                    |
| ------ | ---------------------- | -------------------------------------------- |
| **定长**  | 任意长输入 → 固定 64 个十六进制字符  | token 有 200+ 字符且长度不定，摘要恒为 64 字符，**适合当 key / 定长 value** |
| **单向**  | 无法从摘要反推原文             | **Redis 被拖库也拿不到可用 token**（核心安全收益）             |
| **抗碰撞** | 不同输入几乎不可能得到同一摘要       | 摘要可安全当 token 的"代表"做**相等性比对**                 |

> 一句话：**定长让它能当 key，单向让它安全，抗碰撞让它能比对**。三条缺一不可。
>
> 实现上用 JDK 自带 `MessageDigest`（`TokenDigestUtil.sha256Hex`），不引入额外依赖（项目 BOM 未带 hutool-crypto）。

#### 6. 关键概念：「单槽」——一个用户只有一个坑位

`ie:auth:token:{userId}` 的 key 里**没有会话/设备维度**，`cacheToken` 用的是 `SET` **覆盖**而非追加：

```java
// 注意是 opsForValue().set()，不是往集合里追加
stringRedisTemplate.opsForValue().set(KEY_AUTH_TOKEN + userId, sha256Hex(accessToken), ...);
```

所以一个用户**同时只存在一份 access 登录态 + 一份 refresh 会话**。用户换设备登录 → 新摘要直接覆盖旧摘要 → 旧设备立刻失效。这就是 PROGRESS 里记的「单会话语义 / 多设备互踢」。

> **由此得出一个重要推论（纠正常见误解）**：在当前单槽实现下，「登出删登录态」和「踢人删登录态」删的是**同一个 key**，对 access 的拦截效果**完全重合**。下方「登出 vs 踢人」表里"单个 token vs 该用户全部 token"说的是**设计意图**上的粒度，不是当前实现的效果差异——当前实现下"全部"就退化成"那唯一一个"。两者真正分道扬镳，要等改成多会话结构（见本章末尾「数据类型选型」）。

#### 7. 当前 jwt 分布：refresh 有 jti，access 没有

| 令牌      | 是否有 `jti` | 失效靠什么                                    |
| ------- | --------- | ---------------------------------------- |
| refresh | ✅ 有（UMS-2 已落地） | 服务端记当前 jti 摘要，一次性轮换；旧 jti 重放视为泄露 → 吊销全部会话 |
| access  | ❌ 没有       | 靠登录态缓存的**摘要比对**（存在 + 相等才放行）               |

原因：access 只有 2h、且请求量大，给它编号并逐个吊销成本高；登录态摘要比对已经能达到"只认最新那张"的等效效果。将来若上多设备，access 也需要带 `sid`（会话 ID）才能定位到 Hash 里的具体 field。

### 我在项目里怎么用的（完整代码链路）

#### 链路①：登录失败锁定

常量定义 `AuthConstants.java`：

```java
public static final int MAX_LOGIN_FAIL_COUNT = 5;        // 阈值 5 次
public static final long LOGIN_LOCK_SECONDS = 30 * 60L;  // 锁 30 分钟
public static final String KEY_LOGIN_FAIL = "ie:auth:login-fail:";  // 计数 key
public static final String KEY_LOGIN_LOCK = "ie:auth:lock:";        // 锁 key
```

登录主流程 `AuthServiceImpl.login()`：

```java
// 1. 锁定检查：命中锁 key 直接拒绝，不查库、不比密码
if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(KEY_LOGIN_LOCK + account))) {
    throw new BizException(ErrorCode.ACCOUNT_LOCKED);
}
// 4. 密码错误 → 递增计数
if (!passwordEncoder.matches(...)) {
    handleLoginFail(account);
    throw new BizException(ErrorCode.PASSWORD_ERROR);
}
// 5. 登录成功 → 清空失败计数
stringRedisTemplate.delete(KEY_LOGIN_FAIL + account);
```

核心算法（`AuthServiceImpl.handleLoginFail()`，等价逻辑已抽出到 `LoginAttemptService` 的 `recordFailure`/`isLocked`/`clearFailCount` 便于复用）：

```java
private void handleLoginFail(String account) {
    String failKey = KEY_LOGIN_FAIL + account;
    Long failCount = stringRedisTemplate.opsForValue().increment(failKey);  // 原子自增
    // 第一次失败才设窗口 TTL；后续沿用已有窗口，避免频繁重置过期时间
    if (failCount != null && failCount == 1L) {
        stringRedisTemplate.expire(failKey, Duration.ofSeconds(LOGIN_LOCK_SECONDS));
    }
    if (failCount != null && failCount >= MAX_LOGIN_FAIL_COUNT) {
        // 达到阈值：设锁 key（固定 30 分钟）+ 清空计数（解封后从零重新累计）
        stringRedisTemplate.opsForValue().set(KEY_LOGIN_LOCK + account, "1",
                Duration.ofSeconds(LOGIN_LOCK_SECONDS));
        stringRedisTemplate.delete(failKey);
    }
}
```

#### 链路②：Token 黑名单（登出 + 校验）

黑名单实现 `RedisTokenBlacklistService.java`：

```java
@Override
public boolean isBlacklisted(String token) {
    return Boolean.TRUE.equals(stringRedisTemplate.hasKey(blacklistKey(token)));
}
@Override
public void blacklist(String token, long ttlSeconds) {
    // 值存 "1" 占位即可，命中判断只依赖 key 是否存在
    stringRedisTemplate.opsForValue().set(blacklistKey(token), "1", Duration.ofSeconds(ttlSeconds));
}
private String blacklistKey(String token) {
    // key = 前缀 + token 的 SHA-256 摘要（不存明文，防泄露）
    return KEY_AUTH_BLACKLIST + sha256Hex(token);
}
```

登出时加入黑名单 `AuthServiceImpl.logout()`：

```java
public void logout(String accessToken) {
    long remainingSeconds = jwtUtil.getRemainingSeconds(accessToken);  // 剩余有效期
    if (remainingSeconds > 0) {
        tokenBlacklistService.blacklist(accessToken, remainingSeconds);  // TTL=剩余有效期
    }
    stringRedisTemplate.delete(KEY_AUTH_TOKEN + userId);  // 同时删登录态
}
```

校验时黑名单优先 `JwtAuthFilter.doFilterInternal()`（此处只展示黑名单环节，登录态校验见链路③）：

```java
// 黑名单优先于签名校验（TD §8.3：黑名单 → 签名 → 过期 → 登录态）
if (blacklistService != null && blacklistService.isBlacklisted(token)) {
    writeUnauthorized(response, ErrorCode.UNAUTHORIZED);
    return;
}
```

#### 链路③：登录态缓存（按用户，登录/刷新写 → 改密/禁用删 → 每次请求校验读）

> **这是最容易绕晕的一段，先记住一个「三时刻」模型**：
> 一个 key 的生命周期 = **写（登录/刷新时）** → **读（每次请求校验时）** → **删（改密/禁用/登出时）**。
> 缺了「读」这一环，前面的写和删就都是摆设——这正是修复前「踢人假失效」的根因（已修复，见下）。

**① 登录成功后，缓存了什么？**

登录成功（或刷新 token）时，`AuthServiceImpl.cacheToken()` 往 Redis 写一条：

```java
// SET ie:auth:token:{userId} = SHA-256(token)，TTL = 2h（跟 access token 寿命走）
stringRedisTemplate.opsForValue().set(KEY_AUTH_TOKEN + userId, TokenDigestUtil.sha256Hex(accessToken),
        Duration.ofSeconds(jwtUtil.getAccessTtlSeconds()));
```

所以「登录后」Redis 里和登录态相关的状态是：

- 写入 `ie:auth:token:{userId}` = **最新一次登录签发的 access token 的 SHA-256 摘要**，2 小时后自动过期；
- 顺带删掉 `ie:auth:login-fail:{账号}`，把失败计数清零（登录成功了，之前的失败记录作废）。

> 为什么只存摘要不存明文？Redis 被拖库时，明文 token 能直接冒充登录，摘要是不可逆的，拿到也没法用（与黑名单服务同一安全约定）。

**② 什么时候「读」这个缓存？（本次修复补上的关键环节）**

每次请求进入 `JwtAuthFilter`，顺序是：**① 查黑名单 → ② 验签名+过期 → ③ 验登录态**。第 ③ 步就是"读缓存"：

```java
JwtPayload payload = jwtUtil.parseAccessToken(token);   // 先验签名/过期，拿到 userId
// 登录态校验：缓存存在 且 摘要匹配当前 token 吗？
if (sessionService != null && !sessionService.isActive(payload.getUserId(), token)) {
    writeUnauthorized(response, ErrorCode.UNAUTHORIZED); // 登录态没了/不是最新 → 401
    return;
}
```

`RedisTokenSessionService.isActive()` 做的事：

```java
String cachedDigest = get(KEY_AUTH_TOKEN + userId);
return cachedDigest != null && cachedDigest.equals(TokenDigestUtil.sha256Hex(token));
```

**③ 踢人（改密/禁用）是怎么做的？**

管理员禁用用户（`UserServiceImpl.updateStatus()`）或用户改密（`updatePassword()`）：

```java
userMapper.updateById(update);                    // 先改库：status=0 或 换新密码
stringRedisTemplate.delete(KEY_AUTH_TOKEN + userId);  // 再删登录态缓存
```

**删完之后，下一次旧 token 请求进来会怎样？**

```
① 用户 A 登录成功 → Redis 写了 ie:auth:token:A = sha256(tokenA)
② 管理员禁用 A → 删了 ie:auth:token:A
③ A 拿旧 token 继续请求 → JwtAuthFilter 验签名(通过) → 验登录态: 查缓存 → key 不存在 → 401 拒绝 ✅
④ 踢人真正生效；A 只有重新登录（重新写缓存）才能继续用
```

**为什么「删」就够？** JWT 本身无状态、无法撤销，但「缓存删了」成了**有状态的失效标记**——只要校验时**每次必查**这个标记，删掉即全拒。这就是「用一次 Redis 查询，换来无状态 JWT 的可撤销能力」。

> 小结：`cacheToken` 写（登录）、`JwtAuthFilter` 读（每次请求）、`updateStatus`/`updatePassword` 删（改密/禁用）——三环齐全才叫「踢人机制生效」。

### 完整时序图（登录锁定）

```
第1次错: INCR login-fail:admin → 1  (设 TTL 30min，窗口起点)
第2~4次错: INCR → 2 → 3 → 4
第5次错: INCR → 5  ≥5 → SET lock:admin=1 (TTL 30min) + DEL login-fail
──────────────────────────────────────────────
第6次(密码对): hasKey(lock:admin)=true → 直接拒绝 2003
───────────────── 30 分钟后 lock:admin 自动过期 ─────────────────
第7次: hasKey(lock:admin)=false → 正常校验
```

### 登出 vs 踢人：黑名单和登录态到底什么关系（串起来看）

这是本章最容易混的地方，单独拎出来对比：

|      | 登出（用户主动）                   | 踢人/改密（管理员或用户触发）                                       |
| ---- | -------------------------- | ----------------------------------------------------- |
| 触发方法 | `AuthServiceImpl.logout()` | `UserServiceImpl.updateStatus()` / `updatePassword()` |
| 做了啥  | 加黑名单 + 删登录态 + 删 refresh 会话   | 删登录态 + 删 refresh 会话                                    |
| 设计粒度 | 单个 token（当前这一次登录）          | 该用户全部 token                                           |
| 实际效果 | ✅ 真失效（黑名单被过滤器读）            | ✅ 真失效（登录态被过滤器读，已修复）                                   |
| ⚠️ 当前实现的真相 | 两者删的是**同一对 key**，对 access 的拦截效果**完全重合** | 同左（"全部" = 单槽里唯一那一份）                      |

**登出为什么同时做三件事？**

```java
public void logout(String accessToken) {
    // ① 加黑名单：废掉「当前这张 access token」
    if (remainingSeconds > 0) {
        tokenBlacklistService.blacklist(accessToken, remainingSeconds);
    }
    // ② 删 refresh 会话：切断「续期能力」
    stringRedisTemplate.delete(KEY_AUTH_REFRESH + userId);
    // ③ 删登录态：废掉 access
    stringRedisTemplate.delete(KEY_AUTH_TOKEN + userId);
}
```

- **① 加黑名单**：按 token 粒度废掉当前这张 access——但注意它**只管 access，refresh token 从没进过黑名单**；
- **② 删 `ie:auth:refresh:{userId}` 才是登出的必需项**：它切断续期能力。若只加黑名单、不删 refresh 会话 key，持有旧 refresh token（7 天有效）的人照样能调 `/refresh` 换一对全新令牌，**登出形同虚设**；
- **③ 删 `ie:auth:token:{userId}`**：按用户粒度废掉 access。但**当前实现下它和 ① 的效果对 access 是重合的**（单槽，见核心原理第 6 节），属于纵深防御，而非"缺了就出洞"。

**反证一：只删 key、不加黑名单，登出能生效吗？** 能——旧 access 因登录态缺失/摘要不匹配被拒，旧 refresh 因会话 key 被删被拒。**所以黑名单对 access 而言是冗余的**。

**反证二：只加黑名单、不删 key，登出能生效吗？不能**——旧 refresh 仍能换新令牌对，登出被绕过。

> **一句话纠正**：登出"三件事都做"并不是因为"一个管单 token、一个管全部 token"（当前单槽下 ① 和 ③ 管的是同一份）。真正的分工是：**黑名单断「这张票」，删 refresh 会话 key 断「续期能力」**。后者才不可替代。

**一句话串起来**：

> 想让 token 失效，本质只有一条路——**在 `JwtAuthFilter` 校验时能查到一个「已失效」的标记**。黑名单（登出）与登录态缓存（踢人/改密）**两条路现在都被过滤器读**，所以：**登出有效，踢人也有效**。

**修复方式**（2026-09-02，方案 A，改动最小、贴合 TD §6.1 单 key 设计）：

- starter-security 新增可选接口 `TokenSessionService`（与 `TokenBlacklistService` 同模式，starter 保持零 Redis 依赖，未注入的服务退化为纯 JWT 校验）；
- `JwtAuthFilter` 在「验签名通过后、建立认证前」调 `sessionService.isActive(userId, token)`：缓存不存在（被删）或摘要不匹配（非最新登录）→ 401；
- UMS 提供 `RedisTokenSessionService` 实现 + `TokenDigestUtil`，并把 `cacheToken` 由存明文改为存 SHA-256 摘要。
- 权衡：方案 A 是单会话语义（同 userId 新登录会顶掉旧 token，多设备互踢）；若未来需多设备并存，再演进 `jti + ver`（见「JWT 无状态」章节方案 B）。

### 补充：白名单 vs 黑名单语义（fail-closed / fail-open）——两者的本质差异

上文澄清了"当前实现下两者效果重合"，那它们的**本质差异**在哪？答案藏在一行代码的方向里：

```java
// 登录态（白名单）：key 存在 且 摘要匹配 → 放行
return cachedDigest != null && cachedDigest.equals(sha256Hex(token));

// 黑名单：key 存在 → 拒绝
return Boolean.TRUE.equals(stringRedisTemplate.hasKey(blacklistKey(token)));
```

|             | key 存在    | key 不存在    | 失效模式         | 术语                        |
| ----------- | --------- | ---------- | ------------ | ------------------------- |
| **登录态缓存**   | 比对摘要决定    | **拒绝**     | 查不到就拒绝 → 安全侧 | **fail-closed**（失败即关闭）    |
| **黑名单**     | 拒绝        | **放行**     | 查不到就放行 → 可用侧 | **fail-open**（失败即开放）      |

**这个方向差异有真实后果**：

- 登录态 key 丢失（Redis 内存淘汰、主从切换丢数据、实例重启）→ **所有正常用户被误踢 401**。这是"白名单式"方案的固有脆弱点——它依赖 key **必须存在**。
- 黑名单 key 丢失 → 最坏情况是"已登出的 token 短暂复活"，**不会误伤正常用户**。

**那该怎么防？** 关键在于 `maxmemory-policy` 的选择：

| 策略                       | 内存满时的行为                  | 对登录态的影响                       |
| ------------------------ | ------------------------ | ----------------------------- |
| `noeviction`（默认）         | **拒绝新写入**，已有 key 不动      | 登录态不会丢，但**新登录会失败**（写不进去）      |
| `volatile-lru`/`volatile-ttl` | 只在**设了 TTL 的 key** 里淘汰 | 登录态**带了 TTL，仍在淘汰范围内** → 可能误踢  |
| `allkeys-lru`            | 在**全部 key** 里淘汰          | 同样会淘汰登录态，且范围更大                |

> 结论：**没有哪个策略能真正保护登录态**，因为它天然带 TTL 就属于可淘汰对象。真正的兜底是**容量充足 + 监控告警 + Redis 高可用（哨兵/集群）**，而不是指望淘汰策略。

> **面试怎么答**：安全组件选型时，"缺失即拒绝"和"缺失即放行"是两个截然不同的失效模式。令牌校验属于**安全边界**，理论上更希望 fail-closed；但 fail-closed 把 Redis 变成了**强依赖**（Redis 挂 = 全员掉线）。工程上的权衡是：给 Redis 配高可用 + 接受短暂不可用，或者在 Redis 不可达时降级为"只验签名"（fail-open）并记录告警。

### 面试可能追问

- **Q1：登录失败锁定为什么用 Redis 而不是数据库？**
  
  - 答：锁定是「30 分钟临时态」，无需持久化；Redis 天然支持 TTL 到期自动解锁，且 INCR 原子自增避免并发丢计数。用数据库还得加定时任务清理过期锁，得不偿失。

- **Q2：`INCR` 为什么能保证并发安全？`get+set` 会有什么问题？**
  
  - 答：Redis 命令单线程串行执行，`INCR` 的「读+加+写」不可分割，天然原子。`get→+1→set` 三步之间可被其他请求穿插，两个并发请求读到同一旧值，后写覆盖先写，造成「丢失更新」（lost update）——本应记 5 次却只记 4 次，锁不住。

- **Q3：JWT 无状态，登出后怎么让 token 失效？**
  
  - 答：用黑名单。登出时把 token 摘要写入 Redis 黑名单（TTL=剩余有效期），每次请求认证过滤器先查黑名单，命中即拒绝。同时顺手删登录态缓存 `ie:auth:token:{userId}`（清在线状态）。这是「有状态的黑名单」弥补「无状态的 JWT」无法撤销的短板（TD ADR-10）。

- **Q4：黑名单 TTL 为什么设为 token 剩余有效期，而不是固定值？**
  
  - 答：token 过期后本就失效，无需继续留在黑名单；设为剩余有效期能让黑名单条目到期自动清除，不无限膨胀，也不浪费内存。

- **Q5：为什么黑名单 key 存的是 SHA-256 摘要而不是 token 明文？**
  
  - 答：安全考虑——防止可用 token 直接暴露在 Redis 键中（运维排查、Redis 被拖库时），摘要不可逆，拿到摘要也无法还原 token。

- **Q6：登出（加黑名单）和踢人（删登录态）有什么区别？**
  
  - 答：两者粒度不同——登出只废「当前这一个 token」，用黑名单按 token 记（key 含 token 摘要）；踢人/改密要废「该用户全部 token」，用登录态按 userId 记（删一个 key 全失效）。修复前两者还有**生效与否**的差异：黑名单被 `JwtAuthFilter` 读所以登出有效，登录态缓存没人读所以踢人删了也是白删（旧 token 最多活到 2h 过期）。**修复后两条路都通**：`JwtAuthFilter` 验签后也校验 `ie:auth:token:{userId}` 存在且摘要匹配，缓存被删即 401，踢人/改密真正生效。
  - **加分补充（承认局限）**：但要注意，**在当前的单槽实现下，这个"粒度不同"只是设计意图，不是可观测的效果差异**——两者删的是同一个 key。真正的分工是「黑名单断这张票，**删 refresh 会话 key 断续期能力**」；后者不可替代，因为 refresh token 从没进过黑名单。

- **Q7：Redis 的 TTL 能加在 Hash 的某个 field 上吗？**
  
  - 答：**不能。** `EXPIRE`/`PEXPIRE` 的作用对象只能是**整个 key**，Hash / Set / ZSet 的内部元素（field / member）**没有独立过期时间**。所以如果用 Hash 存多设备会话（`field=sessionId`），某台设备 token 过期后那一行不会自动消失，会残留成"僵尸 field"。应对三选一：① 整个 key 设 TTL、登录时刷新（最省事）；② 改用 ZSet，`score` 存过期时间戳，定时 `ZREMRANGEBYSCORE` 清理；③ 校验时惰性 `HDEL`（`HDEL` 幂等，并发无害）。

- **Q8：多设备登录要存多份会话，Redis 用什么结构？为什么不用多个 String key？**
  
  - 答：推荐 **Hash**：`ie:auth:sessions:{userId}`，`field=sessionId`、`value=token 摘要`。登出单设备 = `HDEL` 一个 field，踢人全设备 = `DEL` 整个 key，都是 O(1) 且原子。若用多个独立 String key（`{userId}:{sid}`），"全踢"就必须按前缀扫描——`KEYS` 会**阻塞整个 Redis**（生产禁用），`SCAN` 虽不阻塞但仍是 O(N) 且迭代期间的增量可能漏扫。这个对比正好说明：**数据结构选型直接决定操作复杂度**。

- **Q9：登录态缓存（白名单）和黑名单，Redis 出问题时的表现有什么不同？**
  
  - 答：两者**失效模式相反**。登录态是"存在且匹配才放行"，key 丢失 → 一律拒绝 → **fail-closed**，Redis 抖动会导致**正常用户被误踢**；黑名单是"存在才拒绝"，key 丢失 → 一律放行 → **fail-open**，最坏是已登出 token 短暂复活，**不误伤正常人**。安全边界理论上更希望 fail-closed，但那会把 Redis 变成强依赖（Redis 挂 = 全员掉线），工程上要配高可用 + 合理 `maxmemory-policy` + 容量监控，而不是靠淘汰策略兜底。

- **Q10：Redis 的过期 key 是怎么被删掉的？为什么不用定时器精确删除？**
  
  - 答：两种策略配合——**惰性删除**（访问 key 时才检查过期时间戳，过期就删并返回 nil，省 CPU 但没人访问的过期 key 会一直占内存）+ **定期删除**（默认每秒 10 次，随机抽样一批带 TTL 的 key 删掉已过期的，控制内存）。不用定时器的原因：为每个 key 维护一个定时器，内存和时间开销都不可接受；Redis 用"惰性 + 抽样"在 **CPU 与内存之间取折中**。

- **Q11：Redis 五种数据类型分别适合什么场景？为什么存多设备会话用 Hash？**
  
  - 答：**String**（单值，支持 `INCR`/`SETNX`）→ 缓存、计数器、锁；**Hash**（field→value 映射）→ 对象属性、购物车、会话表；**List**（有序可重复，两头 O(1)）→ 队列、最新列表；**Set**（无序去重，`SISMEMBER` O(1)）→ 标签、去重、共同好友；**ZSet**（按 score 有序，**支持按 score 区间批量操作**）→ 排行榜、延迟队列、过期索引。
  - 选 Hash 存会话的原因：一个用户一个 key、里面多台设备各占一个 field；登出 = `HDEL` 一个 field（O(1)），踢人 = `DEL` 整个 key（O(1)，原子，不需要遍历）。而用多个 String key 则"全踢"必须按前缀扫描，`KEYS` 会阻塞生产环境、`SCAN` 是 O(N)。**数据结构选型直接决定操作复杂度**，这就是最好的例子。

- **Q12（连环追问）：Hash 的 field 不能设 TTL，会话过期了怎么办？**
  
  - 答：三条路，按成本递增：
    1. **整个 hash 设 TTL**（`EXPIRE key 7d`，登录/刷新时续期）——不是让 field 过期，而是让**整个 key** 兜底过期。要点：TTL 必须**远大于**元素有效期（否则正在用的会话被误删），且每次登录都要重新 `EXPIRE`。代价是脏数据最多滞留一个 TTL 周期。
    2. **ZSet 当过期索引 + 定时清理**——Hash 存内容（`field=sid`），ZSet 存时间（`member=sid, score=过期时间戳`）。清理时**必须先 `ZRANGEBYSCORE` 查出 sid，再 `HDEL` 内容，最后 `ZREMRANGEBYSCORE` 删索引**（顺序反了就找不到该删什么了）。代价是**双写一致性**问题 + 需要定时任务。
    3. **惰性删除**——校验时发现 token 过期就顺手 `HDEL`。`HDEL` 幂等（删不存在返回 0 不报错），并发无需加锁。但**清不干净**（用户不来就永远留着），只能当辅助。
  - **我的选择**：MVP 用 **1 + 3**（一个 `EXPIRE` 兜底 + 校验时顺手删），两行代码解决 90% 问题；只有需要"按时间范围查询/清理会话"时才上 2。

### 踩坑提醒

1. **坑：用 `get→+1→set` 三步写失败计数，并发下丢计数**
   
   - 现象：高并发暴力破解时，5 次阈值实际可能 7、8 次才锁定，甚至锁不上。
   - 规避：计数一律用 `INCR` 原子自增，返回值就是最新计数，直接用返回值判断是否达阈值。

2. **坑：每次失败都重置计数 key 的 TTL，导致滑动窗口失效**
   
   - 现象：攻击者每 29 分钟试 4 次密码，窗口一直被刷新，永远不被锁定。
   - 规避：TTL 只在「第一次失败」（`failCount == 1`）时设置，窗口起点固定，之后只 `INCR` 不 `EXPIRE`。

3. **坑：达到阈值后只设锁 key、不清计数 key，解封后残留计数**
   
   - 现象：30 分钟锁到期后，之前剩的计数还在，用户再错 1 次就又被锁。
   - 规避：触发锁定时同时 `DEL` 计数 key，锁定解除后从零重新累计。

4. **坑（比"状态不一致"严重得多）：登出只加黑名单、忘了删 refresh 会话 key**
   
   - 现象：access 确实被黑名单拦了，**但 refresh token 从没进过黑名单**。旧 refresh token 有 7 天有效期，只要 `ie:auth:refresh:{userId}` 还在，持有者调 `/refresh` 就能换一对**全新**的 access + refresh——**登出形同虚设**。
   - 规避：登出必须三件事都做——加黑名单（废这张 access）+ 删 refresh 会话（断续期）+ 删登录态（清状态）。**其中删 refresh 会话才是不可替代的那一步。**

5. **坑（已知未修，见 PROGRESS §五待办）：`INCR` + `EXPIRE` 非原子，计数 key 可能永不过期**
   
   - 现象：`handleLoginFail` 先 `INCR` 再 `EXPIRE` 是**两条独立命令**。若进程在两者之间崩溃或 Redis 抖动，`failKey` 就**永远没有 TTL**——该账号的失败计数永久残留，用户半年后偶尔输错一次密码就可能被直接锁定。
   - 规避（三选一）：① **Lua 脚本**把 `INCR` + `EXPIRE` 打包成一次原子执行；② 先用 `SET key 1 EX 1800 NX` 抢锁式初始化（初始化时自带 TTL），再 `INCR`；③ 用 Redisson 的 `RAtomicLong` + `expire`。
   - 同类问题的通用解法：**Redis 的多命令组合一律用 Lua 或 Pipeline 保证原子性**，别在应用代码里拼。

6. **坑：为多设备会话按前缀删 key 时用了 `KEYS` 命令**
   
   - 现象：`KEYS ie:auth:token:1001:*` 一次性遍历**整个键空间**并一次性返回全部匹配 key。Redis 单线程执行命令，数据量大时会阻塞数百毫秒甚至数秒，期间所有请求全部排队。
   - 规避：生产环境**明令禁用 `KEYS`**，用 `SCAN` 增量迭代（游标式，每次取一小批）；**更好的做法是压根别用"多个独立 key"**，改用 Hash 结构，一个 `DEL` 搞定，无需遍历。

### 延伸专题：Redis 数据类型选型（从「单槽」到「多设备」）

> 上面三件事用的全是 **String**（`opsForValue()`）。一旦需求变成"一个用户对应多份会话"，就进入 Redis **数据类型选型**的领域——这是面试高频题。

#### 1. 五种类型分别长什么样（先看图，再看特点）

---

**① String —— 一个 key 对一个值**

```
key                           value
"ie:auth:token:1001"       →  "a3f5c8d2...（64 位摘要）"
"ie:auth:login-fail:admin" →  "3"          ← 数字也能存，可直接 INCR
```

- **特点**：最简单，二进制安全（字符串 / 数字 / 序列化对象都能塞），单值最大 512MB
- **核心命令**：`SET` / `GET` / `DEL` / `INCR` / `SETEX`（设值同时设 TTL）/ `SETNX`（不存在才设，分布式锁的地基）
- **本项目**：**全在用**——失败计数、锁、黑名单、登录态，全是 String
- **适用**：缓存、计数器、分布式锁

---

**② Hash —— 一个 key 对应一张「字段表」**

```
key = "ie:auth:sessions:1001"
  ┌──────────┬─────────────────┐
  │ field    │ value           │
  ├──────────┼─────────────────┤
  │ "7f3a9c" │ "a3f5c8d2..."   │  ← 手机
  │ "b21e08" │ "9d2e41f7..."   │  ← 电脑
  │ "d9044f" │ "1c7b903a..."   │  ← 平板
  └──────────┴─────────────────┘
```

- **特点**：field 唯一、**field 无序**、**field 不能单独设 TTL**（致命限制，见第 5 节）；单取/单改一个 field 是 O(1)，不用整表读写
- **核心命令**：`HSET`（设字段）/ `HGET`（取一个）/ `HDEL`（删字段）/ `HLEN` / `HEXISTS` / `HGETALL`（取全部，⚠️ 大 hash 会阻塞）
- **对比 String 存对象**：把对象 JSON 塞进 String，改一个字段要「读出 → 反序列化 → 改 → 序列化 → 写回」；Hash 直接 `HSET` 一个字段，省掉整块读写
- **适用**：对象属性、购物车、**会话表**

---

**③ List —— 有序（按插入顺序）、可重复的双向队列**

```
key = "ie:mq:queue"
  head ─→ [ "msg1" ⇄ "msg2" ⇄ "msg3" ] ←─ tail
```

- **特点**：**有序**（插入顺序）、**可重复**；两头进出都是 O(1)，**按下标访问是 O(N)**
- **核心命令**：`LPUSH`/`RPUSH`（进）/ `LPOP`/`RPOP`（出）/ `LRANGE`（取区间，⚠️ O(N)）/ `BLPOP`（阻塞弹出，做队列用）
- **别误用**：不要当数组随机访问（`LINDEX` 是 O(N)）；大列表别 `LRANGE 0 -1` 全量拉取
- **适用**：消息队列、最新动态列表

---

**④ Set —— 无序、不重复的集合**

```
key = "ie:user:roles:1001"
  { "admin", "end_user", "kb:read" }     ← 无序，且自动去重
```

- **特点**：**无序**、**自动去重**；**`SISMEMBER` 判存在是 O(1)** —— 这是它最大的价值
- **核心命令**：`SADD` / `SREM` / `SISMEMBER`（判存在）/ `SMEMBERS` / `SINTER`（交集）/ `SUNION`（并集）/ `SDIFF`（差集）
- **适用**：标签、去重、**共同好友（`SINTER`）**、权限集合判断
- **本项目**：权限缓存处可选（`SISMEMBER perms:1001 "kb:read"`）

---

**⑤ ZSet（有序集合）—— 带 score 排序，member 唯一**

```
key = "ie:auth:expire:1001"
  ┌──────────┬──────────────┐
  │ member   │ score        │   ← 按 score 自动升序排列
  ├──────────┼──────────────┤
  │ "7f3a9c" │ 1756876800   │   ← 过期时间戳（越小 = 越早过期）
  │ "b21e08" │ 1756876900   │
  │ "d9044f" │ 1756963200   │
  └──────────┴──────────────┘
```

- **特点**：**有序（按 score）**、member 唯一、**score 可重复**；既能按 member 取，也能**按 score 区间批量操作**
- **核心命令**：`ZADD` / `ZREM` / `ZSCORE`（取某 member 的分数，O(1)）/ `ZRANGE`（按排名取）/ `ZRANGEBYSCORE`（按分数区间取）/ `ZREMRANGEBYSCORE`（**按分数区间批量删**）/ `ZCARD`
- **关键优势**：`ZREMRANGEBYSCORE` 能「一次删掉 score 落在某个区间的所有 member」——**这正是"按过期时间批量清理"需要的能力，Hash 和 Set 都做不到**
- **适用**：排行榜、延迟队列、**过期时间索引**

---

#### 2. 一张表对比（面试背这张就够）

| 类型         | 有序？            | 去重？      | 怎么定位元素                 | 核心优势                             | 典型场景          |
| ---------- | -------------- | -------- | ---------------------- | -------------------------------- | ------------- |
| **String** | —              | —        | 就一个值                   | 最简单，支持 `INCR` / `SETNX`          | 缓存、计数器、锁      |
| **Hash**   | 否（field 无序）    | field 唯一 | 按 field 名              | 按字段读写 O(1)，省 key 数               | 对象属性、购物车、会话表  |
| **List**   | 是（插入顺序）        | 否        | 按两端 / 按下标（O(N)）        | 两头进出 O(1)                        | 队列、最新列表       |
| **Set**    | 否              | **是**    | 按 member 值             | `SISMEMBER` O(1) + 交并差运算          | 标签、去重、共同好友    |
| **ZSet**   | **是（按 score）** | member 唯一 | 按 member **或按 score 区间** | **范围批量操作**（`ZREMRANGEBYSCORE`）    | 排行榜、延迟队列、过期索引 |

> **选型口诀**：
>
> - 就一个值 → **String**
> - 一个对象的多个字段 → **Hash**
> - 要排队 / 讲顺序 → **List**
> - 要判"在不在"、要去重 → **Set**
> - 要排序，或要**按数值范围批量操作** → **ZSet**
>
> **本项目现状**：三件事全用 String；多设备会话应该用 **Hash**；若还要按过期时间清理，再加一个 **ZSet** 当索引。

#### 3. 多设备会话的三种方案对比

| 方案                    | 结构                                        | 登出（单设备）           | 踢人（全设备）    | 主要坑                                  |
| --------------------- | ----------------------------------------- | ----------------- | ---------- | ------------------------------------ |
| A. 多个 String key      | `ie:auth:token:{userId}:{sid}` × N        | `DEL` 指定 key      | 按前缀找全删     | **禁止 `KEYS`**；`SCAN` 是 O(N) 且不保证一致   |
| **B. Hash（推荐）**       | `ie:auth:sessions:{userId}` → `field=sid` | `HDEL` 一个 field   | `DEL` 整个 key | **field 不能单独设 TTL**                  |
| C. ZSet 单独存           | `member=sid`，`score=过期时间戳`                | `ZREM`            | `DEL`      | 只能存时间，内容还得另开 key（所以通常 Hash + ZSet 配合） |

#### 4. 方案 B（Hash）展开

```
ie:auth:sessions:1001  (Hash, TTL=7d，登录/刷新时刷新)
  ├─ "7f3a9c" → sha256(T1)    手机
  ├─ "b21e08" → sha256(T2)    电脑
  └─ "d9044f" → sha256(T3)    平板
```

| 动作            | 命令                                  | 效果           |
| ------------- | ----------------------------------- | ------------ |
| 登录新设备         | `HSET` + `EXPIRE`                   | 新增一行，互不影响    |
| 请求校验          | `HGET sessions:1001 <sid>` 比对摘要      | 只认自己那一行      |
| **登出（单设备）**   | `HDEL sessions:1001 <sid>`          | **只掉这一台**    |
| **踢人/改密（全设备）** | `DEL sessions:1001`                 | **一次操作全掉**   |

> **有意思的推论**：改成 Hash 之后，「登出」和「踢人」才**第一次真正分道扬镳**。单槽时代两者删的是同一个 key（这正是最初困惑的根源），Hash 时代登出 = `HDEL` 一个 field、踢人 = `DEL` 整个 key，语义彻底分离。
>
> 配套改动：access token 需要带上 `sid` claim，否则过滤器不知道该查哪个 field。

#### 5. 硬限制：TTL 只能加在 key 上 —— 以及三种绕开办法（逐个讲透）

必须刻进脑子里的一条：

- `EXPIRE` / `PEXPIRE` 的作用对象**只能是整个 key**；
- Hash / Set / ZSet 的**内部元素（field / member）无法单独设置过期时间**；
- 后果：用 Hash 存多设备会话时，某台设备 token 过期后，它在 hash 里的那一行**不会自动消失**，`HLEN` 只增不减，内存缓慢泄漏。

**问题长这样**（假设现在 13:00）：

```
ie:auth:sessions:1001  (Hash)
  ├─ "7f3a9c" → 摘要    10:00 登录，token 12:00 过期   ← 已过期，但还赖在这儿
  ├─ "b21e08" → 摘要    11:00 登录，token 13:00 过期   ← 正在用
  └─ "d9044f" → 摘要    12:30 登录，token 14:30 过期   ← 正在用
```

下面三种应对，逐个拆开。

---

**方案 1：整个 hash 设 TTL（最省事，MVP 首选）**

**做法**：`HSET` 之后给**整个 key** 设 TTL，每次登录/刷新时 `EXPIRE` 续期。

```java
// 登录新设备
hashOps.put(sessionKey, sid, sha256Hex(token));        // HSET 写一行
redisTemplate.expire(sessionKey, Duration.ofDays(7));  // EXPIRE 给整个 key 续命 7 天
```

**怎么绕过限制的**：不给 field 设 TTL（做不到），而是**给整个 hash 设 TTL**——用户 7 天内只要登录过一次，key 就续命；7 天不登录，整个 key 连同里面所有残留 field **一起消失**。

**三个数字要想清楚**：

| 要点                | 说明                                                                                  |
| ----------------- | ----------------------------------------------------------------------------------- |
| TTL 必须 **远大于** 元素有效期 | access token 只有 2h，key TTL 给 7d。若 TTL 小于 2h，元素还没自然过期就被连带删掉 → **用户被无故踢下线**     |
| 每次登录/刷新都要重新 `EXPIRE` | 否则用户连续用满 7 天后 key 到期，**正在用的会话也被清掉**                                             |
| 脏数据最长滞留 = key 的 TTL | 本例 7 天                                                                              |

**代价**：脏数据最多躺 7 天。1000 活跃用户 × 平均 3 台设备 = 3000 个 field，其中几百个是僵尸——这点内存完全可以接受。

> **一句话**：**不是让 field 过期，而是让整个 hash 过期——用"整体兜底"绕开"field 无 TTL"。**

---

**方案 2：ZSet 当「过期时间索引」+ 定时清理**

**思路**：Hash 负责**存内容**，ZSet 负责**存"什么时候过期"**。Redis 没有"带 TTL 的 field"，那就**自己造一个过期索引**。

```
ie:auth:sessions:1001   (Hash)   field=sid  → token 摘要      【存内容】
ie:auth:expire:1001     (ZSet)   member=sid → 过期时间戳       【存时间索引】
```

```
ie:auth:expire:1001  (ZSet，按 score 自动升序，越靠前越早过期)
  ┌──────────┬────────────┐
  │ member   │ score      │
  ├──────────┼────────────┤
  │ "7f3a9c" │ 1756876800 │  ← 已过期（< now）
  │ "b21e08" │ 1756876900 │  ← 已过期
  │ "d9044f" │ 1756963200 │  ← 未过期
  │ "e13c55" │ 1756963300 │  ← 未过期
  └──────────┴────────────┘
                    ↑ now = 1756877000
```

**写的时候（双写，两笔 + 一个 TTL）**：

```java
hashOps.put(sessionKey, sid, digest);                   // ① Hash 存内容
zSetOps.add(expireKey, sid, expireAtEpochSecond);       // ② ZSet 存过期时间
redisTemplate.expire(expireKey, Duration.ofDays(7));    // ③ 索引 key 自己也要有 TTL
```

**清理的时候（三步，顺序不能反）**：

```java
long now = System.currentTimeMillis() / 1000;
// ① 先按 score 区间查出「已过期」的 sid
//    ★ 必须先查！否则先删了索引，就再也找不到该删哪些内容了
Set<String> expired = zSetOps.rangeByScore(expireKey, 0, now);
if (!expired.isEmpty()) {
    hashOps.delete(sessionKey, expired.toArray());      // ② 删 Hash 里的内容
}
zSetOps.removeRangeByScore(expireKey, 0, now);          // ③ 删 ZSet 里的索引
```

**校验时怎么判存活**：

```java
Double expireAt = zSetOps.score(expireKey, sid);   // ZSCORE，O(1)
if (expireAt == null || expireAt < now) {
    return false;                                  // 索引里没有，或已过期
}
String digest = hashOps.get(sessionKey, sid);      // 再取内容比对
return digest != null && digest.equals(sha256Hex(token));
```

**为什么非得 ZSet**：关键在 `ZRANGEBYSCORE` / `ZREMRANGEBYSCORE` —— **按 score 数值区间批量操作**。Hash 只能按 field 名一个个来，没法表达"把所有过期时间小于 now 的都删掉"。

**代价（这就是它"成本最高"的原因）**：

- **双写一致性**：写要 `HSET` + `ZADD`，删要两边都删。任何一步失败就会出现「内容在、索引没了」（永不清理）或「索引在、内容没了」（校验误判为失效）；
- 需要额外的定时任务（几分钟跑一次）；
- 两个 key 的 TTL 要协调一致。

**什么时候值得上**：设备数很大、对内存敏感、或需要"按过期时间排序 / 分页查会话列表"。**MVP 阶段不值得**。

---

**方案 3：惰性删除（顺手清理，不能单独用）**

**思路**：不主动扫，谁撞上谁清理。

```java
public boolean isActive(Long userId, String sid, String token) {
    String digest = hashOps.get(sessionKey(userId), sid);
    if (digest == null) return false;                    // 记录不存在
    if (!digest.equals(sha256Hex(token))) return false;  // 摘要不匹配

    // 记录存在且对得上，再看 token 本身过期没（解析 JWT 的 exp 即可，不用查 Redis）
    if (jwtUtil.getRemainingSeconds(token) <= 0) {
        hashOps.delete(sessionKey(userId), sid);         // ← 惰性清理：顺手删掉
        return false;
    }
    return true;
}
```

**为什么并发安全**：`HDEL` 是**幂等**的——删存在的 field 返回 1，删不存在的返回 0，**都不会报错**。100 个并发请求同时删同一个 field，最终结果完全一样，**所以不需要加锁**。

**致命短板**：只有"有人拿着过期 token 来请求"才会触发清理。用户登出后再也不来，那条 field **永远留着**——**清不干净**。

**所以它的定位是辅助，不是主力**：

| 组合                 | 效果                              |
| ------------------- | ------------------------------- |
| 只用方案 3              | ❌ 僵尸 field 永远清不掉                |
| **方案 1 + 方案 3（推荐）** | ✅ 惰性删除顺手清大部分，整体 TTL 兜底清剩余       |
| 方案 2 + 方案 3         | ✅ 定时清理为主，惰性删除减轻定时任务压力           |
| 方案 1 + 2 + 3        | 过度设计，MVP 不需要                    |

---

**决策一句话**：

> **MVP 用「方案 1 + 方案 3」**——一个 `EXPIRE` 兜底 + 校验时顺手 `HDEL`，两行代码解决 90% 的问题。
> **只有当你需要"按时间范围查询 / 清理会话"时才上方案 2**——那时 ZSet 的范围操作能力才真正值回它的双写复杂度。

#### 6. `KEYS` 为什么是生产禁命令

`KEYS pattern` 会**一次性遍历整个键空间**并把全部匹配 key 一次性返回。Redis 单线程执行命令，百万级 key 时会阻塞数百毫秒到数秒，**期间所有其它请求全部排队**。

正确替代是 `SCAN`——**游标式增量迭代**，每次只取一小批，不阻塞：

```java
ScanOptions options = ScanOptions.scanOptions()
        .match("ie:auth:token:1001:*").count(100).build();
try (Cursor<byte[]> cursor = redisTemplate.getConnectionFactory()
        .getConnection().scan(options)) {
    while (cursor.hasNext()) {
        keysToDelete.add(new String(cursor.next()));
    }
}
```

但 `SCAN` 仍有两点不足：**迭代期间新增的 key 可能扫不到**（不保证一致）、**总复杂度仍是 O(N)**。这也是推荐 Hash 的深层原因——`DEL` 一个 key 是**原子的一次操作**，压根不需要遍历。

#### 7. 顺带：Redis 的过期 key 是怎么被删掉的

理解了"field 无 TTL"，自然要问"那 key 的 TTL 怎么生效"——Redis 用两种策略配合：

| 策略       | 机制                             | 优点          | 缺点                 |
| -------- | ------------------------------ | ----------- | ------------------ |
| **惰性删除** | 访问 key 时才检查过期时间戳，过期就删并返回 nil  | 省 CPU，按需触发  | 没人访问的过期 key 会一直占内存 |
| **定期删除** | 每秒 10 次随机抽样一批带 TTL 的 key，删掉已过期的 | 主动回收内存，限制时长 | 抽样有漏网，不能保证全清       |

> 面试常问："为什么不用定时器精确删除每个过期 key？"——答：为每个 key 维护一个定时器，内存与 CPU 开销都不可接受；Redis 选择用「惰性 + 抽样」在 **CPU 与内存之间取折中**。

#### 8. 本项目登录态的演进路径

| 阶段       | 结构                                  | 多设备 | 登出/踢人是否分离              |
| -------- | ----------------------------------- | --- | ---------------------- |
| **当前**   | `ie:auth:token:{userId}` String 单槽  | ❌ 互踢 | ❌ 效果重合（删同一个 key）       |
| 演进 1     | Hash `ie:auth:sessions:{userId}`    | ✅   | ✅ `HDEL` vs `DEL`      |
| 演进 2     | Hash + JWT 带 `sid` claim            | ✅   | ✅ 且支持设备列表、远程下线指定设备    |
| 另选方案     | `jti` + `ver` 会话版本号                 | ✅   | 一个版本号管全部 token，无需枚举会话 |

> 演进 1/2 改的是**存储结构**，`ver` 方案改的是**校验依据**——后者对 JWT 的无状态性破坏更小，但拿不到"设备列表"这类管理能力。二者也可以组合：Hash 存会话 + `ver` 做全局快速失效。

---

## Spring Boot 启动流程主线 + 缓存预热钩子（降维记忆版）

- 学于：2026-09-03
- 关联模块：`UmsApplication` + 4 个 starter 的 `AutoConfiguration.imports` + `SecurityAutoConfiguration.jwtUtil`（fail-fast 真实锚点）
- 来源：TD §3.1、SpringBoot 源码 `SpringApplication.run()`

> 目标：**只记主线骨架，不背冷门扩展点**。本章回答两个问题：① Spring Boot 启动到底干了啥；② 三种缓存预热钩子分别钉在哪一步、为什么时机不一样。
>
> **与下一章的关系**：本章是**时间轴（启动流程）**，下一章是这条时间轴上的**一个横切机制（自动装配）**。先有骨架，再挂细节。

### 0. 先背这张表（全章内容都从它展开）

| 顺序 | 阶段                              | 发生了什么              | 钩子                              | 服务开门没？      |
| --- | ------------------------------- | ------------------ | ------------------------------- | ----------- |
| —   | `new SpringApplication()`       | 收集扩展类、推断应用类型       | —                               | ❌ 还没开始      |
| 1   | `prepareEnvironment()`          | 加载 yml / 环境变量 / 命令行参数 | —                               | ❌           |
| 2   | `createApplicationContext()`    | 容器对象诞生（空的）         | —                               | ❌           |
| 3   | `refresh(context)`              | **造 Bean、依赖注入**    | **`@PostConstruct`**            | ❌           |
| 4   | `callRunners()`                 | 执行 Runner          | **`CommandLineRunner`**         | ❌（精确说明见第 5 节） |
| 5   | `publishApplicationReadyEvent()` | 发布就绪事件             | **`@EventListener(ReadyEvent)`** | ✅ 开了        |
| 6   | `return context`                | 启动完成               | —                               | ✅           |

> **记忆锚点只有三个字：开门没？** —— 预热必须赶在"开门"之前做完。

### 1. 降维：一行代码拆两步

你写项目日常只有一行：

```java
SpringApplication.run(UmsApplication.class, args);
```

拆成两步：

```java
SpringApplication app = new SpringApplication(UmsApplication.class);  // ① 备料
app.run(args);                                                        // ② 下锅
```

### 2. ① new SpringApplication() —— 只买菜，不下锅

**只干三件事，一件"活"都不干**：

1. **推断应用类型**（`deduceWebApplicationType()`）：看 classpath 里有没有 `DispatcherServlet` / `DispatcherHandler`，判定是 **SERVLET**（普通 Web）/ **REACTIVE**（WebFlux）/ **NONE**（非 Web）。**这决定了第 2 步创建哪种容器。**
2. **收集扩展类**：把 `ApplicationContextInitializer`、`ApplicationListener` 从 `spring.factories` 读出来存进集合，留着 `run()` 时调用。
3. **记录主类**：记住启动类是哪个——后面 `@ComponentScan` 要以**它的包**为扫描起点。

> **不创建容器、不读 yml、不造 Bean、不连 Redis 数据库。**
>
> 👉 记忆：**new = 买菜备菜，还没下锅。**（`ApplicationContextInitializer`、`ApplicationListener` 属于"备好的配菜"，**面试不追问扩展点就直接忽略**）

### 3. ② run() 的 6 步骨架（删掉所有无关代码）

```java
public ConfigurableApplicationContext run(String... args) {
    // 1. 准备环境：系统变量、命令行参数、application.yml 全部加载进 Environment
    ConfigurableEnvironment environment = prepareEnvironment();

    // 2. 创建 IOC 容器（此时是空的，一个 Bean 都没有）
    ConfigurableApplicationContext context = createApplicationContext();
    context.setEnvironment(environment);

    // 3. 【核心】容器刷新：读配置类 → 造 Bean → 依赖注入
    refresh(context);
    //    ★ 自动装配在这一步的前半段（第 ⑤ 子步，见下一章）
    //    ★ @PostConstruct 在这一步的后半段（第 ⑪ 子步，每个 Bean 造完就触发）

    // 4. refresh 全部结束，执行 Runner
    callRunners(context, args);
    //    ★ CommandLineRunner / ApplicationRunner 在这里

    // 5. 发布就绪事件（同时把 Readiness 状态改为 ACCEPTING_TRAFFIC）
    publishApplicationReadyEvent(context);
    //    ★ @EventListener(ApplicationReadyEvent.class) 在这里

    // 6. 返回容器，启动完成
    return context;
}
```

> 就这 6 步。三个预热钩子**全部钉死在上面注释的位置**。

### 4. 三个钩子分别在哪、为什么时机不一样

#### ① `@PostConstruct` —— 第 3 步 refresh 内部

- **触发时机**：**当前这一个 Bean** 实例化 + 依赖注入完成后立刻执行。
- **坑**：只代表**这一个 Bean 造完了**，别的 Bean 可能还没开始造。
- **后果**：在这个方法里用 `RedisTemplate` / `Mapper`，而它们还没被创建 → **空指针**。
- **附带问题**：每个 Bean 都会执行一次，语义上也不适合做"全局一次性"的事。
- **结论**：❌ **不能做全局缓存预热**。

#### ② `CommandLineRunner` / `ApplicationRunner` —— 第 4 步 callRunners()

- **触发时机**：`refresh()` **全部结束**，`RedisTemplate`、`Mapper`、所有业务 Bean **全部就绪**。
- **关键**：此时外部流量还没打进来（精确说明见下节）。
- **结论**：✅ **业务首选的缓存预热时机**——东西全备好了，客人还没进门。

#### ③ `@EventListener(ApplicationReadyEvent.class)` —— 第 5 步

- **触发时机**：Runner 跑完之后，服务**已对外宣告就绪**。
- **坑**：预热逻辑慢的话，用户请求已经进来、缓存还没做好 → **直接打穿数据库**。
- **结论**：⚠️ 只适合"发通知、打日志、注册到服务发现"这类动作，**不适合重预热**。

#### 时序一句话（牢牢记住）

```
@PostConstruct  →  CommandLineRunner  →  ApplicationReadyEvent
   （早）                                       （晚）
```

### 5. 面试加分：Tomcat 端口到底哪一步开的（纠偏）

第 4 节说"CommandLineRunner 时服务还没接收请求"，这是**面试标准答法**；严格讲有细节，被追问时要能补上。

`refresh(context)` 内部其实有 12 个子步骤，关键是这三个：

```
refresh() 内部（简化）:
  ⑤ invokeBeanFactoryPostProcessors()  ← 【自动装配在这】读 imports、注册 BeanDefinition
  ⑥ registerBeanPostProcessors()
  ⑨ onRefresh()                        ← 创建 WebServer 对象（Tomcat 实例，还没监听端口）
  ⑪ finishBeanFactoryInitialization()  ← 【@PostConstruct 在这】实例化所有单例 Bean
  ⑫ finishRefresh()
       ├─ webServer.start()            ← 【端口真正开始监听】
       └─ 发布 ContextRefreshedEvent
```

**所以精确版本是**：

| 时点                            | Tomcat 端口状态                    |
| ----------------------------- | ------------------------------ |
| `@PostConstruct`（第 ⑪ 步）        | 还没监听（WebServer 对象都还没建）          |
| `CommandLineRunner`（第 4 步）     | **端口已经监听**（socket 能收连接了）        |
| `ApplicationReadyEvent`（第 5 步） | 端口监听 **+ 已对外宣告就绪**             |

**那为什么仍然推荐 `CommandLineRunner` 做预热？** 因为**"端口能收连接" ≠ "流量会打进来"**：

- Spring Boot 在发布 `ApplicationReadyEvent` 的**同时**，把 Readiness 状态改成 `ACCEPTING_TRAFFIC`；
- K8s 的 readinessProbe、服务注册中心 / 负载均衡看的是这个状态，**在此之前不会把流量导过来**；
- 所以 `callRunners()` 阶段外部流量**实际上进不来**——这正是预热的窗口。

> **面试怎么答**：先给标准版（"CommandLineRunner 时服务还没接收请求，预热首选"）；被追问再补精确版（"严格讲 `finishRefresh` 阶段端口就开始监听了，但 Readiness 要等 `ApplicationReadyEvent` 才变为 `ACCEPTING_TRAFFIC`，K8s 在此之前不导流，所以 `callRunners` 仍是最佳窗口"）。**能说出后半段的，是读过源码的。**

### 6. 与下一章「自动装配」的关系（两章串起来）

| 章节   | 讲的是什么                 | 在时间轴上的位置                     |
| ---- | ---------------------- | ---------------------------- |
| 本章   | **启动流程主线**（一条时间轴）      | 全程 6 步                       |
| 下一章  | **自动装配**（时间轴上的一个横切机制） | 钉在 `refresh()` 的**第 ⑤ 子步** |

**自动装配为什么在第 ⑤ 子步？**

`@EnableAutoConfiguration` → `@Import(AutoConfigurationImportSelector.class)`；而 `AutoConfigurationImportSelector` 是个 `DeferredImportSelector`，由 `ConfigurationClassPostProcessor`（一个 `BeanFactoryPostProcessor`）处理——**`BeanFactoryPostProcessor` 的语义就是"在所有 Bean 实例化之前，先加工 Bean 的定义"**。

由此得到一条**很有用的推论**：

> **自动装配（第 ⑤ 子步）早于 `@PostConstruct`（第 ⑪ 子步）。**
> 所以 starter 的 `imports` 文件里登记的 Bean，**在你自己业务 Bean 的 `@PostConstruct` 执行时已经全部注册完了**——starter 的 Bean 可以放心注入。

### 7. 大白话故事版：开饭店

| 步骤                              | 饭店在干嘛                                                     | 对应代码                     |
| ------------------------------- | --------------------------------------------------------- | ------------------------ |
| `new SpringApplication()`       | 去菜市场采购，食材工具全买回来放厨房。**没做饭、没开门**                            | 收集扩展类 + 推断应用类型           |
| ① `prepareEnvironment()`        | 看菜谱，yml / 命令行参数全部读完。**还没做菜**                              | 加载配置                     |
| ② `createApplicationContext()`  | 租好包间（容器诞生），房间是空的                                          | 创建容器                     |
| ③ `refresh()`                   | **厨师开始炒菜（造 Bean）**                                        | 核心                       |
| ↳ 第 ⑤ 子步                        | 先按《人才登记表》把外聘师傅（starter 配置类）请进来                            | 自动装配                     |
| ↳ 第 ⑪ 子步 `@PostConstruct`       | 第一道菜出锅。**只这一道好了，别的还没下锅，不能开席**                             | 单 Bean 就绪                |
| ↳ 第 ⑫ 子步                        | 包间门打开（Tomcat 端口监听），但**招牌灯还没开**                            | 端口监听                     |
| ④ `callRunners()`               | ✅ **全部菜齐了，但招牌灯没开、客人不知道能进**。赶紧把预制菜（DB 数据）摆上前台货架（Redis） | **`CommandLineRunner`（预热首选）** |
| ⑤ `ApplicationReadyEvent`       | **招牌灯亮，正式营业**。这里才摆货 → 客人已进门、货架是空的 → 冲后厨（DB）            | 服务就绪                     |

> 对照表：**菜 = Bean｜货架 = Redis 缓存｜客人 = 用户请求｜招牌灯 = Readiness 就绪状态**

### 8. 我在项目里怎么用的（真实锚点）

**锚点一：项目里目前没有任何缓存预热**

全仓库搜不到 `@PostConstruct` / `CommandLineRunner` / `ApplicationReadyEvent`——**这是待补的能力**。将来做权限缓存、角色缓存预热时，按本章结论优先选 `CommandLineRunner`。

**锚点二：项目里有一个"启动期就失败"的真实例子**

`SecurityAutoConfiguration.jwtUtil()` 在 `@Bean` 方法里做 fail-fast 校验：

```java
@Bean
@ConditionalOnMissingBean(JwtUtil.class)
public JwtUtil jwtUtil(SecurityProperties properties, Environment environment) {
    String secret = properties.getJwtSecret();
    if (secret == null || secret.isBlank()) {
        throw new IllegalStateException("insight.security.jwt-secret 未配置，拒绝启动。...");
    }
    if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
        throw new IllegalStateException("insight.security.jwt-secret 长度不足 32 字节，拒绝启动");
    }
    ...
}
```

**它钉在时间轴的哪里？** —— **第 3 步 `refresh()` 的 Bean 实例化阶段**（`@Bean` 方法被调用时）。

这解释了为什么它效果这么好：

- 它**早于** Tomcat 端口监听（第 ⑫ 子步）→ 配置有问题，服务**根本开不了门**，绝不会带着弱密钥对外提供服务；
- 它**晚于** `prepareEnvironment()`（第 1 步）→ 所以方法里能读到 `Environment`（代码里正是用 `environment.getActiveProfiles()` 判断 prod 环境）。

> **这就是"选对钩子"的价值**：校验放对位置，既能拿到该拿的东西，又能赶在开门前把问题拦住。

### 9. 面试极简回答模板（脑子里只存这套）

> Spring Boot 入口是 `SpringApplication.run()`，内部拆成几个大阶段：先准备环境，加载 yml 和命令行参数；再创建 IOC 容器；然后执行 `refresh()` 刷新容器、实例化所有 Bean。**`@PostConstruct` 就在 Bean 实例化阶段执行，但它只代表单个 Bean 完成，别的 Bean 可能还没造好，会空指针，不适合全局缓存预热。**
> `refresh()` 全部完成后执行 `CommandLineRunner`，**此时所有 Bean 已经就绪，但服务还没对外宣告就绪、流量还没进来**，适合做缓存预热，把数据库数据加载进 Redis。
> Runner 之后触发 `ApplicationReadyEvent`，代表服务完全就绪；此时再预热，耗时长的话会有请求打穿缓存的风险。

### 10. 必背 2 个反问（高频）

**Q1：为什么不用 `@PostConstruct` 做缓存预热？**

答：它只是**当前 Bean** 初始化完成，`RedisTemplate`、`Mapper` 这些依赖可能还没创建 → 空指针。而且每个 Bean 都会触发一次，语义上也不适合做"全局一次性"的事。

**Q2：`CommandLineRunner` 和 `ApplicationReadyEvent` 选哪个？**

答：优先 `CommandLineRunner`。它执行时所有 Bean 就绪，但 Readiness 状态还没变成 `ACCEPTING_TRAFFIC`，K8s / 注册中心不会导流，预热完流量才进来。`ApplicationReadyEvent` 时服务已宣告就绪，预热耗时长会有请求打穿缓存的风险。

### 11. 可以先不记（避免大脑过载）

❌ 暂时屏蔽：

- `ApplicationContextInitializer`
- `ApplicationStartingEvent` / `ApplicationEnvironmentPreparedEvent` / `ApplicationPreparedEvent` / `ContextRefreshedEvent`
- `ConfigFileApplicationListener`
- `SpringApplicationRunListeners` 的七个回调

> 这些是底层扩展组件，**面试不问"Spring Boot 扩展点"完全不用背**。

✅ 必须记住的只有 6 条：

1. `new SpringApplication()`：备料，不干活（顺带推断应用类型）
2. `run()` 六步骨架
3. `refresh()` 内：**第 ⑤ 子步 = 自动装配；第 ⑪ 子步 = `@PostConstruct`；第 ⑫ 子步 = 端口监听**
4. `callRunners()` = `CommandLineRunner`（**预热首选**）
5. `ApplicationReadyEvent` = 服务就绪、可接客
6. 顺序：`@PostConstruct` < `CommandLineRunner` < `ApplicationReadyEvent`

---

## Spring Boot 自动装配机制（AutoConfiguration.imports）

- 学于：2026-08-26（2026-09-03 补：标注它在 `run()` 的哪一步）
- 关联模块：4 个 starter 的 `config` 包（`WebAutoConfiguration` / `SecurityAutoConfiguration` / `MybatisAutoConfiguration` / `RedisAutoConfiguration`）+ 各 starter 的 `META-INF/spring/...AutoConfiguration.imports` 文件
- 来源：TD §3.1

> 目标：彻底搞懂「starter 引进来为什么 Bean 就自动生效了」。核心就一个词——**自动装配（Auto-Configuration）**。

**📍 本章在时间轴上的位置**（承上启下，先读上一章「启动流程主线」）：

```
run() 六步骨架
  └─ 第 3 步 refresh(context)
        └─ 第 ⑤ 子步 invokeBeanFactoryPostProcessors()   ← ★ 自动装配就钉在这里
```

**为什么是这里？** `@EnableAutoConfiguration` → `@Import(AutoConfigurationImportSelector.class)`；而 `AutoConfigurationImportSelector` 是个 `DeferredImportSelector`，由 `ConfigurationClassPostProcessor`（一个 `BeanFactoryPostProcessor`）处理。`BeanFactoryPostProcessor` 的语义就是**"在所有 Bean 实例化之前，先加工 Bean 的定义"**——正合适。

**推论**：自动装配（第 ⑤ 子步）**早于** `@PostConstruct`（第 ⑪ 子步）。所以 starter 的 Bean 在你自己业务 Bean 的 `@PostConstruct` 执行时已全部注册完毕，可以放心注入。

### 直观类比（先建立直觉）

把 Spring 容器想成一个**工厂车间**：

1. **你的启动类 = 车间主任**，只负责喊一声「开工」（`SpringApplication.run`）。
2. **`@ComponentScan` = 车间主任自己的巡视范围**——他只认识自己办公室附近（启动类所在包）的员工，**跨部门的员工他看不见**。
3. **starter 的配置类 = 隔壁部门的老师傅**，他不归车间主任管，主任也不会主动去找他。
4. **`AutoConfiguration.imports` 文件 = 全厂的《人才登记表》**——工厂人事（`AutoConfigurationImportSelector`）启动时会拿着这张表，把表上登记的「老师傅（配置类）」一个个请出来干活。

> 一句话记忆：**启动类自己只会扫自己包（ComponentScan）；starter 的类在别的包，靠 `AutoConfiguration.imports` 这张「登记表」才能被 Spring 找到并生效。**

### 核心原理

#### 1. 先回答最关键的问题：没有这个文件会怎样？

**结论：Bean 不会创建，但项目不报错、能正常编译启动——只是功能「静默失效」。**

原因：`@Configuration` 注解只能让类**具备**「成为配置类」的**资格**，不能让它**被 Spring 找到**。Spring 默认只扫描启动类所在包及其子包（`@ComponentScan`）。而 starter 的包是 `com.insightengine.starter.web`，启动类在 `com.insightengine.ums`——**跨包了，扫不到**。

结果：`TraceFilter`、`JwtAuthFilter`、`RedisTemplate` 这些 Bean 一个都不会创建，但启动不报错，直到你调用功能才发现「没生效」。

#### 2. 没有这个文件时，三种「手动」写法（对比理解）

| 写法                       | 代码                                                           | 缺点                                     |
| ------------------------ | ------------------------------------------------------------ | -------------------------------------- |
| A. 手动 `@Import`          | 启动类上 `@Import({WebAutoConfiguration.class, ...})`            | 每引一个 starter 加一行，漏了就不生效，还耦合具体类名        |
| B. 大范围 `@ComponentScan`  | `@ComponentScan("com.insightengine")`                        | 扫进不该扫的类，失去「按需引入」控制力                    |
| C. 旧版 `spring.factories` | `META-INF/spring.factories` 里写 `EnableAutoConfiguration=...` | Spring Boot 2.x 的老做法，3.x 已用 imports 取代 |

#### 3. `AutoConfiguration.imports` 到底怎么起作用（启动全流程）

**文件位置**（以 starter-web 为例）：

```
insight-engine-starter-web/src/main/resources/
└── META-INF/spring/
    └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

**文件内容**（每行一个全限定类名）：

```
com.insightengine.starter.web.config.WebAutoConfiguration
```

**启动时的完整流程**：

```
启动类 UmsApplication
  └─ @SpringBootApplication（组合注解）
       ├─ @ComponentScan          → 扫自己包（com.insightengine.ums.*）
       └─ @EnableAutoConfiguration
            └─ @Import(AutoConfigurationImportSelector.class)   ← 核心
                 │
                 ├─ ① 扫描 classpath 下所有 jar/模块的
                 │     AutoConfiguration.imports 文件
                 ├─ ② 收集文件里列出的所有类名 → 候选配置类列表
                 ├─ ③ 逐个判断「条件注解」是否满足
                 │     （@ConditionalOnClass / @ConditionalOnMissingBean / @ConditionalOnWebApplication）
                 ├─ ④ 满足条件的类 → 加载进容器，执行其 @Bean 方法
                 └─ ⑤ 不满足的类 → 跳过
```

> 关键：`@EnableAutoConfiguration` 是 `@SpringBootApplication` 内置的，**启动时自动触发，你什么都不用写**。

#### 4. 条件注解：决定「配置类是否真的生效」

看三个真实条件注解的用法：

**① `@ConditionalOnWebApplication`（只在 Servlet 栈生效）**

`WebAutoConfiguration.java`：

```java
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class WebAutoConfiguration {
```

- 作用：普通 Web 服务（WebMvc/Servlet）才生效，WebFlux（gateway）不生效
- 意义：防止给 gateway 误注册 Servlet Filter（`TraceFilter` 等是 Servlet 体系，WebFlux 用不了）

**② `@ConditionalOnMissingBean`（容器里已有就不重复创建）**

`SecurityAutoConfiguration.java`：

```java
@Bean
@ConditionalOnMissingBean(PasswordEncoder.class)
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(10);
}
```

- 作用：容器里**已经存在**同类型 Bean 时，就不创建自己的，避免冲突
- 意义：允许业务方自定义覆盖 starter 的默认实现（「可扩展」的关键）

**③ `@ConditionalOnMissingBean(name=...)`（按名字判断）**

`RedisAutoConfiguration.java`：

```java
@Bean
@ConditionalOnMissingBean(name = "redisTemplate")
public RedisTemplate<String, Object> redisTemplate(...) {
```

- 作用：已有名为 `redisTemplate` 的 Bean 就不覆盖（Spring Boot 官方 Redis 自动配置默认就注册了这个名字）

#### 5. 为什么 Spring Boot 3 用 imports 取代 spring.factories？

| 对比项  | `spring.factories`（2.x）     | `AutoConfiguration.imports`（3.x）                                                   |
| ---- | --------------------------- | ---------------------------------------------------------------------------------- |
| 文件位置 | `META-INF/spring.factories` | `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` |
| 文件性质 | 一个文件塞多种类型（配置、监听器、初始化器）      | **专门只放自动配置类**                                                                      |
| 读取方式 | 启动时全量加载                     | 惰性、按需、配合条件注解过滤                                                                     |
| 职责   | 混合、易冲突                      | 单一、语义清晰                                                                            |

动机：**职责单一 + 加载性能优化**。文件路径本身说明了用途，且能更精准控制加载顺序和条件过滤。

### 我在项目里怎么用的（4 个 starter 的对应关系）

| starter            | imports 文件登记的类              | 装配出的关键 Bean                                                       |
| ------------------ | --------------------------- | ----------------------------------------------------------------- |
| `starter-web`      | `WebAutoConfiguration`      | TraceFilter / UserContextFilter / GlobalExceptionHandler          |
| `starter-mybatis`  | `MybatisAutoConfiguration`  | 分页拦截器 / 审计填充器 / 逻辑删除配置                                            |
| `starter-redis`    | `RedisAutoConfiguration`    | JSON 序列化的 RedisTemplate                                           |
| `starter-security` | `SecurityAutoConfiguration` | SecurityFilterChain / JwtUtil / JwtAuthFilter / PasswordEncoder 等 |

**关键验证**：`UmsApplication.java` 只写了启动类和 `@MapperScan`：

```java
@SpringBootApplication
@MapperScan("com.insightengine.ums.mapper")
public class UmsApplication {
    public static void main(String[] args) {
        SpringApplication.run(UmsApplication.class, args);
    }
}
```

它**没有** `@Import` 任何 starter 的配置类，也没有大范围 `@ComponentScan`，但 UMS 启动后 `JwtAuthFilter`、`RedisTemplate`、分页插件、全局异常处理**全部自动生效**——这就是 4 个 imports 文件在幕后起的作用。

`SecurityAutoConfiguration.java` 还叠加了多个注解，是「配置类怎么增强」的典型：

```java
@Configuration
@EnableWebSecurity        // 激活 Spring Security Web 安全
@EnableMethodSecurity     // 激活 @PreAuthorize 方法级权限
@EnableConfigurationProperties(SecurityProperties.class)  // 绑定 insight.security.* 配置
public class SecurityAutoConfiguration {
```

### 完整心智模型（一句话串起来）

> **`@SpringBootApplication`（含 `@EnableAutoConfiguration`）启动时，`AutoConfigurationImportSelector` 扫描所有 jar 的 `AutoConfiguration.imports` 文件 → 得到候选配置类列表 → 用条件注解过滤 → 把通过的类注册进容器执行其 `@Bean`。没有它，就得回退到手动 `@Import` 或大范围 `@ComponentScan`。**

### 面试可能追问

- **Q1：`@Configuration` 注解的类，Spring 一定能扫到吗？为什么？**
  
  - 答：不一定。`@Configuration` 只赋予类「成为配置类」的资格，不负责「被发现」。Spring 默认只扫启动类所在包（`@ComponentScan`），跨包的配置类扫不到。要让跨包的配置类生效，要么放进 `AutoConfiguration.imports`（自动装配），要么手动 `@Import`，要么扩大 `@ComponentScan` 范围。

- **Q2：starter 的原理是什么？为什么「引依赖即生效」？**
  
  - 答：starter 的本质 = 依赖包 + 自动配置类 + `AutoConfiguration.imports` 登记。引入 starter 后，它的 jar 进 classpath，启动时 `AutoConfigurationImportSelector` 读到它 jar 里的 imports 文件，把配置类加载进容器，Bean 自动装配完成。全程无需业务方写任何配置代码。

- **Q3：`@ConditionalOnMissingBean` 有什么用？什么场景必须加？**
  
  - 答：作用是「容器里已有同类型 Bean 就不重复创建」，实现 starter 的**可覆盖性**。典型场景：业务方想用自己的 `PasswordEncoder` 实现替代 starter 默认的 BCrypt，只要自己声明一个 Bean，starter 的就会自动跳过，不冲突。

- **Q4：`spring.factories` 和 `AutoConfiguration.imports` 有什么区别？**
  
  - 答：都是「自动配置的登记方式」，前者是 Spring Boot 2.x 老机制（一个文件混合多种类型），后者是 3.x 新机制（专用于自动配置类、路径隔离、加载更高效）。Spring Boot 3 项目应只用 imports 文件。

- **Q5：自动装配和 `@ComponentScan` 有什么区别？会不会重复加载？**
  
  - 答：`@ComponentScan` 扫「启动类所在包」的注解类（`@Component`/`@Service`/`@Configuration` 等）；自动装配加载「imports 文件登记」的跨包配置类。两者作用范围不同、互补不重复。配置类上的 `@ConditionalOnMissingBean` 还能进一步保证即使被两处都扫到也不会创建重复 Bean。

### 踩坑提醒

1. **坑：starter 里的配置类写了 `@Configuration`，但没在 imports 文件登记，结果 Bean 静默不生效**
   
   - 现象：项目编译、启动都正常，但功能「莫名其妙不工作」（比如 TraceFilter 没生效、RedisTemplate 是默认序列化）。
   - 规避：写完自动配置类，**必须**同时在该 starter 的 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 里加一行全限定类名，二者缺一不可。

2. **坑：imports 文件路径写错，Spring 读不到**
   
   - 现象：文件放在了错误目录，导致 `AutoConfigurationImportSelector` 扫描不到，配置类不加载。
   - 规避：路径必须精确是 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`（注意是 `spring` 目录下的 `AutoConfiguration.imports`，不是 `spring.factories`）。

3. **坑：把业务服务自己的配置类也塞进 imports 文件**
   
   - 现象：业务配置类本该由 `@ComponentScan` 管理，却混进自动装配，导致加载顺序、条件判断混乱，且被所有引入方共享。
   - 规避：imports 文件**只登记 starter 的自动配置类**；业务服务自己的配置类用 `@ComponentScan`（放启动类包下）即可，职责分开。

---

## JWT 无状态 vs 黑名单/登录态：登出、改密、禁用三种失效场景

- 学于：2026-08-26
- 关联模块：`AuthServiceImpl.logout()` / `UserServiceImpl.updatePassword()` / `UserServiceImpl.updateStatus()` + `RedisTokenBlacklistService` + `JwtAuthFilter`
- 来源：TD ADR-10、PRD §12

> 目标：搞懂「JWT 无状态」到底带来什么问题、三种「要让已签发 token 失效」的场景分别怎么处理、它们背后的设计权衡。

### 直观类比（先建立直觉）

**JWT = 一张「自带防伪水印的门禁卡」**

- 卡上**直接印了你的身份信息**（用户 ID、角色、权限），谁刷都能读出你是谁，**不需要去后台查系统**（这就是「无状态」）。
- 好处：门卫（服务端）不用每次查数据库、不用记录谁在线，**任何一台闸机（任意服务实例）都能独立验卡**，天然适合多实例部署。
- 坏处：卡一旦发出去，**门卫无法「远程作废」这张卡**——卡没到期、水印是真的，门卫就只能放行。要作废，只能靠「额外登记」。

> 一句话记忆：**JWT 无状态 = 服务端不存会话，好处是易扩展，代价是「无法主动撤销」。要撤销，就得引入「有状态」的黑名单/登录态来补。**

### 核心原理

#### 1. JWT 登录完整流程（三步走）

**第 1 步：登录（一次性）**——`AuthServiceImpl.login()` 依次做 5 件事：

```
① 查锁定：Redis 里有没有 ie:auth:lock:{account}
② 查用户：按账号（email/phone）查 DB，拿到用户（含 password_hash 密文）
③ 查状态：账号是否被禁用
④ 验密码：BCrypt.matches(明文, 密文) —— 比对，不是解密
⑤ 签发两个 token：
     access token（2 小时，内含 userId + 角色 + 权限）
     refresh token（7 天，只含 userId）
```

登录成功后，服务端返回**两个 token**（不是密码、不是 Session）：

```json
{
  "code": 0,
  "data": {
    "token": "eyJ...(access token，2小时)",
    "refreshToken": "eyJ...(refresh token，7天)",
    "expiresIn": 7200,
    "user": { "id": 1, "roles": ["super_admin"] }
  }
}
```

**第 2 步：前端把 token 存起来**（`localStorage` 或内存），之后每次请求都带上：

```js
localStorage.setItem('accessToken', res.data.token)
localStorage.setItem('refreshToken', res.data.refreshToken)
```

**第 3 步：之后每次请求都带 access token**，服务端 `JwtAuthFilter` 校验：

```
GET /api/v1/user/page
Authorization: Bearer eyJ...(access token)
        ↓
① 查黑名单（这个 token 登出过吗？）
② 验签名（是不是我们发的、有没有被篡改）
③ 验过期（2 小时到了没）
④ 验登录态（Redis 里 ie:auth:token:{userId} 还在吗？摘要跟当前 token 匹配吗？）→ 被改密/禁用/顶号则 401
⑤ 都通过 → 解析出 userId + 角色 + 权限 → 放行
```

> 这就是「无状态」：第 3 步的每次请求，服务端**不查数据库**，只靠 token 自带信息验证身份和权限——唯一例外是第 ④ 步对登录态缓存做一次 Redis 读取（换来「改密/禁用可踢人」的有状态撤销能力）。

#### 2. token 过期如何「无感刷新」（重点）

**为什么两个 token？—— 有效期矛盾**

|      | access token | refresh token      |
| ---- | ------------ | ------------------ |
| 作用   | 每次请求证明身份     | 专门换新的 access token |
| 有效期  | 2 小时（短）      | 7 天（长）             |
| 泄露后果 | 2 小时内有效，危害小  | 7 天内有效，危害大         |
| 携带方式 | 每次请求都带       | 平时不用，只在刷新时用一次      |

核心矛盾：access 要短（安全），但太短用户要频繁重新登录（体验差）。解决：access 短（安全）+ refresh 长（体验），access 过期用 refresh 悄悄换新。

**access 过期时的完整时序**：

```
① 前端带着「已过期的 access token」请求接口
        ↓
② 服务端 JwtAuthFilter 校验 → 发现过期
        ↓
③ 返回 401 + code=2007（token 已过期，注意不是 2001）
        ↓
④ 前端「响应拦截器」捕获 code=2007
        ↓
⑤ 拦截器自动做「无感刷新」三步：
   a. 拿 localStorage 里的 refreshToken
   b. 调 POST /auth/refresh {"refreshToken":"..."}
   c. 服务端 refresh() 校验 refresh token 有效 → 签发全新 token 对
        ↓
⑥ 前端用新 access token 重发刚才失败的请求
        ↓
⑦ 请求成功，用户完全无感知
```

**「无感」的本质** = 前端拦截器自动处理，用户不参与。没有拦截器 → 请求 401 → 跳登录页 → 被迫重新输密码（有感）。

**前端拦截器核心逻辑（伪代码）**：

```js
axios.interceptors.response.use(
  (response) => response,   // 正常响应直接返回
  async (error) => {
    // 判断是否 token 过期（code === 2007）
    if (error.response.data.code === 2007) {
      // ① 用 refresh token 换新 token
      const newTokens = await axios.post('/auth/refresh', {
        refreshToken: localStorage.getItem('refreshToken')
      })
      // ② 存新 token
      localStorage.setItem('accessToken', newTokens.data.token)
      localStorage.setItem('refreshToken', newTokens.data.refreshToken)
      // ③ 用新 token 重发刚才失败的请求（"无感"的关键）
      error.config.headers.Authorization = 'Bearer ' + newTokens.data.token
      return axios(error.config)
    }
    // refresh token 也过期（code=2001）→ 只能重新登录
    if (error.response.data.code === 2001) {
      window.location.href = '/login'
    }
  }
)
```

**refresh token 也过期了怎么办？** access 过期 → refresh 兜底换新；refresh 也过期（7 天过了）→ 彻底没救，只能重新登录。这就是 refresh 设 7 天而非无限期的原因——既要「少登录」，又要「最终必须重新验证一次身份」。

#### 3. 什么是有状态 / 无状态？

|        | 有状态（Session）           | 无状态（JWT）      |
| ------ | ---------------------- | ------------- |
| 身份存哪   | 服务端存 Session（内存/Redis） | 客户端 token 里自带 |
| 每次请求   | 拿 sessionId 去服务端查      | 直接解析 token 验签 |
| 能否主动登出 | ✅ 删 Session 即可         | ❌ 无法直接撤销      |
| 多实例扩展  | 要共享 Session（粘性/Redis）  | ✅ 任意实例都能验     |

#### 4. 三种「要让 token 失效」的场景（核心）

| 场景     | 粒度          | 项目做法   | 代码位置                               |
| ------ | ----------- | ------ | ---------------------------------- |
| **登出** | 单个 token    | 加黑名单   | `AuthServiceImpl.logout()`         |
| **改密** | 该用户全部 token | 删登录态缓存 | `UserServiceImpl.updatePassword()` |
| **禁用** | 该用户全部 token | 删登录态缓存 | `UserServiceImpl.updateStatus()`   |

**关键区别：登出是「单 token」粒度，改密/禁用是「单用户」粒度。**

- 登出只作废「当前这一次登录」的 token → 用**黑名单**（按 token 记）
- 改密/禁用要作废「这个用户所有已签发的 token」→ 用**登录态缓存**（按 userId 记，一个 key 管全部）

#### 5. 三种场景的代码链路

**① 登出（单 token → 黑名单）**

`AuthServiceImpl.logout()`：

```java
public void logout(String accessToken) {
    long remainingSeconds = jwtUtil.getRemainingSeconds(accessToken);
    if (remainingSeconds > 0) {
        tokenBlacklistService.blacklist(accessToken, remainingSeconds);  // 加黑名单
    }
    stringRedisTemplate.delete(KEY_AUTH_TOKEN + userId);  // 顺带删登录态
}
```

**② 改密（单用户 → 删登录态）**

`UserServiceImpl.updatePassword()`：

```java
userMapper.updateById(update);  // 先改密码
// 改密后旧登录态全部失效，强制重新登录
stringRedisTemplate.delete(AuthConstants.KEY_AUTH_TOKEN + userId);
```

**③ 禁用（单用户 → 删登录态）**

`UserServiceImpl.updateStatus()`：

```java
if (request.getStatus() == AuthConstants.ACCOUNT_DISABLED) {
    stringRedisTemplate.delete(AuthConstants.KEY_AUTH_TOKEN + id);  // 踢下线
}
```

#### 6. 校验时的顺序（谁拦谁）

`JwtAuthFilter.doFilterInternal()` 的校验顺序：

```java
// ① 黑名单优先（登出 token 拦截）：签名前就拦，登出过的一律拒绝
if (blacklistService != null && blacklistService.isBlacklisted(token)) {
    writeUnauthorized(response, ErrorCode.UNAUTHORIZED);
    return;
}
// ② 签名 + 过期校验（JWT 本身有效性）→ 拿到 userId
JwtPayload payload = jwtUtil.parseAccessToken(token);
// ③ 登录态校验（改密/禁用/顶号 token 拦截）：缓存被删或摘要不匹配 → 拒绝
if (sessionService != null && !sessionService.isActive(payload.getUserId(), token)) {
    writeUnauthorized(response, ErrorCode.UNAUTHORIZED);
    return;
}
```

> 关键设计：**黑名单检查在签名校验之前**（登出过的一律拒绝）；**登录态校验在签名之后**（得先解析出 userId 才能查它对应的缓存）。两者共同把「无状态 JWT 无法撤销」补成「可撤销」。

#### 7. 改密/禁用的失效机制：曾经「写而不读」的坑 + 本次修复复盘

**问题**：这套「删缓存实现踢人」的方案，改动前缺了**致命一环——「删缓存」和「查缓存」两头只做了一头**。

**曾经的根因**：

- 写入方删了缓存：`UserServiceImpl.updateStatus()` 禁用时 `delete(ie:auth:token:{id})`；`updatePassword()` 改密时同样删。
- 校验方从不读缓存：`JwtAuthFilter.doFilterInternal()` 只查黑名单 `isBlacklisted(token)`，**从不读 `ie:auth:token:{userId}`**。删了等于白删——过滤器根本不知道缓存被删了。

**修复前现象（时序）**：

```
① 用户 A 已登录，拿到 access token（2h 有效）
② 管理员禁用 A（updateStatus → 删了 ie:auth:token:A）
③ A 拿旧 token 继续请求 → JwtAuthFilter 不读缓存 → 验签通过 → 正常放行 ✅（本该拒绝 ❌）
④ A 还能继续用最多 2 小时，直到 token 自然过期
```

**修复方案对比**（评审时三选一）：

| 方案         | 做法                                                        | 特点                                        |
| ---------- | --------------------------------------------------------- | ----------------------------------------- |
| **A（已采用）** | `JwtAuthFilter` 验签后额外查 `ie:auth:token:{userId}` 存在且摘要匹配   | 改动最小，贴合 TD §6.1 单 key 设计；每次请求多一次 Redis 查询 |
| B          | JWT 引入 `jti` + `ver` 会话版本号，禁用/改密时 `ver`+1 写 Redis，过滤器比对版本 | 更优雅、支持多设备并存，但动 JWT 载荷结构，改动大               |
| C          | 改密/禁用时枚举该用户全部会话 token 加黑名单                                | 需维护 userId→tokenHash 集合，最复杂               |

**为什么本次选 A 而非评审「推荐」的 B**：

- 代码里登录态的「写（`cacheToken`）」和「删（改密/禁用/登出）」**早已写好**，只差过滤器消费——A 是补齐既定设计的最小改动；B 要同时改签发（往 JWT 塞 `ver`）、解析、校验、删四点，属于重构；
- A 的「单会话语义」（同 userId 新登录顶旧 token、多设备互踢）恰与 TD §6.1 的**单 key 设计**一致，不是退化；
- 代价（每次请求多一次 Redis 读）在本项目规模下可接受；待多设备需求出现，再演进 B。

**方案 B（`jti` + `ver`）留作演进方向（若未来要多设备并存）**：

- `jti`（JWT ID）：每个 token 的唯一编号，放进 JWT claim，用于精确标记「某一个」token。
- `ver`（version）：用户级「会话版本号」，改密/禁用时 `ver+1` 写 Redis（key=`ie:auth:ver:{userId}`）。
- 签发时把当前 `ver` 也放进 JWT claim；校验时拿「JWT 里的 ver」和「Redis 里的 ver」比对，不一致就拒绝。
- 好处：**不用枚举每个 token**，一个用户一个 `ver` 就管住他所有 token；且对 JWT 的「无状态」破坏最小（只在版本变化时需要查一次 Redis）。

> 一句话总结：**JWT 无状态无法主动撤销，凡是「要让已签发 token 失效」的诉求，都必须引入一个「有状态的标记」来补——黑名单（单 token）、登录态缓存（单用户）、`ver` 版本号（单用户，更优雅）。**

**本次实际改动**（2026-09-02 落地，3 个新文件 + 2 处修改）：

- starter-security 新增 `TokenSessionService` 接口（与 `TokenBlacklistService` 同模式的可选能力，**未注入实现的服务自动退化为纯 JWT 校验**，不破坏 starter 零 Redis 依赖）；
- `JwtAuthFilter` 在「验签通过后、建立认证前」调用 `sessionService.isActive(userId, token)`（见第 6 节）；
- `SecurityAutoConfiguration` 经 `ObjectProvider<TokenSessionService>` 可选装配；
- UMS 新增 `RedisTokenSessionService`（读 `ie:auth:token:{userId}` 比对 SHA-256）+ `TokenDigestUtil`（JDK MessageDigest 实现，不引 hutool crypto）；
- `AuthServiceImpl.cacheToken` 由存明文改为存 SHA-256 摘要（与注释、与黑名单服务对齐），顺带消掉「明文 token 落 Redis」的泄露面。

### 面试可能追问

- **Q1：JWT 无状态，怎么实现「登出」？**
  
  - 答：JWT 本身无法撤销，靠**黑名单**弥补。登出时把 token 摘要写入 Redis 黑名单（TTL=剩余有效期），每次请求认证过滤器先查黑名单，命中即拒绝。本质是「用有状态的黑名单补无状态 JWT 的短板」。

- **Q2：用户改密码 / 被禁用后，之前签发的 token 怎么失效？**
  
  - 答：这两个场景需要作废「该用户所有 token」，所以按**用户粒度**处理——删登录态缓存 `ie:auth:token:{userId}`。与登出（单 token 黑名单）的粒度不同。

- **Q3：为什么登出用黑名单，改密/禁用用删登录态？能统一吗？**
  
  - 答：粒度不同。登出只作废「当前这一次」token，用黑名单按 token 记；改密/禁用要作废「这个用户全部」token，用登录态按 userId 记（删一个 key 全失效）。技术上能统一（都删登录态），但黑名单还能覆盖「token 被泄露、主动作废某个 token」的场景，职责更清晰。
  - **加分补充（承认局限）**：单槽实现下这个"粒度不同"只是**设计意图**——两者删的是同一个 key，对 access 的拦截效果重合。真正不可替代的是**删 refresh 会话 key**（黑名单只管 access，refresh 从不进黑名单，不删 key 就能用旧 refresh 换全新令牌绕过登出）。详见 Redis 章节「登出为什么同时做三件事」。

- **Q4（深入）：只删登录态缓存，但 JWT 本身没过期，真的能拦住吗？**
  
  - 答：**关键点**——删缓存本身不会自动拦住请求，必须在**校验时查这个缓存**才有效。修复前本项目正栽在这个坑：`JwtAuthFilter` 只查黑名单不查登录态缓存，「删了没人读」，踢人假失效。**修复后**过滤器在验签后调用 `TokenSessionService.isActive(userId, token)` 每次请求比对登录态缓存，缓存被删即 401——「删缓存」与「查缓存」已配套。
  - 更优雅的演进是 **`jti` + `ver` 版本号**：签发时把用户级会话版本 `ver` 放进 JWT，改密/禁用时 `ver+1` 写 Redis，校验时比对版本，不一致即拒绝——一个用户一个版本号就管住他全部 token，无需枚举。本项目当前采用登录态缓存方案（单会话），多设备并存需求出现后再演进。

- **Q5：为什么黑名单/登录态放 Redis 而不是本地内存？**
  
  - 答：微服务多实例部署，本地内存各实例不共享（登出写实例 A、请求打到实例 B 就失效）。Redis 是集中式共享存储，所有实例读同一份。这是「分布式下状态一致性」的核心认知。

### 踩坑提醒

1. **坑（曾踩，已修复）：只删登录态缓存，但认证过滤器不查缓存，导致「删了没用」**
   
   - 现象：改密/禁用后，旧 token 依然能访问（因为 JWT 没过期、过滤器只验签）。
   - 规避：**「删缓存」和「校验时查缓存」必须配套**。修复后本项目过滤器验签后即查登录态缓存（`TokenSessionService.isActive`），该坑已闭合。

2. **坑（比"状态不一致"严重得多）：登出只加黑名单、忘了删 refresh 会话 key**
   
   - 现象：access 被黑名单拦住了，**但 refresh token 从没进过黑名单**。旧 refresh 有 7 天有效期，只要 `ie:auth:refresh:{userId}` 还在，持有者调 `/refresh` 就能换一对全新令牌——**登出形同虚设**。
   - 规避：登出三件事都做——加黑名单（废 access）+ 删 refresh 会话（断续期）+ 删登录态（清状态）。**删 refresh 会话才是必需项。**

3. **坑：改密/禁用删除缓存时用了错误的 key 粒度**
   
   - 现象：想作废「用户全部 token」，却按「单个 token」去删，导致其他设备的 token 还能用。
   - 规避：明确粒度——单 token 用黑名单（key 含 token 摘要），单用户用登录态（key 含 userId）。

4. **坑（已修复）：把完整 token 明文写进 Redis 登录态缓存**
   
   - 现象（修复前）：`cacheToken` 存的是完整 token 明文（注释却写「存摘要」），Redis 一旦被拖库，攻击者直接拿到可用 token。
   - 规避：缓存一律存 **SHA-256 摘要**（和黑名单服务一致），存摘要既能比对、又不暴露原文。已改为 `TokenDigestUtil.sha256Hex(token)` 后入库。

---

## RBAC vs ABAC + 权限进 JWT 的权衡

- 学于：2026-08-26
- 关联模块：`PermissionMapper.selectPermissionCodesByUserId()` / `RoleMapper` / `Role.scope` 字段 / `JwtUtil.createAccessToken()` / `JwtAuthFilter.authenticate()`
- 来源：PRD §12.2、TD §7.4

> 目标：搞懂 RBAC 和 ABAC 的区别、项目用的是哪种、以及「把权限展开写进 JWT」背后的三个权衡。

### 直观类比（先建立直觉）

**RBAC = 按「工牌上的岗位」授权**

公司给员工发工牌，工牌上写「你是经理」，门禁系统一看「经理」就放你进经理办公室。改权限 = 改岗位，不用管你这个人。

**ABAC = 按「现场条件」动态判断**

门禁不看你岗位，而是看「你是谁 + 现在几点 + 在哪个部门 + 要进哪个门」现场算一遍：「张三，工作日 9 点，本部门，普通办公室 → 放行」。

> 一句话记忆：**RBAC 是「提前贴好标签，按标签放行」（粗、简单）；ABAC 是「现场综合判断」（细、灵活、复杂）。**

### 核心原理

#### 1. RBAC vs ABAC 对比

| 维度   | RBAC                    | ABAC            |
| ---- | ----------------------- | --------------- |
| 授权依据 | 角色（role）                | 属性（用户/资源/环境/动作） |
| 粒度   | 粗（一个角色一堆权限）             | 细（可到「谁能看哪条数据」）  |
| 管理成本 | 低，改角色即可                 | 高，规则复杂          |
| 典型场景 | 功能权限（能不能进这个页面）          | 数据权限（能看哪几行数据）   |
| 表结构  | user/role/permission 三张 | 需属性+规则引擎        |

#### 2. 项目用的是哪种？

**RBAC 为主 + 角色上带 scope 数据范围属性**（向 ABAC 过渡的中间形态）。

看 `Role` 实体的 `scope` 字段：`ALL/ORG/WS/SELF`（全局/组织/工作空间/本人），它就是「数据范围」属性——决定这个角色能看哪些数据。`RoleMapper.selectRoleCodesByUserId()` 的注释也写明「具体数据范围在 ABAC 拦截器阶段再按 scope 收敛」。

> 面试亮点：**「我们采用 RBAC 做功能权限，角色上挂 scope 数据范围字段，为后续 ABAC 数据权限留了扩展点」**——这句话比单纯说「我们用了 RBAC」高级得多。

**「角色上带 scope」详解（功能权限 vs 数据权限）**

一个「权限」其实分两层，很多人混为一谈：

| 层        | 回答的问题       | 例子                |
| -------- | ----------- | ----------------- |
| **功能权限** | 你能不能「点这个按钮」 | 能不能进用户管理页         |
| **数据权限** | 你能「看到哪几行数据」 | 进了用户页，看所有人还是只看本部门 |

- **RBAC**（角色→权限表 `ie_role_permission`）管**功能权限**：`member:read`、`role:write` 这些「能不能做某操作」。
- **scope** 管**数据权限**：这个角色能「看到哪个范围的数据」。

scope 四个取值：

| scope 值 | 含义      | 谁用          |
| ------- | ------- | ----------- |
| `ALL`   | 全平台数据   | super_admin |
| `ORG`   | 本组织数据   | org_admin   |
| `WS`    | 本工作空间数据 | ws_admin    |
| `SELF`  | 只有自己的数据 | 普通成员        |

举例：同一个「用户列表」接口，三个人 scope 不同，看到的数据范围不同：

- 小明（super_admin，scope=ALL）→ 看到全公司 1000 个用户
- 小红（org_admin，scope=ORG）→ 只看到本部门 50 个用户
- 小刚（普通成员，scope=SELF）→ 只能看到自己 1 个

> **三个人用同一个接口、同一套功能权限（都有 `member:read`），但因为 scope 不同，看到的数据范围不同。** 这就是「角色上带 scope」：scope 是贴在角色上的「数据范围标签」，决定这个角色能看多宽的数据。

为什么说「RBAC 向 ABAC 过渡」？纯 RBAC 只管「能不能做」（功能），不管「看多少」（数据）；纯 ABAC 完全按属性动态算「能看哪些数据」；项目是 RBAC 管功能 + scope 字段管数据范围的结合。MVP 阶段 scope 字段先建好存着，真正按 scope 过滤数据的逻辑留给后续数据权限拦截器做。

#### 3. 权限进 JWT 的三个权衡（核心难点）

**为什么要把权限展开写进 JWT？**

因为 JWT 无状态，服务端不每次查库。如果每次请求都去数据库查「这个用户有哪些权限」，就失去了无状态的优势。所以**登录时一次性查好权限，写进 token**。

**权衡①：JWT 体积 vs 查库性能**

- 权限写进 JWT → token 变大，但每次请求零查库（快）
- 权限不写 JWT、每次查库 → token 小，但每次请求打数据库（慢）
- 项目选择前者：登录时 `selectPermissionCodesByUserId()` 查好 → 写进 `perms` claim

**权衡②：权限变更的实时性**

- 权限写进 JWT 后，管理员改了用户角色/权限，**已签发的 token 里还是旧权限**，要等 token 过期或重新登录才生效。
- 这是「无状态」的必然代价。若要求权限实时生效，就得引入「有状态」（每次查库/查缓存）。

**权衡③：超级管理员权限多，JWT 膨胀**

- 项目超管有 48 个权限，`perms` claim 会很长，token 变大（HTTP 头每次都要带）。
- 缓解方案：① JWT 只存角色编码，服务端缓存「角色→权限」映射；② 权限编码压缩。

**权衡③详解（先算账，再讲两个缓解方案）**

**为什么膨胀？算一笔账**：

```
48 个权限编码 × 平均 20 字符（编码+引号+逗号） ≈ 960 字符
+ "perms":[...] 外壳 ≈ 1000 字符
× base64url 编码膨胀 1/3 ≈ 1.3KB
→ 整条 token 约 1.5~2KB，且每次 HTTP 请求都要在 Authorization 头全量携带
```

> 关键认知：**权限越多 token 越大，而 token 是「每次请求都全量携带」的，膨胀代价被放大了无数次。**

**缓解方案①：JWT 只存角色编码，服务端缓存「角色→权限」映射**

核心思想：token 里不放 48 个权限，只放 1~2 个角色（如 `"super_admin"`），权限列表挪到服务端查缓存。能缩小的原因是「角色数量远小于权限数量」。

改造前（现状）：

```
登录：查DB得48权限 → 写进 JWT perms → 每次请求 JWT 自带权限
请求：解析 JWT → 直接拿权限 → 组 authorities（零查库）
```

改造后（方案①）：

```
登录：只把 roles=["super_admin"] 写进 JWT（不写 perms）
请求：解析 JWT → 拿角色 → 查「角色→权限」映射缓存 → 组 authorities
                                    ↑
                    Redis key: ie:auth:role:perms:super_admin
                    value: 48个权限编码，TTL 5分钟
```

代码改动点（两处）：

```java
// 登录签发：不再传 permissions
createAccessToken(userId, tenantId, wsId, roles, null);

// 校验：从缓存查权限（JwtAuthFilter.authenticate）
List<String> permissions = rolePermissionCache.getPermissions(payload.getRoles());
```

代价：**不再纯无状态**（每次请求多一次缓存查询），且带来**缓存一致性**问题——改角色权限后缓存是旧的。解决：改权限时主动删缓存，或短 TTL 容忍延迟。

**缓解方案②：权限编码压缩（位图 Bitmap）**

核心思想：48 个权限用 48 个二进制位（bit）表示，有权限=1、无=0，总共 6 字节，转成 16 进制字符串存进 JWT。

```
权限表加 bit_index 列：
  auth:login → 0，org:read → 1，... system:setting → 47
用户有 auth:login(0) + member:read(5) + role:write(20)
bitmap = 2^0 + 2^5 + 2^20 = 1048609
JWT 里存 "100000000000000100010"（21字符）或 16进制（12字符）
```

压缩效果：960 字符 → 12 字符，**压缩近 80 倍**。代价：权限要有固定序号、可读性差（看到数字不知有哪些权限）、权限超 64 个要扩展多字节。

**三种方案对比**：

| 方案             | token 大小 | 每次请求成本 | 实时性 | 复杂度 |
| -------------- | -------- | ------ | --- | --- |
| 现状：权限进 JWT     | 大（~2KB）  | 零查库    | 不实时 | 简单  |
| 方案①：角色进 JWT+缓存 | 小（~300B） | 一次缓存查询 | 可控制 | 中等  |
| 方案②：位图压缩       | 极小       | 零查库    | 不实时 | 中等  |

> 项目现状：MVP 阶段单租户、权限量不大，2KB 可接受，故用「权限进 JWT」换取零查库和简单性；权限规模上来后演进为方案①。**面试话术**：「MVP 用权限进 JWT 换零查库和简单；权限规模上来后，演进为 JWT 只存角色 + Redis 缓存角色→权限映射，兼顾体积和实时性。」

**为什么权限查询要 DISTINCT？**

`PermissionMapper` 的 SQL：

```sql
SELECT DISTINCT p.code
FROM ie_permission p
JOIN ie_role_permission rp ...
```

因为用户可能有多个角色，角色间权限重叠，不去重会重复写进 JWT。

#### 4. 权限链路完整走查

```
登录 → selectPermissionCodesByUserId(userId)  -- 查权限编码（DISTINCT 去重）
     → createAccessToken(..., permissions)     -- 写进 JWT 的 perms claim
     → JwtAuthFilter.parseAccessToken()        -- 解析出 perms
     → authenticate(): perms → SimpleGrantedAuthority  -- 转权限对象
     → @PreAuthorize("hasAuthority('member:read')")   -- 方法级校验
```

### 面试可能追问

- **Q1：RBAC 和 ABAC 的区别？你们用的哪种？**
  
  - 答：RBAC 按角色授权（粗粒度、管理简单），ABAC 按属性动态授权（细粒度、灵活复杂）。我们采用 **RBAC 为主**做功能权限，同时角色上挂 `scope`（ALL/ORG/WS/SELF）数据范围字段，为后续 ABAC 数据权限留了扩展点。

- **Q2：为什么把权限写进 JWT，而不是每次请求查数据库？**
  
  - 答：JWT 无状态的优势就是「服务端不查库」。若每次请求都查权限，就退化成了有状态。登录时一次性查好写进 token，后续请求零查库、高性能。

- **Q3：权限写进 JWT 有什么缺点？**
  
  - 答：两个缺点——① token 变大（超管 48 权限，HTTP 头每次携带）；② **权限变更不实时**（改了角色权限，已签发 token 还是旧权限，要重新登录才生效）。这是无状态设计的必然权衡。

- **Q4：超级管理员权限很多，JWT 膨胀怎么优化？**
  
  - 答：① JWT 只存角色编码，服务端缓存「角色→权限」映射，请求时查缓存；② 权限编码压缩；③ 超管用特殊标记（如 `*` 通配）而非逐条列权限。

- **Q5：为什么权限查询要用 DISTINCT？**
  
  - 答：用户可能拥有多个角色，角色间权限重叠，不去重会把重复权限写进 JWT，token 更大且无意义。

### 踩坑提醒

1. **坑：改了用户角色权限，但用户 token 里还是旧权限，导致权限不生效**
   
   - 现象：管理员给用户加了权限，用户访问却还是 403，以为代码 bug。
   - 规避：理解「权限进 JWT = 登录时快照」，变更要等 token 过期或重新登录。若要实时生效，需配套「删登录态/踢下线」强制重登。

2. **坑：权限查询忘了 DISTINCT，多角色用户 token 里权限重复**
   
   - 现象：JWT 里 `perms` 出现重复项，token 无谓变大。
   - 规避：SQL 用 `SELECT DISTINCT`，或查询后 `Set` 去重。

3. **坑：JWT 里既存角色又存权限，导致 token 过大**
   
   - 现象：`roles` + `perms` 两个 claim 都很长，HTTP 头膨胀。
   - 规避：MVP 阶段两者都存（roles 供数据范围、perms 供方法级权限）；若体积成问题，优先砍 `perms`（改成服务端缓存角色→权限映射），`roles` 保留做数据范围判断。

---

## 无状态 JWT 为什么关 CSRF + @PreAuthorize 原理

- 学于：2026-08-26
- 关联模块：`SecurityAutoConfiguration.securityFilterChain()`（csrf.disable）+ `UserController` 的 `@PreAuthorize` + `JwtAuthFilter.authenticate()`
- 来源：TD §7.3 / §7.4

> 目标：讲透两个面试高频点——① 为什么无状态 JWT 可以关闭 CSRF 防护；② `@PreAuthorize` 到底是怎么工作的（原理 + 完整调用链）。

### 直观类比（先建立直觉）

**CSRF = 黑客借你的「浏览器自动带凭证」干坏事**

你登录了银行网站（浏览器存了 Session Cookie），这时你**点开了一个恶意网页**。恶意网页偷偷发了一个请求到银行「转账给黑客」。因为浏览器**会自动带上银行的 Cookie**，银行以为是你本人操作 → 钱被转走。

**为什么 JWT 能免疫 CSRF？**

JWT 存在 **`Authorization` 请求头**里，**浏览器不会自动携带**（Cookie 才会自动携带）。恶意网页发的请求**带不上你的 JWT**，所以攻击失败。

> 一句话记忆：**CSRF 攻击靠「浏览器自动带 Cookie」；JWT 放请求头、浏览器不自动带，所以天然免疫，可以关 CSRF 防护。**

### 核心原理

#### 1. 前置知识：Cookie 与 Session 的底层（搞懂它才能懂 CSRF）

**Cookie 是什么？**

Cookie 是**浏览器里的一小块存储**，由服务器通过响应头 `Set-Cookie` 下发，浏览器自动保存，之后每次请求**自动带回**给服务器。

```
① 服务器响应：Set-Cookie: sessionId=abc123
        ↓
② 浏览器自动保存这个 cookie（键值对）
        ↓
③ 下次请求，浏览器自动在请求头带上：Cookie: sessionId=abc123
```

三个关键特性（CSRF 的祸根就藏在第 3 条）：

1. **自动携带**：浏览器发请求时**自动**带上 cookie，不需要前端代码干预
2. **按域名隔离**：只有同域名的请求才会带（bank.com 的 cookie 只发给 bank.com）
3. **同域名下跨站请求也带**：用户开着 bank.com 的登录态，别的网站（hacker.com）通过 `<img>`、`<form>`、`<script>` 等方式「借用」浏览器向 bank.com 发请求时，**cookie 照样自动带上**

**Session 是什么？**

Cookie 只存一个「凭证号」（sessionId），**真正的用户数据存在服务器端**，这就是 Session：

```
用户登录 bank.com
        ↓
服务器在内存/Redis 里存一份：「sessionId=abc123 → 用户=张三，已登录」
        ↓
服务器把 sessionId 通过 Set-Cookie 发给浏览器
        ↓
浏览器下次请求自动带 Cookie: sessionId=abc123
        ↓
服务器拿到 abc123 → 去内存/Redis 查 → 找到"张三，已登录" → 放行
```

> 一句话：**Cookie 是「浏览器存的凭证号」，Session 是「服务器存的用户数据」，靠 sessionId 串起来。这就是经典的「有状态认证」——服务器必须记住谁登录了。**

**Cookie/Session 认证的完整流程**：

```
① 登录：用户名密码 → 服务器验证 → 创建 Session（存服务器）→ 返回 sessionId（存 Cookie）
② 后续请求：浏览器自动带 Cookie(sessionId) → 服务器查 Session → 确认身份
③ 登出：服务器删 Session → sessionId 失效
```

**它的问题（为什么 JWT 要替代它）**：

- 服务器要**存** Session（占内存/Redis）
- 多实例部署时，Session 不共享（用户这次打到实例 A，下次打到实例 B，B 没有这个 Session → 掉登录），要额外做 Session 共享
- 这就是「有状态」的代价

#### 2. CSRF 的本质

跨站请求伪造（Cross-Site Request Forgery）：攻击者诱导用户浏览器，向**用户已登录**的网站发送**伪造请求**，利用「浏览器自动携带该站 Cookie」绕过身份验证。

防护的传统手段（CSRF Token）：服务端生成随机 token 存 Session，前端每次请求带上，服务端校验。**它依赖 Session**——而 JWT 项目没有 Session。

#### 3. 为什么无状态 JWT 可以关 CSRF

|          | Cookie + Session | JWT                    |
| -------- | ---------------- | ---------------------- |
| 凭证存哪     | Cookie（浏览器自动带）   | Authorization 头（前端手动带） |
| 浏览器自动携带？ | ✅ 会              | ❌ 不会                   |
| CSRF 风险  | 高（恶意站能借你 Cookie） | 无（恶意站带不上你的 JWT）        |

项目代码 `SecurityAutoConfiguration.java`：

```java
http.csrf(csrf -> csrf.disable())   // JWT 放请求头、不依赖 Cookie，天然免疫 CSRF
```

> 前提：JWT 必须放 `Authorization` 头，**不能放 Cookie**。一旦为了省事把 JWT 存 Cookie，CSRF 风险就回来了，那时不能关 CSRF 防护。

#### 4. @PreAuthorize 详解（注解是什么 / 怎么用 / 代码位置 / 作用 / 原理 / 典型场景）

**① @PreAuthorize 是什么？—— 一个「方法级权限校验」注解**

它是 Spring Security 提供的一个**注解（Annotation）**。注解本身不干活，它只是一个「标记」——真正干活的是 Spring Security 底层的一整套机制（AOP 拦截器），在方法执行前**读这个标记**、做权限校验。

- 全类名：`org.springframework.security.access.prepost.PreAuthorize`
- 作用对象：**方法**（贴在方法上）
- 激活前提：必须有 `@EnableMethodSecurity`（`SecurityAutoConfiguration.java`），否则注解是「死注解」，写了不生效

**② 注解怎么用？—— 括号里写一个 SpEL 表达式**

```java
@PreAuthorize("hasAuthority('member:read')")   // 括号里是 SpEL 表达式
public Result<PageResult<UserPageVO>> page(...) { ... }
```

`hasAuthority('member:read')` 是一段 **SpEL 表达式**（Spring 表达式语言），含义是：「当前用户的权限列表里，有 `member:read` 这个权限吗？」

常用的 SpEL 表达式（面试可能问）：

| 表达式                           | 含义                                  |
| ----------------------------- | ----------------------------------- |
| `hasAuthority('member:read')` | 有 `member:read` 权限                  |
| `hasRole('ADMIN')`            | 有 `ROLE_ADMIN` 角色（注意自动加 `ROLE_` 前缀） |
| `hasAnyAuthority('a','b')`    | 有 a 或 b 任一权限                        |
| `permitAll()`                 | 所有人可访问                              |
| `isAuthenticated()`           | 只要登录即可                              |

**③ 当前项目代码位置在哪？**

- 使用处：`UserController.java` 等（用户/角色管理接口都贴了）
  
  ```java
  @PreAuthorize("hasAuthority('member:read')")   // 用户分页需 member:read
  @PreAuthorize("hasAuthority('member:create')") // 创建用户需 member:create
  @PreAuthorize("hasAuthority('role:write')")    // 角色写操作需 role:write
  ```

- 激活处：`SecurityAutoConfiguration.java` 的 `@EnableMethodSecurity`

- 权限来源处：`JwtAuthFilter.authenticate()` 把 JWT 的 `perms` 转成 `SimpleGrantedAuthority`

**④ 它是什么作用？—— 替代「在每个方法里写 if 判断」**

没有 @PreAuthorize 时，你得在每个方法体里手动写：

```java
// 假设没有 @PreAuthorize，得这么写（又臭又长，每个方法都要写）
public Result<?> page(...) {
    if (!currentUserHasPermission("member:read")) {
        throw new BizException(FORBIDDEN);
    }
    // 业务逻辑...
}
```

有了 @PreAuthorize，**权限校验从业务逻辑里抽离出来**，一个注解搞定，业务代码保持干净。这就是 AOP「横切关注点」的价值。

**⑤ 原理是什么？—— AOP 方法拦截 + SpEL 求值**

`@PreAuthorize("hasAuthority('member:read')")` 是怎么拦住无权限请求的？**它不是写在方法体里的 if 判断，而是 Spring AOP 的方法级拦截器**，完整链路：

```
请求进入 Controller 的 page() 方法"门口"
        ↓
还没进方法体，就被 MethodSecurityInterceptor（AOP 代理）拦住
        ↓
拦截器读取方法上的 @PreAuthorize 注解
        ↓
解析 SpEL 表达式 hasAuthority('member:read') = "当前用户有 member:read 吗？"
        ↓
从 SecurityContext 取出当前用户的 Authentication（权限列表）
        ↓
逐个比对权限列表里有没有 member:read
        ↓
  ├─ 有 → 放行，进入方法体执行
  └─ 没有 → 抛 AccessDeniedException
              → 被 RestAccessDeniedHandler 捕获 → 返回 2006（无权限）
```

关键前提（两个，缺一不可）：

1. **SecurityContext 里必须先有权限**：这是 `JwtAuthFilter.authenticate()` 在请求进入时，把 JWT 的 `perms` 转成 `SimpleGrantedAuthority` 塞进去的：
   
   ```java
   List<SimpleGrantedAuthority> authorities = payload.getPermissions().stream()
           .map(SimpleGrantedAuthority::new).toList();
   UsernamePasswordAuthenticationToken authentication =
           new UsernamePasswordAuthenticationToken(userId, null, authorities);
   SecurityContextHolder.getContext().setAuthentication(authentication);
   ```

2. **顺序不能错**：先由过滤器塞权限，再由 @PreAuthorize 校验权限

**⑥ 典型使用场景（什么时候用）**

| 场景    | 例子                                                 |
| ----- | -------------------------------------------------- |
| 管理类接口 | 用户分页/创建/删除 → `hasAuthority('member:read')`         |
| 敏感操作  | 删除角色 → `hasAuthority('role:write')`                |
| 本人操作  | 改自己密码 → 不用 @PreAuthorize，只要求登录（从上下文取 userId）       |
| 白名单放行 | 登录/注册 → 在 SecurityFilterChain 里 `permitAll()`，不贴注解 |

### 面试可能追问

- **Q1：为什么 JWT 项目可以关闭 CSRF 防护？**
  
  - 答：CSRF 攻击依赖「浏览器自动携带 Cookie」来伪造请求。JWT 存放在 `Authorization` 请求头，由前端显式添加，浏览器不会自动携带，跨站请求带不上 JWT，攻击自然失效。所以无状态 JWT 项目关闭 CSRF 是安全的。**但前提是 JWT 不能存 Cookie**，存 Cookie 就要重新开启 CSRF 防护。

- **Q2：@PreAuthorize 的原理是什么？**
  
  - 答：基于 Spring AOP 的方法级拦截。`MethodSecurityInterceptor` 在方法执行前解析 `@PreAuthorize` 的 SpEL 表达式（如 `hasAuthority('member:read')`），从 `SecurityContext` 取出当前用户的权限列表比对，有权限放行、无权限抛 `AccessDeniedException`。需要 `@EnableMethodSecurity` 激活。

- **Q3：SecurityContext 里的权限是从哪来的？**
  
  - 答：请求进入时，`JwtAuthFilter` 解析 JWT，把 `perms` claim 里的权限编码转成 `SimpleGrantedAuthority`，封装成 `UsernamePasswordAuthenticationToken` 存进 `SecurityContextHolder`。`@PreAuthorize` 校验时从 Context 取权限比对。

- **Q4：CSRF Token 防护和 JWT 有什么关系？**
  
  - 答：CSRF Token 防护依赖 Session（服务端存 token、比对）。JWT 无状态、不用 Session，且凭证在请求头不自动携带，所以不需要 CSRF Token 防护。两者是「不同认证模型下的不同安全策略」。

- **Q5：Cookie 和 Session 的区别？**
  
  - 答：Cookie 是「浏览器端的小块存储」（存凭证号 sessionId），Session 是「服务器端存的用户数据」（存登录状态）。二者靠 sessionId 串起来：登录时服务器创建 Session 并把 sessionId 写进 Cookie 下发，后续浏览器自动带 Cookie，服务器拿 sessionId 查 Session 确认身份。一句话：Cookie 是钥匙，Session 是保险柜，钥匙存客户端、柜子在服务端。

- **Q6：Session 认证有什么缺点？为什么用 JWT 替代？**
  
  - 答：两大缺点——① 服务器要**存** Session（占内存/Redis），用户量大时存储压力大；② 多实例部署时 Session 不共享（请求打到实例 A 建的 Session，下次打到实例 B 没有 → 掉登录），要额外做 Session 共享（Redis 集中存）。JWT 无状态：身份信息自包含在 token 里，服务器不存、任意实例都能验，天然适合微服务横向扩展。

- **Q7：@PreAuthorize 是注解吗？它自己会干活吗？**
  
  - 答：是注解（Annotation），但它本身**不干活**，只是一个「标记」。真正干活的是 Spring Security 底层机制——`@EnableMethodSecurity` 激活后，AOP 的 `MethodSecurityInterceptor` 在方法执行前读这个注解、解析 SpEL 表达式、比对权限。注解是「声明意图」，拦截器是「执行者」。

### 踩坑提醒

1. **坑：为了省事把 JWT 存进 Cookie，却还关着 CSRF 防护**
   
   - 现象：Cookie 会被浏览器自动携带，恶意站能借你的 JWT 发请求，CSRF 风险回来了。
   - 规避：JWT 要么放 `Authorization` 头（可关 CSRF），要么存 Cookie（必须开 CSRF 防护），**二选一，不能混**。

2. **坑：忘了加 @EnableMethodSecurity，导致 @PreAuthorize 全部不生效**
   
   - 现象：所有接口任何人都能访问，权限形同虚设，还没报错。
   - 规避：@PreAuthorize 依赖 @EnableMethodSecurity 激活（项目在 SecurityAutoConfiguration 里已加），缺了它注解就是「死注解」。

3. **坑：@PreAuthorize 里权限编码写错（和 JWT 里对不上）**
   
   - 现象：明明用户有权限却一直 403。
   - 规避：@PreAuthorize 的 `hasAuthority('xxx')` 里的字符串必须和 `perms` claim 里的权限编码（`permission.code`）完全一致，注意大小写和冒号。

---

## JWT 密钥管理与 fail-fast 校验

- 学于：2026-08-27
- 关联模块：`SecurityProperties` / `JwtUtil` / `application.yml`
- 来源：TD ADR-10、PRD §12

> 目标：搞懂 HS256 对称签名的本质、密钥泄露的后果，以及「密钥为什么不能有代码默认值、必须 fail-fast 校验」。

### 直观类比（先建立直觉）

**JWT 签名密钥 = 印钞机的母版**

- 验签就像验钞：母版（密钥）只有一个，印出来的钞票（token）谁都能验，但**能印假钞的只有拿到母版的人**。
- HS256 是「对称」的——**印钞和验钞用同一块母版**。所以母版一旦泄露，攻击者不仅能验，还能自己印。

> 一句话记忆：**HS256 签发与校验用同一把密钥，密钥泄露 = 认证体系被整个攻破，比密码泄露更严重。**

### 核心原理

#### 1. HS256 为什么是「对称签名」

- 签发：`base64url(header).base64url(payload).HMACSHA256(secret)` —— 用 secret 算出签名。
- 校验：服务端用**同一个 secret** 重新算一遍签名，比对是否一致，一致才相信 payload 没被篡改。
- 关键：谁有 secret，谁就能对任意 `userId/roles/perms` 算出一个「合法」签名，等于**完全绕过认证与授权**。而且攻击者无需登录、无异常流量，可长期潜伏。

#### 2. 密钥硬编码的危险

- 硬编码默认值（`SecurityProperties.jwtSecret` 代码里写死、`application.yml` 里同值）意味着：**只要代码/仓库泄露，密钥就泄露**。
- 更糟的是「可预测」：写死一个固定的默认值，攻击者甚至不需要泄露，直接猜/在 GitHub 搜同名默认值就能伪造 token。
- 带默认值上线的隐蔽风险：开发者「忘记配密钥」不会被发现，系统**默默用默认值跑起来**，等于默认裸奔。

#### 3. fail-fast 启动校验（把危险暴露在启动期）

把「忘记配密钥」从「默默用默认值跑起来（危险）」变成「启动就报错（安全）」：

```java
String secret = properties.getJwtSecret();
if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
    throw new IllegalStateException("JWT 密钥未配置或长度不足 32 字节，拒绝启动");
}
```

- HS256 官方建议密钥至少 256 bit（32 字节），低于这个长度有被暴力枚举的风险。
- 校验放在 `JwtUtil` 构造时（Bean 初始化阶段），启动即校验，不留给运行时。

#### 4. 密钥的配置分层（本地 vs 生产）

Spring Boot 配置三层优先级：**环境变量 > application.yml > 代码默认值**。

- **本地开发**：yml 里留开发密钥，或用占位符 `${JWT_SECRET:dev-local-secret-2026-08}`（没设环境变量就用冒号后的默认值）。
- **生产**：环境变量注入随机密钥 `export JWT_SECRET="$(openssl rand -base64 48)"`，**绝不写进 yml**（会进 git 泄露）。
- `@ConfigurationProperties(prefix="insight.security")` 的 relaxed binding 会自动把环境变量 `INSIGHT_SECURITY_JWT_SECRET` 映射到 `jwtSecret` 字段。

### 面试可能追问

- **Q1：JWT 密钥该怎么管？**
  
  - 答：三原则——① 不进代码、不进 git（环境变量/配置中心注入）；② 启动时 fail-fast 校验长度（HS256 ≥32 字节）；③ 生产用随机密钥，开发与生产分离。

- **Q2：HS256 和 RS256 的区别？**
  
  - 答：HS256 是对称签名（同一个 secret 签发和校验），适合单体/内部服务，密钥必须保密；RS256 是非对称（私钥签发、公钥校验），适合第三方鉴权场景，公钥可公开分发。本项目用 HS256 是因为 UMS 自己签、自己验，没有跨系统验签需求。

- **Q3：为什么密钥泄露比密码泄露更严重？**
  
  - 答：密码泄露 → 攻击者只能登录「某个用户」的账号，且可被发现、可改密止损；密钥泄露 → 攻击者能伪造「任意用户、任意权限」的 token，等于绕过整个认证体系，且无登录痕迹、难发现。

### 踩坑提醒

1. **坑：密钥带默认值上线**
   
   - 现象：开发者忘记配密钥，系统默默用写死的默认值跑起来，攻击者用同名默认值即可伪造任意身份 token。
   - 规避：代码不赋默认值 + 启动 fail-fast 校验长度，缺密钥直接拒绝启动。

2. **坑：密钥写进 application.yml 提交到 git**
   
   - 现象：yml 进仓库，密钥随代码历史永久泄露，删掉也追不回。
   - 规避：生产密钥只走环境变量/配置中心；yml 里最多放开发占位符，绝不放真实密钥。

### 实战复盘：为什么会出现（根因）/ 本次怎么修的（2026-09-02）

**为什么会出现**（Review 在 UMS 阶段发现）：

- 骨架阶段为了「本地能直接跑通」，在图省事的心态下把密钥写死成了**两处同值**：
  1. `SecurityProperties.jwtSecret` 字段初始化（代码内默认值）；
  2. `application.yml` 明文配置同一字符串。
- 结果三重叠加：仓库泄露即密钥泄露；默认值含 `change-me` 字样，攻击者可按命名规律直接猜/全网搜；且**没有任何启动校验**——就算忘配密钥，系统也"默默用默认值跑起来"，上线即裸奔。
- 反思：密钥是「用进废退」型配置，默认值看似方便，实则把「上线前必须决策」推迟成「上线后默默裸奔」。

**本次怎么修的**（3 个文件，闭环三件事：删默认、启动拦、注入化）：

| 改动文件                             | 改了什么                                                                               | 目的                      |
| -------------------------------- | ---------------------------------------------------------------------------------- | ----------------------- |
| `SecurityProperties.java`        | 删除 `jwtSecret` 代码内默认值（字段不再初始化）                                                     | 代码里无秘密可泄                |
| `SecurityAutoConfiguration.java` | `jwtUtil` Bean 创建前 fail-fast：空 → 拒启；<32 字节 → 拒启；`prod` profile 且含 `change-me` → 拒启 | 把「忘配/带占位上线」从运行时风险变启动期报错 |
| `application.yml`                | 密钥改为 `${INSIGHT_SECURITY_JWT_SECRET:本地开发默认值}`                                      | 生产用环境变量注入独立随机密钥即覆盖      |

- 分层设计要点：**本地开发**默认 profile 不触发 `prod` 校验，可直接跑；**生产**切 `prod` profile 后，只要还在用开发占位密钥（`change-me`）就直接拒绝启动，逼你注入真密钥——「默认能跑」与「裸奔不可」两者兼得。
- 校验放在 `jwtUtil` Bean 的工厂方法而非 `JwtUtil` 构造器，好处：`@ConditionalOnMissingBean(JwtUtil.class)` 时只拦自动装配路径、业务自定义 JwtUtil 不受限；且能拿到 `Environment` 判断 active profile。

---

## 微服务身份传递的信任边界（双身份源问题）

- 学于：2026-08-27
- 关联模块：`UserContextFilter` / `JwtAuthFilter` / `UserContext` / `WebAutoConfiguration`
- 来源：TD ADR-5
- 2026-09-03 增强：补「架构现实」+ 无网关/有网关**断点级调用栈** + 剥头与 HMAC 签名

> 目标：搞懂「一个系统里为什么不能有两套身份来源」、请求头身份为什么不可信、以及身份传递的正确边界。

### 第 0 步：先看清架构现实（读链路图前必看）

**查证结果（2026-09-03）——理解本问题的前提：**

| 事实 | 证据 |
| --- | --- |
| **网关不存在** | `insight-engine-modules/insight-engine-gateway/` 目录下**只有一个 `pom.xml`，零 Java 代码** |
| **UMS 直连暴露** | `insight-engine-ums/src/main/resources/application.yml:7` → `server.port: 7101`，浏览器直连 `localhost:7101/doc.html` 即可访问 |

```
设计文档里画的「将来」（TD ADR-5）：
  浏览器 → 网关(:8080，验 JWT 后加身份头) → UMS(:7101)

现在的真实情况：
  浏览器/Postman ──────────直连──────────> UMS(:7101)
                （中间没有任何东西）
```

**一句话矛盾**：代码是按「将来有网关」写的，但网关现在不存在，所以本该由网关填的身份头，实际上是**客户端自己填的**。

> **由此得出一条重要区分（面试常考）**：
> - **网关没落地** = 功能没做完（排期问题）
> - **服务无条件信任客户端可控的输入** = 已经写出来的代码缺陷（安全问题）
>
> 这是**两个独立问题**。你可以「保安还没招到」的同时「先把登记册锁进抽屉」——后者不依赖前者，必须现在就修。

### 直观类比（先建立直觉）

**`UserContext` = 一个「我是谁」的盒子（ThreadLocal）**

- 业务代码需要知道「当前用户是谁」时，就从盒子里取（`UserContext.getUserId()`），不关心是谁放进去的。
- 危险在于：**如果盒子里装的身份，是一个「任何人都能自己写」的来源放进去的，那这个盒子就不安全了。**

> 一句话记忆：**身份只能有一个可信来源（服务端签名），绝不能信客户端可任意填写的明文。**

### 核心原理

#### 1. 两套身份来源并存（隐患根源）

系统有两个过滤器都在往 `UserContext` 盒子里塞身份：

| 过滤器                 | 身份从哪来                     | 客户端能伪造吗   | 执行顺序               |
| ------------------- | ------------------------- | --------- | ------------------ |
| `UserContextFilter` | 请求头 `X-User-Id`/`X-Roles` | **能！随便填** | 先（order=HIGHEST+1） |
| `JwtAuthFilter`     | JWT（服务端签名）                | 不能（没密钥）   | 后                  |

- 正常情况下，`JwtAuthFilter` 后执行，会用「可信的 JWT 身份」覆盖 `UserContextFilter` 塞进去的「明文身份」，所以业务代码读到的通常是对的。
- 但**安全依赖「后执行的过滤器一定覆盖先执行的」这个脆弱前提**。

#### 2. 危险场景：白名单接口

`/auth/login`、`/auth/register`、`/auth/refresh` 是白名单（permitAll，**不经过 JwtAuthFilter 校验**），但 `UserContextFilter` 是 `/*` 全路径**照样执行**：

```
① 攻击者请求 POST /auth/register（白名单，不需要 token）
        同时带伪造头：X-User-Id: 1，X-Roles: super_admin
        ↓
② UserContextFilter 无条件解析 → 把「我是 1 号超级管理员」塞进盒子
        ↓
③ 此时【没有 JwtAuthFilter 来覆盖纠正】——白名单不校验 token
        ↓
④ 盒子里残留伪造身份 → 业务代码读 UserContext 就中招
```

- `register` 接口目前没读 `UserContext`，所以还没被实际利用，但这是「随时会爆的越权面」——将来任何白名单接口/内部接口读 `UserContext.getUserId()` 立刻中招。

#### 3. 完整调用栈（断点级，无网关现状）

请求样例：`GET /auth/me`，带真 JWT，同时被伪造 `X-User-Id: 1`

**路径简写约定**（下文 `文件:行号` 均相对 `insight-engine/`）：
- `starter-web` = `insight-engine-starter/insight-engine-starter-web/src/main/java/com/insightengine/starter/web/`
- `starter-security` = `insight-engine-starter/insight-engine-starter-security/src/main/java/com/insightengine/starter/security/`
- `ums` = `insight-engine-modules/insight-engine-ums/src/main/java/com/insightengine/ums/`

```
Tomcat 从线程池取出线程 T1 处理本请求
│
├─ [1] TraceFilter                    order = -2147483648 (HIGHEST_PRECEDENCE)
│      starter-web/filter/TraceFilter.java:47
│      读 X-Trace-Id → MDC.put("traceId")
│      ⚠️ 这里也读客户端头，但只用于日志、不当身份 → 安全
│      ↓ filterChain.doFilter()
│
├─ [2] UserContextFilter              order = -2147483647 (HIGHEST_PRECEDENCE+1)
│      starter-web/filter/UserContextFilter.java
│      ◆ 修复前（漏洞现场）：
│        :54  getHeader("X-User-Id")      ← 读到伪造的 1
│        :60  UserContext.set( 1号用户 )   ← 脏数据进盒子
│      ◆ 修复后（漏洞面闭合）：
│        整个 Bean 未注册 → 本层不存在，直接跳到 [3]
│        原因：WebAutoConfiguration.java:62 的条件注解未满足
│      ↓
│
├─ [3] FilterChainProxy（Spring Security 总入口）   order = -100
│      │  内部是 Spring Security 自己的一串 filter：
│      │
│      ├─ ...内置 filter（本项目 STATELESS，基本空转）
│      │
│      ├─ [3.1] JwtAuthFilter        ★ 身份真正建立的地方
│      │     starter-security/filter/JwtAuthFilter.java
│      │     :75   读 Authorization 头；没有 → 直接 return，不建身份
│      │     :84   查 Redis 黑名单（已登出的 token 在此被拒）
│      │     :89   jwtUtil.parseAccessToken(token)
│      │             └→ starter-security/util/JwtUtil.java:176-181
│      │                verifyWith(secretKey)  ★验签：没密钥伪造不了★
│      │     :92   查 Redis 登录态 ie:auth:token:{userId}
│      │     :123  SecurityContextHolder.setAuthentication(...)
│      │     :126  UserContext.set( JWT里的真实用户 )  ← 覆盖脏数据
│      │
│      ├─ UsernamePasswordAuthenticationFilter（表单登录，本项目用不上）
│      │
│      └─ [3.2] AuthorizationFilter   ★ 授权判定在最后
│            starter-security/config/SecurityAutoConfiguration.java:130-133
│            "/auth/me" 不在白名单 → 要求 authenticated → 通过
│            ↓
│
├─ [4] DispatcherServlet → Controller
│      ums/controller/AuthController.java:91
│      UserContext.getUserId()  ← 拿到真实用户
│      ↓
│
└─ [5] 响应返回，栈逆序退出（finally 生效）
       JwtAuthFilter.java:107   finally → UserContext.clear()  ★修复后新增
       TraceFilter.java:62      finally → MDC.remove()
```

**为什么 `/auth/me` 看不出问题**：`JwtAuthFilter` 后执行，会用 JWT 真值覆盖掉脏数据。**真正的暴露点是白名单接口**：

```
POST /auth/register（permitAll，不需要任何 token）+ 伪造头 X-User-Id: 1

修复前：
  TraceFilter
  → UserContextFilter.java:54   读到 X-User-Id: 1
  → UserContext.java:27         set( 1号超管 )       ★ 无凭证，服务端信了
  → Security 链：/auth/register 在白名单 → AuthorizationFilter 直接放行
  → Controller 执行期间 UserContext.getUserId() == 1    ★★ 暴露点
  → UserContextFilter.java:45   finally → clear

修复后：
  TraceFilter
  → （UserContextFilter 未注册，整层跳过）
  → Security 链：白名单 → 放行
  → Controller 执行期间 UserContext.getUserId() == null   ← 干净
  → 无人 clear（因为无人 set，正确）
```

**目前还没被真正利用的原因**：读 `UserContext` 的三处消费者都在 `anyRequest().authenticated()` 保护下——
- `ums/controller/AuthController.java:91`（`/auth/me`）
- `ums/controller/UserController.java:94`（改密码）
- `starter-mybatis/config/MybatisMetaObjectHandler.java:62`（自动填充 `create_by`/`update_by`）

而 `register` 虽无保护，但它不读 `UserContext`。**所以不是"安全"，是"恰好没撞上"**——任何一个新的白名单/内部接口只要读一次 `UserContext.getUserId()`，当场越权。

#### 4. 修复方案（二选一）

- **方案 A（UMS 走 JWT，默认关闭网关头）**：给 `UserContextFilter` 加条件装配开关，默认关闭：
  
  ```java
  @ConditionalOnProperty(name = "insight.web.trust-gateway-headers", havingValue = "true")
  public FilterRegistrationBean<UserContextFilter> userContextFilterRegistration() { ... }
  ```
  
  UMS 不配开关 → `UserContextFilter` 不生效 → 盒子里只有 `JwtAuthFilter` 塞的「可信身份」。

- **方案 B（走 TD ADR-5 明文头方案）**：给网关下发的头加 HMAC 签名 `X-User-Sign` 验签，或 IP 网段校验兜底。

#### 5. 有网关后的完整链路（下一章要写的）

```
浏览器
  │  Authorization: Bearer <JWT>       ← 用户真凭证
  │  X-User-Id: 1                      ← 攻击者照样能伪造！
  ↓
┌──────── 网关 insight-engine-gateway :8080（下一章要建） ────────┐
│                                                                │
│  过滤器1：剥头（Strip）    ★★★★ 最易漏、也最致命 ★★★★           │
│     删掉客户端传来的所有身份头：                                  │
│       X-User-Id / X-Tenant-Id / X-Workspace-Id / X-Roles        │
│     漏了这步后面全白做 —— 伪造头会原样穿透到下游                  │
│                    ↓                                           │
│  过滤器2：验 JWT（网关持有同一个 secret）                         │
│     失败 → 直接 401，根本不转发                                  │
│                    ↓                                           │
│  过滤器3：下发明文头（用 JWT 里的真值重写）                        │
│     X-User-Id: 3 / X-Tenant-Id: 1 / X-Roles: admin              │
│     （方案 B 进阶）X-User-Sign: HMAC(上面几个头 + 时间戳)          │
│                    ↓                                           │
│  过滤器4：路由转发 → http://ums:7101                             │
└────────────────────────────────────────────────────────────────┘
  ↓
┌──────── UMS 服务 :7101 ─────────────────────────────────────────┐
│  TraceFilter        复用 X-Trace-Id                              │
│  UserContextFilter  ← 此时开关打开                                │
│     insight.web.trust-gateway-headers=true                       │
│     读 X-User-Id → UserContext.set                               │
│     ★ 此刻凭什么信这个头？两个前提：                               │
│       前提1（必需）：网关已剥掉客户端传来的同名头                    │
│       前提2（三选一）：                                            │
│         ① 网络隔离：7101 不对公网暴露，只有网关可达                 │
│         ② 验签：校验 X-User-Sign 的 HMAC                         │
│         ③ 网段：校验来源 IP 属于网关网段                           │
│  Spring Security 链 → JwtAuthFilter → Controller                │
└────────────────────────────────────────────────────────────────┘
```

> **一句话点破**：网关方案里，服务信头的安全性**不是靠"这头是网关写的"这句话保证的**，而是靠「**客户端到不了服务** + **头被剥掉/被签名**」两件事保证。从 UMS 眼里看，**网关写的头和客户端写的头长得一模一样**，它无法区分。

##### 剥头 / 签名——用 HTTP 报文说话

"头"就是 HTTP 请求里的几行纯文本，没有神秘的东西：

```
GET /auth/me HTTP/1.1
Host: localhost:7101
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
X-User-Id: 1                    ← 就是这一行字
X-Roles: super_admin            ← 和这一行字
```

- **"伪造头"** = 攻击者用 curl 自己敲上 `X-User-Id: 1` 这行字
- **"剥头"** = 把这行字**删掉**
- **"签名"** = 额外再加一行校验码，让别人改不动前面几行

**剥头**（网关侧，示意代码，非本项目现有代码）：

```java
ServerHttpRequest mutated = request.mutate()
    .headers(h -> {
        h.remove("X-User-Id");                 // 剥：先删掉客户端传来的
        h.remove("X-Tenant-Id");
        h.remove("X-Roles");
        h.set("X-User-Id", realUserIdFromJwt); // 再用真值重写
    })
    .build();
```

为什么「先删再写」而不是直接覆盖：万一网关 JWT 校验有 bug（异常时仍放行），直接覆盖可能没覆盖成；「先删」保证**最坏情况下头是空的**，下游读到 `null` 顶多"没身份"，不会是"假身份"——**失败时倾向安全（fail-safe）**。

**签名**（HMAC）——网关转发时多两行：

```
X-User-Sign: 9f3a2bc7d1e...       ← 网关盖的"公章"
X-User-Ts:   1756872000           ← 盖章时间
```

```java
// 网关侧（盖章）
String data = userId + "|" + tenantId + "|" + roles + "|" + ts;
String sign = HmacSHA256(sharedSecret, data);   // 密钥只有网关和服务知道

// 服务侧 UserContextFilter（验章）
String expect = HmacSHA256(sharedSecret, 收到的内容按同样规则拼接);
if (!expect.equals(收到的 X-User-Sign)) throw ...  // 对不上 = 被篡改
if (now - 收到的ts > 60_000) throw ...             // 防"录播重放"
```

为什么攻击者没辙：

| 攻击者想干什么 | 结果 |
| --- | --- |
| 把 `X-User-Id` 改成 `1` | 服务重算的校验码 ≠ 原 `X-User-Sign` → **拒绝** |
| 自己造一个 `X-User-Sign` | 没有 `sharedSecret`，算的对不上 → **拒绝** |
| 把整个请求录下来以后重发 | 时间戳超 60 秒 → **拒绝**（防重放） |

**两者对照**：

| | 剥头 | 签名 |
| --- | --- | --- |
| 大白话 | 撕掉假单子 | 盖上公章 |
| 防的是 | 假头从客户端**穿透**网关 | 有人**绕过**网关直连服务造假头 |
| 不做的后果 | 攻击者带 `X-User-Id: 1` 一路穿到服务 | 只要能访问到服务端口，就能随便填身份 |
| 成本 | 网关几行代码，零运维 | 要管理共享密钥、处理时钟偏差 |
| **单独够吗** | **不够**（能直连就白搭） | **够**（数学上保证） |

| 配置组合 | 相当于 | 效果 |
| --- | --- | --- |
| 只剥头，服务端口仍暴露 | 门口贴"请走前门"，**后门没锁** | 等于没做 |
| 剥头 + 网络隔离 | 前门有人查，**后门锁死** | 够用，但依赖部署不出错 |
| 剥头 + HMAC 签名 | 后门锁死，**进来还得对暗号** | 最稳，端口暴露也不怕 |

### 面试可能追问

- **Q1：为什么不能让业务服务信任请求头里的 X-User-Id？**
  
  - 答：请求头是客户端完全可控的，信任它等于把「我是谁」的决定权交给攻击者，构成水平/垂直越权。身份必须来自服务端签名的 JWT（或加 HMAC 签名/IP 校验的网关头）。

- **Q2：两个过滤器都写 UserContext，为什么这是隐患？**
  
  - 答：因为安全依赖「后执行的过滤器一定覆盖先执行的」这个脆弱前提。一旦某个接口不经过后执行的过滤器（如白名单接口），先执行过滤器塞的「不可信身份」就会暴露。

- **Q3：网关还没落地，是"功能没做完"，为什么现在就要修？**
  
  - 答：这是两个独立问题。网关没落地是**排期问题**；而"服务无条件信任客户端可控的请求头"是**已经写出来的代码缺陷**。后者不依赖前者——即便网关永远不建，把明文头过滤器默认关闭也是对的。
  - **加分句**：「功能没做完」和「存在安全缺陷」是两件事，不能互相开脱。

- **Q4：只是关掉一个 Filter，为什么还要改另一个文件？**
  
  - 答：因为**清理职责没跟着走**。`UserContext` 是 ThreadLocal，原分工是 `JwtAuthFilter` 只 set、`UserContextFilter` 的 finally 负责 clear。关掉后者 =「贴便签的人还在、撕便签的人没了」→ Tomcat 线程池复用线程时，下一个请求读到上一个用户的身份（串号）。
  - **铁律**：谁写谁清，且 `clear()` 必须在 `finally`（98-103 行 token 过期/非法会抛异常，写在 try 末尾会被跳过）。

- **Q5：将来"网关解析 JWT 后下发明文身份头"，怎么保证安全？**
  
  - 答：两个动作必须做全——① **剥头**：网关先删掉客户端传来的同名身份头，再用 JWT 真值重写（先删再写，保证失败时头是空的而非假的）；② **验证来源**（三选一）：网络隔离（服务端口只有网关可达）/ HMAC 签名头 `X-User-Sign` / 来源 IP 网段校验。
  - **易漏点**：只剥头但服务端口仍对公网暴露 = 门上贴"请走前门"而后门没锁，等于没做。

### 踩坑提醒

1. **坑：白名单接口是越权重灾区**
   - 现象：permitAll 接口不校验 token，但全路径过滤器照常解析请求头，伪造头直接生效。
   - 规避：身份只能有一个可信来源；走 JWT 就关闭明文头过滤器（条件装配开关），走网关头就加签名/IP 校验。

2. **坑：只关 Filter 不补清理 → 线程池随机串号**（本次修复真正的隐藏项）
   - 现象：`UserContextFilter` 默认关闭后，`UserContext`（ThreadLocal）没人清理，下一个复用同一线程的请求读到上一个用户的身份。**比越权更隐蔽——它不看攻击者脸色，随机发作**。
   - 规避：任何"关掉/替换过滤器"的改动，都要追问一句**它原来顺手干的活谁接**；固定过一遍「谁写、谁清、谁读」。

3. **坑：开关被误开 / 配置漂移 → 漏洞原地复活**
   - 现象：某天有人为了解决"`UserContext` 是 null"随手在配置中心加了 `insight.web.trust-gateway-headers=true`，越权面悄悄回来且无人记得。
   - 规避：① 把这个开关纳入生产配置审计清单（正常环境它不该出现）；② `havingValue="true"` 是**精确字符串匹配**，写 `TRUE`、`1`、带空格都**不生效**，调试时别被误导去改代码；③ 只有在"网关已剥头 + 已上验签/网段校验"的前提下才能开。

4. **坑：以为关了 Filter 就"身份统一了"，其实还有两套上下文**
   - 现象：`JwtAuthFilter` 同时写 `SecurityContextHolder`（:123）和 `UserContext`（:126），有的代码读前者、有的读后者；且 `@Async`/自定义线程池里**两个都读不到**。
   - 规避：业务层统一从 `UserContext` 取身份；异步任务需要身份就显式传参，不要用 `InheritableThreadLocal` 去"救"（线程池复用场景它无效）。

### 本次落地复盘：为什么选方案 A（2026-09-02，UMS-1）

两个方案不是"谁对谁错"，而是**把「身份可信锚点」放在哪**：

| 方案 | 可信锚点 | 前置条件 | 改动量 | 与现状契合度 |
| --- | --- | --- | --- | --- |
| **A（已采用）**：默认关明文头，服务自验 JWT | 每个服务持有的 secret + JWT 签名 | 各服务需持 secret | 1 个装配开关 + 清理职责移交 | ✅ UMS 现状就是自验 JWT |
| B：信任网关 HMAC 签名头 / IP 网段 | 网关（唯一出口） | gateway 先落地 + 共享 secret/可信网段 | HMAC 校验 + 网段配置，较大 | ⚠️ gateway 未落地，无从验证 |

**具体改动**：
- `WebAutoConfiguration.userContextFilterRegistration` 加 `@ConditionalOnProperty(name = "insight.web.trust-gateway-headers", havingValue = "true")`——默认不注册 `UserContextFilter`；
- `JwtAuthFilter` 在 finally 中 `UserContext.clear()`——原来 `UserContext` 的清理靠 `UserContextFilter` 兜底，现在它默认不在，写入方必须自己负责清理（否则线程池串号）；
- 白名单接口（login/register/refresh）从此**不再有任何过滤器会把伪造明文头塞进 `UserContext`**，越权面闭合。

**留的"后门"**：将来 gateway 落地、真要切 ADR-5 明文头方案时，把开关置 true 即可——方案 A 不是把 B 的路堵死，而是把"默认安全"和"按需演进"都留好了。

### 断点自查清单（自己跑一遍，比读代码印象深十倍）

在 IDE 里给这些位置打断点，启动 UMS，用 Postman 打一次**带伪造头 `X-User-Id: 1`** 的请求：

| # | 断点位置 | 修复后预期 |
| --- | --- | --- |
| 1 | `starter-web/config/WebAutoConfiguration.java:63` | **启动时不进** ← "Filter 没注册"的直接证据 |
| 2 | `starter-web/filter/UserContextFilter.java:54` | **永远不进** ← "漏洞面闭合"的直接证据 |
| 3 | `starter-web/filter/TraceFilter.java:47` | 会进，读 `X-Trace-Id`（只写日志，不当身份 → 安全） |
| 4 | `starter-security/filter/JwtAuthFilter.java:75` | 无 `Authorization` 头 → 直接 return，不建身份 |
| 5 | `starter-security/filter/JwtAuthFilter.java:89` | 有 token 时进来 → 走 `JwtUtil.java:176` 验签 |
| 6 | `starter-security/filter/JwtAuthFilter.java:126` | 用 JWT 里的身份写 `UserContext` |
| 7 | `starter-security/filter/JwtAuthFilter.java:107` | **每次请求必进**，finally 清理 |
| 8 | `starter-security/config/SecurityAutoConfiguration.java:130` | 白名单判定处 |
| 9 | `ums/controller/AuthController.java:91` | `UserContext.getUserId()` 取值处 |

想看修复前对比：`git stash` → 重启 → 再打一次，会看到**断点 2 被命中、`UserContext` 里坐着 `1`**；看完 `git stash pop` 恢复。

### 状态总览：三种形态对照

| | 修复前（现在） | 修复后（现在） | 将来有网关 |
| --- | --- | --- | --- |
| 谁能访问 UMS 7101 | 任何人直连 | 任何人直连 | **只有网关可达** |
| 身份来源 | 请求头（可伪造）+ JWT **两个** | **只有 JWT 一个** | 请求头（网关写的 + 有保护） |
| `UserContextFilter` | 注册，无条件信头 | **不注册** | 注册，且配套剥头/验签/网段 |
| `UserContext` 谁写 | 两个 Filter 都写 | 只有 `JwtAuthFilter` | `UserContextFilter` |
| `UserContext` 谁清 | `UserContextFilter` | **`JwtAuthFilter` finally** | `UserContextFilter` |

### 下一章（写网关）的前置决策

写 `insight-engine-gateway` 时要先定：**用纯网络隔离，还是 HMAC 签名头？**
- **网络隔离**：零代码，但依赖部署（UMS 不映射宿主端口，只有 gateway 映射 8080）；
- **HMAC 签名**：要引入共享 secret + 处理时钟偏差，但摆脱对部署的依赖。

**这决定了 `insight.web.trust-gateway-headers=true` 那天能不能真的打开。**

---

## Refresh Token 安全：为什么必须轮换 + 一次性 jti 机制

- 学于：2026-09-02
- 关联模块：`JwtUtil`（`createRefreshToken`/`parseRefreshToken`）/ `AuthServiceImpl.refresh`/`logout` / `UserServiceImpl.updateStatus`/`updatePassword` / `AuthConstants.KEY_AUTH_REFRESH`
- 来源：TD ADR-10、UMS review（UMS-2）

> 目标：搞懂「为什么 access 撤销做得再好、refresh 不处理就白搭」「什么是 refresh 一次性轮换」「jti 重放为什么是泄露信号」「吊销粒度怎么选」。

### 直观类比（先建立直觉）

**access token = 短期门禁卡，refresh token = 办卡授权书**

- 门禁卡（access）2 小时过期、每次进门刷，丢了损失可控——这是我们在前面章节用黑名单/登录态管好的"卡"。
- 但**授权书（refresh）7 天有效、凭它能无限补办新门禁卡**。如果授权书被偷了，攻击者每天补一张新卡，你之前把旧卡作废得再彻底也没用——**他根本不进门，只补卡**。

> 一句话记忆：**access 撤销管的是"卡"，refresh 撤销管的是"发卡权"。不处理 refresh，登出/改密后攻击者仍能靠泄露的 refresh 无限续期，会话注销形同虚设。**

### 核心原理

#### 1. 为什么 refresh 是薄弱面（修复前的问题）

修复前 `AuthServiceImpl.refresh()` 只做三件事：验签名 → 验类型 → 验过期。只要 refresh token 没过期（7 天内）、签名对，就**无条件签发新 token 对**：

```
攻击者拿到用户泄露的 refresh token（登出/改密前签发的）
        ↓
7 天内随时调 /auth/refresh → 服务端验签通过 → 发新 access + 新 refresh
        ↓
登出（只黑名单了旧 access）？改密（只删了 access 登录态）？→ 都拦不住这个 refresh
        ↓
攻击者永远有"最新一套 token"，会话注销 = 失效
```

**本质**：access 侧的撤销机制（黑名单/登录态）都在"校验 access 时"生效，而 refresh 换新完全不经过这些校验——**两条撤销链在 refresh 这里是断开的**。

#### 2. 一次性轮换（rotation）：让 refresh "用一次就作废"

核心思路：**每次 refresh 成功，旧 refresh token 立即作废，只发一个全新的 refresh token**。这样：

- 正常用户：手头永远只有"最新那一个 refresh"，用完旧的换新的，链条不断；
- 攻击者：如果偷的是**旧** refresh，用它去刷新时服务端一查——"这不是当前有效的那个"，**拒绝 + 报警**。

**为什么能识破旧 token？—— 靠 `jti`（JWT ID）做会话指纹**：

- 签发 refresh 时生成唯一 `jti`，并把它（的摘要）存到 Redis：`ie:auth:refresh:{userId} = sha256(jti)`，TTL=7d；
- refresh 请求进来：解析出 token 里的 `jti` → 和 Redis 里存的当前 jti 比对：
  - **一致** → 是当前有效 refresh → 轮换：签发新对（新 jti 覆盖旧的存进 Redis）；
  - **Redis 里没有 / 不一致** → 这不是当前有效的 refresh → 重放信号。

#### 3. 同 jti 重放 → 吊销全部会话（泄露处置）

**如果发现"旧 jti 再次出现"，说明这个 jti 已经被轮换掉了——它的再次出现只有一种解释：token 泄露了**（正常用户不会用已经换掉的旧 refresh）。此时不是简单拒绝，而是**按泄露处置**：

```java
if (activeDigest == null || !activeDigest.equals(sha256Hex(jti))) {
    // 吊销该用户全部会话：删 access 登录态 + refresh 会话 → 全部 token 即刻失效
    stringRedisTemplate.delete(KEY_AUTH_TOKEN + userId);
    stringRedisTemplate.delete(KEY_AUTH_REFRESH + userId);
    throw new BizException(ErrorCode.UNAUTHORIZED, "刷新令牌已失效或检测到重放，已注销全部会话");
}
```

> 逻辑：**"旧 token 被重用" = 有人拿到了本不该再有的 token = 假设泄露 = 宁可错杀、全部重登。**

#### 4. 吊销粒度：谁该被连带作废？

| 操作 | 要作废什么 | 粒度 | 实现 |
| --- | --- | --- | --- |
| `logout()` | 当前登录这次会话 | 单会话（当前 userId 下最新一轮） | access 加黑名单 + 删 `KEY_AUTH_TOKEN` + 删 `KEY_AUTH_REFRESH` |
| 改密 `updatePassword()` | 该用户全部会话 | 用户级 | 删 `KEY_AUTH_TOKEN` + `KEY_AUTH_REFRESH` |
| 禁用 `updateStatus()` | 该用户全部会话 | 用户级 | 删 `KEY_AUTH_TOKEN` + `KEY_AUTH_REFRESH` |

- 三个操作现在都**连 refresh 会话 key 一起删**——改密/禁用/登出后，该用户手头所有 refresh token（含已泄露的）去刷新时，Redis 查无此 key → 直接拒绝，**7 天续期通道关闭**。
- 兜底注意：**不能**用 access 登录态 key 存在性做 refresh 前置校验——access 登录态 TTL=2h（随 access 过期），而 refresh 有效 7 天；用户超过 2 小时没活动后正常刷新时 access 登录态 key 已自然过期，误用它当"吊销信号"会把正常刷新也拦掉。吊销信号应当是 **refresh 会话 key 本身**（它是 7 天 TTL，只有被主动删才缺失）。

### 我在项目里怎么用的

**改动 1：JwtUtil** —— refresh token 携带 jti，并暴露 refresh TTL：

```java
public String createRefreshToken(Long userId, String jti) {
    return Jwts.builder()
            .id(jti)                                   // jti 写入标准 claim
            .subject(String.valueOf(userId))
            .claim(CLAIM_TYPE, TYPE_REFRESH)
            .expiration(...)                           // refreshTtlMillis（7d）
            .signWith(secretKey).compact();
}
// 解析返回 JwtRefreshPayload(userId, jti)
```

**改动 2：AuthServiceImpl** —— 登录/刷新成功签发时，生成 jti 并登记会话：

```java
String refreshJti = UUID.randomUUID().toString().replace("-", "");
String refreshToken = jwtUtil.createRefreshToken(user.getId(), refreshJti);
cacheRefreshToken(user.getId(), refreshJti);  // SET ie:auth:refresh:{userId}=sha256(jti), TTL=7d
```

**改动 3：refresh()** —— 校验 + 轮换 + 重放处置（核心逻辑见上文第 3 节）。

### 面试可能追问

- **Q1：access 和 refresh 的撤销粒度为什么不同？**
  
  - 答：access 短命、按次校验，用黑名单按 token 或登录态按用户都行；refresh 是"发卡权"，必须按用户级吊销（删一个 userId 的 refresh 会话 key，该用户全部 refresh 立即失效），不能只按单条 token 记——攻击者可能持有任意一条旧 refresh。
  
- **Q2：为什么 refresh 要"一次性轮换"而不是复用同一个 refresh？**
  
  - 答：复用同一个 refresh = 泄露后无法区分"合法持有者刷新"和"攻击者刷新"，且一个 token 多处使用本身就是泄露信号。轮换后"旧 token 再出现"就有了明确的语义——重放，可据此吊销全部会话。

- **Q3：怎么识别 refresh token 被重放？**
  
  - 答：服务端按 userId 记录"当前有效 jti"。refresh 时比对 token 里的 jti 与 Redis 当前 jti：一致说明是当前有效；不一致（或查无）说明是已被轮换/吊销的旧 token——正常用户不会用旧 token，出现即视为泄露。

### 踩坑提醒

1. **坑：只修 access 撤销，不处理 refresh——登出/改密形同虚设**
   - 现象：改密/禁用后 access 被拦了，但攻击者拿旧 refresh 调 `/auth/refresh` 又拿到全新 token 对。
   - 规避：登出/改密/禁用必须**连 refresh 会话 key 一起删**，堵死续期通道。
2. **坑：refresh 前置校验误用 access 登录态 key**
   - 现象：access 登录态 TTL=2h（随 access 过期），用户 3 小时没活动后正常刷新被误判"已登出"。
   - 规避：吊销信号用 refresh 会话 key（TTL=7d，只被主动删）；access 登录态 key 只用于 access 校验。
3. **坑：存 refresh 会话明文 jti / token**
   - 现象：Redis 存了完整 refresh token 或可逆标识，拖库即泄露可用凭据。
   - 规避：只存 jti 的 SHA-256 摘要（`TokenDigestUtil.sha256Hex`），比对用摘要、拖库拿不到明文。

---

## MDC 日志上下文：是什么、用来干什么、TraceFilter 怎么配合

- 学于：2026-09-02
- 关联模块：`starter-web` 的 `TraceFilter` / `GlobalExceptionHandler` + logback pattern
- 来源：TD §4.4

> 目标：先讲清楚「MDC 是什么、解决什么问题」，再讲清楚「TraceFilter 这个类是干嘛的」，最后串成一条线。

### 先看问题：日志很多，怎么知道哪几条属于同一次请求？

一个 HTTP 请求进来，代码一路会打很多条日志（进 Controller → 查用户 → 校验密码 → 查权限 → 写缓存……可能几十条）。同时几百个用户在线，日志文件里就是几万条日志，**全都乱序混在一起**。

当用户反馈「我登录失败了」，你要在几万条日志里，找出**属于这一个用户、这一次请求**的那几十条——怎么办？

答案：**给每个请求贴一个唯一编号（traceId），让这次请求打的所有日志都自动带上这个编号。** 查问题时按编号一过滤，这次请求的日志立刻全部浮出来。

```
没有 traceId（无法串联，分不清谁是谁）：
INFO  用户登录
INFO  查询用户成功
WARN  密码校验失败      ← 是哪个用户？哪个请求？不知道

有 traceId（一眼串联）：
[abc123] INFO  用户登录
[abc123] INFO  查询用户成功
[abc123] WARN  密码校验失败   ← 都是 abc123 这一个请求
[def456] INFO  用户登录        ← 另一个请求，编号不同，互不干扰
```

### MDC 本身是什么

一句话定义：**MDC 就是一个「每个线程私有的小盒子」，你可以往里面放 `key=value`，日志框架打印日志时会自动把盒子里的值一起打出来。**

拆开讲：

- **MDC** = Mapped Diagnostic Context（映射诊断上下文），是 SLF4J 提供的一个工具类。
- 它内部就是一个 `Map<String, String>`（键值对盒子），常用方法：

| 方法              | 作用                     |
| --------------- | ---------------------- |
| `MDC.put(k, v)` | 往当前线程的盒子里放一个 key=value |
| `MDC.get(k)`    | 从当前线程的盒子里取出来           |
| `MDC.remove(k)` | 删掉盒子里的某个 key           |
| `MDC.clear()`   | 清空整个盒子                 |

- 关键特性：**这个盒子是「每个线程各有一份」的**（底层就是 `ThreadLocal<Map>`），A 线程放的东西 B 线程看不到。
- 它和日志的关系：logback 的日志格式里写 `%X{traceId}`，意思就是「打印日志时，去当前线程的盒子里取出 `traceId` 这个 key 的值，填到日志里」。

所以它**解决的核心问题就一句话**：**你不需要在每一行 `log.info(...)` 里手动传 traceId，日志也能自动带上它。**

### TraceFilter 是做什么的（重点）

`TraceFilter` 是**每个 HTTP 请求的「入口守门员」**——它在业务代码执行**之前**先把编号贴好，在业务代码执行**之后**再清理。看代码：

```java
protected void doFilterInternal(request, response, filterChain) {
    // ① 生成/复用 traceId：上游(网关)传了合法的就用，没传或非法就新生成
    String traceId = request.getHeader(Constants.HEADER_TRACE_ID);
    if (traceId == null || !TRACE_ID_PATTERN.matcher(traceId).matches()) {
        traceId = IdUtil.fastSimpleUUID();
    }

    // ② 把 traceId 放进"当前线程的盒子"（MDC）→ 之后业务代码打日志自动带上
    MDC.put("traceId", traceId);

    try {
        // ③ 放行，让后续过滤器 / Controller / Service 继续执行
        filterChain.doFilter(request, response);
    } finally {
        // ④ 请求结束，把盒子清空，防止线程复用串号
        MDC.remove("traceId");
    }
}
```

**为什么要有它？** 因为「往 MDC 里放 traceId」这个动作，必须发生在业务代码打**第一行日志之前**。而 Filter 是请求进来最早执行的环节，所以由它当入口，提前把编号贴好。之后 Controller/Service 里随便 `log.info(...)`，日志都会自动带上 traceId，**业务代码完全无感知**。

**为什么继承 `OncePerRequestFilter`？** 保证一个请求只执行一次。即使请求发生 forward/include（一次请求被多次转发），过滤器也不会重复执行、重复生成 traceId。

**为什么 `finally` 里要 `MDC.remove`？** Tomcat 的线程池会复用线程。如果不清理，这个线程下次被别的请求复用，盒子里还残留上一个请求的 traceId，日志就串号了。放 `finally` 保证无论成功还是抛异常都清理。

### 三个参与者分工（一条线串起来）

```
TraceFilter（写入者）         logback pattern（读取者）        业务代码（无感知）
「我负责贴编号」               「%X{traceId} 我负责取编号」      「我只管 log.info」
MDC.put("traceId", x)    →    [%X{traceId}] 打印 x       →   log.info("登录")
```

- `TraceFilter` 写：`MDC.put("traceId", traceId)`
- logback 读：pattern 里 `[%X{traceId}]`
- 业务读（可选）：`GlobalExceptionHandler` 里 `MDC.get("traceId")`，把 traceId 回填到错误响应体，前端拿到报错时能按这个编号来反馈，我们就能在日志里定位。

```java
private String currentTraceId() {
    String traceId = MDC.get("traceId");
    return StrUtil.isBlank(traceId) ? null : traceId;
}
```

### 项目里的真实代码位置

| 代码                            | 位置                                        | 做的事            |
| ----------------------------- | ----------------------------------------- | -------------- |
| `MDC.put("traceId", traceId)` | `TraceFilter.doFilterInternal`            | 请求进来时写入        |
| `MDC.remove("traceId")`       | `TraceFilter` 的 `finally`                 | 请求结束时清理        |
| `MDC.get("traceId")`          | `GlobalExceptionHandler.currentTraceId()` | 错误响应回填 traceId |
| `[%X{traceId}]`               | logback pattern（TD §4.4 约定）               | 日志打印时自动取值      |

> 注意：项目目前**还没有实际的 `logback-spring.xml` 文件**，TD.md 只给了约定 pattern。MDC 的存取逻辑已经就绪，logback 配置文件是后续要补的落地项。

### 面试可能追问

- **Q1：MDC 的底层是什么？**
  
  - 答：`ThreadLocal<Map<String, String>>`。每个线程一份独立 Map，实现线程隔离。

- **Q2：为什么日志能「自动」带 traceId，业务代码不用手动传？**
  
  - 答：logback pattern 里的 `%X{traceId}` 会在每次输出日志时，从当前线程的 MDC 里取 traceId 值填入。写入方（Filter `MDC.put`）和读取方（logback `%X{}`）通过同一个 key 解耦。

- **Q3：`MDC.remove` / `MDC.clear` 为什么必须放 `finally`？**
  
  - 答：Tomcat 线程池会复用线程。若不清理，上一个请求的 traceId 残留，下一个请求的日志会串号。放 `finally` 保证成功失败都清理。

- **Q4：`@Async` 或 `CompletableFuture` 的子线程里，能拿到主线程的 MDC 吗？**
  
  - 答：不能。MDC 是线程隔离的，子线程有自己的 ThreadLocal。异步场景要么手动把 traceId 传给子线程，要么用 `MDC.getCopyOfContextMap()` 拷贝上下文，或用支持上下文传递的线程池（如 `TaskDecorator`）。

### 踩坑提醒

1. **坑：忘 `MDC.remove`，线程复用串号**
   
   - 现象：A 请求日志里混入 B 请求的 traceId，排查链路全乱。
   - 规避：`finally` 里清理，或直接 `MDC.clear()`。

2. **坑：异步/线程池场景丢 traceId**
   
   - 现象：主线程打日志有 traceId，子线程打日志没有（打印成空或 `-`）。
   - 规避：异步任务里显式传递 traceId（入参传入，或用 `TaskDecorator`/`MDC.getCopyOfContextMap()` 拷贝上下文）。

3. **坑：MDC 塞超长/带换行的值（安全）**
   
   - 现象：若直接 `MDC.put` 用户输入，攻击者可用换行、ANSI 注入伪造日志（日志注入）。
   - 规避：写入前做白名单校验（本项目 `TraceFilter` 的正则 `[A-Za-z0-9-]{1,64}`）。

---

## ThreadLocal 线程隔离与 remove 防串号（UserContext / MDC 的共同底层）

- 学于：2026-09-02
- 关联模块：`starter-web` 的 `UserContext`（用户上下文持有器）+ `UserContextFilter`（写/清）+ `TraceFilter`（MDC）；同族：`JwtAuthFilter` 用的 `SecurityContextHolder`、日志框架的 MDC 底层都是 ThreadLocal
- 来源：实际开发中「UserContext.clear() 为什么必须在 finally 调」「MDC.remove() 为什么防串号」的共同根因

> 目标：把上一节 MDC 笔记里反复出现的「线程隔离」「线程复用串号」「finally 清理」讲到底——它们全都指向同一个底层机制 **ThreadLocal**。这一节讲透三件事：① 它凭什么做到线程隔离；② 为什么容器线程池下必须 `remove()`；③ 不 remove 会发生什么（串号演示）。

### 直观类比（先建立直觉）

把 ThreadLocal 想成**一个公共储物柜，但每个线程配一把专属钥匙**，或者更贴切一点——**公司给每位员工发了一个「专属工位抽屉」**：

1. 每个线程就像一位员工，有自己的**专属工位**（线程私有存储），别人看不见也碰不着。
2. `ThreadLocal` 对象本身只是一个「抽屉编号牌」（一个普通的堆上对象），它**自己不存数据**。
3. 当你执行 `threadLocal.set(value)`，实际是：**去「当前线程」的工位抽屉，贴上编号牌，把 value 放进去**。
4. 当你执行 `threadLocal.get()`，实际是：**去「当前线程」的工位抽屉，按编号牌把 value 取出来**。
5. 关键：两个线程各自有各自的工位，`线程A.set` 的东西 `线程B.get` 永远拿不到——这就是**线程隔离**。

> 一句话记忆：**ThreadLocal 不是「一个变量一份值」，而是「一个变量在每个线程里各有一份值，线程之间井水不犯河水」。**

### 核心原理（它底层到底怎么做到的）

底层是三层结构，缺一不可：

```
每个 Thread 对象内部持有一个 ThreadLocalMap（一个以 ThreadLocal 为 key 的 Map）
        ↓
ThreadLocalMap 的 key   = ThreadLocal 实例本身（弱引用）
ThreadLocalMap 的 value = 你 set 进去的值
```

**所以 `threadLocal.set(v)` 的真实语义是：**

```java
Thread current = Thread.currentThread();        // ① 先拿到「当前线程」
ThreadLocalMap map = current.threadLocals;      // ② 取当前线程专属的 map（没有就建）
map.set(this, v);                               // ③ 以 this(ThreadLocal) 为 key 放值
```

**`threadLocal.get()` 的真实语义是：**

```java
ThreadLocalMap map = Thread.currentThread().threadLocals;  // 找当前线程的 map
return map == null ? null : (T) map.get(this);             // 以 this 为 key 取值
```

由此得出几个关键结论：

1. **数据存在「线程」身上，不在 ThreadLocal 对象身上**。ThreadLocal 对象只是公共的 key（编号牌）。
2. **静态的 ThreadLocal 实例被所有线程共享**，但**共享的是「钥匙」不是「抽屉」**——每个线程用自己的 map 去存，天然互不干扰，**无需加锁**（这是它最大的价值：既线程安全又零锁开销）。
3. **`remove()` 的作用**：把「当前线程 map 里，以该 ThreadLocal 为 key 的那一项」整个删掉。删掉后该线程再 `get()` 返回 null，干净如新。

### 为什么必须 remove()？—— 容器线程池复用的「串号」问题

这是 Web 项目最容易踩、也最核心的点。**Tomcat 不会为每个请求新建线程，而是维护一个线程池**：

```
请求 A 到达 → 线程池挑空闲线程 T 处理 → 请求结束 → 线程 T 归还池中（不销毁！）
请求 B 到达 → 线程池可能又把线程 T 分配出去处理
```

**如果 UserContext 在请求结束时不清理，就会发生：**

```
① 请求 A（用户1）→ 线程 T 执行 → UserContext.set(用户1)
② 请求 A 结束，线程 T 回池——但 T 的 ThreadLocal 里还留着「用户1」
③ 请求 B（用户2 或匿名）到达 → 线程池把 T 分配给它
④ 请求 B 的 UserContextFilter 若没解析到用户头 → 不 set → 不覆盖
⑤ 请求 B 的业务代码 UserContext.get() → 读到「用户1」→ 数据越权/串号！
```

这就是**串号**——用户 2 的请求拿到了用户 1 的身份，做数据权限过滤时会把用户 1 的数据查出来，造成**水平越权**。

**为什么必须放 `finally`？** 因为请求处理过程中可能抛异常。如果清理写在 try 的正常路径末尾，异常一抛就跳过了清理，线程 T 带着脏上下文回池，下次照样串号。`finally` 保证**无论成功还是异常都必须清理**：

```java
try {
    // set + 执行业务
} finally {
    UserContext.clear();   // 铁律：必须 finally，不能只写在 try 末尾
}
```

> 一句话记忆：**ThreadLocal 只保证「不同线程之间不串」；线程池复用让「同一个线程在不同时间处理不同请求」，串号就发生在这——所以必须用 remove() 斩断「上个请求留在这个线程上的残渣」。**

### 我在项目里怎么用的（三处 ThreadLocal 全家桶）

本项目其实有**三处** ThreadLocal 的典型应用，恰好覆盖了「业务上下文 / 日志上下文 / 安全上下文」三类：

| 场景               | 类                                 | ThreadLocal 载体                           | 谁负责 set                                  | 谁负责 remove                                                   |
| ---------------- | --------------------------------- | ---------------------------------------- | ---------------------------------------- | ------------------------------------------------------------ |
| ① 业务上下文（当前用户）    | `UserContext`                     | `ThreadLocal<LoginUser> HOLDER`          | `JwtAuthFilter`（默认）/ `UserContextFilter`（走网关头方案） | 写入方自己 `finally` 清：默认 `JwtAuthFilter`，走网关头方案则 `UserContextFilter` |
| ② 日志上下文（traceId） | `TraceFilter` + logback           | SLF4J `MDC`（内部即 ThreadLocal）             | `TraceFilter` `MDC.put("traceId", ...)`  | `TraceFilter` 的 `finally`（`MDC.remove("traceId")`）           |
| ③ 安全上下文（认证身份）    | `JwtAuthFilter` + Spring Security | `SecurityContextHolder`（内部即 ThreadLocal） | `JwtAuthFilter` `setAuthentication(...)` | Spring Security 框架自身在请求链结束时清理                                |

**① `UserContext`（业务上下文）的标准写法：**

```java
public final class UserContext {
    // 线程本地持有器，隔离每个请求的用户身份
    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    public static void set(LoginUser loginUser)   { HOLDER.set(loginUser); }
    public static LoginUser get()                 { return HOLDER.get(); }
    public static Long getUserId()                { LoginUser u = HOLDER.get(); return u == null ? null : u.getUserId(); }
    // ...
    public static void clear()                    { HOLDER.remove(); }   // ← 本质就是 ThreadLocal.remove()
}
```

**② 写入/清理方（谁负责写 + 谁负责清）：**

```java
@Override
protected void doFilterInternal(...) {
    try {
        LoginUser loginUser = parse(request);          // 从网关头/JWT 解析身份
        if (loginUser != null) {
            UserContext.set(loginUser);               // ① 写进当前线程的 ThreadLocal
        }
        filterChain.doFilter(request, response);       // ② 业务代码随处可读
    } finally {
        // 与 TraceFilter 同理，请求结束必须清理，防止线程复用时上下文串号
        UserContext.clear();                          // ③ finally 里 remove（铁律）
    }
}
```

> **注意（2026-09-02 起）**：这套「写入方在 finally 自清」的模板正是 `JwtAuthFilter` 的写法（它默认是 `UserContext` 的唯一写入方，负责 finally 清理）；`UserContextFilter`（解析明文头那个）默认已不装配，仅「走网关明文头方案」的服务开启——但无论谁写，**铁律不变：写入方自己 finally 清**。

**③ `TraceFilter`（MDC 同源同理）：**

```java
MDC.put("traceId", traceId);   // MDC 底层就是一个 ThreadLocal<Map>
try {
    filterChain.doFilter(request, response);
} finally {
    MDC.remove("traceId");     // 同一个道理：防 traceId 串号
}
```

> 串起来理解：`UserContext` 存的「当前用户」、MDC 存的「traceId」、`SecurityContextHolder` 存的「Authentication」——本质都是**「每个请求一份、随线程走的临时上下文」**。它们共享同一套规则：**请求级数据放 ThreadLocal，请求结束必须 remove**。

### 面试可能追问

- **Q1：ThreadLocal 的实现原理？为什么能做到线程隔离且不加锁？**
  
  - 答：每个 `Thread` 内部持有一个 `ThreadLocalMap`（以 ThreadLocal 实例为 key 的 Map）。`set(v)` 的实际动作是往「当前线程」自己的 map 里写；`get()` 是从当前线程的 map 里读。因为每个线程访问的是自己私有的 map，天然互不干扰，所以无需加锁——数据存在线程身上，而不是共享的 ThreadLocal 对象上。

- **Q2：Web 容器里为什么必须 remove？不 remove 会怎样？**
  
  - 答：容器（Tomcat）用线程池复用线程，一个请求结束线程不销毁、带着 ThreadLocal 里的旧值回池。下一个请求复用该线程时，若新请求没有重新 set（如匿名请求、解析不到身份头），`get()` 就会读到上一个请求残留的值——用户串号、日志 traceId 串号，甚至构成越权。所以必须在请求结束（`finally` 里）remove，把线程恢复成干净状态。

- **Q3：`set(null)` 和 `remove()` 有区别吗？**
  
  - 答：有。`remove()` 是把当前线程 map 里该项**整个 entry 删掉**（key 和 value 一起移除）；`set(null)` 只把 value 置空，entry（key 的弱引用 + 空的 value 槽）仍残留在 map 里，清理语义不彻底。规范做法是 `remove()`。

- **Q4：ThreadLocal 会内存泄漏吗？什么场景下？**
  
  - 答：会，经典面试题。ThreadLocalMap 的 key 是**弱引用**——当外部不再持有 ThreadLocal 实例时，key 会被 GC 回收，但 **value 是强引用**，若线程还活着（线程池线程长期存活），value 永远无法被回收，形成「key 没了、value 悬空」的泄漏。规避：用完 `remove()`（本项目请求结束统一清理即为此）。
  - 补充：即使不调用 remove，get/set 时 ThreadLocalMap 也会做部分清理（探测式清除），但这只保证「下次操作时顺带清」；规范上仍应显式 remove。

- **Q5：`@Async` / 线程池子线程里能拿到主线程的 ThreadLocal 吗？**
  
  - 答：默认拿不到。ThreadLocal 是线程私有的，子线程有自己的 map。需要传递时：① `InheritableThreadLocal`（子线程创建时拷贝一份，仅限「创建时」，线程池复用场景无效）；② 线程池用 `TransmittableThreadLocal`（TTL）解决任务提交时的值传递；③ 或者干脆显式把 userId/traceId 作为参数传给子线程。本项目异步任务尚少，但 `@Async` 里若读 `UserContext.get()` 会拿到 null——这是已知边界，需显式传参。

### 踩坑提醒

1. **坑：请求结束忘记 remove → 线程复用串号（本项目最核心）**
   
   - 现象：偶发性「用户 A 的请求查出了用户 B 的数据」「日志 traceId 对不上」「权限偶尔判断错」，重启后消失、高并发下变频繁。
   - 规避：请求级 Filter 统一管生命周期——`try { set } finally { clear()/remove() }`，业务代码只读不写不删；绝不能只在某条成功路径末尾清理。

2. **坑：清理代码写在 try 正常路径末尾，没用 finally**
   
   - 现象：正常请求不串号，一旦业务抛异常，清理被跳过，线程带脏上下文回池，后续请求开始串号。
   - 规避：remove 一律放 `finally`，保证成功、异常都执行。

3. **坑：把用户上下文直接当静态变量存（普通 static 字段）**
   
   - 现象：没用 ThreadLocal、直接在类里写 `public static LoginUser currentUser`——所有请求共享一个字段，高并发下疯狂串号。
   - 规避：必须用 `ThreadLocal` 包裹（如 `UserContext.HOLDER`），并配套请求级清理。

4. **坑：子线程/异步任务里读不到主线程的上下文**
   
   - 现象：`@Async`/`ExecutorService` 里 `UserContext.get()`、`MDC.get("traceId")` 返回 null（日志丢 traceId、审计缺用户）。
   - 规避：显式传参，或用 `InheritableThreadLocal`（创建时拷贝）/ TTL（线程池传递）——本项目当前采用「显式传参」优先。

5. **坑（进阶）：用了 ThreadLocal 但从不 remove（线程池场景内存泄漏）**
   
   - 现象：长运行应用内存缓慢增长，heap dump 发现大量 ThreadLocalMap value 悬空（key 弱引用已被回收，value 还被线程强引用）。
   - 规避：线程池/长生命周期线程里用 ThreadLocal 必须配对 remove；配合「请求级 finally 清理」从根上消除。

---

（持续沉淀中，学一个补一个）
