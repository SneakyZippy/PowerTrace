package com.antigravity.battery.core.engine

import com.antigravity.battery.core.model.BatterySnapshot
import com.antigravity.battery.core.model.CulpritApp
import com.antigravity.battery.core.model.SessionDelta
import com.antigravity.battery.core.model.UidMetrics
import com.antigravity.battery.core.model.WakelockTagStat
import com.antigravity.battery.core.power.EnergyAttributionModel
import com.antigravity.battery.core.resolver.UidPackageResolver
import kotlin.math.roundToInt

/**
 * Computes exact differential metrics (Delta = End - Start) between two snapshots,
 * handling counter rollbacks, reboot detection, and energy attribution.
 */
class SnapshotDeltaCalculator(
    private val attributionModel: EnergyAttributionModel,
    private val culpritDetector: WakeupCulpritDetector,
    private val uidResolver: UidPackageResolver
) {

    fun computeDelta(start: BatterySnapshot, end: BatterySnapshot): SessionDelta {
        val startDev = start.deviceMetrics
        val endDev = end.deviceMetrics

        // Detect device reboot or charging anomaly
        val isRebootDetected = endDev.totalRealtimeMs < startDev.totalRealtimeMs
        val isChargerDetected = endDev.batteryLevelPercent > startDev.batteryLevelPercent
        val isInterrupted = isRebootDetected || isChargerDetected
        val interruptionReason = when {
            isRebootDetected -> "Device reboot occurred during monitoring session"
            isChargerDetected -> "Device was connected to a charger during monitoring session"
            else -> null
        }

        // Monotonic duration calculations
        val rawDurationMs = end.timestampMs - start.timestampMs
        val realtimeMs = if (!isRebootDetected) {
            (endDev.totalRealtimeMs - startDev.totalRealtimeMs).coerceAtLeast(0L)
        } else {
            rawDurationMs.coerceAtLeast(0L)
        }

        val uptimeMs = if (!isRebootDetected) {
            (endDev.totalUptimeMs - startDev.totalUptimeMs).coerceAtLeast(0L)
        } else {
            (rawDurationMs * 0.7).toLong()
        }

        val screenOnMs = if (!isRebootDetected) {
            (endDev.screenOnDurationMs - startDev.screenOnDurationMs).coerceAtLeast(0L)
        } else 0L

        val screenOffMs = (realtimeMs - screenOnMs).coerceAtLeast(0L)
        val deepSleepMs = (realtimeMs - uptimeMs).coerceAtLeast(0L)
        val awakeScreenOffMs = (uptimeMs - screenOnMs).coerceAtLeast(0L)

        val sleepEfficiency = if (screenOffMs > 0) {
            ((deepSleepMs.toDouble() / screenOffMs.toDouble()) * 100.0).coerceIn(0.0, 100.0)
        } else 100.0

        val batteryDrop = (startDev.batteryLevelPercent - endDev.batteryLevelPercent).coerceAtLeast(0)
        val voltageDrop = startDev.voltageMv - endDev.voltageMv
        val tempDelta = endDev.temperatureDeciCelsius - startDev.temperatureDeciCelsius

        val sessionHours = (realtimeMs.toDouble() / 3_600_000.0).coerceAtLeast(0.001)
        val drainRatePerHour = (batteryDrop / sessionHours * 100.0).roundToInt() / 100.0

        // Compute UID deltas
        val allUids = (start.uidMetricsMap.keys + end.uidMetricsMap.keys).toSet()
        android.util.Log.d("SnapshotDelta", "computeDelta: comparing ${allUids.size} UIDs (start has ${start.uidMetricsMap.size}, end has ${end.uidMetricsMap.size})")
        val culpritsList = mutableListOf<CulpritApp>()

        for (uid in allUids) {
            val startUid = start.uidMetricsMap[uid] ?: UidMetrics(uid)
            val endUid = end.uidMetricsMap[uid] ?: UidMetrics(uid)

            val deltaUid = computeUidDelta(startUid, endUid, isRebootDetected)

            // Resolve app metadata
            val appInfo = uidResolver.resolve(uid)

            // Resolve wakeup alarms
            val startAlarms = start.packageAlarms[appInfo.primaryPackageName]?.wakeupCount ?: 0
            val endAlarms = end.packageAlarms[appInfo.primaryPackageName]?.wakeupCount ?: 0
            val wakeupCount = (endAlarms - startAlarms).coerceAtLeast(0)

            // Skip UIDs with practically zero activity
            if (deltaUid.cpuUserTimeMs + deltaUid.cpuSystemTimeMs == 0L &&
                deltaUid.partialWakelockDurationMs == 0L &&
                deltaUid.mobileRadioActiveMs == 0L &&
                deltaUid.wifiScanCount == 0 &&
                deltaUid.gpsDurationMs == 0L &&
                wakeupCount == 0
            ) {
                continue
            }

            android.util.Log.d("SnapshotDelta", "Found active UID $uid (${appInfo.primaryPackageName}): cpuUser=${deltaUid.cpuUserTimeMs}, cpuSys=${deltaUid.cpuSystemTimeMs}, wl=${deltaUid.partialWakelockDurationMs}ms, wakeups=$wakeupCount")

            val topActions = end.packageAlarms[appInfo.primaryPackageName]?.topAlarmActions ?: emptyMap()

            // Calculate estimated energy (mAh)
            val (totalMah, energyBreakdown) = attributionModel.calculateEnergy(deltaUid)

            // Identify warning badges
            val isScreenOffDominant = screenOffMs > screenOnMs
            val badges = culpritDetector.detectBadges(
                deltaUid = deltaUid,
                wakeupAlarms = wakeupCount,
                sessionDurationMs = realtimeMs,
                isScreenOffDominant = isScreenOffDominant
            )

            culpritsList.add(
                CulpritApp(
                    uid = uid,
                    primaryPackageName = appInfo.primaryPackageName,
                    appLabel = appInfo.appLabel,
                    sharedPackageNames = appInfo.sharedPackageNames,
                    isSystemApp = appInfo.isSystemApp,
                    totalEstimatedMah = totalMah,
                    energyBreakdown = energyBreakdown,
                    cpuUserTimeMs = deltaUid.cpuUserTimeMs,
                    cpuSystemTimeMs = deltaUid.cpuSystemTimeMs,
                    partialWakelockMs = deltaUid.partialWakelockDurationMs,
                    wakelockCount = deltaUid.wakelockCount,
                    wakeupAlarmCount = wakeupCount,
                    mobileRadioActiveMs = deltaUid.mobileRadioActiveMs,
                    wifiScanCount = deltaUid.wifiScanCount,
                    gpsDurationMs = deltaUid.gpsDurationMs,
                    mobileBytesRx = deltaUid.mobileBytesRx,
                    mobileBytesTx = deltaUid.mobileBytesTx,
                    wifiBytesRx = deltaUid.wifiBytesRx,
                    wifiBytesTx = deltaUid.wifiBytesTx,
                    topWakelockTags = deltaUid.wakelockTags.values.sortedByDescending { it.durationMs }.take(5),
                    topAlarmActions = topActions,
                    warningBadges = badges
                )
            )
        }

        // Rank culprits descending by mAh drain, secondary by wakeups
        culpritsList.sortWith(
            compareByDescending<CulpritApp> { it.totalEstimatedMah }
                .thenByDescending { it.wakeupAlarmCount }
                .thenByDescending { it.partialWakelockMs }
        )
        android.util.Log.d("SnapshotDelta", "computeDelta: finished with ${culpritsList.size} culprits")

        return SessionDelta(
            durationMs = realtimeMs,
            batteryLevelStart = startDev.batteryLevelPercent,
            batteryLevelEnd = endDev.batteryLevelPercent,
            batteryPercentDrop = batteryDrop,
            voltageDropMv = voltageDrop,
            tempDeltaDeciCelsius = tempDelta,
            drainRatePercentPerHour = drainRatePerHour,
            realtimeMs = realtimeMs,
            uptimeMs = uptimeMs,
            screenOnDurationMs = screenOnMs,
            screenOffDurationMs = screenOffMs,
            deepSleepDurationMs = deepSleepMs,
            awakeScreenOffMs = awakeScreenOffMs,
            deepSleepEfficiencyPercent = (sleepEfficiency * 10.0).roundToInt() / 10.0,
            isInterrupted = isInterrupted,
            interruptionReason = interruptionReason,
            culprits = culpritsList
        )
    }

    private fun computeUidDelta(start: UidMetrics, end: UidMetrics, isReboot: Boolean): UidMetrics {
        val cpuUser = if (isReboot) end.cpuUserTimeMs else (end.cpuUserTimeMs - start.cpuUserTimeMs).coerceAtLeast(0L)
        val cpuSys = if (isReboot) end.cpuSystemTimeMs else (end.cpuSystemTimeMs - start.cpuSystemTimeMs).coerceAtLeast(0L)
        val wakelockDuration = if (isReboot) end.partialWakelockDurationMs else (end.partialWakelockDurationMs - start.partialWakelockDurationMs).coerceAtLeast(0L)
        val wakelockCount = if (isReboot) end.wakelockCount else (end.wakelockCount - start.wakelockCount).coerceAtLeast(0)
        val radioActive = if (isReboot) end.mobileRadioActiveMs else (end.mobileRadioActiveMs - start.mobileRadioActiveMs).coerceAtLeast(0L)
        val mobRx = if (isReboot) end.mobileBytesRx else (end.mobileBytesRx - start.mobileBytesRx).coerceAtLeast(0L)
        val mobTx = if (isReboot) end.mobileBytesTx else (end.mobileBytesTx - start.mobileBytesTx).coerceAtLeast(0L)
        val wifiScans = if (isReboot) end.wifiScanCount else (end.wifiScanCount - start.wifiScanCount).coerceAtLeast(0)
        val wifiRunning = if (isReboot) end.wifiRunningMs else (end.wifiRunningMs - start.wifiRunningMs).coerceAtLeast(0L)
        val wifiRx = if (isReboot) end.wifiBytesRx else (end.wifiBytesRx - start.wifiBytesRx).coerceAtLeast(0L)
        val wifiTx = if (isReboot) end.wifiBytesTx else (end.wifiBytesTx - start.wifiBytesTx).coerceAtLeast(0L)
        val gpsDuration = if (isReboot) end.gpsDurationMs else (end.gpsDurationMs - start.gpsDurationMs).coerceAtLeast(0L)

        // Compute tag deltas
        val allTags = (start.wakelockTags.keys + end.wakelockTags.keys).toSet()
        val tagDeltas = mutableMapOf<String, WakelockTagStat>()
        for (tag in allTags) {
            val startTag = start.wakelockTags[tag]
            val endTag = end.wakelockTags[tag]
            if (endTag != null) {
                val duration = if (isReboot || startTag == null) endTag.durationMs else (endTag.durationMs - startTag.durationMs).coerceAtLeast(0L)
                val count = if (isReboot || startTag == null) endTag.count else (endTag.count - startTag.count).coerceAtLeast(0)
                if (duration > 0 || count > 0) {
                    tagDeltas[tag] = WakelockTagStat(tag, duration, count)
                }
            }
        }

        return UidMetrics(
            uid = end.uid,
            cpuUserTimeMs = cpuUser,
            cpuSystemTimeMs = cpuSys,
            cpuClusterTimesMs = end.cpuClusterTimesMs,
            partialWakelockDurationMs = wakelockDuration,
            wakelockCount = wakelockCount,
            mobileRadioActiveMs = radioActive,
            mobileBytesRx = mobRx,
            mobileBytesTx = mobTx,
            wifiScanCount = wifiScans,
            wifiRunningMs = wifiRunning,
            wifiBytesRx = wifiRx,
            wifiBytesTx = wifiTx,
            gpsDurationMs = gpsDuration,
            wakelockTags = tagDeltas
        )
    }
}
