using Microsoft.VisualStudio.TestTools.UnitTesting;

namespace Zikr.Tests
{
    [TestClass]
    public class UpdateCheckerTests
    {
        [TestMethod]
        public void NewerIsGreater()
        {
            Assert.AreEqual(1, UpdateChecker.CompareVersions("1.4.0", "1.3.0"));
        }

        [TestMethod]
        public void OlderIsLesser()
        {
            Assert.AreEqual(-1, UpdateChecker.CompareVersions("1.2.0", "1.3.0"));
        }

        [TestMethod]
        public void EqualVersions()
        {
            Assert.AreEqual(0, UpdateChecker.CompareVersions("1.3.0", "1.3.0"));
        }

        [TestMethod]
        public void DifferentLengthsCompareCorrectly()
        {
            Assert.AreEqual(0, UpdateChecker.CompareVersions("1.4", "1.4.0"));
            Assert.AreEqual(1, UpdateChecker.CompareVersions("1.4.1", "1.4"));
        }
    }
}
