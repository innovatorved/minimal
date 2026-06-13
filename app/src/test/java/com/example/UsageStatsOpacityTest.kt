package com.example

import org.junit.Assert.assertEquals
import org.junit.Test

class UsageStatsOpacityTest {

    private fun opacityForOpenCount(openCount: Int, maxOpenCount: Int): Float {
        if (maxOpenCount <= 0) return 1f
        val ratio = openCount.toFloat() / maxOpenCount.toFloat()
        return (0.4f + ratio * 0.6f).coerceIn(0.4f, 1f)
    }

    @Test
    fun opacityAtMaxIsFull() {
        assertEquals(1f, opacityForOpenCount(10, 10), 0.01f)
    }

    @Test
    fun opacityAtZeroIsMinimum() {
        assertEquals(0.4f, opacityForOpenCount(0, 10), 0.01f)
    }

    @Test
    fun opacityAtHalfIsMidRange() {
        assertEquals(0.7f, opacityForOpenCount(5, 10), 0.01f)
    }
}
