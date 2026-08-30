"""Detects whether the microphone is currently in use by any application
— this one or another, e.g. a browser tab in a Google Meet call — so a
reminder can be skipped rather than spoken over a meeting.

Two independent signals are checked, in order:
  1. `pactl list short source-outputs` — PulseAudio/PipeWire (the sound
     server on effectively every desktop Linux distro today) lists one
     row per active recording stream, regardless of which app opened it.
  2. ALSA's own `/proc/asound/card*/pcm*c/sub*/status`, for the rare
     system running bare ALSA with no sound server — the kernel reports
     "state: RUNNING" while a capture substream is open.

The parsing logic is split into pure functions so it's unit-testable
without a real audio stack, mirroring pick_delay_seconds in scheduler.py.
"""
import glob
import subprocess


def _parse_pactl_source_outputs(output_text):
    """`pactl list short source-outputs` prints one line per active
    recording stream, nothing at all when the mic is idle."""
    return bool(output_text.strip())


def _parse_alsa_status(status_text):
    return "RUNNING" in status_text


def _pactl_reports_active_input():
    try:
        result = subprocess.run(
            ["pactl", "list", "short", "source-outputs"],
            capture_output=True, text=True, timeout=2,
        )
    except (OSError, subprocess.TimeoutExpired):
        return None  # pactl unavailable — not a signal either way
    if result.returncode != 0:
        return None
    return _parse_pactl_source_outputs(result.stdout)


def _alsa_reports_active_capture():
    status_files = glob.glob("/proc/asound/card*/pcm*c/sub*/status")
    if not status_files:
        return None  # no ALSA capture devices visible — not a signal either way
    for path in status_files:
        try:
            with open(path) as f:
                if _parse_alsa_status(f.read()):
                    return True
        except OSError:
            continue
    return False


def is_microphone_in_use():
    """True if a capture stream looks active; False if checked and found
    idle; effectively False (permissive) if neither signal is readable
    on this system, so the feature never blocks reminders outright on a
    system it can't inspect."""
    for check in (_pactl_reports_active_input, _alsa_reports_active_capture):
        result = check()
        if result is not None:
            return result
    return False
