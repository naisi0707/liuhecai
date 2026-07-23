#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import json
import urllib.request

BASE = "http://127.0.0.1:8080"
HOST = "lbw.local"


def req(method, path, body=None, token=None, host=HOST):
    data = None if body is None else json.dumps(body).encode("utf-8")
    headers = {"Content-Type": "application/json", "X-Forwarded-Host": host}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    r = urllib.request.Request(BASE + path, data=data, headers=headers, method=method)
    with urllib.request.urlopen(r, timeout=30) as resp:
        return json.loads(resp.read().decode("utf-8"))


def main():
    health = req("GET", "/api/health")
    assert health["code"] == 0, health

    login = req("POST", "/api/agent/auth/login", {"username": "agent_a", "password": "agent123"})
    assert login["code"] == 0, login
    token = login["data"]["token"]

    fetch = req("POST", "/api/agent/draws/fetch", {}, token=token)
    assert fetch["code"] == 0, fetch
    assert len(fetch["data"]["saved"]) >= 1, fetch
    print("fetch:", fetch["data"])

    latest = req("GET", "/api/draws/latest-all")
    assert latest["code"] == 0, latest
    assert len(latest["data"]) == 3, latest
    has_data = any(x.get("issueNo") for x in latest["data"])
    assert has_data, latest
    print("latest types:", [x["lotteryType"] for x in latest["data"]])

    ov = req(
        "POST",
        "/api/agent/draws/override",
        {
            "lotteryType": "MACAU_NEW",
            "issueNo": "20991231",
            "drawTime": "2026-07-22T21:30:00",
            "numbers": ["01", "02", "03", "04", "05", "06"],
            "specialNumber": "07",
            "note": "m6 verify",
        },
        token=token,
    )
    assert ov["code"] == 0, ov
    assert ov["data"]["overridden"] is True, ov
    assert ov["data"]["specialNumber"] == "07", ov
    print("override ok:", ov["data"]["issueNo"], ov["data"]["numbers"], "+", ov["data"]["specialNumber"])

    # tenant isolation: zzws should not see lbw override of same issue unless it has its own
    other = req("GET", "/api/draws/latest?lotteryType=MACAU_NEW", host="zzws.local")
    assert other["code"] == 0, other
    print("zzws latest:", other["data"].get("issueNo"), "overridden=", other["data"].get("overridden"))
    assert other["data"].get("overridden") is False or other["data"].get("issueNo") != "20991231"

    print("M6 VERIFY PASS")


if __name__ == "__main__":
    main()
