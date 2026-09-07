#!/usr/bin/env python3
"""Fetch and prepare per-phrase recitation clips for the zikr list.

Source is hisnmuslim.com's per-dua catalogue: every dua in Hisn al-Muslim
has its own MP3 at /audio/ar/<id>.mp3, all recited by حمد الدريهم, so the
whole set is one consistent voice.

Two things make this more than a download loop:

  * The files carry an embedded cover-art MJPEG stream. Any ffmpeg filter
    that doesn't say `-map 0:a` silently does nothing at all.
  * Each clip recites its phrase two to four times with gaps between.
    Trimming silence is not enough - we take the *first* utterance only,
    found from an RMS envelope rather than ffmpeg's silencedetect (which
    finds no silence in these at any threshold, because the reverb tail
    never drops below the noise floor).

Reads `source` ("hisn:<dua id>") from data/zikr.json, writes clips to
data/audio/<zikr id>.mp3, and writes back the measured duration as
`seconds` so the long-zikr gate uses real numbers, not estimates.

Usage:  python3 tools/fetch_audio.py [--dry-run] [--only 1,3,7]
"""
import argparse
import array
import json
import math
import subprocess
import sys
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ZIKR_JSON = ROOT / "data" / "zikr.json"
AUDIO_DIR = ROOT / "data" / "audio"
UA = {"User-Agent": "zikr-audio-fetch/1.0 (+https://github.com/abu/zikr)"}

ENVELOPE_RATE = 8000      # Hz, mono - plenty to locate speech
WINDOW = 0.05             # 50ms envelope resolution
GATE_BELOW_PEAK = 22      # dB below peak counts as speech
MIN_GAP = 0.45            # s of quiet that ends the first utterance
PAD = 0.18                # s kept either side


def download(dua_id, dest):
    url = f"https://www.hisnmuslim.com/audio/ar/{dua_id}.mp3"
    req = urllib.request.Request(url, headers=UA)
    dest.write_bytes(urllib.request.urlopen(req, timeout=60).read())


def envelope(path):
    """Per-window RMS in dBFS. `-map 0:a` skips the cover-art stream."""
    raw = subprocess.run(
        ["ffmpeg", "-v", "error", "-i", str(path), "-map", "0:a",
         "-ac", "1", "-ar", str(ENVELOPE_RATE), "-f", "s16le", "-"],
        capture_output=True, check=True).stdout
    samples = array.array("h")
    samples.frombytes(raw[: len(raw) // 2 * 2])
    step = int(ENVELOPE_RATE * WINDOW)
    out = []
    for i in range(0, len(samples) - step, step):
        chunk = samples[i:i + step]
        power = sum(v * v for v in chunk) / step
        out.append(-99.0 if power <= 0 else 20 * math.log10(math.sqrt(power) / 32768))
    return out


def first_utterance(env):
    """(start, end) seconds of the first contiguous speech burst."""
    if not env:
        return None
    gate = max(env) - GATE_BELOW_PEAK
    loud = [level > gate for level in env]
    if True not in loud:
        return None
    start = loud.index(True)
    end, quiet = start, 0.0
    for i in range(start, len(loud)):
        if loud[i]:
            end, quiet = i, 0.0
        else:
            quiet += WINDOW
            if quiet >= MIN_GAP:
                break
    return max(0.0, start * WINDOW - PAD), end * WINDOW + PAD


def encode(src, dest, start, end):
    subprocess.run(
        ["ffmpeg", "-v", "error", "-y", "-i", str(src), "-map", "0:a",
         "-ss", f"{start:.2f}", "-to", f"{end:.2f}",
         "-af", "loudnorm=I=-18:TP=-2:LRA=11",
         "-ar", "44100", "-ac", "1", "-b:a", "96k", str(dest)],
        check=True)


def duration(path):
    return float(subprocess.run(
        ["ffprobe", "-v", "error", "-show_entries", "format=duration",
         "-of", "csv=p=0", str(path)],
        capture_output=True, text=True, check=True).stdout.strip())


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--dry-run", action="store_true",
                    help="report what would be fetched, touch nothing")
    ap.add_argument("--only", help="comma-separated zikr ids")
    args = ap.parse_args()

    entries = json.loads(ZIKR_JSON.read_text(encoding="utf-8"))
    wanted = {int(x) for x in args.only.split(",")} if args.only else None

    AUDIO_DIR.mkdir(parents=True, exist_ok=True)
    work = AUDIO_DIR / ".raw"
    work.mkdir(exist_ok=True)

    changed, skipped, failed = 0, 0, 0
    for entry in entries:
        zid, source = entry["id"], entry.get("source")
        if wanted and zid not in wanted:
            continue
        if not source or not source.startswith("hisn:"):
            print(f"  {zid:>3}  skip      no source mapped")
            skipped += 1
            continue
        dua_id = source.split(":", 1)[1]
        if args.dry_run:
            print(f"  {zid:>3}  would fetch hisn:{dua_id}")
            continue

        raw, out = work / f"{dua_id}.mp3", AUDIO_DIR / f"{zid}.mp3"
        try:
            if not raw.exists():
                download(dua_id, raw)
            span = first_utterance(envelope(raw))
            if span is None:
                print(f"  {zid:>3}  FAIL      no speech detected in hisn:{dua_id}")
                failed += 1
                continue
            encode(raw, out, *span)
            secs = round(duration(out), 1)
            before = duration(raw)
            if entry.get("seconds") != secs:
                entry["seconds"] = secs
                changed += 1
            print(f"  {zid:>3}  ok        {before:5.1f}s -> {secs:4.1f}s   {entry['transliteration'][:38]}")
        except (subprocess.CalledProcessError, OSError) as exc:
            print(f"  {zid:>3}  FAIL      {exc}")
            failed += 1

    if not args.dry_run and changed:
        ZIKR_JSON.write_text(
            json.dumps(entries, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        print(f"\nupdated `seconds` on {changed} entr{'y' if changed == 1 else 'ies'}")
    print(f"skipped {skipped}, failed {failed}")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
