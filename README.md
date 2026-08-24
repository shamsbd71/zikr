# Zikr

A minimal macOS menu bar app that surfaces a short dhikr (Subhanallah, Alhamdulillah, Allahu Akbar, La ilaha illallah, and 20 others from the general Hisnul Muslim collection) at random intervals throughout the day.

## What it does

- Lives only in the menu bar (crescent icon) — no Dock icon, no windows unless you open Settings.
- Picks a random zikr and shows it every N–M minutes (you set the range).
- Two display styles: a system notification, or a soft full-screen flash that fades after ~2 seconds.
- Optional "Launch at login" via `SMAppService` (Apple's current login-item API — no LaunchAgent plist needed).
- Settings, on/off, and interval are the only options. Nothing else — no azan, no prayer times, no accounts, no network calls.

## Design notes

- **Audio**: the flash style plays the built-in macOS system sound "Tink" instead of a bundled recording. This avoids any audio-licensing question entirely (system sounds ship with every Mac) and keeps the app dependency-free. Swap `SoundPlayer.swift` for a bundled CC0/PD clip if you'd rather have something custom.
- **Icon**: `Sources/IconGen` draws the app icon as vector shapes (Core Graphics) — a crescent + star on a teal/gold squircle — rather than a raster asset pulled from somewhere. Re-run it any time to tweak colors/geometry.
- **Memory footprint**: the whole app is one `Timer`, a 24-item struct array, and a couple of small views. No polling loops, no persistent windows, no database.

## Build & install

```sh
./build.sh
```

This compiles a release binary via Swift Package Manager, regenerates the icon, assembles `dist/Zikr.app`, ad-hoc code-signs it, and installs it to `~/Applications/Zikr.app`. Open it from there (or `open ~/Applications/Zikr.app`).

Requires macOS 14+ and the Swift toolchain (Xcode Command Line Tools are enough — no full Xcode needed).

## Project layout

```
Sources/ZikrReminder/
  ZikrReminderApp.swift       — MenuBarExtra + Settings scene entry point
  Models/                     — Zikr struct + the bundled zikr list
  Services/                   — scheduler, notifications, flash overlay, sound, login item
  Views/                      — menu dropdown, settings form, flash overlay content
  Support/                    — AppSettings (UserDefaults-backed), DisplayStyle enum
Sources/IconGen/               — standalone tool that renders AppIcon
Resources/                     — generated icon assets (icon_1024.png, AppIcon.icns)
build.sh                       — build, icon regen, .app bundling, ad-hoc signing, install
```
