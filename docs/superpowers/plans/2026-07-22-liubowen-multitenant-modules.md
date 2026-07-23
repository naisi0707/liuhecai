# 刘伯温同构多租户论坛 — 分模块实现计划

> **For agentic workers:** 本计划按**模块门禁**执行。每个模块全部完成后，必须等待用户明确验收通过，才能把该模块勾选为完成并开始下一模块。禁止跨模块并行开工。  
> REQUIRED SUB-SKILL when implementing a module: `superpowers:executing-plans` 或按任务逐步落地；模块内可再拆 Task。

**Goal:** 交付可运营的多租户资料论坛（结构对齐刘伯温；超管开站；代理运营；人工充值；公开源开奖）。

**Architecture:** 单库 `tenant_id` + Host 解析租户；`apps/api` Spring Boot；`apps/web|admin|agent` 三个 Nuxt 3 应用；`packages/shared` 共享类型与请求封装。

**Tech Stack:** Nuxt 3、Spring Boot 3、MyBatis-Plus（TenantLine）、MySQL 8、Redis、JWT、MapStruct、Element Plus（仅 admin/agent）

**规范（必读）：**

- [docs/backend-coding-standards.md](../../backend-coding-standards.md)
- [docs/frontend-coding-standards.md](../../frontend-coding-standards.md)
- [docs/superpowers/specs/2026-07-22-liubowen-multitenant-forum-design.md](../specs/2026-07-22-liubowen-multitenant-forum-design.md)

**门禁规则：**

1. 同一时间只做**一个**模块。
2. 模块交付物 + 验收清单完成后，停下来等用户说「通过 / 打勾」。
3. 用户通过后，才把下方对应 `- [ ]` 改为 `- [x]`，再开下一模块。
4. 下一模块开始前，先写清该模块的详细 Task 步骤（若本文件该节仍是摘要），再编码。

---



## 模块总览（验收看板）


| 模块  | 名称                 | 状态  | 用户验收 |
| --- | ------------------ | --- | ---- |
| M1  | 工程骨架与本地环境          | 已完成 | ✅    |
| M2  | 多租户基础设施            | 已完成 | ✅    |
| M3  | 三端鉴权（超管/代理/用户）     | 已完成 | ✅    |
| M4  | 超管开站与域名/代理账号       | 已完成 | ✅    |
| M5  | 代理换肤与站点配置          | 已完成 | ✅    |
| M6  | 开奖（拉取 + 覆盖 + 前台展示） | 已完成 | ✅    |
| M7  | 资料帖与购帖扣币           | 已完成 | ✅    |
| M8  | 人工充值确认             | 已完成 | ✅    |
| M9  | 前台刘伯温结构页补齐与联调      | 待验收 | ⏳    |
| M10 | MVP 总验收与演示数据       | 未开始 | ⏳    |


---



## 仓库文件地图（锁定）

```
liuhecai/
├── apps/
│   ├── api/                 # Spring Boot 3 · com.liuhecai
│   ├── web/                 # 站点前台 Nuxt
│   ├── admin/               # 超管 Nuxt + Element Plus
│   └── agent/               # 代理 Nuxt + Element Plus
├── packages/
│   └── shared/              # Result、request、公共类型
├── docker-compose.yml       # MySQL + Redis
├── docs/
│   ├── backend-coding-standards.md
│   ├── frontend-coding-standards.md
│   └── superpowers/
└── README.md
```

---



## M1 — 工程骨架与本地环境

**目标：** 使用本机已有 JDK/Maven/Node/pnpm/MySQL；API `/health`；三个 Nuxt 应用可启动；shared 包可被引用。**不使用 Docker。**

**Files（将创建）：**

- `apps/api/`（Spring Boot 骨架，含 `Result`、健康检查；M1 排除数据源自动配置）
- `apps/web/`、`apps/admin/`、`apps/agent/`（Nuxt 3 骨架）
- `packages/shared/`（`Result` 类型 + request 封装）
- `README.md`（启动说明）
- 根 `package.json` + `pnpm-workspace.yaml`

**验收清单（用户勾选）：**

- [ ] `apps/api` 启动后 `GET /api/health` 返回统一 `Result` 成功结构
- [ ] `web` / `admin` / `agent` 三个应用均可打开首页占位（3000/3001/3002）
- [ ] `packages/shared` 被三端成功 import（页上显示 Result 校验通过）
- [ ] README 写明端口与启动命令（无 Docker）
- [ ] **用户口头/文字确认「M1 通过」**

