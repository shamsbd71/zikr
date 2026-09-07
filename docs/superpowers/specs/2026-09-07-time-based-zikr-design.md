# Time-of-day weighted zikr selection + expanded list

**Date:** 2026-09-07
**Status:** approved, not yet implemented
**Scope:** all four platforms (macOS, Linux, Windows, Android)

## Problem

Zikr picks uniformly at random from 21 phrases. The list was
deliberately restricted to "non time-locked dhikr so a random reminder
is always appropriate" (`ZikrList.swift:3`), which rules out the
morning, evening and sleep adhkar — the ones most tied to a moment in
the day, and the ones a user is most likely to want.

A feature request asks for two things: a larger list, and selection
that reflects the time of day rather than being uniformly random.

## Constraints

- **No prayer times.** `CLAUDE.md` scopes out Adhan/prayer times/Qibla.
  Windows are wall-clock, local time, no location, no network.
- **No new user-facing settings.** The product pitch is "the user does
  nothing." The feature is invisible; it just picks better.
- **Ids 1–22 are load-bearing.** `ZikrData.kt:45` resolves the unlock
  greeting with `.first { it.id == 22 }`, and `UnlockGreeter.swift`
  mirrors it. Renumbering breaks both. New entries start at 23; id 21
  stays retired.
- **Desktop parity.** macOS/Linux/Windows must move together.
  Android is included here too: this is pure selection logic with no
  platform surface, so excluding it would be arbitrary drift rather
  than a Phase 1 boundary.

## Decisions

| Decision | Choice | Rejected |
|---|---|---|
| Selection model | Weighted random | Strict buckets (empty-pool cases); fixed anchor events (two parallel systems) |
| Entry length cap | ~15 words / ~7s | ~8 words (only 6 time-locked entries exist that short — nothing to weight); ~20 words (reads as recitation, not ambience) |
| Settings surface | None; windows hardcoded | One toggle; toggle + editable windows |
| Data layout | One canonical `data/zikr.json` | Codegen the Swift; keep 4 copies + CI drift check |

## Data model

`data/zikr.json` at the repo root becomes the single source of truth.
Each platform's build copies it into place. Two new fields:

```json
{ "id": 1,  "arabic": "...", "transliteration": "SubhanAllah",
  "translation": "Glory be to Allah", "windows": [], "source": "hisn:240" }

{ "id": 23, "arabic": "...", "transliteration": "Bismika Allahumma amutu wa ahya",
  "translation": "In Your name, O Allah, I die and I live",
  "windows": ["night"], "source": "hisn:105" }
```

- `windows`: array of window names. **Empty means anytime** — the
  default, and the majority (24 of 40 entries).
- `source`: provenance of the recitation, `hisn:<dua id>` referring to
  hisnmuslim.com's per-dua catalogue. Doubles as the fetch key for the
  audio pipeline (see the companion spec).

Ids remain unique and stable. `windows` values must be one of the three
names below; anything else fails the schema test.

## Windows

Wall-clock, local time, hardcoded.

| Window | Range | Entries tagged |
|---|---|---|
| `morning` | 04:30–10:00 | 8 |
| `evening` | 15:00–19:00 | 6 |
| `night` | 21:00–02:00 (wraps midnight) | 8 |
| *(anytime)* | always eligible | 24 |

Morning and evening are not disjoint sets of *entries*. Hisn al-Muslim
treats them as one chapter — most of its duas are said at both times
with only أصبحنا/أمسينا swapped — so roughly six entries carry
`["morning","evening"]` and are counted in both rows above. Distinct
entry total is therefore **40**, not 46: 8 morning-or-evening, 8 night,
24 anytime.

A `waking` window was considered and dropped: only two qualifying
entries exist at the length cap and both sit inside `morning`.

Wrap-past-midnight uses the same approach as `isWithinQuietHours`,
which already shipped and is tested on Linux and Windows.

## Selection

One pure function per platform, named per that platform's convention
(`weightFor` / `weight_for` / `WeightFor`):

```
weightFor(zikr, minutesSinceMidnight):
    if zikr.windows is empty:            return 1
    if any window contains the time:     return 3
    return 0
```

Selection is a weighted pick over the cumulative sum, skipping the
previously spoken id.

