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
    }
}
