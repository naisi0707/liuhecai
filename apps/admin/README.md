# 超管后台 (`apps/admin`)

端口：`http://127.0.0.1:3001`（根目录 `pnpm dev:admin`）

## 菜单地图

- **总览**：跨站 KPI
- **用户与代理**：代理管理、用户管理、操作审计
- **站点**：站点列表、域名绑定（开站创建从站点列表 / 总览进入）

API 基址见 `nuxt.config.ts` → `runtimeConfig.public.apiBase`。联调与账号见仓库根目录 [README.md](../../README.md)。
