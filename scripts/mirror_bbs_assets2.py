# -*- coding: utf-8 -*-
from __future__ import annotations
import ssl
import urllib.request
from pathlib import Path
from urllib.parse import urljoin

BASE = "https://lt-1.311992a2.buzz/"
PUBLIC = Path(r"G:/part-time/liuhecai/apps/web/public")
ctx = ssl._create_unverified_context()

# paths relative to site root (not only /bbs/)
ASSETS = [
    "bbs/images/logo.png",
    "bbs/images/top.png",
    "bbs/images/311992.gif",
    "bbs/images/bg.webp",
    "bbs/images/bg-xs.webp",
    "bbs/images/bga.webp",
    "bbs/images/bga-xs.webp",
    "bbs/images/kb.webp",
    "bbs/images/m-kb.webp",
    "bbs/images/tab.webp",
    "bbs/images/acyz.webp",
    "bbs/images/acyz2.webp",
    "bbs/images/number.png",
    "bbs/images/number100x100.png",
    "bbs/images/number88x88.png",
    "bbs/images/number66x66.png",
    "bbs/images/number56x56.png",
    "bbs/images/number40x40.png",
    "bbs/images/number32x32.png",
    "bbs/images/2025sxt.jpg",
    "bbs/images/di2.png",
    "bbs/fta1/jiantou.gif",
    "bbs/fta1/macau.png",
    "bbs/fta1/wechat2.png",
    "bbs/fta1/z129.png",
    "bbs/ftimg/headhunt.gif",
    "bbs/main.css",
    "bbs/main.js",
    "bbs/css/bootstrap.min.css",
    "bbs/css/bootstrap-icons.css",
    "bbs/css/bbsdialogstyle.css",
    "bbs/js/bootstrap.bundle.min.js",
    "bbs/js/MiniDialog-es5.min.js",
    "bbs/js/lazysizes.min.js",
    "bbs/js/jquery/3.6.0/jquery.min.js",
    "bbs/js/jquery_lazyload/1.9.7/jquery.lazyload.min.js",
    "bbs/sznum.ttf",
    "bbs/num.ttf",
    "92.gif",
    "ye.gif",
    "weix.jpg",
    "qq.jpg",
]


def fetch(url: str) -> bytes:
    req = urllib.request.Request(
        url,
        headers={
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
            "Referer": "https://lt-1.311992a2.buzz/bbs/index.php",
            "Accept": "*/*",
        },
    )
    with urllib.request.urlopen(req, context=ctx, timeout=60) as resp:
        return resp.read()


def main() -> None:
    ok = fail = 0
    for rel in ASSETS:
        url = urljoin(BASE, rel)
        dest = PUBLIC / rel
        # root assets under public/ for /92.gif style OR under bbs/_site
        if not rel.startswith("bbs/"):
            dest = PUBLIC / "bbs" / "_site" / Path(rel).name
        dest.parent.mkdir(parents=True, exist_ok=True)
        try:
            data = fetch(url)
            dest.write_bytes(data)
            ok += 1
            print("OK", rel, len(data), "->", dest.relative_to(PUBLIC))
        except Exception as e:
            fail += 1
            print("FAIL", rel, e)
    print("done ok=", ok, "fail=", fail)


if __name__ == "__main__":
    main()
