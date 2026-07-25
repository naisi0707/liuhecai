#!/usr/bin/env python3
from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from ssh_util import connect, run


def main() -> int:
    c = connect()
    try:
        run(
            c,
            r"""
set +e
journalctl -u liuhecai-api -n 80 --no-pager | head -80
echo '==== run jar once ===='
cd /www/wwwroot/liuhecai
set -a; . /etc/liuhecai/api.env; set +a
timeout 25 /usr/bin/java -jar apps/api/target/liuhecai-api-0.0.1-SNAPSHOT.jar 2>&1 | head -80
""",
            timeout=90,
        )
        return 0
    finally:
        c.close()


if __name__ == "__main__":
    raise SystemExit(main())
