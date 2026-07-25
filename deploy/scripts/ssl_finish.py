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
# clear stale certbot lock
pkill -9 certbot 2>/dev/null
rm -f /var/lib/letsencrypt/.certbot.lock /tmp/.certbot.lock
sleep 1

echo '=== existing certs ==='
ls -la /etc/letsencrypt/live

# finish missing forum cert
if [ ! -d /etc/letsencrypt/live/746528.xyz ]; then
  certbot --nginx -n --agree-tos --register-unsafely-without-email -d 746528.xyz
  echo EXIT_746528:$?
fi

# ensure nginx has ssl server blocks — certbot usually rewrites config
grep -R "ssl_certificate" /etc/nginx/sites-enabled/ -n || true
grep -R "listen 443" /etc/nginx/sites-enabled/ -n || true
cat /etc/nginx/sites-enabled/liuhecai.conf | head -120

# verify https locally with SNI if certs exist
for d in 585520.xyz 785412.xyz 658951.xyz 152687.xyz 746528.xyz; do
  if [ -d /etc/letsencrypt/live/$d ]; then
    code=$(curl -sk -o /dev/null -w '%{http_code}' --resolve $d:443:127.0.0.1 https://$d/api/health)
    echo "https $d -> $code"
  fi
done

# camouflage marker check via host header http
curl -sS -H 'Host: 157456.com' http://127.0.0.1/ | grep -oE 'camouflage|發發發|线路选择|/goto' | head -10 || true
""",
            timeout=300,
        )
        return 0
    finally:
        c.close()


if __name__ == "__main__":
    raise SystemExit(main())
