# 入口伪装（对齐 311992.com）

## 原站链路

```text
311992.com（永久域）
  → 假电商页  dh-1.311992a0.buzz/demo/
  → 叠层 iframe  zy/88.html（电信/移动/联通/广电/澳门线路）
  → goto.php?…（目标 URL 数字串编码）
  → 真实论坛  lt-1.311992a2.buzz/bbs/
```

| 层 | 表现 | 作用 |
|---|---|---|
| 永久域首页 | 英文假商城（商品名「發發發」等） | 扫域名/备案看起来像外贸站 |
| 叠层导航 | 高 z-index iframe | 真人可见「临时线路」 |
| 线路页 | 五条运营商文案 | 伪装成线路选择 |
| 跳转 | 编码 URL，源码无明文论坛域 | 降低直接暴露 |
| 论坛 | 刘伯温资料站 | 业务本体 |

未见「按爬虫 UA 返回不同页」的可靠证据；伪装靠 **独立入口域 + 假站视觉 + 编码跳转**。

## URL 编解码（与原站一致）

```js
// 编码：每个字符 charCode + 1000，拼成连续 4 位数字
function encode(url) {
  return [...url].map((c) => String(c.charCodeAt(0) + 1000)).join('')
}

// 解码：每 4 位 parseInt - 1000 → fromCharCode
function decode(uu) {
  const u = []
  for (let j = 4; j <= uu.length; j += 4) {
    u.push(String.fromCharCode(parseInt(uu.toString().substr(j - 4, 4), 10) - 1000))
  }
  return u.join('')
}
```

本仓库实现：`packages/shared` 的 `encodeGotoUrl` / `decodeGotoUrl`。

## 本项目映射

| 原站 | 本项目 |
|---|---|
| 永久域 | `domains.role = ENTRY` 的 Host |
| 论坛域 | `domains.role = FORUM`（默认；可 `is_primary`） |
| 假商城 + 线路 | `apps/web` 在 ENTRY Host 下渲染伪装壳；线路由 `entry_lines` 配置 |
| `goto.php` | `/goto?u=` + 白名单校验（允许 `entryLines` 各目标论坛 Host + 本域 `forumHost`） |

运营约定：永久域绑 `ENTRY`，论坛子域绑 `FORUM`。

### 可配置线路

- 表：`entry_lines`（按 ENTRY 的 `domains.id`）
- 超管后台：域名绑定页 → ENTRY 行「配置线路」（`GET/PUT /api/admin/entry-domains/{domainId}/lines`）
- 前台：`GET /api/tenant/current` 在 `domainRole=ENTRY` 时返回 `entryLines: [{ label, color, forumUrl }]`
- 组件：`CamouflageLineNav.vue`（取代静态 `camouflage/nav.html` iframe）
- 新绑 ENTRY 域时自动写入默认五条（目标均为该域所属租户）

本地演示（`application-local.yml` 需 `trust-forwarded-host: true`）：

- 论坛：`http://127.0.0.1:3000/?host=127.0.0.1`
- 入口：`http://127.0.0.1:3000/?host=entry.127.0.0.1`
- 种子域名：`127.0.0.1`=`FORUM`，`entry.127.0.0.1`=`ENTRY`（`entry.*` 优先配对去掉前缀后的论坛域）

## 验收清单

- [ ] ENTRY Host 打开为假商城，可见线路文案
- [ ] 点线路经 `/goto?u=…` 进入论坛 Host
- [ ] 线路链接为编码串或 `/goto?u=`，非裸论坛域名（落点除外）
- [ ] FORUM Host 仍为正常论坛，不被伪装页覆盖
- [ ] `application-local.yml` 等密钥不入库
