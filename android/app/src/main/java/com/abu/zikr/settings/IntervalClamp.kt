package com.abu.zikr.settings

/**
 * Keeps min <= max, mirroring AppSettings.swift / settings.py's
 * clamp behavior: pushes the other bound along rather than silently
 * accepting an inverted range. Pure function so it's unit-testable
 * without DataStore.
 */
object IntervalClamp {
    fun onMinChanged(newMin: Int, currentMax: Int): Pair<Int, Int> {
        val max = if (newMin > currentMax) newMin else currentMax
        return newMin to max
    }

    fun onMaxChanged(newMax: Int, currentMin: Int): Pair<Int, Int> {
        val min = if (newMax < currentMin) newMax else currentMin
        return min to newMax
    }
}
