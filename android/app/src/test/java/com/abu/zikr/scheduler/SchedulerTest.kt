package com.abu.zikr.scheduler

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SchedulerTest {
    @Test
    fun delayWithinBounds() {
        repeat(200) {
            val delay = pickDelaySeconds(20, 45)
            assertTrue(delay >= 20 * 60L)
            assertTrue(delay <= 45 * 60L)
        }
    }

    @Test
    fun degenerateRangeReturnsExactValue() {
        assertEquals(10 * 60L, pickDelaySeconds(10, 10))
    }

    @Test
    fun invertedRangeDoesNotThrowAndStaysSane() {
        val delay = pickDelaySeconds(45, 20)
        assertTrue(delay >= 45 * 60L)
    }

    @Test
    fun floorsBelowOneMinuteToOne() {
        assertEquals(60L, pickDelaySeconds(0, 0))
    }
}
