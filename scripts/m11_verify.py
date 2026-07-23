#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""M11: Agent CMS — login, edit rules page, public GET must reflect change."""
import json
import time
import urllib.error
import urllib.request

BASE = "http://127.0.0.1:8080"
HOST = "lbw.local"


def req(method, path, body=None, token=None, host=HOST):
    data = None if body is None else json.dumps(body).encode("utf-8")
    headers = {"Content-Type": "application/json", "X-Forwarded-Host": host}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    r = urllib.request.Request(BASE + path, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(r, timeout=30) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        return json.loads(e.read().decode("utf-8"))


def main():
    assert req("GET", "/api/health")["code"] == 0

    agent = req("POST", "/api/agent/auth/login", {"username": "agent_a", "password": "agent123"})
    assert agent["code"] == 0, agent
    at = agent["data"]["token"]

    menus = req("GET", "/api/agent/cms/menus", token=at)
    assert menus["code"] == 0, menus
    assert len(menus["data"]) >= 4, menus
    print("menus", len(menus["data"]))

    marker = f"M11保障_{int(time.time())}"
    page = req("GET", "/api/agent/cms/pages/rules", token=at)
    assert page["code"] == 0, page
    content = dict(page["data"]["content"] or {})
    guarantees = list(content.get("guarantees") or [])
    assert guarantees, "rules guarantees empty"
    guarantees[0] = {"title": guarantees[0].get("title") or "保障一：", "body": marker}
    content["guarantees"] = guarantees
    content["heading"] = content.get("heading") or "充值与购买规则"

    saved = req(
        "PUT",
        "/api/agent/cms/pages/rules",
        {"title": page["data"]["title"], "content": content},
        token=at,
    )
    assert saved["code"] == 0, saved
    assert saved["data"]["content"]["guarantees"][0]["body"] == marker
    print("agent put rules ok")

    pub = req("GET", "/api/site/pages/rules")
    assert pub["code"] == 0, pub
    assert pub["data"]["content"]["guarantees"][0]["body"] == marker, pub
    print("public get rules ok")

    # hide kefu menu then assert public menus omit it
    items = []
    for m in menus["data"]:
        items.append({
            "code": m["code"],
            "title": m["title"],
            "path": m["path"],
            "sortNo": m["sortNo"],
            "visible": 0 if m["code"] == "kefu" else m["visible"],
        })
    hid = req("PUT", "/api/agent/cms/menus", {"items": items}, token=at)
    assert hid["code"] == 0, hid
    pub_menus = req("GET", "/api/site/menus")
    assert pub_menus["code"] == 0, pub_menus
    codes = [m["code"] for m in pub_menus["data"]]
    assert "kefu" not in codes, codes
    print("hide kefu ok")

    # restore kefu visible
    for it in items:
        if it["code"] == "kefu":
            it["visible"] = 1
    req("PUT", "/api/agent/cms/menus", {"items": items}, token=at)

    print("M11 VERIFY PASS")


if __name__ == "__main__":
    main()
