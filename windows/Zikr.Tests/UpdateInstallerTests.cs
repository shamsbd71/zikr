using System.Collections.Generic;
using Microsoft.VisualStudio.TestTools.UnitTesting;

namespace Zikr.Tests
{
    [TestClass]
    public class UpdateInstallerTests
    {
        private static Dictionary<string, object> Asset(string name, string url) =>
            new Dictionary<string, object> { { "name", name }, { "browser_download_url", url } };

        [TestMethod]
        public void PicksWindowsInstallerAmongOtherAssets()
        {
            var assets = new System.Collections.ArrayList
            {
                Asset("Zikr-1.11.0.zip", "https://example.com/Zikr-1.11.0.zip"),
                Asset("zikr_1.11.0_all.deb", "https://example.com/zikr_1.11.0_all.deb"),
                Asset("ZikrSetup-1.11.0.exe", "https://example.com/ZikrSetup-1.11.0.exe"),
                Asset("Zikr-1.11.0.apk", "https://example.com/Zikr-1.11.0.apk"),
            };
            Assert.AreEqual("https://example.com/ZikrSetup-1.11.0.exe", UpdateChecker.FindInstallerUrl(assets));
        }

        [TestMethod]
        public void NoInstallerMeansNull()
        {
            var assets = new System.Collections.ArrayList { Asset("Zikr-1.11.0.zip", "https://example.com/a.zip") };
            Assert.IsNull(UpdateChecker.FindInstallerUrl(assets));
            Assert.IsNull(UpdateChecker.FindInstallerUrl(null));
        }

        [TestMethod]
        public void RefusesNonHttpsInstallerUrl()
        {
            var assets = new System.Collections.ArrayList { Asset("ZikrSetup-1.11.0.exe", "http://example.com/ZikrSetup.exe") };
            Assert.IsNull(UpdateChecker.FindInstallerUrl(assets));
        }

        [TestMethod]
        public void ProgressPercent()
        {
            Assert.AreEqual(-1, UpdateInstaller.ProgressPercent(10, -1));
            Assert.AreEqual(0, UpdateInstaller.ProgressPercent(0, 200));
            Assert.AreEqual(50, UpdateInstaller.ProgressPercent(100, 200));
            Assert.AreEqual(100, UpdateInstaller.ProgressPercent(300, 200));
        }

        [TestMethod]
        public void InstallerRunsSilentlyAndClosesRunningZikr()
        {
            StringAssert.Contains(UpdateInstaller.SilentArgs, "/VERYSILENT");
            StringAssert.Contains(UpdateInstaller.SilentArgs, "/CLOSEAPPLICATIONS");
        }
    }
}
