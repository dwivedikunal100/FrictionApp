package com.friction.app

import com.friction.app.data.repository.DailyStat
import org.junit.Assert.*
import org.junit.Test

class DailyStatTest {

    @Test
    fun `construction and property access`() {
        val stat = DailyStat(daysAgo = 3, openCount = 12, timeSavedMs = 45000L)
        assertEquals(3, stat.daysAgo)
        assertEquals(12, stat.openCount)
        assertEquals(45000L, stat.timeSavedMs)
    }

    @Test
    fun `equality between identical instances`() {
        val stat1 = DailyStat(daysAgo = 0, openCount = 5, timeSavedMs = 1000L)
        val stat2 = DailyStat(daysAgo = 0, openCount = 5, timeSavedMs = 1000L)
        assertEquals(stat1, stat2)
    }

    @Test
    fun `inequality with different values`() {
        val stat1 = DailyStat(daysAgo = 0, openCount = 5, timeSavedMs = 1000L)
        val stat2 = DailyStat(daysAgo = 1, openCount = 5, timeSavedMs = 1000L)
        assertNotEquals(stat1, stat2)
    }

    @Test
    fun `copy preserves unchanged fields`() {
        val stat = DailyStat(daysAgo = 2, openCount = 10, timeSavedMs = 5000L)
        val copied = stat.copy(openCount = 20)
        assertEquals(2, copied.daysAgo)
        assertEquals(20, copied.openCount)
        assertEquals(5000L, copied.timeSavedMs)
    }

    @Test
    fun `zero values are valid`() {
        val stat = DailyStat(daysAgo = 0, openCount = 0, timeSavedMs = 0L)
        assertEquals(0, stat.openCount)
        assertEquals(0L, stat.timeSavedMs)
    }
}
