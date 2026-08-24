#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")"

APP_NAME="Zikr"
BUNDLE_ID="com.abu.ZikrReminder"
BUILD_DIR=".build/release"
APP_DIR="dist/${APP_NAME}.app"
INSTALL_DIR="${HOME}/Applications"
VERSION="${VERSION:-1.0}"
INSTALL="${INSTALL:-1}"

echo "==> Building release binary"
swift build -c release --product ZikrReminder

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

if [ -d "Resources/Audio" ]; then
  cp -R "Resources/Audio" "$APP_DIR/Contents/Resources/Audio"
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
  mkdir -p "$INSTALL_DIR"
  rm -rf "${INSTALL_DIR}/${APP_NAME}.app"
  cp -R "$APP_DIR" "$INSTALL_DIR/"
  echo "==> Done: ${INSTALL_DIR}/${APP_NAME}.app"
else
  echo "==> Done: ${APP_DIR}"
fi
