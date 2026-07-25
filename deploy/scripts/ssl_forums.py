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
set +e
# Try forum domains one-by-one; Cloudflare orange-cloud may still work if origin is this host
for d in 585520.xyz 785412.xyz 658951.xyz 152687.xyz 746528.xyz; do
  echo "=== cert $d ==="
  certbot --nginx -n --agree-tos --register-unsafely-without-email -d "$d"
  echo EXIT:$?
done
ls -la /etc/letsencrypt/live 2>/dev/null || echo 'no certs yet'
nginx -t && systemctl reload nginx
""",
            timeout=600,
        )
        return 0
    finally:
        c.close()


if __name__ == "__main__":
    raise SystemExit(main())
