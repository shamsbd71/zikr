# Zikr — instructions for Claude

@AGENTS.md

The shared rules for every AI agent live in `AGENTS.md` (imported
above) and the human guide in `CONTRIBUTING.md`. Edit those, not this
file, for anything that applies to all contributors. This file holds
only Claude-specific and maintainer-machine notes.

## Maintainer machine notes

These apply to the maintainer's Mac; a contributor's setup may differ.

- macOS only — no `dotnet` and no `gi`/GTK, so Linux/Windows changes
  can't be built here. Verify them through CI (`gh run watch`).
- No full Xcode (Command Line Tools only), so no iOS SDK and no
  xcbuild — that's why `build.sh` uses SwiftPM + `lipo`.
- Android has a full local toolchain and **should be used, not
  skipped**: Android Studio's bundled JDK and the SDK at
  `~/Library/Android/sdk` with AVD `Medium_Phone_API_36.1` (arm64,
  hardware-accelerated). Boot it headless:

  ```
  export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
  export ANDROID_HOME="$HOME/Library/Android/sdk"
  export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"
  emulator -avd Medium_Phone_API_36.1 -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect &
  adb wait-for-device
  ```

  Then `adb install -r <apk>`, drive the UI with `adb shell input`, and
  check what really happened with `adb logcat` / `adb shell dumpsys`.

## Releasing (maintainer only)

Follow CONTRIBUTING.md → Releasing. In short: PR merged with green CI →
rename `[Unreleased]` to `## [X.Y.Z] — YYYY-MM-DD` (Asia/Dhaka) → bump
the version footer in `docs/index.html` in all three languages → commit
to `main` → tag `vX.Y.Z` and push → `gh run watch` the Release run and
confirm all four assets are attached. Patch for fixes, minor for a new
feature/setting.
