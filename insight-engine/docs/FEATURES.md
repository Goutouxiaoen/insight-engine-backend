# 功能模块实现清单（FEATURES）

> 版本：v1.0
> 首次建立：2026-08-26
> 读者：项目唯一开发者
> 定位：**每个功能模块完成后沉淀于此**，说明「实现了哪些功能」+「每个功能如何实现」，
>       方便随时回看某个功能的关键类、关键逻辑、设计决策。
> 与真相源的关系：PRD（做什么）/ TD（怎么做）/ IF（接口契约）/ PROGRESS（进度）/ 本文档（实现说明）。

---

## 目录

- 一、UMS 认证与用户服务（insight-engine-ums）
- 二、基础设施 starter（starter-web / mybatis / redis / security）

---

# 一、UMS 认证与用户服务（insight-engine-ums）

## 模块概览

| 项 | 值 |
|----|----|
| 服务名 | insight-engine-ums（用户与权限服务） |
| 端口 | 7101（PRD §9.2） |
| 定位 | 认证 + 用户 + 角色 + 权限（RBAC + JWT），全平台统一身份入口 |
| 依赖 starter | web / security / mybatis / redis |
| 核心表 | ie_user / ie_role / ie_permission / ie_role_permission / ie_member |
| 接口文档 | Knife4j：`http://localhost:7101/doc.html` |
| 接口数量 | 16 个（认证 5 + 用户 5 + 角色 5 + 权限树 1） |

---

## 功能模块 1：认证（AuthController / AuthService）

**职责**：登录、刷新、登出、注册、当前用户——身份认证全闭环。

### 1.1 登录 `POST /auth/login`

- **接口**：账号（邮箱/手机号）+ 密码 → 返回 token + refreshToken + expiresIn + 用户信息
- **实现类**：`AuthServiceImpl.login()`
- **关键逻辑**（PRD §12.1.5 登录安全策略）：
  1. 锁定检查：Redis 命中 `ie:auth:lock:{account}` 直接拒绝（2003），不查库
  2. 查用户：`UserMapper.selectByAccount()`（email OR phone 双列匹配）
  3. 状态校验：禁用返回 2004
  4. 密码校验：BCrypt `matches()`，失败递增 Redis 计数，5 次锁定 30 分钟
  5. 成功：清计数、更新 last_login_at、签发 JWT、写登录态缓存
- **为什么锁定用 Redis 而非落库**：锁定是 30 分钟临时态，无需持久化；Redis 天然支持 TTL 到期自动解锁

### 1.2 刷新令牌 `POST /auth/refresh`

- **接口**：refreshToken → 换新令牌对
- **实现类**：`AuthServiceImpl.refresh()`
- **关键逻辑**：`JwtUtil.parseRefreshToken()` 校验签名+类型+过期，失败返回 2001；成功后重签令牌对
- **为什么 refresh 只校验不查密码**：刷新令牌本身已含签名与用户 ID，无需二次密码认证（TD §7.2）

### 1.3 登出 `POST /auth/logout`

- **接口**：Authorization 头带 access token → 加入黑名单
- **实现类**：`AuthServiceImpl.logout()` + `RedisTokenBlacklistService`
- **关键逻辑**：token 加黑名单（TTL = 剩余有效期）+ 删除登录态缓存
- **为什么黑名单 TTL 用剩余有效期**：token 过期后本就失效，黑名单条目到期自动清除，不无限膨胀（TD ADR-10）

### 1.4 注册 `POST /auth/register`

- **接口**：邮箱 + 密码 + 昵称 → 返回新用户 ID
- **实现类**：`AuthServiceImpl.register()`
- **关键逻辑**：邮箱唯一校验 → BCrypt 加密 → 落库 → 挂默认工作空间（org=1/ws=1）+ end_user 角色
- **为什么注册用户挂 end_user 角色**：MVP 单租户开放注册，新用户默认最小权限角色（init.sql 预置 id=5）

### 1.5 当前用户 `GET /auth/me`

- **接口**：返回当前登录用户信息 + 角色列表 + 工作空间
- **实现类**：`AuthServiceImpl.currentUser()`
- **关键逻辑**：用户 ID 从 `UserContext`（JWT 已解析）读取，不信任客户端传参，杜绝水平越权
- **为什么 workspaceName 由 UMS 直查 ie_workspace**：MVP 临时方案（同库只读），workspace 服务落地后改走 Feign（见 `WorkspaceMapper` 注释）

---

## 功能模块 2：用户管理（UserController / UserService）

**职责**：管理员对用户的 CRUD、启停、改密（需对应权限）。

### 2.1 用户分页 `GET /api/v1/user/page`

- **接口**：pageNum/pageSize + keyword → 分页列表
- **权限**：`member:read`
- **实现类**：`UserServiceImpl.page()`
- **关键逻辑**：keyword 模糊匹配昵称/邮箱；手机号脱敏输出（138****1234）

### 2.2 创建用户 `POST /api/v1/user`

- **接口**：邮箱 + 昵称 + 密码 + 可选手机号 + 角色 ID → 新用户 ID
- **权限**：`member:create`
- **实现类**：`UserServiceImpl.create()`
- **关键逻辑**：邮箱唯一校验 → BCrypt → 落库 + 挂指定角色到默认工作空间（事务）

### 2.3 更新用户 `PUT /api/v1/user/{id}`

- **接口**：昵称/手机号 → 无返回值
- **权限**：`member:update`
- **关键逻辑**：仅更新非空字段；手机号空串转 null

### 2.4 启用/禁用 `PUT /api/v1/user/{id}/status`

