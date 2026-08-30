using System.Linq;
using Microsoft.VisualStudio.TestTools.UnitTesting;

namespace Zikr.Tests
{
    [TestClass]
    public class ChangelogTests
    {
        private const string Sample =
            "# Changelog\n\n" +
            "## [Unreleased]\n\n" +
            "### Added\n" +
            "- something not released\n\n" +
            "## [1.4.0] — 2026-08-30\n\n" +
            "### Added\n" +
            "- pause during calls\n\n" +
            "## [1.3.0] — 2026-08-27\n\n" +
            "### Added\n" +
            "- windows build\n";

        [TestMethod]
        public void SkipsUnreleasedSection()
        {
            var entries = Changelog.Parse(Sample);
            Assert.IsFalse(entries.Any(e => e.Version == "Unreleased"));
        }

        [TestMethod]
        public void ParsesVersionAndDate()
        {
            var entries = Changelog.Parse(Sample);
            Assert.AreEqual("1.4.0", entries[0].Version);
            Assert.AreEqual("2026-08-30", entries[0].Date);
            StringAssert.Contains(entries[0].Body, "pause during calls");
        }

        [TestMethod]
        public void ParsesAllReleasedVersions()
        {
            Assert.AreEqual(2, Changelog.Parse(Sample).Count);
        }
    }
}
