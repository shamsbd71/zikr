using System;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Microsoft.Win32;

namespace Zikr.Tests
{
    [TestClass]
    public class MicMonitorTests
    {
        private string _consentStoreKeyPath;

        [TestInitialize]
        public void Setup()
        {
            // A throwaway subkey under HKCU standing in for the real
            // CapabilityAccessManager\ConsentStore\microphone key, so
            // tests never read the machine's actual mic-usage history.
            _consentStoreKeyPath = @"Software\ZikrTests\ConsentStore-" + Guid.NewGuid();
        }

        [TestCleanup]
        public void Cleanup()
        {
            Registry.CurrentUser.DeleteSubKeyTree(_consentStoreKeyPath, throwOnMissingSubKey: false);
        }

        private void WriteAppEntry(string subPath, string appName, long? start, long? stop)
        {
            using (var key = Registry.CurrentUser.CreateSubKey(_consentStoreKeyPath + subPath + @"\" + appName))
            {
                if (start.HasValue) key.SetValue("LastUsedTimeStart", start.Value, RegistryValueKind.QWord);
                if (stop.HasValue) key.SetValue("LastUsedTimeStop", stop.Value, RegistryValueKind.QWord);
            }
        }

        [TestMethod]
        public void MissingConsentStoreKeyMeansNotInUse()
        {
            Assert.IsFalse(MicMonitor.IsInUse(_consentStoreKeyPath));
        }

        [TestMethod]
        public void EmptyConsentStoreKeyMeansNotInUse()
        {
            Registry.CurrentUser.CreateSubKey(_consentStoreKeyPath).Close();
            Assert.IsFalse(MicMonitor.IsInUse(_consentStoreKeyPath));
        }

        [TestMethod]
        public void PackagedAppCurrentlyCapturingMeansInUse()
        {
            WriteAppEntry("", "Microsoft.Teams", start: 132000000000000000, stop: 0);
            Assert.IsTrue(MicMonitor.IsInUse(_consentStoreKeyPath));
        }

        [TestMethod]
        public void PackagedAppThatFinishedMeansNotInUse()
        {
            WriteAppEntry("", "Microsoft.Teams", start: 132000000000000000, stop: 132000000001000000);
            Assert.IsFalse(MicMonitor.IsInUse(_consentStoreKeyPath));
        }

        [TestMethod]
        public void NonPackagedAppCurrentlyCapturingMeansInUse()
        {
            // Desktop apps (Zoom, Chrome/Google Meet, etc.) live under
            // NonPackaged, not directly under the microphone key.
            WriteAppEntry(@"\NonPackaged", "C%23Users%23me%23zoom.exe", start: 132000000000000000, stop: 0);
            Assert.IsTrue(MicMonitor.IsInUse(_consentStoreKeyPath));
        }

        [TestMethod]
        public void EntryWithNoStartTimeMeansNotInUse()
        {
            WriteAppEntry("", "SomeApp", start: null, stop: null);
            Assert.IsFalse(MicMonitor.IsInUse(_consentStoreKeyPath));
        }
    }
}