**详细步骤：** 见下文「M1 详细 Task」。

---



## M2 — 多租户基础设施

**目标：** `tenants` / `domains` 表；Host → tenant；MyBatis-Plus TenantLine；未识别域名友好错误。

**Files：**

- SQL migration / schema：`apps/api/src/main/resources/db/migration/` 或 `schema.sql`
- `TenantContext`、拦截器/过滤器、TenantLineHandler
- `TenantController`（内部测试用）或仅 Service + 集成测试
- `apps/web` 中间件：调用「当前租户」接口渲染站名占位

**验收：**

- [ ] 种子数据：2 个租户、2 个域名
- [ ] 用不同 Host（本地可用 hosts 或 Header 模拟）请求，返回不同租户信息
- [ ] 业务表示例插入自动带 `tenant_id`，跨租户查不到
- [ ] 未知域名返回约定错误码/错误页
- [ ] **用户确认「M2 通过」**

---



## M3 — 三端鉴权

**目标：** 超管 / 代理 / 前台用户三套登录与 JWT；权限互不串用。

**Files：**

- `super_admins` / `agent_accounts` / `users` 表与 CRUD 登录
- JWT 签发校验、Security 配置或拦截器分 realm
- `admin` 登录页、`agent` 登录页、`web` 登录/注册页（最小）

**验收：**

- [ ] 三端各自登录成功拿到 token，访问受保护接口
- [ ] 代理 token 调超管接口被拒；用户 token 调代理接口被拒
- [ ] 注册用户绑定当前租户
- [ ] **用户确认「M3 通过」**

---



## M4 — 超管开站与域名/代理账号

**目标：** 超管可创建租户、绑域名、生成/重置代理账密、启停站点。

**Files：**

- Admin API：`TenantAdminController` 等
- `apps/admin` 租户列表/创建/域名/代理账号页

**验收：**

- [ ] 超管 UI 可开第 2 个站并看到生成的代理账密
- [ ] 新站域名解析（或本地 hosts）后前台显示新站名
- [ ] 停用后前台不可用
- [ ] **用户确认「M4 通过」**

---



## M5 — 代理换肤与站点配置

**目标：** 代理改站名/Logo/主色/字体/公告/客服/广告位；前台 CSS 变量生效。

**Files：**

- `tenants.theme_json` 等字段更新 API
- `apps/agent` 配置页
- `apps/web` 注入 CSS 变量与公告条

**验收：**

- [ ] 代理改主色与公告后，前台刷新可见
- [ ] 不影响其他租户外观
- [ ] **用户确认「M5 通过」**

---



## M6 — 开奖

**目标：** 全局开奖表 + Job 拉至少 1 个公开源；代理可覆盖；前台 Tab 展示号码与生肖。

**Files：**

- `draw_results_global`、`draw_overrides`
- `DrawFetchJob`、解析器接口（可插拔）
- web 首页开奖区 + 倒计时（可先静态下期时间规则）

**验收：**

- [ ] Job 能写入至少一期结果（或 mock 源在测试环境）
- [ ] 拉取失败有日志且可手工补录
- [ ] 前台三个彩种 Tab 可切换（无数据的 Tab 可空态）
- [ ] **用户确认「M6 通过」**

---



## M7 — 资料帖与购帖扣币

**目标：** 代理/体系内发帖审核；用户扣币购帖；未购锁定正文。

**Files：**

- `topics` / `orders` / `coin_logs`
- 购帖事务 + 乐观锁扣余额
- web 列表/详情；agent 审帖

**验收：**

- [ ] 未购不可见正文；购买后可见且余额减少
- [ ] 重复购买不重复扣（或明确幂等策略并测过）
- [ ] 租户 A 帖子在租户 B 不可见
- [ ] **用户确认「M7 通过」**

---



## M8 — 人工充值确认

**目标：** 用户提交充值申请；代理确认加币；充值指引页展示客服方式。

**Files：**

- `recharge_requests`
- web 充值页；agent 确认列表

**验收：**

- [ ] 申请 → 代理确认 → 余额与流水正确
- [ ] 拒绝状态可区分
- [ ] **用户确认「M8 通过」**

---



## M9 — 前台结构补齐与联调

**目标：** 规则/客服/首页信息架构对齐刘伯温模块；三端联调无阻塞。

**验收：**

