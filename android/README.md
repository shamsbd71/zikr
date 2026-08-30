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
| A single self-rescheduling `Timer` | A single self-rescheduling `WorkManager` job |
| UserDefaults | Jetpack DataStore |

Same 21-phrase zikr list (`res/raw/zikr.json`, copied from the other
builds' data — keep in sync by hand), same settings (interval range,
speak-aloud toggle), same bundled-audio-overrides-voice convention... 
except audio isn't bundled here — see Known limitations.

## Why no foreground service, no persistent notification

Phase 1's behavior is "wake briefly, show one notification, optionally
speak one phrase, done" — a short one-shot burst, not sustained
background activity. Android's background restrictions mainly target
*continuous* background work (which is exactly what Phase 2's
usage-polling would need); a `WorkManager` `OneTimeWorkRequest` firing
occasionally doesn't need a foreground service or its mandatory
persistent notification. `WorkManager` also persists its own schedule
and re-arms itself after a device reboot automatically — no custom
`BOOT_COMPLETED` receiver needed.

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
