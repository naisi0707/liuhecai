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
export PATH=/opt/node-v20.18.1/bin:/usr/local/bin:$PATH
rm -f /usr/local/bin/pnpm /usr/local/bin/pm2
# find real binaries
ls -la /opt/node-v20.18.1/bin/ | head
ls -la "$(npm root -g)/../bin" 2>/dev/null | head || true
PNPM=$(find /root /opt/node-v20.18.1 /usr/local -name pnpm -type f 2>/dev/null | head -1)
PM2=$(find /root /opt/node-v20.18.1 /usr/local -name pm2 -type f 2>/dev/null | head -1)
echo "PNPM=$PNPM"
echo "PM2=$PM2"
# npm global modules path
echo "npm root -g: $(npm root -g)"
ls "$(npm root -g)/pnpm/bin" 2>/dev/null || true
ls "$(npm root -g)/pm2/bin" 2>/dev/null || true
if [ -z "$PNPM" ]; then
  PNPM="$(npm root -g)/pnpm/bin/pnpm.cjs"
fi
if [ -z "$PM2" ]; then
  PM2="$(npm root -g)/pm2/bin/pm2"
fi
cat > /usr/local/bin/pnpm <<EOF
#!/bin/bash
exec /usr/local/bin/node "$(npm root -g)/pnpm/bin/pnpm.cjs" "\$@"
EOF
cat > /usr/local/bin/pm2 <<EOF
#!/bin/bash
exec /usr/local/bin/node "$(npm root -g)/pm2/bin/pm2" "\$@"
EOF
chmod +x /usr/local/bin/pnpm /usr/local/bin/pm2
pnpm -v
pm2 -v
node -v
""",
        )
        return code
    finally:
        c.close()


if __name__ == "__main__":
    raise SystemExit(main())
