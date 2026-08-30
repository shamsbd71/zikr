"""Checks GitHub Releases for a newer tag than the running version. No
in-place self-update, same decision as Windows: installs vary too much
across distros (.deb, a future AppImage/flatpak, a manual tarball) for
one safe overwrite strategy. "Check for Updates" surfaces what changed
and opens the release page for you to download and reinstall.
"""
import json
import urllib.request

API_URL = "https://api.github.com/repos/shamsbd71/zikr/releases/latest"
RELEASES_URL = "https://github.com/shamsbd71/zikr/releases/latest"


def compare_versions(a, b):
    """-1 if a<b, 0 if equal, 1 if a>b, comparing dot-separated numeric
    components (mirrors the macOS/Windows builds' numeric compare)."""
    def parts(v):
        return [int(p) if p.isdigit() else 0 for p in v.split(".")]

    pa, pb = parts(a), parts(b)
    n = max(len(pa), len(pb))
    pa += [0] * (n - len(pa))
    pb += [0] * (n - len(pb))
    if pa < pb:
        return -1
    if pa > pb:
        return 1
    return 0


def check_for_update(current_version, timeout=5):
    """Returns {"status": "available", "version": "1.5.0"} |
    {"status": "up_to_date"} | {"status": "error", "message": "..."}."""
    try:
        req = urllib.request.Request(API_URL, headers={"Accept": "application/vnd.github+json"})
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            data = json.loads(resp.read().decode("utf-8"))
    except Exception:
        return {"status": "error", "message": "Couldn't check for updates. Try again later."}

    tag = data.get("tag_name", "")
    latest = tag[1:] if tag.startswith("v") else tag
    if not latest:
        return {"status": "error", "message": "Couldn't check for updates. Try again later."}

    if compare_versions(latest, current_version) > 0:
        return {"status": "available", "version": latest}
    return {"status": "up_to_date"}
