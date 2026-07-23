# -*- coding: utf-8 -*-
"""Generate seed SQL for topic preview_content (往期成绩)."""
from __future__ import annotations

import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def esc(s: str) -> str:
    return s.replace("\\", "\\\\").replace("'", "''")


def make_preview(author: str, play: str, issue: int = 203, history_from: int = 198) -> str:
    parts: list[str] = []
    for n in range(history_from, issue):
        hit = "准" if n % 3 else "错"
        tip = f"{n % 49 + 1:02d}-{((n * 3) % 49) + 1:02d}-{((n * 7) % 49) + 1:02d}"
        open_n = (
            f"{((n * 2) % 49) + 1:02d}-{((n * 5) % 49) + 1:02d}-{((n * 11) % 49) + 1:02d}-"
            f"{((n * 13) % 49) + 1:02d}-{((n * 17) % 49) + 1:02d}-{((n * 19) % 49) + 1:02d}特:{tip}"
        )
        parts.append(
            f'<b><font size="4"><font face="helvetica neue  ">{n}期:'
            f'<span style="background-color:#ff99bb "><span style="color:#000000 ">『{author}』</span></span> '
            f'&#127847;<span style="color:#660066 ">{play}</span>&#127847;<br>'
            f"    【{tip}】【{((n * 4) % 49) + 1:02d}-{((n * 6) % 49) + 1:02d}-{((n * 8) % 49) + 1:02d}】"
            f"【{((n * 9) % 49) + 1:02d}-{((n * 10) % 49) + 1:02d}-{((n * 12) % 49) + 1:02d}】<br>"
            f'    开:<span style="color:#ff0000 ">{open_n}</span>{hit}</font></font></b><br><br>'
        )
    parts.append(
        f'<b><font size="4"><font face="helvetica neue  ">{issue}期:'
        f'<span style="background-color:#ff99bb "><span style="color:#000000 ">『{author}』</span></span> '
        f'&#127847;<span style="color:#660066 ">{play}</span>&#127847;</font></font></b>'
    )
    return "".join(parts)


def main() -> None:
    full = json.loads((ROOT / "mirror" / "decoded" / "topic.html.json").read_text(encoding="utf-8"))[
        "de-content"
    ]
    seed = (ROOT / "apps" / "api" / "src" / "main" / "resources" / "db" / "seed-m9-lbw-topics.sql").read_text(
        encoding="utf-8"
    )
    authors: list[tuple[str, str]] = []
    for m in re.finditer(
        r"'203期：【([^】]+)】【([^】]+)】[^']*', 'MACAU_NEW', '203', '([^']+)'",
        seed,
    ):
        authors.append((m.group(3), m.group(2)))

    lines = [
        "-- Seed public preview (往期) for topic detail",
        "USE liuhecai;",
        (
            "UPDATE topics SET preview_content = '"
            + esc(full)
            + "', view_count = GREATEST(IFNULL(view_count,0), 8748) "
            "WHERE tenant_id = 1001 AND play_type = '重见天日';"
        ),
    ]
    seen: set[str] = set()
    for play_type, play in authors:
        if play_type in seen or play_type == "重见天日":
            continue
        seen.add(play_type)
        html = make_preview(play_type, play)
        lines.append(
            "UPDATE topics SET preview_content = '"
            + esc(html)
            + "', view_count = IF(IFNULL(view_count,0)=0, FLOOR(100+RAND()*9000), view_count) "
            f"WHERE tenant_id = 1001 AND play_type = '{esc(play_type)}' "
            "AND (preview_content IS NULL OR preview_content = '');"
        )

    out = ROOT / "apps" / "api" / "src" / "main" / "resources" / "db" / "seed-m13-topic-preview.sql"
    out.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"wrote {out} bytes={out.stat().st_size} updates={len(lines) - 2}")


if __name__ == "__main__":
    main()
