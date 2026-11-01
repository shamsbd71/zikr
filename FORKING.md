# Running your own fork

Zikr is MIT-licensed, so you can fork it, change it, and ship your own
builds. If you just want to send a fix upstream, you don't need any of
this. Fork, branch, and open a PR (see [CONTRIBUTING.md](CONTRIBUTING.md)).

This page is for forks that **publish their own releases or site**.

## 1. Point the fork at itself

Every app checks GitHub for updates and downloads its "What's New" from
the upstream repo. Until you change that, your users will be offered
upstream's releases, and the macOS app will install them over your build.

```sh
python3 tools/fork_setup.py yourname/your-repo --dry-run   # preview
python3 tools/fork_setup.py yourname/your-repo             # apply
git diff                                                    # review, then commit
```

This rewrites the repo links in the updaters (macOS, Linux, Windows,
Android), the changelog fetchers, the installer and package metadata,
the READMEs, and the site. It also clears the site's Cloudflare
analytics token so your visitors aren't counted on upstream's dashboard.
It does not touch `LICENSE` (MIT requires you to keep the original
copyright line; you may add your own) or the history in `CHANGELOG.md`.

## 2. Turn on Actions and Pages

- **Actions**: forks start with workflows turned off. Enable them under
  the fork's *Actions* tab. `test.yml` then runs on every push and PR.
- **Releases**: push a tag `vX.Y.Z`. `release.yml` builds all four
  installers and publishes a GitHub Release using the built-in
  `GITHUB_TOKEN`. No secrets are needed.
- **Site**: *Settings → Pages → Deploy from branch → `main` / `docs`*.
  It will be served at `https://yourname.github.io/your-repo/`.
- **Analytics (optional)**: to use your own Cloudflare Web Analytics,
  paste your token into `CF_BEACON_TOKEN` in `docs/index.html`. With the
  token empty, the site makes no third-party requests.

## 3. Decide whether to install alongside upstream

By default your build uses the same app identifiers as upstream, so it
replaces an upstream install rather than sitting next to it. That's
usually what you want for a personal fork. If you're shipping a
separate app to other people, change these:

| Platform | Identifier | Where |
|---|---|---|
| macOS | `com.abu.ZikrReminder` | `build.sh` (Info.plist), logger subsystems and the notification id in `Sources/ZikrReminder/`, and the bundle-id check in `.github/workflows/test.yml` |
| Windows | Inno Setup `AppId` GUID | `windows/packaging/ZikrSetup.iss` (generate a new GUID) |
| Linux | package name `zikr` | `linux/packaging/debian/control`, `build_deb.sh` |
| Android | `com.abu.zikr` | `applicationId` in `android/app/build.gradle.kts` (the Kotlin `namespace`/packages can stay) |

Please also give it a different display name and icon, so users can
tell your app apart from upstream's.

## 4. Code signing

Upstream ships ad-hoc-signed macOS builds and an unsigned Windows
installer. The Android APK is signed with a release key kept in Actions
secrets.

**Android needs your own key** if you publish APKs, because Android only
installs an update over an app signed with the same key. Run
`tools/android_signing_setup.sh` once in your fork. It creates a key in
`~/.zikr-signing` (back it up) and sets the `ZIKR_KEYSTORE_*` secrets on
your fork. Without them, your release APKs fall back to the debug key,
which changes between CI runs, so in-app updates won't install.

If you add other signing (Apple Developer ID, Authenticode), store
those credentials as Actions secrets too. Never commit them.

## 5. Staying in sync

```sh
git remote add upstream https://github.com/shamsbd71/zikr.git
git fetch upstream
git merge upstream/main     # conflicts will mostly be the URLs from step 1
```

If you fix something that isn't specific to your fork, please send it
upstream too.
