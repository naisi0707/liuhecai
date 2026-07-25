#!/usr/bin/env python3
"""Upload built artifacts and configure DB/nginx/systemd/pm2."""
from __future__ import annotations

import os
import secrets
import string
import sys
import tarfile
import tempfile
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from ssh_util import HOST, connect, load_password, run

ROOT = Path(__file__).resolve().parents[2]
REMOTE_ROOT = "/www/wwwroot/liuhecai"


def make_archive() -> Path:
    tmp = Path(tempfile.gettempdir()) / f"liuhecai-deploy-{int(time.time())}.tar.gz"
    include_dirs = [
        "apps/api/src/main/resources",
        "apps/api/target",
        "apps/web/.output",
        "apps/admin/.output",
        "apps/agent/.output",
        "deploy",
        "packages/shared",
        "docs",
    ]
    include_files = [
        "package.json",
        "pnpm-workspace.yaml",
        "pnpm-lock.yaml",
        ".npmrc",
        "README.md",
    ]
    print("Creating archive", tmp)
    with tarfile.open(tmp, "w:gz") as tar:
        for rel in include_dirs:
            p = ROOT / rel
            if p.exists():
                tar.add(p, arcname=rel)
        for rel in include_files:
            p = ROOT / rel
            if p.exists():
                tar.add(p, arcname=rel)
        # ensure uploads keep
        keep = ROOT / "uploads" / ".gitkeep"
        if keep.exists():
            tar.add(keep, arcname="uploads/.gitkeep")
    print("Archive size MB", round(tmp.stat().st_size / 1024 / 1024, 2))
    return tmp


def sftp_put(c, local: Path, remote: str) -> None:
    sftp = c.open_sftp()
    try:
        print("Uploading", local, "->", remote)
        # progress-ish by chunked put
        sftp.put(str(local), remote)
    finally:
        sftp.close()


def gen_secret(n: int = 48) -> str:
    alphabet = string.ascii_letters + string.digits
    return "".join(secrets.choice(alphabet) for _ in range(n))


