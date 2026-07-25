#!/usr/bin/env python3
"""Public + origin recheck for entry/forum domains."""
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
echo '=== origin IP ==='
curl -4 -sS ifconfig.me; echo

echo '=== local Host checks ==='
for h in 157456.com 157465.com www.157456.com 585520.xyz; do
  echo -n "http Host=$h tenant: "
  curl -sS -H "Host: $h" http://127.0.0.1/api/tenant/current | head -c 180
  echo
done

echo '=== public curl from server ==='
for u in \
  https://157456.com/api/health \
  https://157465.com/api/health \
  http://157456.com/api/health \
  http://157465.com/api/health \
  https://585520.xyz/api/health \
  https://785412.xyz/api/health
do
  echo -n "$u -> "
  curl -sS -o /tmp/curl_body -w '%{http_code} ip:%{remote_ip} redir:%{redirect_url}' -L --max-time 15 "$u"
  echo
  head -c 120 /tmp/curl_body; echo
done

echo '=== dig ==='
for h in 157456.com 157465.com 585520.xyz admin.236841.xyz; do
  echo -n "$h A: "
  getent ahostsv4 "$h" | awk '{print $1}' | sort -u | tr '\n' ' '
  echo
done
""",
            timeout=120,
        )
        return 0
    finally:
        c.close()


if __name__ == "__main__":
    raise SystemExit(main())
