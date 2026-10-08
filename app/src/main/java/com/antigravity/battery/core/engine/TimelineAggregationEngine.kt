package com.antigravity.battery.core.engine

import com.antigravity.battery.core.model.BatterySnapshot
import com.antigravity.battery.core.model.CulpritApp
import com.antigravity.battery.core.model.DeviceLevelMetrics
import com.antigravity.battery.core.model.SnapshotSerializer
import com.antigravity.battery.core.model.UidMetrics
import com.antigravity.battery.core.model.WakelockTagStat
import com.antigravity.battery.core.power.EnergyAttributionModel
import com.antigravity.battery.core.resolver.UidPackageResolver
import com.antigravity.battery.data.db.PeriodicSnapshotEntity
import kotlin.math.roundToInt

enum class TimeWindowFilter(val label: String) {
    SINCE_LAST_CHARGE("Since Last Charge"),
    TODAY("Today"),
    OVERNIGHT_SLEEP("Overnight Sleep 🌙"),
    YESTERDAY("Yesterday"),
    PAST_7_DAYS("Past 7 Days")
}

data class TimelinePoint(
    val timestampMs: Long,
    val batteryPercent: Int,
    val isCharging: Boolean,
    val isScreenOn: Boolean
)

data class TimelineViewData(
    val filter: TimeWindowFilter,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val points: List<TimelinePoint>,
    val totalDischargePercent: Int,
    val totalChargePercent: Int,
    val averageDrainRatePerHour: Double,
    val totalDischargeDurationMs: Long,
    val screenOnDurationMs: Long,
    val deepSleepDurationMs: Long,
    val awakeScreenOffMs: Long,
    val deepSleepEfficiencyPercent: Double,
    val topCulprits: List<CulpritApp>
)

