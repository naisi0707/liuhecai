#!/usr/bin/env python3
"""Resume after artifact upload: npm install, restart api/pm2, health checks."""
from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from ssh_util import connect, run

REMOTE = "/www/wwwroot/liuhecai"

CMD = rf"""
set -e
export PATH=/usr/local/bin:/opt/node-v20.18.1/bin:$PATH
for app in web admin agent; do
  cd {REMOTE}/apps/$app/.output/server
  if [ -f package.json ]; then
    npm install --omit=dev --no-progress --loglevel=error
  fi
done
cp {REMOTE}/deploy/nginx/liuhecai.conf /etc/nginx/sites-available/liuhecai.conf
nginx -t && systemctl reload nginx
cp {REMOTE}/deploy/systemd/liuhecai-api.service /etc/systemd/system/liuhecai-api.service
JAVA_BIN=$(command -v java)
sed -i "s|/usr/bin/java|$JAVA_BIN|" /etc/systemd/system/liuhecai-api.service
sed -i 's/User=www-data/User=root/' /etc/systemd/system/liuhecai-api.service
sed -i 's/Group=www-data/Group=root/' /etc/systemd/system/liuhecai-api.service
systemctl daemon-reload
systemctl restart liuhecai-api
cp {REMOTE}/deploy/pm2/ecosystem.config.cjs /etc/liuhecai/ecosystem.config.cjs
pm2 restart all || pm2 start /etc/liuhecai/ecosystem.config.cjs
pm2 save
sleep 8
systemctl --no-pager -l is-active liuhecai-api
curl -sS http://127.0.0.1:8080/api/health; echo
curl -sS -o /dev/null -w 'web:%{{http_code}}\n' http://127.0.0.1:3000/ || true
curl -sS -o /dev/null -w 'admin:%{{http_code}}\n' http://127.0.0.1:3001/ || true
curl -sS -o /dev/null -w 'agent:%{{http_code}}\n' http://127.0.0.1:3002/ || true
curl -sS -H 'Host: 585520.xyz' http://127.0.0.1/api/health; echo
curl -sS -o /dev/null -w 'nginx-forum:%{{http_code}}\n' -H 'Host: 585520.xyz' http://127.0.0.1/
. /etc/liuhecai/api.env
mysql -uliuhecai -p"$MYSQL_PASSWORD" -N -e "SHOW COLUMNS FROM liuhecai.topics LIKE 'tag';" || true
"""


def main() -> int:
    c = connect()
    try:
        code, _ = run(c, CMD, timeout=420)
        return code
    finally:
        c.close()


if __name__ == "__main__":
    raise SystemExit(main())
