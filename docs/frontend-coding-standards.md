# Nuxt 3 前端编码规范

> 适用于本项目三端：`apps/web`（站点前台）、`apps/admin`（超管）、`apps/agent`（代理后台）。  
> 与 [backend-coding-standards.md](./backend-coding-standards.md) 对齐：统一消费后端 `Result { code, message, data }`，页面要薄、类型清晰、可换肤、租户以后端隔离为准。

---

## 1. 技术栈与基本原则

| 项 | 约定 |
|---|---|
| 框架 | Nuxt 3 |
| 语言 | TypeScript |
| 写法 | Composition API + `<script setup lang="ts">` |
| 样式 | 纯 CSS / SCSS（不用 Tailwind；前台不引入重型 UI 库） |
| 请求 | 封装 `$fetch` / `ofetch`，统一解包 `Result` |
| 状态 | Pinia（仅跨页共享状态） |
| 结构 | 三个独立 Nuxt 应用 + `packages/shared` |

**基本原则**

1. **页面要薄**：组装布局与调用 composable/api，不堆业务与请求细节。
2. **类型对齐后端**：使用 `Result<T>`、`XxxVO`、`PageResult<T>`，禁止另起一套字段名。
3. **请求统一出口**：一律走共享 `request` 封装，页面拿到的已是 `data`。
4. **样式可换肤**：颜色与关键视觉走 CSS 变量，服务代理换肤。
5. **租户隔离以后端为准**：前端不伪造 `tenantId` 绕过隔离。

---

## 2. Monorepo 目录结构

```
liuhecai/
├── apps/
│   ├── web/                 # 站点前台（Host 识别租户）
│   ├── admin/               # 超管后台
│   └── agent/               # 代理运营后台
├── packages/
│   └── shared/              # Result 类型、request 封装、公共 utils、共享 VO 类型
└── docs/
    ├── backend-coding-standards.md
    └── frontend-coding-standards.md
```

### 2.1 单个 Nuxt 应用内部

```
apps/web/   （admin / agent 同构）
├── pages/                   # 路由页面（薄）
├── components/              # UI 组件（可按业务分子目录）
│   ├── lottery/
│   └── common/
├── composables/             # useXxx 可复用逻辑
├── api/                     # 按资源拆分的 API 函数
├── stores/                  # Pinia
├── assets/
│   └── styles/
│       ├── variables.scss   # 全局 SCSS / 设计 token
│       ├── theme.scss       # 主题 / CSS 变量
│       └── main.scss
├── layouts/
├── middleware/
├── plugins/                 # 如 request 插件、错误提示
├── utils/
├── types/                   # 本端特有类型（能共享的放 packages/shared）
├── nuxt.config.ts
└── package.json
```

### 2.2 packages/shared 建议内容

```
packages/shared/
├── types/
│   ├── result.ts            # Result、PageResult
│   └── vo/                  # UserVO 等与后端对齐的类型
├── request/
│   └── client.ts            # ofetch 封装、解包 Result
└── utils/
```

三端通过 workspace 依赖引用 `@liuhecai/shared`（包名可按仓库最终定名调整，全项目保持一致）。

---

## 3. 分层职责

