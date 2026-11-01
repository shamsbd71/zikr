# AGENTS.md — instructions for AI coding agents

This file is for AI coding agents (Claude Code, Codex, Cursor, Copilot,
Gemini, Aider, …) working in this repo. Humans should read
[CONTRIBUTING.md](CONTRIBUTING.md) first; everything here applies to
them too. `CLAUDE.md` imports this file, so there is one source of truth.

## What this is

Zikr is a tray/menu-bar app that speaks a random dhikr (Islamic
remembrance phrase — SubhanAllah, Alhamdulillah, Allahu Akbar, etc.)
aloud at random intervals throughout the day. The entire product
philosophy is **the user does nothing**: no taps, no counting, no
habit-tracker streak. It just speaks, on its own schedule, and gets out
of the way.

Native builds for four platforms, each a separate, deliberately
non-shared implementation (not a port via a cross-platform toolkit):

| | macOS | Linux | Windows | Android |
|---|---|---|---|---|
| Language/UI | Swift/SwiftUI+AppKit | Python 3 + GTK3 | C#/.NET Framework 4.8 + WinForms | Kotlin/Jetpack Compose |
| Path | `Sources/ZikrReminder/` | `linux/zikr/` | `windows/Zikr/` | `android/app/` |
| Tests | CI build + launch smoke test | `linux/tests/` | `windows/Zikr.Tests/` | `android/app/src/test/` |
| Settings key style | `camelCase` | `snake_case` | `PascalCase` | `snake_case` |

Shared data, read by every platform (never duplicate it per platform):

- `data/zikr.json` — the one zikr list (id, arabic, transliteration,
  translation, `windows` = time-of-day tags, `seconds`, `source`).
- `data/audio/<id>.mp3` — the recitation for each id. Linux/Windows
  convert to ogg/wav at package time via `tools/convert_audio.py`.

## Hard rules

1. **Scope is intentionally narrow.** Don't add anything that asks the
   user to open the app, tap something, track a streak, or check in.
   Don't add Adhan, prayer times, Qibla, Quran reader, or other
   Islamic-app features beyond dhikr reminders.
2. **Native per platform.** Don't introduce a cross-platform abstraction
   library (Electron, Flutter, Qt, Tauri, a shared tray/TTS/notification
   lib…). Each subsystem uses that platform's own API — see
   `linux/README.md`, `windows/README.md`, `android/README.md` for the
   mapping tables.
