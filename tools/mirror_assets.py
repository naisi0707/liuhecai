# -*- coding: utf-8 -*-
"""Mirror public UI assets from original bbs (no paid topic bodies)."""
from __future__ import annotations

import json
import re
import ssl
import urllib.request
from pathlib import Path
from urllib.parse import urljoin, urlparse

BASE = "https://lt-1.311992a2.buzz/bbs/"
MIRROR = Path(r"G:/part-time/liuhecai/tools/mirror")
OUT = Path(r"G:/part-time/liuhecai/apps/web/public/bbs")
CTX = ssl._create_unverified_context()

KNOWN = [
    "https://lt-1.311992a2.buzz/bbs/css/bootstrap.min.css",
    "https://lt-1.311992a2.buzz/bbs/main.css",
    "https://lt-1.311992a2.buzz/bbs/css/bootstrap-icons.css",
    "https://lt-1.311992a2.buzz/bbs/css/bbsdialogstyle.css",
    "https://lt-1.311992a2.buzz/bbs/js/jquery/3.6.0/jquery.min.js",
    "https://lt-1.311992a2.buzz/bbs/js/lazysizes.min.js",
    "https://lt-1.311992a2.buzz/bbs/main.js",
    "https://lt-1.311992a2.buzz/bbs/images/bga.webp",
    "https://lt-1.311992a2.buzz/bbs/images/logo.png",
    "https://lt-1.311992a2.buzz/bbs/images/top.png",
    "https://lt-1.311992a2.buzz/bbs/images/311992.gif",
    "https://lt-1.311992a2.buzz/bbs/images/2025sxt.jpg",
    "https://lt-1.311992a2.buzz/bbs/images/di2.png",
    "https://lt-1.311992a2.buzz/92.gif",
    "https://lt-1.311992a2.buzz/ye.gif",
    "https://lt-1.311992a2.buzz/bbs/fta1/jiantou.gif",
    "https://lt-1.311992a2.buzz/bbs/fta1/macau.png",
    "https://lt-1.311992a2.buzz/bbs/fta1/wechat2.png",
    "https://lt-1.311992a2.buzz/bbs/fta1/z129.png",
    "https://lt-1.311992a2.buzz/weix.jpg",
    "https://lt-1.311992a2.buzz/qq.jpg",
    "https://lt-1.311992a2.buzz/bbs/ftimg/headhunt.gif",
]


def local_path(url: str) -> Path | None:
    p = urlparse(url)
    if "311992a2.buzz" not in p.netloc and "311992" not in p.netloc:
        # keep only same-origin skin assets; skip third-party tip images
        return None
    path = p.path
    if path.startswith("/bbs/"):
        rel = path[len("/bbs/") :]
    elif path.startswith("/"):
        rel = path.lstrip("/")
        if not rel.startswith(("images/", "css/", "js/", "fta1/", "ftimg/")):
            # root-level assets like /weix.jpg /92.gif
            rel = f"_root/{Path(rel).name}"
    else:
        return None
    return OUT / rel


def download(url: str) -> bool:
    dest = local_path(url)
    if dest is None:
        return False
    dest.parent.mkdir(parents=True, exist_ok=True)
    if dest.exists() and dest.stat().st_size > 0:
        return True
    try:
        req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
        with urllib.request.urlopen(req, context=CTX, timeout=45) as resp:
            data = resp.read()
        dest.write_bytes(data)
        print("OK", dest.relative_to(OUT), len(data))
        return True
    except Exception as e:
        print("FAIL", url, e)
        return False


def extract_from_html() -> set[str]:
    urls: set[str] = set(KNOWN)
    for f in MIRROR.glob("*.html"):
        t = f.read_text(encoding="utf-8", errors="ignore")
        for m in re.finditer(r"""(?:href|src)=["']([^"'#]+)["']""", t, re.I):
            u = m.group(1).strip()
            if u.startswith(("data:", "javascript:", "#", "mailto:")):
                continue
            urls.add(urljoin(BASE, u))
        for m in re.finditer(r"""url\(["']?([^)"']+)["']?\)""", t, re.I):
            u = m.group(1).strip()
            if u.startswith("data:"):
                continue
            urls.add(urljoin(BASE, u))
    return urls


def extract_css_urls(css_text: str, css_url: str) -> set[str]:
    found: set[str] = set()
    for m in re.finditer(r"""url\(["']?([^)"']+)["']?\)""", css_text, re.I):
        u = m.group(1).strip()
        if u.startswith("data:"):
            continue
        found.add(urljoin(css_url, u))
    return found


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    urls = extract_from_html()
    # first pass download css/js/images
    for u in sorted(urls):
        if any(x in u.lower() for x in (".css", ".js", ".png", ".jpg", ".jpeg", ".gif", ".webp", ".svg", ".ico", ".woff", ".ttf", ".eot")):
            download(u)
        elif "/images/" in u or "/fta1/" in u or "/ftimg/" in u:
            download(u)

    # second pass: parse local css for fonts/images
    for css in OUT.rglob("*.css"):
        text = css.read_text(encoding="utf-8", errors="ignore")
        # rewrite absolute urls later; first download deps
        # guess original url
        rel = css.relative_to(OUT).as_posix()
        css_url = urljoin(BASE, rel)
        for u in extract_css_urls(text, css_url):
            download(u)

    # also download topic page shell for structure (public)
    for name, url in [
        ("topic.html", "https://lt-1.311992a2.buzz/bbs/topic.php?id=15052"),
    ]:
        dest = MIRROR / name
        if not dest.exists():
            try:
                req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
                with urllib.request.urlopen(req, context=CTX, timeout=45) as resp:
                    dest.write_bytes(resp.read())
                print("HTML", name, dest.stat().st_size)
            except Exception as e:
                print("FAIL html", name, e)

    # rewrite css url paths to local /bbs/...
    for css in OUT.rglob("*.css"):
        text = css.read_text(encoding="utf-8", errors="ignore")
        orig = text

        def repl(m: re.Match[str]) -> str:
            raw = m.group(1).strip().strip("\"'")
            if raw.startswith("data:"):
                return m.group(0)
            absu = urljoin(urljoin(BASE, css.relative_to(OUT).as_posix()), raw)
            lp = local_path(absu)
            if lp and lp.exists():
                # path relative to this css file
                try:
                    rel = Path(lp).relative_to(css.parent).as_posix()
                except ValueError:
                    rel = "/" + ("bbs/" + lp.relative_to(OUT).as_posix())
                    return f'url("{rel}")'
                return f'url("{rel}")'
            return m.group(0)

        text2 = re.sub(r"url\(([^)]+)\)", repl, text)
        if text2 != orig:
            css.write_text(text2, encoding="utf-8")
            print("rewrote", css)

    manifest = {
        "base": BASE,
        "files": [p.relative_to(OUT).as_posix() for p in OUT.rglob("*") if p.is_file()],
    }
    (MIRROR / "manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
    print("files", len(manifest["files"]))


if __name__ == "__main__":
    main()
