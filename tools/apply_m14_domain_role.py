# -*- coding: utf-8 -*-
import re
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
yml = (ROOT / "apps/api/src/main/resources/application-local.yml").read_text(encoding="utf-8")
pwd = re.search(r"password:\s*(\S+)", yml).group(1)


def mysql(sql: str) -> None:
    r = subprocess.run(
        ["mysql", "-uroot", f"-p{pwd}", "--default-character-set=utf8mb4", "liuhecai", "-e", sql],
        capture_output=True,
    )
    out = r.stdout.decode("utf-8", errors="replace")
    err = r.stderr.decode("utf-8", errors="replace")
    if out:
        print(out)
    if r.returncode != 0 and "Duplicate column" not in err and "Duplicate entry" not in err:
        raise SystemExit(err)
    if err and "Using a password" not in err:
        print(err)


mysql(
    "ALTER TABLE domains ADD COLUMN role VARCHAR(16) NOT NULL DEFAULT 'FORUM' "
    "COMMENT 'ENTRY入口伪装 FORUM论坛' AFTER is_primary"
)
mysql(
    "INSERT INTO domains (id, tenant_id, host, is_primary, role, status) "
    "SELECT 2005, 1001, 'entry.127.0.0.1', 0, 'ENTRY', 1 FROM DUAL "
    "WHERE NOT EXISTS (SELECT 1 FROM domains WHERE host='entry.127.0.0.1')"
)
mysql("SELECT id, host, role, is_primary FROM domains WHERE tenant_id=1001 ORDER BY id")
