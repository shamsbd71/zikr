"""Fetches and parses CHANGELOG.md straight from the repo's main branch,
mirroring ChangelogFetcher.swift, so "What's New" and the update dialog
show the same text as the file in the repo.
"""
import urllib.request

RAW_URL = "https://raw.githubusercontent.com/shamsbd71/zikr/main/CHANGELOG.md"


def parse(markdown_text):
    """Pure function: splits on '## [version] — date' headers."""
    entries = []
    current_version = None
    current_date = ""
    current_body = []

    def flush():
        if current_version and current_version.lower() != "unreleased":
            entries.append({
                "version": current_version,
                "date": current_date,
                "body": "\n".join(current_body).strip(),
            })

    for line in markdown_text.split("\n"):
        if line.startswith("## ["):
            flush()
            current_body = []
            after_bracket = line[4:]
            if "]" in after_bracket:
                close = after_bracket.index("]")
                current_version = after_bracket[:close]
                rest = after_bracket[close + 1:]
                current_date = rest.replace("—", "").strip()
            else:
                current_version = None
        elif current_version is not None:
            current_body.append(line)
    flush()
    return entries


def fetch(timeout=5):
    try:
        with urllib.request.urlopen(RAW_URL, timeout=timeout) as resp:
            text = resp.read().decode("utf-8")
    except Exception:
        return []
    return parse(text)
