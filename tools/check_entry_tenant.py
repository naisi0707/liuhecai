# -*- coding: utf-8 -*-
import json
import urllib.request

for host in ("127.0.0.1", "entry.127.0.0.1"):
    req = urllib.request.Request(
        "http://127.0.0.1:8080/api/tenant/current",
        headers={"X-Forwarded-Host": host, "Host": "127.0.0.1:8080"},
    )
    with urllib.request.urlopen(req, timeout=10) as resp:
        body = json.loads(resp.read().decode("utf-8"))
    data = body.get("data") or {}
    print(
        host,
        "code=",
        body.get("code"),
        "role=",
        data.get("domainRole"),
        "forumHost=",
        data.get("forumHost"),
        "name=",
        data.get("name"),
    )
