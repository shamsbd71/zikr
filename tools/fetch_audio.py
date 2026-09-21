#!/usr/bin/env python3
"""Fetch and prepare per-phrase recitation clips for the zikr list.

Source is dua.gtaf.org's copy of the Hisn al-Muslim recitation, served
from static.gtaf.org as one MP3 per dua. It is the same recitation the
book's own site publishes, but its copies are already trimmed of the
narrator reading the chapter title, which is the whole reason this
pipeline moved to it: the previous source opened many files with a
spoken title, and every attempt to locate the dhikr inside that by
matching against a synthesised reference shipped audibly wrong clips
twice. Here the file simply *is* the dua, so for the large majority of
the list there is nothing to locate and nothing to guess.

The site also publishes each dua split into segments, so where one file
does hold several phrases, the split is a known fact about the data
rather than something inferred from the waveform.

`source` in data/zikr.json says how to get the clip:

    gtaf:<dua>              the whole file - trim silence, keep it all
    gtaf:<dua>#<n>          the n-th spoken burst
    gtaf:<dua>#<n>@<gap>    ... splitting on <gap> seconds of quiet,
                            for a dua recited as several repetitions

Every clip is then checked against a duration synthesised from the
shipped Arabic, and a clip that is not a credible length for its own
text is rejected rather than written. A wrong clip is worse than none,
because TTS already covers a missing one.

Usage:  python3 tools/fetch_audio.py [--dry-run] [--only 1,3,7] [--check]
"""
import argparse
import array
import json
import math
import re
import subprocess
import sys
import tempfile
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ZIKR_JSON = ROOT / "data" / "zikr.json"
AUDIO_DIR = ROOT / "data" / "audio"
UA = {"User-Agent": "zikr-audio-fetch/2.0 (+https://github.com/abu/zikr)"}

ENVELOPE_RATE = 8000      # Hz, mono - plenty to locate speech
WINDOW = 0.05             # 50ms envelope resolution
GATE_BELOW_PEAK = 22      # dB below peak counts as speech
MIN_GAP = 0.45            # s of quiet that separates utterances
PAD = 0.18                # s kept either side
TOLERANCE = (0.6, 2.2)    # credible speech/expected duration ratio
SLACK = 1.0               # s of absolute leeway, which is what saves the
                          # shortest phrases: a reciter enunciating two
                          # words takes about twice as long as the
                          # synthesiser does, so their ratio is extreme
                          # while the gap is still under a second.

SOURCE_RE = re.compile(r"^gtaf:(\d+)(?:#(\d+)(?:@([\d.]+))?)?$")


def audio_number(dua):
    """The CDN's file number for a dua, as dua.gtaf.org's player derives it.

    Mirrors the mapping in the site's own player: a handful of duas share
    one recording, and the numbering is offset by one below 250. Kept
    verbatim rather than tidied, so it can be diffed against the source.
    """
    if not dua or dua in (341, 352):
        return None
    if dua in (67, 74, 96):
        return 66
    if dua in (83, 106):
        return 82
    return dua if dua >= 250 else dua - 1


def download(dua, dest):
    n = audio_number(dua)
    if n is None:
        raise ValueError(f"dua {dua} has no recording")
    url = f"https://static.gtaf.org/files/v1/audio/hisnul-muslim/n{n}.mp3"
    req = urllib.request.Request(url, headers=UA)
    dest.write_bytes(urllib.request.urlopen(req, timeout=60).read())


def envelope(path):
    """Per-window RMS in dBFS."""
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


def bursts(env, min_gap=MIN_GAP):
    """Every contiguous speech burst as (start, end) seconds."""
    if not env:
        return []
    gate = max(env) - GATE_BELOW_PEAK
    loud = [level > gate for level in env]
    found, i = [], 0
    while i < len(loud):
        if not loud[i]:
            i += 1
            continue
        start = last = i
        quiet = 0.0
        while i < len(loud):
            if loud[i]:
                last, quiet = i, 0.0
            else:
                quiet += WINDOW
                if quiet >= min_gap:
                    break
            i += 1
        found.append((start * WINDOW, last * WINDOW))
    return found


def expected_seconds(arabic):
    """How long the phrase should take, per macOS's Arabic voice.

    Only ever a sanity check on the finished clip - never used to decide
    what to cut, which is what made the previous pipeline fragile.
    """
    try:
        with tempfile.NamedTemporaryFile(suffix=".aiff", delete=False) as tmp:
            ref = Path(tmp.name)
        try:
            subprocess.run(["say", "-v", "Majed", "-o", str(ref), arabic],
                           capture_output=True, check=True)
            return duration(ref)
        finally:
            ref.unlink(missing_ok=True)
    except (subprocess.CalledProcessError, OSError, ValueError):
        return None


