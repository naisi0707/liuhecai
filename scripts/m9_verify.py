#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import json
import urllib.request

checks = [
    ("web", "http://127.0.0.1:3000/", ("site-shell", "开奖专区", "精选资料")),
    ("admin", "http://127.0.0.1:3001/", ("超管后台", "el-card")),
    ("agent", "http://127.0.0.1:3002/", ("代理后台", "el-card")),
    ("api", "http://127.0.0.1:8080/api/health", ()),
]


def fetch(url: str) -> tuple[int, str]:
    try:
        with urllib.request.urlopen(url, timeout=20) as resp:
            return resp.status, resp.read().decode("utf-8", errors="ignore")
    except Exception:
        # Windows 上 Nuxt 可能只监听 IPv6
        alt = url.replace("127.0.0.1", "[::1]")
        with urllib.request.urlopen(alt, timeout=20) as resp:
            return resp.status, resp.read().decode("utf-8", errors="ignore")


def main():
    for name, url, needles in checks:
        code, body = fetch(url)
        assert code == 200, (name, code)
        assert "localStorage is not defined" not in body, name
        if name == "api":
            data = json.loads(body)
            assert data.get("code") == 0, data
        else:
            for n in needles:
                assert n in body, (name, n)
        print(f"{name} OK")
    print("M9 VERIFY PASS")


if __name__ == "__main__":
    main()
