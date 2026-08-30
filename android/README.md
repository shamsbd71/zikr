# Zikr for Android

Same app as the macOS/Linux/Windows builds, ported natively — Kotlin +
Jetpack Compose, built on Android's own standards. This is **Phase 1**
only: ambient reminders at a random interval, the same spirit as every
other build ("you don't do anything, it just interrupts you"). It does
not detect what app you're in — see the root `CLAUDE.md`/conversation
history for why that's a deliberately separate, harder Phase 2 (Family
Controls on iOS, Accessibility/UsageStats on Android — both real, both
higher permission/store-review risk, deferred for now).

| macOS build | Android build |
|---|---|
| MenuBarExtra | A minimal Settings screen (Compose) — no persistent tray equivalent on Android |
| UNUserNotificationCenter | `NotificationCompat` + a notification channel |
| AVSpeechSynthesizer | `android.speech.tts.TextToSpeech` |
| A single self-rescheduling `Timer` | A single self-rescheduling `AlarmManager.setExactAndAllowWhileIdle` alarm |
| UserDefaults | Jetpack DataStore |

Same 21-phrase zikr list (`res/raw/zikr.json`, copied from the other
builds' data — keep in sync by hand), same settings (interval range,
speak-aloud toggle), same bundled-audio-overrides-voice convention... 
except audio isn't bundled here — see Known limitations.

## Why AlarmManager, not WorkManager

The original build used a `WorkManager` `OneTimeWorkRequest` with
`setInitialDelay`. It looked right — the job scheduled correctly and
showed up in `dumpsys jobscheduler` as expected — but real-device
testing (and a repeatable reproduction on the emulator: schedule a
short interval, background the app, wait) showed the job would sit
"READY" (every constraint satisfied, fully overdue) and simply never
get dispatched once Android froze the app's process in the background.
That's precisely the "no zikr in duration" bug: reminders looked
scheduled but never actually fired.

`AlarmManager.setExactAndAllowWhileIdle` (falling back to
`setAndAllowWhileIdle` if the user hasn't granted the exact-alarm
permission) is the mechanism every real alarm/reminder app uses
instead, specifically because it's allowed to wake a frozen/idle
process — ordinary background jobs aren't. Verified end-to-end on an
emulator: backgrounded the app, waited for the alarm to fire, confirmed
the notification actually posted and the next alarm was rescheduled.

This means `ReminderAlarmReceiver` (not a persistent foreground
service) does the "wake briefly, show one notification, optionally
speak one phrase, done" work — still a short one-shot burst, not
sustained background activity, so no foreground service or its
mandatory persistent notification is needed. Unlike WorkManager, a raw
`AlarmManager` alarm does *not* survive a reboot on its own — a
`BootReceiver` re-arms it on `ACTION_BOOT_COMPLETED`.

### Exact alarm permission

Android 12+ requires the user to explicitly grant "Alarms & reminders"
(`SCHEDULE_EXACT_ALARM`) for exact-timing alarms — it's not a normal
runtime permission dialog, it's a toggle in system Settings. The
Settings screen shows a banner with a button that deep-links straight
to it when not granted. Without it, reminders still fire (via the
inexact fallback) but with looser timing.

## Why no in-place self-updater

Same decision as Linux/Windows, for the same reason, plus Android adds
its own: an app can't silently replace its own APK at all without
either Play Store's own update mechanism or the user manually
re-installing. There's no "Check for Updates" yet — download a new APK
from [releases](https://github.com/shamsbd71/zikr/releases/latest) and
install over the old one when a new version ships.

## Install

Download `Zikr-<version>.apk` from
[releases](https://github.com/shamsbd71/zikr/releases/latest), open
it on your device, and allow "install from unknown sources" if
prompted (this is a debug-signed build — no Play Store listing yet).
On first launch, Android will ask for notification permission — allow
it, or reminders won't show.

## Build it yourself

Requires a JDK 17 and the Android SDK (Android Studio manages both, or
install the SDK command-line tools directly and set `ANDROID_HOME`):

```sh
cd android
./gradlew assembleDebug
```

Produces `app/build/outputs/apk/debug/app-debug.apk`.

## Run the tests

```sh
cd android
./gradlew testDebugUnitTest
```

Unit tests cover the random-interval scheduling math
(`pickDelaySeconds`), the min/max interval clamp logic
(`IntervalClamp`), and the zikr list parser (`ZikrData.parse`, including
a check against the real bundled `zikr.json` for the expected 21-entry
count) — all pure functions, no emulator or Android framework needed.

## Known limitations

- No Arabic TTS voice ships on every device the way it does on
  macOS/Windows — `Speech.kt` checks `TextToSpeech.isLanguageAvailable`
  and falls back to reading the English transliteration if Arabic isn't
  installed, same fallback convention as every other platform. Users
  can install an Arabic TTS language pack via Settings → System →
  Languages → Text-to-speech.
- No bundled-audio-recording override yet (the other builds can play a
  real recording per zikr if you drop one in `Resources/`) — could be
  added later as a raw/asset lookup by id, not implemented in Phase 1.
- minSdk 26 (Android 8.0) — notification channels are required from
  that version on, and it's a reasonable modern floor.