| 层 | 允许 | 禁止 |
|---|---|---|
| pages | 调 api/composable、拼组件、极少 UI 状态 | 长请求逻辑、手写 `res.code` 解包、复杂业务分支堆砌 |
| components | 展示与交互；props / emit | 默认直接调后端 API |
| api/* | 定义接口函数，返回已解包的 `data` | 负责路由跳转、弹 Toast（除非项目约定在拦截器统一处理） |
| composables | 复用状态与流程（倒计时、表单流等） | 变成巨型上帝模块 |
| stores | 登录态、token、租户主题等跨页状态 | 缓存所有列表/详情接口 |
| shared | 类型、request、真正跨端工具 | 某端专用页面 UI |

### 3.1 页面必须很薄

```vue
<script setup lang="ts">
import { fetchUser } from '~/api/user'

const route = useRoute()
const id = computed(() => Number(route.params.id))

const { data, pending, error } = await useAsyncData(
  () => `user-${id.value}`,
  () => fetchUser(id.value),
)
</script>

<template>
  <div>
    <p v-if="pending">加载中…</p>
    <p v-else-if="error">加载失败</p>
    <UserProfile v-else-if="data" :user="data" />
  </div>
</template>
```

### 3.2 API 模块

```ts
// apps/web/api/user.ts
import { request } from '@liuhecai/shared/request'
import type { UserVO, PageResult } from '@liuhecai/shared/types'
import type { UserCreateRequest, UserQuery } from '~/types/user'

export function fetchUser(id: number) {
  return request<UserVO>(`/api/users/${id}`)
}

export function fetchUserPage(query: UserQuery) {
  return request<PageResult<UserVO>>('/api/users', {
    query: query as Record<string, unknown>,
  })
}

export function createUser(body: UserCreateRequest) {
  return request<UserVO>('/api/users', { method: 'POST', body })
}
```

### 3.3 组件

- 展示组件：只通过 props 收数据，事件用 emit。
- 需要数据时由页面/composable 注入，**不要**在子组件里随便 `$fetch`。
- 文件名 PascalCase：`DrawCountdown.vue`、`PostList.vue`。

---

## 4. 类型与后端对齐

与后端规范一致：

```ts
// packages/shared/types/result.ts
export interface Result<T> {
  code: number
  message: string
  data: T
}

export interface PageResult<T> {
  total: number
  page: number
  size: number
  records: T[]
}
```

```ts
// packages/shared/types/vo/user.ts
export interface UserVO {
  id: number
  username: string
  enabled: number
  createdAt: string
}
```

**约定**

1. 字段名与后端 JSON 保持一致（注意时间字段格式与 Jackson 配置统一）。
2. 入参类型可用本端 `types` 下的 `XxxCreateRequest` / `XxxQuery`，含义与后端 DTO 对齐。
3. 禁止用 `any` 偷懒；实在过渡期用 `unknown` 再收窄。
4. 页面 / 组件只消费 VO，不把「带 password 的原始对象」传来传去。

---

## 5. 请求封装（解包 Result）

统一封装 `$fetch` / `ofetch`：

```ts
// packages/shared/request/client.ts（示意）
import type { Result } from '../types/result'

export class ApiError extends Error {
  code: number
  constructor(code: number, message: string) {
    super(message)
    this.code = code
  }
}

type RequestOptions = {
  method?: string
  body?: unknown
  query?: Record<string, unknown>
  headers?: Record<string, string>
}

export async function request<T>(url: string, options: RequestOptions = {}): Promise<T> {
  const res = await $fetch<Result<T>>(url, {
    baseURL: useRuntimeConfig().public.apiBase,
    method: options.method,
    body: options.body,
    query: options.query,
    headers: options.headers,
  })

  if (res.code !== 0) {
    throw new ApiError(res.code, res.message || '请求失败')
  }
  return res.data
}
```

**约定**

| 项 | 做法 |
|---|---|
| 成功 | `code === 0`，返回 `data` |
| 业务失败 | 抛 `ApiError(code, message)` |
| 鉴权 | 在封装或插件里挂 `Authorization`；401 统一登出/跳转登录 |
| 调用方 | 拿到的已是 `T`，**不要**再判断 `res.code` |
| 禁止 | 页面/组件内大量裸 `$fetch` 且各自解包 |

首屏数据优先：

```ts
const { data } = await useAsyncData('draw-latest', () => fetchLatestDraw())
```

按钮点击等交互：

```ts
async function onSubmit() {
  try {
    await createUser(form)
    // 成功提示 / 跳转
  } catch (e) {
    // 特殊分支；通用错误可由插件统一 toast
  }
}
```

---

## 6. 状态管理（Pinia）

**适合进 Store**

- 登录用户信息、token
- 租户公开配置 / 主题 CSS 变量来源
- 全局 UI 开关（如公告条）

**不适合进 Store**

- 一次性列表、详情页数据
- 表单临时草稿（可用页内 `ref` 或 composable）

```ts
// stores/user.ts
export const useUserStore = defineStore('user', () => {
  const token = ref<string | null>(null)
  const profile = ref<UserVO | null>(null)

  function setSession(t: string, user: UserVO) {
    token.value = t
    profile.value = user
  }

  function logout() {
    token.value = null
    profile.value = null
  }

  return { token, profile, setSession, logout }
})
```

命名统一：`useUserStore`、`useTenantStore`。

---

## 7. 样式与换肤

1. 使用 **SCSS + CSS 变量**，不引入 Tailwind。
2. 设计 token 集中在 `assets/styles/variables.scss` / `theme.scss`。
3. 组件样式默认 `scoped`；避免滥用 `:deep()`。
4. **换肤**：代理后台配置主色、背景等 → 前台拉取后写入：

```ts
function applyTheme(vars: Record<string, string>) {
  const root = document.documentElement
  Object.entries(vars).forEach(([key, value]) => {
    root.style.setProperty(`--${key}`, value)
  })
}
```

```scss
.btn-primary {
  background: var(--color-primary);
  color: var(--color-on-primary);
}
```

5. 禁止在大量组件里写死 `#ff0000` 这类无法换肤的色值（装饰性极少数例外需注释说明）。
6. 前台以还原站点结构为目标，样式以 SCSS + CSS 变量为主。超管 / 代理后台已使用 **Element Plus**，页面布局与表单组件按该库约定编写；换肤仍以 CSS 变量为准。

---

## 8. 多租户（前端侧）

| 端 | 约定 |
|---|---|
| web | 用户通过站点域名访问；Host 由后端识别租户。前端拉公开配置（站名、皮肤、公告）注入主题与 `useTenantStore` |
| agent | 登录态绑定本租户；请求不随意改 `tenantId` |
| admin | 可跨租户；操作某站时显式选择目标租户（与后端超管接口约定一致） |

**硬规则**

1. 前端**不要**自己编造 `tenantId` 塞进普通业务请求来「切换站点」。
2. 数据隔离以后端 `tenant_id` / TenantLine 为准；前端只做展示与鉴权体验。
3. SSR 场景注意：主题/租户配置在服务端与客户端一致，避免闪烁（可用 `useAsyncData` 首屏拉取）。

---

## 9. 命名与路由约定

| 对象 | 约定 | 示例 |
|---|---|---|
| 页面路由 | 文件系统路由；目录 kebab-case | `pages/lottery/index.vue` → `/lottery` |
| 组件文件 | PascalCase | `DrawCountdown.vue` |
| composable | `use` 前缀 | `useDrawCountdown.ts` |
| api 函数 | 动词开头 | `fetchUser`、`createPost`、`updateTheme` |
| store | `useXxxStore` | `useTenantStore` |
| 类型 | 与后端一致 | `UserVO`、`LotteryDrawVO` |
| CSS 变量 | `--color-*`、`--font-*` | `--color-primary` |

三端路由前缀可自行划分（如 admin 全站在域名根路径），但 **API 路径** 与后端 `/api/...` 保持一致。

---

## 10. 三端差异摘要

| 主题 | web | admin | agent |
|---|---|---|---|
| 用户 | 注册登录、看开奖/资料 | 开站、域名、代理账号、跨站用户只读运维 | 换肤、公告、资料管理、加减币、充值确认、开奖拉取/本站覆盖 |
| 租户 | Host → 后端识别 | 显式选站 / 跨租户 | 固定本站 |
| 换肤 | 消费 CSS 变量 | 不做主配置 | 主配置入口 |
| 权限 | 登录用户 | 超管角色 | 代理运营角色 |

公共类型与 `request` 必须复用 `packages/shared`，避免三端复制解包逻辑。

---

## 11. 禁止事项清单

1. 页面内堆砌请求、业务判断、手写 `res.code` 解包。
2. 组件默认直接调用后端（应经 `api/` 或 composable）。
3. 不走统一 `request`，满项目裸 `$fetch` 且各自处理错误。
4. 滥用 `any`；前后端字段名不一致却不说明。
5. 把所有接口数据塞进 Pinia。
6. 颜色/皮肤写死在大量组件中，导致无法换肤。
7. 前端伪造或随意传递 `tenantId` 试图绕过隔离。
8. 在 `packages/shared` 中放入某端专用页面或重度 UI。
9. 使用 Options API 作为新代码默认写法（存量除外，新代码统一 `<script setup>`）。
10. 引入 Tailwind 或把重型 UI 库当作前台主方案（与本规范冲突时需先修订文档）。

---

## 12. 提交前自检（Checklist）

- [ ] 页面是否足够薄？复杂逻辑是否进了 composable/api？
- [ ] 是否通过统一 `request` 拿到 `data`，而非手解 `Result`？
- [ ] 类型是否使用 `XxxVO` / `PageResult`，与后端一致？
- [ ] 跨页状态才进 Pinia？列表详情是否没乱塞 store？
- [ ] 主题色是否走 CSS 变量？
- [ ] 是否未在前端随意传 `tenantId` 绕过隔离？
- [ ] 可共享代码是否放在 `packages/shared` 而非复制三份？

---

## 修订记录

| 日期 | 说明 |
|---|---|
| 2026-07-22 | 初版：Nuxt3+TS、三端 monorepo、Result 解包、SCSS 换肤、租户约定 |
