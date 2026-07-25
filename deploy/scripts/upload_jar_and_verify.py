#!/usr/bin/env python3
from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from ssh_util import connect, run

ROOT = Path(__file__).resolve().parents[2]
JAR = ROOT / "apps/api/target/liuhecai-api-0.0.1-SNAPSHOT.jar"


def main() -> int:
    if not JAR.exists():
        print("missing jar", JAR)
        return 1
    c = connect()
    try:
        sftp = c.open_sftp()
        remote = "/www/wwwroot/liuhecai/apps/api/target/liuhecai-api-0.0.1-SNAPSHOT.jar"
        print("upload jar", JAR.stat().st_size)
        sftp.put(str(JAR), remote)
        sftp.put(str(ROOT / "deploy/nginx/liuhecai.conf"), "/etc/nginx/sites-available/liuhecai.conf")
        sftp.close()
        run(
            c,
            r"""
set -e
nginx -t && systemctl reload nginx
systemctl restart liuhecai-api
sleep 7
echo 'origin http Host=157465'
curl -sS -H 'Host: 157465.com' http://127.0.0.1/api/tenant/current | head -c 220; echo
echo 'origin https SNI'
curl -sk --resolve 157465.com:443:127.0.0.1 https://157465.com/api/tenant/current | head -c 220; echo
echo 'public'
curl -sS --max-time 15 https://157465.com/api/tenant/current
echo
curl -sS -o /dev/null -w 'entry:%{http_code}\n' --max-time 15 https://157465.com/
curl -sS -o /dev/null -w 'forum:%{http_code}\n' --max-time 15 https://585520.xyz/
# cleanup local demo domain on prod
. /etc/liuhecai/api.env
mysql -uliuhecai -p"$MYSQL_PASSWORD" -e "DELETE FROM liuhecai.domains WHERE host='entry.127.0.0.1';"
""",
            timeout=120,
        )
        return 0
    finally:
        c.close()


if __name__ == "__main__":
    raise SystemExit(main())
