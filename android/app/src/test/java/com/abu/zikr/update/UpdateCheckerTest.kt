package com.abu.zikr.update

import org.junit.Assert.assertEquals
import org.junit.Test

class UpdateCheckerTest {
    @Test
    fun newerIsGreater() {
        assertEquals(1, UpdateChecker.compareVersions("1.7.0", "1.6.1"))
    }

    @Test
    fun olderIsLesser() {
        assertEquals(-1, UpdateChecker.compareVersions("1.2.0", "1.3.0"))
    }

    @Test
    fun equalVersions() {
        assertEquals(0, UpdateChecker.compareVersions("1.3.0", "1.3.0"))
    }

    @Test
    fun differentLengthsCompareCorrectly() {
        assertEquals(0, UpdateChecker.compareVersions("1.4", "1.4.0"))
        assertEquals(1, UpdateChecker.compareVersions("1.4.1", "1.4"))
    }

    @Test
    fun nonNumericSuffixDoesNotThrow() {
        // "-test"/"-dev" style versions from CI test builds shouldn't crash the comparison.
        assertEquals(0, UpdateChecker.compareVersions("0.0.0-test", "0.0.0-test"))
    }
}
