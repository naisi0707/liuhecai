# Entry Lines Config Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let super-admins configure per-ENTRY-domain camouflage “线路” buttons (label/color/count/target tenant); web ENTRY pages render those lines and `/goto` whitelists their forum hosts.

**Architecture:** New table `entry_lines` keyed by `domains.id` (ENTRY only). Admin CRUD replaces the full line list for a domain. `GET /api/tenant/current` attaches `entryLines` when `domainRole=ENTRY`. Web replaces static `nav.html` iframe with `CamouflageLineNav.vue`. `TenantContext` gains `domainId` so lines resolve by the current Host’s domain row.

**Tech Stack:** Spring Boot 3 + MyBatis-Plus, Nuxt 3 (web/admin), `packages/shared` types, Element Plus admin UI.

**Spec:** [docs/superpowers/specs/2026-07-24-entry-lines-config-design.md](../specs/2026-07-24-entry-lines-config-design.md)

---

## File map

| Path | Responsibility |
|---|---|
| `apps/api/.../db/schema-m15-entry-lines.sql` | CREATE TABLE + optional seed notes |
| `apps/api/.../config/SchemaMigrateRunner.java` | Idempotent CREATE TABLE |
| `apps/api/.../entity/EntryLine.java` | MP entity |
| `apps/api/.../mapper/EntryLineMapper.java` | BaseMapper |
| `apps/api/.../vo/EntryLineAdminVO.java` | Admin list item |
| `apps/api/.../vo/EntryLinePublicVO.java` | Public `{label,color,forumUrl}` |
| `apps/api/.../dto/EntryLinesSaveRequest.java` | PUT body |
| `apps/api/.../service/EntryLineAdminService.java` (+ impl) | Admin get/replace + default seed |
| `apps/api/.../controller/EntryLineAdminController.java` | `/api/admin/entry-domains/{id}/lines` |
| `apps/api/.../tenant/TenantContext.java` | Add `domainId` |
| `apps/api/.../tenant/TenantResolveFilter.java` | Set domain id |
| `apps/api/.../vo/TenantVO.java` | `List<EntryLinePublicVO> entryLines` |
| `apps/api/.../service/impl/TenantQueryServiceImpl.java` | Fill entryLines; forum URL builder |
| `apps/api/.../service/impl/TenantAdminServiceImpl.java` | On ENTRY bind → seed defaults |
| `packages/shared/src/types/vo/tenant.ts` | `EntryLinePublicVO` + field on tenant |
| `packages/shared/src/index.ts` | Re-export type |
| `apps/admin/pages/domains.vue` | ENTRY “配置线路” drawer UI |
| `apps/web/components/CamouflageLineNav.vue` | Render buttons + goto |
| `apps/web/components/CamouflageShop.vue` | Use Vue nav; drop iframe |
| `apps/web/pages/goto.vue` | Whitelist from `entryLines` hosts |
| `apps/web/composables/useForumEntryUrl.ts` | Export `buildForumUrlForHost(host)` helper (or share logic) |
| `docs/entry-camouflage.md` | Document configurable lines |

---

### Task 1: Schema + Entity + Mapper

**Files:**
- Create: `apps/api/src/main/resources/db/schema-m15-entry-lines.sql`
- Create: `apps/api/src/main/java/com/liuhecai/entity/EntryLine.java`
- Create: `apps/api/src/main/java/com/liuhecai/mapper/EntryLineMapper.java`
- Modify: `apps/api/src/main/java/com/liuhecai/config/SchemaMigrateRunner.java`

- [ ] **Step 1: Add SQL file**

