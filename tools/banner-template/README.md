# 可编辑横幅模板（基于服务器原图）

## 原图来源（服务器）

| 项 | 值 |
|----|-----|
| 服务器 | `45.152.64.102` |
| 远程路径 | `/www/wwwroot/liuhecai/apps/web/.output/public/site/images/311992.gif` |
| 本地副本 | `originals/site-311992.gif`（1150×358，8 帧动图） |
| 站内引用 | 刘伯温 `theme_json.bannerUrl` = `/bbs/images/311992.gif` |

同内容还有：`img/banner.gif`、`bbs/images/311992.gif`。

## 你要的「一模一样 + 可改字」——推荐用 PS

自动去字做不到和原版一样干净（字是画在图上的）。你本机有 PS，请用：

1. 打开 `ps-起始帧@2x.png`（或 `ps-起始帧-请用内容识别填充去字.png`）
2. 用 **内容识别填充 / 生成式填充** 抹掉：主标题、副标题、域名  
   （保留龙、军师、两个圆形 logo、右上印章）
3. 存成 `background.png`（建议仍 1150×358；或用 `@2x` 再缩小）
4. 运行脚本：`在Photoshop中创建文字层.jsx`  
   → 生成可真正改字的文字层
5. 改文案后导出 PNG；需要动效可再跑下面的 `make_gif` / 用原 GIF 时间轴

也可直接打开已生成的 `横幅-*.psd`（Photopea 也能开），在文字层上改字。

## 已按 urls.md 生成的品牌文件

| 品牌 | 域名 | PSD | GIF |
|------|------|-----|-----|
| 刘伯温 | 585520.xyz | `横幅-liubowen.psd` | `out/liubowen-banner.gif` |
| 至尊无上 | 152687.xyz | `横幅-zhizun.psd` | `out/zhizun-banner.gif` |
| 神算子 | 785412.xyz | `横幅-shensuanzi.psd` | `out/shensuanzi-banner.gif` |
| 招财宝 | 658951.xyz | `横幅-zhaocaibao.psd` | `out/zhaocaibao-banner.gif` |
| 荣华富贵 | 746528.xyz | `横幅-ronghuafugui.psd` | `out/ronghuafugui-banner.gif` |

文案配置：`brands.json`

## 命令

```bash
# 重新拉服务器原图（需能 SSH）
# 见 deploy/scripts/ssh_util.py

# 出 PSD
node build_psd.js liubowen
node build_psd.js --title 刘伯温 --domain 585520.xyz

# 出 GIF
../../.venv-banner/bin/python make_gif.py --all
```

## 说明

- `uploads/` 目录当前是空的；横幅原图在 web 静态资源里，不在上传目录。
- 其余 4 个租户库里还没有独立 `bannerUrl`，目前共用这套刘伯温原图换字。
- 你在 PS 里生成式去字后的 `background.png` 发我/放回本目录，我可以再帮你批量出五站 GIF。
