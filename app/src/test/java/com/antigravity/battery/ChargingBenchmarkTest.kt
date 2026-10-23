package com.antigravity.battery

import com.antigravity.battery.core.model.Cable
import com.antigravity.battery.core.model.ChargeTestSession
import com.antigravity.battery.core.model.LiveChargeTelemetry
import com.antigravity.battery.core.model.PowerBrick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChargingBenchmarkTest {

    @Test
    fun testPowerWattageCalculation() {
        val voltageMv = 4200
        val currentMa = 6000
        val wattage = (voltageMv.toFloat() * currentMa.toFloat()) / 1_000_000f

        assertEquals(25.2f, wattage, 0.001f)
    }

    @Test
    fun testFastChargePowerCalculation() {
        val voltageMv = 4350
        val currentMa = 10000
        val wattage = (voltageMv.toFloat() * currentMa.toFloat()) / 1_000_000f

        assertEquals(43.5f, wattage, 0.001f)
    }

    @Test
    fun testChargeTestSessionCalculations() {
        val startTime = 1000000L
        val endTime = startTime + (15 * 60 * 1000L) // 15 mins

        val session = ChargeTestSession(
            startTimeMs = startTime,
            endTimeMs = endTime,
            startBatteryLevel = 20,
            endBatteryLevel = 55,
            peakWattage = 33.5f,
            avgWattage = 26.2f,
            minVoltageMv = 3900,
            maxVoltageMv = 4300,
            peakCurrentMa = 8000,
            startTempDeciC = 280, // 28.0°C
            maxTempDeciC = 356,   // 35.6°C
            isThrottled = false
        )

        assertEquals(15 * 60 * 1000L, session.durationMs)
        assertEquals(35, session.batteryGained)
        assertEquals(28.0f, session.startTempCelsius, 0.01f)
        assertEquals(35.6f, session.maxTempCelsius, 0.01f)
        assertFalse(session.isThrottled)
    }

    @Test
    fun testSmartThresholdEvaluation() {
        val minDurationMs = 5 * 60 * 1000L
        val minGain = 5

        // Case 1: Short session (2 mins), gained 1% -> should not prompt
        val shortSession = ChargeTestSession(
            startTimeMs = 1000000L,
            endTimeMs = 1000000L + (2 * 60 * 1000L),
            startBatteryLevel = 50,
            endBatteryLevel = 51,
            peakWattage = 15f,
            avgWattage = 12f,
            minVoltageMv = 4000,
            maxVoltageMv = 4050,
            peakCurrentMa = 3000,
            startTempDeciC = 300,
            maxTempDeciC = 310
        )
        val shortQualifies = shortSession.durationMs >= minDurationMs && shortSession.batteryGained >= minGain
        assertFalse(shortQualifies)

        // Case 2: Long session (10 mins), but 0% gain (e.g. idle high drain) -> should not prompt
        val flatSession = ChargeTestSession(
            startTimeMs = 1000000L,
            endTimeMs = 1000000L + (10 * 60 * 1000L),
            startBatteryLevel = 90,
            endBatteryLevel = 90,
            peakWattage = 3f,
            avgWattage = 2f,
            minVoltageMv = 4200,
            maxVoltageMv = 4200,
            peakCurrentMa = 500,
            startTempDeciC = 300,
            maxTempDeciC = 310
        )
        val flatQualifies = flatSession.durationMs >= minDurationMs && flatSession.batteryGained >= minGain
        assertFalse(flatQualifies)

        // Case 3: 8 minutes session, gained 9% -> should qualify for prompt
        val validSession = ChargeTestSession(
            startTimeMs = 1000000L,
            endTimeMs = 1000000L + (8 * 60 * 1000L),
            startBatteryLevel = 25,
            endBatteryLevel = 34,
            peakWattage = 28f,
            avgWattage = 22f,
            minVoltageMv = 4000,
            maxVoltageMv = 4250,
            peakCurrentMa = 6500,
            startTempDeciC = 290,
            maxTempDeciC = 340
        )
        val validQualifies = validSession.durationMs >= minDurationMs && validSession.batteryGained >= minGain
        assertTrue(validQualifies)
    }

    @Test
    fun testLiveTelemetryModel() {
        val telemetry = LiveChargeTelemetry(
            isCharging = true,
            chargePlugType = "AC Wall Charger",
            currentWattage = 27.5f,
            currentMa = 6500,
            voltageMv = 4230,
            batteryLevelPercent = 42,
            tempDeciCelsius = 315,
            peakWattageSoFar = 31.0f,
            avgWattageSoFar = 25.0f,
            sessionDurationMs = 180000L,
            isThrottlingDetected = false
        )

        assertEquals(4.23f, telemetry.voltageV, 0.001f)
        assertEquals(31.5f, telemetry.tempCelsius, 0.01f)
        assertTrue(telemetry.isCharging)
    }

    @Test
    fun testAccessoryCreation() {
        val brick = PowerBrick(
            name = "Anker 65W GaN",
            manufacturer = "Anker",
            ratedWattage = 65f,
            portType = "USB-C PD"
        )
        assertEquals("Anker 65W GaN", brick.name)
        assertEquals(65f, brick.ratedWattage, 0.01f)

        val cable = Cable(
            name = "100W 2m Cable",
            manufacturer = "Baseus",
            ratedWattage = 100f,
            lengthMeters = 2.0f,
            cableType = "Type-C to Type-C"
        )
        assertEquals(100f, cable.ratedWattage, 0.01f)
        assertEquals(2.0f, cable.lengthMeters, 0.01f)
    }
}
