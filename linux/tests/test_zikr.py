"""Pure-logic tests for the Linux build. Run with:

    XDG_CONFIG_HOME=$(mktemp -d) python3 -m unittest discover -s linux/tests -v

(from the linux/ directory, or with linux/ on PYTHONPATH). Needs
XDG_CONFIG_HOME pointed somewhere disposable *before* zikr.settings is
imported, since it computes config paths at import time — the CI workflow
does this. Importing zikr.scheduler/zikr.app requires PyGObject (gi) and
its typelibs, same as running the app itself.
"""
import os
import tempfile
import unittest
from pathlib import Path

# Must happen before any `zikr.*` import — settings.py reads this at
# import time to compute where its config file lives.
os.environ.setdefault("XDG_CONFIG_HOME", tempfile.mkdtemp(prefix="zikr-test-config-"))

from zikr.zikr_data import ALL, random_zikr  # noqa: E402
from zikr.scheduler import pick_delay_seconds  # noqa: E402
from zikr.mic_monitor import _parse_alsa_status, _parse_pactl_source_outputs  # noqa: E402
from zikr.update_checker import compare_versions  # noqa: E402
from zikr.changelog import parse as parse_changelog  # noqa: E402


class TestZikrData(unittest.TestCase):
    def test_expected_count(self):
        # Keep in sync with Sources/ZikrReminder/Models/ZikrList.swift.
        self.assertEqual(len(ALL), 21)

    def test_ids_unique(self):
        ids = [z["id"] for z in ALL]
        self.assertEqual(len(ids), len(set(ids)), "duplicate id in zikr.json")

    def test_every_entry_has_required_fields(self):
        for z in ALL:
            for field in ("id", "arabic", "transliteration", "translation"):
                self.assertIn(field, z)
            self.assertTrue(z["arabic"].strip())
            self.assertTrue(z["transliteration"].strip())
            self.assertTrue(z["translation"].strip())

    def test_random_zikr_returns_a_real_entry(self):
        for _ in range(50):
            self.assertIn(random_zikr(), ALL)


class TestSchedulerDelay(unittest.TestCase):
    def test_delay_within_bounds(self):
        for _ in range(200):
            delay = pick_delay_seconds(20, 45)
            self.assertGreaterEqual(delay, 20 * 60)
            self.assertLessEqual(delay, 45 * 60)

    def test_degenerate_range_returns_exact_value(self):
        self.assertEqual(pick_delay_seconds(10, 10), 10 * 60)

    def test_inverted_range_does_not_raise_and_stays_sane(self):
        # UI is expected to keep min <= max, but the scheduler shouldn't
        # misbehave even if it somehow received an inverted range.
        delay = pick_delay_seconds(45, 20)
        self.assertGreaterEqual(delay, 45 * 60)

    def test_floors_below_one_minute_to_one(self):
        delay = pick_delay_seconds(0, 0)
        self.assertEqual(delay, 60)


