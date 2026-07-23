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

    uname = f"m8u_{int(time.time())}"
    reg = req("POST", "/api/user/auth/register", {"username": uname, "password": "user123"})
    assert reg["code"] == 0, reg
    ut = reg["data"]["token"]

    me0 = req("GET", "/api/user/me", token=ut)
    assert me0["data"]["coinBalance"] == 0

    created = req(
        "POST",
        "/api/user/recharges",
        {"amount": 80, "payChannel": "微信", "remark": "m8 verify"},
        token=ut,
    )
    assert created["code"] == 0, created
    rid = created["data"]["id"]
    assert created["data"]["status"] == 0

    rejected = req(
        "POST",
        "/api/user/recharges",
        {"amount": 30, "payChannel": "QQ", "remark": "to reject"},
        token=ut,
    )
    assert rejected["code"] == 0, rejected
    rid2 = rejected["data"]["id"]

    rej = req("POST", f"/api/agent/recharges/{rid2}/reject", {"reason": "凭证不清"}, token=at)
    assert rej["code"] == 0, rej
    assert rej["data"]["status"] == 2
    assert rej["data"]["rejectReason"] == "凭证不清"
    print("reject ok")

    ok = req("POST", f"/api/agent/recharges/{rid}/approve", {}, token=at)
    assert ok["code"] == 0, ok
    assert ok["data"]["status"] == 1
    assert ok["data"]["coinBalance"] == 80, ok
    print("approve ok", ok["data"]["coinBalance"])

    ok2 = req("POST", f"/api/agent/recharges/{rid}/approve", {}, token=at)
    assert ok2["code"] == 0, ok2
    assert ok2["data"]["coinBalance"] == 80, ok2
    print("approve idempotent ok")

    me1 = req("GET", "/api/user/me", token=ut)
    assert me1["data"]["coinBalance"] == 80, me1

    mine = req("GET", "/api/user/recharges", token=ut)
    assert mine["code"] == 0
    statuses = {x["id"]: x["status"] for x in mine["data"]}
    assert statuses[rid] == 1
    assert statuses[rid2] == 2
    print("M8 VERIFY PASS")


if __name__ == "__main__":
    main()