**Why 0 and not a small epsilon out of window:** a sleep dua at 10:00
is simply wrong, and suppressing it is the point of the feature. The
randomness lives in the anytime pool, which is *always* eligible, so
there is no empty-pool branch to handle.

**Why 3.** With 8 windowed entries against a 24-entry anytime pool, a
weight of `W` gives the windowed set a share of `8W / (8W + 24)`:

| W | 1 | 2 | **3** | 5 |
|---|---|---|---|---|
| windowed share during its window | 25% | 40% | **50%** | 63% |

`W = 3` splits it evenly: in the morning window, half the reminders are
morning adhkar and half are the familiar anytime phrases. That is the
intended feel — the time of day tilts the selection without taking it
over. `W = 5` would make morning adhkar the clear default and push
SubhanAllah and Alhamdulillah out of most mornings.

The constant is a single named value per platform so this can be
re-tuned in one place, and the share arithmetic above is what the tests
pin — not the raw constant.

## Per-platform changes

| Platform | Changes |
|---|---|
| macOS | Delete the hardcoded array in `ZikrList.swift`; add `ZikrLoader.swift` (reads bundled JSON) and `ZikrSelector.swift` (`weightFor`). `build.sh` gains a `cp` beside the existing `Resources/Audio` copy at line 45. |
| Linux | `zikr_data.py`: add `weight_for` and `weighted_random`. `build_deb.sh` copies the canonical file. `_DATA_PATHS` unchanged. |
| Windows | `ZikrData.cs`: add `WeightFor` and `WeightedRandom`. `Zikr.csproj` copies the canonical file. |
| Android | `ZikrData.kt`: add `weightFor`; `random()` becomes weighted. `bismillah()` untouched. |

macOS gains a load-failure path it does not have today (the array is
currently a compile-time constant). Mitigation: `ZikrLoader` treats a
missing or unparseable bundle resource as a fatal programming error at
startup rather than degrading silently, matching Linux's existing
`FileNotFoundError` behaviour in `zikr_data.py`.

## List expansion

From 21 to 40 entries, all drawn from the Hisn al-Muslim per-dua
catalogue so that every entry has a matching recitation available.

Census of that catalogue at a 15-word cap (267 duas total, all with
per-dua audio): 8 morning/evening, 8 sleep, 2 waking, 115 anytime
candidates. The ~24 anytime slots are curated down from those 115.

Text sourcing:

- **Arabic** — `hisnmuslim.com/api/ar/<chapter>.json`, field `ARABIC_TEXT`.
- **Translation** — `hisnmuslim.com/api/en/<chapter>.json`, field
  `TRANSLATED_TEXT`, keyed by the *same* dua id. Needs editing: the
  supplied translations are verbose and parenthetical, whereas the
  existing list is terse ("Glory be to Allah").
- **Transliteration** — not provided by either endpoint. Hand-authored
  for the 21 new entries, following the existing list's conventions.

## Testing

`weightFor` is pure specifically so it runs without a native toolchain.

| Platform | Suite |
|---|---|
| Linux | `linux/tests/test_zikr.py` |
| Windows | `windows/Zikr.Tests/` |
| Android | `android/app/src/test/.../ZikrDataTest.kt` |
| macOS | none, per existing convention |

Cases:

1. Anytime entry returns 1 at every hour tested.
2. Windowed entry returns 5 inside its window, 0 outside.
3. Wrapping window (`night`, 21:00–02:00) returns 5 at 23:00 and 01:00,
   0 at 12:00.
4. Boundary minutes: inclusive start, exclusive end.
5. Schema: every id unique, every `windows` value a known name, every
   entry within the length cap, id 22 present.
6. CI drift check: all four bundled copies byte-identical to
   `data/zikr.json`.

## Out of scope

- Recorded qari audio — companion spec, separate PR. Includes replacing
  Windows' WAV-only `SoundPlayer` with a `winmm.dll` MCI P/Invoke so
  Windows can ship the same MP3 assets as the other platforms (~6MB
  rather than ~50MB of WAV).
- Any user-configurable window or on/off toggle.
- Long adhkar (Ayat al-Kursi, Sayyidul Istighfar). Revisit only once
  recorded audio exists and there is a reason to opt into them.
