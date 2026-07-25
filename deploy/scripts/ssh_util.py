#!/usr/bin/env python3
from __future__ import annotations

import sys
import time
from pathlib import Path

import paramiko

HOST = "45.152.64.102"

__all__ = ["HOST", "connect", "load_password", "load_host", "run"]


def _cred_lines() -> list[str]:
    txt = Path(__file__).resolve().parents[2] / "docs" / "服务器.txt"
    return [ln.strip() for ln in txt.read_text(encoding="utf-8").splitlines() if ln.strip()]


def load_host() -> str:
    import os

    if os.environ.get("LIUHECAI_SSH_HOST"):
        return os.environ["LIUHECAI_SSH_HOST"]
    lines = _cred_lines()
    if lines and lines[0][0].isdigit():
        return lines[0]
    return HOST


def load_password() -> str:
    import os

    if os.environ.get("LIUHECAI_SSH_PASSWORD"):
        return os.environ["LIUHECAI_SSH_PASSWORD"]
    lines = _cred_lines()
    return lines[-1]


def connect() -> paramiko.SSHClient:
    c = paramiko.SSHClient()
    c.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    c.connect(
        load_host(),
        username="root",
        password=load_password(),
        timeout=30,
        allow_agent=False,
        look_for_keys=False,
        banner_timeout=60,
    )
    return c


def _safe_print(chunk: str) -> None:
    try:
        print(chunk, end="", flush=True)
    except UnicodeEncodeError:
        try:
            sys.stdout.buffer.write(chunk.encode("utf-8", "replace"))
            sys.stdout.buffer.flush()
        except Exception:
            pass


def run(c: paramiko.SSHClient, cmd: str, timeout: int = 600) -> tuple[int, str]:
    print(">>>", cmd[:180].replace("\n", " "), flush=True)
    _, stdout, _ = c.exec_command(cmd, timeout=timeout, get_pty=True)
    buf: list[str] = []
    start = time.time()
    while not stdout.channel.exit_status_ready():
        if stdout.channel.recv_ready():
            chunk = stdout.channel.recv(4096).decode("utf-8", "replace")
            buf.append(chunk)
            _safe_print(chunk)
        elif time.time() - start > timeout:
            raise TimeoutError(cmd[:80])
        else:
            time.sleep(0.2)
    while stdout.channel.recv_ready():
        chunk = stdout.channel.recv(4096).decode("utf-8", "replace")
        buf.append(chunk)
        _safe_print(chunk)
    code = stdout.channel.recv_exit_status()
    print("\nexit", code, flush=True)
    return code, "".join(buf)