class TimelineAggregationEngine(
    private val attributionModel: EnergyAttributionModel,
    private val culpritDetector: WakeupCulpritDetector,
    private val uidResolver: UidPackageResolver
) {

    fun aggregate(
        filter: TimeWindowFilter,
        snapshots: List<PeriodicSnapshotEntity>,
        nowMs: Long = System.currentTimeMillis()
    ): TimelineViewData {
        if (snapshots.isEmpty()) {
            return TimelineViewData(
                filter = filter,
                startTimeMs = nowMs - 86400000L,
                endTimeMs = nowMs,
                points = emptyList(),
                totalDischargePercent = 0,
                totalChargePercent = 0,
                averageDrainRatePerHour = 0.0,
                totalDischargeDurationMs = 0L,
                screenOnDurationMs = 0L,
                deepSleepDurationMs = 0L,
                awakeScreenOffMs = 0L,
                deepSleepEfficiencyPercent = 100.0,
                topCulprits = emptyList()
            )
        }

        val points = snapshots.map { s ->
            TimelinePoint(
                timestampMs = s.timestampMs,
                batteryPercent = s.batteryLevel,
                isCharging = s.isCharging,
                isScreenOn = s.isScreenOn
            )
        }

        var totalDischargeDrop = 0
        var totalChargeGain = 0
        var totalDischargeDurationMs = 0L
        var totalScreenOnMs = 0L
        var totalDeepSleepMs = 0L
        var totalAwakeScreenOffMs = 0L

        // UID accumulator across all discharge intervals
        val aggregatedUidDeltas = mutableMapOf<Int, MutableUidDelta>()
        val aggregatedAlarms = mutableMapOf<String, Int>()
        val aggregatedAlarmActions = mutableMapOf<String, MutableMap<String, Int>>()

        // Step through adjacent snapshot intervals
        for (i in 0 until snapshots.size - 1) {
            val curr = snapshots[i]
            val next = snapshots[i + 1]

            val deltaLevel = next.batteryLevel - curr.batteryLevel
            val deltaDuration = (next.timestampMs - curr.timestampMs).coerceAtLeast(0L)

            if (curr.isCharging || next.isCharging || deltaLevel > 0) {
                // Charging segment
                if (deltaLevel > 0) totalChargeGain += deltaLevel
            } else {
                // Discharging segment
                val drop = (curr.batteryLevel - next.batteryLevel).coerceAtLeast(0)
                totalDischargeDrop += drop
                totalDischargeDurationMs += deltaDuration

                // Hardware sleep states
                val deltaRealtime = (next.totalRealtimeMs - curr.totalRealtimeMs).coerceAtLeast(0L)
                val deltaUptime = (next.totalUptimeMs - curr.totalUptimeMs).coerceAtLeast(0L)
                val deltaScreenOn = (next.screenOnDurationMs - curr.screenOnDurationMs).coerceAtLeast(0L)

                totalScreenOnMs += deltaScreenOn
                val sliceDeepSleep = (deltaRealtime - deltaUptime).coerceAtLeast(0L)
                val sliceAwakeScreenOff = (deltaUptime - deltaScreenOn).coerceAtLeast(0L)
                totalDeepSleepMs += sliceDeepSleep
                totalAwakeScreenOffMs += sliceAwakeScreenOff

                // Accumulate UID deltas between curr and next
                val currUids = SnapshotSerializer.deserializeUidMetrics(curr.uidMetricsJson)
                val nextUids = SnapshotSerializer.deserializeUidMetrics(next.uidMetricsJson)
                val currAlarms = SnapshotSerializer.deserializePackageAlarms(curr.packageAlarmsJson)
                val nextAlarms = SnapshotSerializer.deserializePackageAlarms(next.packageAlarmsJson)

                accumulateUids(currUids, nextUids, aggregatedUidDeltas)
                accumulateAlarms(currAlarms, nextAlarms, aggregatedAlarms, aggregatedAlarmActions)
            }
        }

        val dischargeHours = (totalDischargeDurationMs.toDouble() / 3_600_000.0).coerceAtLeast(0.01)
        val drainRatePerHour = ((totalDischargeDrop / dischargeHours) * 100.0).roundToInt() / 100.0

        val screenOffMs = (totalDischargeDurationMs - totalScreenOnMs).coerceAtLeast(0L)
        val sleepEfficiency = if (screenOffMs > 0) {
            ((totalDeepSleepMs.toDouble() / screenOffMs.toDouble()) * 100.0).coerceIn(0.0, 100.0)
        } else 100.0

        // Build culprit list from aggregated deltas
        val culpritsList = mutableListOf<CulpritApp>()
        for ((uid, delta) in aggregatedUidDeltas) {
            if (delta.cpuUser + delta.cpuSys == 0L && delta.wlDuration == 0L && delta.radioMs == 0L && delta.wifiScans == 0) {
                continue
            }

            val appInfo = uidResolver.resolve(uid)
            val uidMetric = UidMetrics(
                uid = uid,
                cpuUserTimeMs = delta.cpuUser,
                cpuSystemTimeMs = delta.cpuSys,
                partialWakelockDurationMs = delta.wlDuration,
                wakelockCount = delta.wlCount,
                mobileRadioActiveMs = delta.radioMs,
                mobileBytesRx = delta.mobRx,
                mobileBytesTx = delta.mobTx,
                wifiScanCount = delta.wifiScans,
                wifiRunningMs = delta.wifiRunMs,
                wifiBytesRx = delta.wifiRx,
                wifiBytesTx = delta.wifiTx,
                gpsDurationMs = delta.gpsMs,
                wakelockTags = delta.tags
            )

            val (totalMah, breakdown) = attributionModel.calculateEnergy(uidMetric)
            val wakeups = aggregatedAlarms[appInfo.primaryPackageName] ?: 0
            val topActions = aggregatedAlarmActions[appInfo.primaryPackageName] ?: emptyMap()

            val badges = culpritDetector.detectBadges(
                deltaUid = uidMetric,
                wakeupAlarms = wakeups,
                sessionDurationMs = totalDischargeDurationMs,
                isScreenOffDominant = screenOffMs > totalScreenOnMs
            )

            culpritsList.add(
                CulpritApp(
                    uid = uid,
                    primaryPackageName = appInfo.primaryPackageName,
                    appLabel = appInfo.appLabel,
                    sharedPackageNames = appInfo.sharedPackageNames,
                    isSystemApp = appInfo.isSystemApp,
                    totalEstimatedMah = totalMah,
                    energyBreakdown = breakdown,
                    cpuUserTimeMs = delta.cpuUser,
                    cpuSystemTimeMs = delta.cpuSys,
                    partialWakelockMs = delta.wlDuration,
                    wakelockCount = delta.wlCount,
                    wakeupAlarmCount = wakeups,
                    mobileRadioActiveMs = delta.radioMs,
                    wifiScanCount = delta.wifiScans,
                    gpsDurationMs = delta.gpsMs,
                    mobileBytesRx = delta.mobRx,
                    mobileBytesTx = delta.mobTx,
                    wifiBytesRx = delta.wifiRx,
                    wifiBytesTx = delta.wifiTx,
                    topWakelockTags = delta.tags.values.sortedByDescending { it.durationMs }.take(5),
                    topAlarmActions = topActions,
                    warningBadges = badges
                )
            )
        }

        culpritsList.sortWith(
            compareByDescending<CulpritApp> { it.totalEstimatedMah }
                .thenByDescending { it.wakeupAlarmCount }
                .thenByDescending { it.partialWakelockMs }
        )

        return TimelineViewData(
            filter = filter,
            startTimeMs = snapshots.first().timestampMs,
            endTimeMs = snapshots.last().timestampMs,
            points = points,
            totalDischargePercent = totalDischargeDrop,
            totalChargePercent = totalChargeGain,
            averageDrainRatePerHour = drainRatePerHour,
            totalDischargeDurationMs = totalDischargeDurationMs,
            screenOnDurationMs = totalScreenOnMs,
            deepSleepDurationMs = totalDeepSleepMs,
            awakeScreenOffMs = totalAwakeScreenOffMs,
            deepSleepEfficiencyPercent = (sleepEfficiency * 10.0).roundToInt() / 10.0,
            topCulprits = culpritsList
        )
    }

    private fun accumulateUids(
        curr: Map<Int, UidMetrics>,
        next: Map<Int, UidMetrics>,
        accumulator: MutableMap<Int, MutableUidDelta>
    ) {
        val allUids = (curr.keys + next.keys).toSet()
        for (uid in allUids) {
            val c = curr[uid] ?: UidMetrics(uid)
            val n = next[uid] ?: UidMetrics(uid)

            val dUser = (n.cpuUserTimeMs - c.cpuUserTimeMs).coerceAtLeast(0L)
            val dSys = (n.cpuSystemTimeMs - c.cpuSystemTimeMs).coerceAtLeast(0L)
            val dWl = (n.partialWakelockDurationMs - c.partialWakelockDurationMs).coerceAtLeast(0L)
            val dWlCount = (n.wakelockCount - c.wakelockCount).coerceAtLeast(0)
            val dRadio = (n.mobileRadioActiveMs - c.mobileRadioActiveMs).coerceAtLeast(0L)
            val dMobRx = (n.mobileBytesRx - c.mobileBytesRx).coerceAtLeast(0L)
            val dMobTx = (n.mobileBytesTx - c.mobileBytesTx).coerceAtLeast(0L)
            val dWifiScans = (n.wifiScanCount - c.wifiScanCount).coerceAtLeast(0)
            val dWifiRun = (n.wifiRunningMs - c.wifiRunningMs).coerceAtLeast(0L)
            val dWifiRx = (n.wifiBytesRx - c.wifiBytesRx).coerceAtLeast(0L)
            val dWifiTx = (n.wifiBytesTx - c.wifiBytesTx).coerceAtLeast(0L)
            val dGps = (n.gpsDurationMs - c.gpsDurationMs).coerceAtLeast(0L)

            val entry = accumulator.getOrPut(uid) { MutableUidDelta() }
            entry.cpuUser += dUser
            entry.cpuSys += dSys
            entry.wlDuration += dWl
            entry.wlCount += dWlCount
            entry.radioMs += dRadio
            entry.mobRx += dMobRx
            entry.mobTx += dMobTx
            entry.wifiScans += dWifiScans
            entry.wifiRunMs += dWifiRun
            entry.wifiRx += dWifiRx
            entry.wifiTx += dWifiTx
            entry.gpsMs += dGps

            // Tags
            for ((tag, nextStat) in n.wakelockTags) {
                val currStat = c.wakelockTags[tag]
                val dTagDur = if (currStat != null) (nextStat.durationMs - currStat.durationMs).coerceAtLeast(0L) else nextStat.durationMs
                val dTagCnt = if (currStat != null) (nextStat.count - currStat.count).coerceAtLeast(0) else nextStat.count
                if (dTagDur > 0 || dTagCnt > 0) {
                    val existing = entry.tags[tag]
                    entry.tags[tag] = WakelockTagStat(
                        tag = tag,
                        durationMs = (existing?.durationMs ?: 0L) + dTagDur,
                        count = (existing?.count ?: 0) + dTagCnt
                    )
                }
            }
        }
    }

    private fun accumulateAlarms(
        curr: Map<String, com.antigravity.battery.core.model.PackageAlarmMetrics>,
        next: Map<String, com.antigravity.battery.core.model.PackageAlarmMetrics>,
        totalAlarms: MutableMap<String, Int>,
        actionsMap: MutableMap<String, MutableMap<String, Int>>
    ) {
        for ((pkg, nextMetric) in next) {
            val currMetric = curr[pkg]
            val dWakeups = if (currMetric != null) (nextMetric.wakeupCount - currMetric.wakeupCount).coerceAtLeast(0) else nextMetric.wakeupCount
            if (dWakeups > 0) {
                totalAlarms[pkg] = (totalAlarms[pkg] ?: 0) + dWakeups
            }

            val targetActions = actionsMap.getOrPut(pkg) { mutableMapOf() }
            nextMetric.topAlarmActions.forEach { (act, cnt) ->
                val prevCnt = currMetric?.topAlarmActions?.get(act) ?: 0
                val dAct = (cnt - prevCnt).coerceAtLeast(0)
                if (dAct > 0) {
                    targetActions[act] = (targetActions[act] ?: 0) + dAct
                }
            }
        }
    }

    private class MutableUidDelta {
        var cpuUser: Long = 0L
        var cpuSys: Long = 0L
        var wlDuration: Long = 0L
        var wlCount: Int = 0
        var radioMs: Long = 0L
        var mobRx: Long = 0L
        var mobTx: Long = 0L
        var wifiScans: Int = 0
        var wifiRunMs: Long = 0L
        var wifiRx: Long = 0L
        var wifiTx: Long = 0L
        var gpsMs: Long = 0L
        val tags = mutableMapOf<String, WakelockTagStat>()
    }
}
