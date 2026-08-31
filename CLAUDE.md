# Zikr — instructions for Claude

## What this is

A tray/menu-bar app that speaks a random dhikr (Islamic remembrance
phrase — SubhanAllah, Alhamdulillah, Allahu Akbar, etc.) aloud at
random intervals throughout the day. The entire product philosophy is
**the user does nothing**: no taps, no counting, no habit-tracker
streak. It just speaks, on its own schedule, and gets out of the way.
Don't add features that require the user to open the app or interact
with it — that would contradict the core pitch (see the site's "what
most reminder apps ask of you" vs. "what Zikr asks of you" panel).

Native builds for four platforms, each a separate, deliberately
non-shared implementation (not a port via a cross-platform toolkit):

| | macOS | Linux | Windows | Android |
|---|---|---|---|---|
| Language/UI | Swift/SwiftUI+AppKit | Python 3 + GTK3 | C#/.NET Framework 4.8 + WinForms | Kotlin/Jetpack Compose |
| Path | `Sources/ZikrReminder/` | `linux/zikr/` | `windows/Zikr/` | `android/app/` |
| Tests | none (see below) | `linux/tests/` | `windows/Zikr.Tests/` | `android/app/src/test/` |

**Why separate implementations, not a shared cross-platform layer**:
every subsystem (tray icon, notifications, TTS, autostart, config
storage, mic-in-use detection, update checking) is built on that
platform's actual native API, chosen deliberately each time rather than
reaching for an abstraction library. See `linux/README.md`,
`windows/README.md`, and `android/README.md` for the full mapping
table and rationale per subsystem.

**Cross-platform parity is a hard expectation among the three desktop
builds (macOS/Linux/Windows).** A feature added to one should be added
to all three unless there's a documented, deliberate reason not to
(e.g. only macOS self-installs updates in place — Linux and Windows
intentionally don't, because installs vary too much across
distros/package managers to safely overwrite). When in doubt, mirror
the architecture: same setting key (translated to each platform's
naming convention — `camelCase` on macOS, `snake_case` on Linux/JSON,
`PascalCase` on Windows), same default value, same menu item wording,
same UI section placement.

**Android is intentionally behind the desktop trio in scope** — it's
Phase 1 of a deliberately phased mobile plan (see conversation history
or ask the user for the plan doc if one gets written): ambient random
reminders only, no mic-pause, no update dialog/changelog viewer, no
launch-at-login equivalent. Don't treat Android's smaller feature set
as drift to "fix" — check whether it's an intentional Phase 1 gap
before porting a desktop feature over. A "social-media-triggered
reminder" variant (Phase 2) was scoped but deliberately deferred — it
needs OS-specific, higher-risk mechanisms (iOS Family Controls
entitlement, Android Accessibility/UsageStats) on both a future iOS
build and an Android update, not yet started either one.

## Repo layout

```
Sources/ZikrReminder/     macOS app (Swift Package Manager, no Xcode project)
linux/zikr/                Linux app (Python/GTK3)
windows/Zikr/               Windows app (C#/.NET Framework 4.8/WinForms)
windows/Zikr.Tests/          Windows MSTest suite
android/app/                 Android app (Kotlin/Jetpack Compose, Phase 1)
android/app/src/test/        Android JUnit suite (pure logic only, no emulator)
docs/                        GitHub Pages site (docs/index.html - trilingual EN/BN/AR)
.github/workflows/          test.yml (CI, all 4 platforms) + release.yml (tag-triggered)
CHANGELOG.md                 Keep-a-Changelog-style, one entry per release
DESIGN.md                    Site design system reference - read before touching docs/index.html
```

macOS has no Xcode project — build via `./build.sh` (Swift Package
Manager, universal arm64+x86_64 binary via `lipo`, since xcbuild-based
multi-arch builds need full Xcode which isn't installed on the dev
machine).

## Release process

1. Land changes via a feature branch + PR (not direct pushes to
   `main`) — CI (`test.yml`, all platforms) must be green before
   merging.
2. Add an entry to `CHANGELOG.md` under `## [Unreleased]` as part of
   the PR (or right after merging, before tagging) — what changed and
   why, one bullet list per category (Added/Fixed/Changed).
3. On merge, rename that `[Unreleased]` section to `## [X.Y.Z] — date`
   (date format `YYYY-MM-DD`, Asia/Dhaka), bump the version footer in
   `docs/index.html` (three languages — search for the previous
   version string), commit directly to `main`.
4. Tag `vX.Y.Z` and push the tag — `release.yml` re-runs the full test
   gate, then builds and publishes all platform installers (macOS zip,
   Linux .deb, Windows installer, Android APK) as one GitHub Release.
5. Verify the release actually built (`gh run watch <run-id>`) — don't
   assume success from the tag push alone.

Version bump convention: patch for fixes, minor for a new
feature/setting, same as this project has followed so far (no major
version bump has happened yet - still pre-1.0-in-spirit despite the
`v1.x` tags).

## Testing philosophy

**Never trust "should work."** This dev machine is macOS-only with no
`dotnet` or `gi`/GTK available, so Linux/Windows changes cannot be
compiled or run locally — always verify Linux/Windows changes via the
real CI runner (`gh run watch`) before considering them done, and read
the actual CI logs rather than assuming a green checkmark means what
you think it means.

**Android is the exception — a full local toolchain exists on this
machine and should be used, not skipped.** Android Studio is installed
at `/Applications/Android Studio.app`, which bundles a real JDK
(`/Applications/Android Studio.app/Contents/jbr/Contents/Home`) and
there's a full Android SDK at `~/Library/Android/sdk` including a
working AVD (`Medium_Phone_API_36.1`, arm64, hardware-accelerated on
this Apple Silicon Mac). This means Android changes can be genuinely
built (`JAVA_HOME=".../jbr/Contents/Home" ./gradlew assembleDebug`),
installed on a booted emulator, and exercised end-to-end with `adb` —
not just syntax-reviewed and shipped on faith. This was the difference
that caught a real bug: the original WorkManager-based reminder
scheduler looked correct in every static review and even in
`dumpsys jobscheduler` output, but only revealed itself as broken (job
scheduled and overdue, never dispatched, once the process was frozen in
the background) by actually backgrounding the app on the emulator and
waiting. Boot it headless for testing:
```
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"
emulator -avd Medium_Phone_API_36.1 -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect &
adb wait-for-device
```
Then `adb install -r <apk>`, `adb shell input tap/text/keyevent` to
drive the UI, `adb exec-out screencap -p > file.png` to see it (`adb
shell screencap` writing to `/sdcard` directly can fail with a
permission error — pipe through `exec-out` instead), and `adb logcat`
/`adb shell dumpsys <service>` to verify what actually happened rather
than what should have happened. A stray "Process system isn't
responding" / "System UI isn't responding" dialog right after a cold
boot is emulator flakiness under software rendering, not an app bug —
wait it out rather than debugging your own code for it.

For Windows/Linux, or when something can be verified locally without
the full toolchain (macOS build, Linux/Android pure-logic functions run
directly via `python3`/manually reasoned through, C#/Kotlin syntax and
XML well-formedness by careful manual review), do that first, but don't
skip CI verification just because the parts you *could* check passed.

Pure logic (interval math, settings clamping, version comparison,
changelog parsing, registry-ledger parsing, zikr-list JSON parsing) is
factored into small testable functions on all platforms specifically so
it doesn't require the full native toolchain to exercise — prefer this
pattern for new logic over embedding it directly in UI/platform-glue
code.

## Known gotchas learned the hard way

- **macOS universal binary**: `build.sh` compiles arm64 and x86_64
  separately and `lipo`s them together — an arm64-only binary shipped
  once and broke installs on Intel Macs ("unsupported Mac" error).
- **Windows version stamping**: the `.csproj`'s `<Version>` doesn't
  update itself — CI passes `-p:Version=X.Y.Z` at build time
  (`test.yml`/`release.yml`). If you add a new Windows build/test step,
  make sure it also gets a `-p:Version=...` if the version needs to be
  meaningful (e.g. anything read via
  `AssemblyInformationalVersionAttribute`).
- **Linux version stamping**: `zikr/__version__` in the repo source is
  a static placeholder — `linux/packaging/build_deb.sh` `sed`-patches
  the *packaged copy* to the real `$VERSION` at build time. Never hand
  -edit the repo source's `__version__` expecting it to match a
  release; it drifted stale for several releases before this was
  caught.
- **WinForms + background threads**: don't capture
  `SynchronizationContext.Current` in a field initializer to marshal
  background work back to the UI thread — field initializers run
  before `Application.Run` installs the sync context, so it captures
  `null`. Use a handle-forced `Control` + `BeginInvoke` instead (see
  `TrayApp.cs`'s `_uiMarshal`) for any tray-only `ApplicationContext`
  app with no main form.
- **GitHub Actions queued-forever ≠ broken workflow**: if a run sits
  queued with zero jobs started for an extended period, check
  githubstatus.com before assuming the YAML is wrong — this has
  happened at least once due to a platform-wide Actions outage.
- **Android version stamping**: same class of bug as Windows/Linux,
  avoided proactively this time — `app/build.gradle.kts`'s
  `versionName` reads a Gradle property (`-PversionNameOverride=`,
  passed by `release.yml`) rather than a hardcoded value, specifically
  because the other two platforms shipped stale versions for multiple
  releases before anyone noticed.
- **Android `org.json` in local unit tests**: `org.json.JSONArray`/
  `JSONObject` are stub jars that throw `Stub!` in local JVM unit tests
  (`src/test`, as opposed to instrumented `src/androidTest`) — only the
  on-device implementation is real. Add
  `testImplementation("org.json:json:...")` (a real pure-JVM
  implementation of the same package) to actually exercise JSON-parsing
  logic in local tests, rather than reaching for
  `testOptions.unitTests.isReturnDefaultValues` (which just makes the
  stubs return nulls/zeros silently instead of throwing — not the same
  as parsing correctly).
- **Android Gradle wrapper jar**: `gradle-wrapper.jar` is a binary file
  that can't be hand-written. It was fetched directly from Gradle's own
  tagged release
  (`raw.githubusercontent.com/gradle/gradle/v8.9.0/gradle/wrapper/gradle-wrapper.jar`)
  rather than generated locally, since this dev machine has no JDK to
  run `gradle wrapper` with. `test.yml`'s `test-android` job runs
  `gradle/actions/wrapper-validation@v4` to check its checksum against
  Gradle's known-good list as a safety net for exactly this kind of
  manually-sourced binary.
- **Android: WorkManager `OneTimeWorkRequest` is not reliable for
  "must actually fire" reminders.** It looks completely fine in static
  review and even in `dumpsys jobscheduler` (job scheduled, later shows
  "READY" - all constraints satisfied) but can simply never get
  dispatched once Android freezes the app's process in the background -
  reproduced directly on the emulator, matching a real user report of
  "no zikr in duration." Fixed by switching to
  `AlarmManager.setExactAndAllowWhileIdle` (falling back to
  `setAndAllowWhileIdle` without the user-granted `SCHEDULE_EXACT_ALARM`
  permission) via a `BroadcastReceiver`, which is specifically allowed
  to wake a frozen/idle process - the standard mechanism real
  alarm/reminder apps use. Unlike WorkManager, this needs its own
  `BOOT_COMPLETED` receiver to re-arm after a reboot. If a future
  background-triggered feature is tempted to reach for WorkManager
  again, test it the same way this was caught: schedule a short
  interval, background the app, wait past the trigger time, and check
  whether it actually fired - not just whether it was scheduled.

## i18n (GitHub Pages site)

`docs/index.html` supports English, Bangla, and Arabic via a
`STRINGS` dict keyed by language in the trailing `<script>` block, with
`data-i18n`/`data-i18n-html` attributes on elements. **Any copy change
must be made in all three languages together** — don't add an
English-only string and leave BN/AR stale. Arabic also flips
`dir="rtl"` on `<html>`; check new layout in Arabic before shipping.
See `DESIGN.md` for the full design system (palette, type, layout
concept, icon style, motion rules) before making visual changes.

## What NOT to do

- Don't add anything that asks the user to open the app, tap something,
  track a streak, or check in — contradicts the core pitch.
- Don't add Adhan, prayer times, Qibla, or other Islamic-app features
  beyond dhikr reminders — scope is intentionally narrow (see the
  site's FAQ, "Does Zikr play Adhan or show prayer times?").
- Don't add a cross-platform abstraction library for tray/notifications
  /TTS/etc. — the whole point is native-per-platform.
- Don't bundle third-party reciter audio without a verified open
  license — see the note in `README.md`'s Design notes section about
  why no audio is bundled today.
