# Time-of-day weighted zikr selection, expanded list, long-zikr opt-in

**Date:** 2026-09-07
**Status:** approved, not yet implemented
**Scope:** all four platforms (macOS, Linux, Windows, Android)

## Problem

Zikr picks uniformly at random from 21 phrases. The list was
deliberately restricted to "non time-locked dhikr so a random reminder
is always appropriate" (`ZikrList.swift:3`), which rules out the
morning, evening and sleep adhkar — the ones most tied to a moment in
the day.

Three requests, addressed together because they touch the same data:

1. A larger list, with selection that reflects the time of day.
2. Adhkar longer than 5 seconds must be opt-in, not sprung on the user.
3. Show a "what's new" modal on first launch after an update.

Item 3 is independent of the data model and ships as its own PR; it is
specified in the last section.

## Constraints

- **No prayer times.** `CLAUDE.md` scopes out Adhan/prayer times/Qibla.
  Windows are wall-clock, local time, no location, no network.
- **No settings for the time windows.** The pitch is "the user does
  nothing"; windows are hardcoded. The *length* opt-in is the one new
  setting, and it exists because unexpected long audio is intrusive.
- **Ids 1–22 are load-bearing.** `ZikrData.kt:45` resolves the unlock
  greeting with `.first { it.id == 22 }`, and `UnlockGreeter.swift`
  mirrors it. New entries start at 23; id 21 stays retired.
- **Desktop parity.** macOS/Linux/Windows move together. Android is
  included in all of this — see "Android scope" below.

## Decisions

| Decision | Choice | Rejected |
|---|---|---|
| Selection model | Weighted random | Strict buckets (empty-pool cases); fixed anchor events (two parallel systems) |
| Long-zikr boundary | 5s, opt-in beyond | 10s (kept time-weighting alive by default, but overrides the stated 5s line); conditional per-pool caps (two rules, unnameable in one checkbox) |
| Time windows | Hardcoded, no setting | Toggle; toggle + editable times |
| Data layout | One canonical `data/zikr.json` | Codegen the Swift; keep 4 copies + CI drift check |
| Audio source | Hamad Al-Duraihim, via hisnmuslim's per-dua catalogue | Slicing Afasy/Fares Abbad/Abu Ziyad out of whole-book tracks (a duration match in a 40-minute file is far weaker than in a 7-second one); commissioning a reciter; shipping TTS only |
| Settings surface | Exactly one checkbox: longer adhkar | Any time-window setting at all |

### Accepted consequence of the 5s boundary

Only 3 adhkar in the whole Hisn al-Muslim catalogue are both
time-locked and ≤5s (1 morning/evening, 2 sleep). So with the setting
off — the default — **time-of-day weighting has almost nothing to act
on and is effectively inert.** It becomes meaningful when the user
enables longer adhkar, which is also when the morning/evening/sleep
sets become available.

This was raised and accepted deliberately. It shapes two things:

- The setting is named for what it *unlocks*, not what it permits, so
  the gain is legible: **"Include longer adhkar (morning, evening and
  night remembrances)"**, identical wording on all four platforms.
- Time-weighting is still built now. It costs one pure function, and it
  is the difference between the long mode being a coherent feature and
  being an undifferentiated dump of 213 extra entries.

Also accepted: existing id 9 measures 6.2s and therefore moves behind
the setting. That is a small regression for current users and goes in
the changelog.

## Data model

`data/zikr.json` at the repo root is the single source of truth; each
platform's build copies it into place. Three new fields:

```json
{ "id": 1,  "arabic": "...", "transliteration": "SubhanAllah",
  "translation": "Glory be to Allah",
  "windows": [], "seconds": 2.9, "source": "hisn:240" }

{ "id": 23, "arabic": "...", "transliteration": "Bismika Allahumma amutu wa ahya",
  "translation": "In Your name, O Allah, I die and I live",
  "windows": ["night"], "seconds": 7.4, "source": "hisn:105" }
```

- `windows` — array of window names; **empty means anytime**.
- `seconds` — **measured** duration of the trimmed recitation, not an
  estimate. Populated by running the audio pipeline while building the
  list, so the value is real from day one even though the clips
  themselves ship in a later PR. Also serves as the TTS length estimate.
