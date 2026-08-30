#!/bin/bash
# Builds zikr_<version>_all.deb from linux/zikr, for Debian/Ubuntu and
# derivatives (Mint, Pop!_OS, etc). Architecture-independent — it's pure
# Python, no compiled binary.
set -euo pipefail
cd "$(dirname "$0")/.."   # -> linux/

VERSION="${VERSION:-1.0.0}"
STAGE="$(mktemp -d)"
trap 'rm -rf "$STAGE"' EXIT

PKG_ROOT="$STAGE/zikr_${VERSION}_all"
mkdir -p \
  "$PKG_ROOT/DEBIAN" \
  "$PKG_ROOT/usr/bin" \
  "$PKG_ROOT/usr/lib/python3/dist-packages" \
  "$PKG_ROOT/usr/share/applications" \
  "$PKG_ROOT/usr/share/icons/hicolor/256x256/apps" \
  "$PKG_ROOT/usr/share/doc/zikr"

# --- control ---
sed "s/__VERSION__/${VERSION}/" packaging/debian/control > "$PKG_ROOT/DEBIAN/control"

cat > "$PKG_ROOT/DEBIAN/postinst" <<'POSTINST'
#!/bin/sh
set -e
if command -v gtk-update-icon-cache >/dev/null 2>&1; then
  gtk-update-icon-cache -f -t /usr/share/icons/hicolor >/dev/null 2>&1 || true
fi
if command -v update-desktop-database >/dev/null 2>&1; then
  update-desktop-database -q /usr/share/applications >/dev/null 2>&1 || true
fi
exit 0
POSTINST
chmod 755 "$PKG_ROOT/DEBIAN/postinst"

# --- python package ---
cp -R zikr "$PKG_ROOT/usr/lib/python3/dist-packages/zikr"
find "$PKG_ROOT/usr/lib/python3/dist-packages/zikr" -name "__pycache__" -exec rm -rf {} + 2>/dev/null || true
# Stamp the real version into the packaged copy only (never the repo
# source) so the running app knows its own version for the update
# checker's "you have X" comparison - was previously left hardcoded and
# went stale across several releases.
sed -i "s/^__version__ = .*/__version__ = \"${VERSION}\"/" "$PKG_ROOT/usr/lib/python3/dist-packages/zikr/__init__.py"

# --- launcher shim ---
cat > "$PKG_ROOT/usr/bin/zikr" <<'SHIM'
#!/bin/sh
exec python3 -m zikr "$@"
SHIM
chmod 755 "$PKG_ROOT/usr/bin/zikr"

# --- desktop entry + icon ---
cp packaging/zikr.desktop "$PKG_ROOT/usr/share/applications/zikr.desktop"
cp zikr/data/icon.png "$PKG_ROOT/usr/share/icons/hicolor/256x256/apps/zikr.png"

# --- docs ---
cp ../LICENSE "$PKG_ROOT/usr/share/doc/zikr/copyright" 2>/dev/null || true

DIST_DIR="../dist"
mkdir -p "$DIST_DIR"
dpkg-deb --build --root-owner-group "$PKG_ROOT" "$DIST_DIR/zikr_${VERSION}_all.deb"
echo "==> Built $DIST_DIR/zikr_${VERSION}_all.deb"
