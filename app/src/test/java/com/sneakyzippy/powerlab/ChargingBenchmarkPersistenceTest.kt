package com.sneakyzippy.powerlab

import com.sneakyzippy.powerlab.core.model.ChargeTestSession
import com.sneakyzippy.powerlab.service.ChargingNotificationHelper
import com.sneakyzippy.powerlab.service.ContinuousMonitoringService
import com.sneakyzippy.powerlab.ui.util.TimeFormatUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ChargingBenchmarkPersistenceTest {

    @Test
    fun testOvernightChargeDurationCalculation() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.OCTOBER, 30, 22, 45, 0)
        val startTimeMs = cal.timeInMillis

        cal.set(2026, Calendar.OCTOBER, 31, 1, 37, 0)
        val endTimeMs = cal.timeInMillis

        val expectedDurationMs = ((2 * 60 + 52) * 60 * 1000L) // 2h 52m = 10,320,000 ms

        val session = ChargeTestSession(
            id = "test-overnight",
            brickName = "Anker 65W GaN",
            cableName = "Braided 100W Cable",
            startTimeMs = startTimeMs,
            endTimeMs = endTimeMs,
            startBatteryLevel = 15,
            endBatteryLevel = 100,
            peakWattage = 45.2f,
            avgWattage = 28.5f,
            minVoltageMv = 4000,
            maxVoltageMv = 4400,
            peakCurrentMa = 9000,
            startTempDeciC = 250,
            maxTempDeciC = 345,
            isThrottled = false
        )

        assertEquals(expectedDurationMs, session.durationMs)
        assertEquals(85, session.batteryGained)

        // Time range and duration human formatting
        val timeRange = TimeFormatUtils.formatTimeRange(session.startTimeMs, session.endTimeMs)
        val durationFormatted = TimeFormatUtils.formatDurationHuman(session.durationMs)

        assertEquals("22:45 → 01:37 (+1d)", timeRange)
        assertEquals("2h 52m", durationFormatted)
    }

    @Test
    fun testAccessoryCombinationsGrouping() {
        val s1 = ChargeTestSession(
            id = "s1",
            brickName = "Anker 65W",
            cableName = "Cable A (100W)",
            startTimeMs = 1000000L,
            endTimeMs = 2000000L,
            startBatteryLevel = 10,
            endBatteryLevel = 50,
            peakWattage = 45.0f,
            avgWattage = 32.0f,
            minVoltageMv = 4000,
            maxVoltageMv = 4300,
            peakCurrentMa = 8000,
            startTempDeciC = 250,
            maxTempDeciC = 340
        )

        val s2 = ChargeTestSession(
            id = "s2",
            brickName = "Anker 65W",
            cableName = "Cable B (60W)",
            startTimeMs = 3000000L,
            endTimeMs = 4000000L,
            startBatteryLevel = 20,
            endBatteryLevel = 45,
            peakWattage = 27.5f,
            avgWattage = 22.0f,
            minVoltageMv = 4000,
            maxVoltageMv = 4200,
            peakCurrentMa = 5000,
            startTempDeciC = 250,
            maxTempDeciC = 310
        )

        val sessions = listOf(s1, s2)

        // Group by charger combinations
        val combinations = sessions.groupBy { it.cableName }.map { (cable, list) ->
            val best = list.maxByOrNull { it.peakWattage }!!
            cable to best.peakWattage
        }.sortedByDescending { it.second }

        assertEquals(2, combinations.size)
        assertEquals("Cable A (100W)", combinations[0].first)
        assertEquals(45.0f, combinations[0].second, 0.01f)
        assertEquals("Cable B (60W)", combinations[1].first)
        assertEquals(27.5f, combinations[1].second, 0.01f)
    }

    @Test
    fun testNotificationIdCollisionPrevention() {
        // ContinuousMonitoringService uses 2002 as ongoing foreground service notification
        val continuousNotificationId = ContinuousMonitoringService.NOTIFICATION_ID
        val graceNotificationId = ChargingNotificationHelper.NOTIFICATION_ID_DISCONNECT_GRACE
        val finishedNotificationId = ChargingNotificationHelper.NOTIFICATION_ID

        assertNotEquals(
            "Disconnect grace notification ID must not collide with ContinuousMonitoringService",
            continuousNotificationId,
            graceNotificationId
        )
        assertNotEquals(
            "Session finished notification ID must not collide with ContinuousMonitoringService",
            continuousNotificationId,
            finishedNotificationId
        )
    }

    @Test
    fun testMultiHourSessionMetricsNotTruncated() {
        val startMs = 1700000000000L
        val disconnectMs = startMs + (7 * 3600 * 1000L) // unplugged 7 hours later

        val session = ChargeTestSession(
            id = "multi-hour-night",
            brickName = "65W GaN",
            cableName = "100W USB-C",
            startTimeMs = startMs,
            endTimeMs = disconnectMs,
            startBatteryLevel = 22,
            endBatteryLevel = 100,
            peakWattage = 38.5f,
            avgWattage = 24.1f,
            minVoltageMv = 3850,
            maxVoltageMv = 4400,
            peakCurrentMa = 8500,
            startTempDeciC = 260,
            maxTempDeciC = 360,
            isThrottled = false
        )

        assertEquals(7 * 3600 * 1000L, session.durationMs)
        assertEquals(78, session.batteryGained)
        assertTrue("Session duration must exceed 6 hours", session.durationMs > 6 * 3600 * 1000L)
    }
}
