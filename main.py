#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Termux MCP server terintegrasi dengan mcp_config.json (hot-reload).

Semua konfigurasi ada di mcp_config.json, tidak hardcoded di sini.
Perintah slash: /s_home, /allow_dir, /s_dir, /set, /status, /help
"""

import json
import os
import sys
import shutil
import subprocess
import socket
import threading
import glob as g
from datetime import datetime

# Pastikan config.py dari direktori yang sama bisa diimpor
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from config import (
    CFG,
    handle_message,
    parse_slash_command,
    check_allowed,
    is_path_allowed,
    resolve_tool_path,
    resolve_cwd,
    status_text,
    needs_confirmation,
)

HOST = os.environ.get("MCP_HOST", "127.0.0.1")
PORT = int(os.environ.get("MCP_PORT", "9998"))

STATUS_REASONS = {
    200: "OK",
    202: "Accepted",
    400: "Bad Request",
    404: "Not Found",
    500: "Internal Server Error",
}


def log(msg):
    now = datetime.now().strftime("%H:%M:%S")
    print(f"[{now}] {msg}", file=sys.stderr, flush=True)


# ---------------------------------------------------------------------------
# Tool: cmd
# ---------------------------------------------------------------------------

def cmd(command, cwd=None, timeout=None):
    # 1) Slash command ditangani lebih dulu (tidak menjalankan shell)
    slash_reply = handle_message(command)
    if slash_reply is not None:
        return {
            "ok": True,
            "slash": True,
            "stdout": slash_reply,
            "stderr": "",
            "code": 0,
        }

    # 2) Timeout dari config
    if timeout is None:
        timeout = int(CFG.get("tools.cmd.default_timeout", 60) or 60)
    max_timeout = int(CFG.get("tools.cmd.max_timeout", 600) or 600)
    if timeout > max_timeout:
        timeout = max_timeout

    # 3) Resolve cwd + cek izin direktori
    try:
        resolved_cwd = str(resolve_cwd(cwd))
        check_allowed(resolved_cwd, "cmd")
    except PermissionError as e:
        return {"ok": False, "error": str(e)}

    try:
        r = subprocess.run(
            command,
            shell=True,
            cwd=resolved_cwd,
            capture_output=True,
            text=True,
            timeout=timeout,
        )
        return {
            "ok": True,
            "cwd": resolved_cwd,
            "stdout": r.stdout,
            "stderr": r.stderr,
            "code": r.returncode,
        }
    except subprocess.TimeoutExpired:
        return {"ok": False, "error": f"timeout ({timeout}s)"}
    except Exception as e:
        return {"ok": False, "error": str(e)}


# ---------------------------------------------------------------------------
# Tool: edit / tulis
# ---------------------------------------------------------------------------

def edit(path, content, mode=None):
    if mode is None:
        mode = CFG.get("tools.edit.default_mode", "overwrite") or "overwrite"
    if mode not in ("overwrite", "append"):
        return {"ok": False, "error": f"invalid mode: {mode}"}
    try:
        target = resolve_tool_path(path)
        check_allowed(target, "edit")
        os.makedirs(os.path.dirname(str(target)) or ".", exist_ok=True)
        wm = "w" if mode == "overwrite" else "a"
        with open(target, wm, encoding="utf-8") as f:
            f.write(content)
        return {
            "ok": True,
            "path": str(target),
            "size": os.path.getsize(target),
            "mode": mode,
        }
    except PermissionError as e:
        return {"ok": False, "error": str(e)}
    except Exception as e:
        return {"ok": False, "error": str(e)}


def tulis(path, content):
    return edit(path, content, "overwrite")


# ---------------------------------------------------------------------------
# Tool: hapus
# ---------------------------------------------------------------------------

def hapus(path):
    try:
        target = resolve_tool_path(path)
        check_allowed(target, "hapus")
        if os.path.isdir(target) and not os.path.islink(target):
            shutil.rmtree(target)
        else:
            os.remove(target)
        return {"ok": True, "deleted": str(target)}
    except PermissionError as e:
        return {"ok": False, "error": str(e)}
    except Exception as e:
        return {"ok": False, "error": str(e)}


# ---------------------------------------------------------------------------
# Tool: lihat
# ---------------------------------------------------------------------------

def lihat(path, start=0, limit=None):
    try:
        target = resolve_tool_path(path)
        check_allowed(target, "lihat")
        with open(target, "r", encoding="utf-8", errors="replace") as f:
            lines = f.readlines()
        total = len(lines)
        if limit is None:
            chunk = lines[start:]
        else:
            chunk = lines[start : start + limit]
        return {
            "ok": True,
            "path": str(target),
            "total": total,
            "start": start,
            "end": start + len(chunk),
            "text": "".join(chunk),
        }
    except PermissionError as e:
        return {"ok": False, "error": str(e)}
    except Exception as e:
        return {"ok": False, "error": str(e)}


# ---------------------------------------------------------------------------
# Tool: cari
# ---------------------------------------------------------------------------

def cari(pattern, path="."):
    try:
        base = resolve_tool_path(path or ".")
        check_allowed(base, "cari")
        pattern_full = os.path.join(str(base), pattern)
        matches = g.glob(pattern_full, recursive=True)
        results = []
        for m in matches:
            # Hanya kembalikan hasil di dalam allow_dirs
            if not is_path_allowed(m):
                continue
            is_dir = os.path.isdir(m)
            results.append(
                {
                    "path": m,
                    "type": "dir" if is_dir else "file",
                    "size": os.path.getsize(m) if not is_dir else 0,
                }
            )
        return {
            "ok": True,
            "base": str(base),
            "pattern": pattern,
            "count": len(results),
            "results": results,
        }
    except PermissionError as e:
        return {"ok": False, "error": str(e)}
    except Exception as e:
        return {"ok": False, "error": str(e)}


TOOLS = {
    "cmd": cmd,
    "edit": edit,
    "hapus": hapus,
    "tulis": tulis,
    "lihat": lihat,
    "cari": cari,
}

# Operasi tiap tool untuk kebijakan konfirmasi agent_policy
TOOL_OPS = {
    "cmd": "execute",
    "edit": "edit",
    "tulis": "write",
    "hapus": "delete",
    "lihat": "read",
    "cari": "read",
}


TOOLS_SCHEMA = [
    {
        "name": "cmd",
        "description": "Jalankan perintah shell tanpa batas output/waktu. Mendukung slash command: /s_home, /allow_dir, /s_dir, /set, /status, /help. Tambahkan confirm=true bila kebijakan meminta konfirmasi.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "command": {"type": "string"},
                "cwd": {"type": "string"},
                "timeout": {"type": "integer"},
                "confirm": {"type": "boolean"},
            },
            "required": ["command"],
        },
    },
    {
        "name": "edit",
        "description": "Tulis, timpa, atau tambah isi file tanpa batas ukuran",
        "inputSchema": {
            "type": "object",
            "properties": {
                "path": {"type": "string"},
                "content": {"type": "string"},
                "mode": {"type": "string", "enum": ["overwrite", "append"]},
                "confirm": {"type": "boolean"},
            },
            "required": ["path", "content"],
        },
    },
    {
        "name": "hapus",
        "description": "Hapus file atau folder",
        "inputSchema": {
            "type": "object",
            "properties": {
                "path": {"type": "string"},
                "confirm": {"type": "boolean"},
            },
            "required": ["path"],
        },
    },
    {
        "name": "tulis",
        "description": "Buat file baru tanpa batas ukuran",
        "inputSchema": {
            "type": "object",
            "properties": {
                "path": {"type": "string"},
                "content": {"type": "string"},
                "confirm": {"type": "boolean"},
            },
            "required": ["path", "content"],
        },
    },
    {
        "name": "lihat",
        "description": "Baca isi file, limit kosong berarti semua baris",
        "inputSchema": {
            "type": "object",
            "properties": {
                "path": {"type": "string"},
                "start": {"type": "integer"},
                "limit": {"type": "integer"},
            },
            "required": ["path"],
        },
    },
    {
        "name": "cari",
        "description": "Cari file dengan wildcard, semua hasil",
        "inputSchema": {
            "type": "object",
            "properties": {
                "pattern": {"type": "string"},
                "path": {"type": "string"},
            },
            "required": ["pattern"],
        },
    },
]


# ---------------------------------------------------------------------------
# RPC
# ---------------------------------------------------------------------------

def handle_rpc(req):
    method = req.get("method", "")
    rid = req.get("id", 0)

    if method == "initialize":
        return {
            "jsonrpc": "2.0",
            "id": rid,
            "result": {
                "protocolVersion": "2024-11-05",
                "capabilities": {"tools": {}},
                "serverInfo": {"name": "TermuxMCP", "version": "5.1.0"},
            },
        }
    if method == "notifications/initialized":
        return None
    if method == "tools/list":
        return {"jsonrpc": "2.0", "id": rid, "result": {"tools": TOOLS_SCHEMA}}
    if method == "tools/call":
        params = req.get("params", {})
        name = params.get("name", "")
        args = params.get("arguments", {})
        if name not in TOOLS:
            return {
                "jsonrpc": "2.0",
                "id": rid,
                "error": {"code": -32601, "message": f"Unknown tool: {name}"},
            }

        log(f"{name}({str(args)[:150]})")

        # 1) Tool dinonaktifkan?
        if CFG.get(f"tools.{name}.enabled", True) is False:
            return tool_result(rid, {
                "ok": False,
                "error": f"tool '{name}' dinonaktifkan di mcp_config.json (tools.{name}.enabled=false)",
            })

        # 2) Slash command di cmd tidak perlu konfirmasi eksekusi
        is_slash = name == "cmd" and parse_slash_command(args.get("command", "")) is not None

        # 3) Kebijakan konfirmasi agent (plan/auto_accept/confirm_before_*)
        if not is_slash:
            op = TOOL_OPS.get(name, "execute")
            if needs_confirmation(op) and not bool(CFG.get("agent_policy.auto_accept", False)) and args.get("confirm") is not True:
                return tool_result(rid, {
                    "ok": False,
                    "confirm_required": True,
                    "tool": name,
                    "op": op,
                    "message": (
                        f"Kebijakan agent meminta konfirmasi untuk operasi '{op}' "
                        f"pada tool '{name}'."
                    ),
                    "hint": (
                        "Minta izin user terlebih dahulu. Jika user setuju, "
                        "panggil ulang tool yang sama dengan argumen confirm=true."
                    ),
                })

        # 4) Buang argumen kontrol 'confirm' sebelum memanggil tool
        call_args = dict(args)
        call_args.pop("confirm", None)

        # 5) Jalankan tool
        try:
            result = TOOLS[name](**call_args)
            return tool_result(rid, result)
        except TypeError as e:
            return {
                "jsonrpc": "2.0",
                "id": rid,
                "error": {"code": -32602, "message": f"Invalid params: {e}"},
            }
        except Exception as e:
            return {
                "jsonrpc": "2.0",
                "id": rid,
                "error": {"code": -32603, "message": str(e)},
            }
    if method == "ping":
        return {"jsonrpc": "2.0", "id": rid, "result": {}}

    return {
        "jsonrpc": "2.0",
        "id": rid,
        "error": {"code": -32601, "message": f"Unknown method: {method}"},
    }


def tool_result(rid, result):
    return {
        "jsonrpc": "2.0",
        "id": rid,
        "result": {
            "content": [
                {
                    "type": "text",
                    "text": json.dumps(result, indent=2, ensure_ascii=False),
                }
            ]
        },
    }


# ---------------------------------------------------------------------------
# HTTP
# ---------------------------------------------------------------------------

def http_resp(body, status=200):
    reason = STATUS_REASONS.get(status, "OK")
    head = (
        f"HTTP/1.1 {status} {reason}\r\n"
        "Content-Type: application/json; charset=utf-8\r\n"
        f"Content-Length: {len(body)}\r\n"
        "Access-Control-Allow-Origin: *\r\n"
        "Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n"
        "Access-Control-Allow-Headers: Content-Type, Accept\r\n"
        "Connection: close\r\n\r\n"
    ).encode()
    return head + body


def read_http_request(conn):
    data = b""
    while b"\r\n\r\n" not in data:
        chunk = conn.recv(65536)
        if not chunk:
            break
        data += chunk

    if not data or b"\r\n\r\n" not in data:
        return None

    header_raw, _, body = data.partition(b"\r\n\r\n")
    try:
        text = header_raw.decode("utf-8", errors="replace")
    except Exception:
        return None

    lines = text.split("\r\n")
    parts = lines[0].split(" ")
    method = parts[0].upper()
    path = parts[1] if len(parts) > 1 else "/"

    headers = {}
    for line in lines[1:]:
        if ":" in line:
            k, v = line.split(":", 1)
            headers[k.strip().lower()] = v.strip()

    try:
        length = int(headers.get("content-length", "0") or 0)
    except ValueError:
        return None

    if length < 0:
        return None

    while len(body) < length:
        chunk = conn.recv(min(65536, length - len(body)))
        if not chunk:
            break
        body += chunk

    return {"method": method, "path": path, "headers": headers, "body": body[:length]}


def config_summary():
    return {
        "home": CFG.get("paths.home"),
        "allow_dirs": CFG.get("paths.allow_dirs"),
        "work_dir": CFG.get("paths.work_dir"),
        "extra_work_dirs": CFG.get("paths.extra_work_dirs"),
        "agent_policy": CFG.get("agent_policy"),
        "hot_reload": CFG.get("hot_reload"),
    }


def handle_client(conn, addr):
    try:
        req = read_http_request(conn)
        if req is None:
            return

        method, path, body = req["method"], req["path"], req["body"]

        if method == "OPTIONS":
            conn.sendall(http_resp(b"", 200))
            return

        if method == "GET" and path == "/":
            info = {
                "server": "TermuxMCP",
                "status": "online",
                "tools": [t["name"] for t in TOOLS_SCHEMA],
                "time": datetime.now().isoformat(),
                "config": config_summary(),
            }
            conn.sendall(http_resp(json.dumps(info, indent=2).encode()))
        elif method == "POST" and path == "/":
            try:
                mcp_req = json.loads(body or "{}")
                result = handle_rpc(mcp_req)
                if result is None:
                    conn.sendall(http_resp(b"", 202))
                else:
                    conn.sendall(http_resp(json.dumps(result).encode()))
            except json.JSONDecodeError:
                err = {
                    "jsonrpc": "2.0",
                    "error": {"code": -32700, "message": "Parse error"},
                }
                conn.sendall(http_resp(json.dumps(err).encode(), 400))
        else:
            conn.sendall(http_resp(json.dumps({"error": "Not found"}).encode(), 404))
    except (BrokenPipeError, ConnectionResetError):
        pass
    except Exception as e:
        log(f"{addr}: {e}")
    finally:
        try:
            conn.close()
        except Exception:
            pass


def main():
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    sock.bind((HOST, PORT))
    sock.listen(20)
    log(f"Listening on {HOST}:{PORT}")
    log(f"Config: {status_text().replace(chr(10), ' | ')}")
    while True:
        try:
            conn, addr = sock.accept()
            threading.Thread(target=handle_client, args=(conn, addr), daemon=True).start()
        except KeyboardInterrupt:
            log("Stopped")
            break
    sock.close()


if __name__ == "__main__":
    main()
