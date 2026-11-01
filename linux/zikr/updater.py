"""Downloads a release's .deb and installs it through the system package
manager, then restarts Zikr. Zikr never overwrites its own files: the
.deb goes through apt (with a polkit password prompt via pkexec), exactly
like a manual install.

Which path is used depends on how Zikr is running:
  * installed from the .deb, with pkexec + apt-get   -> install in place
  * installed from the .deb, no pkexec               -> open the .deb in
    the desktop's software installer
  * running from a source checkout / anything else   -> open the release
    page (there is no package to upgrade)

The decision functions are pure so they're unit-testable without GTK.
"""
import os
import shutil
import subprocess
import urllib.request
from pathlib import Path

# Where build_deb.sh puts the package; only a copy running from here was
# installed by the .deb and can be upgraded by apt.
INSTALLED_PREFIX = "/usr/lib/python3/dist-packages/zikr"
_PKG_FILE = Path(__file__).resolve()


def find_deb_url(assets):
    """The release's Debian package URL from GitHub's asset list, or None."""
    for asset in assets or []:
        name = asset.get("name", "")
        url = asset.get("browser_download_url", "")
        if name.startswith("zikr_") and name.endswith(".deb") and url.startswith("https://"):
            return url
    return None


def choose_method(module_path, deb_url, has_pkexec, has_apt, has_xdg_open):
    """'apt' | 'open-deb' | 'browser'. Pure."""
    installed = str(module_path).startswith(INSTALLED_PREFIX + "/")
    if not installed or not deb_url:
        return "browser"
    if has_pkexec and has_apt:
        return "apt"
    if has_xdg_open:
        return "open-deb"
    return "browser"


def install_method(deb_url):
    return choose_method(
        _PKG_FILE, deb_url,
        has_pkexec=shutil.which("pkexec") is not None,
        has_apt=shutil.which("apt-get") is not None,
        has_xdg_open=shutil.which("xdg-open") is not None,
    )


def progress_percent(read, total):
    """0-100 when the total is known, -1 otherwise."""
    if not total or total <= 0:
        return -1
    return max(0, min(100, read * 100 // total))


def download(url, version, on_progress=None, timeout=30):
    """Blocking; call from a worker thread. Returns the .deb path."""
    cache = Path(os.environ.get("XDG_CACHE_HOME", Path.home() / ".cache")) / "zikr" / "updates"
    cache.mkdir(parents=True, exist_ok=True)
    dest = cache / f"zikr_{version}_all.deb"
    partial = dest.with_suffix(".deb.part")

    req = urllib.request.Request(url, headers={"User-Agent": "Zikr-Linux-Updater"})
    with urllib.request.urlopen(req, timeout=timeout) as resp, open(partial, "wb") as out:
        total = int(resp.headers.get("Content-Length") or 0)
        read, last = 0, -2
        while True:
            chunk = resp.read(64 * 1024)
            if not chunk:
                break
            out.write(chunk)
            read += len(chunk)
            pct = progress_percent(read, total)
            if on_progress and pct != last:
                last = pct
                on_progress(pct)
    if total and read != total:
        raise IOError("Download incomplete")
    partial.replace(dest)
    return dest


def install_deb_with_apt(deb_path):
    """Runs apt through pkexec (shows the desktop's password prompt).
    Returns True on success. Blocking."""
    result = subprocess.run(
        ["pkexec", "apt-get", "install", "-y", "--allow-downgrades", str(deb_path)],
        stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
    )
    return result.returncode == 0


def open_with_desktop(target):
    subprocess.Popen(["xdg-open", str(target)])


def relaunch():
    """Start the freshly installed version in its own session; the caller
    quits this one right after."""
    subprocess.Popen(["/usr/bin/zikr"], start_new_session=True,
                     stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
