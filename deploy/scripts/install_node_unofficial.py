#!/usr/bin/env python3
"""Install Node 20 unofficial build (glibc 2.17) for Ubuntu 18.04."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from ssh_util import connect, run


def main() -> int:
    c = connect()
    try:
        code, out = run(
            c,
            r"""
set -e
VER=v20.18.1
ARCH=x64
BASE=/opt/node-$VER
if [ ! -x "$BASE/bin/node" ]; then
  curl -fsSL -o /tmp/node.tgz \
    "https://unofficial-builds.nodejs.org/download/release/${VER}/node-${VER}-linux-${ARCH}-glibc-217.tar.gz"
  rm -rf "$BASE"
  mkdir -p "$BASE"
  tar -xzf /tmp/node.tgz -C "$BASE" --strip-components=1
  rm -f /tmp/node.tgz
fi
export PATH=/usr/local/bin:$BASE/bin:$PATH
ln -sfn "$BASE/bin/node" /usr/local/bin/node
ln -sfn "$BASE/bin/npm" /usr/local/bin/npm
ln -sfn "$BASE/bin/npx" /usr/local/bin/npx
/usr/local/bin/node -v
/usr/local/bin/npm install -g pnpm@9 pm2
# npm global bin may be under BASE or /usr/local
for d in "$BASE/bin" /usr/local/bin "$(/usr/local/bin/npm root -g)/../bin"; do
  [ -x "$d/pnpm" ] && ln -sfn "$d/pnpm" /usr/local/bin/pnpm
  [ -x "$d/pm2" ] && ln -sfn "$d/pm2" /usr/local/bin/pm2
done
/usr/local/bin/node -v
/usr/local/bin/pnpm -v
/usr/local/bin/pm2 -v
""",
            timeout=500,
        )
        return code
    finally:
        c.close()


if __name__ == "__main__":
    raise SystemExit(main())
