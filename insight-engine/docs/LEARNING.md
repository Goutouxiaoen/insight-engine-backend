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
- [x] Spring Boot 自动装配机制（AutoConfiguration.imports）（2026-08-26）
- [x] Spring Security + JWT 认证：无状态 vs 黑名单/登录态（登出/改密/禁用三种失效）+ 关 CSRF 原因 + @PreAuthorize 原理（2026-08-26）
- [x] RBAC vs ABAC + 权限进 JWT 的权衡（2026-08-26）
- [x] JWT 密钥管理与 fail-fast 校验（2026-08-27）
- [x] 微服务身份传递的信任边界（2026-08-27）
- [x] Nacos 服务注册与配置中心（2026-08-26，配置中心待学）
- [x] Redis 登录失败锁定 + Token 黑名单 + 登录态缓存（2026-08-26）

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
- [ ] Redis 分布式锁 / 缓存穿透·击穿·雪崩
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

| 模块 | packaging | 作用 |
|------|-----------|------|
| `insight-engine`（父 POM） | pom | 聚合全部子模块；统一 Java 17 / UTF-8；import 四个 BOM；统一编译插件配置 |
| `insight-engine-dependencies`（BOM） | pom | 统一内部模块 + 三方补充依赖（MyBatis-Plus/JJWT/Knife4j/Hutool…）的版本号 |
| `insight-engine-starter` | pom | **聚合 8 个 starter**（本身无代码，只是父目录） |
| `insight-engine-modules` | pom | **聚合 gateway + 11 个业务服务**（本身无代码，只是父目录） |

