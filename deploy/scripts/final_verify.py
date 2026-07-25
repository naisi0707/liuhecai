#!/usr/bin/env python3
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from ssh_util import connect, run


def main() -> int:
    c = connect()
    try:
        run(
            c,
            r"""
set -e
echo 'API health'
curl -sS http://127.0.0.1:8080/api/health
echo
echo 'Tenants'
for h in 585520.xyz 157456.com 785412.xyz; do
  curl -sS -H "Host: $h" http://127.0.0.1/api/tenant/current | python3 -c 'import sys,json;d=json.load(sys.stdin)["data"];print(d["host"], d["domainRole"], d["name"], d.get("forumHost"))'
done
echo 'HTTPS'
for d in 585520.xyz 785412.xyz 658951.xyz 152687.xyz 746528.xyz; do
  code=$(curl -sk -o /dev/null -w '%{http_code}' --resolve $d:443:127.0.0.1 https://$d/api/health)
  cn=$(echo | openssl s_client -servername $d -connect 127.0.0.1:443 2>/dev/null | openssl x509 -noout -subject | sed 's/.*= //')
  echo "$d http=$code cert=$cn"
done
echo 'ENTRY camouflage'
curl -sS -H 'Host: 157456.com' http://127.0.0.1/ | tr '\n' ' ' | grep -c -i 'shop'
echo 'shop-marker-count-above'
echo 'Admin/Agent'
curl -sS -o /dev/null -w 'admin:%{http_code}\n' -H 'Host: admin.236841.xyz' http://127.0.0.1/
curl -sS -o /dev/null -w 'agent:%{http_code}\n' -H 'Host: agent.236841.xyz' http://127.0.0.1/
systemctl is-active liuhecai-api nginx
pm2 jlist | python3 -c 'import sys,json; apps=json.load(sys.stdin); print([(a["name"], a["pm2_env"]["status"]) for a in apps])'
""",
        )
        return 0
    finally:
        c.close()


if __name__ == "__main__":
    raise SystemExit(main())
