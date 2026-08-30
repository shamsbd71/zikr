package com.abu.zikr.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class IntervalClampTest {
    @Test
    fun minCannotExceedMax() {
        val (min, max) = IntervalClamp.onMinChanged(newMin = 50, currentMax = 30)
        assertEquals(50, min)
        assertEquals(50, max)
    }

    @Test
    fun maxCannotGoBelowMin() {
        val (min, max) = IntervalClamp.onMaxChanged(newMax = 10, currentMin = 40)
        assertEquals(10, min)
        assertEquals(10, max)
    }

    @Test
    fun normalRangeUnaffected() {
        val (min, max) = IntervalClamp.onMinChanged(newMin = 15, currentMax = 45)
        assertEquals(15, min)
        assertEquals(45, max)
    }
}
