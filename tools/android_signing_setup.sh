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
KEYTOOL="${KEYTOOL:-keytool}"
if ! command -v "$KEYTOOL" >/dev/null 2>&1; then
  KEYTOOL="/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/keytool"
fi

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
