#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
~/mcp/config.py
Integrasi mcp_config.json dengan main.py — hot-reload, tanpa restart.

Semua pengaturan ada di mcp_config.json. main.py cukup mengimpor:
    from config import (
        CFG,
        handle_message, parse_slash_command,
        check_allowed, is_path_allowed,
        resolve_tool_path, resolve_cwd, resolve_home, resolve_work_dir,
        needs_confirmation, status_text,
    )
"""

import json
import os
import re
import sys
from pathlib import Path

SCRIPT_DIR = Path(__file__).resolve().parent
CONFIG_PATH = SCRIPT_DIR / "mcp_config.json"

DEFAULT_CONFIG = {
    "version": 1,
    "server": {"name": "TermuxMCP", "log_level": "info"},
    "paths": {
        "home": "~/mcp/",
        "allow_dirs": "all",
        "work_dir": "default",
        "extra_work_dirs": [],
    },
    "commands": {
        "s_home":    {"enabled": True, "persist": True, "default": "~/mcp/"},
        "allow_dir": {"enabled": True, "persist": True, "default": "all", "separator": "&"},
        "s_dir":     {"enabled": True, "persist": True, "default": "default"},
        "set":       {"enabled": True, "persist": True, "default": "~/mcp/*"},
    },
    "agent_policy": {
        "mode": "ask",
        "plan": "ask",
        "auto_accept": False,
        "text_only": "ask",
        "default_choice": "ask",
        "allow_agent_change_config": False,
        "confirm_before_write": True,
        "confirm_before_edit": True,
        "confirm_before_delete": True,
        "confirm_before_execute": True,
        "confirm_before_read": False,
    },
    "tools": {
        "cmd":   {"enabled": True, "default_timeout": 60, "max_timeout": 600, "shell": "bash"},
        "edit":  {"enabled": True, "default_mode": "overwrite"},
        "tulis": {"enabled": True},
        "lihat": {"enabled": True, "default_limit": 500},
        "cari":  {"enabled": True},
        "hapus": {"enabled": True, "confirm": True},
    },
    "hot_reload": {"enabled": True, "interval_seconds": 2},
}


def deep_merge(base, override):
    if isinstance(base, dict) and isinstance(override, dict):
        out = dict(base)
        for k, v in override.items():
            out[k] = deep_merge(out.get(k), v)
        return out
    return override if override is not None else base


class ConfigManager:
    def __init__(self, path=CONFIG_PATH):
        self.path = Path(path)
        self._mtime = None
        self._config = deep_merge({}, DEFAULT_CONFIG)
        self.reload(force=True)

    def reload(self, force=False):
        try:
            if self.path.is_file():
                mtime = self.path.stat().st_mtime
                if force or mtime != self._mtime:
                    with open(self.path, "r", encoding="utf-8") as f:
                        loaded = json.load(f)
                    self._config = deep_merge(DEFAULT_CONFIG, loaded)
                    self._mtime = mtime
                    return True
        except Exception as exc:
            print(f"[config] gagal reload {self.path}: {exc}", file=sys.stderr)
        return False

    def get(self, dotted, default=None):
        self.reload()
        node = self._config
        for part in dotted.split("."):
            if isinstance(node, dict) and part in node:
                node = node[part]
            else:
                return default
        return node

    def set_value(self, dotted, value, persist=False):
        parts = dotted.split(".")
        node = self._config
        for part in parts[:-1]:
            node = node.setdefault(part, {})
        node[parts[-1]] = value
        if persist:
            self.save()

    def save(self):
        tmp = self.path.with_suffix(".json.tmp")
        with open(tmp, "w", encoding="utf-8") as f:
            json.dump(self._config, f, ensure_ascii=False, indent=2)
            f.write("\n")
            f.flush()
            os.fsync(f.fileno())
        os.replace(tmp, self.path)
        self._mtime = self.path.stat().st_mtime


CFG = ConfigManager()


# ---------------------------------------------------------------------------
# Path helper
# ---------------------------------------------------------------------------

def expand_path(raw):
    """Ubah nilai path dari config menjadi Path absolut.

    Nilai khusus:
      'default'                        -> home Termux (bawaan)
      'script' | '~/mcp/*' | 'mcp/*'   -> direktori tempat main.py/config.py berjalan
    """
    raw = str(raw or "").strip()
    if raw == "default":
        return Path.home()
    if raw in ("script", "~/mcp/*", "mcp/*"):
        return SCRIPT_DIR
    raw = os.path.expandvars(os.path.expanduser(raw))
    p = Path(raw)
    if not p.is_absolute():
        p = SCRIPT_DIR / p
    return p.resolve()


def resolve_home():
    """Direktori utama MCP (paths.home). Default ~/mcp/."""
    return expand_path(CFG.get("paths.home", "~/mcp/"))


def resolve_work_dir():
    """Direktori kerja aktif (paths.work_dir). default = home Termux."""
    return expand_path(CFG.get("paths.work_dir", "default"))


def resolve_tool_path(path):
    """Resolve path untuk tool file (edit/tulis/hapus/lihat/cari).

    - Path absolut atau berawalan ~ -> dipakai apa adanya.
    - Path relatif -> relatif terhadap work_dir bila di-set (bukan 'default'),
      selain itu relatif terhadap home (paths.home).
    """
    p = str(path or "")
    if p.startswith("~"):
        return Path(os.path.expanduser(p)).resolve()
    pp = Path(p)
    if pp.is_absolute():
        return pp.resolve()
    wd = CFG.get("paths.work_dir", "default")
    if wd not in ("", "default", None):
        base = expand_path(wd)
    else:
        base = resolve_home()
    return (base / pp).resolve()


def resolve_cwd(cwd):
    """Resolve cwd untuk tool cmd.

    - cwd eksplisit -> resolve_tool_path(cwd).
    - Kosong/None -> work_dir bila di-set, selain itu home Termux
      (sesuai '/s_dir > default' = direktori bawaan Termux).
    """
    if cwd not in (None, "", "."):
        return resolve_tool_path(cwd)
    wd = CFG.get("paths.work_dir", "default")
    if wd not in ("", "default", None):
        return expand_path(wd)
    return Path.home()


# ---------------------------------------------------------------------------
# Izin direktori (allow_dir)
# ---------------------------------------------------------------------------

def split_dir_list(raw):
    """Pecah nilai allow_dir: 'a & b && c, d; e' -> ['a','b','c','d','e']."""
    if raw is None:
        return []
    if isinstance(raw, list):
        return [str(x).strip() for x in raw if str(x).strip()]
    return [x.strip() for x in re.split(r"[&;,]+|\n+", str(raw)) if x.strip()]


def is_path_allowed(path):
    """True bila path berada di dalam allow_dirs (atau allow = all/*)."""
    allow = CFG.get("paths.allow_dirs", "all")
    try:
        target = Path(path).expanduser().resolve()
    except Exception:
        return False
    for raw in split_dir_list(allow):
        if raw in ("all", "*"):
            return True
        try:
            target.relative_to(expand_path(raw))
            return True
        except (ValueError, OSError):
            continue
    return False


def check_allowed(path, op="edit"):
    """Raise PermissionError bila path TIDAK diizinkan untuk operasi op."""
    if not is_path_allowed(path):
        raise PermissionError(
            f"[x] '{path}' TIDAK diizinkan untuk {op}. "
            f"allow_dir saat ini: {CFG.get('paths.allow_dirs')}. "
            f"Gunakan /allow_dir untuk mengubah."
        )
    return Path(path)


# ---------------------------------------------------------------------------
# Slash command: /s_home, /allow_dir, /s_dir, /set, /status, /help
# ---------------------------------------------------------------------------

SLASH_RE = re.compile(r"^\s*/([A-Za-z_][A-Za-z0-9_]*)\s*(?:>\s*(.*))?$")


def parse_slash_command(text):
    """Return (nama_command, value) atau None bila bukan slash command."""
    m = SLASH_RE.match(text or "")
    if not m:
        return None
    return m.group(1).lower(), (m.group(2) or "").strip()


def apply_slash_command(text):
    """Jalankan satu slash command. Return string balasan."""
    parsed = parse_slash_command(text)
    if parsed is None:
        return None
    name, value = parsed

    cmd = CFG.get(f"commands.{name}")
    if cmd is None:
        return f"[x] perintah /{name} tidak dikenal."
    if isinstance(cmd, dict) and cmd.get("enabled", True) is False:
        return f"[x] perintah /{name} dinonaktifkan di mcp_config.json."
    persist = bool(cmd.get("persist", True)) if isinstance(cmd, dict) else True

    if name == "s_home":
        v = value or (cmd.get("default", "~/mcp/") if isinstance(cmd, dict) else "~/mcp/")
        CFG.set_value("paths.home", v, persist=persist)
        return f"[ok] /s_home -> {v}  ({expand_path(v)})"

    if name == "allow_dir":
        v = value or (cmd.get("default", "all") if isinstance(cmd, dict) else "all")
        CFG.set_value("paths.allow_dirs", v, persist=persist)
        return f"[ok] /allow_dir -> {v}"

    if name == "s_dir":
        v = value or (cmd.get("default", "default") if isinstance(cmd, dict) else "default")
        CFG.set_value("paths.work_dir", v, persist=persist)
        return f"[ok] /s_dir -> {v}  ({resolve_work_dir()})"

    if name == "set":
        v = value or (cmd.get("default", "~/mcp/*") if isinstance(cmd, dict) else "~/mcp/*")
        CFG.set_value("paths.work_dir", v, persist=persist)
        extras = CFG.get("paths.extra_work_dirs", [])
        if not isinstance(extras, list):
            extras = []
        if v not in extras:
            extras.append(v)
            CFG.set_value("paths.extra_work_dirs", extras, persist=persist)
        return f"[ok] /set -> {v}  ({expand_path(v)})"

    return f"[x] perintah /{name} belum punya handler di config.py."


def handle_message(text):
    """Pintu masuk slash command. Return string balasan, atau None bila bukan command."""
    if text is None:
        return None
    t = (text or "").strip()
    if t.lower() in ("/status", "/info"):
        return status_text()
    if t.lower() == "/help":
        return (
            "Perintah:\n"
            "  /s_home > <path>       set direktori utama file (default ~/mcp/)\n"
            "  /allow_dir > <value>   izin dir; all/* = semua, atau: ~/mcp/ & ~/storage/emulated/0/\n"
            "  /s_dir > <value>       set dir kerja shell; default = bawaan Termux\n"
            "  /set > <value>         tambah/set dir kerja; ~/mcp/* = ikut lokasi script\n"
            "  /status                 lihat status config\n"
        )
    return apply_slash_command(t)


# ---------------------------------------------------------------------------
# Kebijakan agent (plan / auto_accept / text_only / konfirmasi)
# ---------------------------------------------------------------------------

def policy(key, default=None):
    return CFG.get(f"agent_policy.{key}", default)


def should_ask(key, default="ask"):
    v = policy(key, default)
    if isinstance(v, str):
        return v.lower() in ("ask", "confirm", "interactive")
    return bool(v)


def needs_confirmation(op):
    """Apakah operasi butuh konfirmasi user? op: write/edit/delete/execute/read."""
    if op == "read":
        return bool(policy("confirm_before_read", False))
    return bool(policy(f"confirm_before_{op}", True))


def agent_gate(action, op="execute"):
    """Kembalikan (ok, pesan). False artinya agent harus tanya user dulu."""
    mode = policy("mode", "ask")
    auto = bool(policy("auto_accept", False))
    if needs_confirmation(op) and not auto:
        return False, (
            f"[?] perlu konfirmasi untuk: {action}  "
            f"(mode={mode}, auto_accept={auto})"
        )
    return True, f"[ok] {action}"


def status_text():
    lines = [
        "[config]",
        f"  file      = {CONFIG_PATH}",
        f"  home      = {CFG.get('paths.home')} -> {resolve_home()}",
        f"  allow_dir = {CFG.get('paths.allow_dirs')}",
        f"  work_dir  = {CFG.get('paths.work_dir')} -> {resolve_work_dir()}",
        f"  extra     = {CFG.get('paths.extra_work_dirs')}",
        f"  policy    = mode={policy('mode', 'ask')} plan={policy('plan', 'ask')} "
        f"auto_accept={policy('auto_accept', False)} text_only={policy('text_only', 'ask')}",
        f"  hot_reload= {CFG.get('hot_reload.enabled', True)} "
        f"(interval {CFG.get('hot_reload.interval_seconds', 2)}s)",
    ]
    return "\n".join(lines)
