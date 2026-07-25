#!/usr/bin/env python3
"""Fix ENTRY domain to 157465.com, nginx, cert, rebuild API cors via env."""
from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from ssh_util import connect, run

ROOT = Path(__file__).resolve().parents[2]


def main() -> int:
    c = connect()
    try:
        # DB
        run(
            c,
            r"""
set -e
. /etc/liuhecai/api.env
mysql -uliuhecai -p"$MYSQL_PASSWORD" liuhecai <<'SQL'
DELETE FROM domains WHERE host IN ('157456.com','www.157456.com','157465.com','www.157465.com');
INSERT INTO domains (id, tenant_id, host, is_primary, role, status) VALUES
(2102, 1001, '157465.com', 0, 'ENTRY', 1),
(2103, 1001, 'www.157465.com', 0, 'ENTRY', 1);
SELECT host, role, tenant_id FROM domains ORDER BY id;
SQL
""",
        )

        # Upload nginx WITHOUT 157465 ssl block first if cert missing — use temp conf
        # Strategy: upload HTTP-only entry first, certbot, then full conf
        sftp = c.open_sftp()
        sftp.put(str(ROOT / "deploy/nginx/liuhecai.conf"), "/tmp/liuhecai.conf.full")
        sftp.close()

        run(
            c,
            r"""
set -e
# If cert not yet present, strip 157465 ssl server block for first reload
if [ ! -f /etc/letsencrypt/live/157465.com/fullchain.pem ]; then
  awk '
    BEGIN{skip=0}
    /^# ENTRY camouflage/{skip=1}
    skip==1 && /^}$/{skip=0; next}
    skip==1{next}
    {print}
  ' /tmp/liuhecai.conf.full > /etc/nginx/sites-available/liuhecai.conf
else
  cp /tmp/liuhecai.conf.full /etc/nginx/sites-available/liuhecai.conf
fi
nginx -t
systemctl reload nginx

echo '=== origin tenant ==='
curl -sS -H 'Host: 157465.com' http://127.0.0.1/api/tenant/current
echo

# ACME
pkill -9 certbot 2>/dev/null || true
rm -f /var/lib/letsencrypt/.certbot.lock
mkdir -p /var/www/html
certbot certonly --webroot -w /var/www/html -n --agree-tos --register-unsafely-without-email -d 157465.com
echo CERT_EXIT:$?
ls -la /etc/letsencrypt/live/157465.com || true

# apply full nginx with ssl
if [ -f /etc/letsencrypt/live/157465.com/fullchain.pem ]; then
  cp /tmp/liuhecai.conf.full /etc/nginx/sites-available/liuhecai.conf
  # also redirect entry http -> https like forums
  # (keep http proxy for now; CF handles edge https)
  nginx -t && systemctl reload nginx
fi

# CORS update without rebuild: append env override
if ! grep -q LIUHECAI_CORS_ALLOWED_ORIGINS /etc/liuhecai/api.env; then
  cat >> /etc/liuhecai/api.env <<'EOF'
LIUHECAI_CORS_ALLOWED_ORIGINS=https://157465.com,https://www.157465.com,https://585520.xyz,https://785412.xyz,https://658951.xyz,https://152687.xyz,https://746528.xyz,https://admin.236841.xyz,https://agent.236841.xyz,http://157465.com,http://www.157465.com,http://585520.xyz,http://785412.xyz,http://658951.xyz,http://152687.xyz,http://746528.xyz,http://admin.236841.xyz,http://agent.236841.xyz
EOF
fi
systemctl restart liuhecai-api
sleep 6
curl -sS http://127.0.0.1:8080/api/health; echo

echo '=== public checks ==='
curl -sS --max-time 15 https://157465.com/api/health; echo
curl -sS --max-time 15 https://157465.com/api/tenant/current; echo
curl -sS --max-time 15 https://157465.com/ | tr '\n' ' ' | grep -oiE 'shop|camouflage|goto' | sort | uniq -c | head
curl -sS --max-time 15 https://585520.xyz/api/health; echo
""",
            timeout=300,
        )
        return 0
    finally:
        c.close()


if __name__ == "__main__":
    raise SystemExit(main())
