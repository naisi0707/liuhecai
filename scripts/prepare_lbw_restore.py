# -*- coding: utf-8 -*-
"""Prepare original-site fragments/CSS/SQL for 1:1 web restore."""
from __future__ import annotations

import json
import re
from pathlib import Path

ROOT = Path(r"G:/part-time/liuhecai")
MIRROR = ROOT / "mirror/bbs"
DECODED = ROOT / "mirror/decoded"
PUBLIC = ROOT / "apps/web/public/bbs"
FRAG = PUBLIC / "fragments"
SQL = ROOT / "apps/api/src/main/resources/db"


def rewrite_html(html: str) -> str:
    # normalize asset paths to /bbs/... and root assets
    repls = [
        (r'src="images/', 'src="/bbs/images/'),
        (r"src='images/", "src='/bbs/images/"),
        (r'src="fta1/', 'src="/bbs/fta1/'),
        (r'src="ftimg/', 'src="/bbs/ftimg/'),
        (r'src="/bbs/fta1/', 'src="/bbs/fta1/'),
        (r'href="rcg.php"', 'href="/recharge"'),
        (r'href="register.php"', 'href="/register"'),
        (r'href="/bbs/register.php"', 'href="/register"'),
        (r'href="/bbs/rcg.php"', 'href="/recharge"'),
        (r'href="/bbs/login.php"', 'href="/login"'),
        (r'href="login.php"', 'href="/login"'),
        (r'src="/92.gif"', 'src="/92.gif"'),
        (r'src="/ye.gif"', 'src="/ye.gif"'),
        (r'src="/weix.jpg"', 'src="/weix.jpg"'),
        (r'src="/qq.jpg"', 'src="/qq.jpg"'),
    ]
    out = html
    for a, b in repls:
        out = out.replace(a, b)
    # strip external live iframes that may fail offline — keep structure but mark optional
    # keep them for 99% visual when network ok
    return out


def extract_inline_css(index_html: str) -> str:
    blocks = re.findall(r"<style[^>]*>(.*?)</style>", index_html, flags=re.S | re.I)
    # skip huge badge bootstrap copy if duplicated; keep all for fidelity
    css = "\n\n".join(blocks)
    css = css.replace("url(images/", "url(/bbs/images/")
    css = css.replace("url(images/", "url(/bbs/images/")
    return css


def build_topic_sql(titles: dict[str, str]) -> str:
    lines = [
        "-- Seed topics from mirrored homepage titles (demo bodies, not scraped paid content)",
        "USE liuhecai;",
        "DELETE FROM topic_orders WHERE tenant_id = 1001;",
        "DELETE FROM topics WHERE tenant_id = 1001;",
    ]
    items = list(titles.items())
    for i, (_k, title) in enumerate(items):
        play = "资料"
        m = re.search(r"【([^】]+)】", title)
        if m:
            play = m.group(1)
        price = 88 + (i % 20) * 10
        t = title.replace("\\", "\\\\").replace("'", "''")
        p = play.replace("'", "''")
        content = f"{title}\\n高手已加密演示正文。本站自有演示数据。\\n购买后可见。"
        content = content.replace("'", "''")
        tid = 3000001 + i
        lines.append(
            "INSERT INTO topics (id, tenant_id, title, lottery_type, issue_no, play_type, price, content, status, created_by, created_at, updated_at) "
            f"VALUES ({tid}, 1001, '{t}', 'MACAU_NEW', '203', '{p}', {price}, '{content}', 1, NULL, NOW(), NOW());"
        )
    theme = (
        '{"primaryColor":"#dc0000","fontFamily":"Microsoft YaHei",'
        '"logoUrl":"/bbs/images/logo.png","adBanner":"/bbs/images/311992.gif",'
        '"assetBase":"/bbs","skin":"lbw"}'
    )
    lines.append(
        "UPDATE tenants SET "
        f"theme_json='{theme}', "
        "announcement='刘伯温论坛公告：既日起！凡是新顾客注册统一送288金币老顾客多充多送，充值后自由购买。', "
        "kefu_wechat='K13532372818', kefu_qq='3757584264' "
        "WHERE id=1001;"
    )
    return "\n".join(lines) + "\n"


def main() -> None:
    FRAG.mkdir(parents=True, exist_ok=True)
    index_html = (MIRROR / "index.html").read_text(encoding="utf-8", errors="ignore")
    (PUBLIC / "site-extra.css").write_text(extract_inline_css(index_html), encoding="utf-8")

    home = (DECODED / "de-addhtmltxta.html").read_text(encoding="utf-8")
    (FRAG / "home-top.html").write_text(rewrite_html(home), encoding="utf-8")

    titles = json.loads((DECODED / "topic_titles.json").read_text(encoding="utf-8"))
    (SQL / "seed-m9-lbw-topics.sql").write_text(build_topic_sql(titles), encoding="utf-8")

    # also dump title list for frontend fallback
    clean = []
    for i, (_k, title) in enumerate(titles.items()):
        if i < 48:
            tag, color = "出售帖", "#FF0000"
        elif i < 70:
            tag, color = "精品帖", "#008000"
        elif i < 90:
            tag, color = "热门帖", "#0000FF"
        else:
            tag, color = "推荐帖", "#0000FF"
        clean.append({"tag": tag, "color": color, "title": title})
    (FRAG / "home-titles.json").write_text(json.dumps(clean, ensure_ascii=False, indent=2), encoding="utf-8")
    print("fragments ok", len(clean), "titles")


if __name__ == "__main__":
    main()
