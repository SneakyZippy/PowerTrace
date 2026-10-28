package com.antigravity.battery.core.engine

import com.antigravity.battery.core.model.BatterySnapshot
import com.antigravity.battery.core.model.CulpritApp
import com.antigravity.battery.core.model.DeviceLevelMetrics
import com.antigravity.battery.core.model.SnapshotSerializer
import com.antigravity.battery.core.model.UidMetrics
import com.antigravity.battery.core.model.WakelockTagStat
import com.antigravity.battery.core.power.EnergyAttributionModel
import com.antigravity.battery.core.resolver.UidPackageResolver
import com.antigravity.battery.data.db.LightweightSnapshot
import com.antigravity.battery.data.db.PeriodicSnapshotEntity
import kotlin.math.roundToInt

enum class TimeWindowFilter(val label: String) {
    SINCE_LAST_CHARGE("Since Unplugged ⚡"),
    LAST_2_HOURS("Last 2h"),
    LAST_8_HOURS("Last 8h"),
    OVERNIGHT_SLEEP("Overnight 🌙"),
    TODAY("Today"),
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

    /**
     * Identifies the minimal snapshot IDs needed for telemetry delta calculation.
     * Only the boundary snapshots (start and end) of each contiguous discharge period
     * need to have their heavy JSON metrics loaded.
     */
    fun findRequiredBoundarySnapshotIds(snapshots: List<LightweightSnapshot>): List<Long> {
        if (snapshots.isEmpty()) return emptyList()
        val ids = mutableSetOf<Long>()
        var segmentStartIdx: Int? = null

        for (i in 0 until snapshots.size - 1) {
            val curr = snapshots[i]
            val next = snapshots[i + 1]
            val deltaLevel = next.batteryLevel - curr.batteryLevel
            val isDischarge = !curr.isCharging && !next.isCharging && deltaLevel <= 0 && next.totalRealtimeMs >= curr.totalRealtimeMs

            if (isDischarge) {
                if (segmentStartIdx == null) {
                    segmentStartIdx = i
                }
            } else {
                if (segmentStartIdx != null) {
                    ids.add(snapshots[segmentStartIdx].id)
                    ids.add(snapshots[i].id)
                    segmentStartIdx = null
                }
            }
        }

        if (segmentStartIdx != null) {
            ids.add(snapshots[segmentStartIdx].id)
            ids.add(snapshots.last().id)
        }

        ids.add(snapshots.first().id)
        ids.add(snapshots.last().id)
        return ids.toList()
    }

    /**
     * Backward-compatible entrypoint used when full PeriodicSnapshotEntities are already in memory (e.g. tests).
     */
    fun aggregate(
        filter: TimeWindowFilter,
        snapshots: List<PeriodicSnapshotEntity>,
        nowMs: Long = System.currentTimeMillis()
    ): TimelineViewData {
        val lightSnapshots = snapshots.map { s ->
            LightweightSnapshot(
                id = s.id,
                timestampMs = s.timestampMs,
                batteryLevel = s.batteryLevel,
                voltageMv = s.voltageMv,
                tempDeciCelsius = s.tempDeciCelsius,
                isCharging = s.isCharging,
                isScreenOn = s.isScreenOn,
                totalRealtimeMs = s.totalRealtimeMs,
                totalUptimeMs = s.totalUptimeMs,
                screenOnDurationMs = s.screenOnDurationMs,
                screenOffDurationMs = s.screenOffDurationMs
            )
        }
        val fullMap = snapshots.associateBy { it.id }
        return aggregateLightweight(filter, lightSnapshots, fullMap, nowMs)
    }

