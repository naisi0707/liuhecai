# -*- coding: utf-8 -*-
"""Mirror public UI assets from 刘伯温 forum pages (no paid content scraping)."""
from __future__ import annotations

import re
import urllib.request
import ssl
from pathlib import Path
from urllib.parse import urljoin, urlparse

BASE = "https://lt-1.311992a2.buzz/bbs/"
MIRROR = Path(r"G:/part-time/liuhecai/mirror")
PUBLIC = Path(r"G:/part-time/liuhecai/apps/web/public/bbs")
PAGES = {
    "index.html": "index.php",
    "rule.html": "rule.php",
    "rcg.html": "rcg.php",
    "kefu.html": "kefu.php",
    "login.html": "login.php",
    "register.html": "register.php",
    "topic.html": "topic.php?id=15052",
}

ctx = ssl._create_unverified_context()


def fetch(url: str) -> bytes:
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
    with urllib.request.urlopen(req, context=ctx, timeout=60) as resp:
        return resp.read()


def extract_refs(html: str) -> set[str]:
    refs: set[str] = set()
    for m in re.findall(r"""(?:href|src)\s*=\s*["']([^"']+)["']""", html, re.I):
        if m.startswith(("data:", "javascript:", "#", "mailto:", "tel:")):
            continue
        refs.add(m)
    for m in re.findall(r"""url\(\s*['"]?([^'")]+)['"]?\s*\)""", html, re.I):
        if m.startswith("data:"):
            continue
        refs.add(m)
    return refs


def local_path_for(abs_url: str) -> Path | None:
    p = urlparse(abs_url)
    if "311992" not in p.netloc and "lt-1" not in p.netloc:
        # keep same-site only; also allow buzz domain
        if not p.netloc.endswith(".buzz") and "311992" not in abs_url:
            return None
    path = p.path
    if not path or path.endswith("/"):
        return None
    # map /bbs/... -> public/bbs/... and /weix.jpg etc under public/bbs/root/
    if path.startswith("/bbs/"):
        rel = path[len("/bbs/") :]
        return PUBLIC / rel
    # root-level images like /weix.jpg /qq.jpg
    rel = path.lstrip("/")
    return PUBLIC / "_root" / rel


def main() -> None:
    MIRROR.mkdir(parents=True, exist_ok=True)
    PUBLIC.mkdir(parents=True, exist_ok=True)
    html_dir = MIRROR / "bbs"
    html_dir.mkdir(parents=True, exist_ok=True)

    all_refs: set[str] = set()
    for name, path in PAGES.items():
        url = urljoin(BASE, path)
        print("PAGE", url)
        try:
            data = fetch(url)
        except Exception as e:
            print(" FAIL", e)
            continue
        (html_dir / name).write_bytes(data)
        text = data.decode("utf-8", errors="ignore")
        all_refs |= extract_refs(text)
        print("  saved", name, len(data))

    # also scan downloaded html again
    for f in html_dir.glob("*.html"):
        all_refs |= extract_refs(f.read_text(encoding="utf-8", errors="ignore"))

    downloaded = 0
    skipped = 0
    for ref in sorted(all_refs):
        abs_url = urljoin(BASE, ref)
        dest = local_path_for(abs_url)
        if dest is None:
            skipped += 1
            continue
        # skip php/html endpoints
        if dest.suffix.lower() in {".php", ".html", ".htm"} or not dest.suffix:
            # might be css/js without query
            parsed = urlparse(abs_url)
            if not Path(parsed.path).suffix:
                skipped += 1
                continue
            if dest.suffix.lower() in {".php", ".html", ".htm"}:
                skipped += 1
                continue
        dest.parent.mkdir(parents=True, exist_ok=True)
        if dest.exists() and dest.stat().st_size > 0:
            continue
        try:
            data = fetch(abs_url)
            dest.write_bytes(data)
            downloaded += 1
            print("ASSET", dest.relative_to(PUBLIC), len(data))
        except Exception as e:
            print("ASSET FAIL", abs_url, e)

    # second pass: extract url() from downloaded css
    css_refs: set[str] = set()
    for css in PUBLIC.rglob("*.css"):
        css_refs |= extract_refs(css.read_text(encoding="utf-8", errors="ignore"))
        # relative urls against css location need special handling
        text = css.read_text(encoding="utf-8", errors="ignore")
        for m in re.findall(r"""url\(\s*['"]?([^'")]+)['"]?\s*\)""", text, re.I):
            if m.startswith("data:"):
                continue
            abs_url = urljoin(urljoin(BASE, str(css.relative_to(PUBLIC)).replace("\\", "/")), m)
            # better: css under public/bbs/css/x.css -> base https://.../bbs/css/
            rel_css = css.relative_to(PUBLIC).as_posix()
            css_page_url = urljoin(BASE, rel_css)
            abs_url = urljoin(css_page_url, m)
            dest = local_path_for(abs_url)
            if dest is None:
                continue
            if dest.exists() and dest.stat().st_size > 0:
                continue
            try:
                data = fetch(abs_url)
                dest.parent.mkdir(parents=True, exist_ok=True)
                dest.write_bytes(data)
                downloaded += 1
                print("CSS-ASSET", dest.relative_to(PUBLIC), len(data))
            except Exception as e:
                print("CSS-ASSET FAIL", abs_url, e)

    print("DONE downloaded=", downloaded, "skipped_refs=", skipped)
    print("PUBLIC tree count=", sum(1 for _ in PUBLIC.rglob("*") if _.is_file()))


if __name__ == "__main__":
    main()