def credible(secs, expect):
    """Is a finished clip a believable length for its own text?

    Measures the speech, not the silence: `secs` includes the padding
    this script adds on both sides, which is a rounding error on a long
    dua and most of the clip on a two-word one.

    Returns (ok, speech_seconds, ratio).
    """
    speech = max(0.0, secs - 2 * PAD)
    if not expect:
        return True, speech, 0.0
    ratio = speech / expect
    ok = TOLERANCE[0] <= ratio <= TOLERANCE[1] or abs(speech - expect) <= SLACK
    return ok, speech, ratio


def span_for(entry, raw):
    """The (start, end) of this entry's phrase inside its downloaded file."""
    m = SOURCE_RE.match(entry["source"])
    if not m:
        raise ValueError(f"unparseable source {entry['source']!r}")
    _, nth, gap = m.group(1), m.group(2), m.group(3)
    gap = float(gap) if gap else MIN_GAP
    found = bursts(envelope(raw), gap)
    if not found:
        raise ValueError("no speech found in the recording")
    if nth is None:
        # The file is the dua. Keep everything from the first word to the
        # last and drop only the silence around it.
        start, end = found[0][0], found[-1][1]
    else:
        i = int(nth) - 1
        if i >= len(found):
            raise ValueError(f"wanted burst {nth} but found {len(found)} at gap {gap}")
        start, end = found[i]
    return max(0.0, start - PAD), end + PAD


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
    ap.add_argument("--check", action="store_true",
                    help="re-measure the clips already on disk, fetch nothing")
    args = ap.parse_args()

    entries = json.loads(ZIKR_JSON.read_text(encoding="utf-8"))
    wanted = {int(x) for x in args.only.split(",")} if args.only else None

    AUDIO_DIR.mkdir(parents=True, exist_ok=True)
    # Namespaced by source: dua numbering differs between publishers, and a
    # flat cache would happily hand back the previous source's file for the
    # same number.
    work = AUDIO_DIR / ".raw" / "gtaf"
    work.mkdir(parents=True, exist_ok=True)

    changed, skipped, failed = 0, 0, 0
    for entry in entries:
        zid, source = entry["id"], entry.get("source")
        if wanted and zid not in wanted:
            continue
        if not source:
            print(f"  {zid:>3}  skip      no source mapped")
            skipped += 1
            continue
        m = SOURCE_RE.match(source)
        if not m:
            print(f"  {zid:>3}  FAIL      unparseable source {source!r}")
            failed += 1
            continue
        dua = int(m.group(1))
        out = AUDIO_DIR / f"{zid}.mp3"

        if args.check:
            if not out.exists():
                print(f"  {zid:>3}  missing   {source}")
                failed += 1
                continue
            secs, expect = duration(out), expected_seconds(entry["arabic"])
            ok, speech, ratio = credible(secs, expect)
            print(f"  {zid:>3}  {'ok ' if ok else 'ODD'}       {secs:4.1f}s  "
                  f"speech {speech:4.1f}s  expect ~{expect:4.1f}s  x{ratio:4.2f}  "
                  f"{entry['transliteration'][:32]}")
            if not ok:
                failed += 1
            continue

        if args.dry_run:
            print(f"  {zid:>3}  would fetch {source} -> n{audio_number(dua)}.mp3")
            continue

        raw = work / f"{dua}.mp3"
        try:
            if not raw.exists():
                download(dua, raw)
            start, end = span_for(entry, raw)
            encode(raw, out, start, end)
            secs = round(duration(out), 1)

            # A clip that is not a credible length for its own text means
            # the cut landed on the wrong thing. Drop it and let TTS speak.
            expect = expected_seconds(entry["arabic"])
            ok, speech, ratio = credible(secs, expect)
            if not ok:
                out.unlink(missing_ok=True)
                print(f"  {zid:>3}  REJECT    speech {speech:4.1f}s vs expected "
                      f"~{expect:4.1f}s (x{ratio:.2f})  {entry['transliteration'][:32]}")
                failed += 1
                continue
            if entry.get("seconds") != secs:
                entry["seconds"] = secs
                changed += 1
            print(f"  {zid:>3}  ok        {duration(raw):5.1f}s -> {secs:4.1f}s   "
                  f"{entry['transliteration'][:38]}")
        except (subprocess.CalledProcessError, OSError, ValueError) as exc:
            print(f"  {zid:>3}  FAIL      {exc}")
            failed += 1

    if not args.dry_run and not args.check and changed:
        ZIKR_JSON.write_text(
            json.dumps(entries, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        print(f"\nupdated `seconds` on {changed} entr{'y' if changed == 1 else 'ies'}")
    print(f"skipped {skipped}, failed {failed}")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
