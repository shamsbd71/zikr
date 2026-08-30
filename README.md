# Zikr

A minimal system-tray app that surfaces a short dhikr (Subhanallah, Alhamdulillah, Allahu Akbar, La ilaha illallah, and 17 others from the general Hisnul Muslim collection) at random intervals throughout the day. Native builds for **macOS** (this README), **[Linux](linux/README.md)** (Python/GTK), **[Windows](windows/README.md)** (C#/.NET Framework + WinForms), and **[Android](android/README.md)** (Kotlin/Jetpack Compose, Phase 1 — ambient reminders only, sideloaded APK) — see those READMEs for why each is a separate implementation, not a recompile.

Repo: https://github.com/shamsbd71/zikr · Site: https://shamsbd71.github.io/zikr/ · License: [MIT](LICENSE)

## What it does

- Lives only in the menu bar (crescent icon) — no Dock icon, no windows unless you open Settings.
- Picks a random zikr and shows it every N–M minutes (you set the range).
- Speaks the zikr aloud in Arabic using the built-in macOS voice, and shows it via a system notification or a soft full-screen flash (your choice, with adjustable on-screen duration).
- Says "Bismillah" once at launch (covers login, if "Launch at login" is on) and again every time the screen is unlocked.
- Optional "Launch at login" via `SMAppService` (Apple's current login-item API — no LaunchAgent plist needed).
- Optional "Pause during calls (mic in use)" — skips a reminder rather than talking over a meeting, by checking whether any app (this one or another, e.g. Zoom or a browser tab) currently has the microphone open.
- "Check for Updates…" and "What's New…" in the menu — checks run automatically on launch and every 24 hours too. The update dialog shows the changelog inline and installs itself in place (or opt into "Automatically download and install updates" to skip the dialog entirely).
- Settings, on/off, interval, style, and voice toggle are the only options. Nothing else — no azan, no prayer times, no accounts.

## Design notes

- **Audio**: zikr is spoken aloud with `AVSpeechSynthesizer` using the Arabic system voice ("Majed", installed on every Mac), so it's pronounced rather than just chimed. Falls back to reading the transliteration in English if no Arabic voice is present. To use a real reciter's audio instead, drop a clip at `Resources/Audio/<id>.mp3` (id matches the `Zikr.id` in `ZikrList.swift`; `.m4a`/`.caf`/`.wav`/`.aiff` also work) — `build.sh` bundles the folder automatically and `ZikrSpeaker` prefers it over the system voice. We didn't bundle any ourselves: the well-known Hisnul Muslim recordings we found (Internet Archive, IslamHouse) are full multi-dua CD tracks with no clear per-phrase reuse license, not something to include without a verified license.
- **Icon**: `Sources/IconGen` draws the app icon as vector shapes (Core Graphics) — a crescent + star on a teal/gold squircle — rather than a raster asset pulled from somewhere. Re-run it any time to tweak colors/geometry.
- **Memory footprint**: the whole app is one `Timer`, a 24-item struct array, and a couple of small views. No polling loops, no persistent windows, no database.
- **Self-update**: no third-party updater framework. `UpdateChecker` hits the GitHub Releases API directly; `UpdateFlow` decides whether to ask first (the normal case, via a Sparkle-style dialog showing what's new) or install silently (only if you opted into automatic installs). Installing downloads the release zip, clears its quarantine flag (it's our own ad-hoc-signed build, not a third-party download), swaps it over the running `.app`, and relaunches. `UpdateScheduler` runs the check once shortly after launch and then every 24 hours.

## Build & install

```sh
./build.sh
```

This compiles a release binary via Swift Package Manager, regenerates the icon, assembles `dist/Zikr.app`, ad-hoc code-signs it, and installs it to `~/Applications/Zikr.app`. Open it from there (or `open ~/Applications/Zikr.app`).

Requires macOS 14+ and the Swift toolchain (Xcode Command Line Tools are enough — no full Xcode needed). The built binary is universal (arm64 + Intel x86_64) — `build.sh` compiles both slices separately and combines them with `lipo`, since xcbuild-based multi-arch builds need full Xcode. This is also why older releases failed to open on Intel Macs with an "unsupported Mac" error: the binary was arm64-only.

## Releasing

Push a tag matching `vX.Y.Z` and `.github/workflows/release.yml` builds both platforms in parallel — the macOS app on a macOS runner, the Linux `.deb` on an Ubuntu runner (with a headless smoke test before packaging) — and publishes one GitHub Release with both assets attached:

```sh
git tag v1.1.1
git push origin v1.1.1
```

The macOS in-app updater compares its own `CFBundleShortVersionString` against the latest release tag, so the version in that tag is what users will be offered. Linux and Windows show the same update dialog but never install in place by design — see [linux/README.md](linux/README.md) and [windows/README.md](windows/README.md).

## Project layout

```
Sources/ZikrReminder/
  ZikrReminderApp.swift       — MenuBarExtra + Settings scene entry point
  Models/                     — Zikr struct + the bundled zikr list
  Services/                   — scheduler, speech, notifications, flash overlay, login item, updater, unlock greeter
  Views/                      — menu dropdown, settings form, flash overlay content
  Support/                    — AppSettings (UserDefaults-backed), DisplayStyle enum
Sources/IconGen/               — standalone tool that renders AppIcon
Sources/BannerGen/             — standalone tool that renders docs/og-banner (social preview image)
Resources/                     — generated icon assets (icon_1024.png, AppIcon.icns) — gitignored, rebuilt by build.sh
docs/                          — GitHub Pages site (https://shamsbd71.github.io/zikr/)
linux/                          — Linux build (Python/GTK) — see linux/README.md
.github/workflows/release.yml  — tag-triggered build + GitHub Release publish (both platforms)
build.sh                       — macOS build, icon regen, .app bundling, ad-hoc signing, install
```

To regenerate the site's social preview image after changing `Sources/BannerGen/main.swift`:

```sh
swift run BannerGen docs/og-banner.png
sips -s format jpeg -s formatOptions 82 docs/og-banner.png --out docs/og-banner.jpg
rm docs/og-banner.png
```
