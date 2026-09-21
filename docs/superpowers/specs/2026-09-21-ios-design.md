# iOS: ambient dhikr reminders without a background process

**Date:** 2026-09-21
**Status:** design approved, not yet implemented (blocked on Xcode)
**Scope:** new `ios/` target. No change to the existing four platforms.

## Problem

Every existing build works the same way: a long-lived process holds a
timer, wakes at a random moment, and speaks. macOS and Linux keep a tray
process alive; Windows keeps an `ApplicationContext`; Android was forced
onto `AlarmManager` precisely because a frozen background process could
not be relied on to fire (`CLAUDE.md`, "WorkManager is not reliable").

iOS offers no version of this. An app that is not foregrounded does not
get to run code at a moment of its choosing. There is no tray, no
`AlarmManager` equivalent, and no way to invoke a speech synthesiser
from the background at a chosen time. `BGAppRefreshTask` exists but is
explicitly opportunistic — the system decides if and when, which is the
opposite of what a reminder needs.

So the desktop architecture cannot be ported. The question is whether
the *product* survives the constraint.

## Constraints

- **The user does nothing.** `CLAUDE.md` rules out anything that asks
  the user to open the app, tap, or track a streak. A design that only
  speaks while the app is open is not Zikr; it is a different product.
- **No background execution.** Whatever fires must be something iOS
  schedules on the app's behalf, ahead of time.
- **Scope stays narrow.** No Adhan, no prayer times, no Qibla.
- **Id 22 stays load-bearing.** `UnlockGreeter.swift` and
  `ZikrData.kt:45` both resolve the unlock greeting by `id == 22`. iOS
  has no unlock hook at all, so this build simply has no unlock
  greeting — but the id must not be repurposed.

## Decision

Pre-schedule `UNNotificationRequest`s at randomised times, each
carrying its zikr's recitation as a `UNNotificationSound`.

The clip is the notification's sound. Nothing needs to run: iOS already
plays a sound when it delivers a notification, and it will play a
bundled file instead of the default tone if asked. The user gets a
dhikr spoken aloud at a random moment without the app running, which is
the whole pitch.

### Why this is viable here specifically

A custom notification sound must be under 30 seconds and in linear PCM
(or MA4/µ-law/a-law) in a `.caf`, `.aif` or `.wav` container. The list
was already measured for the long-zikr opt-in, and the longest adhkar is
**17.8s** — every one of the 49 qualifies. `tools/convert_audio.py
--format caf` produces them and fails the build if any clip ever crosses
30s, because iOS's failure mode is to silently substitute the default
tone.

### Rejected alternatives

- **Speak with `AVSpeechSynthesizer` on a background task.** Needs the
  app running; `BGAppRefreshTask` cannot be pinned to a time.
- **Silent push from a server.** Adds a backend and a network
  dependency to an app that has neither, and iOS throttles silent pushes
  anyway.
- **The `audio` background mode.** Keeping an audio session alive all
  day to speak occasionally is battery-hostile and a plausible App
  Review rejection, since the app is not an audio player.

## The 64-notification ceiling

iOS keeps at most **64** pending local notification requests per app and
silently drops the rest. This is the central design constraint.

- At the default 20–45 minute interval (~38/day at the midpoint), 64
  requests cover roughly **a day and a half**.
- The queue is topped back up whenever the app is foregrounded, and
  opportunistically from a `BGAppRefreshTask`.
- If the user never opens the app and the system never grants a refresh,
  reminders stop after the queue drains.

That last point is a real, honest limitation with no clean fix on iOS,
and it must be stated plainly in the App Store description rather than
discovered. Mitigations that do not violate the "user does nothing"
rule:

- Schedule the full 64 every time the app is opened, so any incidental
  launch refills the queue.
- Prefer wider intervals on iOS than on desktop, since each request
  bought is a slot spent. A 30–90 minute range covers ~2.5 days.
- Make the last scheduled notification, and only that one, a quiet
  prompt to open the app — accepted as the single exception to "never
  ask the user to open it", because the alternative is silence.

## Other iOS-specific losses

- **Silent switch and Focus modes suppress the sound.** The
  notification still arrives; the dhikr is not spoken. Nothing can be
  done about this, and it should be said in the description.
- **No mic-in-use pause.** iOS does not expose whether another app is
  recording, and the notification is scheduled long before the moment
  anyway. Dropped, not deferred.
- **No unlock greeting.** No unlock hook exists.
- **No self-update.** The App Store owns updates.
- **Notification permission is required**, and a denial leaves the app
  with nothing to do. Onboarding has to explain why before asking.

## Data

Unchanged. `data/zikr.json` stays the single list, copied into the
bundle by the build the same way the other four platforms take it. The
CAF clips are generated at build time from `data/audio/*.mp3`; they are
not committed, for the same reason the ogg and wav sets are not — three
committed encodings of the same five minutes is derived data that drifts
(`tools/convert_audio.py` header).

Time-of-day weighting carries over unchanged: the window is known at
scheduling time, so each slot picks from the adhkar appropriate to the
moment it will fire.

## Phase 1 scope

In: notification permission onboarding, the scheduler and its top-up,
the 49-phrase list with time weighting, CAF sounds, an interval setting,
the long-zikr opt-in, and a settings screen.

Out: widgets, Apple Watch, iCloud sync, Live Activities, the
social-media-triggered variant (which needs the Family Controls
entitlement and is its own project).

## Testing

The discipline that caught the Android WorkManager bug applies directly,
because this is the same class of risk — something that looks correctly
scheduled but never fires:

1. Schedule with a deliberately short interval, background the app, and
   confirm a notification actually arrives with the recitation audible,
   on a real device rather than only the Simulator.
2. Confirm the queue refills on foreground, and that
   `getPendingNotificationRequests` never exceeds 64.
3. Let the queue drain past 64 and confirm the documented behaviour is
   what actually happens.
4. Confirm a clip over 30s falls back to the default tone, so the build
   guard is protecting against something real.
5. Check silent-switch and Focus behaviour and write down what happens.

None of this can be done on the current dev machine: it has Command Line
Tools only, no Xcode and no iOS SDK. Installing Xcode is a prerequisite
for writing any of this code, and a real device is a prerequisite for
trusting it.