- **接口**：status(0/1) → 无返回值
- **权限**：`member:update`
- **关键逻辑**：禁用时删除 Redis 登录态，已签发 token 下次校验即失效（强制重新登录）

### 2.5 修改密码 `PUT /api/v1/user/password`

- **接口**：旧密码 + 新密码 → 无返回值
- **实现类**：`UserServiceImpl.updatePassword()`
- **关键逻辑**：校验旧密码（2002）→ BCrypt 更新 → 删除登录态（旧 token 全失效）

---

## 功能模块 3：角色管理（RoleController / RoleService）

**职责**：角色 CRUD 与授权（RBAC 核心）。

### 3.1 角色列表 `GET /api/v1/role/list`

- **权限**：`role:read`
- **实现类**：`RoleServiceImpl.list()`，返回全部角色（含内置）

### 3.2 创建角色 `POST /api/v1/role`

- **权限**：`role:write`
- **关键逻辑**：编码唯一校验 → 落库 → 可选批量授权（事务）

### 3.3 角色详情 `GET /api/v1/role/{id}`

- **权限**：`role:read`，含已授权权限 ID 列表

### 3.4 删除角色 `DELETE /api/v1/role/{id}`

- **权限**：`role:write`
- **关键逻辑**：`builtin=1` 内置角色禁止删除（1003），防止误删预置角色；删除后清理角色-权限关联

### 3.5 角色授权 `PUT /api/v1/role/{id}/permissions`

- **权限**：`role:write`
- **关键逻辑**：先删后插（`RolePermissionMapper.deleteByRoleId` + `batchInsert`），权限集合与请求完全一致，幂等

---

## 功能模块 4：权限（PermissionController / PermissionService）

### 4.1 权限树 `GET /api/v1/permission/tree`

- **权限**：`role:read`
- **实现类**：`PermissionServiceImpl.tree()`
- **关键逻辑**：按 `resource` 字段分组（LinkedHashMap 保序），组内为权限点；resource → 中文名映射本地维护
- **为什么用 LinkedHashMap**：保证分组顺序稳定，前端展示不随查询顺序抖动

---

## 功能模块 5：RBAC 权限体系（贯穿全局）

**职责**：角色-权限-用户的完整权限链路。

### 权限链路

```
用户 → member(角色关联) → role → role_permission → permission(code)
```

- 登录时：`PermissionMapper.selectPermissionCodesByUserId()` 查出用户全部权限编码（DISTINCT 去重，多角色权限可能重叠）
- 权限编码写入 JWT 的 `perms` Claim（登录时展开，因为 JWT 无状态、服务端不每次查库）
- 接口鉴权：`JwtAuthFilter` 把 `perms` 转为 `SimpleGrantedAuthority` 写入 SecurityContext
- 方法级权限：Controller 上用 `@PreAuthorize("hasAuthority('member:read')")` 精确控制

### 内置角色与权限（init.sql 种子）

| 角色 | code | 说明 |
|------|------|------|
| 超级管理员 | super_admin | 全部 48 个权限 |
| 组织管理员 | org_admin | 组织级管理 |
| 空间管理员 | ws_admin | 工作空间级管理 |
| 普通成员 | member | 空间内操作 |
| 访客 | end_user | 最小权限（注册默认角色） |

---

# 二、基础设施 starter

## starter-web（阶段 1 已实现）

统一响应体 / 全局异常处理 / TraceID / 用户上下文（详见 PROGRESS 阶段 1）。

## starter-mybatis（本次实现）

- **定位**：MyBatis-Plus 统一装配，业务服务引入即获得通用 ORM 能力
- **实现类**：`MybatisAutoConfiguration` / `MybatisMetaObjectHandler`
- **能力**：
  1. 全局逻辑删除：`deleted` 字段（0 正常/1 删除），所有含该列的表自动走逻辑删除
  2. 分页插件：PaginationInnerInterceptor（PG 方言），单页上限 100（双保险拦截非法分页）
  3. 审计字段自动填充：created_at/updated_at/created_by/updated_by（UTC + 未登录兜底 0）
- **为什么逻辑删除用全局配置而非逐实体注解**：避免新表漏配导致数据被物理删除

## starter-redis（本次实现）

- **定位**：RedisTemplate 统一序列化
- **实现类**：`RedisAutoConfiguration`
- **能力**：key 用 StringRedisSerializer（可读），value 用 JSON 序列化（保留类型信息，反序列化还原 POJO 而非 LinkedHashMap）
- **为什么不用 JDK 默认序列化**：二进制不可读、要求实现 Serializable、不可跨语言

## starter-security（本次实现）

- **定位**：无状态 JWT 认证体系，任何业务服务引入即获得统一鉴权
- **实现类**：`SecurityAutoConfiguration` / `JwtUtil` / `JwtAuthFilter` / `RestAuthenticationEntryPoint` / `RestAccessDeniedHandler` / `TokenBlacklistService`
- **能力**：
  1. SecurityFilterChain：关闭 CSRF + 无状态 Session + 白名单放行 + 其余需认证
  2. JWT 签发/解析（HS256）：access(2h) / refresh(7d)，`type` Claim 防令牌混淆
  3. 认证过滤器：Bearer 头解析 → 权限转 authorities → 填充 UserContext
  4. 未认证/无权限统一转 Result 结构（2001/2006）
  5. 可选黑名单：`TokenBlacklistService` 接口 + `ObjectProvider` 注入，未提供实现则退化为纯无状态校验
- **为什么黑名单做成可选接口**：starter-security 不依赖 Redis，其他服务复用时不强制引入 Redis 依赖

---

> 后续每完成一个功能模块，在「一、」下按「功能模块 N」格式追加，保持表格 + 关键逻辑说明的结构。