- `source` — `hisn:<dua id>`, provenance and the pipeline's fetch key.

## Windows

Wall-clock, local time, hardcoded.

| Window | Range |
|---|---|
| `morning` | 04:30–10:00 |
| `evening` | 15:00–19:00 |
| `night` | 21:00–02:00 (wraps midnight) |

Morning and evening are not disjoint sets of entries — Hisn al-Muslim
treats them as one chapter whose duas are mostly said at both times
with only أصبحنا/أمسينا swapped — so many entries carry
`["morning","evening"]`. A `waking` window was dropped: too few
qualifying entries, and all sit inside `morning`.

Wrap-past-midnight reuses the approach proven by `isWithinQuietHours`.

## Selection

```
eligible(zikr, allowLong):
    return allowLong or zikr.seconds <= 5.0

weightFor(zikr, minutesSinceMidnight):
    if zikr.windows is empty:            return 1
    if any window contains the time:     return 3
    return 0
```

Selection filters by `eligible`, then does a weighted pick over the
cumulative sum, skipping the previously spoken id.

**Why 0 out of window:** a sleep dua at 10:00 is wrong, and suppressing
it is the point. The randomness lives in the anytime pool, which is
always eligible and never empty, so there is no empty-pool branch.

**Why 3:** with the setting on, roughly 8 windowed entries face a
~30-entry anytime pool. Weight `W` gives the windowed set a share of
`8W / (8W + 30)` — W=1 gives 21%, W=3 gives 44%, W=5 gives 57%. W=3
tilts selection toward the hour without taking it over. The constant is
a single named value per platform, and the tests pin the resulting
*share*, not the raw constant.

## List composition

**Built: 52 entries, 40 with a verified recitation.** The remaining 12
fall back to TTS, which is the existing shipping behaviour on all four
platforms.

| Tier | Count |
|---|---|
| Default (`seconds` ≤ 5) | 30 |
| Opt-in (`seconds` > 5) | 22 |

| Window | Tagged | Of which in the default pool |
|---|---|---|
| `morning` | 10 | 2 |
| `evening` | 7 | 3 |
| `night` | 6 | 2 |
| *(anytime)* | 35 | 25 |

The 12 TTS-only entries are short fragments that Hisn al-Muslim never
recites standalone - they occur only inside longer duas. Extracting
them by duration match is possible but unsafe on its own: the matcher
compares length, not words, so a span of the right length inside a
multi-phrase dua may be the wrong phrase entirely. Any such extraction
must be listened to before it ships.

Excluded entirely: anything over ~25s (Ayat al-Kursi, the 3 Quls,
Sayyidul Istighfar). Even behind the setting, a 45-second recitation is
a different product from an ambient reminder. Revisit separately.

Text sourcing:

- **Arabic** — `hisnmuslim.com/api/ar/<chapter>.json`, `ARABIC_TEXT`.
- **Translation** — `hisnmuslim.com/api/en/<chapter>.json`,
  `TRANSLATED_TEXT`, keyed by the same dua id. Needs editing: supplied
  translations are verbose and parenthetical; the existing list is terse.
- **Transliteration** — not provided by either endpoint. Hand-authored.

## Per-platform changes

| Platform | Changes |
|---|---|
| macOS | Delete the hardcoded array in `ZikrList.swift`; add `ZikrLoader.swift` (reads bundled JSON) and `ZikrSelector.swift`. `AppSettings.swift` gains `allowLongZikr`; `SettingsView.swift` gains the checkbox. `build.sh` gains a `cp` beside the existing `Resources/Audio` copy at line 45. |
| Linux | `zikr_data.py`: `weight_for`, `eligible`, `weighted_random`. `settings.py` gains `allow_long_zikr`; `ui/settings_window.py` gains the checkbox. `build_deb.sh` copies the canonical file. |
| Windows | `ZikrData.cs`: `WeightFor`, `Eligible`, `WeightedRandom`. `Settings.cs` gains `AllowLongZikr`; `SettingsForm.cs` gains the checkbox. `Zikr.csproj` copies the canonical file. |
| Android | `ZikrData.kt`: `weightFor`, `eligible`; `random()` becomes weighted. `Settings.kt` + `SettingsScreen.kt` gain the toggle. `bismillah()` untouched. |

