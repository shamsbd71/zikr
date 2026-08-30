"""Wires up the tray icon, its menu, and the scheduler. Mirrors
ZikrReminderApp.swift + MenuContentView.swift.

Tray backend picks the best available option at import time:
  1. Ayatana AppIndicator3 (Ubuntu/most modern distros' standard)
  2. AppIndicator3 (older Ubuntu/Unity, some other distros)
  3. GtkStatusIcon (deprecated but universally present in GTK3; covers
     XFCE/MATE/KDE and any GNOME session without the AppIndicator
     extension installed)
"""
import threading
from pathlib import Path

import gi

gi.require_version("Gtk", "3.0")
from gi.repository import Gtk, GLib  # noqa: E402

_INDICATOR_BACKEND = None
try:
    gi.require_version("AyatanaAppIndicator3", "0.1")
    from gi.repository import AyatanaAppIndicator3 as AppIndicator3
    _INDICATOR_BACKEND = "ayatana"
except (ImportError, ValueError):
    try:
        gi.require_version("AppIndicator3", "0.1")
        from gi.repository import AppIndicator3
        _INDICATOR_BACKEND = "appindicator"
    except (ImportError, ValueError):
        AppIndicator3 = None

from . import __version__ as CURRENT_VERSION
from . import changelog, scheduler as scheduler_mod, update_checker
from .settings import Settings
from .ui import settings_window, update_dialog

_BUNDLED_ICON = Path(__file__).resolve().parent / "data" / "icon.png"
# Prefer a themed icon name (resolves via the icon theme, respects
# light/dark variants some themes provide); fall back to the bundled PNG's
# absolute path, which always works regardless of icon theme/cache state.
ICON_NAME = "zikr" if Path("/usr/share/icons/hicolor/256x256/apps/zikr.png").exists() else str(_BUNDLED_ICON)


class ZikrApp:
    def __init__(self):
        self.settings = Settings()
        self.scheduler = scheduler_mod.Scheduler(self.settings)
        self._build_tray()
        self.scheduler.start()
        GLib.timeout_add_seconds(5, self._startup_update_check)
        GLib.timeout_add_seconds(24 * 60 * 60, self._periodic_update_check)

    def run(self):
        Gtk.main()

    # ---- tray ----

    def _build_menu(self):
        menu = Gtk.Menu()

        active_item = Gtk.CheckMenuItem(label="Active")
        active_item.set_active(self.settings.enabled)
        active_item.connect("toggled", self._on_toggle_active)
        menu.append(active_item)

        test_item = Gtk.MenuItem(label="Test Zikr (Speak + Flash)")
        test_item.connect("activate", lambda *_: self.scheduler.fire_now())
        menu.append(test_item)

        menu.append(Gtk.SeparatorMenuItem())

        settings_item = Gtk.MenuItem(label="Settings…")
        settings_item.connect("activate", lambda *_: settings_window.show(self.settings, self._on_settings_changed))
        menu.append(settings_item)

        whatsnew_item = Gtk.MenuItem(label="What's New…")
        whatsnew_item.connect("activate", lambda *_: self._show_whats_new())
        menu.append(whatsnew_item)

        update_item = Gtk.MenuItem(label="Check for Updates…")
        update_item.connect("activate", lambda *_: self._check_for_updates(force=True))
        menu.append(update_item)

        menu.append(Gtk.SeparatorMenuItem())

        quit_item = Gtk.MenuItem(label="Quit Zikr")
        quit_item.connect("activate", lambda *_: Gtk.main_quit())
        menu.append(quit_item)

        menu.show_all()
        return menu

    def _build_tray(self):
        menu = self._build_menu()

        if AppIndicator3:
            self._indicator = AppIndicator3.Indicator.new(
                "zikr", ICON_NAME, AppIndicator3.IndicatorCategory.APPLICATION_STATUS
            )
            self._indicator.set_status(AppIndicator3.IndicatorStatus.ACTIVE)
            self._indicator.set_menu(menu)
        else:
            # GtkStatusIcon fallback — no AppIndicator library available.
            self._status_icon = Gtk.StatusIcon.new_from_icon_name(ICON_NAME)
            self._status_icon.set_tooltip_text("Zikr")
            self._status_icon.connect(
                "popup-menu",
                lambda icon, button, time: menu.popup(None, None, Gtk.StatusIcon.position_menu, icon, button, time),
            )
            self._status_icon.connect("activate", lambda *_: self.scheduler.fire_now())

    # ---- menu actions ----

    def _on_toggle_active(self, item):
        self.settings.enabled = item.get_active()
        self.scheduler.on_settings_changed()

    def _on_settings_changed(self):
        self.scheduler.on_settings_changed()

    # ---- updates ----

    def _startup_update_check(self):
        self._check_for_updates(force=False)
        return False  # one-shot

    def _periodic_update_check(self):
        self._check_for_updates(force=False)
        return True  # keep repeating

    def _check_for_updates(self, force):
        def worker():
            result = update_checker.check_for_update(CURRENT_VERSION)
            GLib.idle_add(self._handle_update_result, result, force)

        threading.Thread(target=worker, daemon=True).start()

    def _handle_update_result(self, result, force):
        status = result["status"]

        if status == "error":
            if force:
                update_dialog.show_message("Zikr Update", result["message"])
            return False

        if status == "up_to_date":
            if force:
                update_dialog.show_message("Zikr Update", f"You're up to date (v{CURRENT_VERSION}).")
            return False

        version = result["version"]
        if not force and version == self.settings.skipped_update_version:
            return False

        def worker():
            entries = changelog.fetch()
            body = next((e["body"] for e in entries if e["version"] == version), "")
            GLib.idle_add(self._show_update_dialog, version, body)

        threading.Thread(target=worker, daemon=True).start()
        return False

    def _show_update_dialog(self, version, body):
        def on_skip():
            self.settings.skipped_update_version = version

        update_dialog.show_update_available(
            current_version=CURRENT_VERSION, new_version=version, changelog_body=body, on_skip=on_skip,
        )
        return False

    def _show_whats_new(self):
        def worker():
            entries = changelog.fetch()
            GLib.idle_add(update_dialog.show_whats_new, entries)

        threading.Thread(target=worker, daemon=True).start()
