#!/usr/bin/env python3
import json
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
echo '=== tenant roles ==='
for h in 585520.xyz 157456.com 785412.xyz 658951.xyz 152687.xyz 746528.xyz; do
  echo "HOST=$h"
  curl -sS -H "Host: $h" http://127.0.0.1/api/tenant/current | python3 -c 'import sys,json; d=json.load(sys.stdin)["data"]; print(d.get("name"), d.get("domainRole"), d.get("forumHost"))'
done
echo '=== pages ==='
curl -sS -o /dev/null -w 'forum:%{http_code}\n' -H 'Host: 585520.xyz' http://127.0.0.1/
curl -sS -o /dev/null -w 'entry:%{http_code}\n' -H 'Host: 157456.com' http://127.0.0.1/
curl -sS -o /dev/null -w 'admin:%{http_code}\n' -H 'Host: admin.157465.com' http://127.0.0.1/
curl -sS -o /dev/null -w 'agent:%{http_code}\n' -H 'Host: agent.157465.com' http://127.0.0.1/
# entry page should contain camouflage markers
curl -sS -H 'Host: 157456.com' http://127.0.0.1/ | tr '\n' ' ' | grep -oE '.{0,40}(發發發|camouflage|线路|goto).{0,40}' | head -5 || echo 'no camouflage marker found in html snippet'
""",
            timeout=120,
        )

        # Check public IP and DNS from server POV
        run(
            c,
            r"""
echo '=== public ip ==='
curl -sS ifconfig.me || curl -sS ip.sb || true
echo
echo '=== dig from server ==='
for h in 157456.com 585520.xyz admin.157465.com; do
  echo -n "$h -> "
  getent hosts "$h" | awk '{print $1}' | head -1 || true
done
""",
        )

        # Attempt certbot; may fail if DNS not pointing here
        code, out = run(
            c,
            r"""
set +e
# ensure nginx site ready
nginx -t && systemctl reload nginx
certbot --nginx -n --agree-tos --register-unsafely-without-email \
  -d 157456.com -d www.157456.com \
  -d 585520.xyz -d 785412.xyz -d 658951.xyz -d 152687.xyz -d 746528.xyz \
  -d admin.157465.com -d agent.157465.com
echo CERTBOT_EXIT:$?
""",
            timeout=300,
        )
        return 0
    finally:
        c.close()


if __name__ == "__main__":
    raise SystemExit(main())
