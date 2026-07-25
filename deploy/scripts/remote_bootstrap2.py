#!/usr/bin/env python3
"""Resume package install after apt lock / timeouts."""
from __future__ import annotations

import sys
import time
from pathlib import Path

import paramiko

HOST = "45.152.64.102"
USER = "root"


def load_password() -> str:
    import os

    if os.environ.get("LIUHECAI_SSH_PASSWORD"):
        return os.environ["LIUHECAI_SSH_PASSWORD"]
    txt = Path(__file__).resolve().parents[2] / "docs" / "服务器.txt"
    lines = [ln.strip() for ln in txt.read_text(encoding="utf-8").splitlines() if ln.strip()]
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
        banner_timeout=60,
    )
    return c


def run(c: paramiko.SSHClient, cmd: str, timeout: int = 900) -> tuple[int, str, str]:
    print(">>>", cmd[:180].replace("\n", " "), flush=True)
    stdin, stdout, stderr = c.exec_command(cmd, timeout=timeout, get_pty=True)
    # stream until exit
    buf = []
    start = time.time()
    while True:
        if stdout.channel.exit_status_ready():
            break
        if stdout.channel.recv_ready():
            chunk = stdout.channel.recv(4096).decode("utf-8", "replace")
            buf.append(chunk)
            print(chunk, end="", flush=True)
        elif time.time() - start > timeout:
            stdout.channel.close()
            raise TimeoutError(cmd[:80])
        else:
            time.sleep(0.2)
    # drain
    while stdout.channel.recv_ready():
        chunk = stdout.channel.recv(4096).decode("utf-8", "replace")
        buf.append(chunk)
        print(chunk, end="", flush=True)
    code = stdout.channel.recv_exit_status()
    err = stderr.read().decode("utf-8", "replace") if not stderr.channel.closed else ""
    out = "".join(buf)
    print("\nexit", code, flush=True)
    return code, out, err


def wait_apt(c: paramiko.SSHClient) -> None:
    run(
        c,
        r"""
set +e
for i in $(seq 1 60); do
  if fuser /var/lib/dpkg/lock-frontend >/dev/null 2>&1 || fuser /var/lib/dpkg/lock >/dev/null 2>&1; then
    echo "apt busy $i"
    sleep 5
  else
    echo "apt free"
    exit 0
  fi
done
echo "force unlock"
killall apt-get apt dpkg 2>/dev/null
sleep 2
rm -f /var/lib/dpkg/lock-frontend /var/lib/dpkg/lock /var/cache/apt/archives/lock
dpkg --configure -a
exit 0
""",
        timeout=400,
    )


def main() -> int:
    c = connect()
    try:
        wait_apt(c)

        run(
            c,
            r"""
set -e
export DEBIAN_FRONTEND=noninteractive
apt-get install -y ca-certificates curl wget gnupg unzip git nginx
systemctl enable nginx
systemctl start nginx
nginx -v
""",
            timeout=400,
        )

        # JDK 17 tarball (more reliable than apt on 18.04)
        run(
            c,
            r"""
set -e
if java -version 2>&1 | grep -E 'version "17|version "21'; then
  java -version
  exit 0
fi
cd /opt
if [ ! -x /opt/jdk-17/bin/java ]; then
  rm -rf /opt/jdk-17
  mkdir -p /opt/jdk-17
  curl -fsSL -o /tmp/jdk17.tar.gz 'https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse'
  tar -xzf /tmp/jdk17.tar.gz -C /opt/jdk-17 --strip-components=1
  rm -f /tmp/jdk17.tar.gz
fi
update-alternatives --install /usr/bin/java java /opt/jdk-17/bin/java 1717
update-alternatives --install /usr/bin/javac javac /opt/jdk-17/bin/javac 1717
update-alternatives --set java /opt/jdk-17/bin/java
update-alternatives --set javac /opt/jdk-17/bin/javac
java -version
""",
            timeout=500,
        )

        # Maven
        run(
            c,
            r"""
set -e
if ! command -v mvn >/dev/null 2>&1; then
  cd /opt
  curl -fsSL -o /tmp/maven.tgz https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.tar.gz
  tar -xzf /tmp/maven.tgz -C /opt
  rm -f /tmp/maven.tgz
  rm -rf /opt/maven
  mv /opt/apache-maven-3.9.9 /opt/maven
  ln -sfn /opt/maven/bin/mvn /usr/local/bin/mvn
fi
mvn -v | head -3
""",
            timeout=300,
        )

        # Node 20 + pnpm + pm2
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
npm install -g pnpm@9 pm2
node -v
pnpm -v
pm2 -v
ln -sfn "$(command -v node)" /usr/local/bin/node
ln -sfn "$(command -v npm)" /usr/local/bin/npm
ln -sfn "$(command -v pnpm)" /usr/local/bin/pnpm
ln -sfn "$(command -v pm2)" /usr/local/bin/pm2
""",
            timeout=700,
        )

        # certbot
        run(
            c,
            r"""
set -e
export DEBIAN_FRONTEND=noninteractive
if ! command -v certbot >/dev/null 2>&1; then
  apt-get install -y certbot python3-certbot-nginx || true
fi
if ! command -v certbot >/dev/null 2>&1; then
  pip3 install -U pip || true
  pip3 install certbot certbot-nginx || true
fi
certbot --version || echo 'certbot missing'
""",
            timeout=400,
        )

        # MySQL tune + ensure running
        run(
            c,
            r"""
set -e
systemctl start mysql || true
CONF=/etc/mysql/mysql.conf.d/mysqld.cnf
if [ -f "$CONF" ] && ! grep -q 'innodb_buffer_pool_size' "$CONF"; then
  printf '\n[mysqld]\ninnodb_buffer_pool_size = 256M\nmax_connections = 100\n' >> "$CONF"
  systemctl restart mysql
fi
mysql --version
""",
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
ss -lntp | grep -E ':80|:443|:3306' || true
""",
        )
        return 0
    finally:
        c.close()


if __name__ == "__main__":
    sys.exit(main())
