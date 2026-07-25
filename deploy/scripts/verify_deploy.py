#!/usr/bin/env python3
from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from ssh_util import connect, run


def main() -> int:
    c = connect()
    try:
        code, _ = run(
            c,
            r"""
set -e
systemctl is-active liuhecai-api
curl -sS http://127.0.0.1:8080/api/health; echo
curl -sS -o /dev/null -w 'web:%{http_code}\n' http://127.0.0.1:3000/
curl -sS -o /dev/null -w 'admin:%{http_code}\n' http://127.0.0.1:3001/
curl -sS -o /dev/null -w 'agent:%{http_code}\n' http://127.0.0.1:3002/
curl -sS -H 'Host: 585520.xyz' http://127.0.0.1/api/health; echo
curl -sS -o /dev/null -w 'nginx-forum:%{http_code}\n' -H 'Host: 585520.xyz' http://127.0.0.1/
curl -sS -o /dev/null -w 'nginx-entry:%{http_code}\n' -H 'Host: 157465.com' http://127.0.0.1/
. /etc/liuhecai/api.env
mysql -uliuhecai -p"$MYSQL_PASSWORD" -N -e "SHOW COLUMNS FROM liuhecai.topics LIKE 'tag';"
pm2 jlist | python3 -c "import sys,json; d=json.load(sys.stdin); print('pm2', [(x['name'], x['pm2_env']['status']) for x in d])"
""",
            timeout=60,
        )
        return code
    finally:
        c.close()


if __name__ == "__main__":
    raise SystemExit(main())
