using Microsoft.VisualStudio.TestTools.UnitTesting;

namespace Zikr.Tests
{
    [TestClass]
    public class SchedulerTests
    {
        [TestMethod]
        public void DelayWithinBounds()
        {
            for (int i = 0; i < 200; i++)
            {
                double delay = Scheduler.PickDelaySeconds(20, 45);
                Assert.IsTrue(delay >= 20 * 60, $"delay {delay} below min");
                Assert.IsTrue(delay <= 45 * 60, $"delay {delay} above max");
            }
        }

        [TestMethod]
        public void DegenerateRangeReturnsExactValue()
        {
            Assert.AreEqual(10 * 60, Scheduler.PickDelaySeconds(10, 10));
        }

        [TestMethod]
        public void InvertedRangeDoesNotThrowAndStaysSane()
        {
            double delay = Scheduler.PickDelaySeconds(45, 20);
            Assert.IsTrue(delay >= 45 * 60);
        }

        [TestMethod]
        public void FloorsBelowOneMinuteToOne()
        {
            Assert.AreEqual(60, Scheduler.PickDelaySeconds(0, 0));
        }

        [TestMethod]
        public void QuietHoursWithinNormalRange()
        {
            // 13:00 is inside 09:00-17:00.
            Assert.IsTrue(Scheduler.IsWithinQuietHours(13 * 60, 9 * 60, 17 * 60));
            Assert.IsFalse(Scheduler.IsWithinQuietHours(8 * 60, 9 * 60, 17 * 60));
            Assert.IsFalse(Scheduler.IsWithinQuietHours(17 * 60, 9 * 60, 17 * 60));
        }

        [TestMethod]
        public void QuietHoursWrapsPastMidnight()
        {
            // 22:00-06:00 window.
            Assert.IsTrue(Scheduler.IsWithinQuietHours(23 * 60, 22 * 60, 6 * 60));
            Assert.IsTrue(Scheduler.IsWithinQuietHours(1 * 60, 22 * 60, 6 * 60));
            Assert.IsFalse(Scheduler.IsWithinQuietHours(12 * 60, 22 * 60, 6 * 60));
            Assert.IsFalse(Scheduler.IsWithinQuietHours(6 * 60, 22 * 60, 6 * 60));
        }

        [TestMethod]
        public void QuietHoursEqualBoundsMeansNoWindow()
        {
            Assert.IsFalse(Scheduler.IsWithinQuietHours(10 * 60, 10 * 60, 10 * 60));
        }
    }
}
