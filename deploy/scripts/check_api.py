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
systemctl --no-pager -l status liuhecai-api | head -50
echo '--- journal ---'
journalctl -u liuhecai-api -n 40 --no-pager
echo '--- curl ---'
curl -sS -m 5 http://127.0.0.1:8080/api/health; echo
curl -sS -o /dev/null -w 'web:%{http_code}\n' http://127.0.0.1:3000/
curl -sS -o /dev/null -w 'admin:%{http_code}\n' http://127.0.0.1:3001/
curl -sS -o /dev/null -w 'agent:%{http_code}\n' http://127.0.0.1:3002/
ls -lh /www/wwwroot/liuhecai/apps/api/target/*.jar
cat /etc/systemd/system/liuhecai-api.service
""",
            timeout=60,
        )
        return 0
    finally:
        c.close()


if __name__ == "__main__":
    raise SystemExit(main())
