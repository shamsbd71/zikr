#!/usr/bin/env python3
"""Point a fork of Zikr at its own GitHub repo.

Every platform's updater, "What's New" viewer, installer metadata and the
website link to the upstream repo (shamsbd71/zikr). A fork that publishes
its own releases needs those to point at the fork instead, or its users
will be offered upstream's releases (and macOS will install them).

    python3 tools/fork_setup.py yourname/zikr            # rewrite files
    python3 tools/fork_setup.py yourname/zikr --dry-run  # only list changes

What it changes:
  * shamsbd71.github.io/zikr -> <owner>.github.io/<repo>   (site URLs)
  * shamsbd71/zikr           -> <owner>/<repo>             (API, raw, release URLs)
  * remaining "shamsbd71"    -> <owner>                    (publisher / maintainer)
  * clears the Cloudflare Web Analytics token in docs/index.html, so your
    site's visitors aren't counted in upstream's dashboard

What it deliberately leaves alone:
  * LICENSE - MIT requires keeping the original copyright notice
  * CHANGELOG.md - its history links point at releases that really are upstream's
  * bundle / application ids (com.abu.*) - see FORKING.md for when to change them

Run it once, review `git diff`, commit.
"""
from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

UPSTREAM_OWNER = "shamsbd71"
UPSTREAM_REPO = "zikr"

ROOT = Path(__file__).resolve().parents[1]
SKIP_DIRS = {".git", ".build", "dist", "build", ".gradle", "audio", "__pycache__",
             ".venv", "node_modules", "bin", "obj"}
SKIP_FILES = {"LICENSE", "CHANGELOG.md", "FORKING.md", "fork_setup.py"}
TEXT_SUFFIXES = {".swift", ".py", ".cs", ".kt", ".kts", ".md", ".html", ".iss",
                 ".yml", ".yaml", ".sh", ".toml", ".json", ".xml", ".txt", ""}

CF_TOKEN_RE = re.compile(r'(var CF_BEACON_TOKEN = ")[0-9a-fA-F]{32}(";)')


def candidate_files() -> list[Path]:
    out = []
    for p in ROOT.rglob("*"):
        if not p.is_file() or p.name in SKIP_FILES:
            continue
        if any(part in SKIP_DIRS for part in p.relative_to(ROOT).parts):
            continue
        if p.suffix not in TEXT_SUFFIXES and p.name != "control":
            continue
        out.append(p)
    return sorted(out)


def rewrite(text: str, owner: str, repo: str, path: Path) -> str:
    new = text.replace(f"{UPSTREAM_OWNER}.github.io/{UPSTREAM_REPO}", f"{owner}.github.io/{repo}")
    new = new.replace(f"{UPSTREAM_OWNER}/{UPSTREAM_REPO}", f"{owner}/{repo}")
    new = new.replace(UPSTREAM_OWNER, owner)
    if path.name == "index.html" and path.parent.name == "docs":
        new = CF_TOKEN_RE.sub(r'\1\2', new)
    return new


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("slug", help="your fork as owner/repo, e.g. yourname/zikr")
    ap.add_argument("--dry-run", action="store_true", help="list files that would change, write nothing")
    args = ap.parse_args()

    if not re.fullmatch(r"[A-Za-z0-9-]+/[A-Za-z0-9._-]+", args.slug):
        ap.error("slug must look like owner/repo")
    owner, repo = args.slug.split("/")
    if (owner, repo) == (UPSTREAM_OWNER, UPSTREAM_REPO):
        ap.error("that is the upstream repo; pass your fork's owner/repo")

    changed = []
    for path in candidate_files():
        try:
            text = path.read_text(encoding="utf-8")
        except UnicodeDecodeError:
            continue
        new = rewrite(text, owner, repo, path)
        if new != text:
            changed.append(path.relative_to(ROOT))
            if not args.dry_run:
                path.write_text(new, encoding="utf-8")

    verb = "would change" if args.dry_run else "changed"
    print(f"{verb} {len(changed)} file(s):")
    for p in changed:
        print(f"  {p}")
    if not args.dry_run and changed:
        print("\nReview with `git diff`, then commit. Next steps are in FORKING.md.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
