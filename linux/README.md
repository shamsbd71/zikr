# Zikr for Linux

Same app as the macOS build, ported natively rather than recompiled —
SwiftUI/AppKit don't exist on Linux, so this is Python + GTK3, built on
the actual Linux desktop standards:

| macOS build              | Linux build                                   |
|---------------------------|------------------------------------------------|
| MenuBarExtra               | AppIndicator (Ayatana, with AppIndicator3 and GtkStatusIcon fallbacks) |
| UNUserNotificationCenter   | libnotify (freedesktop.org Notifications spec) |
| AVSpeechSynthesizer         | speech-dispatcher, falling back to espeak-ng directly |
| SMAppService (login item)  | XDG autostart (`~/.config/autostart/zikr.desktop`) |
| UserDefaults                | JSON file under `$XDG_CONFIG_HOME/zikr/`       |
| CoreAudio `DeviceIsRunningSomewhere` (mic-in-use check) | `pactl list short source-outputs`, falling back to ALSA's `/proc/asound/card*/pcm*c/sub*/status` |

Same 21-phrase zikr list, same settings (interval range, notification vs.
full-screen flash with adjustable duration, speak-aloud toggle, launch at
login, pause during calls), same bundled-audio-overrides-system-voice
behavior.

**One real difference: no in-place self-updater.** The macOS build
replaces its own `.app` bundle because that's a well-established, safe
pattern for a single signed bundle. Linux installs are far more varied
(apt-installed dist-packages, a `~/.local` user install, someone's own
venv...) — silently overwriting arbitrary files across those isn't a
sound default. "Check for Updates…" opens the GitHub releases page
instead, so you grab the new `.deb` (or re-run `install.sh`) yourself.

## Install

**Debian / Ubuntu / Mint / Pop!_OS** — download the `.deb` from
[releases](https://github.com/shamsbd71/zikr/releases/latest):

```sh
sudo apt install ./zikr_*_all.deb
```

**Fedora / Arch / openSUSE / anything else** — no native package (see
*Known limitations*), user-local install instead:

```sh
git clone https://github.com/shamsbd71/zikr.git
cd zikr/linux
./packaging/install.sh
```

The installer prints the one-time system package command for your distro
(GTK/AppIndicator/notification/speech bindings only come from your
package manager, not pip — that's normal for GTK apps).

## Run

```sh
zikr
```

It lives in the system tray from there. Click the icon for Settings,
a manual test, or Quit.

## Build the .deb yourself

```sh
cd linux
VERSION=1.1.0 ./packaging/build_deb.sh
```

Produces `dist/zikr_1.1.0_all.deb`. Architecture-independent — it's pure
Python, no compiled binary, so one package covers x86_64, arm64, etc.

## Known limitations

- **Vanilla GNOME** (no extensions) doesn't render AppIndicator tray
  icons at all — install the *AppIndicator and KStatusNotifierItem
  Support* GNOME Shell extension, or use a GTK-status-icon-aware
  environment (KDE Plasma, XFCE, MATE, Cinnamon, or Ubuntu's default
  session, which ships the extension already).
- Arabic TTS quality depends entirely on what's on the system —
  speech-dispatcher's espeak-ng backend is functional but robotic,
  same tradeoff as the macOS build's default voice. Drop a real
  recording at `zikr/data/audio/<id>.{ogg,mp3,wav,flac}` (id matches
  `zikr.json`) to override it, same convention as the macOS build's
  `Resources/Audio/`.
