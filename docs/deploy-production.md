# 生产部署说明

服务器：`45.152.64.102`（Ubuntu 18.04；以 `docs/服务器.txt` 为准）  
应用根目录：`/www/wwwroot/liuhecai`

> Ubuntu 18.04 glibc 过旧，**勿在服务器上用官方 Node 20 构建**。本机构建后上传产物；运行时使用 unofficial Node 20（glibc 2.17）于 `/opt/node-v20.18.1`。

## 进程

| 服务 | 管理 | 说明 |
|------|------|------|
| API | `systemctl restart liuhecai-api` | 环境变量 `/etc/liuhecai/api.env` |
| web/admin/agent | `pm2 restart all` | 配置 `/etc/liuhecai/ecosystem.config.cjs` |
| Nginx | `nginx -t && systemctl reload nginx` | `/etc/nginx/sites-available/liuhecai.conf` |

## 域名

完整对照见 **[urls.md](./urls.md)**。摘要：

| Host | 用途 | HTTPS |
|------|------|-------|
| `157465.com` / `www.157465.com` | ENTRY 伪装入口（Cloudflare 已代理） | Let's Encrypt / Cloudflare Full |
| `585520.xyz` | 刘伯温论坛 FORUM | 已签发 |
| `152687.xyz` | 至尊无上论坛 FORUM | 已签发 |
| `785412.xyz` | 神算子论坛 FORUM | 已签发 |
| `658951.xyz` | 招财宝论坛 FORUM | 已签发 |
| `746528.xyz` | 荣华富贵论坛 FORUM | 已签发 |
| `admin.157465.com` | 超级管理端 | Cloudflare 已代理；证书与 ENTRY 同 SAN |
| `agent.157465.com` | 代理端 | 同上 |

演示账号（上线后请改密）：超管 `admin` / `admin123`；各站主代理见 [urls.md](./urls.md)（`agent_a` / `agent_b` / `agent_ssz` / `agent_zcb` / `agent_rhfg`，密码均为 `agent123`）。

## 本机构建后上传

```powershell
# 本机
pnpm install
$env:NUXT_IGNORE_LOCK='1'
pnpm --filter @liuhecai/web build
pnpm --filter @liuhecai/admin build
pnpm --filter @liuhecai/agent build
& D:\apache-maven-3.9.8\bin\mvn.cmd -q -DskipTests package -f apps/api/pom.xml
# 已上线环境增量发布（不改 api.env / MySQL 密码）：
python deploy/scripts/redeploy_release.py
# 仅 API：
python deploy/scripts/upload_jar_and_verify.py
# 仅前台 web：
python deploy/scripts/redeploy_web.py
# 首次装机全量（会重写 api.env，勿用于日常更新）：
# python deploy/scripts/upload_and_setup.py
```

## 常用运维

```bash
systemctl restart liuhecai-api
pm2 restart all
nginx -t && systemctl reload nginx
certbot renew --quiet
```

密钥仅存服务器：`/etc/liuhecai/api.env`（勿提交仓库）。`docs/服务器.txt` 已 gitignore。

## 验收命令（在服务器上）

```bash
curl -sS -H 'Host: 585520.xyz' http://127.0.0.1/api/health
curl -sS -H 'Host: 157465.com' http://127.0.0.1/api/tenant/current   # domainRole=ENTRY
curl -sk --resolve 585520.xyz:443:127.0.0.1 https://585520.xyz/api/health
```
