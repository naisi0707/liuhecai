#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""E2E helpers: HTTP Result client, MySQL checks, Playwright UI helpers.

Deps: pip install pymysql playwright && playwright install chromium
"""
from __future__ import annotations

import json
import os
import re
import time
import urllib.error
import urllib.request
from pathlib import Path
from typing import Any, Optional

ROOT = Path(__file__).resolve().parents[1]
ARTIFACTS = Path(__file__).resolve().parent / "e2e-artifacts"

BASE = os.environ.get("E2E_API_BASE", "http://127.0.0.1:8080").rstrip("/")
HOST = os.environ.get("E2E_HOST", "lbw.local")
HOST_OTHER = os.environ.get("E2E_HOST_OTHER", "zzws.local")
WEB = os.environ.get("E2E_WEB", "http://127.0.0.1:3000").rstrip("/")
ADMIN = os.environ.get("E2E_ADMIN", "http://127.0.0.1:3001").rstrip("/")
AGENT = os.environ.get("E2E_AGENT", "http://127.0.0.1:3002").rstrip("/")
SKIP_UI = os.environ.get("E2E_SKIP_UI", "0").strip() in ("1", "true", "True", "yes")


def marker(prefix: str = "e2e") -> str:
    return f"{prefix}_{int(time.time())}_{os.getpid()}"


def _read_local_yml_password() -> Optional[str]:
    path = ROOT / "apps" / "api" / "src" / "main" / "resources" / "application-local.yml"
    if not path.is_file():
        return None
    text = path.read_text(encoding="utf-8", errors="replace")
    m = re.search(r"^\s*password:\s*(.+)\s*$", text, re.M)
    if not m:
        return None
    val = m.group(1).strip().strip("'\"")
    return val or None


def mysql_config() -> dict:
    password = os.environ.get("MYSQL_PASSWORD") or _read_local_yml_password() or "changeme"
    return {
        "host": os.environ.get("MYSQL_HOST", "127.0.0.1"),
        "user": os.environ.get("MYSQL_USER", "root"),
        "password": password,
        "database": os.environ.get("MYSQL_DB", "liuhecai"),
        "charset": "utf8mb4",
        "autocommit": True,
    }


def db_connect():
    import pymysql

    return pymysql.connect(**mysql_config())


def db_one(sql: str, args=None):
    conn = db_connect()
    try:
        with conn.cursor() as cur:
            cur.execute(sql, args or ())
            return cur.fetchone()
    finally:
        conn.close()


def db_all(sql: str, args=None):
    conn = db_connect()
    try:
        with conn.cursor() as cur:
            cur.execute(sql, args or ())
            return cur.fetchall()
    finally:
        conn.close()


def req(
    method: str,
    path: str,
    body: Any = None,
    token: Optional[str] = None,
    host: Optional[str] = None,
    timeout: int = 30,
) -> dict:
    data = None if body is None else json.dumps(body, ensure_ascii=False).encode("utf-8")
    headers = {
        "Content-Type": "application/json",
        "X-Forwarded-Host": host or HOST,
    }
    if token:
        headers["Authorization"] = f"Bearer {token}"
    request = urllib.request.Request(BASE + path, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(request, timeout=timeout) as resp:
            raw = resp.read().decode("utf-8")
            return json.loads(raw) if raw else {"code": 0, "data": None}
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", errors="replace")
        try:
            return json.loads(raw)
        except json.JSONDecodeError:
            return {"code": e.code, "message": raw, "data": None}


def req_raw(
    method: str,
    path: str,
    data: Optional[bytes] = None,
    headers: Optional[dict] = None,
    timeout: int = 30,
) -> tuple[int, bytes]:
    h = dict(headers or {})
    request = urllib.request.Request(BASE + path, data=data, headers=h, method=method)
    try:
        with urllib.request.urlopen(request, timeout=timeout) as resp:
            return resp.status, resp.read()
    except urllib.error.HTTPError as e:
        return e.code, e.read()


def upload_png(token: str, host: Optional[str] = None) -> dict:
    """Minimal 1x1 PNG multipart upload."""
    png = (
        b"\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR\x00\x00\x00\x01\x00\x00\x00\x01"
        b"\x08\x02\x00\x00\x00\x90wS\xde\x00\x00\x00\x0cIDATx\x9cc\xf8\x0f\x00"
        b"\x00\x01\x01\x00\x05\x18\xd8N\x00\x00\x00\x00IEND\xaeB`\x82"
    )
    boundary = f"----e2e{int(time.time())}"
    body = (
        f"--{boundary}\r\n"
        f'Content-Disposition: form-data; name="file"; filename="e2e.png"\r\n'
        f"Content-Type: image/png\r\n\r\n"
    ).encode("utf-8") + png + f"\r\n--{boundary}--\r\n".encode("utf-8")
    headers = {
        "Content-Type": f"multipart/form-data; boundary={boundary}",
        "Authorization": f"Bearer {token}",
        "X-Forwarded-Host": host or HOST,
    }
    status, raw = req_raw("POST", "/api/agent/uploads", data=body, headers=headers)
    try:
        return json.loads(raw.decode("utf-8"))
    except Exception:
        return {"code": status, "message": raw.decode("utf-8", errors="replace"), "data": None}


def assert_ok(resp: dict, label: str = "") -> Any:
    if not isinstance(resp, dict) or resp.get("code") != 0:
        raise AssertionError(f"{label} expected code=0, got {resp}")
    return resp.get("data")


def probe_http(url: str, timeout: int = 5) -> bool:
    try:
        with urllib.request.urlopen(url, timeout=timeout) as resp:
            return 200 <= resp.status < 500
    except Exception:
        return False


def ensure_services() -> None:
    checks = [
        ("API", f"{BASE}/api/health"),
        ("web", WEB + "/"),
        ("admin", ADMIN + "/login"),
        ("agent", AGENT + "/login"),
    ]
    missing = []
    for name, url in checks:
        if SKIP_UI and name in ("web", "admin", "agent"):
            continue
        if name == "API":
            try:
                data = req("GET", "/api/health")
                if data.get("code") != 0:
                    missing.append(f"{name} ({url})")
            except Exception:
                missing.append(f"{name} ({url})")
        elif not probe_http(url):
            missing.append(f"{name} ({url})")
    if missing:
        raise RuntimeError(
            "服务未就绪，请先启动 API + 三端前端：\n  - " + "\n  - ".join(missing)
        )
    # DB
    try:
        db_one("SELECT 1")
    except Exception as e:
        raise RuntimeError(f"MySQL 连接失败: {e}") from e


def screenshot(page, name: str) -> Path:
    ARTIFACTS.mkdir(parents=True, exist_ok=True)
    path = ARTIFACTS / f"{name}_{int(time.time())}.png"
    page.screenshot(path=str(path), full_page=True)
    return path


def ui_assert_text(page, text: str, label: str) -> None:
    try:
        page.get_by_text(text, exact=False).first.wait_for(state="visible", timeout=20000)
    except Exception as e:
        path = screenshot(page, f"fail_{label}")
        raise AssertionError(f"UI [{label}] 未找到文案 {text!r}，截图 {path}: {e}") from e


def ui_login_element(page, base_url: str, username: str, password: str, after_text: str, label: str) -> None:
    page.goto(base_url + "/login", wait_until="domcontentloaded")
    page.wait_for_timeout(500)
    inputs = page.locator("input")
    # Element Plus: username then password
    inputs.nth(0).fill(username)
    inputs.nth(1).fill(password)
    page.get_by_role("button", name="登录").click()
    try:
        page.get_by_text(after_text, exact=False).first.wait_for(state="visible", timeout=20000)
    except Exception as e:
        path = screenshot(page, f"fail_{label}_login")
        raise AssertionError(f"UI [{label}] 登录后未看到 {after_text!r}，截图 {path}: {e}") from e