class TestSettings(unittest.TestCase):
    def setUp(self):
        # Fresh XDG_CONFIG_HOME per test so settings.py's module-level
        # CONFIG_FILE/AUTOSTART_FILE paths (already fixed at import time)
        # still point somewhere real: we reload the module against a new
        # temp dir each time rather than relying on those constants.
        import importlib

        self.tmp = tempfile.TemporaryDirectory()
        os.environ["XDG_CONFIG_HOME"] = self.tmp.name
        import zikr.settings as settings_mod

        importlib.reload(settings_mod)
        self.settings_mod = settings_mod

    def tearDown(self):
        self.tmp.cleanup()

    def test_defaults_on_fresh_instance(self):
        s = self.settings_mod.Settings()
        self.assertTrue(s.enabled)
        self.assertEqual(s.min_interval_minutes, 20)
        self.assertEqual(s.max_interval_minutes, 45)
        self.assertEqual(s.display_style, "notification")
        self.assertTrue(s.pause_during_calls)
        self.assertEqual(s.skipped_update_version, "")

    def test_round_trips_through_disk(self):
        s = self.settings_mod.Settings()
        s.min_interval_minutes = 33
        s.display_style = "flash"

        s2 = self.settings_mod.Settings()
        self.assertEqual(s2.min_interval_minutes, 33)
        self.assertEqual(s2.display_style, "flash")

    def test_min_cannot_exceed_max(self):
        s = self.settings_mod.Settings()
        s.max_interval_minutes = 30
        s.min_interval_minutes = 50
        self.assertEqual(s.min_interval_minutes, 50)
        self.assertEqual(s.max_interval_minutes, 50, "max should be pulled up to match min")

    def test_max_cannot_go_below_min(self):
        s = self.settings_mod.Settings()
        s.min_interval_minutes = 40
        s.max_interval_minutes = 10
        self.assertEqual(s.max_interval_minutes, 10)
        self.assertEqual(s.min_interval_minutes, 10, "min should be pulled down to match max")

    def test_launch_at_login_writes_and_removes_autostart_entry(self):
        s = self.settings_mod.Settings()
        s.launch_at_login = True
        self.assertTrue(self.settings_mod.AUTOSTART_FILE.exists())
        content = self.settings_mod.AUTOSTART_FILE.read_text()
        self.assertIn("Exec=", content)
        self.assertIn("Zikr", content)

        s.launch_at_login = False
        self.assertFalse(self.settings_mod.AUTOSTART_FILE.exists())

    def test_new_instance_reads_real_autostart_state_not_stale_json(self):
        # Mirrors AppSettings.swift: launch_at_login's source of truth is
        # whether the autostart file actually exists, not the cached JSON.
        s = self.settings_mod.Settings()
        s.launch_at_login = True

        self.settings_mod.AUTOSTART_FILE.unlink()  # simulate external removal
        s2 = self.settings_mod.Settings()
        self.assertFalse(s2.launch_at_login)


class TestMicMonitorParsing(unittest.TestCase):
    def test_pactl_empty_output_means_idle(self):
        self.assertFalse(_parse_pactl_source_outputs(""))
        self.assertFalse(_parse_pactl_source_outputs("\n"))

    def test_pactl_nonempty_output_means_active(self):
        self.assertTrue(_parse_pactl_source_outputs("123\t456\tmy-app\n"))

    def test_alsa_running_state_means_active(self):
        self.assertTrue(_parse_alsa_status("state: RUNNING\nother: stuff\n"))

    def test_alsa_other_states_mean_idle(self):
        self.assertFalse(_parse_alsa_status("state: CLOSED\n"))
        self.assertFalse(_parse_alsa_status(""))


class TestVersionCompare(unittest.TestCase):
    def test_newer_is_greater(self):
        self.assertEqual(compare_versions("1.4.0", "1.3.0"), 1)

    def test_older_is_lesser(self):
        self.assertEqual(compare_versions("1.2.0", "1.3.0"), -1)

    def test_equal_versions(self):
        self.assertEqual(compare_versions("1.3.0", "1.3.0"), 0)

    def test_different_lengths_compare_correctly(self):
        self.assertEqual(compare_versions("1.4", "1.4.0"), 0)
        self.assertEqual(compare_versions("1.4.1", "1.4"), 1)


class TestChangelogParsing(unittest.TestCase):
    SAMPLE = (
        "# Changelog\n\n"
        "## [Unreleased]\n\n"
        "### Added\n"
        "- something not released\n\n"
        "## [1.4.0] — 2026-08-30\n\n"
        "### Added\n"
        "- pause during calls\n\n"
        "## [1.3.0] — 2026-08-27\n\n"
        "### Added\n"
        "- windows build\n"
    )

    def test_skips_unreleased_section(self):
        versions = [e["version"] for e in parse_changelog(self.SAMPLE)]
        self.assertNotIn("Unreleased", versions)

    def test_parses_version_and_date(self):
        entries = parse_changelog(self.SAMPLE)
        self.assertEqual(entries[0]["version"], "1.4.0")
        self.assertEqual(entries[0]["date"], "2026-08-30")
        self.assertIn("pause during calls", entries[0]["body"])

    def test_parses_all_released_versions(self):
        self.assertEqual(len(parse_changelog(self.SAMPLE)), 2)


class TestIconAndAssets(unittest.TestCase):
    def test_bundled_icon_exists(self):
        icon = Path(__file__).resolve().parent.parent / "zikr" / "data" / "icon.png"
        self.assertTrue(icon.exists(), "zikr/data/icon.png is missing")
        self.assertGreater(icon.stat().st_size, 0)


if __name__ == "__main__":
    unittest.main()