macOS gains a load-failure path it does not have today. `ZikrLoader`
treats a missing or unparseable bundle resource as a fatal startup
error rather than degrading silently, matching Linux's existing
`FileNotFoundError` in `zikr_data.py`.

## Testing

`weightFor` and `eligible` are pure so they run without a native
toolchain: `linux/tests/test_zikr.py`, `windows/Zikr.Tests/`,
`android/app/src/test/.../ZikrDataTest.kt`. macOS has no suite, per
existing convention.

1. Anytime entry returns weight 1 at every hour tested.
2. Windowed entry returns 3 inside its window, 0 outside.
3. Wrapping window (`night`) returns 3 at 23:00 and 01:00, 0 at 12:00.
4. Boundary minutes: inclusive start, exclusive end.
5. `eligible` excludes `seconds > 5` when the setting is off, includes
   it when on; a `seconds == 5.0` entry is included in both.
6. With the setting off, the eligible pool is non-empty and contains
   the unlock-greeting id 22.
7. Schema: ids unique, `windows` values known, `seconds` present and
   positive on every entry, id 22 present.
8. CI drift check: all four bundled copies byte-identical to
   `data/zikr.json`.

## What's new on update (separate PR)

`WhatsNewController.swift` / `WhatsNewForm.cs` /
`update_dialog.show_whats_new` already exist but are **menu-triggered
only** (`MenuContentView.swift:25`, `TrayApp.cs:88`, `app.py:77`).
There is no persisted last-seen version anywhere in the repo.

- Add a `lastSeenVersion` setting on each platform, written after the
  modal is shown.
- At startup, if `lastSeenVersion` differs from the running version,
  present the existing modal once. On a **fresh install** — no stored
  value — record the current version and show nothing; the modal is for
  people who updated, not first-timers.
- Android has no changelog viewer at all and gets one built: changelog
  fetch, a Compose dialog, and version tracking.

### Android scope note

This expands Android past the Phase 1 boundary that `CLAUDE.md`
documents ("no update dialog/changelog viewer"). That was a deliberate
call, not drift. **`CLAUDE.md` must be updated in the same PR** so a
later session does not read the new code as an accident and revert it.
Android also gains weighted selection and the long-zikr setting here,
for the same reason: both are portable logic with no platform-specific
risk.

## PR sequence

1. **Data + selection** — canonical `data/zikr.json`, ~55 entries,
   `weightFor`/`eligible`, the long-zikr setting, tests, CI drift check.
2. **Audio** — `tools/fetch_audio.py` (fetch, first-burst trim,
   loudness-normalise, per-platform encode), the clips, attribution in
   `README.md` and the site footer, and replacing Windows' WAV-only
   `SoundPlayer` with a `winmm.dll` MCI P/Invoke so it can ship the
   same MP3s as everyone else.
3. **What's new on update** — including Android's changelog viewer and
   the `CLAUDE.md` scope update.

## Audio pipeline notes (validated, not yet implemented)

Verified live against hisnmuslim.com on 2026-09-07:

- All 267 duas have per-dua MP3s at `/audio/ar/<id>.mp3`. One reciter
  throughout: حمد الدريهم (Hamad Al-Duraihim).
- Files carry an embedded cover-art MJPEG stream; ffmpeg filters must
  target `-map 0:a` or they silently do nothing.
- **Clips contain the phrase recited 2–4 times**, not once. Silence
  trimming is insufficient; the pipeline detects the first contiguous
  speech burst via a 50ms RMS envelope gated 22dB below peak, breaking
  on the first gap ≥0.45s, with 0.18s padding.
- Measured on 9 clips: 6.9s→2.9s (SubhanAllah), 10.2s→1.7s (Subhana
  Rabbiyal Azeem), 8.7s→2.9s (Bismillah). Correctly keeps genuinely
  long phrases: id 9, 7.0s→6.2s.
- Output at mono 44.1kHz 96kbps ≈ 34KB/clip, so ~55 clips ≈ 1.9MB —
  well under the 6MB first estimated.
