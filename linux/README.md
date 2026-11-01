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

**Updates install through apt, never by overwriting files.** An
automatic check runs on launch and every 24 hours, and "Check for
Updates…" runs one on demand. When a new release exists, the tray menu
gets an **Update to vX…** item at the top, and the Skip / Remind Later /
Install Update dialog appears with the changelog inline. Installing
downloads the release's `.deb` and runs
`pkexec apt-get install` on it (your desktop asks for your password),
then restarts Zikr. If `pkexec` isn't available, the `.deb` opens in
your software installer instead. If Zikr isn't running from the `.deb`
(a source checkout, a venv), there's nothing for apt to upgrade, so it
opens the release page. All of this lives in `zikr/updater.py`.
"What's New…" shows the changelog on its own, update available or not.

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
  recitation ships for every phrase, installed to
  `/usr/share/zikr/audio/<id>.ogg` by `build_deb.sh` and preferred over
  TTS. Vorbis rather than the canonical mp3 because `paplay`/`aplay` go
  through libsndfile, which reads Ogg but not mp3; `tools/convert_audio.py`
  does the conversion at package time so the repo keeps one copy of the
  audio, not three.
