package com.antigravity.battery

import com.antigravity.battery.core.engine.AnalyticsEngine
import com.antigravity.battery.core.model.InsightSeverity
import com.antigravity.battery.core.model.PackageAlarmMetrics
import com.antigravity.battery.core.model.PowerProfileData
import com.antigravity.battery.core.model.SnapshotSerializer
import com.antigravity.battery.core.model.UidMetrics
import com.antigravity.battery.core.model.WakelockTagStat
import com.antigravity.battery.core.resolver.ResolvedAppInfo
import com.antigravity.battery.core.resolver.UidPackageResolver
import com.antigravity.battery.data.db.LightweightSnapshot
import com.antigravity.battery.data.db.PeriodicSnapshotEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyticsEngineTest {

    private class TestUidResolver : UidPackageResolver() {
        override fun resolve(uid: Int): ResolvedAppInfo {
            return when (uid) {
                1000 -> ResolvedAppInfo(1000, "android", "Android System", listOf("android"), true)
                10339 -> ResolvedAppInfo(10339, "com.whatsapp", "WhatsApp", listOf("com.whatsapp"), false)
                else -> ResolvedAppInfo(uid, "uid.$uid", "App $uid", emptyList(), false)
            }
        }
    }

    @Test
    fun testAnalyticsEngineMultiDayRollup() {
        val resolver = TestUidResolver()
        val engine = AnalyticsEngine(resolver, PowerProfileData(batteryCapacityMah = 4865.0))

        val uidsStart = mapOf(
            10339 to UidMetrics(10339, cpuUserTimeMs = 10000L, partialWakelockDurationMs = 50000L, wakelockTags = mapOf("XmppWorker" to WakelockTagStat("XmppWorker", 50000L, 5)))
        )
        val uidsEnd = mapOf(
            10339 to UidMetrics(10339, cpuUserTimeMs = 500000L, partialWakelockDurationMs = 2500000L, wakelockTags = mapOf("XmppWorker" to WakelockTagStat("XmppWorker", 2500000L, 40)))
        )
        val alarmsStart = mapOf("com.whatsapp" to PackageAlarmMetrics("com.whatsapp", 2))
        val alarmsEnd = mapOf("com.whatsapp" to PackageAlarmMetrics("com.whatsapp", 65))

        val baseTime = 1792000000000L // arbitrary fixed epoch

        val lightSnapshots = listOf(
            LightweightSnapshot(
                id = 1,
                timestampMs = baseTime,
                batteryLevel = 90,
                voltageMv = 4200,
                tempDeciCelsius = 260,
                isCharging = false,
                isScreenOn = false,
                totalRealtimeMs = 100000L,
                totalUptimeMs = 30000L,
                screenOnDurationMs = 0L,
                screenOffDurationMs = 100000L
            ),
            LightweightSnapshot(
                id = 2,
                timestampMs = baseTime + 7200000L, // 2 hours later
                batteryLevel = 84,
                voltageMv = 4100,
                tempDeciCelsius = 280,
                isCharging = false,
                isScreenOn = false,
                totalRealtimeMs = 7300000L,
                totalUptimeMs = 1530000L,
                screenOnDurationMs = 0L,
                screenOffDurationMs = 7300000L
            )
        )

        val boundaryEntities = mapOf(
            1L to PeriodicSnapshotEntity(
                id = 1,
                timestampMs = baseTime,
                batteryLevel = 90,
                voltageMv = 4200,
                tempDeciCelsius = 260,
                isCharging = false,
                isScreenOn = false,
                totalRealtimeMs = 100000L,
                totalUptimeMs = 30000L,
                screenOnDurationMs = 0L,
                screenOffDurationMs = 100000L,
                uidMetricsJson = SnapshotSerializer.serializeUidMetrics(uidsStart),
                packageAlarmsJson = SnapshotSerializer.serializePackageAlarms(alarmsStart)
            ),
            2L to PeriodicSnapshotEntity(
                id = 2,
                timestampMs = baseTime + 7200000L,
                batteryLevel = 84,
                voltageMv = 4100,
                tempDeciCelsius = 280,
                isCharging = false,
                isScreenOn = false,
                totalRealtimeMs = 7300000L,
                totalUptimeMs = 1530000L,
                screenOnDurationMs = 0L,
                screenOffDurationMs = 7300000L,
                uidMetricsJson = SnapshotSerializer.serializeUidMetrics(uidsEnd),
                packageAlarmsJson = SnapshotSerializer.serializePackageAlarms(alarmsEnd)
            )
        )

        val result = engine.computeAnalytics(lightSnapshots, boundaryEntities)

        assertNotNull(result)
        assertEquals(6, result.totalDischargeDrop)
        assertEquals(2.0, result.totalDischargeHours, 0.1)
        assertEquals(3.0, result.avgDrainRatePerHour, 0.1)
        assertEquals(4865.0, result.estimatedCapacityMah, 0.1)

        // Verify wakelock leader identified
        assertTrue(result.topWakelockLeaders.isNotEmpty())
        val topWl = result.topWakelockLeaders.first()
        assertEquals("WhatsApp", topWl.appLabel)
        assertEquals("XmppWorker", topWl.topTag)
        assertTrue(topWl.durationMs > 2000000L)

        // Verify alarm leader identified
        assertTrue(result.topAlarmLeaders.isNotEmpty())
        val topAlarm = result.topAlarmLeaders.first()
        assertEquals("com.whatsapp", topAlarm.packageName)
        assertEquals(63, topAlarm.wakeupCount)

        // Verify hardware energy attribution
        assertTrue(result.hardwareEnergy.totalMah > 0.0)
        assertTrue(result.hardwareEnergy.cpuMah > 0.0)
        assertTrue(result.hardwareEnergy.wakelockMah > 0.0)
        assertTrue(result.hardwareEnergy.wifiMah < result.hardwareEnergy.totalMah * 0.5)

        // Verify potential savings calculated
        assertTrue(topWl.potentialSavingsMah > 0.0)
        assertTrue(topAlarm.potentialSavingsMah > 0.0)

        // Verify network comparison
        assertNotNull(result.networkComparison)

        // Verify insights generated
        assertTrue(result.insights.isNotEmpty())
    }

    @Test
    fun testDiagnosticExporterFormats() {
        val resolver = TestUidResolver()
        val engine = AnalyticsEngine(resolver, PowerProfileData(batteryCapacityMah = 4865.0))
        val result = engine.computeAnalytics(emptyList(), emptyMap())

        // Test HTML generation
        val html = com.antigravity.battery.data.export.DiagnosticExporter.exportToHtml(result, "Test Pixel")
        assertTrue(html.contains("<!DOCTYPE html>"))
        assertTrue(html.contains("PowerTrace Battery Diagnostic"))
        assertTrue(html.contains("Test Pixel"))
        assertTrue(html.contains("Hardware Subsystem Energy Attribution"))

        // Test CSV generation
        val csv = com.antigravity.battery.data.export.DiagnosticExporter.exportToCsv(result)
        assertTrue(csv.contains("PowerTrace Battery Diagnostics CSV Export"))
        assertTrue(csv.contains("## Hardware Energy Attribution"))

        // Test summary text
        val text = com.antigravity.battery.data.export.DiagnosticExporter.exportSummaryText(result)
        assertTrue(text.contains("PowerTrace Battery Diagnostic Summary"))
    }
}