    /**
     * High-speed aggregation using lightweight snapshots for timeline points/durations,
     * and only querying/parsing JSON for discharge segment boundaries.
     */
    fun aggregateLightweight(
        filter: TimeWindowFilter,
        snapshots: List<LightweightSnapshot>,
        boundarySnapshots: Map<Long, PeriodicSnapshotEntity>,
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
                deepSleepEfficiencyPercent = 0.0,
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

        // Track contiguous discharge segments
        data class Segment(val startIdx: Int, val endIdx: Int, val durationMs: Long)
        val dischargeSegments = mutableListOf<Segment>()
        var currentSegmentStart: Int? = null

        // Step through adjacent snapshot intervals for hardware timers
        for (i in 0 until snapshots.size - 1) {
            val curr = snapshots[i]
            val next = snapshots[i + 1]

            val deltaLevel = next.batteryLevel - curr.batteryLevel
            val deltaDuration = (next.timestampMs - curr.timestampMs).coerceAtLeast(0L)

            val isChargingInterval = curr.isCharging || next.isCharging || deltaLevel > 0
            val isReboot = next.totalRealtimeMs < curr.totalRealtimeMs

            if (isChargingInterval || isReboot) {
                if (deltaLevel > 0) totalChargeGain += deltaLevel
                if (currentSegmentStart != null) {
                    if (currentSegmentStart != i) {
                        val segDuration = (snapshots[i].timestampMs - snapshots[currentSegmentStart].timestampMs).coerceAtLeast(0L)
                        dischargeSegments.add(Segment(currentSegmentStart, i, segDuration))
                    }
                    currentSegmentStart = null
                }
            } else {
                // Discharging segment
                val drop = (curr.batteryLevel - next.batteryLevel).coerceAtLeast(0)
                totalDischargeDrop += drop
                totalDischargeDurationMs += deltaDuration

                // Hardware sleep states
                val rawDeltaRealtime = (next.totalRealtimeMs - curr.totalRealtimeMs).coerceAtLeast(0L)
                val deltaRealtime = if (deltaDuration > 0L && rawDeltaRealtime > deltaDuration) deltaDuration else rawDeltaRealtime
                val rawDeltaUptime = (next.totalUptimeMs - curr.totalUptimeMs).coerceAtLeast(0L)
                val deltaUptime = rawDeltaUptime.coerceAtMost(deltaRealtime)
                val rawDeltaScreenOn = (next.screenOnDurationMs - curr.screenOnDurationMs).coerceAtLeast(0L)
                val deltaScreenOn = rawDeltaScreenOn.coerceAtMost(deltaUptime)

                totalScreenOnMs += deltaScreenOn
                val sliceDeepSleep = (deltaRealtime - deltaUptime).coerceAtLeast(0L)
                val sliceAwakeScreenOff = (deltaUptime - deltaScreenOn).coerceAtLeast(0L)
                totalDeepSleepMs += sliceDeepSleep
                totalAwakeScreenOffMs += sliceAwakeScreenOff

                if (currentSegmentStart == null) {
                    currentSegmentStart = i
                }
            }
        }

        if (currentSegmentStart != null && currentSegmentStart != snapshots.size - 1) {
            val lastIdx = snapshots.size - 1
            val segDuration = (snapshots[lastIdx].timestampMs - snapshots[currentSegmentStart].timestampMs).coerceAtLeast(0L)
            dischargeSegments.add(Segment(currentSegmentStart, lastIdx, segDuration))
        }

        // Aggregate UID deltas across identified discharge segments
        val aggregatedUidDeltas = mutableMapOf<Int, MutableUidDelta>()
        val aggregatedAlarms = mutableMapOf<String, Int>()
        val aggregatedAlarmActions = mutableMapOf<String, MutableMap<String, Int>>()

        for (segment in dischargeSegments) {
            val startEntity = boundarySnapshots[snapshots[segment.startIdx].id]
            val endEntity = boundarySnapshots[snapshots[segment.endIdx].id]
            if (startEntity != null && endEntity != null) {
                val startUids = SnapshotSerializer.deserializeUidMetrics(startEntity.uidMetricsJson)
                val endUids = SnapshotSerializer.deserializeUidMetrics(endEntity.uidMetricsJson)
                val startAlarms = SnapshotSerializer.deserializePackageAlarms(startEntity.packageAlarmsJson)
                val endAlarms = SnapshotSerializer.deserializePackageAlarms(endEntity.packageAlarmsJson)

                accumulateUids(startUids, endUids, aggregatedUidDeltas, segment.durationMs)
                accumulateAlarms(startAlarms, endAlarms, aggregatedAlarms, aggregatedAlarmActions)
            }
        }

        val dischargeHours = (totalDischargeDurationMs.toDouble() / 3_600_000.0).coerceAtLeast(0.01)
        val drainRatePerHour = ((totalDischargeDrop / dischargeHours) * 100.0).roundToInt() / 100.0

        val screenOffHardwareMs = totalDeepSleepMs + totalAwakeScreenOffMs
        val sleepEfficiency = if (screenOffHardwareMs > 0L) {
            ((totalDeepSleepMs.toDouble() / screenOffHardwareMs.toDouble()) * 100.0).coerceIn(0.0, 100.0)
        } else 0.0

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
                isScreenOffDominant = screenOffHardwareMs > totalScreenOnMs
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
        accumulator: MutableMap<Int, MutableUidDelta>,
        segmentDurationMs: Long = Long.MAX_VALUE
    ) {
        val maxCpuMs = (segmentDurationMs * 8).coerceAtLeast(60_000L)
        val maxHardwareMs = segmentDurationMs.coerceAtLeast(60_000L)

        val allUids = (curr.keys + next.keys).toSet()
        for (uid in allUids) {
            val c = curr[uid] ?: UidMetrics(uid)
            val n = next[uid] ?: UidMetrics(uid)

            val rawUser = (n.cpuUserTimeMs - c.cpuUserTimeMs).coerceAtLeast(0L)
            val rawSys = (n.cpuSystemTimeMs - c.cpuSystemTimeMs).coerceAtLeast(0L)
            val dUser = rawUser.coerceAtMost(maxCpuMs)
            val dSys = rawSys.coerceAtMost(maxCpuMs)

            val rawWl = (n.partialWakelockDurationMs - c.partialWakelockDurationMs).coerceAtLeast(0L)
            val dWl = rawWl.coerceAtMost(maxHardwareMs)
            val dWlCount = (n.wakelockCount - c.wakelockCount).coerceAtLeast(0)

            val rawRadio = (n.mobileRadioActiveMs - c.mobileRadioActiveMs).coerceAtLeast(0L)
            val dRadio = rawRadio.coerceAtMost(maxHardwareMs)

            val dMobRx = (n.mobileBytesRx - c.mobileBytesRx).coerceAtLeast(0L)
            val dMobTx = (n.mobileBytesTx - c.mobileBytesTx).coerceAtLeast(0L)
            val rawWifiScans = (n.wifiScanCount - c.wifiScanCount).coerceAtLeast(0)
            val dWifiScans = (if (rawWifiScans > 50) minOf((rawWifiScans / 1_000_000L).toInt(), 10) else rawWifiScans).coerceIn(0, 10)

            val rawWifiRun = (n.wifiRunningMs - c.wifiRunningMs).coerceAtLeast(0L)
            val dWifiRun = (if (rawWifiRun > maxHardwareMs * 500) rawWifiRun / 1000L else rawWifiRun).coerceIn(0L, maxHardwareMs)

            val dWifiRx = (n.wifiBytesRx - c.wifiBytesRx).coerceAtLeast(0L)
            val dWifiTx = (n.wifiBytesTx - c.wifiBytesTx).coerceAtLeast(0L)

            val rawGps = (n.gpsDurationMs - c.gpsDurationMs).coerceAtLeast(0L)
            val dGps = rawGps.coerceAtMost(maxHardwareMs)

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
                val rawTagDur = if (currStat != null) (nextStat.durationMs - currStat.durationMs).coerceAtLeast(0L) else nextStat.durationMs
                val dTagDur = rawTagDur.coerceAtMost(maxHardwareMs)
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
