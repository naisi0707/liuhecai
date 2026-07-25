#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Verify account-mgmt APIs by HTTP + MySQL row assertions (not response-only).

Usage:
  MYSQL_PASSWORD=changeme python3 scripts/mgmt_ops_db_verify.py
"""
from __future__ import annotations

import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from e2e_lib import assert_ok, db_one, db_all, marker, req  # noqa: E402


class Fail(Exception):
    pass


def db_dict(sql: str, args=None):
    import pymysql
    from e2e_lib import mysql_config

    conn = pymysql.connect(**mysql_config(), cursorclass=pymysql.cursors.DictCursor)
    try:
        with conn.cursor() as cur:
            cur.execute(sql, args or ())
            return cur.fetchone()
    finally:
        conn.close()


def expect(cond, msg: str):
    if not cond:
        raise Fail(msg)


def login_admin() -> str:
    data = assert_ok(req("POST", "/api/admin/auth/login", {"username": "admin", "password": "admin123"}), "admin login")
    token = data.get("token")
    expect(token, "admin token missing")
    return token


def login_agent(username: str, password: str) -> str:
    data = assert_ok(
        req("POST", "/api/agent/auth/login", {"username": username, "password": password}),
        f"agent login {username}",
    )
    token = data.get("token")
    expect(token, "agent token missing")
    return token


def audit_exists(action: str, target_id: str | None = None) -> bool:
    if target_id is None:
        row = db_one(
            "SELECT COUNT(*) FROM op_audit_logs WHERE action=%s AND created_at >= NOW() - INTERVAL 5 MINUTE",
            (action,),
        )
    else:
        row = db_one(
            "SELECT COUNT(*) FROM op_audit_logs WHERE action=%s AND target_id=%s AND created_at >= NOW() - INTERVAL 5 MINUTE",
            (action, str(target_id)),
        )
    return bool(row and row[0] > 0)


def main() -> int:
    stamp = marker("mgmt")
    passed = []
    failed = []

    def run(name, fn):
        try:
            fn()
            passed.append(name)
            print(f"PASS  {name}")
        except Exception as e:
            failed.append((name, str(e)))
            print(f"FAIL  {name}: {e}")

    admin = login_admin()

    tenants = assert_ok(req("GET", "/api/admin/tenants", token=admin), "list tenants")
    expect(isinstance(tenants, list) and len(tenants) >= 1, "need at least 1 tenant")
    tenant = tenants[0]
    tenant_id = tenant["id"]
    agents = tenant.get("agents") or []
    expect(agents, "tenant has no agents")
    primary = next((a for a in agents if a.get("isPrimary") == 1), agents[0])
    primary_id = primary["id"]

    # ---------- admin create user ----------
    def t_admin_create_user():
        uname = f"u_{stamp}"
        before = db_one("SELECT COUNT(*) FROM users WHERE tenant_id=%s AND username=%s", (tenant_id, uname))[0]
        expect(before == 0, "user already exists")
        data = assert_ok(
            req("POST", "/api/admin/users", {"tenantId": tenant_id, "username": uname}, token=admin),
            "admin create user",
        )
        uid = data["id"]
        raw = data["rawPassword"]
        expect(raw and len(raw) >= 6, "rawPassword missing")
        row = db_dict("SELECT id, username, enabled, coin_balance, password_hash FROM users WHERE id=%s", (uid,))
        expect(row is not None, "user row not in DB")
        expect(row["username"] == uname, "username mismatch")
        expect(int(row["enabled"]) == 1, "enabled should be 1")
        expect(int(row["coin_balance"]) == 0, "coin should be 0")
        expect(row["password_hash"] and row["password_hash"] != raw, "password should be hashed")
        expect(audit_exists("USER_CREATE", str(uid)), "audit USER_CREATE missing")
        globals()["created_user_id"] = uid
        globals()["created_user_name"] = uname

    run("admin_create_user+db", t_admin_create_user)
    uid = globals().get("created_user_id")

    # ---------- admin adjust coins ----------
    def t_admin_adjust_coins():
        expect(uid, "need created user")
        before = db_dict("SELECT coin_balance FROM users WHERE id=%s", (uid,))
        bal0 = int(before["coin_balance"])
        data = assert_ok(
            req("POST", f"/api/admin/users/{uid}/coins", {"amount": 50, "remark": f"test-{stamp}"}, token=admin),
            "admin coins",
        )
        expect(int(data["coinBalance"]) == bal0 + 50, f"api balance {data}")
        after = db_dict("SELECT coin_balance FROM users WHERE id=%s", (uid,))
        expect(int(after["coinBalance"] if "coinBalance" in after else after["coin_balance"]) == bal0 + 50, "db coin not updated")
        log = db_one(
            "SELECT change_amount, balance_after FROM coin_logs WHERE user_id=%s ORDER BY id DESC LIMIT 1",
            (uid,),
        )
        expect(log is not None and int(log[0]) == 50 and int(log[1]) == bal0 + 50, f"coin_logs bad: {log}")
        expect(audit_exists("USER_COIN_ADJUST", str(uid)), "audit USER_COIN_ADJUST missing")

    run("admin_adjust_coins+db", t_admin_adjust_coins)

    # ---------- admin reset password ----------
    def t_admin_reset_user_pwd():
        expect(uid, "need created user")
        before = db_dict("SELECT password_hash, COALESCE(token_version,0) tv FROM users WHERE id=%s", (uid,))
        data = assert_ok(req("POST", f"/api/admin/users/{uid}/reset-password", token=admin), "reset user pwd")
        expect(data.get("rawPassword"), "rawPassword empty")
        after = db_dict("SELECT password_hash, COALESCE(token_version,0) tv FROM users WHERE id=%s", (uid,))
        expect(after["password_hash"] != before["password_hash"], "password_hash unchanged")
        expect(int(after["tv"]) > int(before["tv"]), f"token_version not bumped: {before['tv']} -> {after['tv']}")
        expect(audit_exists("USER_RESET_PASSWORD", str(uid)), "audit USER_RESET_PASSWORD missing")

    run("admin_reset_user_password+db", t_admin_reset_user_pwd)

    # ---------- admin force logout ----------
    def t_admin_force_logout_user():
        expect(uid, "need created user")
        before = db_dict("SELECT COALESCE(token_version,0) tv FROM users WHERE id=%s", (uid,))
        assert_ok(req("POST", f"/api/admin/users/{uid}/force-logout", token=admin), "force logout user")
        after = db_dict("SELECT COALESCE(token_version,0) tv FROM users WHERE id=%s", (uid,))
        expect(int(after["tv"]) > int(before["tv"]), "token_version not bumped on force logout")
        expect(audit_exists("USER_FORCE_LOGOUT", str(uid)), "audit USER_FORCE_LOGOUT missing")

    run("admin_force_logout_user+db", t_admin_force_logout_user)

    # ---------- admin soft delete user ----------
    def t_admin_soft_delete_user():
        expect(uid, "need created user")
        before = db_dict("SELECT enabled, COALESCE(token_version,0) tv FROM users WHERE id=%s", (uid,))
        assert_ok(req("POST", f"/api/admin/users/{uid}/delete", token=admin), "soft delete user")
        after = db_dict("SELECT enabled, COALESCE(token_version,0) tv FROM users WHERE id=%s", (uid,))
        expect(int(after["enabled"]) == 0, "enabled not 0 after delete")
        expect(int(after["tv"]) > int(before["tv"]), "token_version not bumped on delete")
        expect(audit_exists("USER_DELETE", str(uid)), "audit USER_DELETE missing")
        # re-enable for later agent tests if needed
        assert_ok(req("PUT", f"/api/admin/users/{uid}/enabled", {"enabled": 1}, token=admin), "re-enable user")

    run("admin_soft_delete_user+db", t_admin_soft_delete_user)

    # ---------- admin batch enable ----------
    def t_admin_batch_enable():
        expect(uid, "need created user")
        assert_ok(req("POST", "/api/admin/users/batch-enabled", {"ids": [uid], "enabled": 0}, token=admin), "batch ban")
        row = db_dict("SELECT enabled FROM users WHERE id=%s", (uid,))
        expect(int(row["enabled"]) == 0, "batch ban db fail")
        assert_ok(req("POST", "/api/admin/users/batch-enabled", {"ids": [uid], "enabled": 1}, token=admin), "batch unban")
        row2 = db_dict("SELECT enabled FROM users WHERE id=%s", (uid,))
        expect(int(row2["enabled"]) == 1, "batch unban db fail")

    run("admin_batch_enabled+db", t_admin_batch_enable)

    # ---------- admin create agent ----------
    def t_admin_create_agent():
        aname = f"ag_{stamp}"
        data = assert_ok(
            req("POST", f"/api/admin/tenants/{tenant_id}/agents", {"username": aname}, token=admin),
            "create agent",
        )
        aid = data["id"]
        expect(data.get("rawPassword"), "agent rawPassword missing")
        row = db_dict(
            "SELECT id, username, enabled, is_primary, tenant_id FROM agent_accounts WHERE id=%s",
            (aid,),
        )
        expect(row is not None, "agent not in DB")
        expect(row["username"] == aname, "agent username mismatch")
        expect(int(row["enabled"]) == 1, "agent enabled")
        expect(int(row["is_primary"] or 0) == 0, "new agent should not steal primary")
        expect(str(row["tenant_id"]) == str(tenant_id), "tenant mismatch")
        expect(audit_exists("AGENT_CREATE", str(aid)), "audit AGENT_CREATE missing")
        globals()["created_agent_id"] = aid
        globals()["created_agent_name"] = aname
        globals()["created_agent_pwd"] = data["rawPassword"]

    run("admin_create_agent+db", t_admin_create_agent)
    aid = globals().get("created_agent_id")

    # ---------- set primary then restore ----------
    def t_set_primary():
        expect(aid, "need secondary agent")
        old_primary = db_dict("SELECT primary_agent_id FROM tenants WHERE id=%s", (tenant_id,))["primary_agent_id"]
        assert_ok(
            req("PUT", f"/api/admin/tenants/{tenant_id}/primary-agent", {"agentId": aid}, token=admin),
            "set primary",
        )
        trow = db_dict("SELECT primary_agent_id FROM tenants WHERE id=%s", (tenant_id,))
        expect(str(trow["primary_agent_id"]) == str(aid), "tenant.primary_agent_id not updated")
        newp = db_dict("SELECT is_primary, primary_key FROM agent_accounts WHERE id=%s", (aid,))
        expect(int(newp["is_primary"]) == 1 and int(newp["primary_key"] or 0) == 1, "new primary flags")
        old = db_dict("SELECT is_primary, primary_key FROM agent_accounts WHERE id=%s", (old_primary,))
        expect(int(old["is_primary"] or 0) == 0, "old primary is_primary not cleared")
        expect(old["primary_key"] is None, "old primary_key not null")
        expect(audit_exists("AGENT_SET_PRIMARY", str(aid)), "audit AGENT_SET_PRIMARY missing")
        # restore original primary so we can delete the test agent
        assert_ok(
            req("PUT", f"/api/admin/tenants/{tenant_id}/primary-agent", {"agentId": old_primary}, token=admin),
            "restore primary",
        )
        globals()["restored_primary_id"] = old_primary

    run("admin_set_primary+db", t_set_primary)

    # ---------- agent enable / reset / logout / delete ----------
    def t_agent_ops():
        expect(aid, "need secondary agent")
        before = db_dict(
            "SELECT enabled, password_hash, COALESCE(token_version,0) tv FROM agent_accounts WHERE id=%s",
            (aid,),
        )
        assert_ok(req("PUT", f"/api/admin/agents/{aid}/enabled", {"enabled": 0}, token=admin), "disable agent")
        mid = db_dict("SELECT enabled FROM agent_accounts WHERE id=%s", (aid,))
        expect(int(mid["enabled"]) == 0, "agent disable db fail")
        assert_ok(req("PUT", f"/api/admin/agents/{aid}/enabled", {"enabled": 1}, token=admin), "enable agent")
        data = assert_ok(req("POST", f"/api/admin/agents/{aid}/reset-password", token=admin), "reset agent pwd")
        after = db_dict(
            "SELECT password_hash, COALESCE(token_version,0) tv FROM agent_accounts WHERE id=%s",
            (aid,),
        )
        expect(after["password_hash"] != before["password_hash"], "agent hash unchanged")
        expect(int(after["tv"]) > int(before["tv"]), "agent token_version not bumped")
        tv1 = int(after["tv"])
        assert_ok(req("POST", f"/api/admin/agents/{aid}/force-logout", token=admin), "force logout agent")
        tv2 = int(db_dict("SELECT COALESCE(token_version,0) tv FROM agent_accounts WHERE id=%s", (aid,))["tv"])
        expect(tv2 > tv1, "force logout token_version")
        # soft delete secondary
        assert_ok(req("POST", f"/api/admin/agents/{aid}/delete", token=admin), "delete agent")
        row = db_dict("SELECT enabled, COALESCE(token_version,0) tv FROM agent_accounts WHERE id=%s", (aid,))
        expect(int(row["enabled"]) == 0, "agent delete enabled!=0")
        expect(int(row["tv"]) > tv2, "agent delete token_version")
        expect(audit_exists("AGENT_DELETE", str(aid)), "audit AGENT_DELETE missing")
        # primary cannot delete
        resp = req("POST", f"/api/admin/agents/{primary_id}/delete", token=admin)
        expect(resp.get("code") != 0, f"primary delete should fail, got {resp}")
        prow = db_dict("SELECT enabled FROM agent_accounts WHERE id=%s", (primary_id,))
        expect(int(prow["enabled"]) == 1, "primary was wrongly disabled")

    run("admin_agent_ops+db", t_agent_ops)

    # ---------- tenant update ----------
    def t_tenant_update():
        before = db_dict("SELECT name, announcement FROM tenants WHERE id=%s", (tenant_id,))
        new_name = f"{before['name']}_t"
        new_ann = f"ann-{stamp}"
        assert_ok(
            req(
                "PUT",
                f"/api/admin/tenants/{tenant_id}",
                {"name": new_name, "announcement": new_ann},
                token=admin,
            ),
            "update tenant",
        )
        after = db_dict("SELECT name, announcement FROM tenants WHERE id=%s", (tenant_id,))
        expect(after["name"] == new_name, "tenant name not updated")
        expect(after["announcement"] == new_ann, "announcement not updated")
        expect(audit_exists("TENANT_UPDATE", str(tenant_id)), "audit TENANT_UPDATE missing")
        # restore name (keep announcement)
        assert_ok(
            req(
                "PUT",
                f"/api/admin/tenants/{tenant_id}",
                {"name": before["name"], "announcement": before["announcement"] or ""},
                token=admin,
            ),
            "restore tenant",
        )

    run("admin_tenant_update+db", t_tenant_update)

    # ---------- admin change password (then restore via DB hash swap carefully) ----------
    def t_admin_change_password():
        before = db_dict(
            "SELECT password_hash, COALESCE(token_version,0) tv FROM super_admins WHERE username='admin'",
        )
        # change to temp
        assert_ok(
            req(
                "PUT",
                "/api/admin/auth/password",
                {"oldPassword": "admin123", "newPassword": "admin123x"},
                token=admin,
            ),
            "admin change pwd",
        )
        after = db_dict(
            "SELECT password_hash, COALESCE(token_version,0) tv FROM super_admins WHERE username='admin'",
        )
        expect(after["password_hash"] != before["password_hash"], "admin hash unchanged")
        expect(int(after["tv"]) > int(before["tv"]), "admin token_version not bumped")
        expect(audit_exists("PASSWORD_CHANGE"), "audit PASSWORD_CHANGE missing")
        # old token should be invalid
        bad = req("GET", "/api/admin/me", token=admin)
        expect(bad.get("code") != 0, f"old token should fail, got {bad}")
        # login with new password
        new_tok = login_admin_new("admin123x")
        # change back
        assert_ok(
            req(
                "PUT",
                "/api/admin/auth/password",
                {"oldPassword": "admin123x", "newPassword": "admin123"},
                token=new_tok,
            ),
            "admin restore pwd",
        )
        globals()["admin"] = login_admin()

    def login_admin_new(pwd: str) -> str:
        data = assert_ok(req("POST", "/api/admin/auth/login", {"username": "admin", "password": pwd}), "admin relogin")
        return data["token"]

    run("admin_change_password+db", t_admin_change_password)
    admin = globals().get("admin") or login_admin()

    # ---------- agent-side user ops ----------
    def t_agent_user_ops():
        # reset primary agent password to known value for login
        reset = assert_ok(
            req("POST", f"/api/admin/agents/{primary_id}/reset-password", token=admin),
            "reset primary for agent login",
        )
        agent_token = login_agent(primary["username"], reset["rawPassword"])
        uname = f"au_{stamp}"
        data = assert_ok(
            req("POST", "/api/agent/users", {"username": uname}, token=agent_token),
            "agent create user",
        )
        auid = data["id"]
        row = db_dict("SELECT username, enabled, tenant_id, coin_balance FROM users WHERE id=%s", (auid,))
        expect(row and row["username"] == uname, "agent-created user missing")
        expect(str(row["tenant_id"]) == str(tenant_id), "agent create wrong tenant")
        expect(audit_exists("USER_CREATE", str(auid)), "agent USER_CREATE audit")

        bal0 = int(row["coin_balance"])
        coins = assert_ok(
            req("POST", f"/api/agent/users/{auid}/coins", {"amount": 20, "remark": "agent-test"}, token=agent_token),
            "agent coins",
        )
        expect(int(coins["coinBalance"]) == bal0 + 20, "agent coin api")
        bal1 = int(db_dict("SELECT coin_balance FROM users WHERE id=%s", (auid,))["coin_balance"])
        expect(bal1 == bal0 + 20, "agent coin db")

        before = db_dict("SELECT password_hash, COALESCE(token_version,0) tv FROM users WHERE id=%s", (auid,))
        assert_ok(req("POST", f"/api/agent/users/{auid}/reset-password", token=agent_token), "agent reset")
        after = db_dict("SELECT password_hash, COALESCE(token_version,0) tv FROM users WHERE id=%s", (auid,))
        expect(after["password_hash"] != before["password_hash"], "agent reset hash")
        expect(int(after["tv"]) > int(before["tv"]), "agent reset tv")

        tv = int(after["tv"])
        assert_ok(req("POST", f"/api/agent/users/{auid}/force-logout", token=agent_token), "agent force logout")
        tv2 = int(db_dict("SELECT COALESCE(token_version,0) tv FROM users WHERE id=%s", (auid,))["tv"])
        expect(tv2 > tv, "agent force logout tv")

        assert_ok(
            req("POST", "/api/agent/users/batch-enabled", {"ids": [auid], "enabled": 0}, token=agent_token),
            "agent batch disable",
        )
        expect(int(db_dict("SELECT enabled FROM users WHERE id=%s", (auid,))["enabled"]) == 0, "batch disable db")
        assert_ok(
            req("POST", "/api/agent/users/batch-enabled", {"ids": [auid], "enabled": 1}, token=agent_token),
            "agent batch enable",
        )
        expect(int(db_dict("SELECT enabled FROM users WHERE id=%s", (auid,))["enabled"]) == 1, "batch enable db")

        assert_ok(req("POST", f"/api/agent/users/{auid}/delete", token=agent_token), "agent delete user")
        row2 = db_dict("SELECT enabled FROM users WHERE id=%s", (auid,))
        expect(int(row2["enabled"]) == 0, "agent delete db")
        expect(audit_exists("USER_DELETE", str(auid)), "agent USER_DELETE audit")

        # agent change password
        before_a = db_dict(
            "SELECT password_hash, COALESCE(token_version,0) tv FROM agent_accounts WHERE id=%s",
            (primary_id,),
        )
        assert_ok(
            req(
                "PUT",
                "/api/agent/auth/password",
                {"oldPassword": reset["rawPassword"], "newPassword": "agentTmp9"},
                token=agent_token,
            ),
            "agent change pwd",
        )
        after_a = db_dict(
            "SELECT password_hash, COALESCE(token_version,0) tv FROM agent_accounts WHERE id=%s",
            (primary_id,),
        )
        expect(after_a["password_hash"] != before_a["password_hash"], "agent pwd hash")
        expect(int(after_a["tv"]) > int(before_a["tv"]), "agent pwd tv")
        # restore via admin reset
        assert_ok(req("POST", f"/api/admin/agents/{primary_id}/reset-password", token=admin), "restore agent pwd")

    run("agent_user_ops+db", t_agent_user_ops)

    # ---------- agent batch enable agents API ----------
    def t_admin_batch_agents():
        # create a disposable agent for batch
        aname = f"bag_{stamp}"
        data = assert_ok(
            req("POST", f"/api/admin/tenants/{tenant_id}/agents", {"username": aname}, token=admin),
            "create batch agent",
        )
        bid = data["id"]
        assert_ok(
            req("POST", "/api/admin/agents/batch-enabled", {"ids": [bid], "enabled": 0}, token=admin),
            "batch disable agents",
        )
        expect(int(db_dict("SELECT enabled FROM agent_accounts WHERE id=%s", (bid,))["enabled"]) == 0, "batch agent db0")
        assert_ok(
            req("POST", "/api/admin/agents/batch-enabled", {"ids": [bid], "enabled": 1}, token=admin),
            "batch enable agents",
        )
        expect(int(db_dict("SELECT enabled FROM agent_accounts WHERE id=%s", (bid,))["enabled"]) == 1, "batch agent db1")
        assert_ok(req("POST", f"/api/admin/agents/{bid}/delete", token=admin), "cleanup batch agent")

    run("admin_batch_agents+db", t_admin_batch_agents)

    print("\n======== SUMMARY ========")
    print(f"passed: {len(passed)}")
    print(f"failed: {len(failed)}")
    for name, err in failed:
        print(f"  - {name}: {err}")
    return 1 if failed else 0


if __name__ == "__main__":
    # wait briefly for API
    for i in range(30):
        try:
            r = req("POST", "/api/admin/auth/login", {"username": "admin", "password": "admin123"})
            if r.get("code") == 0:
                break
        except Exception:
            pass
        time.sleep(1)
    else:
        print("API not ready on :8080")
        sys.exit(2)
    sys.exit(main())
