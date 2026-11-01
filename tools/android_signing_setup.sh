#!/usr/bin/env bash
# One-time: create Zikr's permanent Android release key and hand it to
# GitHub Actions as secrets, so every release APK is signed with the
# same key and in-app updates install over the previous version.
#
#   tools/android_signing_setup.sh
#
# The keystore and its password stay in ~/.zikr-signing (never in the
# repo). BACK THAT FOLDER UP somewhere safe (password manager, encrypted
# drive). If it is lost, no future APK can update existing installs.
set -euo pipefail

D="$HOME/.zikr-signing"
K="$D/zikr-release.jks"
# macOS ships a /usr/bin/keytool stub that only works when a JDK is
# installed, so "is it on PATH" isn't enough - pick the first one that
# actually runs. Android Studio's bundled JDK is the usual answer.
KEYTOOL=""
for k in "${KEYTOOL_PATH:-}" \
         "${JAVA_HOME:+$JAVA_HOME/bin/keytool}" \
         "/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/keytool" \
         "$HOME/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/keytool" \
         keytool; do
  [ -n "$k" ] || continue
  if "$k" -help >/dev/null 2>&1; then KEYTOOL="$k"; break; fi
done
if [ -z "$KEYTOOL" ]; then
  echo "No working keytool found. Install a JDK or Android Studio, or set KEYTOOL_PATH." >&2
  exit 1
fi
command -v gh >/dev/null 2>&1 && gh auth status >/dev/null 2>&1 || {
  echo "GitHub CLI isn't logged in - run: gh auth login" >&2
  exit 1
}

mkdir -p "$D"; chmod 700 "$D"
if [ -f "$K" ]; then
  echo "Using existing $K"
  # shellcheck disable=SC1091
  source "$D/credentials.env"
  PW="$ZIKR_KEYSTORE_PASSWORD"
else
  PW="$(openssl rand -base64 32 | tr -dc 'A-Za-z0-9' | head -c 28)"
  "$KEYTOOL" -genkeypair -keystore "$K" -storetype PKCS12 -alias zikr \
    -keyalg RSA -keysize 4096 -validity 36500 \
    -storepass "$PW" -keypass "$PW" -dname "CN=Zikr, O=Zikr, C=BD" >/dev/null
  printf 'ZIKR_KEYSTORE_PASSWORD=%s\nZIKR_KEY_ALIAS=zikr\nZIKR_KEY_PASSWORD=%s\n' "$PW" "$PW" > "$D/credentials.env"
  chmod 600 "$D"/*
  echo "Created $K"
fi

base64 < "$K" | tr -d '\n' | gh secret set ZIKR_KEYSTORE_BASE64
printf %s "$PW"  | gh secret set ZIKR_KEYSTORE_PASSWORD
printf %s zikr   | gh secret set ZIKR_KEY_ALIAS
printf %s "$PW"  | gh secret set ZIKR_KEY_PASSWORD
echo "GitHub secrets set. Back up $D now."
