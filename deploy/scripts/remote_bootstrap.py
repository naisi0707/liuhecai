#!/usr/bin/env python3
"""Bootstrap Ubuntu 18.04 host for liuhecai production."""
from __future__ import annotations

import sys
import time

import paramiko

HOST = "45.152.64.102"
USER = "root"


def load_password() -> str:
    import os
    from pathlib import Path

    if os.environ.get("LIUHECAI_SSH_PASSWORD"):
        return os.environ["LIUHECAI_SSH_PASSWORD"]
    txt = Path(__file__).resolve().parents[2] / "docs" / "服务器.txt"
    lines = [ln.strip() for ln in txt.read_text(encoding="utf-8").splitlines() if ln.strip()]
    # last non-empty line is password per docs/服务器.txt layout
    return lines[-1]


def connect() -> paramiko.SSHClient:
    c = paramiko.SSHClient()
    c.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    c.connect(
        HOST,
        username=USER,
        password=load_password(),
        timeout=30,
        allow_agent=False,
        look_for_keys=False,
    )
    return c


def run(c: paramiko.SSHClient, cmd: str, timeout: int = 600) -> tuple[int, str, str]:
    print(">>>", cmd[:200].replace("\n", " "))
    stdin, stdout, stderr = c.exec_command(cmd, timeout=timeout)
    out = stdout.read().decode("utf-8", "replace")
    err = stderr.read().decode("utf-8", "replace")
    code = stdout.channel.recv_exit_status()
    if out:
        print(out[-5000:])
    if err:
        print("STDERR:", err[-2500:])
    print("exit", code)
    return code, out, err


