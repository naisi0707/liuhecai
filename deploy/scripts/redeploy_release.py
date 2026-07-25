#!/usr/bin/env python3
"""Incremental prod redeploy: upload jar + Nuxt outputs, keep api.env / MySQL intact."""
from __future__ import annotations

import sys
import tarfile
import tempfile
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from ssh_util import connect, run

ROOT = Path(__file__).resolve().parents[2]
REMOTE = "/www/wwwroot/liuhecai"
JAR_NAME = "liuhecai-api-0.0.1-SNAPSHOT.jar"


def make_archive() -> Path:
    required = [
        ROOT / "apps/api/target" / JAR_NAME,
        ROOT / "apps/web/.output/server/index.mjs",
        ROOT / "apps/admin/.output/server/index.mjs",
        ROOT / "apps/agent/.output/server/index.mjs",
    ]
    for p in required:
        if not p.exists():
            raise SystemExit(f"missing build artifact: {p}")

    tmp = Path(tempfile.gettempdir()) / f"liuhecai-release-{int(time.time())}.tar.gz"
    include = [
        "apps/api/target/" + JAR_NAME,
        "apps/api/src/main/resources/db",
        "apps/web/.output",
        "apps/admin/.output",
        "apps/agent/.output",
        "packages/shared",
        "deploy/pm2/ecosystem.config.cjs",
        "deploy/systemd/liuhecai-api.service",
        "deploy/nginx/liuhecai.conf",
    ]
    print("Creating", tmp)
    with tarfile.open(tmp, "w:gz") as tar:
        for rel in include:
            p = ROOT / rel
            if p.exists():
                tar.add(p, arcname=rel)
    print("Archive MB", round(tmp.stat().st_size / 1024 / 1024, 2))
    return tmp


def main() -> int:
    archive = make_archive()
    c = connect()
    try:
        sftp = c.open_sftp()
        remote_tar = "/tmp/liuhecai-release.tar.gz"
        print("Uploading", archive, "->", remote_tar)
        sftp.put(str(archive), remote_tar)
        sftp.close()

        run(
            c,
            rf"""
set -e
export PATH=/usr/local/bin:/opt/node-v20.18.1/bin:$PATH
cd {REMOTE}
# replace app artifacts only — keep uploads / api.env
rm -rf apps/web/.output apps/admin/.output apps/agent/.output
mkdir -p apps/api/target apps/api/src/main/resources
tar -xzf /tmp/liuhecai-release.tar.gz -C {REMOTE}
ls -lh apps/api/target/{JAR_NAME}
ls apps/web/.output/server/index.mjs apps/admin/.output/server/index.mjs apps/agent/.output/server/index.mjs
# apply m16 if present (idempotent)
if [ -f apps/api/src/main/resources/db/schema-m16-topic-tag.sql ]; then
  . /etc/liuhecai/api.env
  mysql -uliuhecai -p"$MYSQL_PASSWORD" liuhecai < apps/api/src/main/resources/db/schema-m16-topic-tag.sql \
    && echo 'm16 applied' || echo 'm16 skip/warn (column may exist)'
fi
# npm deps for nitro outputs
for app in web admin agent; do
  cd {REMOTE}/apps/$app/.output/server
  if [ -f package.json ]; then
    npm install --omit=dev
  fi
done
# nginx/systemd/pm2 config refresh (no secret rewrite)
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
curl -sS http://127.0.0.1:8080/api/health || true
echo
curl -sS -o /dev/null -w 'web:%{{http_code}}\n' http://127.0.0.1:3000/ || true
curl -sS -o /dev/null -w 'admin:%{{http_code}}\n' http://127.0.0.1:3001/ || true
curl -sS -o /dev/null -w 'agent:%{{http_code}}\n' http://127.0.0.1:3002/ || true
curl -sS -H 'Host: 585520.xyz' http://127.0.0.1/api/health
echo
curl -sS -o /dev/null -w 'nginx-forum:%{{http_code}}\n' -H 'Host: 585520.xyz' http://127.0.0.1/
# confirm topics.tag column
. /etc/liuhecai/api.env
mysql -uliuhecai -p"$MYSQL_PASSWORD" -N -e "SHOW COLUMNS FROM liuhecai.topics LIKE 'tag';" || true
""",
            timeout=420,
        )
        print("\n=== redeploy_release done (api.env untouched) ===")
        return 0
    finally:
        c.close()
        try:
            archive.unlink()
        except OSError:
            pass


if __name__ == "__main__":
    raise SystemExit(main())
