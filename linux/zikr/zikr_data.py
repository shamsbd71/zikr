"""Loads the bundled zikr list.

Reads the same data/zikr.json every other platform reads - installed to
/usr/share/zikr/zikr.json by build_deb.sh, or straight out of the repo
when running from a checkout. There is deliberately no copy inside this
package: four hand-synced copies is exactly how the list drifted to 21
stale entries while the canonical file had 49.
"""
import json
import random
from pathlib import Path

_DATA_PATHS = [
    Path("/usr/share/zikr/zikr.json"),                       # installed
    Path(__file__).resolve().parents[2] / "data" / "zikr.json",  # checkout
]


def _load():
    for path in _DATA_PATHS:
        if path.exists():
            return json.loads(path.read_text(encoding="utf-8"))
    raise FileNotFoundError("zikr.json not found in any known location")


def reminders(entries):
    """Entries marked `greeting` are only ever the unlock greeting on
    macOS/Android, never a random reminder - and this build has no
    unlock greeting, so it never says them."""
    return [z for z in entries if not z.get("greeting")]


ALL = reminders(_load())


def random_zikr():
    return random.choice(ALL)
