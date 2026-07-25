# 生产网址对照表

服务器：`45.152.64.102`  
种子数据见 `apps/api/src/main/resources/db/schema-prod-domains.sql`。

## 前台（用户访问）

| 网址 | 角色 | 对应租户 / 用途 |
|------|------|-----------------|
| https://157465.com | ENTRY 伪装入口 | 假商城 + 线路选择；跳转到各论坛 |
| https://www.157465.com | ENTRY 伪装入口 | 同上（www） |
| https://585520.xyz | FORUM 论坛 | 刘伯温论坛（主域） |
| https://152687.xyz | FORUM 论坛 | 至尊无上论坛 |
| https://785412.xyz | FORUM 论坛 | 神算子论坛 |
| https://658951.xyz | FORUM 论坛 | 招财宝论坛 |
| https://746528.xyz | FORUM 论坛 | 荣华富贵论坛 |

说明：

- **ENTRY**：打开是伪装壳，不是正常论坛首页。
- **FORUM**：各品牌正式论坛站。

## 后台（运营）

| 网址 | 用途 | 演示账号 | 备注 |
|------|------|----------|------|
| https://admin.157465.com | 超级管理端 | `admin` / `admin123` | Cloudflare 已代理；与 ENTRY 共用证书 SAN |
| https://agent.157465.com | 代理端 | 见下表各站主代理 | 同上 |

### 各站主代理（一站一主代理，密码演示均为 `agent123`）

| 站点 | 主论坛域 | 主代理账号 |
|------|----------|------------|
| 刘伯温论坛 | 585520.xyz | `agent_a` |
| 至尊无上论坛 | 152687.xyz | `agent_b` |
| 神算子论坛 | 785412.xyz | `agent_ssz` |
| 招财宝论坛 | 658951.xyz | `agent_zcb` |
| 荣华富贵论坛 | 746528.xyz | `agent_rhfg` |

库层约束：`agent_accounts.tenant_id` / `domains.tenant_id` → `tenants`；`tenants.primary_agent_id` → 主代理；每站主代理唯一。

IP 白名单：超管后台「IP 白名单」页配置，默认关闭（全放开）；启用后限制超管与代理 API（含登录）。

上线后请尽快修改演示密码。

## 本地开发（对照）

| 地址 | 用途 | 账号 |
|------|------|------|
| http://127.0.0.1:3000/?host=lbw.local | 前台论坛（刘伯温） | — |
| http://127.0.0.1:3000/?host=entry.127.0.0.1 | 前台 ENTRY 伪装 | — |
| http://localhost:3001 | 超级管理端 | `admin` / `admin123` |
| http://localhost:3002 | 代理端 | `agent_a` / `agent123` |
| http://localhost:8080 | API | — |

## Nginx 路由摘要

| Host | 反代目标 |
|------|----------|
| 论坛 / ENTRY 域名 | `liuhecai-web` → `127.0.0.1:3000` |
| `admin.157465.com` | `liuhecai-admin` → `127.0.0.1:3001` |
| `agent.157465.com` | `liuhecai-agent` → `127.0.0.1:3002` |
| `/api/*` | Spring Boot → `127.0.0.1:8080` |

配置文件：`deploy/nginx/liuhecai.conf`（服务器：`/etc/nginx/sites-available/liuhecai.conf`）。
