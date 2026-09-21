using System.Linq;
using Microsoft.VisualStudio.TestTools.UnitTesting;

namespace Zikr.Tests
{
    [TestClass]
    public class ZikrDataTests
    {
        [TestMethod]
        public void ExpectedCount()
        {
            // The one repo-root data/zikr.json every platform reads;
            // this project links it rather than keeping a copy.
            Assert.AreEqual(49, ZikrData.All.Count);
        }

        [TestMethod]
        public void IdsAreUnique()
        {
            var ids = ZikrData.All.Select(z => z.Id).ToList();
            Assert.AreEqual(ids.Count, ids.Distinct().Count(), "duplicate id in zikr.json");
        }

        [TestMethod]
        public void EveryEntryHasRequiredFields()
        {
            foreach (var z in ZikrData.All)
            {
                Assert.IsFalse(string.IsNullOrWhiteSpace(z.Arabic));
                Assert.IsFalse(string.IsNullOrWhiteSpace(z.Transliteration));
                Assert.IsFalse(string.IsNullOrWhiteSpace(z.Translation));
            }
        }

        [TestMethod]
        public void RandomZikrReturnsARealEntry()
        {
            for (int i = 0; i < 50; i++)
            {
                var z = ZikrData.RandomZikr();
                CollectionAssert.Contains(ZikrData.All, z);
            }
        }
    }
}
