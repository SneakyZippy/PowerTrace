package com.antigravity.battery

import com.antigravity.battery.ui.util.TimeFormatUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class TimeFormatUtilsTest {

    @Test
    fun testFormatDurationHuman() {
        assertEquals("< 1m", TimeFormatUtils.formatDurationHuman(30_000L))
        assertEquals("5m", TimeFormatUtils.formatDurationHuman(5 * 60 * 1000L))
        assertEquals("45m", TimeFormatUtils.formatDurationHuman(45 * 60 * 1000L))
        assertEquals("1h", TimeFormatUtils.formatDurationHuman(60 * 60 * 1000L))
        assertEquals("2h 52m", TimeFormatUtils.formatDurationHuman((2 * 3600 + 52 * 60) * 1000L))
        assertEquals("1d 4h", TimeFormatUtils.formatDurationHuman((28 * 3600) * 1000L))
    }

    @Test
    fun testFormatTimeRangeSameDay() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.OCTOBER, 30, 14, 0, 0)
        val start = cal.timeInMillis
        cal.set(2026, Calendar.OCTOBER, 30, 15, 30, 0)
        val end = cal.timeInMillis

        val formatted = TimeFormatUtils.formatTimeRange(start, end)
        assertEquals("14:00 → 15:30", formatted)
    }

    @Test
    fun testFormatTimeRangeCrossMidnight() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.OCTOBER, 30, 22, 45, 0)
        val start = cal.timeInMillis
        cal.set(2026, Calendar.OCTOBER, 31, 1, 37, 0)
        val end = cal.timeInMillis

        val formatted = TimeFormatUtils.formatTimeRange(start, end)
        assertEquals("22:45 → 01:37 (+1d)", formatted)
    }
}
