using System;
using System.IO;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Microsoft.Win32;

namespace Zikr.Tests
{
    [TestClass]
    public class SettingsTests
    {
        private string _configDir;
        private string _runKeyPath;

        [TestInitialize]
        public void Setup()
        {
            _configDir = Path.Combine(Path.GetTempPath(), "zikr-test-" + Guid.NewGuid());
            // A throwaway subkey under HKCU - never the real Run key -
            // so tests can't accidentally make the test runner autostart
            // anything, and don't touch the real Zikr install's entry.
            _runKeyPath = @"Software\ZikrTests\" + Guid.NewGuid();
        }

        [TestCleanup]
        public void Cleanup()
        {
            if (Directory.Exists(_configDir)) Directory.Delete(_configDir, recursive: true);
            Registry.CurrentUser.DeleteSubKeyTree(_runKeyPath, throwOnMissingSubKey: false);
        }

        private Settings New() => new Settings(_configDir, _runKeyPath);

        [TestMethod]
        public void DefaultsOnFreshInstance()
        {
            var s = New();
            Assert.IsTrue(s.Enabled);
            Assert.AreEqual(20, s.MinIntervalMinutes);
            Assert.AreEqual(45, s.MaxIntervalMinutes);
            Assert.AreEqual("notification", s.DisplayStyle);
        }

        [TestMethod]
        public void RoundTripsThroughDisk()
        {
            var s = New();
            s.SetMinInterval(33);
            s.DisplayStyle = "flash";
            s.Save();

            var s2 = New();
            Assert.AreEqual(33, s2.MinIntervalMinutes);
            Assert.AreEqual("flash", s2.DisplayStyle);
        }

        [TestMethod]
        public void MinCannotExceedMax()
        {
            var s = New();
            s.SetMaxInterval(30);
            s.SetMinInterval(50);
            Assert.AreEqual(50, s.MinIntervalMinutes);
            Assert.AreEqual(50, s.MaxIntervalMinutes, "max should be pulled up to match min");
        }

        [TestMethod]
        public void MaxCannotGoBelowMin()
        {
            var s = New();
            s.SetMinInterval(40);
            s.SetMaxInterval(10);
            Assert.AreEqual(10, s.MaxIntervalMinutes);
            Assert.AreEqual(10, s.MinIntervalMinutes, "min should be pulled down to match max");
        }

        [TestMethod]
        public void LaunchAtLoginWritesAndRemovesRegistryValue()
        {
            var s = New();
            s.LaunchAtLogin = true;

            using (var key = Registry.CurrentUser.OpenSubKey(_runKeyPath))
            {
                Assert.IsNotNull(key);
                Assert.IsNotNull(key.GetValue("Zikr"));
            }

            s.LaunchAtLogin = false;

            using (var key = Registry.CurrentUser.OpenSubKey(_runKeyPath))
            {
                Assert.IsNull(key?.GetValue("Zikr"));
            }
        }

        [TestMethod]
        public void NewInstanceReadsRealAutostartStateNotStaleJson()
        {
            // Mirrors AppSettings.swift / settings.py: LaunchAtLogin's
            // source of truth is whether the registry value actually
            // exists, not the cached JSON.
            var s = New();
            s.LaunchAtLogin = true;

            using (var key = Registry.CurrentUser.OpenSubKey(_runKeyPath, writable: true))
            {
                key.DeleteValue("Zikr", throwOnMissingValue: false); // simulate external removal
            }

            var s2 = New();
            Assert.IsFalse(s2.LaunchAtLogin);
        }
    }
}
