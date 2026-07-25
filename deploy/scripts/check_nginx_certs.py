#!/usr/bin/env python3
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
set -e
echo '=== remote nginx server_name/ssl lines ==='
grep -nE 'server_name|ssl_certificate |listen 443' /etc/nginx/sites-enabled/liuhecai.conf
echo '=== cert subjects ==='
for d in 585520.xyz 785412.xyz 658951.xyz 152687.xyz 746528.xyz; do
  echo -n "$d file: "
  openssl x509 -in /etc/letsencrypt/live/$d/fullchain.pem -noout -subject
done
echo '=== nginx -T ssl servers ==='
nginx -T 2>/dev/null | grep -E 'server_name|ssl_certificate |listen 443' | head -80
""",
        )
        return 0
    finally:
        c.close()


if __name__ == "__main__":
    raise SystemExit(main())
