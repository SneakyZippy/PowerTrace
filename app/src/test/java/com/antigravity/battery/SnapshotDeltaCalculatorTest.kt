package com.antigravity.battery

import android.content.Context
import com.antigravity.battery.core.dump.MockDiagnosticDumpSource
import com.antigravity.battery.core.engine.SnapshotDeltaCalculator
import com.antigravity.battery.core.engine.SnapshotManager
import com.antigravity.battery.core.engine.WakeupCulpritDetector
import com.antigravity.battery.core.model.CulpritWarningBadge
import com.antigravity.battery.core.model.PowerProfileData
import com.antigravity.battery.core.power.EnergyAttributionModel
import com.antigravity.battery.core.resolver.ResolvedAppInfo
import com.antigravity.battery.core.resolver.UidPackageResolver
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SnapshotDeltaCalculatorTest {

    private class TestUidResolver : UidPackageResolver() {
        override fun resolve(uid: Int): ResolvedAppInfo {
            return when (uid) {
                1000 -> ResolvedAppInfo(1000, "android", "Android System", listOf("android"), true)
                10085 -> ResolvedAppInfo(10085, "com.google.android.gms", "Google Play Services", listOf("com.google.android.gms"), true)
                10182 -> ResolvedAppInfo(10182, "com.popular.social.app", "Popular Social", listOf("com.popular.social.app"), false)
                10214 -> ResolvedAppInfo(10214, "com.instant.messenger", "Messenger", listOf("com.instant.messenger"), false)
                else -> ResolvedAppInfo(uid, "uid.$uid", "App $uid", emptyList(), false)
            }
        }
    }

    @Test
    fun testComputeOvernightDelta() = runBlocking {
        val mockSource = MockDiagnosticDumpSource()
        val snapshotManager = SnapshotManager(mockSource)

        // Capture Start Snapshot
        mockSource.setSnapshotStage(endSnapshot = false)
        val start = snapshotManager.captureSnapshot(1700000000000L)

        // Capture End Snapshot (8 hours later)
        mockSource.setSnapshotStage(endSnapshot = true)
        val end = snapshotManager.captureSnapshot(1700028800000L)

        val powerProfile = PowerProfileData(cpuAwakeMa = 28.0)
        val attributionModel = EnergyAttributionModel(powerProfile)
        val detector = WakeupCulpritDetector()
        val resolver = TestUidResolver()

        val calculator = SnapshotDeltaCalculator(attributionModel, detector, resolver)
        val delta = calculator.computeDelta(start, end)

        // Battery level dropped from 95% to 84% -> 11%
        assertEquals(11, delta.batteryPercentDrop)

        // Realtime 38,800,000 - 10,000,000 = 28,800,000 ms (8.0 hours)
        assertEquals(28800000L, delta.realtimeMs)

        // Drain rate = 11% / 8.0 hr = 1.38 %/hr
        assertEquals(1.38, delta.drainRatePercentPerHour, 0.05)

        assertFalse(delta.isInterrupted)

        // Culprits should be populated and sorted descending by drain / alarms
        assertTrue(delta.culprits.isNotEmpty())
        val topCulprit = delta.culprits.first()
        assertNotNull(topCulprit)

        // Check social app has runaway wakelock badge
        val socialCulprit = delta.culprits.find { it.uid == 10182 }
        assertNotNull(socialCulprit)
        assertTrue(socialCulprit!!.warningBadges.contains(CulpritWarningBadge.RUNAWAY_WAKELOCK))
        assertTrue(socialCulprit.totalEstimatedMah > 0.0)

        // Check Google Play Services has wakeup alarms recorded
        val gmsCulprit = delta.culprits.find { it.uid == 10085 }
        assertNotNull(gmsCulprit)
        assertEquals(64 - 12, gmsCulprit!!.wakeupAlarmCount)
    }
}
