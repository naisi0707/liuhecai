#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""全量 E2E：HTTP 业务断言 + MySQL 落库 + Playwright 三端页面核对。

前置：API :8080、web :3000、admin :3001、agent :3002、MySQL liuhecai。

安装依赖：
  pip install pymysql playwright
  playwright install chromium

运行：
  python scripts/e2e_full_verify.py
  set E2E_SKIP_UI=1   # 仅 HTTP+DB
  set MYSQL_PASSWORD=...
"""
from __future__ import annotations

import sys
import traceback
from pathlib import Path
from typing import Any, Callable, List, Tuple

# allow `python scripts/e2e_full_verify.py`
_SCRIPTS = Path(__file__).resolve().parent
if str(_SCRIPTS) not in sys.path:
    sys.path.insert(0, str(_SCRIPTS))

from e2e_lib import (
    ADMIN,
    AGENT,
    HOST,
    HOST_OTHER,
    SKIP_UI,
    WEB,
    assert_ok,
    db_one,
    ensure_services,
    marker,
    req,
    req_raw,
    screenshot,
    ui_assert_text,
    ui_login_element,
    upload_png,
)

ResultRow = Tuple[str, bool, str]


class Runner:
    def __init__(self) -> None:
        self.results: List[ResultRow] = []
        self.ctx: dict[str, Any] = {}

    def run(self, name: str, fn: Callable[[], None]) -> None:
        try:
            fn()
            self.results.append((name, True, "PASS"))
            print(f"[PASS] {name}")
        except Exception as e:
            msg = str(e)
            self.results.append((name, False, msg))
            print(f"[FAIL] {name}: {msg}")
            traceback.print_exc()


def scenario_health(r: Runner) -> None:
    data = assert_ok(req("GET", "/api/health"), "health")
    assert data and data.get("status") == "UP", data
    lbw = assert_ok(req("GET", "/api/tenant/current", host=HOST), "tenant lbw")
    assert lbw.get("name"), lbw
    zzws = assert_ok(req("GET", "/api/tenant/current", host=HOST_OTHER), "tenant zzws")
    assert zzws.get("name"), zzws
    assert lbw.get("id") != zzws.get("id"), (lbw, zzws)
    r.ctx["tenant_id"] = lbw.get("id")


def scenario_admin_smoke(r: Runner) -> None:
    login = assert_ok(
        req("POST", "/api/admin/auth/login", {"username": "admin", "password": "admin123"}),
        "admin login",
    )
    token = login["token"]
    r.ctx["admin_token"] = token
    me = assert_ok(req("GET", "/api/admin/me", token=token), "admin me")
    assert me.get("username") == "admin", me
    dash = assert_ok(req("GET", "/api/admin/dashboard", token=token), "admin dashboard")
    assert "kpis" in dash or dash.get("kpis") is not None or isinstance(dash, dict), dash
    tenants = assert_ok(req("GET", "/api/admin/tenants", token=token), "admin tenants")
    assert isinstance(tenants, list) and len(tenants) >= 1, tenants
    agents = assert_ok(req("GET", "/api/admin/agents", token=token), "admin agents")
    assert agents is not None
    users = assert_ok(req("GET", "/api/admin/users", token=token), "admin users")
    assert users is not None
    logs = assert_ok(req("GET", "/api/admin/audit-logs", token=token), "audit logs")
    assert logs is not None
    # export CSV (not Result JSON)
    status, raw = req_raw(
        "GET",
        "/api/admin/agents/export",
        headers={"Authorization": f"Bearer {token}", "X-Forwarded-Host": HOST},
    )
    assert status == 200 and len(raw) > 10, (status, raw[:200])


def scenario_agent_smoke(r: Runner) -> None:
    admin = r.ctx.get("admin_token")
    assert admin, "need admin_token from admin_smoke"
    # 本地库密码可能与种子不一致：用超管重置后再登
    agents = assert_ok(req("GET", "/api/admin/agents?username=agent_a&size=20", token=admin), "find agent_a")
    records = agents.get("records") if isinstance(agents, dict) else agents
    agent_id = None
    for a in records or []:
        if a.get("username") == "agent_a":
            agent_id = a.get("id")
            break
    assert agent_id, f"agent_a not found: {agents}"
    reset = assert_ok(req("POST", f"/api/admin/agents/{agent_id}/reset-password", token=admin), "reset agent pwd")
    raw = reset.get("rawPassword")
    assert raw, reset
    r.ctx["agent_password"] = raw
    r.ctx["agent_id"] = agent_id

    login = assert_ok(
        req("POST", "/api/agent/auth/login", {"username": "agent_a", "password": raw}),
        "agent login",
    )
    token = login["token"]
    r.ctx["agent_token"] = token
    me = assert_ok(req("GET", "/api/agent/me", token=token), "agent me")
    assert me.get("username") == "agent_a", me
    dash = assert_ok(req("GET", "/api/agent/dashboard", token=token), "agent dashboard")
    assert isinstance(dash, dict), dash
    menus = assert_ok(req("GET", "/api/agent/cms/menus", token=token), "cms menus")
    assert isinstance(menus, list) and len(menus) >= 1, menus
    pages = assert_ok(req("GET", "/api/agent/cms/pages", token=token), "cms pages")
    assert pages is not None


def scenario_site_config(r: Runner) -> None:
    at = r.ctx["agent_token"]
    mk = marker("ann")
    r.ctx["ann_marker"] = mk
    cfg = assert_ok(req("GET", "/api/agent/site-config", token=at), "get site-config")
    r.ctx["site_config_backup"] = dict(cfg)
    body = {
        "name": cfg.get("name") or "E2E站",
        "announcement": mk,
        "kefuWechat": cfg.get("kefuWechat") or "wx_e2e",
        "kefuQq": cfg.get("kefuQq") or "10000",
        "primaryColor": cfg.get("primaryColor") or "#c62828",
        "fontFamily": cfg.get("fontFamily") or "Microsoft YaHei",
        "logoUrl": cfg.get("logoUrl") or "",
    }
    saved = assert_ok(req("PUT", "/api/agent/site-config", body, token=at), "put site-config")
    assert mk in (saved.get("announcement") or ""), saved
    row = db_one("SELECT announcement FROM tenants WHERE id=%s", (r.ctx["tenant_id"],))
    assert row and mk in (row[0] or ""), row
    pub = assert_ok(req("GET", "/api/tenant/current"), "public tenant after cfg")
    assert mk in (pub.get("announcement") or ""), pub


def scenario_cms_rules(r: Runner) -> None:
    at = r.ctx["agent_token"]
    mk = marker("rule")
    r.ctx["rule_marker"] = mk
    page = assert_ok(req("GET", "/api/agent/cms/pages/rules", token=at), "get rules")
    r.ctx["rules_backup"] = page
    content = dict(page.get("content") or {})
    guarantees = list(content.get("guarantees") or [])
    if not guarantees:
        guarantees = [{"title": "保障一", "body": mk}]
    else:
        guarantees[0] = {
            "title": guarantees[0].get("title") or "保障一",
            "body": mk,
        }
    content["guarantees"] = guarantees
    content["heading"] = content.get("heading") or "充值与购买规则"
    saved = assert_ok(
        req("PUT", "/api/agent/cms/pages/rules", {"title": page.get("title") or "规则", "content": content}, token=at),
        "put rules",
    )
    assert saved["content"]["guarantees"][0]["body"] == mk, saved
    row = db_one(
        "SELECT content_json FROM site_pages WHERE tenant_id=%s AND page_key='rules'",
        (r.ctx["tenant_id"],),
    )
    assert row and mk in (row[0] or ""), row
    pub = assert_ok(req("GET", "/api/site/pages/rules"), "public rules")
    assert pub["content"]["guarantees"][0]["body"] == mk, pub


def scenario_topic_purchase(r: Runner) -> None:
    at = r.ctx["agent_token"]
    secret = marker("topicbody")
    title = marker("topictitle")
    r.ctx["topic_secret"] = secret
    r.ctx["topic_title"] = title
    created = assert_ok(
        req(
            "POST",
            "/api/agent/topics",
            {
                "title": title,
                "lotteryType": "MACAU_NEW",
                "issueNo": "20990101",
                "playType": "特码",
                "price": 10,
                "content": secret,
                "status": 1,
            },
            token=at,
        ),
        "create topic",
    )
    tid = str(created["id"])
    r.ctx["topic_id"] = tid
    row = db_one("SELECT content, status FROM topics WHERE id=%s", (int(tid),))
    assert row and row[0] == secret and row[1] == 1, row

    uname = marker("u").replace("-", "")[:24]
    r.ctx["username"] = uname
    reg = assert_ok(
        req("POST", "/api/user/auth/register", {"username": uname, "password": "user123"}),
        "register",
    )
    ut = reg["token"]
    r.ctx["user_token"] = ut
    detail0 = assert_ok(req("GET", f"/api/topics/{tid}", token=ut), "topic locked")
    assert detail0.get("contentVisible") is False, detail0
    assert not detail0.get("content"), detail0

    # isolation
    other_list = assert_ok(req("GET", "/api/topics", host=HOST_OTHER), "zzws topics")
    assert all(str(t.get("id")) != tid for t in (other_list or [])), other_list

    grant = assert_ok(
        req("POST", "/api/agent/coins/grant", {"username": uname, "amount": 100}, token=at),
        "grant",
    )
    # grant returns map with coinBalance or balance
    bal = grant.get("coinBalance", grant.get("balance", grant))
    if isinstance(bal, dict):
        bal = bal.get("coinBalance") or bal.get("balance")
    # some APIs wrap differently
    me = assert_ok(req("GET", "/api/user/me", token=ut), "me after grant")
    assert me.get("coinBalance") == 100, (grant, me)
    uid = me.get("id")
    r.ctx["user_id"] = uid
    urow = db_one("SELECT coin_balance FROM users WHERE id=%s", (uid,))
    assert urow and urow[0] == 100, urow

    buy = assert_ok(req("POST", f"/api/user/topics/{tid}/purchase", {}, token=ut), "purchase")
    assert buy.get("alreadyPurchased") is False, buy
    assert buy.get("coinBalance") == 90, buy
    order = db_one(
        "SELECT id FROM topic_orders WHERE user_id=%s AND topic_id=%s",
        (uid, int(tid)),
    )
    assert order, "missing topic_order"
    clog = db_one(
        "SELECT change_amount, balance_after FROM coin_logs WHERE user_id=%s AND biz_type='PURCHASE' ORDER BY id DESC LIMIT 1",
        (uid,),
    )
    assert clog and clog[0] == -10 and clog[1] == 90, clog

    detail1 = assert_ok(req("GET", f"/api/topics/{tid}", token=ut), "topic unlocked")
    assert detail1.get("contentVisible") is True, detail1
    assert detail1.get("content") == secret, detail1


def scenario_recharge(r: Runner) -> None:
    at = r.ctx["agent_token"]
    ut = r.ctx["user_token"]
    uid = r.ctx["user_id"]
    me0 = assert_ok(req("GET", "/api/user/me", token=ut), "me before recharge")
    bal0 = me0["coinBalance"]

    rid_ok = assert_ok(
        req("POST", "/api/user/recharges", {"amount": 50, "payChannel": "微信", "remark": marker("rc")}, token=ut),
        "recharge create",
    )
    rid = rid_ok["id"]
    r.ctx["recharge_id"] = rid
    assert rid_ok.get("status") == 0, rid_ok

    rid_bad = assert_ok(
        req("POST", "/api/user/recharges", {"amount": 20, "payChannel": "QQ", "remark": "reject-me"}, token=ut),
        "recharge create2",
    )
    rej = assert_ok(
        req("POST", f"/api/agent/recharges/{rid_bad['id']}/reject", {"reason": "凭证不清"}, token=at),
        "reject",
    )
    assert rej.get("status") == 2, rej
    row_rej = db_one("SELECT status, reject_reason FROM recharge_requests WHERE id=%s", (rid_bad["id"],))
    assert row_rej and row_rej[0] == 2 and row_rej[1] == "凭证不清", row_rej

    ok = assert_ok(req("POST", f"/api/agent/recharges/{rid}/approve", {}, token=at), "approve")
    assert ok.get("status") == 1, ok
    me1 = assert_ok(req("GET", "/api/user/me", token=ut), "me after recharge")
    assert me1["coinBalance"] == bal0 + 50, (bal0, me1)
    row = db_one("SELECT status FROM recharge_requests WHERE id=%s", (rid,))
    assert row and row[0] == 1, row
    urow = db_one("SELECT coin_balance FROM users WHERE id=%s", (uid,))
    assert urow and urow[0] == bal0 + 50, urow


def scenario_draw_override(r: Runner) -> None:
    at = r.ctx["agent_token"]
    issue = f"E2E{int(__import__('time').time()) % 100000000}"
    r.ctx["draw_issue"] = issue
    ov = assert_ok(
        req(
            "POST",
            "/api/agent/draws/override",
            {
                "lotteryType": "MACAU_NEW",
                "issueNo": issue,
                "drawTime": "2099-01-01T12:00:00",
                "numbers": ["01", "02", "03", "04", "05", "06"],
                "specialNumber": "07",
                "note": "e2e override",
            },
            token=at,
        ),
        "draw override",
    )
    assert ov is not None
    row = db_one(
        "SELECT issue_no FROM draw_overrides WHERE tenant_id=%s AND lottery_type='MACAU_NEW' AND issue_no=%s",
        (r.ctx["tenant_id"], issue),
    )
    assert row and row[0] == issue, row
    latest = assert_ok(req("GET", "/api/draws/latest?lotteryType=MACAU_NEW"), "public latest")
    # may be object or list item
    if isinstance(latest, list):
        found = any(x.get("issueNo") == issue for x in latest)
        assert found, latest
    else:
        assert latest.get("issueNo") == issue, latest


def scenario_agent_user_coins(r: Runner) -> None:
    at = r.ctx["agent_token"]
    uid = r.ctx["user_id"]
    uname = r.ctx["username"]
    page = assert_ok(req("GET", f"/api/agent/users?username={uname}", token=at), "agent users page")
    # page may be PageResult
    records = page.get("records") if isinstance(page, dict) else page
    if records is None and isinstance(page, list):
        records = page
    assert records is not None
    detail = assert_ok(req("GET", f"/api/agent/users/{uid}", token=at), "agent user detail")
    before = detail.get("coinBalance")
    adj = assert_ok(
        req("POST", f"/api/agent/users/{uid}/coins", {"amount": 5, "remark": "e2e adjust"}, token=at),
        "adjust coins",
    )
    bal = adj.get("coinBalance", adj.get("balance"))
    if isinstance(bal, dict):
        bal = bal.get("coinBalance")
    me = assert_ok(req("GET", "/api/user/me", token=r.ctx["user_token"]), "me after adjust")
    assert me["coinBalance"] == before + 5, (before, adj, me)
    urow = db_one("SELECT coin_balance FROM users WHERE id=%s", (uid,))
    assert urow and urow[0] == before + 5, urow
    logs = assert_ok(req("GET", f"/api/agent/users/{uid}/coin-logs", token=at), "coin logs")
    assert logs is not None
    orders = assert_ok(req("GET", f"/api/agent/users/{uid}/orders", token=at), "orders")
    assert orders is not None
    r.ctx["user_balance"] = me["coinBalance"]


def scenario_admin_read(r: Runner) -> None:
    token = r.ctx["admin_token"]
    uid = r.ctx["user_id"]
    detail = assert_ok(req("GET", f"/api/admin/users/{uid}", token=token), "admin user")
    assert detail.get("username") == r.ctx["username"], detail
    agent_id = r.ctx.get("agent_id")
    assert agent_id, "agent_id missing"
    ad = assert_ok(req("GET", f"/api/admin/agents/{agent_id}", token=token), "admin agent detail")
    assert ad.get("username") == "agent_a" or ad.get("id"), ad


def scenario_upload(r: Runner) -> None:
    at = r.ctx["agent_token"]
    up = upload_png(at)
    data = assert_ok(up, "upload")
    url = data.get("url") or data.get("path") or ""
    assert url, data


def scenario_get_smokes(r: Runner) -> None:
    at = r.ctx["agent_token"]
    ut = r.ctx["user_token"]
    for method, path, tok in [
        ("GET", "/api/draws/latest-all", None),
        ("GET", "/api/site/menus", None),
        ("GET", "/api/site/tenants", None),
        ("GET", "/api/site/pages/home", None),
        ("GET", "/api/user/recharges", ut),
        ("GET", "/api/agent/topics", at),
        ("GET", "/api/agent/recharges", at),
        ("GET", "/api/agent/draws/latest-all", at),
        ("GET", "/api/demo-notes", None),
    ]:
        assert_ok(req(method, path, token=tok), path)


def scenario_ui(r: Runner) -> None:
    if SKIP_UI:
        print("[SKIP] UI (E2E_SKIP_UI=1)")
        return
    from playwright.sync_api import sync_playwright

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page(viewport={"width": 1280, "height": 900})
        try:
            # admin
            ui_login_element(page, ADMIN, "admin", "admin123", "总览", "admin")
            ui_assert_text(page, "站点", "admin-kpi")

            # agent dashboard
            ui_login_element(page, AGENT, "agent_a", r.ctx.get("agent_password") or "agent123", "运营看板", "agent")
            page.goto(AGENT + "/recharges", wait_until="networkidle")
            ui_assert_text(page, "充值确认", "agent-recharges")
            page.goto(AGENT + f"/users/{r.ctx['user_id']}", wait_until="networkidle")
            ui_assert_text(page, r.ctx["username"], "agent-user")
            ui_assert_text(page, str(r.ctx["user_balance"]), "agent-balance")

            # web home announcement
            page.goto(WEB + "/", wait_until="networkidle")
            ui_assert_text(page, r.ctx["ann_marker"], "web-ann")

            # web rules
            page.goto(WEB + "/rules", wait_until="networkidle")
            ui_assert_text(page, r.ctx["rule_marker"], "web-rules")

            # web topic unlocked via localStorage token
            page.goto(WEB + "/", wait_until="domcontentloaded")
            page.evaluate(
                """([token]) => { localStorage.setItem('user_token', token); }""",
                [r.ctx["user_token"]],
            )
            page.goto(WEB + f"/topic/{r.ctx['topic_id']}", wait_until="networkidle")
            ui_assert_text(page, r.ctx["topic_secret"], "web-topic")
            ui_assert_text(page, r.ctx["topic_title"], "web-topic-title")
        finally:
            try:
                screenshot(page, "final")
            except Exception:
                pass
            browser.close()


def restore_site(r: Runner) -> None:
    """Best-effort restore announcement / rules so demo site stays usable."""
    at = r.ctx.get("agent_token")
    if not at:
        return
    backup = r.ctx.get("site_config_backup")
    if backup:
        ann = backup.get("announcement") or ""
        if len(ann) > 512:
            ann = ann[:512]
        body = {
            "name": backup.get("name") or "站",
            "announcement": ann,
            "kefuWechat": backup.get("kefuWechat") or "",
            "kefuQq": backup.get("kefuQq") or "",
            "primaryColor": backup.get("primaryColor") or "#c62828",
            "fontFamily": backup.get("fontFamily") or "Microsoft YaHei",
            "logoUrl": backup.get("logoUrl") or "",
        }
        req("PUT", "/api/agent/site-config", body, token=at)
    rules = r.ctx.get("rules_backup")
    if rules:
        req(
            "PUT",
            "/api/agent/cms/pages/rules",
            {"title": rules.get("title") or "规则", "content": rules.get("content") or {}},
            token=at,
        )


def main() -> int:
    # fix Windows console
    if hasattr(sys.stdout, "reconfigure"):
        try:
            sys.stdout.reconfigure(encoding="utf-8")
        except Exception:
            pass

    print("E2E full verify starting...")
    print(f"  API={__import__('e2e_lib').BASE} HOST={HOST} SKIP_UI={SKIP_UI}")
    ensure_services()
    r = Runner()
    steps = [
        ("health_tenant", scenario_health),
        ("admin_smoke", scenario_admin_smoke),
        ("agent_smoke", scenario_agent_smoke),
        ("site_config", scenario_site_config),
        ("cms_rules", scenario_cms_rules),
        ("topic_purchase", scenario_topic_purchase),
        ("recharge", scenario_recharge),
        ("draw_override", scenario_draw_override),
        ("agent_user_coins", scenario_agent_user_coins),
        ("admin_read", scenario_admin_read),
        ("upload", scenario_upload),
        ("get_smokes", scenario_get_smokes),
        ("ui_playwright", scenario_ui),
    ]
    for name, fn in steps:
        r.run(name, lambda f=fn: f(r))

    try:
        restore_site(r)
    except Exception as e:
        print(f"[WARN] restore_site: {e}")

    passed = sum(1 for _, ok, _ in r.results if ok)
    failed = sum(1 for _, ok, _ in r.results if not ok)
    print("\n======== SUMMARY ========")
    for name, ok, msg in r.results:
        print(f"  {'PASS' if ok else 'FAIL'}  {name}" + ("" if ok else f" — {msg[:120]}"))
    print(f"Total {len(r.results)}  PASS {passed}  FAIL {failed}")
    if failed:
        print("E2E VERIFY FAIL")
        return 1
    print("E2E VERIFY PASS")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
