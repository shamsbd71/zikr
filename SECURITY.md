# Security policy

## Supported versions

Only the latest release gets fixes. Every app checks for updates on
its own, so most users are on the newest version.

## Reporting a vulnerability

Please **don't open a public issue**. Report it privately through
GitHub: go to the repo's **Security** tab and choose **Report a
vulnerability**. Include what you found, how to reproduce it, and the
platform and version.

You should get a reply within a week. Once a fix is released, you'll be
credited in the changelog unless you'd rather not be.

## What's in scope

The parts of Zikr worth looking at most closely:

- **The updaters.** Every platform calls the GitHub Releases API. The
  macOS app downloads a zip, clears its quarantine flag, and replaces
  itself. The Android app downloads and offers to install an APK.
  Anything that could make an app fetch or install something other than
  a genuine release from this repo is serious.
- **Installers and packages**: the Windows installer, the Linux .deb
  maintainer scripts, and autostart entries.
- **The website** (`docs/`): it's static, but injected scripts or
  broken content security are in scope.

Zikr has no accounts and no servers, and the apps collect nothing, so
there is no user data on our side to leak.
