"""Pure logic behind the Linux in-app updater (no GTK needed)."""
import unittest

from zikr.updater import INSTALLED_PREFIX, choose_method, find_deb_url, progress_percent

ASSETS = [
    {"name": "Zikr-1.11.0.zip", "browser_download_url": "https://example.com/Zikr-1.11.0.zip"},
    {"name": "zikr_1.11.0_all.deb", "browser_download_url": "https://example.com/zikr_1.11.0_all.deb"},
    {"name": "ZikrSetup-1.11.0.exe", "browser_download_url": "https://example.com/ZikrSetup-1.11.0.exe"},
]
INSTALLED = INSTALLED_PREFIX + "/updater.py"
DEB = "https://example.com/zikr_1.11.0_all.deb"


class FindDebUrlTests(unittest.TestCase):
    def test_picks_the_deb(self):
        self.assertEqual(find_deb_url(ASSETS), DEB)

    def test_none_when_missing_or_empty(self):
        self.assertIsNone(find_deb_url(ASSETS[:1]))
        self.assertIsNone(find_deb_url(None))

    def test_refuses_non_https(self):
        self.assertIsNone(find_deb_url([{"name": "zikr_1_all.deb", "browser_download_url": "http://x/zikr.deb"}]))


class ChooseMethodTests(unittest.TestCase):
    def test_installed_with_pkexec_and_apt_installs_in_place(self):
        self.assertEqual(choose_method(INSTALLED, DEB, True, True, True), "apt")

    def test_installed_without_pkexec_opens_software_installer(self):
        self.assertEqual(choose_method(INSTALLED, DEB, False, True, True), "open-deb")

    def test_source_checkout_opens_release_page(self):
        self.assertEqual(choose_method("/home/me/zikr/linux/zikr/updater.py", DEB, True, True, True), "browser")

    def test_release_without_deb_opens_release_page(self):
        self.assertEqual(choose_method(INSTALLED, None, True, True, True), "browser")

    def test_nothing_available_opens_release_page(self):
        self.assertEqual(choose_method(INSTALLED, DEB, False, False, False), "browser")


class ProgressTests(unittest.TestCase):
    def test_progress(self):
        self.assertEqual(progress_percent(10, 0), -1)
        self.assertEqual(progress_percent(50, 100), 50)
        self.assertEqual(progress_percent(150, 100), 100)


if __name__ == "__main__":
    unittest.main()
