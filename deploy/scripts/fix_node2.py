#!/usr/bin/env python3
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from ssh_util import connect, run


def main() -> int:
    c = connect()
    try:
        code, _ = run(
            c,
            r"""
set -e
mv /root/.npmrc /root/.npmrc.bak 2>/dev/null || true
export NVM_DIR=/root/.nvm
. "$NVM_DIR/nvm.sh"
nvm use --delete-prefix v20.20.2 --silent || true
nvm use 20
nvm alias default 20
node -v
npm -v
npm install -g pnpm@9 pm2
NODE_BIN=$(dirname $(nvm which current))
echo NODE_BIN=$NODE_BIN
ln -sfn "$NODE_BIN/node" /usr/local/bin/node
ln -sfn "$NODE_BIN/npm" /usr/local/bin/npm
ln -sfn "$NODE_BIN/npx" /usr/local/bin/npx
ln -sfn "$(command -v pnpm)" /usr/local/bin/pnpm
ln -sfn "$(command -v pm2)" /usr/local/bin/pm2
/usr/local/bin/node -v
/usr/local/bin/pnpm -v
/usr/local/bin/pm2 -v
""",
            timeout=400,
        )
        return code
    finally:
        c.close()


if __name__ == "__main__":
    raise SystemExit(main())
