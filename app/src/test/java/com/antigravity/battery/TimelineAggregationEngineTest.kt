package com.antigravity.battery

import android.content.Context
import com.antigravity.battery.core.engine.TimeWindowFilter
import com.antigravity.battery.core.engine.TimelineAggregationEngine
import com.antigravity.battery.core.engine.WakeupCulpritDetector
import com.antigravity.battery.core.model.PackageAlarmMetrics
import com.antigravity.battery.core.model.PowerProfileData
import com.antigravity.battery.core.model.SnapshotSerializer
import com.antigravity.battery.core.model.UidMetrics
import com.antigravity.battery.core.model.WakelockTagStat
import com.antigravity.battery.core.power.EnergyAttributionModel
import com.antigravity.battery.core.resolver.ResolvedAppInfo
import com.antigravity.battery.core.resolver.UidPackageResolver
import com.antigravity.battery.data.db.PeriodicSnapshotEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineAggregationEngineTest {

    private class TestUidResolver : UidPackageResolver() {
        override fun resolve(uid: Int): ResolvedAppInfo {
            return when (uid) {
                1000 -> ResolvedAppInfo(1000, "android", "Android System", listOf("android"), true)
                10182 -> ResolvedAppInfo(10182, "com.popular.social.app", "Popular Social", listOf("com.popular.social.app"), false)
                else -> ResolvedAppInfo(uid, "uid.$uid", "App $uid", emptyList(), false)
            }
        }
    }

    @Test
    fun testSegregateDischargeAndCharging() {
        val profile = PowerProfileData(cpuAwakeMa = 28.0)
        val attribution = EnergyAttributionModel(profile)
        val detector = WakeupCulpritDetector()
        val resolver = TestUidResolver()
        val engine = TimelineAggregationEngine(attribution, detector, resolver)

        val uids1 = mapOf(
            10182 to UidMetrics(10182, cpuUserTimeMs = 10000L, partialWakelockDurationMs = 50000L, wakelockTags = mapOf("SyncTag" to WakelockTagStat("SyncTag", 50000L, 5)))
        )
        val uids2 = mapOf(
            10182 to UidMetrics(10182, cpuUserTimeMs = 120000L, partialWakelockDurationMs = 700000L, wakelockTags = mapOf("SyncTag" to WakelockTagStat("SyncTag", 700000L, 25)))
        )
        val alarms1 = mapOf("com.popular.social.app" to PackageAlarmMetrics("com.popular.social.app", 2))
        val alarms2 = mapOf("com.popular.social.app" to PackageAlarmMetrics("com.popular.social.app", 15))

        val snapshots = listOf(
            PeriodicSnapshotEntity(
                id = 1,
                timestampMs = 1000000L,
                batteryLevel = 100,
                voltageMv = 4200,
                tempDeciCelsius = 280,
                isCharging = false,
                isScreenOn = false,
                totalRealtimeMs = 1000000L,
                totalUptimeMs = 800000L,
                screenOnDurationMs = 100000L,
                screenOffDurationMs = 900000L,
                uidMetricsJson = SnapshotSerializer.serializeUidMetrics(uids1),
                packageAlarmsJson = SnapshotSerializer.serializePackageAlarms(alarms1)
            ),
            PeriodicSnapshotEntity(
                id = 2,
                timestampMs = 4600000L, // + 1 hour
                batteryLevel = 94, // -6% discharge
                voltageMv = 4100,
                tempDeciCelsius = 285,
                isCharging = false,
                isScreenOn = false,
                totalRealtimeMs = 4600000L,
                totalUptimeMs = 2000000L,
                screenOnDurationMs = 200000L,
                screenOffDurationMs = 4400000L,
                uidMetricsJson = SnapshotSerializer.serializeUidMetrics(uids2),
                packageAlarmsJson = SnapshotSerializer.serializePackageAlarms(alarms2)
            ),
            PeriodicSnapshotEntity(
                id = 3,
                timestampMs = 6400000L, // + 30 min charging
                batteryLevel = 99, // +5% charging
                voltageMv = 4300,
                tempDeciCelsius = 310,
                isCharging = true,
                isScreenOn = false,
                totalRealtimeMs = 6400000L,
                totalUptimeMs = 3800000L,
                screenOnDurationMs = 250000L,
                screenOffDurationMs = 6150000L,
                uidMetricsJson = SnapshotSerializer.serializeUidMetrics(uids2),
                packageAlarmsJson = SnapshotSerializer.serializePackageAlarms(alarms2)
            )
        )

        val result = engine.aggregate(TimeWindowFilter.TODAY, snapshots, 6500000L)

        // Discharge drop should be exactly 6%
        assertEquals(6, result.totalDischargePercent)

        // Charge gain should be 5%
        assertEquals(5, result.totalChargePercent)

        // Culprit verification: Social app should have accumulated wakelocks and wakeups
        val culprit = result.topCulprits.find { it.uid == 10182 }
        assertNotNull(culprit)
        assertEquals(13, culprit!!.wakeupAlarmCount)
        assertEquals(650000L, culprit.partialWakelockMs)
        assertTrue(culprit.totalEstimatedMah > 0.0)
    }
}
