# -*- coding: utf-8 -*-
from __future__ import annotations

import re
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def mysql_password() -> str:
    yml = (ROOT / "apps/api/src/main/resources/application-local.yml").read_text(encoding="utf-8")
    m = re.search(r"password:\s*(\S+)", yml)
    if not m:
        raise SystemExit("password not found in application-local.yml")
    return m.group(1)


def run_mysql(sql: str | bytes, as_bytes: bool = False) -> None:
    pwd = mysql_password()
    cmd = ["mysql", "-uroot", f"-p{pwd}", "--default-character-set=utf8mb4"]
    if isinstance(sql, str) and not as_bytes:
        r = subprocess.run(cmd + ["liuhecai", "-e", sql], capture_output=True)
    else:
        data = sql if isinstance(sql, bytes) else sql.encode("utf-8")
        r = subprocess.run(cmd, input=data, capture_output=True)
    if r.returncode != 0:
        raise SystemExit(r.stderr.decode("utf-8", errors="replace"))
    out = r.stdout.decode("utf-8", errors="replace").strip()
    if out:
        print(out)


def main() -> None:
    seed = ROOT / "apps/api/src/main/resources/db/seed-m13-topic-preview.sql"
    print("applying", seed)
    run_mysql(seed.read_bytes(), as_bytes=True)
    run_mysql(
        "SELECT COUNT(*) AS total, "
        "SUM(preview_content IS NOT NULL) AS with_preview "
        "FROM topics WHERE tenant_id=1001;"
    )
    run_mysql(
        "SELECT id, play_type, view_count, CHAR_LENGTH(preview_content) AS preview_len "
        "FROM topics WHERE tenant_id=1001 AND play_type='重见天日' LIMIT 1;"
    )


if __name__ == "__main__":
    main()
