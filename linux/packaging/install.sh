#!/bin/bash
# User-local install for distros without a .deb (Fedora, Arch, openSUSE,
# etc.) — no root needed for the app itself, though your system's GTK/
# notification/speech packages must be installed separately (see below).
set -euo pipefail
cd "$(dirname "$0")/.."   # -> linux/

DEST="${HOME}/.local/share/zikr-app"
BIN="${HOME}/.local/bin"
APPS="${HOME}/.local/share/applications"
ICONS="${HOME}/.local/share/icons/hicolor/256x256/apps"

echo "==> Installing Zikr to ${DEST}"
rm -rf "$DEST"
mkdir -p "$DEST" "$BIN" "$APPS" "$ICONS"
cp -R zikr "$DEST/zikr"
find "$DEST" -name "__pycache__" -exec rm -rf {} + 2>/dev/null || true

cat > "$BIN/zikr" <<SHIM
#!/bin/sh
exec env PYTHONPATH="${DEST}:\${PYTHONPATH:-}" python3 -m zikr "\$@"
SHIM
chmod 755 "$BIN/zikr"

sed "s|Exec=zikr|Exec=${BIN}/zikr|" packaging/zikr.desktop > "$APPS/zikr.desktop"
cp zikr/data/icon.png "$ICONS/zikr.png"

cat <<EOF

==> Installed. Make sure these are on your system (one-time, via your
    distro's package manager):

    Fedora:   sudo dnf install python3-gobject gtk3 libappindicator-gtk3 speech-dispatcher
    Arch:     sudo pacman -S python-gobject gtk3 libayatana-appindicator speech-dispatcher
    openSUSE: sudo zypper install python3-gobject gtk3 libappindicator3-1 speech-dispatcher

If ${BIN} isn't on your PATH, add:
    export PATH="\$HOME/.local/bin:\$PATH"
to your shell profile.

Run with: zikr
EOF
