#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""M12: upload + HTML sanitize verification."""
import io
import json
import struct
import time
import urllib.error
import urllib.request

BASE = "http://127.0.0.1:8080"
HOST = "lbw.local"


def req(method, path, body=None, token=None, host=HOST, data=None, headers=None):
    hdrs = {"X-Forwarded-Host": host}
    if headers:
        hdrs.update(headers)
    if body is not None:
        data = json.dumps(body).encode("utf-8")
        hdrs["Content-Type"] = "application/json"
    r = urllib.request.Request(BASE + path, data=data, headers=hdrs, method=method)
    if token:
        r.add_header("Authorization", f"Bearer {token}")
    try:
        with urllib.request.urlopen(r, timeout=30) as resp:
            raw = resp.read()
            ctype = resp.headers.get("Content-Type", "")
            if "application/json" in ctype or raw[:1] in (b"{", b"["):
                return resp.status, json.loads(raw.decode("utf-8"))
            return resp.status, raw
    except urllib.error.HTTPError as e:
        raw = e.read()
        try:
            return e.code, json.loads(raw.decode("utf-8"))
        except Exception:
            return e.code, raw


def tiny_png() -> bytes:
    # 1x1 PNG
    return bytes.fromhex(
        "89504e470d0a1a0a0000000d49484452000000010000000108060000001f15c489"
        "0000000a49444154789c63000100000500010d0a2db40000000049454e44ae426082"
    )


def multipart(file_bytes: bytes, filename: str, content_type: str = "image/png"):
    boundary = "----m12boundary"
    body = (
        f"--{boundary}\r\n"
        f'Content-Disposition: form-data; name="file"; filename="{filename}"\r\n'
        f"Content-Type: {content_type}\r\n\r\n"
    ).encode("utf-8") + file_bytes + f"\r\n--{boundary}--\r\n".encode("utf-8")
    headers = {"Content-Type": f"multipart/form-data; boundary={boundary}"}
    return body, headers


def main():
    code, health = req("GET", "/api/health")
    assert code == 200 and health["code"] == 0, health

    _, agent = req("POST", "/api/agent/auth/login", {"username": "agent_a", "password": "agent123"})
    assert agent["code"] == 0, agent
    at = agent["data"]["token"]

    # 1) upload png
    body, headers = multipart(tiny_png(), "ok.png")
    code, up = req("POST", "/api/agent/uploads", token=at, data=body, headers=headers)
    assert code == 200 and up["code"] == 0, up
    url = up["data"]["url"]
    assert url.startswith("/uploads/"), url
    print("upload ok", url)

    code, blob = req("GET", url)
    assert code == 200 and isinstance(blob, (bytes, bytearray)) and blob[:8] == b"\x89PNG\r\n\x1a\n", (code, type(blob))
    print("public get upload ok")

    # 2) reject svg
    body, headers = multipart(b"<svg xmlns='http://www.w3.org/2000/svg'></svg>", "x.svg", "image/svg+xml")
    code, bad = req("POST", "/api/agent/uploads", token=at, data=body, headers=headers)
    assert code == 200 and bad["code"] != 0, bad
    print("reject svg ok")

    # 2b) reject fake png (text with .png)
    body, headers = multipart(b"notanimage", "fake.png", "image/png")
    code, fake = req("POST", "/api/agent/uploads", token=at, data=body, headers=headers)
    assert code == 200 and fake["code"] != 0, fake
    print("reject fake png ok")

    # 3) XSS sanitize rules
    marker = f"m12ok_{int(time.time())}"
    page = req("GET", "/api/agent/cms/pages/rules", token=at)[1]
    assert page["code"] == 0, page
    content = dict(page["data"]["content"] or {})
    guarantees = list(content.get("guarantees") or [{"title": "保障一：", "body": ""}])
    guarantees[0] = {
        "title": guarantees[0].get("title") or "保障一：",
        "body": f"<script>alert(1)</script><p>{marker}</p><img src=x onerror=alert(1)>",
    }
    content["guarantees"] = guarantees
    saved = req(
        "PUT",
        "/api/agent/cms/pages/rules",
        {"title": page["data"]["title"], "content": content},
        token=at,
    )[1]
    assert saved["code"] == 0, saved
    body_html = saved["data"]["content"]["guarantees"][0]["body"]
    assert "<script" not in body_html.lower(), body_html
    assert "onerror" not in body_html.lower(), body_html
    assert marker in body_html, body_html
    pub = req("GET", "/api/site/pages/rules")[1]
    assert marker in pub["data"]["content"]["guarantees"][0]["body"]
    assert "<script" not in pub["data"]["content"]["guarantees"][0]["body"].lower()
    print("rules sanitize ok")

    # 4) topic sanitize
    topic = req(
        "POST",
        "/api/agent/topics",
        {
            "title": f"m12_{int(time.time())}",
            "lotteryType": "MACAU_NEW",
            "issueNo": "20260722",
            "playType": "测试",
            "price": 1,
            "content": f"<script>x</script><p>topic_{marker}</p>",
            "status": 1,
        },
        token=at,
    )[1]
    assert topic["code"] == 0, topic
    tc = topic["data"]["content"]
    assert "<script" not in tc.lower(), tc
    assert f"topic_{marker}" in tc, tc
    print("topic sanitize ok")

    print("M12 VERIFY PASS")


if __name__ == "__main__":
    main()
