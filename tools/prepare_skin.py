# -*- coding: utf-8 -*-
"""Decode GBK/base64 obfuscated blocks and prepare static fragments + topic seed."""
from __future__ import annotations

import base64
import json
import re
from pathlib import Path

MIRROR = Path(r"G:/part-time/liuhecai/tools/mirror")
FRAG = Path(r"G:/part-time/liuhecai/apps/web/public/bbs/fragments")
SQL = Path(r"G:/part-time/liuhecai/apps/api/src/main/resources/db")


def decode_b64_gbk(val: str) -> str:
    raw = base64.b64decode(val)
    try:
        return raw.decode("gbk")
    except Exception:
        return raw.decode("utf-8", errors="ignore")


def rewrite_paths(html: str) -> str:
    reps = [
        ('src="images/', 'src="/bbs/images/'),
        ("src='images/", "src='/bbs/images/"),
        ('src="fta1/', 'src="/bbs/fta1/'),
        ('src="/bbs/fta1/', 'src="/bbs/fta1/'),
        ('src="/92.gif"', 'src="/bbs/_root/92.gif"'),
        ('src="/ye.gif"', 'src="/bbs/_root/ye.gif"'),
        ('src="/weix.jpg"', 'src="/bbs/_root/weix.jpg"'),
        ('src="/qq.jpg"', 'src="/bbs/_root/qq.jpg"'),
        ('src="/bbs/fta1/wechat2.png"', 'src="/bbs/fta1/wechat2.png"'),
        ('href="register.php"', 'href="/register"'),
        ('href="/bbs/register.php"', 'href="/register"'),
        ('href="login.php"', 'href="/login"'),
        ('href="/bbs/login.php"', 'href="/login"'),
        ('href="rcg.php"', 'href="/recharge"'),
        ('href="/bbs/rcg.php"', 'href="/recharge"'),
        ('href="rule.php"', 'href="/rules"'),
        ('href="kefu.php"', 'href="/kefu"'),
        ('href="index.php"', 'href="/"'),
        ('href="/recharge"', 'href="/recharge"'),
        # drop external live iframes for local/demo stability; keep placeholder slots in vue
        (
            '<div class="white-box"><iframe marginwidth="0" marginheight="0" frameborder="0" width="100%" scrolling="no" height="180"\n\n        src="https://zhibo.77kj.vip/kj/a4.html?am"></iframe></div>',
            '<div class="white-box draw-slot" data-slot="draw"></div>',
        ),
        (
            '<iframe id="live-iframe" width="100%" height="300" src="https://1e.36351c.com:8443/zb/kjzb.html" frameborder="0"\n\n    scrolling="no" target="_blank"></iframe>',
            '<div class="live-slot" data-slot="live"></div>',
        ),
    ]
    for a, b in reps:
        html = html.replace(a, b)
    # hands.gif keep remote or skip
    return html


def extract_payloads(html: str) -> dict[str, str]:
    out: dict[str, str] = {}
    for m in re.finditer(r"decrypt\('([^']+)','([^']*)'\)", html):
        key, val = m.group(1), m.group(2)
        if val:
            out[key] = decode_b64_gbk(val)
    return out


def main() -> None:
    FRAG.mkdir(parents=True, exist_ok=True)
    index = (MIRROR / "index.html").read_text(encoding="utf-8", errors="ignore")
    payloads = extract_payloads(index)
    (FRAG / "decoded-payloads.json").write_text(
        json.dumps({k: v[:200] + ("..." if len(v) > 200 else "") for k, v in payloads.items()}, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )

    # home header block
    if "de-addhtmltxta" in payloads:
        html = rewrite_paths(payloads["de-addhtmltxta"])
        (FRAG / "home-top.html").write_text(html, encoding="utf-8")
        print("home-top", len(html))

    # titles already in home-titles.json; build SQL seed
    titles_path = FRAG / "home-titles.json"
    titles = json.loads(titles_path.read_text(encoding="utf-8"))
    lines = [
        "-- Auto seed demo topics (structure titles from public list; no paid bodies)",
        "DELETE FROM topic_orders WHERE topic_id IN (SELECT id FROM topics WHERE tenant_id = 1001 AND title LIKE '203期：%');",
        "DELETE FROM topics WHERE tenant_id = 1001 AND (title LIKE '203期：%' OR title = 'M7测试帖');",
    ]
    for i, t in enumerate(titles[:80], start=1):
        title = t["title"].replace("'", "''")
        # extract play type roughly from 【...】
        m = re.search(r"【([^】]+)】", title)
        play = (m.group(1) if m else "资料").replace("'", "''")
        price = 88 + (i % 7) * 10
        lines.append(
            "INSERT INTO topics (tenant_id, title, lottery_type, issue_no, play_type, price, content, status, created_at, updated_at) "
            f"VALUES (1001, '{title}', 'MACAU_NEW', '203', '{play}', {price}, "
            f"'高手已加密，购买即可查看资料', 1, NOW(), NOW());"
        )
    seed = SQL / "seed-web-topics.sql"
    seed.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print("seed topics", len(titles[:80]), "->", seed)

    # theme json for tenant
    theme = {
        "skin": "liubowen",
        "assetBase": "/bbs",
        "logoUrl": "/bbs/images/logo.png",
        "bannerUrl": "/bbs/images/311992.gif",
        "bgUrl": "/bbs/images/bga.webp",
        "qrWechatUrl": "/bbs/_root/weix.jpg",
        "qrQqUrl": "/bbs/_root/qq.jpg",
        "primaryColor": "#dc0000",
        "fontFamily": "Microsoft YaHei",
        "sisterSites": [
            {"name": "至尊无上论坛", "domain": "305551.com", "href": "https://305551.com", "cta": "查看至尊无上", "color": "#FF0000"},
            {"name": "神算子论坛", "domain": "933858.com", "href": "https://933858.com", "cta": "查看神算子", "color": "#22ac38"},
        ],
    }
    (FRAG / "theme-liubowen.json").write_text(json.dumps(theme, ensure_ascii=False, indent=2), encoding="utf-8")
    theme_sql = SQL / "seed-web-theme.sql"
    theme_sql.write_text(
        "UPDATE tenants SET "
        f"theme_json = '{json.dumps(theme, ensure_ascii=False)}', "
        "logo_url = '/bbs/images/logo.png', "
        "primary_color = '#dc0000', "
        "font_family = 'Microsoft YaHei', "
        "kefu_wechat = 'K13532372818', "
        "kefu_qq = '3757584264', "
        "announcement = '刘伯温论坛公告：既日起！凡是新顾客注册统一送288金币老顾客多充多送，充值后自由购买。微信号：K13532372818 QQ：3757584264' "
        "WHERE id = 1001;\n",
        encoding="utf-8",
    )
    print("theme sql", theme_sql)


if __name__ == "__main__":
    main()