def main() -> int:
    archive = make_archive()
    mysql_password = gen_secret(24)
    jwt_secret = gen_secret(64)

    c = connect()
    try:
        run(c, "mkdir -p /www/wwwroot /tmp /etc/liuhecai /www/wwwroot/liuhecai/uploads")
        sftp_put(c, archive, "/tmp/liuhecai-deploy.tar.gz")

        run(
            c,
            rf"""
set -e
rm -rf {REMOTE_ROOT}.bak
if [ -d {REMOTE_ROOT} ]; then mv {REMOTE_ROOT} {REMOTE_ROOT}.bak; fi
mkdir -p {REMOTE_ROOT}
tar -xzf /tmp/liuhecai-deploy.tar.gz -C {REMOTE_ROOT}
mkdir -p {REMOTE_ROOT}/uploads
chown -R www-data:www-data {REMOTE_ROOT}/uploads || chown -R nginx:nginx {REMOTE_ROOT}/uploads || true
# ensure jar path
ls -lh {REMOTE_ROOT}/apps/api/target/*.jar
ls {REMOTE_ROOT}/apps/web/.output/server/index.mjs
ls {REMOTE_ROOT}/apps/admin/.output/server/index.mjs
ls {REMOTE_ROOT}/apps/agent/.output/server/index.mjs
""",
            timeout=180,
        )

        # MySQL setup — unix_socket root; MySQL 5.7 compatible
        run(
            c,
            rf"""
set -e
mysql -uroot <<SQL
CREATE DATABASE IF NOT EXISTS liuhecai DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
GRANT ALL PRIVILEGES ON liuhecai.* TO 'liuhecai'@'localhost' IDENTIFIED BY '{mysql_password}';
FLUSH PRIVILEGES;
SQL
""",
        )

        # Import schemas in order
        run(
            c,
            rf"""
set -e
DB={REMOTE_ROOT}/apps/api/src/main/resources/db
mysql -uliuhecai -p'{mysql_password}' liuhecai < "$DB/schema-m2.sql"
for f in schema-m3.sql schema-m6.sql schema-m7.sql schema-m8.sql schema-m11-cms.sql schema-m12-ops.sql schema-m13-topic-detail.sql schema-m14-domain-role.sql; do
  if [ -f "$DB/$f" ]; then
    echo "IMPORT $f"
    mysql -uliuhecai -p'{mysql_password}' liuhecai < "$DB/$f" || echo "WARN $f had errors (may be ok)"
  fi
done
mysql -uliuhecai -p'{mysql_password}' liuhecai < "$DB/schema-prod-domains.sql"
for f in seed-web-theme.sql seed-web-topics.sql; do
  if [ -f "$DB/$f" ]; then
    echo "SEED $f"
    mysql -uliuhecai -p'{mysql_password}' liuhecai < "$DB/$f" || true
  fi
done
mysql -uliuhecai -p'{mysql_password}' -e "USE liuhecai; SELECT id,name FROM tenants; SELECT host,role,tenant_id FROM domains;"
""",
            timeout=300,
        )

        # api.env
        run(
            c,
            rf"""
set -e
umask 077
cat > /etc/liuhecai/api.env <<EOF
SPRING_PROFILES_ACTIVE=prod
MYSQL_USER=liuhecai
MYSQL_PASSWORD={mysql_password}
LIUHECAI_JWT_SECRET={jwt_secret}
EOF
chmod 600 /etc/liuhecai/api.env
# also store for operator (root only)
cp /etc/liuhecai/api.env /root/liuhecai-api.env
chmod 600 /root/liuhecai-api.env
""",
        )

        # nginx
        run(
            c,
            rf"""
set -e
cp {REMOTE_ROOT}/deploy/nginx/liuhecai.conf /etc/nginx/sites-available/liuhecai.conf
ln -sfn /etc/nginx/sites-available/liuhecai.conf /etc/nginx/sites-enabled/liuhecai.conf
rm -f /etc/nginx/sites-enabled/default
nginx -t
systemctl reload nginx
""",
        )

        # systemd — java path may be temurin
        run(
            c,
            rf"""
set -e
JAVA_BIN=$(command -v java)
# rewrite ExecStart to absolute java if needed
cp {REMOTE_ROOT}/deploy/systemd/liuhecai-api.service /etc/systemd/system/liuhecai-api.service
sed -i "s|/usr/bin/java|$JAVA_BIN|" /etc/systemd/system/liuhecai-api.service
# run as root if www-data cannot read jar (simpler on BT box)
sed -i 's/User=www-data/User=root/' /etc/systemd/system/liuhecai-api.service
sed -i 's/Group=www-data/Group=root/' /etc/systemd/system/liuhecai-api.service
# bind API to localhost only via JVM not needed; firewall later
systemctl daemon-reload
systemctl enable liuhecai-api
systemctl restart liuhecai-api
sleep 8
systemctl --no-pager -l status liuhecai-api | head -40
curl -sS http://127.0.0.1:8080/api/health || true
""",
            timeout=120,
        )

        # pm2 — install node_modules for .output if needed (nitro may vendor deps)
        run(
            c,
            rf"""
set -e
export PATH=/usr/local/bin:/opt/node-v20.18.1/bin:$PATH
# ensure each .output/server has its package deps
for app in web admin agent; do
  cd {REMOTE_ROOT}/apps/$app/.output/server
  if [ -f package.json ]; then
    /usr/local/bin/npm install --omit=dev || true
  fi
done
cd {REMOTE_ROOT}
# fix ecosystem node interpreter
cp {REMOTE_ROOT}/deploy/pm2/ecosystem.config.cjs /etc/liuhecai/ecosystem.config.cjs
pm2 delete all || true
pm2 start /etc/liuhecai/ecosystem.config.cjs
pm2 save
pm2 startup systemd -u root --hp /root | tail -5
(pm2 startup systemd -u root --hp /root | grep -E 'sudo|systemctl' | bash) || true
pm2 status
curl -sS -o /dev/null -w 'web:%{{http_code}}\n' http://127.0.0.1:3000/ || true
curl -sS -o /dev/null -w 'admin:%{{http_code}}\n' http://127.0.0.1:3001/ || true
curl -sS -o /dev/null -w 'agent:%{{http_code}}\n' http://127.0.0.1:3002/ || true
""",
            timeout=300,
        )

        # via nginx host header
        run(
            c,
            r"""
set -e
curl -sS -H 'Host: 585520.xyz' http://127.0.0.1/api/health
echo
curl -sS -H 'Host: 585520.xyz' http://127.0.0.1/api/tenant/current | head -c 500
echo
curl -sS -H 'Host: 157456.com' http://127.0.0.1/api/tenant/current | head -c 500
echo
curl -sS -o /dev/null -w 'nginx-web:%{http_code}\n' -H 'Host: 585520.xyz' http://127.0.0.1/
""",
        )

        print("\n=== SECRETS stored on server: /etc/liuhecai/api.env and /root/liuhecai-api.env ===")
        return 0
    finally:
        c.close()
        try:
            archive.unlink()
        except OSError:
            pass


if __name__ == "__main__":
    raise SystemExit(main())
