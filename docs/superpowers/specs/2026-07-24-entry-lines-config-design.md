# 入口线路可配置（按 ENTRY 域）设计

日期：2026-07-24  
状态：待用户审阅  
范围：超管配置 ENTRY 域名下的「线路选择」按钮；前台按配置跳转到目标租户论坛。

## 1. 背景与目标

当前入口伪装线路页（电信/移动/联通/广电/澳门）五条按钮共用同一 `forumHost`，无法按按钮指向不同品牌站。

**目标**

- 按 **ENTRY 入口域** 独立配置线路列表
- 每条线路绑定 **目标租户**（跳转该租户 FORUM 主域名）
- 文案、颜色、条数可改（增删排序）
- 保持 `/goto?u=` 编码跳转与白名单校验

**非目标**

- 不改假商城视觉稿
- 不在此需求中做代理端配置（仅超管）
- 不引入按爬虫 UA 分流

## 2. 决策摘要

| 项 | 选择 |
|---|---|
| 配置粒度 | 按 ENTRY 域名（`domains.id` where `role=ENTRY`） |
| 跳转目标 | 选租户 → 解析其主 FORUM 域 |
| 文案/颜色/条数 | 均可配置 |
| 存储 | 独立表 `entry_lines`（方案 A） |
| 前台渲染 | Vue 组件替换静态 `nav.html` 按钮逻辑 |

## 3. 数据模型

### 3.1 表 `entry_lines`

| 列 | 类型 | 说明 |
|---|---|---|
| `id` | BIGINT PK | 雪花/assign_id |
| `entry_domain_id` | BIGINT NOT NULL | 指向 `domains.id`，且该域 `role=ENTRY` |
| `sort_order` | INT NOT NULL DEFAULT 0 | 升序展示 |
| `label` | VARCHAR(64) NOT NULL | 按钮文案 |
| `color` | VARCHAR(16) NOT NULL | 背景色，如 `#c62828` |
| `target_tenant_id` | BIGINT NOT NULL | 目标租户 |
| `status` | TINYINT NOT NULL DEFAULT 1 | 1 启用 0 停用 |
| `created_at` / `updated_at` | DATETIME | 常规时间戳 |

索引：`KEY idx_entry_lines_domain (entry_domain_id, sort_order)`。

约束（应用层）：

- `entry_domain_id` 对应域名必须 `role=ENTRY` 且 `status=1`
- `target_tenant_id` 必须存在、`status=1`，且至少有一条启用的 FORUM 域（优先 `is_primary=1`）

### 3.2 默认种子（每个新 ENTRY 域可选）

绑定新 ENTRY 域时，若不存在线路则自动插入 5 条默认：

| sort | label | color | target |
|---|---|---|---|
| 1 | 电信临时线路 | `#c62828` | 该 ENTRY 所属租户（可改） |
| 2 | 移动临时线路 | `#1565c0` | 同上 |
| 3 | 联通临时线路 | `#2e7d32` | 同上 |
| 4 | 广电临时线路 | `#6a1b9a` | 同上 |
| 5 | 澳门直达专线 | `#ef6c00` | 同上 |

生产已有 `157465.com`：迁移时写入上述默认，超管再改为各品牌租户。

## 4. API

### 4.1 超管

- `GET /api/admin/entry-domains/{domainId}/lines` → `List<EntryLineAdminVO>`
- `PUT /api/admin/entry-domains/{domainId}/lines`  
  Body：`{ lines: [{ id?, sortOrder, label, color, targetTenantId, status }] }`  
  语义：**整表替换**该入口域下启用配置（事务内删旧插新，或按 id upsert + 删除缺失 id）

`EntryLineAdminVO`：`id, sortOrder, label, color, targetTenantId, targetTenantName, targetForumHost, status`

### 4.2 前台（ENTRY Host）

扩展现有租户接口或新增轻量接口（二选一，优先扩展 VO 以免多一次请求）：

- `GET /api/tenant/current` 在 `domainRole=ENTRY` 时附带：

```json
"entryLines": [
  {
    "label": "电信临时线路",
    "color": "#c62828",
    "forumUrl": "https://585520.xyz/"
  }
]
```

仅返回 `status=1` 且目标租户/论坛域可用的线路；`forumUrl` 由服务端根据目标租户主 FORUM 域 + 当前请求协议生成（本地 `*.local` / IP 仍可用 `?host=` 演示规则，与现 `useForumEntryUrl` 一致）。

### 4.3 `/goto` 白名单

解码后的目标 Host 合法条件改为：

- 属于**当前 ENTRY 域已配置线路**中任一 `target_tenant_id` 的启用 FORUM 域；或
- 本地演示兼容：`127.0.0.1` / `localhost` / `*.local`

不再仅校验单一 `tenant.forumHost`。

## 5. 超管 UI

页面：[`apps/admin/pages/domains.vue`](../../../apps/admin/pages/domains.vue)（或拆 `entry-lines.vue`，默认扩 domains）

1. 租户域名列表中，`role=ENTRY` 行增加「配置线路」
2. 抽屉/二级卡片：可编辑表格
   - 列：排序、文案、颜色（color picker 或输入）、目标租户（下拉全量启用租户）、启用、删除
   - 操作：新增一行、保存、恢复默认五条（可选）
3. 保存调用 `PUT .../lines`

## 6. 前台 UI

- 新增组件如 `CamouflageLineNav.vue`：读取 `tenant.entryLines`，渲染按钮；点击 `encodeGotoUrl(forumUrl)` → `/goto?u=`
- [`CamouflageShop.vue`](../../../apps/web/components/CamouflageShop.vue) 用该组件替换 iframe + 静态 `nav.html`（`nav.html` 可保留作备份或删除，以组件为准）
- 无可用线路时：不展示叠层，控制台/页内不报错打断假商城

## 7. 分层与规范对齐

- Controller 薄；Service 校验 ENTRY / 目标租户 / 论坛域
- 出参 VO，入参 DTO + Jakarta Validation
- 无 MyBatis XML；Schema 用 `schema-m15-entry-lines.sql` + `SchemaMigrateRunner` 幂等建表
- 共享类型放入 `packages/shared`：`EntryLinePublicVO` 等
- 超管请求走现有 `useAdminAuth().api`

## 8. 验收标准

- [ ] 超管可为 `157465.com` 配置：电信→刘伯温、移动→神算子等不同租户并保存
- [ ] ENTRY 打开线路按钮文案/颜色与配置一致；点击进入对应论坛域
- [ ] `/goto` 拒绝未在该入口线路白名单中的 Host
- [ ] 增删线路、排序后前台立即生效（刷新即可）
- [ ] FORUM 域访问不受影响；未配置线路时假商城仍可打开

## 9. 实现顺序建议

1. Schema + Entity/Mapper + Admin CRUD  
2. Tenant VO 附带 `entryLines` + goto 白名单  
3. Admin UI  
4. Web `CamouflageLineNav` 替换静态页  
5. 生产迁移种子 + 冒烟

## 10. 自检

- 无 TBD/模糊选项；配置粒度、目标选择、文案可变均已闭合  
- 与现有 `domains.role=ENTRY`、`/goto` 编解码、多租户 Host 解析兼容  
- 范围不含代理端、不含假商城重设计  
