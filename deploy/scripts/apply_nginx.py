#!/usr/bin/env python3
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from ssh_util import connect, run, load_password, HOST
import paramiko


def main() -> int:
    local = Path(__file__).resolve().parents[1] / "nginx" / "liuhecai.conf"
    c = connect()
    try:
        sftp = c.open_sftp()
        sftp.put(str(local), "/etc/nginx/sites-available/liuhecai.conf")
        sftp.close()
        run(
            c,
            r"""
set -e
ln -sfn /etc/nginx/sites-available/liuhecai.conf /etc/nginx/sites-enabled/liuhecai.conf
nginx -t
systemctl reload nginx
echo '=== https sni checks ==='
for d in 585520.xyz 785412.xyz 658951.xyz 152687.xyz 746528.xyz; do
  # verify cert CN/SAN matches via openssl
  echo -n "$d cert: "
  echo | openssl s_client -servername $d -connect 127.0.0.1:443 2>/dev/null | openssl x509 -noout -subject -ext subjectAltName 2>/dev/null | head -3
  code=$(curl -sk -o /dev/null -w '%{http_code}' --resolve $d:443:127.0.0.1 https://$d/api/health)
  echo "health=$code"
done
echo '=== entry html sample ==='
curl -sS -H 'Host: 157456.com' http://127.0.0.1/ | head -c 1500
echo
echo '=== entry domainRole ==='
curl -sS -H 'Host: 157456.com' http://127.0.0.1/api/tenant/current
echo
""",
            timeout=120,
        )
        return 0
    finally:
        c.close()


if __name__ == "__main__":
    raise SystemExit(main())
