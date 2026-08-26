"""Loads the bundled zikr list. Same 21 general adhkar as the macOS build's
ZikrList.swift (data/zikr.json is kept in sync with it by hand)."""
import json
import random
from pathlib import Path

_DATA_PATHS = [
    Path(__file__).resolve().parent / "data" / "zikr.json",
    Path("/usr/share/zikr/zikr.json"),
]


def _load():
    for path in _DATA_PATHS:
        if path.exists():
            return json.loads(path.read_text(encoding="utf-8"))
    raise FileNotFoundError("zikr.json not found in any known location")


ALL = _load()


def random_zikr():
    return random.choice(ALL)
