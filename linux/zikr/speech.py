"""Speaks a zikr aloud.

Mirrors the macOS build's ZikrSpeaker: prefer a bundled audio clip for the
phrase if one exists (assets/audio/<id>.*), otherwise fall back to text-to-
speech. On Linux that's speech-dispatcher (spd-say) — the desktop-standard
TTS abstraction used by GNOME/KDE accessibility — falling back to espeak-ng
directly if speech-dispatcher isn't installed. Both are queried for an
Arabic voice; if neither has one, the transliteration is spoken in English
instead so the app still says *something* meaningful.
"""
import shutil
import subprocess
from pathlib import Path

ASSETS_AUDIO_DIRS = [
    Path(__file__).resolve().parent / "data" / "audio",
    Path("/usr/share/zikr/audio"),
]

AUDIO_PLAYERS = ["paplay", "ffplay", "aplay"]


def _bundled_clip(zikr_id):
    for base in ASSETS_AUDIO_DIRS:
        for ext in ("ogg", "mp3", "wav", "flac"):
            path = base / f"{zikr_id}.{ext}"
            if path.exists():
                return path
    return None


def _play_clip(path):
    player = next((p for p in AUDIO_PLAYERS if shutil.which(p)), None)
    if not player:
        return False
    args = [player, "-nodisp", "-autoexit", str(path)] if player == "ffplay" else [player, str(path)]
    subprocess.Popen(args, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    return True


def _speak_spd(text, lang):
    if not shutil.which("spd-say"):
        return False
    subprocess.Popen(
        ["spd-say", "-l", lang, "-r", "-15", text],
        stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
    )
    return True


def _speak_espeak(text, lang):
    exe = shutil.which("espeak-ng") or shutil.which("espeak")
    if not exe:
        return False
    subprocess.Popen(
        [exe, "-v", lang, "-s", "140", text],
        stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
    )
    return True


def speak(zikr):
    clip = _bundled_clip(zikr["id"])
    if clip and _play_clip(clip):
        return

    if _speak_spd(zikr["arabic"], "ar") or _speak_espeak(zikr["arabic"], "ar"):
        return

    # No Arabic voice available anywhere on this system — fall back to
    # reading the transliteration in English rather than staying silent.
    _speak_spd(zikr["transliteration"], "en") or _speak_espeak(zikr["transliteration"], "en")
