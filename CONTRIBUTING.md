# Contributing to Zikr

Thanks for wanting to help. Zikr is small on purpose, so the most
useful contributions are bug fixes, better reliability, translations,
and careful work on one platform at a time.

Using an AI coding agent? Point it at [AGENTS.md](AGENTS.md). Most
agents pick it up automatically, and it has the same rules as this file.

## Before you start: what Zikr is and isn't

Zikr speaks a short dhikr at random times and then gets out of the way.
**The user does nothing.** That one idea decides most proposals:

| Welcome | Out of scope |
|---|---|
| Bug fixes and reliability (reminders that don't fire, audio, tray icons, installers) | Anything that asks the user to open the app, tap, count, check in, or keep a streak |
| Matching a feature across macOS / Linux / Windows | Adhan, prayer times, Qibla, Quran reader, or other Islamic-app features |
| New adhkar with a verified source and openly licensed audio | Audio from reciters without a verified open license |
| Site copy and translations (EN / BN / AR) | Cross-platform frameworks (Electron, Flutter, Qt, Tauri…) |
| Accessibility, packaging for more distros | Accounts, analytics inside the app, ads |

Not sure? Open an issue (or a discussion) first. A two-line "would you
take a PR that…?" saves everyone time.

## How the project is laid out

Four native apps, one shared data folder:

| Platform | Code | Language | Tests |
|---|---|---|---|
| macOS | `Sources/ZikrReminder/` | Swift / SwiftUI + AppKit | CI build + launch smoke test |
| Linux | `linux/zikr/` | Python 3 + GTK3 | `linux/tests/` |
| Windows | `windows/Zikr/` | C# / .NET Framework 4.8 + WinForms | `windows/Zikr.Tests/` |
| Android | `android/app/` | Kotlin / Jetpack Compose | `android/app/src/test/` |
| Shared | `data/zikr.json`, `data/audio/` | — | parsed by every platform's tests |

Each platform's README explains which native API it uses for the tray,
notifications, speech, autostart, and settings:
[Linux](linux/README.md) · [Windows](windows/README.md) ·
[Android](android/README.md) · macOS is in the main [README](README.md).

## Setting up

You only need the toolchain for the platform you're changing. CI builds
and tests all four on every pull request, so you can rely on it for the
others.

### macOS
- macOS 14+, Xcode Command Line Tools (`xcode-select --install`). Full
  Xcode is not needed.
- `./build.sh` builds a universal app and installs it to
  `~/Applications/Zikr.app`. `INSTALL=0 ./build.sh` only builds into
  `dist/`.
- Dev builds are stamped `99.0.0-dev` so the auto-updater won't swap
  them for the last release. If your changes seem to disappear, check
  the version in `~/Applications/Zikr.app/Contents/Info.plist`.

### Linux
- Debian/Ubuntu:
  `sudo apt install python3-gi gir1.2-notify-0.7 gir1.2-ayatanaappindicator3-0.1 speech-dispatcher`
- Run from the checkout: `cd linux && python3 -m zikr`
- Test: `cd linux && python3 -m unittest discover -s tests -v`
- Package: `linux/packaging/build_deb.sh` (also needs `ffmpeg`,
  `dpkg-dev`).

### Windows
- .NET SDK plus the .NET Framework 4.8 targeting pack.
- Build: `dotnet build windows/Zikr/Zikr.csproj -c Release -p:Version=0.0.0-test`
- Test: `dotnet test windows/Zikr.Tests/Zikr.Tests.csproj -c Release`
- Installer: Inno Setup (`windows/packaging/ZikrSetup.iss`).

### Android
- JDK 17 and the Android SDK (Android Studio includes both).
- Build: `cd android && ./gradlew assembleDebug`
- Test: `cd android && ./gradlew testDebugUnitTest`
- For anything that runs in the background, test on an emulator or
  phone: set a short interval, background the app, wait, and confirm it
  actually spoke. Code that only looks right has failed here before.

## Making a change

1. **Fork** the repo and create a branch: `feat/…`, `fix/…`, `docs/…`,
   or `chore/…`.
2. **Keep the desktop apps in step.** A new setting or behavior on
   macOS, Linux, or Windows should land on all three, with the same
   default, the same menu wording, and the same place in Settings. Key
   names follow each platform's style (`quietHoursEnabled` /
   `quiet_hours_enabled` / `QuietHoursEnabled`). If you can only do one
   platform, that's fine. Say so in the PR and we'll finish the others
   or open follow-up issues.
3. **Android is behind on purpose** (Phase 1: reminders only). Please
   ask before porting a desktop feature to it.
4. **Put logic where it can be tested.** Interval math, time windows,
   parsing, and version comparison go in small functions with unit
   tests, not inside UI code.
5. **Update `CHANGELOG.md`** under `## [Unreleased]` with one bullet
   (Added / Fixed / Changed): what changed and why, written for users.
6. **Open a pull request.** Fill in the template. All four CI jobs must
   pass before merge.

### Adding or editing a dhikr

All platforms read `data/zikr.json`. Each entry looks like this:

```json
{
  "id": 23,
  "arabic": "حَسْبِيَ اللَّهُ …",
  "transliteration": "Hasbiyallahu …",
  "translation": "Allah is sufficient for me …",
  "windows": ["morning", "evening"],
  "seconds": 8.8,
  "source": "gtaf:81"
}
```

- `windows` is empty for "any time", or lists the times of day it
  belongs to. `seconds` is the recitation length. `source` points to the
  Hisn al-Muslim entry it came from.
- Audio goes in `data/audio/<id>.mp3`. `tools/fetch_audio.py --only <id>`
  fetches and cuts it from dua.gtaf.org. Explain in the PR where any
  other audio comes from and what license it uses.
- Never reuse or renumber an existing `id`. Settings and audio files
  depend on it.
- Please cite the hadith or Hisn al-Muslim reference. Reviewers will
  check the Arabic text against it.

### Website changes

The site is `docs/index.html`, served by GitHub Pages. Read
[DESIGN.md](DESIGN.md) first. Every piece of text exists in English,
Bangla, and Arabic in the `STRINGS` block at the bottom of the file.
Change all three together. If you can't translate, leave the English in
the BN/AR slots and say so in the PR so a translator can follow up.
Check the Arabic layout, which is right-to-left.

## Reporting bugs

Use the bug report template. The most helpful details are your OS and
version, the Zikr version (in the menu or Settings), what you expected,
and what happened. For "reminders stopped," say how long the app had
been running and whether the computer slept or the phone was idle.

Security issues: please don't open a public issue. See
[SECURITY.md](SECURITY.md).

## Releasing (maintainer)

1. Merge PRs with green CI.
2. Rename `## [Unreleased]` in `CHANGELOG.md` to
   `## [X.Y.Z] — YYYY-MM-DD` (Asia/Dhaka date). Use a patch version for
   fixes and a minor version for a new feature or setting.
3. Update the version footer in `docs/index.html` in all three
   languages.
4. Commit to `main`, then `git tag vX.Y.Z && git push origin vX.Y.Z`.
5. `release.yml` runs the full test suite again, then builds the macOS
   zip, Linux .deb, Windows installer, and Android APK and publishes one
   GitHub Release. Watch it with `gh run watch` and check that all four
   files are attached.

## Running your own fork

See [FORKING.md](FORKING.md) for pointing the updater, the site, and the
installers at your own repo.

## Code of conduct

Be kind and patient. This project is part of people's worship. See
[CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md).

By contributing, you agree that your contributions are licensed under
the project's [MIT License](LICENSE).
