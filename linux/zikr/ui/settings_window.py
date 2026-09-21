"""The Settings window. Mirrors SettingsView.swift — same handful of
options, nothing more."""
import gi

gi.require_version("Gtk", "3.0")
from gi.repository import Gtk  # noqa: E402

_window = None


def show(settings, on_changed):
    global _window
    if _window:
        _window.present()
        return

    win = Gtk.Window(title="Zikr Settings")
    win.set_default_size(380, -1)
    win.set_resizable(False)
    win.set_border_width(18)

    def closed(*_a):
        global _window
        _window = None

    win.connect("destroy", closed)

    root = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=14)
    win.add(root)

    enabled_row = Gtk.CheckButton(label="Enable reminders")
    enabled_row.set_active(settings.enabled)

    def on_enabled(btn):
        settings.enabled = btn.get_active()
        on_changed()

    enabled_row.connect("toggled", on_enabled)
    root.pack_start(enabled_row, False, False, 0)

    root.pack_start(Gtk.Separator(), False, False, 0)

    timing_label = Gtk.Label(label="<b>Timing</b>", use_markup=True, xalign=0)
    root.pack_start(timing_label, False, False, 0)

    min_row = Gtk.Box(orientation=Gtk.Orientation.HORIZONTAL, spacing=10)
    min_row.pack_start(Gtk.Label(label="Min minutes", xalign=0, width_chars=12), False, False, 0)
    min_adj = Gtk.Adjustment(value=settings.min_interval_minutes, lower=1, upper=180, step_increment=1)
    min_spin = Gtk.SpinButton(adjustment=min_adj)

    def on_min(spin):
        settings.min_interval_minutes = int(spin.get_value())
        # Settings clamps max up if it's now below min — reflect that here.
        if max_spin.get_value() != settings.max_interval_minutes:
            max_spin.set_value(settings.max_interval_minutes)
        on_changed()

    min_spin.connect("value-changed", on_min)
    min_row.pack_start(min_spin, True, True, 0)
    root.pack_start(min_row, False, False, 0)

    max_row = Gtk.Box(orientation=Gtk.Orientation.HORIZONTAL, spacing=10)
    max_row.pack_start(Gtk.Label(label="Max minutes", xalign=0, width_chars=12), False, False, 0)
    max_adj = Gtk.Adjustment(value=settings.max_interval_minutes, lower=1, upper=180, step_increment=1)
    max_spin = Gtk.SpinButton(adjustment=max_adj)

    def on_max(spin):
        settings.max_interval_minutes = int(spin.get_value())
        # Settings clamps min down if it's now above max — reflect that here.
        if min_spin.get_value() != settings.min_interval_minutes:
            min_spin.set_value(settings.min_interval_minutes)
        on_changed()

    max_spin.connect("value-changed", on_max)
    max_row.pack_start(max_spin, True, True, 0)
    root.pack_start(max_row, False, False, 0)

    root.pack_start(Gtk.Separator(), False, False, 0)

    appearance_label = Gtk.Label(label="<b>Appearance</b>", use_markup=True, xalign=0)
    root.pack_start(appearance_label, False, False, 0)

    style_row = Gtk.Box(orientation=Gtk.Orientation.HORIZONTAL, spacing=10)
    style_row.pack_start(Gtk.Label(label="Style", xalign=0, width_chars=12), False, False, 0)
    style_combo = Gtk.ComboBoxText()
    style_combo.append("notification", "Notification")
    style_combo.append("flash", "Full-Screen Flash")
    style_combo.set_active_id(settings.display_style)

    def on_style(combo):
        settings.display_style = combo.get_active_id()
        on_changed()
        duration_row.set_visible(settings.display_style == "flash")

    style_combo.connect("changed", on_style)
    style_row.pack_start(style_combo, True, True, 0)
    root.pack_start(style_row, False, False, 0)

    duration_row = Gtk.Box(orientation=Gtk.Orientation.HORIZONTAL, spacing=10)
    duration_row.pack_start(Gtk.Label(label="Visible for (s)", xalign=0, width_chars=12), False, False, 0)
    duration_adj = Gtk.Adjustment(value=settings.flash_duration_seconds, lower=1, upper=8, step_increment=0.5)
    duration_spin = Gtk.SpinButton(adjustment=duration_adj, digits=1)

    def on_duration(spin):
        settings.flash_duration_seconds = round(spin.get_value(), 1)
        on_changed()

    duration_spin.connect("value-changed", on_duration)
    duration_row.pack_start(duration_spin, True, True, 0)
    root.pack_start(duration_row, False, False, 0)
    duration_row.set_visible(settings.display_style == "flash")

    speak_row = Gtk.CheckButton(label="Speak zikr aloud")
    speak_row.set_active(settings.speak_aloud)

    def on_speak(btn):
        settings.speak_aloud = btn.get_active()
        on_changed()

    speak_row.connect("toggled", on_speak)
    root.pack_start(speak_row, False, False, 0)

    root.pack_start(Gtk.Separator(), False, False, 0)

    system_label = Gtk.Label(label="<b>System</b>", use_markup=True, xalign=0)
    root.pack_start(system_label, False, False, 0)

    login_row = Gtk.CheckButton(label="Launch at login")
    login_row.set_active(settings.launch_at_login)

    def on_login(btn):
        settings.launch_at_login = btn.get_active()

    login_row.connect("toggled", on_login)
    root.pack_start(login_row, False, False, 0)

    calls_row = Gtk.CheckButton(label="Pause during calls (mic in use)")
    calls_row.set_active(settings.pause_during_calls)

    def on_calls(btn):
        settings.pause_during_calls = btn.get_active()

    calls_row.connect("toggled", on_calls)
    root.pack_start(calls_row, False, False, 0)

    root.pack_start(Gtk.Separator(), False, False, 0)

    quiet_label = Gtk.Label(label="<b>Quiet Hours</b>", use_markup=True, xalign=0)
    root.pack_start(quiet_label, False, False, 0)

    quiet_row = Gtk.CheckButton(label="Turn off reminders during a daily window")
    quiet_row.set_active(settings.quiet_hours_enabled)
    root.pack_start(quiet_row, False, False, 0)

    def time_row(label_text, initial_minutes):
        row = Gtk.Box(orientation=Gtk.Orientation.HORIZONTAL, spacing=10)
        row.pack_start(Gtk.Label(label=label_text, xalign=0, width_chars=12), False, False, 0)
        hour_adj = Gtk.Adjustment(value=initial_minutes // 60, lower=0, upper=23, step_increment=1)
        hour_spin = Gtk.SpinButton(adjustment=hour_adj)
        row.pack_start(hour_spin, False, False, 0)
        row.pack_start(Gtk.Label(label=":"), False, False, 0)
        minute_adj = Gtk.Adjustment(value=initial_minutes % 60, lower=0, upper=59, step_increment=1)
        minute_spin = Gtk.SpinButton(adjustment=minute_adj)
        row.pack_start(minute_spin, False, False, 0)
        return row, hour_spin, minute_spin

    start_row, start_hour_spin, start_minute_spin = time_row("From", settings.quiet_hours_start_minutes)
    root.pack_start(start_row, False, False, 0)

    end_row, end_hour_spin, end_minute_spin = time_row("To", settings.quiet_hours_end_minutes)
    root.pack_start(end_row, False, False, 0)

    def on_start_time(_spin):
        settings.quiet_hours_start_minutes = int(start_hour_spin.get_value()) * 60 + int(start_minute_spin.get_value())

    start_hour_spin.connect("value-changed", on_start_time)
    start_minute_spin.connect("value-changed", on_start_time)

    def on_end_time(_spin):
        settings.quiet_hours_end_minutes = int(end_hour_spin.get_value()) * 60 + int(end_minute_spin.get_value())

    end_hour_spin.connect("value-changed", on_end_time)
    end_minute_spin.connect("value-changed", on_end_time)

    def on_quiet(btn):
        settings.quiet_hours_enabled = btn.get_active()
        start_row.set_visible(settings.quiet_hours_enabled)
        end_row.set_visible(settings.quiet_hours_enabled)

    quiet_row.connect("toggled", on_quiet)
    start_row.set_visible(settings.quiet_hours_enabled)
    end_row.set_visible(settings.quiet_hours_enabled)

    root.pack_start(Gtk.Separator(), False, False, 0)

    test_btn = Gtk.Button(label="Test Zikr (Speak + Flash)")

    def on_test(_btn):
        from .. import flash, speech
        from ..zikr_data import random_zikr
        z = random_zikr()
        if settings.speak_aloud:
            speech.speak(z)
        flash.present(z, settings.flash_duration_seconds)

    test_btn.connect("clicked", on_test)
    root.pack_start(test_btn, False, False, 0)

    hint = Gtk.Label(
        label="Test always speaks the zikr aloud and flashes it on screen,\nregardless of the Style setting above.",
        xalign=0,
    )
    hint.get_style_context().add_class("dim-label")
    root.pack_start(hint, False, False, 0)

    win.show_all()
    _window = win
