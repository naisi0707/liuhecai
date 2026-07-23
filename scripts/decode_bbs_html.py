# -*- coding: utf-8 -*-
"""Decode GBK+base64 decrypt() payloads from mirrored HTML."""
from __future__ import annotations
import base64
import json
import re
from pathlib import Path

MIRROR = Path(r"G:/part-time/liuhecai/mirror/bbs")
OUT = Path(r"G:/part-time/liuhecai/mirror/decoded")


def decode_payload(b64: str) -> str:
    raw = base64.b64decode(b64)
    try:
        return raw.decode("gbk")
    except Exception:
        return raw.decode("utf-8", errors="replace")


def extract_from_html(text: str) -> dict[str, str]:
    found: dict[str, str] = {}
    # decrypt('id','payload');
    for m in re.finditer(r"decrypt\(\s*'([^']+)'\s*,\s*'([^']+)'\s*\)", text):
        found[m.group(1)] = decode_payload(m.group(2))
    # decrypt2('payload')
    for m in re.finditer(r"decrypt2\(\s*'([^']+)'\s*\)", text):
        found[f"decrypt2:{m.group(1)[:12]}"] = decode_payload(m.group(1))
    return found


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    index = (MIRROR / "index.html").read_text(encoding="utf-8", errors="ignore")
    decoded = extract_from_html(index)
    (OUT / "index_decrypt.json").write_text(
        json.dumps({k: v[:500] + ("…" if len(v) > 500 else "") for k, v in decoded.items()}, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    # save large blocks fully
    for key in ("de-addhtmltxta", "de-addhtmltxtb", "de-sy-zc"):
        if key in decoded:
            (OUT / f"{key}.html").write_text(decoded[key], encoding="utf-8")
            print(key, "len=", len(decoded[key]))

    # topic titles sample
    titles = {k: v for k, v in decoded.items() if k.startswith("de-newstitle")}
    (OUT / "topic_titles.json").write_text(json.dumps(titles, ensure_ascii=False, indent=2), encoding="utf-8")
    print("titles", len(titles))

    # also decode login/register for structure hints
    for name in ("login.html", "register.html", "rule.html", "rcg.html", "topic.html"):
        p = MIRROR / name
        if not p.exists():
            continue
        d = extract_from_html(p.read_text(encoding="utf-8", errors="ignore"))
        (OUT / f"{name}.json").write_text(json.dumps(d, ensure_ascii=False, indent=2), encoding="utf-8")
        print(name, "keys", len(d))


if __name__ == "__main__":
    main()
