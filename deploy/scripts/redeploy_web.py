#!/usr/bin/env python3
"""Upload rebuilt web .output and restart pm2 web."""
from __future__ import annotations

import sys
import tarfile
import tempfile
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from ssh_util import connect, run

ROOT = Path(__file__).resolve().parents[2]


def main() -> int:
    tmp = Path(tempfile.gettempdir()) / f"liuhecai-web-{int(time.time())}.tar.gz"
    with tarfile.open(tmp, "w:gz") as tar:
        tar.add(ROOT / "apps/web/.output", arcname="apps/web/.output")
        tar.add(ROOT / "packages/shared/src", arcname="packages/shared/src")
    print("archive", tmp, "MB", round(tmp.stat().st_size / 1024 / 1024, 2))

    c = connect()
    try:
        sftp = c.open_sftp()
        sftp.put(str(tmp), "/tmp/liuhecai-web.tar.gz")
        sftp.close()
        run(
            c,
            r"""
set -e
export PATH=/usr/local/bin:/opt/node-v20.18.1/bin:$PATH
cd /www/wwwroot/liuhecai
rm -rf apps/web/.output
tar -xzf /tmp/liuhecai-web.tar.gz -C /www/wwwroot/liuhecai
cd apps/web/.output/server && npm install --omit=dev
pm2 restart liuhecai-web
sleep 3
curl -sS -H 'Host: 157456.com' http://127.0.0.1/ | tr '\n' ' ' | grep -oiE 'camouflage|發發發|线路|/goto|fake-shop|shop' | head -20 || true
echo '--- title ---'
curl -sS -H 'Host: 157456.com' http://127.0.0.1/ | grep -oE '<title>[^<]+</title>' | head -3
echo '--- ssl sni ---'
for d in 585520.xyz 746528.xyz; do
  echo -n "$d -> "
  echo | openssl s_client -servername $d -connect 127.0.0.1:443 2>/dev/null | openssl x509 -noout -subject
done
""",
            timeout=180,
        )
        return 0
    finally:
        c.close()
        try:
            tmp.unlink()
        except OSError:
            pass


if __name__ == "__main__":
    raise SystemExit(main())
