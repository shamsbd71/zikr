#!/usr/bin/env python3
"""Report what Zikr can already measure without collecting anything.

Everything here comes from GitHub's own numbers, fetched with the `gh`
CLI's existing auth. No code ships to a user, no request leaves their
machine, and nothing here can identify anyone - which matters, because
the site promises in three languages that the app has "no analytics,
telemetry, or account system", and a release download count is a fact
GitHub records whether or not this script exists.

What it can and cannot tell you:

  downloads   Exact, per asset, for all time. The closest thing to an
              install count. An update that self-installs on macOS also
              counts here, so it is downloads, not people.
  traffic     Repo page views and clones. GitHub keeps only 14 days and
              exposes no history, so this is a snapshot: run it on a
              schedule if you want a series.

It deliberately says nothing about how often anyone actually uses the
app. That number does not exist anywhere, by design.

Usage:  python3 tools/stats.py [--json]
"""
import argparse
import json
import subprocess
import sys
from collections import defaultdict

REPO = "shamsbd71/zikr"

# Which platform an asset belongs to, by how release.yml names it.
PLATFORMS = [
    (".apk", "Android"),
    (".deb", "Linux"),
    (".exe", "Windows"),
    (".zip", "macOS"),
]


def gh(path, jq=None):
    cmd = ["gh", "api", path]
    if jq:
        cmd += ["--jq", jq]
    out = subprocess.run(cmd, capture_output=True, text=True)
    if out.returncode != 0:
        raise RuntimeError(out.stderr.strip() or f"gh api {path} failed")
    return out.stdout


def platform_of(name):
    for suffix, label in PLATFORMS:
        if name.endswith(suffix):
            return label
    return "other"


def collect():
    releases = json.loads(gh(f"repos/{REPO}/releases"))
    per_platform = defaultdict(int)
    per_release = []
    for rel in releases:
        assets = [
            {"name": a["name"], "downloads": a["download_count"],
             "platform": platform_of(a["name"])}
            for a in rel.get("assets", [])
        ]
        for a in assets:
            per_platform[a["platform"]] += a["downloads"]
        per_release.append({
            "tag": rel["tag_name"],
            "published": (rel.get("published_at") or "")[:10],
            "total": sum(a["downloads"] for a in assets),
            "assets": assets,
        })

    traffic = {}
    for kind in ("views", "clones"):
        try:
            d = json.loads(gh(f"repos/{REPO}/traffic/{kind}"))
            traffic[kind] = {"total": d.get("count", 0), "uniques": d.get("uniques", 0)}
        except RuntimeError as exc:
            # Needs push access; a read-only token simply cannot see it.
            traffic[kind] = {"error": str(exc)}

    return {
        "repo": REPO,
        "downloads_total": sum(per_platform.values()),
        "downloads_by_platform": dict(per_platform),
        "releases": per_release,
        "traffic_14d": traffic,
    }


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--json", action="store_true", help="machine-readable output")
    args = ap.parse_args()

    try:
        data = collect()
    except (RuntimeError, FileNotFoundError) as exc:
        print(f"could not read GitHub stats: {exc}", file=sys.stderr)
        print("is the `gh` CLI installed and authenticated? try: gh auth status", file=sys.stderr)
        return 1

    if args.json:
        print(json.dumps(data, indent=2))
        return 0

    print(f"\n  {data['repo']}\n")
    print(f"  DOWNLOADS — {data['downloads_total']} all time")
    by = data["downloads_by_platform"]
    width = max((v for v in by.values()), default=1) or 1
    for label in ("macOS", "Linux", "Windows", "Android", "other"):
        if label not in by:
            continue
        n = by[label]
        bar = "#" * max(1, round(n / width * 28)) if n else ""
        print(f"    {label:<9} {n:>5}  {bar}")

    print(f"\n  BY RELEASE")
    for rel in data["releases"]:
        if not rel["assets"]:
            continue
        print(f"    {rel['tag']:<9} {rel['published']}  {rel['total']:>4} total")
        for a in sorted(rel["assets"], key=lambda x: -x["downloads"]):
            print(f"        {a['platform']:<8} {a['downloads']:>4}  {a['name']}")

    print(f"\n  REPO TRAFFIC — last 14 days (GitHub keeps no more)")
    for kind, d in data["traffic_14d"].items():
        if "error" in d:
            print(f"    {kind:<8} unavailable ({d['error'][:48]})")
        else:
            print(f"    {kind:<8} {d['total']:>4} total, {d['uniques']:>3} unique")

    print("\n  Site visitors are not here: GitHub Pages serves static files")
    print("  and reports nothing back. That needs a decision - see the")
    print("  notes in docs/superpowers/specs on measurement.\n")
    return 0


if __name__ == "__main__":
    sys.exit(main())