- [ ] 主导航与关键区块齐全（首页/规则/充值/客服/注册登录）
- [ ] 移动端可完整走通主路径
- [ ] **用户确认「M9 通过」**

---



## M10 — MVP 总验收

**对照 spec §9：**

- [ ] 2 租户不同域名数据不串
- [ ] 代理改肤/审帖/加币即时生效
- [ ] 注册→充值确认→购帖解锁跑通
- [ ] 至少 1 彩种自动开奖 + 可手工补
- [ ] 结构对齐刘伯温（自有素材）
- [ ] **用户确认「MVP 通过」**

---



## M1 详细 Task（仅本模块现在可执行）



### Task 1: pnpm workspace + shared（跳过 Docker，使用本机环境）

**Files:**

- Create: `package.json`、`pnpm-workspace.yaml`
- Create: `packages/shared/package.json`、`packages/shared/src/index.ts`、`packages/shared/src/result.ts`

- [ ] **Step 1: workspace 配置**

`pnpm-workspace.yaml`:

```yaml
packages:
  - 'apps/*'
  - 'packages/*'
```

- [ ] **Step 2: shared Result 类型**

```ts
// packages/shared/src/result.ts
export interface Result<T = unknown> {
  code: number
  message: string
  data: T
}

export function isOk<T>(r: Result<T>): boolean {
  return r.code === 0
}
```

- [ ] **Step 3: 导出**

```ts
// packages/shared/src/index.ts
export * from './result'
```



### Task 3: Spring Boot API 骨架

**Files:**

- Create: `apps/api/` Maven/Gradle 工程（推荐 Maven）
- `com.liuhecai.LiuhecaiApplication`
- `com.liuhecai.common.result.Result`
- `com.liuhecai.interfaces.health.HealthController`
- `application.yml`（端口、mysql、redis 占位）

- [ ] **Step 1: 生成 Spring Boot 3 项目**（Java 17+），依赖：web、validation、mybatis-plus、mysql、redis、lombok
- [ ] **Step 2: 统一 Result**（字段 `code/message/data`，成功 `code=0`，与前端规范一致）
- [ ] **Step 3: HealthController**

```java
@RestController
@RequestMapping("/api")
public class HealthController {
  @GetMapping("/health")
  public Result<Map<String, String>> health() {
    return Result.ok(Map.of("status", "UP"));
  }
}
```

- [ ] **Step 4: 启动验证**

Run: 启动应用后 `curl http://localhost:8080/api/health`  
Expected: `{"code":0,"message":"...","data":{"status":"UP"}}`

### Task 4: 三个 Nuxt 应用骨架

**Files:**

- Create: `apps/web`、`apps/admin`、`apps/agent`
- 各自 `nuxt.config.ts` 引用 `@liuhecai/shared`（workspace 协议）

- [ ] **Step 1:** `nuxi init` **三个应用（TypeScript）**
- [ ] **Step 2: 端口约定** — web `3000`、admin `3001`、agent `3002`
- [ ] **Step 3: 各应用首页占位**（显示应用名 + 从 shared import `Result` 类型做编译校验）
- [ ] **Step 4: 验证三端均可打开**



### Task 5: README + 用户验收门禁

- [ ] **Step 1: 写 README 启动步骤与端口表**
- [ ] **Step 2: 向用户提交 M1 验收清单，停止编码**
- [ ] **Step 3: 用户确认「M1 通过」后，将模块总览 M1 标为完成，再展开 M2 详细 Task**

---



## 执行约定（按你的要求）

1. **默认只做 M1**，直到你回复「M1 通过」。
2. 每个模块结束时我会贴「验收清单」对照结果，不自动打勾。
3. 你说「通过」后我才更新看板与 `task_plan.md`，并开始下一模块详细计划+实现。

---



## Self-review（计划作者）


| Spec 项               | 对应模块                       |
| -------------------- | -------------------------- |
| 多租户 tenant_id + Host | M2                         |
| 超管开站/域名/代理           | M4                         |
| 代理运营换肤等              | M5                         |
| 开奖拉取+覆盖              | M6                         |
| 资料/购帖                | M7                         |
| 人工充值                 | M8                         |
| 前台结构                 | M1 占位 → M9 补齐              |
| 三端鉴权                 | M3                         |
| MVP 验收               | M10                        |
| 不抓原站数据               | 全模块约束                      |
| 编码规范三 Nuxt 应用        | 文件地图已对齐（修正原 spec 单 web 表述） |


