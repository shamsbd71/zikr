#!/usr/bin/env python3
"""Derive each platform's audio format from the canonical mp3 clips.

data/audio/<id>.mp3 is the one source of truth, produced by
fetch_audio.py. It is not what every platform can play, though, so the
derived formats are generated at package time rather than committed:

    macOS    mp3   AVAudioPlayer plays it directly - no conversion
    Android  mp3   MediaPlayer plays it directly - no conversion
    Linux    ogg   paplay/aplay go through libsndfile, which handles
                   Vorbis but not mp3; ffplay would, but it is only
                   present if ffmpeg is installed, so ogg is the format
                   that works on a plain desktop. speech.py already
                   prefers .ogg over .mp3 when both exist.
    Windows  wav   System.Media.SoundPlayer is PCM-only. 22.05kHz mono
                   keeps the installer near 13MB instead of 27MB, which
                   is inaudible for speech at this length.
    iOS      caf   A clip is delivered as a notification's sound, which
                   iOS requires to be linear PCM (or MA4/u-law/a-law) in
                   .caf/.aif/.wav and under 30 seconds. The longest
                   adhkar here is 17.8s, so the whole list qualifies.

Committing three copies of the same five minutes of audio would put
~20MB of derived data in the repo and invite the formats drifting apart,
which is the same trap the four hand-synced zikr.json copies fell into.

Usage:  python3 tools/convert_audio.py --format ogg|wav --out DIR
"""
import argparse
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
AUDIO_DIR = ROOT / "data" / "audio"

# ffmpeg arguments per target format.
ENCODERS = {
    "ogg": ["-c:a", "libvorbis", "-q:a", "3", "-ar", "44100", "-ac", "1"],
    "wav": ["-c:a", "pcm_s16le", "-ar", "22050", "-ac", "1"],
    "caf": ["-c:a", "pcm_s16le", "-ar", "22050", "-ac", "1"],
}

# iOS refuses a notification sound longer than this and falls back to the
# default tone, silently - so it is worth failing the build over.
IOS_SOUND_LIMIT_SECONDS = 30.0


def duration(path):
    return float(subprocess.run(
        ["ffprobe", "-v", "error", "-show_entries", "format=duration",
         "-of", "csv=p=0", str(path)],
        capture_output=True, text=True, check=True).stdout.strip())


def convert(src, dest, fmt):
    subprocess.run(
        ["ffmpeg", "-v", "error", "-y", "-i", str(src), "-map", "0:a"]
        + ENCODERS[fmt] + [str(dest)],
        check=True)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--format", required=True, choices=sorted(ENCODERS))
    ap.add_argument("--out", required=True, help="directory to write into")
    args = ap.parse_args()

    sources = sorted(AUDIO_DIR.glob("*.mp3"), key=lambda p: int(p.stem))
    if not sources:
        print(f"no clips in {AUDIO_DIR}", file=sys.stderr)
        return 1

    out = Path(args.out)
    out.mkdir(parents=True, exist_ok=True)

    done = 0
    for src in sources:
        dest = out / f"{src.stem}.{args.format}"
        try:
            convert(src, dest, args.format)
        except subprocess.CalledProcessError as exc:
            print(f"  {src.stem}: FAILED {exc}", file=sys.stderr)
            return 1
        if args.format == "caf":
            secs = duration(dest)
            if secs > IOS_SOUND_LIMIT_SECONDS:
                print(f"  {src.stem}: {secs:.1f}s exceeds the {IOS_SOUND_LIMIT_SECONDS:.0f}s "
                      "iOS notification-sound limit", file=sys.stderr)
                return 1
        done += 1

    total = sum(p.stat().st_size for p in out.glob(f"*.{args.format}"))
    print(f"converted {done} clips to {args.format} in {out} ({total / 1e6:.1f} MB)")
    # A partial set is worse than an obvious failure: the app would play
    # some adhkar and silently speak the rest with TTS.
    if done != len(sources):
        print(f"expected {len(sources)}, wrote {done}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
