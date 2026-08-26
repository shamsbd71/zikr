"""Desktop notifications via libnotify (freedesktop.org Notifications spec)
— the standard notification mechanism across GNOME, KDE, XFCE, and most
other Linux desktops. Mirrors NotificationManager.swift on macOS.
"""
import gi

gi.require_version("Notify", "0.7")
from gi.repository import Notify  # noqa: E402

_initialized = False


def _ensure_init():
    global _initialized
    if not _initialized:
        Notify.init("Zikr")
        _initialized = True


def deliver(zikr):
    _ensure_init()
    body = f"{zikr['arabic']}\n{zikr['translation']}"
    notification = Notify.Notification.new(zikr["transliteration"], body, "zikr")
    try:
        notification.show()
    except Exception:
        pass
