# Zikr for Windows

Same app as the macOS and Linux builds, ported natively — C#/.NET
Framework 4.8 + WinForms, built on actual Windows desktop standards:

| macOS build              | Linux build                                   | Windows build                          |
|---------------------------|------------------------------------------------|------------------------------------------|
| MenuBarExtra               | AppIndicator (Ayatana/AppIndicator3/GtkStatusIcon) | `NotifyIcon` (system tray)         |
| UNUserNotificationCenter   | libnotify                                        | `NotifyIcon.ShowBalloonTip` (renders as a modern Action Center toast on Win10+) |
| AVSpeechSynthesizer         | speech-dispatcher / espeak-ng                   | `System.Speech.Synthesis` (SAPI5)     |
| SMAppService (login item)  | XDG autostart entry                             | `HKCU\...\Run` registry value          |
| UserDefaults                | JSON under `$XDG_CONFIG_HOME/zikr/`             | JSON under `%APPDATA%\Zikr\`           |

Same 21-phrase zikr list, same settings (interval range, notification vs.
full-screen flash with adjustable duration, speak-aloud toggle, launch at
login), same bundled-audio-overrides-voice convention.

## Why .NET Framework 4.8, not modern .NET

.NET Framework 4.8 ships built into every Windows 10/11 install already —
zero runtime install friction, matching the "just download and run"
distribution used for the other platforms. Its IL is architecture-neutral
(AnyCPU) and runs natively on both x64 and ARM64 Windows through the OS's
built-in CLR, **with a single build** — no separate per-architecture
binaries. That directly avoids the class of bug that broke the early
macOS releases on Intel Macs (an arm64-only binary shipped as if it were
universal), rather than relying on remembering to combine architectures
correctly every release.

## No in-place self-updater

Same decision as Linux, for the same reason: installs vary (Inno Setup to
`Program Files`, a future winget/choco package, a portable extraction),
so silently overwriting arbitrary files isn't a sound default across all
of them. "Check for Updates…" opens the GitHub releases page instead.

## Install

Download `ZikrSetup-<version>.exe` from
[releases](https://github.com/shamsbd71/zikr/releases/latest) and run it.
It's a per-user install (no admin rights needed) via Inno Setup — Start
Menu shortcut, optional desktop shortcut, standard uninstaller entry in
"Apps & features".

Windows will show a SmartScreen warning on first run since the binary
isn't signed with a paid code-signing certificate (same situation as the
ad-hoc-signed macOS build). Click "More info" → "Run anyway".

## Build it yourself

Requires the .NET SDK (which includes MSBuild support for `net48`
SDK-style projects) — Visual Studio Build Tools or the .NET SDK installer
both work.

```powershell
cd windows
dotnet build Zikr\Zikr.csproj -c Release
```

Produces `Zikr\bin\Release\net48\Zikr.exe` plus its `Resources\` folder
(icon, zikr.json) alongside it.

## Run the tests

```powershell
cd windows
dotnet test
```

Unit tests cover the zikr list, the random-interval scheduling math, and
`Settings` persistence/registry handling — the registry and config-dir
tests use injected throwaway paths (see `Settings`'s constructor), never
the real `%APPDATA%\Zikr` or the real `Run` key.

## Build the installer

Requires [Inno Setup](https://jrsoftware.org/isinfo.php) (`iscc.exe` on
PATH — `choco install innosetup` on the CI runner, or the installer from
their site locally):

```powershell
cd windows
dotnet build Zikr\Zikr.csproj -c Release
iscc /DMyAppVersion=1.3.0 packaging\ZikrSetup.iss
```

Produces `dist\ZikrSetup-1.3.0.exe`.

## Known limitations

- No Arabic SAPI voice ships with Windows by default (unlike macOS, which
  ships one). `Speech.cs` looks for one and falls back to reading the
  transliteration in English if none is installed. Users can install an
  Arabic language pack (Settings → Time & Language → Language & region)
  to get one, or drop a real recording at
  `Resources\audio\<id>.wav` (id matches `zikr.json`) to override it
  entirely, same convention as the other builds.
- Only `.wav` is supported for bundled audio overrides (via the built-in
  `System.Media.SoundPlayer`, kept dependency-free rather than adding an
  MP3/OGG decoding library for a feature nobody's using yet).