> 关键理解：`insight-engine-starter` 和 `insight-engine-modules` 这两个目录里**只有 pom.xml、没有 src/**，它们不是"服务"，是"文件夹式的分组"。IDEA 里看到它们嵌套子模块是正常现象。

#### 第二层：公共基础模块（被所有人依赖）

| 模块 | 关键类 | 作用 |
|------|--------|------|
| `insight-engine-common` | `Result` / `ErrorCode` / `BizException` / `PageQuery` / `PageResult` / `Constants` | 纯 POJO，**零框架依赖**。统一响应体、错误码（IF 附录 A）、业务异常、分页封装、全局常量 |

#### 第三层：契约模块

| 模块 | 作用 |
|------|------|
| `insight-engine-api` | Feign 接口 + 共享 DTO。**服务间只能通过这里调用**，禁止直接引对方内部类（阶段 3 起逐步填充各业务域的契约） |

#### 第四层：8 个技术能力 starter（即插即用）

| starter | 提供的能力 | 当前状态 |
|---------|-----------|----------|
| `starter-web` | **统一响应/异常/TraceID/用户上下文**（TraceFilter、UserContextFilter、GlobalExceptionHandler、自动装配） | ✅ 阶段 1 已实现 |
| `starter-mybatis` | MyBatis-Plus、逻辑删除、审计填充、数据权限拦截器 | 骨架（阶段 5 实现） |
| `starter-security` | Spring Security + JWT、方法级权限 | 骨架（阶段 3 实现） |
| `starter-redis` | RedisTemplate、分布式锁、缓存防护 | 骨架 |
| `starter-nacos` | 服务注册 + 配置中心 | 骨架 |
| `starter-ai` | Spring AI + LangChain4j 装配 | 骨架（阶段 6 实现） |
| `starter-mq` | RabbitMQ 信封/确认/重试死信 | 骨架（阶段 7 实现） |
| `starter-observability` | Micrometer + Prometheus | 骨架（阶段 11 实现） |

> 关键理解：starter 是"**能力包**"不是"服务"。业务服务（ums/kb…）要什么能力就 `依赖` 哪个 starter，能力自动生效。例如 ums 需要 Web + 异常处理 + 安全，就引 `starter-web` + `starter-security`。

#### 第五层：业务服务（modules，真正可运行的微服务）

| 服务 | 端口 | 一句话职责 | 对应开发阶段 |
|------|------|-----------|-------------|
| `gateway` | 7000 | 统一入口：路由、鉴权、限流、跨域 | 阶段 4 |
| `ums` | 7101 | 用户、角色、权限、登录 JWT（RBAC） | 阶段 3 ★第一个微服务 |
| `workspace` | 7102 | 组织、工作空间、成员 | 阶段 5 |
| `model` | 7103 | 模型网关：厂商/模型/路由/流式/计量 | 阶段 6 ★核心 |
| `kb` | 7104 | 知识库：文档解析、切片、向量检索 | 阶段 7 |
| `agent` | 7105 | Agent：ReAct、工具调用、版本 | 阶段 9 ★核心 |
| `tool` | 7106 | 工具市场：内置/HTTP 工具 | 阶段 8 |
| `conv` | 7107 | 对话：会话、消息、流式输出 | 阶段 10 |
| `billing` | 7108 | 计费：配额、用量、账单 | 阶段 11 |
| `obs` | 7109 | 监控审计：指标、调用链、审计日志 | 阶段 11 |
| `notify` | 7110 | 通知：渠道、模板、投递 | 阶段 11 |

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

| 对象 | 对应文件 | 能写什么 |
|------|---------|---------|
| 员工（类/接口） | `Xxx.java` | 类的字段、方法、逻辑 |
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

| 层次 | 回答的问题 | 技术 | 依赖 Docker 吗 |
|------|-----------|------|---------------|
| 发现层 | 我要调的服务 IP 和端口是多少？ | Nacos | ❌ 无关 |
| 传输层 | 拿到地址后数据怎么送过去？ | 网络 | ✅ 有关 |

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

| 方式 | 做法 | 问题 |
|------|------|------|
| 黑名单 | 列出"不允许的字符"，逐个过滤 | 控制字符成千上万，**容易漏**，而且难枚举全 |
| **白名单** | 只允许"明确安全的字符集"，其余全拒 | **从根上杜绝**，安全、简单、可预测 |

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

**② `UserContextFilter`（信任网关明文身份头）**

现状：无条件信任网关下发的明文身份头（`X-User-Id`/`X-Tenant-Id`/`X-Roles`），直接组装 `LoginUser`。这是 TD ADR-5 的既定权衡（网关唯一入口、内网可信），但若业务服务被绕过网关直连，即可伪造身份越权（完整拆解见《微服务身份传递的信任边界》）。

越权面的防护手段（三选一）：

| 方案 | 做法 | 代价 |
|------|------|------|
| 网络层端口隔离 | 业务服务端口（7101~7110）不对外暴露，只放网关访问 | 运维成本 |
| 代码层 HMAC 签名 | 网关下发 `X-User-Sign` 签名头，业务服务共享密钥验签 | 加一层签名逻辑 |
| IP 网段校验 | 业务服务只信任来自网关 IP 的请求 | 最便宜，网关 IP 变了要维护 |

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

## Git 提交与拉取的标准动作（减少代码冲突）

- 学于：2026-08-26
- 关联模块：整个项目的版本管理
- 来源：DEVGUIDE §9.2

> 目标：把"提交、拉取代码的标准动作"讲清楚，核心就一句——**先拉后推、小步提交、及时合主线**，能极大减少代码冲突。

### 直观类比（桌上文件 + 便签 + 文件柜）

三个对象对应关系（这是搞懂 Git 的地基）：

| 对象 | 类比 | 说明 |
|------|------|------|
| 本地分支 `dev-xuy` | 你桌上正在写的文件 | 你真正干活的地方 |
| 远程分支 `dev-xuy` | 公司服务器上的文件柜 | 团队共享的真相 |
| 快照 `origin/dev-xuy` | 桌上贴的便签 | 记着"我印象中服务器那份长这样" |

三个关键结论（解决"到底推哪个"的困惑）：

1. **`git commit` 只改"桌上文件"（本地分支），便签（快照）纹丝不动。**
2. **`git push` 推的是"桌上文件"（本地分支），推成功后 Git 顺手把便签刷新成最新。**
3. **便签只在 `fetch` / `pull` / `push` 三个动作时被动刷新，平时永远静止。**

### 核心原理（先搞懂这几个）

**1. 三个世界**：同一个分支名存在于三处——远程分支（服务器）、远程跟踪分支（本地快照 `origin/xxx`）、本地分支（你的工作区）。快照是"记忆"，不是"另一个工作副本"。

**2. `git pull origin dev` 的方向**：永远是把「远程 dev」拉下来，合并到「**你当前站着的分支**」。拆开 = `fetch`（下载到快照 `origin/dev`）+ `merge`（快照合进当前分支）。

**3. `git push origin dev-xuy` 推的是「本地分支 dev-xuy」**，不是快照；快照 `origin/dev-xuy` 只是被 push 成功后顺带刷新。

**4. 冲突的本质**：两个分支**改了同一文件的同一行**，Git 无法自动判断保留谁，停下来让你人工决定。冲突发生在**任何一次"合并"动作**：本地 merge、pull、PR 合并都可能触发。

### 我在项目里怎么用的（标准动作）

#### ① 每天开工前：拉最新主线

```bash
git checkout dev
git pull origin dev        # 远程 dev → 本地 dev
git checkout dev-xuy
git merge dev              # 本地 dev → 本地 dev-xuy
# 或一步：git checkout dev-xuy && git pull origin dev
```

#### ② 开发中：小步、频繁提交

```bash
git add .
git commit -m "feat(ums): 完成登录接口"   # 一个小功能一个 commit，别攒一堆
```

> 遵循 DEVGUIDE §9.2：每个任务一个 commit；类型 `feat`/`fix`/`docs`/`refactor`。

#### ③ 提交推送前：先拉后推（减少冲突的关键）

```bash
git add .
git commit -m "feat: xxx"

# 关键一步：push 前先 pull，把主线最新代码合进来，冲突提前在这里解决
git checkout dev-xuy
git pull origin dev        # 远程 dev → 当前 dev-xuy（有冲突就地解决）

git push origin dev-xuy    # 本地 dev-xuy → 远程 dev-xuy（先拉后推，避免 non-fast-forward）
```

#### ④ 冲突解决套路

```bash
# 冲突后文件里出现标记，手动合并、删掉标记：
<<<<<<< HEAD
你的代码
=======
别人的代码
>>>>>>> dev

git add .
git commit -m "merge: 解决与 dev 的冲突"
git push origin dev-xuy
```

### 减少冲突的三板斧（记忆点）

1. **先拉后推**：push 前一定先 `pull`，把别人的改动先合进来、解决完冲突再推。
2. **小步提交**：频繁 commit，别攒几百行才提交一次，冲突面小、易回滚。
3. **及时合主线**：别让 `dev-xuy` 长时间偏离 `dev`，每隔一段时间就把主线最新代码 merge 进来，避免最后一次性大冲突。

### 面试可能追问

- **Q1：`git fetch` 和 `git pull` 的区别？**
  - 答：`fetch` 只把远程更新到本地快照 `origin/xxx`，**不动工作分支**；`pull` = `fetch` + `merge`，把远程合并进**当前分支**。安全起见可以先 `fetch` 看差异再决定是否 `merge`。

- **Q2：`git merge` 和 `git rebase` 的区别？**
  - 答：`merge` 保留两个分支的历史，产生一个"合并提交"，历史是分叉的；`rebase` 把你分支的提交"搬家"到目标分支顶端，历史是一条直线更干净，但会**改写提交哈希**，公共分支慎用（会坑队友）。

- **Q3：合并冲突怎么解决？**
  - 答：找到 `<<<<<<<` / `=======` / `>>>>>>>` 标记，手动保留想要的代码、删掉标记 → `git add` → `git commit`；或用 IDE 三栏对比工具。解决冲突就是"人工告诉 Git 最终该长什么样"。

- **Q4：为什么 push 前要先 pull？不 pull 会怎样？**
  - 答：如果远程分支已被别人推进了新提交，你本地落后，直接 `push` 会报 `non-fast-forward` 被拒绝。先 `pull` 把远程最新代码合进来（解决可能出现的冲突），再 `push` 才能成功。

### 踩坑提醒

1. **坑：直接 `git push origin dev`（推公共分支）**
   - 现象：污染公共分支、无法 review、误操作难回滚。
   - 规避：只推自己的 `dev-xuy`，通过 PR/MR 合入 `dev`。

2. **坑：攒一大堆改动才提交一次**
   - 现象：冲突面巨大、难排查、回滚只能整块回退。
   - 规避：小步提交，一个功能/一个任务一个 commit。

3. **坑：长时间不合并主线，最后一次性 merge 大冲突**
   - 现象：分支偏离 `dev` 太久，最后 merge 时满屏冲突。
   - 规避：定期（每天/每个功能点）`pull origin dev` 把主线合进来，冲突化整为零。

4. **坑：push 被拒（non-fast-forward）就懵了**
   - 现象：报 `rejected ... non-fast-forward` 后不知道怎么处理。
   - 规避：这是"本地落后于远程"的正常提示，`git pull origin dev-xuy`（可能产生冲突）→ 解决 → 再 `git push`。

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

## Redis 登录失败锁定 + Token 黑名单 + 登录态缓存（防暴力破解 & 主动登出）

- 学于：2026-08-26
- 关联模块：`insight-engine-ums` 的 `AuthServiceImpl` + `RedisTokenBlacklistService` + `AuthConstants` + `JwtAuthFilter`
- 来源：PRD §12.1.5、TD §6.1、TD ADR-10

> 目标：把 UMS 用 Redis 实现的三件事**彻底掰开揉碎**——① 登录失败锁定（防暴力破解）、② Token 黑名单（主动登出）、③ 登录态缓存（支持踢人）。先纠正一个概念：**登录失败锁定不是「限流」**，两者的本质区别见下方「核心原理」。

### 直观类比（先建立直觉）

**类比①：登录失败锁定 = 门禁刷卡机**

门禁卡刷错 5 次，机器就把你**锁在门外 30 分钟**（哪怕你后来想起了正确密码，也进不去，得等解封）。计数器要能「连续记错几次」，且「锁定到期自动解封」——这正好对应 Redis 的「计数 + TTL 过期」两个能力。

**类比②：Token 黑名单 = 门禁卡挂失名单**

你丢了门禁卡（登出），物业不是去改门禁的密码（改 JWT 密钥），而是把你的**卡号（token 摘要）**写进一本「挂失名单（黑名单）」。下次有人刷这张卡，保安先查挂失名单——**名单里有的，直接拦下**，根本不用验卡真伪。

**类比③：登录态缓存 = 门禁系统里「谁还持卡在线」的登记表**

物业想「踢人下线」（比如员工离职），只要在登记表里删掉这个人的记录，他手里的卡下次就刷不了。这个登记表就是 `ie:auth:token:{userId}`。

### 核心原理

#### 1. 登录失败锁定 ≠ 限流（先分清）

| 对比项 | 登录失败锁定（本项目已实现） | 限流 Rate Limit（本项目未做） |
|--------|--------------------------|------------------------------|
| 目的 | 防止暴力破解密码（**安全**） | 防止请求量过大打垮服务（**稳定性**） |
| 触发条件 | 连续**失败 N 次**（业务事件） | 单位时间**请求次数超阈值** |
| 计数维度 | 按「账号」记失败次数 | 按「接口/IP/用户」记调用次数 |
| 失败动作 | 锁定账号 30 分钟 | 拒绝请求或排队 |
| 清零时机 | 登录成功 或 窗口过期 | 时间窗口滚动 |

> 一句话：**锁定是「你做错事惩罚你」，限流是「你来太频繁拦你」。**

#### 2. 登录失败锁定的三个 Redis 操作

PRD §12.1.5 需求：**连续密码错 5 次 → 锁定 30 分钟**。拆成三个 Redis 能力：

| Redis 命令 | 项目代码 | 作用 |
|-----------|---------|------|
| `INCR` | `opsForValue().increment(failKey)` | 原子自增失败次数 |
| `EXPIRE` | `expire(failKey, 30min)` | 设滑动窗口 TTL（只在第一次失败设） |
| `EXISTS` / `SET` | `hasKey(lockKey)` / `set(lockKey,"1",30min)` | 检查/设置锁定标记 |

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

#### 5. 为什么黑名单 key 用 SHA-256 摘要而非 token 明文？

防止 token 泄露在 Redis 键中（运维排查 Redis 时不会直接看到可用 token）。用 JDK 自带 `MessageDigest` 计算 SHA-256，不引入额外依赖。

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

核心算法 `handleLoginFail()`：

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

校验时黑名单优先 `JwtAuthFilter.doFilterInternal()`：

```java
// 黑名单优先于签名校验（TD §8.3：黑名单 → 签名 → 过期）
if (blacklistService != null && blacklistService.isBlacklisted(token)) {
    writeUnauthorized(response, ErrorCode.UNAUTHORIZED);
    return;
}
```

#### 链路③：登录态缓存（支持踢人）

登录/刷新成功后写缓存 `AuthServiceImpl.cacheToken()`：

```java
// ie:auth:token:{userId} = access token，TTL = 2h（access 有效期）
stringRedisTemplate.opsForValue().set(KEY_AUTH_TOKEN + userId, accessToken,
        Duration.ofSeconds(jwtUtil.getAccessTtlSeconds()));
```

「踢人」场景：管理员禁用用户（`UserServiceImpl.updateStatus()`）或用户改密（`updatePassword()`）时，删除 `ie:auth:token:{userId}`，该用户已签发的 token 下次请求即失效。

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

### 面试可能追问

- **Q1：登录失败锁定为什么用 Redis 而不是数据库？**
  - 答：锁定是「30 分钟临时态」，无需持久化；Redis 天然支持 TTL 到期自动解锁，且 INCR 原子自增避免并发丢计数。用数据库还得加定时任务清理过期锁，得不偿失。

- **Q2：`INCR` 为什么能保证并发安全？`get+set` 会有什么问题？**
  - 答：Redis 命令单线程串行执行，`INCR` 的「读+加+写」不可分割，天然原子。`get→+1→set` 三步之间可被其他请求穿插，两个并发请求读到同一旧值，后写覆盖先写，造成「丢失更新」（lost update）——本应记 5 次却只记 4 次，锁不住。

- **Q3：JWT 无状态，登出后怎么让 token 失效？**
  - 答：用黑名单。登出时把 token 摘要写入 Redis 黑名单（TTL=剩余有效期），每次请求认证过滤器先查黑名单，命中即拒绝。这是「有状态的黑名单」弥补「无状态的 JWT」无法撤销的短板（TD ADR-10）。

- **Q4：黑名单 TTL 为什么设为 token 剩余有效期，而不是固定值？**
  - 答：token 过期后本就失效，无需继续留在黑名单；设为剩余有效期能让黑名单条目到期自动清除，不无限膨胀，也不浪费内存。

- **Q5：为什么黑名单 key 存的是 SHA-256 摘要而不是 token 明文？**
  - 答：安全考虑——防止可用 token 直接暴露在 Redis 键中（运维排查、Redis 被拖库时），摘要不可逆，拿到摘要也无法还原 token。

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

4. **坑：登出时忘了删除登录态缓存，只加黑名单**
   - 现象：黑名单让「旧 token」失效了，但登录态缓存里还留着，如果后续有「踢人/改密」逻辑判断登录态，会出现状态不一致。
   - 规避：登出同时做两件事——加黑名单（废 token）+ 删登录态缓存（清状态）。

---

## Spring Boot 自动装配机制（AutoConfiguration.imports）

- 学于：2026-08-26
- 关联模块：4 个 starter 的 `config` 包（`WebAutoConfiguration` / `SecurityAutoConfiguration` / `MybatisAutoConfiguration` / `RedisAutoConfiguration`）+ 各 starter 的 `META-INF/spring/...AutoConfiguration.imports` 文件
- 来源：TD §3.1

> 目标：彻底搞懂「starter 引进来为什么 Bean 就自动生效了」。核心就一个词——**自动装配（Auto-Configuration）**。

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

| 写法 | 代码 | 缺点 |
|------|------|------|
| A. 手动 `@Import` | 启动类上 `@Import({WebAutoConfiguration.class, ...})` | 每引一个 starter 加一行，漏了就不生效，还耦合具体类名 |
| B. 大范围 `@ComponentScan` | `@ComponentScan("com.insightengine")` | 扫进不该扫的类，失去「按需引入」控制力 |
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

| 对比项 | `spring.factories`（2.x） | `AutoConfiguration.imports`（3.x） |
|--------|--------------------------|-----------------------------------|
| 文件位置 | `META-INF/spring.factories` | `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` |
| 文件性质 | 一个文件塞多种类型（配置、监听器、初始化器） | **专门只放自动配置类** |
| 读取方式 | 启动时全量加载 | 惰性、按需、配合条件注解过滤 |
| 职责 | 混合、易冲突 | 单一、语义清晰 |

动机：**职责单一 + 加载性能优化**。文件路径本身说明了用途，且能更精准控制加载顺序和条件过滤。

### 我在项目里怎么用的（4 个 starter 的对应关系）

| starter | imports 文件登记的类 | 装配出的关键 Bean |
|---------|---------------------|------------------|
| `starter-web` | `WebAutoConfiguration` | TraceFilter / UserContextFilter / GlobalExceptionHandler |
| `starter-mybatis` | `MybatisAutoConfiguration` | 分页拦截器 / 审计填充器 / 逻辑删除配置 |
| `starter-redis` | `RedisAutoConfiguration` | JSON 序列化的 RedisTemplate |
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
④ 都通过 → 解析出 userId + 角色 + 权限 → 放行
```

> 这就是「无状态」：第 3 步的每次请求，服务端**不查数据库**，只靠 token 自带信息验证身份和权限。

#### 2. token 过期如何「无感刷新」（重点）

**为什么两个 token？—— 有效期矛盾**

| | access token | refresh token |
|--|-------------|--------------|
| 作用 | 每次请求证明身份 | 专门换新的 access token |
| 有效期 | 2 小时（短） | 7 天（长） |
| 泄露后果 | 2 小时内有效，危害小 | 7 天内有效，危害大 |
| 携带方式 | 每次请求都带 | 平时不用，只在刷新时用一次 |

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

| | 有状态（Session） | 无状态（JWT） |
|--|------------------|--------------|
| 身份存哪 | 服务端存 Session（内存/Redis） | 客户端 token 里自带 |
| 每次请求 | 拿 sessionId 去服务端查 | 直接解析 token 验签 |
| 能否主动登出 | ✅ 删 Session 即可 | ❌ 无法直接撤销 |
| 多实例扩展 | 要共享 Session（粘性/Redis） | ✅ 任意实例都能验 |

#### 4. 三种「要让 token 失效」的场景（核心）

| 场景 | 粒度 | 项目做法 | 代码位置 |
|------|------|---------|---------|
| **登出** | 单个 token | 加黑名单 | `AuthServiceImpl.logout()` |
| **改密** | 该用户全部 token | 删登录态缓存 | `UserServiceImpl.updatePassword()` |
| **禁用** | 该用户全部 token | 删登录态缓存 | `UserServiceImpl.updateStatus()` |

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
// ① 黑名单优先（登出 token 拦截）
if (blacklistService != null && blacklistService.isBlacklisted(token)) {
    writeUnauthorized(response, ErrorCode.UNAUTHORIZED);
    return;
}
// ② 签名 + 过期校验（JWT 本身有效性）
JwtPayload payload = jwtUtil.parseAccessToken(token);
```

> 关键设计：**黑名单检查在签名校验之前**——因为一个 token 即使签名有效、没过期，只要登出过就必须拒绝。

#### 7. 改密/禁用的失效机制其实不完整（深入拆解）

上面这套方案看着「逻辑自洽」，但落到代码上有致命缺口：**「删缓存」和「查缓存」两头只做了一头**。

**根因**：

- 写入方删了缓存：`UserServiceImpl.updateStatus()` 禁用时 `delete(ie:auth:token:{id})`；`updatePassword()` 改密时同样删。
- 校验方从不读缓存：`JwtAuthFilter.doFilterInternal()` 只查黑名单 `isBlacklisted(token)`，**从不读 `ie:auth:token:{userId}`**。删了等于白删——过滤器根本不知道缓存被删了。

**现象（时序）**：

```
① 用户 A 已登录，拿到 access token（2h 有效）
② 管理员禁用 A（updateStatus → 删了 ie:auth:token:A）
③ A 拿旧 token 继续请求 → JwtAuthFilter 不读缓存 → 验签通过 → 正常放行 ✅（本该拒绝 ❌）
④ A 还能继续用最多 2 小时，直到 token 自然过期
```

**附带问题**：`AuthServiceImpl.cacheToken()` 是「死代码」——写进去没人读，纯浪费 Redis 内存；且存的是**完整 token 明文**（注释却说「存摘要」，自相矛盾），Redis 被拖库即泄露可用 token。

**修复方案**（三选一，推荐 B）：

| 方案 | 做法 | 特点 |
|------|------|------|
| A | `JwtAuthFilter` 验签后额外查 `ie:auth:token:{userId}` 存在且摘要匹配 | 改动最小，但每次请求多一次 Redis 查询 |
| **B（推荐）** | JWT 引入 `jti` + `ver` 会话版本号，禁用/改密时 `ver`+1 写 Redis，过滤器比对版本 | 无状态友好、可扩展 |
| C | 改密/禁用时枚举该用户全部会话 token 加黑名单 | 需维护 userId→tokenHash 集合，最复杂 |

**方案 B（`jti` + `ver`）怎么落地**：

- `jti`（JWT ID）：每个 token 的唯一编号，放进 JWT claim，用于精确标记「某一个」token。
- `ver`（version）：用户级「会话版本号」，改密/禁用时 `ver+1` 写 Redis（key=`ie:auth:ver:{userId}`）。
- 签发时把当前 `ver` 也放进 JWT claim；校验时拿「JWT 里的 ver」和「Redis 里的 ver」比对，不一致就拒绝。
- 好处：**不用枚举每个 token**，一个用户一个 `ver` 就管住他所有 token；且对 JWT 的「无状态」破坏最小（只在版本变化时需要查一次 Redis）。

> 一句话总结：**JWT 无状态无法主动撤销，凡是「要让已签发 token 失效」的诉求，都必须引入一个「有状态的标记」来补——黑名单（单 token）、登录态缓存（单用户）、`ver` 版本号（单用户，更优雅）。**

**附带修复**：`cacheToken` 一律改存 SHA-256 摘要（与 `RedisTokenBlacklistService` 对齐），杜绝明文 token 落 Redis。

### 面试可能追问

- **Q1：JWT 无状态，怎么实现「登出」？**
  - 答：JWT 本身无法撤销，靠**黑名单**弥补。登出时把 token 摘要写入 Redis 黑名单（TTL=剩余有效期），每次请求认证过滤器先查黑名单，命中即拒绝。本质是「用有状态的黑名单补无状态 JWT 的短板」。

- **Q2：用户改密码 / 被禁用后，之前签发的 token 怎么失效？**
  - 答：这两个场景需要作废「该用户所有 token」，所以按**用户粒度**处理——删登录态缓存 `ie:auth:token:{userId}`。与登出（单 token 黑名单）的粒度不同。

- **Q3：为什么登出用黑名单，改密/禁用用删登录态？能统一吗？**
  - 答：粒度不同。登出只作废「当前这一次」token，用黑名单按 token 记；改密/禁用要作废「这个用户全部」token，用登录态按 userId 记（删一个 key 全失效）。技术上能统一（都删登录态），但黑名单还能覆盖「token 被泄露、主动作废某个 token」的场景，职责更清晰。

- **Q4（深入）：只删登录态缓存，但 JWT 本身没过期，真的能拦住吗？**
  - 答：**关键点**——删缓存本身不会自动拦住请求，必须在**校验时查这个缓存**才有效。如果认证过滤器只看 JWT 签名+过期、不查登录态缓存，那删了也没用。所以「删缓存」和「校验时查缓存」必须配套。本项目 `JwtAuthFilter` 目前查了黑名单但未查登录态缓存，正是「删了没人读」的缺口。
  - 更优雅的解法是 **`jti` + `ver` 版本号**：签发时把用户级会话版本 `ver` 放进 JWT，改密/禁用时 `ver+1` 写 Redis，校验时比对版本，不一致即拒绝——一个用户一个版本号就管住他全部 token，无需枚举。

- **Q5：为什么黑名单/登录态放 Redis 而不是本地内存？**
  - 答：微服务多实例部署，本地内存各实例不共享（登出写实例 A、请求打到实例 B 就失效）。Redis 是集中式共享存储，所有实例读同一份。这是「分布式下状态一致性」的核心认知。

### 踩坑提醒

1. **坑：只删登录态缓存，但认证过滤器不查缓存，导致「删了没用」**
   - 现象：改密/禁用后，旧 token 依然能访问（因为 JWT 没过期、过滤器只验签）。
   - 规避：**「删缓存」和「校验时查缓存」必须配套**。要么过滤器校验时也查登录态缓存，要么把 token 也加黑名单，否则失效逻辑形同虚设。

2. **坑：登出只加黑名单、忘了删登录态，状态不一致**
   - 现象：黑名单让旧 token 失效了，但登录态缓存残留，后续踢人/判断在线状态时出错。
   - 规避：登出同时做两件事——加黑名单（废 token）+ 删登录态（清状态）。

3. **坑：改密/禁用删除缓存时用了错误的 key 粒度**
   - 现象：想作废「用户全部 token」，却按「单个 token」去删，导致其他设备的 token 还能用。
   - 规避：明确粒度——单 token 用黑名单（key 含 token 摘要），单用户用登录态（key 含 userId）。

4. **坑：把完整 token 明文写进 Redis 登录态缓存**
   - 现象：`cacheToken` 存的是完整 token 明文（注释却写「存摘要」），Redis 一旦被拖库，攻击者直接拿到可用 token。
   - 规避：缓存一律存 **SHA-256 摘要**（和黑名单服务一致），存摘要既能比对、又不暴露原文。

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

| 维度 | RBAC | ABAC |
|------|------|------|
| 授权依据 | 角色（role） | 属性（用户/资源/环境/动作） |
| 粒度 | 粗（一个角色一堆权限） | 细（可到「谁能看哪条数据」） |
| 管理成本 | 低，改角色即可 | 高，规则复杂 |
| 典型场景 | 功能权限（能不能进这个页面） | 数据权限（能看哪几行数据） |
| 表结构 | user/role/permission 三张 | 需属性+规则引擎 |

#### 2. 项目用的是哪种？

**RBAC 为主 + 角色上带 scope 数据范围属性**（向 ABAC 过渡的中间形态）。

看 `Role` 实体的 `scope` 字段：`ALL/ORG/WS/SELF`（全局/组织/工作空间/本人），它就是「数据范围」属性——决定这个角色能看哪些数据。`RoleMapper.selectRoleCodesByUserId()` 的注释也写明「具体数据范围在 ABAC 拦截器阶段再按 scope 收敛」。

> 面试亮点：**「我们采用 RBAC 做功能权限，角色上挂 scope 数据范围字段，为后续 ABAC 数据权限留了扩展点」**——这句话比单纯说「我们用了 RBAC」高级得多。

**「角色上带 scope」详解（功能权限 vs 数据权限）**

一个「权限」其实分两层，很多人混为一谈：

| 层 | 回答的问题 | 例子 |
|----|-----------|------|
| **功能权限** | 你能不能「点这个按钮」 | 能不能进用户管理页 |
| **数据权限** | 你能「看到哪几行数据」 | 进了用户页，看所有人还是只看本部门 |

- **RBAC**（角色→权限表 `ie_role_permission`）管**功能权限**：`member:read`、`role:write` 这些「能不能做某操作」。
- **scope** 管**数据权限**：这个角色能「看到哪个范围的数据」。

scope 四个取值：

| scope 值 | 含义 | 谁用 |
|---------|------|------|
| `ALL` | 全平台数据 | super_admin |
| `ORG` | 本组织数据 | org_admin |
| `WS` | 本工作空间数据 | ws_admin |
| `SELF` | 只有自己的数据 | 普通成员 |

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

| 方案 | token 大小 | 每次请求成本 | 实时性 | 复杂度 |
|------|-----------|-------------|--------|--------|
| 现状：权限进 JWT | 大（~2KB） | 零查库 | 不实时 | 简单 |
| 方案①：角色进 JWT+缓存 | 小（~300B） | 一次缓存查询 | 可控制 | 中等 |
| 方案②：位图压缩 | 极小 | 零查库 | 不实时 | 中等 |

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

| | Cookie + Session | JWT |
|--|-----------------|-----|
| 凭证存哪 | Cookie（浏览器自动带） | Authorization 头（前端手动带） |
| 浏览器自动携带？ | ✅ 会 | ❌ 不会 |
| CSRF 风险 | 高（恶意站能借你 Cookie） | 无（恶意站带不上你的 JWT） |

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

| 表达式 | 含义 |
|--------|------|
| `hasAuthority('member:read')` | 有 `member:read` 权限 |
| `hasRole('ADMIN')` | 有 `ROLE_ADMIN` 角色（注意自动加 `ROLE_` 前缀） |
| `hasAnyAuthority('a','b')` | 有 a 或 b 任一权限 |
| `permitAll()` | 所有人可访问 |
| `isAuthenticated()` | 只要登录即可 |

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

| 场景 | 例子 |
|------|------|
| 管理类接口 | 用户分页/创建/删除 → `hasAuthority('member:read')` |
| 敏感操作 | 删除角色 → `hasAuthority('role:write')` |
| 本人操作 | 改自己密码 → 不用 @PreAuthorize，只要求登录（从上下文取 userId） |
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

---

## 微服务身份传递的信任边界（双身份源问题）

- 学于：2026-08-27
- 关联模块：`UserContextFilter` / `JwtAuthFilter` / `UserContext` / `WebAutoConfiguration`
- 来源：TD ADR-5

> 目标：搞懂「一个系统里为什么不能有两套身份来源」、请求头身份为什么不可信、以及身份传递的正确边界。

### 直观类比（先建立直觉）

**`UserContext` = 一个「我是谁」的盒子（ThreadLocal）**

- 业务代码需要知道「当前用户是谁」时，就从盒子里取（`UserContext.getUserId()`），不关心是谁放进去的。
- 危险在于：**如果盒子里装的身份，是一个「任何人都能自己写」的来源放进去的，那这个盒子就不安全了。**

> 一句话记忆：**身份只能有一个可信来源（服务端签名），绝不能信客户端可任意填写的明文。**

### 核心原理

#### 1. 两套身份来源并存（隐患根源）

系统有两个过滤器都在往 `UserContext` 盒子里塞身份：

| 过滤器 | 身份从哪来 | 客户端能伪造吗 | 执行顺序 |
|--------|-----------|--------------|---------|
| `UserContextFilter` | 请求头 `X-User-Id`/`X-Roles` | **能！随便填** | 先（order=HIGHEST+1） |
| `JwtAuthFilter` | JWT（服务端签名） | 不能（没密钥） | 后 |

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

#### 3. 修复方案（二选一）

- **方案 A（UMS 走 JWT，默认关闭网关头）**：给 `UserContextFilter` 加条件装配开关，默认关闭：
  ```java
  @ConditionalOnProperty(name = "insight.web.trust-gateway-headers", havingValue = "true")
  public FilterRegistrationBean<UserContextFilter> userContextFilterRegistration() { ... }
  ```
  UMS 不配开关 → `UserContextFilter` 不生效 → 盒子里只有 `JwtAuthFilter` 塞的「可信身份」。
- **方案 B（走 TD ADR-5 明文头方案）**：给网关下发的头加 HMAC 签名 `X-User-Sign` 验签，或 IP 网段校验兜底。

### 面试可能追问

- **Q1：为什么不能让业务服务信任请求头里的 X-User-Id？**
  - 答：请求头是客户端完全可控的，信任它等于把「我是谁」的决定权交给攻击者，构成水平/垂直越权。身份必须来自服务端签名的 JWT（或加 HMAC 签名/IP 校验的网关头）。

- **Q2：两个过滤器都写 UserContext，为什么这是隐患？**
  - 答：因为安全依赖「后执行的过滤器一定覆盖先执行的」这个脆弱前提。一旦某个接口不经过后执行的过滤器（如白名单接口），先执行过滤器塞的「不可信身份」就会暴露。

### 踩坑提醒

1. **坑：白名单接口是越权重灾区**
   - 现象：permitAll 接口不校验 token，但全路径过滤器照常解析请求头，伪造头直接生效。
   - 规避：身份只能有一个可信来源；走 JWT 就关闭明文头过滤器（条件装配开关），走网关头就加签名/IP 校验。

---

（持续沉淀中，学一个补一个）
