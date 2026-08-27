"""Persisted preferences, backed by a JSON file under $XDG_CONFIG_HOME.

Mirrors the macOS build's AppSettings: one small file, no database. Launch
at login is handled separately via an XDG autostart .desktop entry, since
that's the file the desktop environment actually looks for.
"""
import json
import os
import shutil
import sys
from pathlib import Path

CONFIG_DIR = Path(os.environ.get("XDG_CONFIG_HOME", Path.home() / ".config")) / "zikr"
CONFIG_FILE = CONFIG_DIR / "settings.json"
AUTOSTART_DIR = Path(os.environ.get("XDG_CONFIG_HOME", Path.home() / ".config")) / "autostart"
AUTOSTART_FILE = AUTOSTART_DIR / "zikr.desktop"

DEFAULTS = {
    "enabled": True,
    "min_interval_minutes": 20,
    "max_interval_minutes": 45,
    "display_style": "notification",  # "notification" | "flash"
    "speak_aloud": True,
    "flash_duration_seconds": 2.0,
    "launch_at_login": False,
    "pause_during_calls": True,
}


class Settings:
    def __init__(self):
        self._data = dict(DEFAULTS)
        self._load()
        self.launch_at_login = AUTOSTART_FILE.exists()

    def _load(self):
        if CONFIG_FILE.exists():
            try:
                on_disk = json.loads(CONFIG_FILE.read_text())
                self._data.update({k: v for k, v in on_disk.items() if k in DEFAULTS})
            except (json.JSONDecodeError, OSError):
                pass

    def save(self):
        CONFIG_DIR.mkdir(parents=True, exist_ok=True)
        CONFIG_FILE.write_text(json.dumps(self._data, indent=2))

    def __getattr__(self, name):
        if name in DEFAULTS:
            return self._data[name]
        raise AttributeError(name)

    def __setattr__(self, name, value):
        if name not in DEFAULTS:
            super().__setattr__(name, value)
            return

        self._data[name] = value
        # Keep min <= max, mirroring the macOS build's AppSettings, which
        # pushes the other bound along rather than silently accepting an
        # inverted range.
        if name == "min_interval_minutes" and value > self._data["max_interval_minutes"]:
            self._data["max_interval_minutes"] = value
        elif name == "max_interval_minutes" and value < self._data["min_interval_minutes"]:
            self._data["min_interval_minutes"] = value

        self.save()
        if name == "launch_at_login":
            _set_autostart(value)


def _zikr_command():
    """Resolve the command to launch Zikr, for the autostart entry."""
    exe = shutil.which("zikr")
    if exe:
        return exe
    return f"{sys.executable} -m zikr"


def _set_autostart(enabled):
    if enabled:
        AUTOSTART_DIR.mkdir(parents=True, exist_ok=True)
        AUTOSTART_FILE.write_text(
            "[Desktop Entry]\n"
            "Type=Application\n"
            "Name=Zikr\n"
            f"Exec={_zikr_command()}\n"
            "X-GNOME-Autostart-enabled=true\n"
            "NoDisplay=false\n"
        )
    else:
        AUTOSTART_FILE.unlink(missing_ok=True)