```sql
-- M15: entry_lines — per ENTRY domain camouflage route buttons
CREATE TABLE IF NOT EXISTS entry_lines (
    id                BIGINT PRIMARY KEY,
    entry_domain_id   BIGINT       NOT NULL COMMENT 'domains.id role=ENTRY',
    sort_order        INT          NOT NULL DEFAULT 0,
    label             VARCHAR(64)  NOT NULL,
    color             VARCHAR(16)  NOT NULL DEFAULT '#c62828',
    target_tenant_id  BIGINT       NOT NULL,
    status            TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_entry_lines_domain (entry_domain_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [ ] **Step 2: Add entity + mapper**

```java
// EntryLine.java
@Data
@TableName("entry_lines")
public class EntryLine {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long entryDomainId;
    private Integer sortOrder;
    private String label;
    private String color;
    private Long targetTenantId;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

// EntryLineMapper.java
@Mapper
public interface EntryLineMapper extends BaseMapper<EntryLine> {}
```

- [ ] **Step 3: Idempotent migrate in SchemaMigrateRunner**

Add constant + call in `run()`:

```java
private static final String CREATE_ENTRY_LINES = """
        CREATE TABLE IF NOT EXISTS entry_lines (
          id BIGINT PRIMARY KEY,
          entry_domain_id BIGINT NOT NULL,
          sort_order INT NOT NULL DEFAULT 0,
          label VARCHAR(64) NOT NULL,
          color VARCHAR(16) NOT NULL DEFAULT '#c62828',
          target_tenant_id BIGINT NOT NULL,
          status TINYINT NOT NULL DEFAULT 1,
          created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
          updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
          KEY idx_entry_lines_domain (entry_domain_id, sort_order)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """;
// in run(): executeIdempotent(CREATE_ENTRY_LINES);
```

Also broaden `executeIdempotent` catch so “table already exists” is skipped (same as duplicate column), or rely on `IF NOT EXISTS`.

- [ ] **Step 4: Commit**

```bash
git add apps/api/src/main/resources/db/schema-m15-entry-lines.sql \
  apps/api/src/main/java/com/liuhecai/entity/EntryLine.java \
  apps/api/src/main/java/com/liuhecai/mapper/EntryLineMapper.java \
  apps/api/src/main/java/com/liuhecai/config/SchemaMigrateRunner.java
git commit -m "feat(api): add entry_lines schema and entity"
```

---

### Task 2: TenantContext domainId + resolve filter

**Files:**
- Modify: `apps/api/src/main/java/com/liuhecai/tenant/TenantContext.java`
- Modify: `apps/api/src/main/java/com/liuhecai/tenant/TenantResolveFilter.java`

- [ ] **Step 1: Add domainId holder**

```java
private static final ThreadLocal<Long> DOMAIN_ID_HOLDER = new ThreadLocal<>();

public static void setDomainId(Long domainId) { DOMAIN_ID_HOLDER.set(domainId); }
public static Long getDomainId() { return DOMAIN_ID_HOLDER.get(); }
// clear(): DOMAIN_ID_HOLDER.remove();
```

- [ ] **Step 2: Set domain id when domain row is found**

In `TenantResolveFilter`, after loading `Domain domain`, call `TenantContext.setDomainId(domain.getId())` alongside role/host/tenant.

- [ ] **Step 3: Commit**

```bash
git add apps/api/src/main/java/com/liuhecai/tenant/TenantContext.java \
  apps/api/src/main/java/com/liuhecai/tenant/TenantResolveFilter.java
git commit -m "feat(api): expose current domainId in TenantContext"
```

---

### Task 3: Admin EntryLine service + controller

**Files:**
- Create: `apps/api/src/main/java/com/liuhecai/vo/EntryLineAdminVO.java`
- Create: `apps/api/src/main/java/com/liuhecai/dto/EntryLineItemRequest.java`
- Create: `apps/api/src/main/java/com/liuhecai/dto/EntryLinesSaveRequest.java`
- Create: `apps/api/src/main/java/com/liuhecai/service/EntryLineAdminService.java`
- Create: `apps/api/src/main/java/com/liuhecai/service/impl/EntryLineAdminServiceImpl.java`
- Create: `apps/api/src/main/java/com/liuhecai/controller/EntryLineAdminController.java`
- Modify: `apps/api/src/main/java/com/liuhecai/service/impl/TenantAdminServiceImpl.java` (seed on ENTRY bind)

- [ ] **Step 1: DTO/VO**

```java
@Data
public class EntryLineAdminVO {
    private Long id;
    private Integer sortOrder;
    private String label;
    private String color;
    private Long targetTenantId;
    private String targetTenantName;
    private String targetForumHost;
    private Integer status;
}

@Data
public class EntryLineItemRequest {
    private Long id; // optional, ignored on full replace
    @NotNull private Integer sortOrder;
    @NotBlank @Size(max = 64) private String label;
    @NotBlank @Size(max = 16) private String color;
    @NotNull private Long targetTenantId;
    @NotNull private Integer status;
}

@Data
public class EntryLinesSaveRequest {
    @NotNull @Valid
    private List<EntryLineItemRequest> lines;
}
```

- [ ] **Step 2: Service contract**

```java
public interface EntryLineAdminService {
    List<EntryLineAdminVO> listByDomainId(Long domainId);
    List<EntryLineAdminVO> replaceLines(Long domainId, EntryLinesSaveRequest request);
    void seedDefaultsIfEmpty(Long entryDomainId, Long ownerTenantId);
}
```

Impl rules:

1. `requireEntryDomain(domainId)` — domain exists, `status=1`, `role=ENTRY`.
2. `list` — order by `sort_order`, join tenant name + resolve FORUM host via same logic as `resolveForumHost`.
3. `replaceLines` — `@Transactional`; for each item validate target tenant enabled + has FORUM host; `delete` all rows for `entry_domain_id` then `insert` new rows (assign new ids). Empty `lines` allowed (clears config).
4. `seedDefaultsIfEmpty` — if count==0, insert 5 default labels/colors with `target_tenant_id=ownerTenantId`.

Default rows (sort 1..5):

| label | color |
|---|---|
| 电信临时线路 | `#c62828` |
| 移动临时线路 | `#1565c0` |
| 联通临时线路 | `#2e7d32` |
| 广电临时线路 | `#6a1b9a` |
| 澳门直达专线 | `#ef6c00` |

- [ ] **Step 3: Controller**

```java
@RestController
@RequestMapping("/api/admin/entry-domains")
@RequiredArgsConstructor
@Validated
public class EntryLineAdminController {
    private final EntryLineAdminService entryLineAdminService;

    @GetMapping("/{domainId}/lines")
    public Result<List<EntryLineAdminVO>> list(@PathVariable Long domainId) {
        return Result.ok(entryLineAdminService.listByDomainId(domainId));
    }

    @PutMapping("/{domainId}/lines")
    public Result<List<EntryLineAdminVO>> save(@PathVariable Long domainId,
                                               @Valid @RequestBody EntryLinesSaveRequest request) {
        return Result.ok(entryLineAdminService.replaceLines(domainId, request));
    }
}
```

Ensure admin auth interceptor already covers `/api/admin/**` (same as other admin controllers).

- [ ] **Step 4: Seed on ENTRY bind**

In `TenantAdminServiceImpl.bindDomain`, after inserting ENTRY domain, call `entryLineAdminService.seedDefaultsIfEmpty(domain.getId(), tenantId)`.

- [ ] **Step 5: Manual API smoke (local)**

```bash
# login admin, then:
curl -s -H "Authorization: Bearer $TOKEN" http://127.0.0.1:8080/api/admin/entry-domains/2102/lines
```

Expected: `[]` or defaults after seed; PUT with 2 lines returns those 2.

- [ ] **Step 6: Commit**

```bash
git add apps/api/src/main/java/com/liuhecai/vo/EntryLineAdminVO.java \
  apps/api/src/main/java/com/liuhecai/dto/EntryLineItemRequest.java \
  apps/api/src/main/java/com/liuhecai/dto/EntryLinesSaveRequest.java \
  apps/api/src/main/java/com/liuhecai/service/EntryLineAdminService.java \
  apps/api/src/main/java/com/liuhecai/service/impl/EntryLineAdminServiceImpl.java \
  apps/api/src/main/java/com/liuhecai/controller/EntryLineAdminController.java \
  apps/api/src/main/java/com/liuhecai/service/impl/TenantAdminServiceImpl.java
git commit -m "feat(api): admin CRUD for ENTRY domain lines"
```

---

### Task 4: Public TenantVO.entryLines + forum URL builder

**Files:**
- Create: `apps/api/src/main/java/com/liuhecai/vo/EntryLinePublicVO.java`
- Modify: `apps/api/src/main/java/com/liuhecai/vo/TenantVO.java`
- Modify: `apps/api/src/main/java/com/liuhecai/service/impl/TenantQueryServiceImpl.java`
- Modify: `packages/shared/src/types/vo/tenant.ts`
- Modify: `packages/shared/src/index.ts` (if types re-exported via types barrel)

- [ ] **Step 1: Public VO + TenantVO field**

```java
@Data
public class EntryLinePublicVO {
    private String label;
    private String color;
    private String forumUrl;
}
// TenantVO: private List<EntryLinePublicVO> entryLines;
```

- [ ] **Step 2: Fill in getCurrentTenant**

When `"ENTRY".equalsIgnoreCase(vo.getDomainRole())` and `TenantContext.getDomainId() != null`:

1. Load enabled lines for domain ordered by sort.
2. For each, resolve target tenant FORUM host; skip if missing/disabled.
3. Build `forumUrl`:
   - If request Host is local (`127.0.0.1`/`localhost`) OR forum host is local/`*.local` → `{scheme}://{requestHost}/?host={forumHost}` (mirror `useForumEntryUrl`).
   - Else → `{scheme}://{forumHost}/` (omit default ports).
4. Scheme from `X-Forwarded-Proto` if present else `http`.

Inject `EntryLineMapper` (+ reuse DomainMapper). Extract `resolveForumHost(tenantId)` to package-visible helper or shared private method used by admin service too (prefer a small `ForumHostResolver` component to avoid duplication — create `com.liuhecai.service.ForumHostResolver` if copy-paste would exceed ~15 lines twice).

- [ ] **Step 3: Shared TS types**

```ts
export interface EntryLinePublicVO {
  label: string
  color: string
  forumUrl: string
}

export interface TenantPublicVO {
  // ...existing
  entryLines?: EntryLinePublicVO[]
}
```

Export from `packages/shared/src/types` / `index.ts` as existing pattern.

- [ ] **Step 4: Verify**

```bash
curl -s -H "X-Forwarded-Host: entry.127.0.0.1" http://127.0.0.1:8080/api/tenant/current
```

Expected JSON includes `"domainRole":"ENTRY"` and `"entryLines":[{...}]` after defaults seeded.

- [ ] **Step 5: Commit**

```bash
git add apps/api/src/main/java/com/liuhecai/vo/EntryLinePublicVO.java \
  apps/api/src/main/java/com/liuhecai/vo/TenantVO.java \
  apps/api/src/main/java/com/liuhecai/service/impl/TenantQueryServiceImpl.java \
  packages/shared/src/types/vo/tenant.ts packages/shared/src/index.ts
git commit -m "feat(api): attach entryLines on ENTRY tenant current"
```

---

### Task 5: Admin UI — configure lines drawer

**Files:**
- Modify: `apps/admin/pages/domains.vue`

- [ ] **Step 1: State + API helpers**

Add:

```ts
const lineDrawer = ref(false)
const lineDomain = ref<{ id: number|string, host: string } | null>(null)
const lines = ref<Array<{
  sortOrder: number
  label: string
  color: string
  targetTenantId: number | string | null
  status: number
}>>([])
const linesLoading = ref(false)
const linesSaving = ref(false)

async function openLines(domain: { id: number|string, host: string, role?: string }) {
  if ((domain.role || 'FORUM') !== 'ENTRY') return
  lineDomain.value = domain
  lineDrawer.value = true
  linesLoading.value = true
  try {
    lines.value = await api(`/api/admin/entry-domains/${domain.id}/lines`)
    if (!lines.value.length) {
      lines.value = defaultFive(bindTenantId.value || tenants.value[0]?.id)
    }
  } finally {
    linesLoading.value = false
  }
}

function defaultFive(tenantId: number | string | null) {
  const colors = ['#c62828','#1565c0','#2e7d32','#6a1b9a','#ef6c00']
  const labels = ['电信临时线路','移动临时线路','联通临时线路','广电临时线路','澳门直达专线']
  return labels.map((label, i) => ({
    sortOrder: i + 1, label, color: colors[i], targetTenantId: tenantId, status: 1,
  }))
}

async function saveLines() {
  if (!lineDomain.value) return
  linesSaving.value = true
  try {
    lines.value = await api(`/api/admin/entry-domains/${lineDomain.value.id}/lines`, {
      method: 'PUT',
      body: { lines: lines.value.map((l, idx) => ({ ...l, sortOrder: idx + 1 })) },
    })
    ElMessage.success('线路已保存')
    lineDrawer.value = false
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败')
  } finally {
    linesSaving.value = false
  }
}
```

- [ ] **Step 2: Table column action + drawer**

In domains table for selected tenant, add column:

```vue
<el-table-column label="操作" width="120">
  <template #default="{ row }">
    <el-button v-if="(row.role || 'FORUM') === 'ENTRY'" link type="primary" @click="openLines(row)">
      配置线路
    </el-button>
  </template>
</el-table-column>
```

Drawer: editable rows with `el-input` (label), `el-color-picker` or text (color), `el-select` (tenants), switch status, delete, “新增线路”, “恢复默认五条”, “保存”.

- [ ] **Step 3: Manual UI check**

Run `pnpm dev:admin`, login admin, open domains, ENTRY row → 配置线路 → change one target tenant → save → GET API reflects change.

- [ ] **Step 4: Commit**

```bash
git add apps/admin/pages/domains.vue
git commit -m "feat(admin): ENTRY domain line configuration UI"
```

---

### Task 6: Web CamouflageLineNav + goto whitelist

**Files:**
- Create: `apps/web/components/CamouflageLineNav.vue`
- Modify: `apps/web/components/CamouflageShop.vue`
- Modify: `apps/web/pages/goto.vue`
- Modify: `docs/entry-camouflage.md`

- [ ] **Step 1: CamouflageLineNav.vue**

```vue
<script setup lang="ts">
import { encodeGotoUrl, type EntryLinePublicVO } from '@liuhecai/shared'

const { tenant } = useTenant()
const lines = computed(() => (tenant.value?.entryLines || []).filter(l => l.forumUrl))

function go(line: EntryLinePublicVO) {
  const u = encodeGotoUrl(line.forumUrl)
  navigateTo({ path: '/goto', query: { u } })
}
</script>

<template>
  <div v-if="lines.length" class="line-nav">
    <h1>请选择线路进入</h1>
    <button
      v-for="(line, i) in lines"
      :key="i"
      type="button"
      class="line-nav__btn"
      :style="{ background: line.color || '#c62828' }"
      @click="go(line)"
    >
      {{ line.label }}
    </button>
    <p class="line-nav__tip">注：如无法进入到主页，请更换其他浏览器。推荐使用 UC、手机自带、谷歌浏览器进行访问。</p>
  </div>
</template>
```

Style to match current `nav.html` (max-width ~420px, fixed overlay positioning stays in parent).

- [ ] **Step 2: CamouflageShop — replace iframe**

Remove `navSrc` / iframe; render:

```vue
<CamouflageLineNav class="camo-shop__nav-frame" />
```

Keep `.camo-shop__nav-frame` positioning on the component root (or wrap).

- [ ] **Step 3: goto.vue whitelist**

```ts
const allowedHosts = new Set<string>()
for (const line of tenant.value?.entryLines || []) {
  try {
    const u = new URL(line.forumUrl)
    allowedHosts.add(u.hostname.toLowerCase())
    const h = u.searchParams.get('host')
    if (h) allowedHosts.add(h.toLowerCase())
  } catch { /* skip */ }
}
const forumHost = (tenant.value?.forumHost || '').toLowerCase()
if (forumHost) allowedHosts.add(forumHost)
;['127.0.0.1', 'localhost'].forEach(h => allowedHosts.add(h))

const ok = isAllowedGotoTarget(decoded, [...allowedHosts])
// keep ?host= fallback: host param in allowedHosts
```

- [ ] **Step 4: Docs**

Update `docs/entry-camouflage.md`: lines are admin-configurable per ENTRY domain; mention admin path.

- [ ] **Step 5: Manual E2E**

1. ENTRY host shows buttons with configured labels/colors.
2. Click line A → lands on tenant A forum; line B → tenant B.
3. Craft `/goto?u=` to unrelated host → “跳转目标不在白名单”.
4. FORUM host still normal forum (no camouflage).

- [ ] **Step 6: Commit**

```bash
git add apps/web/components/CamouflageLineNav.vue \
  apps/web/components/CamouflageShop.vue \
  apps/web/pages/goto.vue \
  docs/entry-camouflage.md
git commit -m "feat(web): render configurable ENTRY lines and tighten goto allowlist"
```

---

### Task 7: Production seed for 157465.com + smoke

**Files:**
- Create: `apps/api/src/main/resources/db/seed-m15-entry-lines-prod.sql` (optional helper)
- Or one-shot SQL via deploy script / mysql on server

- [ ] **Step 1: Seed SQL**

```sql
-- After entry_lines table exists; domain id for 157465.com from domains table
SET @entry_id = (SELECT id FROM domains WHERE host='157465.com' AND role='ENTRY' LIMIT 1);
DELETE FROM entry_lines WHERE entry_domain_id = @entry_id;
INSERT INTO entry_lines (id, entry_domain_id, sort_order, label, color, target_tenant_id, status) VALUES
(5101, @entry_id, 1, '电信临时线路', '#c62828', 1001, 1),
(5102, @entry_id, 2, '移动临时线路', '#1565c0', 1003, 1),
(5103, @entry_id, 3, '联通临时线路', '#2e7d32', 1004, 1),
(5104, @entry_id, 4, '广电临时线路', '#6a1b9a', 1002, 1),
(5105, @entry_id, 5, '澳门直达专线', '#ef6c00', 1005, 1);
```

Adjust tenant ids to match production (`1001` 刘伯温, `1003` 神算子, etc.).

- [ ] **Step 2: Deploy**

Rebuild API jar + web `.output`, upload/restart (reuse `deploy/scripts` patterns). Apply seed on server MySQL. Verify:

```bash
curl -sS https://157465.com/api/tenant/current | jq '.data.entryLines'
```

- [ ] **Step 3: Commit seed file**

```bash
git add apps/api/src/main/resources/db/seed-m15-entry-lines-prod.sql
git commit -m "chore(db): prod seed for 157465.com entry lines"
```

---

## Spec coverage check

| Spec section | Task |
|---|---|
| `entry_lines` table | Task 1 |
| Admin GET/PUT | Task 3 |
| Seed defaults on ENTRY bind | Task 3 |
| TenantVO.entryLines + forumUrl | Task 4 |
| Admin UI | Task 5 |
| Vue line nav / drop static iframe | Task 6 |
| `/goto` multi-host whitelist | Task 6 |
| Prod 157465 seed | Task 7 |

## Placeholder scan

No TBD steps; concrete SQL/Java/Vue/commands included.

## Type consistency

- `EntryLinePublicVO`: `label`, `color`, `forumUrl`
- `EntryLineAdminVO`: `id`, `sortOrder`, `label`, `color`, `targetTenantId`, `targetTenantName`, `targetForumHost`, `status`
- Admin path: `/api/admin/entry-domains/{domainId}/lines`
