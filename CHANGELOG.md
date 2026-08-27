# Changelog

All notable changes to Zikr are documented here. Dates are release dates
(Asia/Dhaka). See [releases](https://github.com/shamsbd71/zikr/releases)
for downloadable assets per version.

## [Unreleased]

### Added
- "Pause during calls (mic in use)" setting (on by default) — skips a
  scheduled reminder rather than talking over a meeting, by checking
  whether any app has the microphone open: CoreAudio's
  `DeviceIsRunningSomewhere` on macOS, `pactl`/ALSA proc status on
  Linux, and the `CapabilityAccessManager\ConsentStore\microphone`
  registry ledger on Windows. Manual "Test Zikr" always bypasses the
  check, same as it already bypasses the display-style setting.

## [1.3.0] — 2026-08-26

### Added
- Native Windows build (C#/.NET Framework 4.8 + WinForms) — system tray
  icon, SAPI5 speech, full-screen flash / toast notification, launch at
  login via the `Run` registry key, Inno Setup installer.
- GitHub Pages site updated with full platform support info (macOS,
  Linux, Windows) across all three languages.

## [1.2.1] — 2026-08-26

### Added
- Pre-release CI test gate: macOS, Linux, and Windows builds must pass
  their test suite before a tagged release is published.

### Fixed
- Linux `Settings`: `min_interval_minutes` could exceed
  `max_interval_minutes` with no correction, unlike the macOS build.
  Clamp logic now matches across platforms.

## [1.2.0] — 2026-08-26

### Added
- Native Linux build (Python/GTK, AppIndicator with GtkStatusIcon
  fallback, speech-dispatcher/espeak-ng, XDG autostart, `.deb`
  packaging) and a dual-platform release pipeline.

## [1.1.0] — 2026-08-26

### Added
- GitHub Pages site with SEO structure, trilingual support (English,
  Bangla, Arabic), USP-first hero messaging, and a jewel-tone color
  palette.
- Flash-visibility-duration setting.
- Bundled-audio fallback path (plays a real recording per zikr if
  present, falls back to speech synthesis otherwise).

### Fixed
- Universal binary (arm64 + x86_64) — fixes "unsupported Mac" install
  failure on Intel Macs, which previously shipped an Apple Silicon-only
  binary.

### Changed
- Trimmed the zikr list to a more focused set of 21 phrases.

## [1.0.1] — 2026-08-24

### Fixed
- Bismillah not speaking on screen unlock — switched from
  `NSWorkspace.sessionDidBecomeActiveNotification` (fast-user-switching
  only) to `DistributedNotificationCenter`'s
  `com.apple.screenIsUnlocked`.

## [1.0.0] — 2026-08-24

### Added
- Initial release: macOS menu bar app (SwiftUI/AppKit, MenuBarExtra).
- Spoken zikr via `AVSpeechSynthesizer` with Arabic pronunciation.
- Random-interval reminders with notification or full-screen flash
  display, configurable timing.
- Launch at login (`SMAppService`).
- Auto-update checker against GitHub Releases.