def main() -> int:
    c = connect()
    try:
        run(c, "swapon --show; free -h")
        run(
            c,
            r"""
set -e
if [ ! -f /swapfile ]; then
  fallocate -l 2G /swapfile || dd if=/dev/zero of=/swapfile bs=1M count=2048
  chmod 600 /swapfile
  mkswap /swapfile
  swapon /swapfile
  grep -q '/swapfile' /etc/fstab || echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi
swapon --show
free -h
""",
        )

        # Ubuntu 18.04 may still use archive; try update first
        run(
            c,
            r"""
set -e
export DEBIAN_FRONTEND=noninteractive
apt-get update -y || {
  sed -i 's|http://archive.ubuntu.com/ubuntu|http://old-releases.ubuntu.com/ubuntu|g' /etc/apt/sources.list
  sed -i 's|http://security.ubuntu.com/ubuntu|http://old-releases.ubuntu.com/ubuntu|g' /etc/apt/sources.list
  sed -i 's|http://.*.ubuntu.com/ubuntu|http://old-releases.ubuntu.com/ubuntu|g' /etc/apt/sources.list
  apt-get update -y
}
""",
            timeout=300,
        )

        run(
            c,
            r"""
set -e
export DEBIAN_FRONTEND=noninteractive
apt-get install -y ca-certificates curl wget gnupg lsb-release software-properties-common apt-transport-https unzip git
""",
            timeout=300,
        )

        # Nginx
        run(
            c,
            r"""
set -e
export DEBIAN_FRONTEND=noninteractive
apt-get install -y nginx
systemctl enable nginx
systemctl start nginx
nginx -v
""",
            timeout=300,
        )

        # MySQL 8 from Oracle APT (fallback to mysql-server package)
        run(
            c,
            r"""
set -e
export DEBIAN_FRONTEND=noninteractive
if ! command -v mysqld >/dev/null 2>&1; then
  apt-get install -y mysql-server || apt-get install -y mariadb-server
fi
systemctl enable mysql || systemctl enable mariadb || true
systemctl start mysql || systemctl start mariadb || true
mysql --version || true
""",
            timeout=600,
        )

        # Tune MySQL buffer if present
        run(
            c,
            r"""
set -e
CONF=/etc/mysql/mysql.conf.d/mysqld.cnf
ALT=/etc/mysql/mariadb.conf.d/50-server.cnf
TARGET=""
[ -f "$CONF" ] && TARGET="$CONF"
[ -z "$TARGET" ] && [ -f "$ALT" ] && TARGET="$ALT"
if [ -n "$TARGET" ]; then
  if ! grep -q 'innodb_buffer_pool_size' "$TARGET"; then
    echo -e '\n[mysqld]\ninnodb_buffer_pool_size = 256M\nmax_connections = 100' >> "$TARGET"
  fi
  systemctl restart mysql || systemctl restart mariadb || true
fi
""",
        )

        # Temurin JDK 17
        run(
            c,
            r"""
set -e
export DEBIAN_FRONTEND=noninteractive
if ! java -version 2>&1 | grep -q '17'; then
  mkdir -p /etc/apt/keyrings
  wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public | gpg --dearmor -o /etc/apt/keyrings/adoptium.gpg
  echo "deb [signed-by=/etc/apt/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print$2}' /etc/os-release) main" > /etc/apt/sources.list.d/adoptium.list
  apt-get update -y
  apt-get install -y temurin-17-jdk || true
fi
java -version || true
""",
            timeout=400,
        )

        # Fallback JDK via direct tarball if adoptium failed
        run(
            c,
            r"""
set -e
if ! java -version 2>&1 | grep -q '17\|21\|11'; then
  cd /opt
  if [ ! -d /opt/jdk-17 ]; then
    curl -fsSL -o jdk17.tar.gz 'https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.13%2B11/OpenJDK17U-jdk_x64_linux_hotspot_17.0.13_11.tar.gz' \
      || curl -fsSL -o jdk17.tar.gz 'https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse'
    mkdir -p /opt/jdk-17
    tar -xzf jdk17.tar.gz -C /opt/jdk-17 --strip-components=1
    rm -f jdk17.tar.gz
  fi
  update-alternatives --install /usr/bin/java java /opt/jdk-17/bin/java 1717
  update-alternatives --install /usr/bin/javac javac /opt/jdk-17/bin/javac 1717
  update-alternatives --set java /opt/jdk-17/bin/java
  update-alternatives --set javac /opt/jdk-17/bin/javac
fi
java -version
""",
            timeout=400,
        )

        # Maven
        run(
            c,
            r"""
set -e
if ! command -v mvn >/dev/null 2>&1; then
  cd /opt
  if [ ! -d /opt/maven ]; then
    curl -fsSL -o maven.tgz https://dlcdn.apache.org/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.tar.gz \
      || curl -fsSL -o maven.tgz https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.tar.gz
    tar -xzf maven.tgz
    mv apache-maven-3.9.9 /opt/maven
    rm -f maven.tgz
    ln -sf /opt/maven/bin/mvn /usr/local/bin/mvn
  fi
fi
mvn -v | head -3
""",
            timeout=300,
        )

        # Node 20 via nvm
        run(
            c,
            r"""
set -e
export NVM_DIR=/root/.nvm
if [ ! -s "$NVM_DIR/nvm.sh" ]; then
  curl -fsSL https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.1/install.sh | bash
fi
. "$NVM_DIR/nvm.sh"
nvm install 20
nvm alias default 20
node -v
npm -v
npm install -g pnpm@9 pm2
pnpm -v
pm2 -v
# ensure node in path for systemd-less usage
ln -sfn "$(command -v node)" /usr/local/bin/node
ln -sfn "$(command -v npm)" /usr/local/bin/npm
ln -sfn "$(command -v pnpm)" /usr/local/bin/pnpm
ln -sfn "$(command -v pm2)" /usr/local/bin/pm2
""",
            timeout=600,
        )

        # certbot via snap or pip
        run(
            c,
            r"""
set -e
export DEBIAN_FRONTEND=noninteractive
if ! command -v certbot >/dev/null 2>&1; then
  if command -v snap >/dev/null 2>&1; then
    snap install core || true
    snap refresh core || true
    snap install --classic certbot || true
    ln -sfn /snap/bin/certbot /usr/bin/certbot || true
  fi
fi
if ! command -v certbot >/dev/null 2>&1; then
  apt-get install -y certbot python3-certbot-nginx || pip3 install certbot certbot-nginx
fi
certbot --version || true
""",
            timeout=400,
        )

        run(
            c,
            r"""
echo '=== SUMMARY ==='
java -version 2>&1 | head -1
mvn -v 2>&1 | head -1
node -v
pnpm -v
nginx -v 2>&1
mysql --version 2>&1 | head -1
certbot --version 2>&1 | head -1
swapon --show
ss -lntp | grep -E ':80|:443|:3306' || true
""",
        )
        return 0
    finally:
        c.close()


if __name__ == "__main__":
    sys.exit(main())
