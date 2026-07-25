#!/usr/bin/env python3
from __future__ import annotations

import sys
import time
from pathlib import Path

import paramiko

HOST = "45.152.64.102"


def load_password() -> str:
    import os

    if os.environ.get("LIUHECAI_SSH_PASSWORD"):
        return os.environ["LIUHECAI_SSH_PASSWORD"]
    txt = Path(__file__).resolve().parents[2] / "docs" / "服务器.txt"
    lines = [ln.strip() for ln in txt.read_text(encoding="utf-8").splitlines() if ln.strip()]
    return lines[-1]


def run(c, cmd, timeout=700):
    print(">>>", cmd[:160].replace("\n", " "), flush=True)
    _, stdout, stderr = c.exec_command(cmd, timeout=timeout, get_pty=True)
    buf = []
    start = time.time()
    while not stdout.channel.exit_status_ready():
        if stdout.channel.recv_ready():
            chunk = stdout.channel.recv(4096).decode("utf-8", "replace")
            buf.append(chunk)
            print(chunk, end="", flush=True)
        elif time.time() - start > timeout:
            raise TimeoutError(cmd[:80])
        else:
            time.sleep(0.2)
    while stdout.channel.recv_ready():
        chunk = stdout.channel.recv(4096).decode("utf-8", "replace")
        buf.append(chunk)
        print(chunk, end="", flush=True)
    code = stdout.channel.recv_exit_status()
    print("\nexit", code, flush=True)
    return code


def main() -> int:
    c = paramiko.SSHClient()
    c.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    c.connect(HOST, username="root", password=load_password(), timeout=30, allow_agent=False, look_for_keys=False)
    try:
        code = run(
            c,
            r"""
set -e
export NVM_DIR=/root/.nvm
. "$NVM_DIR/nvm.sh"
nvm install 20
nvm use 20
nvm alias default 20
hash -r
which node
node -v
npm install -g pnpm@9 pm2
which pnpm
pnpm -v
pm2 -v
# hardlink into /usr/local for non-interactive shells
NODE_BIN="$(nvm which current | xargs dirname)"
ln -sfn "$NODE_BIN/node" /usr/local/bin/node
ln -sfn "$NODE_BIN/npm" /usr/local/bin/npm
ln -sfn "$NODE_BIN/npx" /usr/local/bin/npx
ln -sfn "$NODE_BIN/pnpm" /usr/local/bin/pnpm || ln -sfn "$(which pnpm)" /usr/local/bin/pnpm
ln -sfn "$NODE_BIN/pm2" /usr/local/bin/pm2 || ln -sfn "$(which pm2)" /usr/local/bin/pm2
# also common npm global bin
NPM_GLOBAL="$(npm root -g)/../bin"
[ -x "$NPM_GLOBAL/pnpm" ] && ln -sfn "$NPM_GLOBAL/pnpm" /usr/local/bin/pnpm
[ -x "$NPM_GLOBAL/pm2" ] && ln -sfn "$NPM_GLOBAL/pm2" /usr/local/bin/pm2
/usr/local/bin/node -v
/usr/local/bin/pnpm -v
/usr/local/bin/pm2 -v
""",
        )
        return code
    finally:
        c.close()


if __name__ == "__main__":
    sys.exit(main())
