# 六合彩多租户资料论坛

结构对齐「刘伯温」类站点；技术栈见 `docs/`。按模块推进，用户验收通过后再开下一模块。

## 环境要求（本机已有即可，不使用 Docker）

- JDK 17+
- Maven 3.9+
- Node.js 20+ / pnpm 9+
- MySQL 8（M2 起启用）
- Redis（按模块需要启用）

## 目录

```
apps/api      Spring Boot API :8080
apps/web      站点前台 Nuxt   :3000
apps/admin    超管后台 Nuxt   :3001
apps/agent    代理后台 Nuxt   :3002
packages/shared  Result / request 共享包
```

## 启动

### 1. 安装前端依赖

```bash
pnpm install
```

### 2. 启动 API（M1 不连库）

```bash
cd apps/api
mvn spring-boot:run
```

健康检查：

```bash
curl http://127.0.0.1:8080/api/health
```

期望类似：

```json
{"code":0,"message":"ok","data":{"status":"UP"}}
```

### 3. 启动前端（各开一个终端）

```bash
pnpm dev:web    # http://127.0.0.1:3000
pnpm dev:admin  # http://127.0.0.1:3001
pnpm dev:agent  # http://127.0.0.1:3002
```

## M2 本地联调（已导入种子后）

```bash
curl http://127.0.0.1:8080/api/health
curl -H "X-Forwarded-Host: lbw.local" http://127.0.0.1:8080/api/tenant/current
curl -H "X-Forwarded-Host: zzws.local" http://127.0.0.1:8080/api/tenant/current
curl -H "X-Forwarded-Host: lbw.local" http://127.0.0.1:8080/api/demo-notes
curl -H "X-Forwarded-Host: zzws.local" http://127.0.0.1:8080/api/demo-notes
curl -H "X-Forwarded-Host: unknown.test" http://127.0.0.1:8080/api/tenant/current
```

数据库密码写在 `apps/api/src/main/resources/application-local.yml`（已 gitignore，勿提交）。

## M3 演示账号

| 端 | 地址 | 账号 | 密码 |
|----|------|------|------|
| 超管 | http://localhost:3001 | admin | admin123 |
| 代理 | http://localhost:3002 | agent_a | agent123 |
| 前台 | http://localhost:3000 | 自行注册 | ≥6 位 |

## M6 开奖联调

```bash
# 导入表
mysql -uroot -p liuhecai < apps/api/src/main/resources/db/schema-m6.sql

# 代理端：登录 →「开奖管理」→ 手动触发拉取 / 手工覆盖
# 前台：新澳门 / 香港 / 老澳门 Tab + 倒计时

python scripts/m6_verify.py
```

公开源 URL 可在 `application.yml` 的 `liuhecai.draw.http-url` 配置；留空则使用 mock。

## M7 资料帖联调

```bash
# 导入表（version 列若已存在可跳过 ALTER）
mysql -uroot -p liuhecai < apps/api/src/main/resources/db/schema-m7.sql

# 代理：资料审帖 → 创建上架
# 代理：用户管理 → 用户详情 → 加减币
# 前台：登录 → 查看锁定正文 → 购买解锁

python scripts/m7_verify.py
```

## M8 充值联调

```bash
mysql -uroot -p liuhecai < apps/api/src/main/resources/db/schema-m8.sql

# 前台：登录 → 充值 → 提交申请
# 代理：充值确认 → 确认加币 / 拒绝

python scripts/m8_verify.py
```

## M9 结构与样式

- 前台：首页 / 规则 / 充值 / 客服 / 登录注册 主导航；朱红论坛风 + 租户主色
- 超管 / 代理：Element Plus 布局（`:3001` / `:3002`）

```bash
python scripts/m9_verify.py
```

## 文档

- 设计：`docs/superpowers/specs/2026-07-22-liubowen-multitenant-forum-design.md`
- 分模块计划：`docs/superpowers/plans/2026-07-22-liubowen-multitenant-modules.md`
- 后端规范：`docs/backend-coding-standards.md`
- 前端规范：`docs/frontend-coding-standards.md`
- 进度看板：`task_plan.md`

## 全量 E2E（HTTP + DB + 前端）

需先启动 API（`:8080`）与三端（web `:3000` / admin `:3001` / agent `:3002`），MySQL 库 `liuhecai` 可用。

```bash
pip install pymysql playwright
playwright install chromium

# 可选：MYSQL_PASSWORD / E2E_SKIP_UI=1（仅接口+库）
python scripts/e2e_full_verify.py
```

失败截图在 `scripts/e2e-artifacts/`。模块级脚本（`scripts/m*_verify.py`）仍可单独使用。
