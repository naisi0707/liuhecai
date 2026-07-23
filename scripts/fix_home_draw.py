# -*- coding: utf-8 -*-
import json
import urllib.request

import pymysql

conn = pymysql.connect(host="127.0.0.1", user="root", password="Fsw350881", database="liuhecai", charset="utf8mb4")
cur = conn.cursor()
cur.execute("SELECT id, content_json FROM site_pages WHERE page_key='home'")
rows = cur.fetchall()
for pid, raw in rows:
    data = json.loads(raw)
    if data.get("showLocalDrawPanel") is True:
        data["showLocalDrawPanel"] = False
        cur.execute("UPDATE site_pages SET content_json=%s WHERE id=%s", (json.dumps(data, ensure_ascii=False), pid))
        print("updated", pid)
    else:
        print("ok", pid, data.get("showLocalDrawPanel"))
conn.commit()
conn.close()

# verify public APIs
for path in ["/api/site/pages/home", "/api/topics", "/api/site/tenants"]:
    req = urllib.request.Request(
        "http://127.0.0.1:8080" + path,
        headers={"X-Forwarded-Host": "localhost"},
    )
    with urllib.request.urlopen(req, timeout=20) as resp:
        body = resp.read().decode("utf-8", errors="replace")
    data = json.loads(body)
    if path.endswith("/home"):
        print("showLocal", data["data"]["content"].get("showLocalDrawPanel"))
    elif path.endswith("/topics"):
        print("topics", len(data.get("data") or []))
    else:
        print("others", [x.get("name") for x in (data.get("data") or [])])