3. **Desktop parity.** A feature added to one of macOS/Linux/Windows
   goes into all three unless there is a documented reason not to. Same
   setting key (in each platform's naming style), same default, same
   menu wording, same placement in Settings. Updates are a good example
   of parity done natively: all three show "Update to vX…" in the menu
   and install in one click, but macOS swaps its `.app`, Windows runs
   the release's Inno Setup installer silently, and Linux installs the
   `.deb` through `pkexec apt-get`.
4. **Android is Phase 1 on purpose.** Ambient random reminders only — no
   mic-pause, no changelog viewer, no launch-at-login. Its smaller
   feature set is not drift to "fix". Ask before porting a desktop
   feature to Android.
5. **iOS is designed, not started.** Read
   `docs/superpowers/specs/2026-09-21-ios-design.md` before writing any
   of it. It cannot copy the other platforms (no background process).
6. **No unlicensed audio.** Don't bundle third-party recitations without
   a verified open license. Current audio comes from dua.gtaf.org via
   `tools/fetch_audio.py`.
7. **Site copy is trilingual.** `docs/index.html` has EN/BN/AR strings in
   a `STRINGS` dict. Any copy change goes into all three languages in
   the same change. Arabic is RTL — check layout in Arabic. Read
   `DESIGN.md` before visual changes.
8. **Every user-visible change gets a `CHANGELOG.md` bullet** under
   `## [Unreleased]` (Added / Fixed / Changed), saying what and why.

## Build & test commands

| Platform | Build | Test | Needs |
|---|---|---|---|
| macOS | `./build.sh` (installs to `~/Applications`); `INSTALL=0 ./build.sh` to only build into `dist/` | CI only (build, universal-binary check, launch smoke test) | macOS 14+, Xcode Command Line Tools |
| Linux | `cd linux && python3 -m zikr` | `cd linux && python3 -m unittest discover -s tests -v` | python3-gi, GTK3, libnotify, AyatanaAppIndicator3, speech-dispatcher |
| Windows | `dotnet build windows/Zikr/Zikr.csproj -c Release -p:Version=0.0.0-test` | `dotnet test windows/Zikr.Tests/Zikr.Tests.csproj -c Release` | Windows, .NET SDK, .NET Framework 4.8 targeting pack |
| Android | `cd android && ./gradlew assembleDebug` | `cd android && ./gradlew testDebugUnitTest` | JDK 17, Android SDK |

CI (`.github/workflows/test.yml`) runs all four on every PR. It is the
source of truth.

## How to verify (don't trust "should work")

- If you cannot build a platform locally, say so, and verify through CI
  (`gh run watch <id>`). Read the logs — a green check doesn't always
  mean the step you care about ran.
- **Put pure logic in small testable functions** (interval math,
  settings clamping, version comparison, changelog parsing, zikr JSON
  parsing, quiet-hours windows) and test it on every platform where
  logic exists. Keep it out of UI/platform glue code.
- **Android: test background behavior on a real emulator or device.**
  Schedule a short interval, background the app, wait past the trigger
  time, and check it actually fired (`adb logcat`, `dumpsys alarm`).
  WorkManager looked correct in every static review and in `dumpsys
  jobscheduler` and still never fired once the process was frozen — the
  scheduler uses `AlarmManager.setExactAndAllowWhileIdle` for that
  reason. Don't switch it back. The same goes for updates: the APK is
  downloaded in the app and the installer opened from the foreground
  activity, because Android 10+ blocks a background receiver from
  opening it.
- **Android release signing**: every release must be signed with the
  same key (Actions secrets, created by `tools/android_signing_setup.sh`)
  or Android refuses the update. `versionCode` is derived from the
  version. Never ship a debug-signed APK as a release.
- **Android emulator tips**: get tap coordinates from
  `adb shell uiautomator dump` bounds, not by eyeballing a screenshot.
  Use `adb exec-out screencap -p > shot.png`. A "System UI isn't
  responding" dialog right after a cold boot under software rendering is
  emulator flakiness, not an app bug.

## Known gotchas

- **macOS dev builds can replace themselves with the last release** if
  "Automatically download and install updates" is on. `build.sh` stamps
  `99.0.0-dev` by default to prevent this. If a build seems to ignore
  your edits, check
  `/usr/libexec/PlistBuddy -c "Print :CFBundleShortVersionString" ~/Applications/Zikr.app/Contents/Info.plist`.
- **Don't install over a running Zikr** — `build.sh` quits it first; keep
  it that way.
- **macOS binary must stay universal** (arm64 + x86_64 via `lipo`). An
  arm64-only build once broke every Intel Mac.
- **Version stamping is done by CI, not source.** Windows gets
  `-p:Version=X.Y.Z`; Linux's `zikr/__version__` is a placeholder that
  `build_deb.sh` patches into the packaged copy; Android reads
  `-PversionNameOverride=`. Never hand-edit these expecting them to
  match a release.
- **WinForms + background threads**: don't capture
  `SynchronizationContext.Current` in a field initializer (it is `null`
  before `Application.Run`). Use the handle-forced control +
  `BeginInvoke` pattern in `TrayApp.cs` (`_uiMarshal`).
- **Android `org.json` in local unit tests** is a stub that throws.
  Tests use the real `org.json:json` test dependency — don't paper over
  it with `isReturnDefaultValues`.
- **`gradle-wrapper.jar` is a binary** fetched from Gradle's tagged
  release and checksum-validated in CI. Don't regenerate or hand-edit it.
- **Actions queued forever with zero jobs** is usually a GitHub outage
  (check githubstatus.com), not broken YAML.

## Repo layout

```
Sources/ZikrReminder/   macOS app (Swift Package Manager, no Xcode project)
Sources/IconGen/        renders the app icon from vector code
Sources/BannerGen/      renders docs/og-banner (social preview)
linux/                  Linux app (Python/GTK3) + .deb packaging
windows/                Windows app (C#/WinForms) + MSTest suite + Inno Setup installer
android/                Android app (Kotlin/Compose, Phase 1)
data/                   zikr.json + audio — shared by every platform
tools/                  audio fetch/convert, release stats, fork setup
docs/                   GitHub Pages site (trilingual) + design specs
.github/workflows/      test.yml (CI, all platforms) + release.yml (tag-triggered)
```

## Workflow for a change

1. Branch from `main` (`feat/…`, `fix/…`, `docs/…`, `chore/…`).
2. Make the change on every platform it applies to (rule 3).
3. Add/adjust tests for any logic you touched.
4. Add a `CHANGELOG.md` bullet under `## [Unreleased]`.
5. Open a PR; CI must be green on all four platforms.
6. Releases (version bump, tag) are done by the maintainer — see
   CONTRIBUTING.md → Releasing. Don't tag or bump versions in a PR.
