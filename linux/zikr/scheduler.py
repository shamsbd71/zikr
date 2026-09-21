"""Drives the app: one GLib timer that fires at a random interval, shows a
random zikr, then reschedules itself. Mirrors ReminderScheduler.swift —
no polling, no retained history.
"""
import datetime
import random

from gi.repository import GLib

from . import flash, mic_monitor, notifications, speech
from .zikr_data import random_zikr


def pick_delay_seconds(min_interval_minutes, max_interval_minutes):
    """Pure function so the random-interval math is unit-testable without
    a GLib main loop."""
    lo = max(1, min_interval_minutes) * 60
    hi = max(lo, max_interval_minutes * 60)
    return random.uniform(lo, hi)


def is_within_quiet_hours(now_minutes, start_minutes, end_minutes):
    """Pure so the wraparound logic (a window like 22:00-06:00 that
    crosses midnight) is easy to test independent of the clock. Equal
    bounds means "no window" rather than "always on" - a user who hasn't
    set both ends yet shouldn't get silenced entirely by accident."""
    if start_minutes == end_minutes:
        return False
    if start_minutes < end_minutes:
        return start_minutes <= now_minutes < end_minutes
    return now_minutes >= start_minutes or now_minutes < end_minutes


def _current_minutes_of_day():
    now = datetime.datetime.now()
    return now.hour * 60 + now.minute


class Scheduler:
    def __init__(self, settings):
        self.settings = settings
        self._source_id = None

    def start(self):
        if self.settings.enabled:
            self._schedule_next()

    def stop(self):
        if self._source_id:
            GLib.source_remove(self._source_id)
            self._source_id = None

    def on_settings_changed(self):
        self.stop()
        if self.settings.enabled:
            self._schedule_next()

    def fire_now(self):
        self._show(random_zikr())

    def _schedule_next(self):
        self.stop()
        delay_seconds = pick_delay_seconds(
            self.settings.min_interval_minutes, self.settings.max_interval_minutes
        )
        self._source_id = GLib.timeout_add(int(delay_seconds * 1000), self._fire)

    def _fire(self):
        in_quiet_hours = self.settings.quiet_hours_enabled and is_within_quiet_hours(
            _current_minutes_of_day(),
            self.settings.quiet_hours_start_minutes,
            self.settings.quiet_hours_end_minutes,
        )
        if not (self.settings.pause_during_calls and mic_monitor.is_microphone_in_use()) and not in_quiet_hours:
            self._show(random_zikr())
        if self.settings.enabled:
            self._schedule_next()
        return False

    def _show(self, zikr):
        if self.settings.speak_aloud:
            speech.speak(zikr)
        if self.settings.display_style == "flash":
            flash.present(zikr, self.settings.flash_duration_seconds)
        else:
            notifications.deliver(zikr)
