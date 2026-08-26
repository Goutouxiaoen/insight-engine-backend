# 学习笔记（LEARNING）

> 本文件是你在开发过程中的**个人技能笔记真相源**。
> 每学完一个技术点，让 AI 帮你把"原理 + 项目用法 + 面试追问点"沉淀到本文件。
> 面试前快速翻阅本文件即可复习全部技术点。

---

## 笔记索引

> 按模块/主题组织，学一个补一个。

### 待学习清单（对应 TD 技术栈）

- [x] Docker 网络模型与端口映射（2026-08-25 已学，见下方笔记）
- [x] Maven 多模块工程与依赖管理（2026-08-25 已学，见下方笔记）
- [x] Java 包级元数据：package-info.java / 包级注解（2026-08-25 已学，见下方笔记）
- [x] Web 安全基础：日志注入 / 越权面 / 白名单校验（2026-08-26 已学，见下方笔记）
- [ ] Spring Boot 3 与 Java 17 新特性
- [ ] Spring Security + JWT 认证
- [ ] RBAC / ABAC 权限模型
- [ ] Spring Cloud Gateway 与过滤器链
- [x] Nacos 服务注册与配置中心（2026-08-26 已学服务注册/发现，配置中心待学）
- [ ] OpenFeign 服务调用
- [ ] MyBatis-Plus 与数据权限拦截器
- [ ] PostgreSQL + PGVector 向量检索
- [ ] Spring AI 与模型适配器模式
- [ ] LangChain4j 与 Agent
- [ ] ReAct 与 Function Calling 原理
- [ ] RabbitMQ 异步任务与死信
- [ ] Redis 缓存与分布式锁
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
- 来源决策：TD §18.2、ADR-11 / ADR-12、PROGRESS 三/四

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

### 踩坑提醒（3 个易踩坑 + 规避）

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
- 来源决策：TD §3「工程结构与模块依赖」、PRD §9.3、PROGRESS 三/四

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
- 来源决策：PROGRESS 踩坑记录（IDEA 文件识别问题引出）

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
- 来源决策：TD §8「微服务治理设计」

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
- 关联模块：`starter-web` 的 `TraceFilter`（已修复）与 `UserContextFilter`（暂缓）
- 来源决策：阶段 1 工程骨架 review 的两个红级问题、TD ADR-5、PROGRESS 七

> 目标：用一次真实的安全 review 讲透三条安全常识——**日志注入、越权面、白名单校验思想**。它们共用一条主线：**永远不要无条件信任客户端可控的输入。**

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

**① `TraceFilter`（已修复，白名单校验 + 非法重生成）**

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

**② `UserContextFilter`（暂缓，红级已知问题）**

现状：无条件信任网关下发的明文身份头（`X-User-Id`/`X-Tenant-Id`/`X-Roles`），直接组装 `LoginUser`。这是 TD ADR-5 的既定权衡（网关唯一入口、内网可信），但若业务服务被绕过网关直连，即可伪造身份越权。

留待阶段 3 的三种补救（至少做一项）：

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

（持续沉淀中，学一个补一个）
