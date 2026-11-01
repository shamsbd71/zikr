# Zikr

A minimal system-tray app that surfaces a short dhikr (Subhanallah, Alhamdulillah, Allahu Akbar, La ilaha illallah, and 45 more from the Hisnul Muslim collection — morning, evening and night adhkar are weighted toward their hour) at random intervals throughout the day. Native builds for **macOS** (this README), **[Linux](linux/README.md)** (Python/GTK), **[Windows](windows/README.md)** (C#/.NET Framework + WinForms), and **[Android](android/README.md)** (Kotlin/Jetpack Compose, Phase 1 — ambient reminders only, sideloaded APK) — see those READMEs for why each is a separate implementation, not a recompile.

Repo: https://github.com/shamsbd71/zikr · Site: https://shamsbd71.github.io/zikr/ · License: [MIT](LICENSE)

## What it does

- Lives only in the menu bar (crescent icon) — no Dock icon, no windows unless you open Settings.
- Picks a random zikr and shows it every N–M minutes (you set the range).
- Speaks the zikr aloud in Arabic using the built-in macOS voice, and shows it via a system notification or a soft full-screen flash (your choice, with adjustable on-screen duration).
- Says "Bismillah" once at launch (covers login, if "Launch at login" is on) and again every time the screen is unlocked.
- Optional "Launch at login" via `SMAppService` (Apple's current login-item API — no LaunchAgent plist needed).
- Optional "Pause during calls (mic in use)" — skips a reminder rather than talking over a meeting, by checking whether any app (this one or another, e.g. Zoom or a browser tab) currently has the microphone open.
- "Check for Updates…" and "What's New…" in the menu — checks run automatically on launch and every 24 hours too. When an update exists, an "Update to vX…" item appears at the top of the menu. The update dialog shows the changelog inline and installs itself in place (or opt into "Automatically download and install updates" to skip the dialog entirely).
- Settings, on/off, interval, style, and voice toggle are the only options. Nothing else — no azan, no prayer times, no accounts.

## Design notes

- **Audio**: every phrase on every platform ships with a real recitation, cut from the Hisn al-Muslim recordings published by [dua.gtaf.org](https://dua.gtaf.org/) and stored as `data/audio/<id>.mp3` (id matches the `id` in `data/zikr.json`). `tools/fetch_audio.py` is the pipeline that fetches and cuts them, and is the place to read for how a clip is chosen and checked. mp3 is the one copy kept in the repo; macOS and Android play it directly, while Linux (Ogg, for libsndfile) and Windows (PCM wav, for `SoundPlayer`) convert at package time via `tools/convert_audio.py`. Where no clip exists, `AVSpeechSynthesizer` speaks the Arabic with the system voice ("Majed", installed on every Mac) so the phrase is still pronounced rather than chimed, falling back to the transliteration if no Arabic voice is present. Dropping a file at `Resources/Audio/<id>.mp3` (`.m4a`/`.caf`/`.wav`/`.aiff` also work) overrides the bundled clip.
- **Icon**: `Sources/IconGen` draws the app icon as vector shapes (Core Graphics) — a crescent + star on a teal/gold squircle — rather than a raster asset pulled from somewhere. Re-run it any time to tweak colors/geometry.
- **Memory footprint**: the whole app is one `Timer`, the zikr list decoded from JSON, and a couple of small views. No polling loops, no persistent windows, no database.
- **Self-update**: no third-party updater framework. `UpdateChecker` hits the GitHub Releases API directly; `UpdateFlow` decides whether to ask first (the normal case, via a Sparkle-style dialog showing what's new) or install silently (only if you opted into automatic installs). Installing downloads the release zip, clears its quarantine flag (it's our own ad-hoc-signed build, not a third-party download), swaps it over the running `.app`, and relaunches. `UpdateScheduler` runs the check once shortly after launch and then every 24 hours.

## Build & install

```sh
./build.sh
```

This compiles a release binary via Swift Package Manager, regenerates the icon, assembles `dist/Zikr.app`, ad-hoc code-signs it, and installs it to `~/Applications/Zikr.app`. Open it from there (or `open ~/Applications/Zikr.app`).

Requires macOS 14+ and the Swift toolchain (Xcode Command Line Tools are enough — no full Xcode needed). The built binary is universal (arm64 + Intel x86_64) — `build.sh` compiles both slices separately and combines them with `lipo`, since xcbuild-based multi-arch builds need full Xcode. This is also why older releases failed to open on Intel Macs with an "unsupported Mac" error: the binary was arm64-only.

## Releasing

Push a tag matching `vX.Y.Z` and `.github/workflows/release.yml` re-runs the full test suite, then builds all four platforms in parallel and publishes one GitHub Release with the macOS zip, Linux `.deb`, Windows installer, and Android APK attached. The full checklist is in [CONTRIBUTING.md](CONTRIBUTING.md#releasing-maintainer).

Every app compares its own version against the latest release tag (on macOS, `CFBundleShortVersionString`), so the version in that tag is what users will be offered. When there's a newer release, each app shows an **Update to vX…** item in its menu (a banner on Android), and one click installs it: macOS swaps its own bundle, Windows runs the release's installer silently, Linux installs the `.deb` through apt, and Android opens the system installer. See [linux/README.md](linux/README.md), [windows/README.md](windows/README.md), and [android/README.md](android/README.md).

## Project layout

```
Sources/ZikrReminder/          — macOS app (SwiftPM, no Xcode project)
  Models/                      — Zikr struct, JSON loader, time-of-day selector
  Services/                    — scheduler, speech, notifications, flash overlay, login item, updater, unlock greeter
  Views/                       — menu dropdown, settings form, flash overlay, update dialog
  Support/                     — AppSettings (UserDefaults-backed), DisplayStyle enum
Sources/IconGen/               — renders the app icon from vector code
Sources/BannerGen/             — renders docs/og-banner (social preview image)
linux/                         — Linux app (Python/GTK3) + .deb packaging — see linux/README.md
windows/                       — Windows app (C#/WinForms) + tests + installer — see windows/README.md
android/                       — Android app (Kotlin/Compose, Phase 1) — see android/README.md
data/                          — zikr.json + recitation audio, shared by every platform
tools/                         — audio fetch/convert, release stats, fork setup
docs/                          — GitHub Pages site (https://shamsbd71.github.io/zikr/) + design specs
.github/workflows/             — test.yml (CI, all platforms) + release.yml (tag-triggered release)
build.sh                       — macOS build, icon regen, .app bundling, ad-hoc signing, install
```

To regenerate the site's social preview image after changing `Sources/BannerGen/main.swift`:

```sh
swift run BannerGen docs/og-banner.png
sips -s format jpeg -s formatOptions 82 docs/og-banner.png --out docs/og-banner.jpg
rm docs/og-banner.png
```

## Contributing

Bug reports, fixes, translations, and careful platform work are welcome. Start with [CONTRIBUTING.md](CONTRIBUTING.md). It covers what's in scope, how to build each platform, and the PR checklist. If you're using an AI coding agent, point it at [AGENTS.md](AGENTS.md). To ship your own builds from a fork, see [FORKING.md](FORKING.md). Please also read the [Code of Conduct](CODE_OF_CONDUCT.md), and report security issues privately as described in [SECURITY.md](SECURITY.md).
