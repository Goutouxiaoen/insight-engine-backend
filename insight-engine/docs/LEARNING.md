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
- [ ] Spring Boot 3 与 Java 17 新特性
- [ ] Spring Security + JWT 认证
- [ ] RBAC / ABAC 权限模型
- [ ] Spring Cloud Gateway 与过滤器链
- [ ] Nacos 服务注册与配置中心
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

（持续沉淀中，学一个补一个）
