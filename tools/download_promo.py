# -*- coding: utf-8 -*-
"""Download post-list promo images used after topic list on original site."""
from __future__ import annotations

import ssl
import urllib.request
from pathlib import Path

CTX = ssl._create_unverified_context()
OUT = Path(r"G:/part-time/liuhecai/apps/web/public/bbs/promo")
OUT.mkdir(parents=True, exist_ok=True)

ITEMS = [
    ("z129.png", "https://lt-1.311992a2.buzz/bbs/fta1/z129.png"),
    ("sgxs.jpg", "https://tk.tutu.finance/aomen/2026/col/203/sgxs.jpg"),
    ("vb4.jpg", "https://tk.tutu.finance/aomen/2026/col/203/vb4.jpg"),
    ("ampgt.jpg", "https://amo.jlidesign.com:4949/col/203/ampgt.jpg"),
    ("amttjs.jpg", "https://amo.jlidesign.com:4949/col/203/amttjs.jpg"),
    ("amgp.jpg", "https://amo.jlidesign.com:4949/col/203/amgp.jpg"),
    ("alalx18m.jpg", "https://amo.jlidesign.com:4949/col/203/alalx18m.jpg"),
    ("ammh.jpg", "https://amo.jlidesign.com:4949/col/203/ammh.jpg"),
    # already local copies:
    ("2025sxt.jpg", "https://lt-1.311992a2.buzz/bbs/images/2025sxt.jpg"),
    ("di2.png", "https://lt-1.311992a2.buzz/bbs/images/di2.png"),
]


def main() -> None:
    for name, url in ITEMS:
        dest = OUT / name
        if dest.exists() and dest.stat().st_size > 1000:
            print("skip", name, dest.stat().st_size)
            continue
        try:
            req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
            with urllib.request.urlopen(req, context=CTX, timeout=60) as resp:
                data = resp.read()
            dest.write_bytes(data)
            print("OK", name, len(data))
        except Exception as e:
            print("FAIL", name, e)


if __name__ == "__main__":
    main()
