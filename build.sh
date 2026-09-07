#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")"

APP_NAME="Zikr"
BUNDLE_ID="com.abu.ZikrReminder"
APP_DIR="dist/${APP_NAME}.app"
INSTALL_DIR="${HOME}/Applications"
# Local builds stamp a version above any published release on purpose.
# With "Automatically download and install updates" enabled, a dev build
# stamped 1.0 sees the latest GitHub release as newer, downloads it, and
# overwrites itself - the running app silently becomes the last release,
# resources and all, which looks exactly like your changes not working.
# CI always passes VERSION explicitly (see release.yml), so this default
# only ever applies to local builds.
VERSION="${VERSION:-99.0.0-dev}"
INSTALL="${INSTALL:-1}"

echo "==> Building release binary (universal: arm64 + x86_64)"
swift build -c release --product ZikrReminder --arch arm64
swift build -c release --product ZikrReminder --arch x86_64
mkdir -p .build/universal
lipo -create \
  ".build/arm64-apple-macosx/release/ZikrReminder" \
  ".build/x86_64-apple-macosx/release/ZikrReminder" \
  -output ".build/universal/ZikrReminder"
lipo -info ".build/universal/ZikrReminder"
BUILD_DIR=".build/universal"

echo "==> Generating app icon"
mkdir -p Resources
swift run -c release IconGen Resources/icon_1024.png >/dev/null

ICONSET="Resources/AppIcon.iconset"
rm -rf "$ICONSET"
mkdir -p "$ICONSET"
for size in 16 32 128 256 512; do
  sips -z $size $size Resources/icon_1024.png --out "$ICONSET/icon_${size}x${size}.png" >/dev/null
  double=$((size * 2))
  sips -z $double $double Resources/icon_1024.png --out "$ICONSET/icon_${size}x${size}@2x.png" >/dev/null
done
cp Resources/icon_1024.png "$ICONSET/icon_512x512@2x.png"
iconutil -c icns "$ICONSET" -o Resources/AppIcon.icns
rm -rf "$ICONSET"

echo "==> Assembling app bundle"
rm -rf "$APP_DIR"
mkdir -p "$APP_DIR/Contents/MacOS" "$APP_DIR/Contents/Resources"
cp "$BUILD_DIR/ZikrReminder" "$APP_DIR/Contents/MacOS/$APP_NAME"
cp Resources/AppIcon.icns "$APP_DIR/Contents/Resources/AppIcon.icns"

cp data/zikr.json "$APP_DIR/Contents/Resources/zikr.json"

# Amiri (SIL OFL 1.1) — a real Naskh face for the Arabic. The system font
# has poor Arabic shaping, which made the flash card look like a missing
# font. FontLoader registers these explicitly at launch - the Info.plist
# ATSApplicationFontsPath key alone did not take.
cp -R Resources/Fonts "$APP_DIR/Contents/Resources/Fonts"

# Recitations, keyed by zikr id. data/audio is the canonical location
# shared with the other platforms; Resources/Audio stays supported so a
# locally dropped-in clip still overrides.
if [ -d "data/audio" ]; then
  mkdir -p "$APP_DIR/Contents/Resources/Audio"
  cp data/audio/*.mp3 "$APP_DIR/Contents/Resources/Audio/" 2>/dev/null || true
fi
if [ -d "Resources/Audio" ]; then
  mkdir -p "$APP_DIR/Contents/Resources/Audio"
  cp -R "Resources/Audio/." "$APP_DIR/Contents/Resources/Audio/"
fi

cat > "$APP_DIR/Contents/Info.plist" <<PLIST
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleName</key>
    <string>${APP_NAME}</string>
    <key>CFBundleDisplayName</key>
    <string>${APP_NAME}</string>
    <key>CFBundleIdentifier</key>
    <string>${BUNDLE_ID}</string>
    <key>ATSApplicationFontsPath</key>
    <string>Fonts</string>
    <key>CFBundleVersion</key>
    <string>${VERSION}</string>
    <key>CFBundleShortVersionString</key>
    <string>${VERSION}</string>
    <key>CFBundlePackageType</key>
    <string>APPL</string>
    <key>CFBundleExecutable</key>
    <string>${APP_NAME}</string>
    <key>CFBundleIconFile</key>
    <string>AppIcon</string>
    <key>LSMinimumSystemVersion</key>
    <string>14.0</string>
    <key>LSUIElement</key>
    <true/>
    <key>LSApplicationCategoryType</key>
    <string>public.app-category.lifestyle</string>
    <key>NSHumanReadableCopyright</key>
    <string>Personal use.</string>
    <key>NSSupportsAutomaticTermination</key>
    <true/>
    <key>NSSupportsSuddenTermination</key>
    <true/>
</dict>
</plist>
PLIST

echo "==> Code signing (ad-hoc)"
codesign --force --deep -s - "$APP_DIR"

if [ "$INSTALL" = "1" ]; then
  # Quit a running copy first. Installing over one leaves rm -rf half
  # done, and the cp that follows produces a bundle missing Resources -
  # which then dies at launch on the zikr.json fatalError, looking like a
  # code bug rather than a bad install.
  if pgrep -x "$APP_NAME" >/dev/null; then
    echo "==> Quitting running $APP_NAME"
    osascript -e "tell application \"$APP_NAME\" to quit" 2>/dev/null || pkill -x "$APP_NAME" || true
    for _ in 1 2 3 4 5; do pgrep -x "$APP_NAME" >/dev/null || break; sleep 1; done
  fi

  mkdir -p "$INSTALL_DIR"
  rm -rf "${INSTALL_DIR}/${APP_NAME}.app"
  cp -R "$APP_DIR" "$INSTALL_DIR/"
  echo "==> Done: ${INSTALL_DIR}/${APP_NAME}.app"
else
  echo "==> Done: ${APP_DIR}"
fi
