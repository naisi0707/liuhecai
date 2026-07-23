#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import json
import time
import urllib.request
import urllib.error

BASE = "http://127.0.0.1:8080"


def req(method, path, body=None, token=None, host="lbw.local"):
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

    uname = f"m7u_{int(time.time())}"
    reg = req("POST", "/api/user/auth/register", {"username": uname, "password": "user123"})
    assert reg["code"] == 0, reg
    ut = reg["data"]["token"]

    secret = f"SECRET_BODY_{int(time.time())}"
    created = req(
        "POST",
        "/api/agent/topics",
        {
            "title": "M7测试帖",
            "lotteryType": "MACAU_NEW",
            "issueNo": "20260722",
            "playType": "特码",
            "price": 10,
            "content": secret,
            "status": 1,
        },
        token=at,
    )
    assert created["code"] == 0, created
    tid = created["data"]["id"]

    detail0 = req("GET", f"/api/topics/{tid}", token=ut)
    assert detail0["code"] == 0, detail0
    assert detail0["data"]["contentVisible"] is False
    assert detail0["data"].get("content") in (None, "")
    print("locked ok")

    grant = req("POST", "/api/agent/coins/grant", {"username": uname, "amount": 100}, token=at)
    assert grant["code"] == 0, grant

    buy = req("POST", f"/api/user/topics/{tid}/purchase", {}, token=ut)
    assert buy["code"] == 0, buy
    assert buy["data"]["alreadyPurchased"] is False
    assert buy["data"]["coinBalance"] == 90, buy
    print("purchase ok", buy["data"])

    buy2 = req("POST", f"/api/user/topics/{tid}/purchase", {}, token=ut)
    assert buy2["code"] == 0, buy2
    assert buy2["data"]["alreadyPurchased"] is True
    assert buy2["data"]["coinBalance"] == 90, buy2
    print("idempotent ok")

    detail1 = req("GET", f"/api/topics/{tid}", token=ut)
    assert detail1["data"]["contentVisible"] is True
    assert detail1["data"]["content"] == secret
    print("unlocked ok")

    other = req("GET", "/api/topics", host="zzws.local")
    assert other["code"] == 0, other
    assert all(t["id"] != tid for t in other["data"]), other
    print("tenant isolation ok")

    print("M7 VERIFY PASS")


if __name__ == "__main__":
    main()
