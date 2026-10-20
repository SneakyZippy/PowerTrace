package com.antigravity.battery.core.engine

import com.antigravity.battery.core.model.ActionableInsight
import com.antigravity.battery.core.model.AlarmLeader
import com.antigravity.battery.core.model.DailyAnalyticsSummary
import com.antigravity.battery.core.model.HardwareEnergyBreakdown
import com.antigravity.battery.core.model.InsightSeverity
import com.antigravity.battery.core.model.NetworkDrainComparison
import com.antigravity.battery.core.model.OverallAnalytics
import com.antigravity.battery.core.model.PowerProfileData
import com.antigravity.battery.core.model.SnapshotSerializer
import com.antigravity.battery.core.model.UidMetrics
import com.antigravity.battery.core.model.WakelockLeader
import com.antigravity.battery.core.power.EnergyAttributionModel
import com.antigravity.battery.core.resolver.UidPackageResolver
import com.antigravity.battery.data.db.LightweightSnapshot
import com.antigravity.battery.data.db.PeriodicSnapshotEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class AnalyticsEngine(
    private val uidResolver: UidPackageResolver,
    private val powerProfile: PowerProfileData = PowerProfileData()
) {

    private val attributionModel = EnergyAttributionModel(powerProfile)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun computeAnalytics(
        snapshots: List<LightweightSnapshot>,
        boundarySnapshots: Map<Long, PeriodicSnapshotEntity>
    ): OverallAnalytics {
        if (snapshots.isEmpty()) {
            return OverallAnalytics(
                totalDays = 0.0,
                totalSnapshots = 0,
                totalDischargeDrop = 0,
                totalChargeGain = 0,
                totalDischargeHours = 0.0,
                totalChargeHours = 0.0,
                avgDrainRatePerHour = 0.0,
                avgDeepSleepEfficiency = 100.0,
                minTempC = 0.0,
                maxTempC = 0.0,
                avgTempC = 0.0,
                minVoltageMv = 0,
                maxVoltageMv = 0,
                estimatedCapacityMah = powerProfile.batteryCapacityMah,
                dailySummaries = emptyList(),
                hardwareEnergy = HardwareEnergyBreakdown(0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
                topWakelockLeaders = emptyList(),
                topAlarmLeaders = emptyList(),
                insights = emptyList()
            )
        }

        val firstTime = snapshots.first().timestampMs
        val lastTime = snapshots.last().timestampMs
        val totalDays = ((lastTime - firstTime).toDouble() / (86_400_000.0)).coerceAtLeast(0.01)

        val temps = snapshots.filter { it.tempDeciCelsius > 0 }.map { it.tempDeciCelsius / 10.0 }
        val voltages = snapshots.filter { it.voltageMv > 0 }.map { it.voltageMv }

        val minTemp = temps.minOrNull() ?: 25.0
        val maxTemp = temps.maxOrNull() ?: 25.0
        val avgTemp = if (temps.isNotEmpty()) (temps.sum() / temps.size * 10.0).roundToInt() / 10.0 else 25.0

        val minVolt = voltages.minOrNull() ?: 3800
        val maxVolt = voltages.maxOrNull() ?: 4300

        // Day-by-day aggregation
        class MutableDaily {
            var startLevel = 100
            var endLevel = 100
            var minLevel = 100
            var maxLevel = 0
            var dropPercent = 0
            var gainPercent = 0
            var dischargeMs = 0L
            var chargeMs = 0L
            var screenOnMs = 0L
            var deepSleepMs = 0L
            var awakeScreenOffMs = 0L
            val dayTemps = mutableListOf<Double>()
            val dayVolts = mutableListOf<Int>()
        }

        val dailyMap = linkedMapOf<String, MutableDaily>()

        var totalDischargeDrop = 0
        var totalChargeGain = 0
        var totalDischargeMs = 0L
        var totalChargeMs = 0L
        var totalScreenOnMs = 0L
        var totalDeepSleepMs = 0L
        var totalAwakeScreenOffMs = 0L

        // Track discharge segments for UID metrics
        data class Segment(val startIdx: Int, val endIdx: Int, val durationMs: Long)
        val dischargeSegments = mutableListOf<Segment>()
        var currentSegmentStart: Int? = null

        for (i in 0 until snapshots.size - 1) {
            val curr = snapshots[i]
            val next = snapshots[i + 1]

            val dateStr = dateFormat.format(Date(curr.timestampMs))
            val daily = dailyMap.getOrPut(dateStr) { MutableDaily().apply { startLevel = curr.batteryLevel } }
            daily.endLevel = next.batteryLevel
            daily.minLevel = minOf(daily.minLevel, curr.batteryLevel)
            daily.maxLevel = maxOf(daily.maxLevel, curr.batteryLevel)
            if (curr.tempDeciCelsius > 0) daily.dayTemps.add(curr.tempDeciCelsius / 10.0)
            if (curr.voltageMv > 0) daily.dayVolts.add(curr.voltageMv)

            val deltaLevel = next.batteryLevel - curr.batteryLevel
            val deltaDuration = (next.timestampMs - curr.timestampMs).coerceAtLeast(0L)
            val isCharging = curr.isCharging || next.isCharging || deltaLevel > 0
            val isReboot = next.totalRealtimeMs < curr.totalRealtimeMs

            if (isCharging || isReboot) {
                if (deltaLevel > 0) {
                    totalChargeGain += deltaLevel
                    daily.gainPercent += deltaLevel
                }
                totalChargeMs += deltaDuration
                daily.chargeMs += deltaDuration

                if (currentSegmentStart != null) {
                    if (currentSegmentStart != i) {
                        val segDur = (snapshots[i].timestampMs - snapshots[currentSegmentStart].timestampMs).coerceAtLeast(0L)
                        dischargeSegments.add(Segment(currentSegmentStart, i, segDur))
                    }
                    currentSegmentStart = null
                }
            } else {
                // Discharging
                val drop = (curr.batteryLevel - next.batteryLevel).coerceAtLeast(0)
                totalDischargeDrop += drop
                daily.dropPercent += drop

                totalDischargeMs += deltaDuration
                daily.dischargeMs += deltaDuration

                val rawRealtime = (next.totalRealtimeMs - curr.totalRealtimeMs).coerceAtLeast(0L)
                val dRealtime = if (deltaDuration > 0L && rawRealtime > deltaDuration) deltaDuration else rawRealtime
                val rawUptime = (next.totalUptimeMs - curr.totalUptimeMs).coerceAtLeast(0L)
                val dUptime = rawUptime.coerceAtMost(dRealtime)
                val rawScreenOn = (next.screenOnDurationMs - curr.screenOnDurationMs).coerceAtLeast(0L)
                val dScreenOn = rawScreenOn.coerceAtMost(dRealtime)

                val sliceDeepSleep = (dRealtime - dUptime).coerceAtLeast(0L)
                val sliceAwake = (dUptime - dScreenOn).coerceAtLeast(0L)

                totalScreenOnMs += dScreenOn
                totalDeepSleepMs += sliceDeepSleep
                totalAwakeScreenOffMs += sliceAwake

                daily.screenOnMs += dScreenOn
                daily.deepSleepMs += sliceDeepSleep
                daily.awakeScreenOffMs += sliceAwake

                if (currentSegmentStart == null) {
                    currentSegmentStart = i
                }
            }
        }

        if (currentSegmentStart != null && currentSegmentStart != snapshots.size - 1) {
            val lastIdx = snapshots.size - 1
            val segDur = (snapshots[lastIdx].timestampMs - snapshots[currentSegmentStart].timestampMs).coerceAtLeast(0L)
            dischargeSegments.add(Segment(currentSegmentStart, lastIdx, segDur))
        }

        // Finalize daily summaries
        val dailySummaries = dailyMap.map { (dateStr, d) ->
            val disH = d.dischargeMs.toDouble() / 3_600_000.0
            val rate = if (disH > 0.05) ((d.dropPercent / disH) * 100.0).roundToInt() / 100.0 else 0.0
            val screenOffMs = (d.dischargeMs - d.screenOnMs).coerceAtLeast(0L)
            val eff = if (screenOffMs > 0) ((d.deepSleepMs.toDouble() / screenOffMs.toDouble()) * 100.0).coerceIn(0.0, 100.0) else 100.0

            DailyAnalyticsSummary(
                dateString = dateStr,
                startLevel = d.startLevel,
                endLevel = d.endLevel,
                minLevel = if (d.minLevel <= 100) d.minLevel else d.endLevel,
                maxLevel = maxOf(d.maxLevel, d.startLevel),
                dropPercent = d.dropPercent,
                gainPercent = d.gainPercent,
                dischargeHours = (disH * 10.0).roundToInt() / 10.0,
                deepSleepHours = (d.deepSleepMs.toDouble() / 3_600_000.0 * 10.0).roundToInt() / 10.0,
                awakeScreenOffHours = (d.awakeScreenOffMs.toDouble() / 3_600_000.0 * 10.0).roundToInt() / 10.0,
                screenOnHours = (d.screenOnMs.toDouble() / 3_600_000.0 * 10.0).roundToInt() / 10.0,
                deepSleepEfficiency = (eff * 10.0).roundToInt() / 10.0,
                drainRatePerHour = rate,
                minTempC = if (d.dayTemps.isNotEmpty()) (d.dayTemps.minOrNull() ?: 25.0) else 25.0,
                maxTempC = if (d.dayTemps.isNotEmpty()) (d.dayTemps.maxOrNull() ?: 25.0) else 25.0,
                avgTempC = if (d.dayTemps.isNotEmpty()) (d.dayTemps.sum() / d.dayTemps.size * 10.0).roundToInt() / 10.0 else 25.0,
                minVoltageMv = if (d.dayVolts.isNotEmpty()) (d.dayVolts.minOrNull() ?: 3800) else 3800,
                maxVoltageMv = if (d.dayVolts.isNotEmpty()) (d.dayVolts.maxOrNull() ?: 4200) else 4200
            )
        }

        // Aggregate UID deltas across discharge segments
        class UidAgg {
            var cpuUser = 0L
            var cpuSys = 0L
            var wlDuration = 0L
            var wlCount = 0
            var radioMs = 0L
            var wifiRx = 0L
            var wifiTx = 0L
            var wifiScans = 0
            var wifiRunMs = 0L
            var gpsMs = 0L
            val tags = mutableMapOf<String, Long>()
        }

        val uidDeltas = mutableMapOf<Int, UidAgg>()
        val packageAlarms = mutableMapOf<String, Int>()
        val packageAlarmActions = mutableMapOf<String, MutableMap<String, Int>>()

        var wifiSegmentMs = 0L
        var cellSegmentMs = 0L
        var wifiSegmentDrop = 0
        var cellSegmentDrop = 0
        var totalWifiBytesAgg = 0L
        var totalCellBytesAgg = 0L

        for (segment in dischargeSegments) {
            val startEntity = boundarySnapshots[snapshots[segment.startIdx].id]
            val endEntity = boundarySnapshots[snapshots[segment.endIdx].id]
            if (startEntity != null && endEntity != null) {
                val sUids = SnapshotSerializer.deserializeUidMetrics(startEntity.uidMetricsJson)
                val eUids = SnapshotSerializer.deserializeUidMetrics(endEntity.uidMetricsJson)
                val sAl = SnapshotSerializer.deserializePackageAlarms(startEntity.packageAlarmsJson)
                val eAl = SnapshotSerializer.deserializePackageAlarms(endEntity.packageAlarmsJson)

                val maxCpu = maxOf(segment.durationMs * 8, 60_000L)
                val maxHw = maxOf(segment.durationMs, 60_000L)

                var segWifiBytes = 0L
                var segCellBytes = 0L
                var segRadioMs = 0L

                for (uid in (sUids.keys + eUids.keys)) {
                    val c = sUids[uid] ?: UidMetrics(uid)
                    val n = eUids[uid] ?: UidMetrics(uid)

                    val dUser = (n.cpuUserTimeMs - c.cpuUserTimeMs).coerceIn(0L, maxCpu)
                    val dSys = (n.cpuSystemTimeMs - c.cpuSystemTimeMs).coerceIn(0L, maxCpu)
                    val dWl = (n.partialWakelockDurationMs - c.partialWakelockDurationMs).coerceIn(0L, maxHw)
                    val dWlCnt = (n.wakelockCount - c.wakelockCount).coerceAtLeast(0)
                    val dRadio = (n.mobileRadioActiveMs - c.mobileRadioActiveMs).coerceIn(0L, maxHw)

                    val dWifiRx = (n.wifiBytesRx - c.wifiBytesRx).coerceAtLeast(0L)
                    val dWifiTx = (n.wifiBytesTx - c.wifiBytesTx).coerceAtLeast(0L)
                    val rawWifiScans = (n.wifiScanCount - c.wifiScanCount).coerceAtLeast(0)
                    val dWifiScans = (if (rawWifiScans > 50) minOf((rawWifiScans / 1_000_000L).toInt(), 10) else rawWifiScans).coerceIn(0, 10)
                    val rawWifiRun = (n.wifiRunningMs - c.wifiRunningMs).coerceAtLeast(0L)
                    val dWifiRun = (if (rawWifiRun > maxHw * 500) rawWifiRun / 1000L else rawWifiRun).coerceIn(0L, maxHw)
                    val dGps = (n.gpsDurationMs - c.gpsDurationMs).coerceIn(0L, maxHw)

                    val dMobRx = (n.mobileBytesRx - c.mobileBytesRx).coerceAtLeast(0L)
                    val dMobTx = (n.mobileBytesTx - c.mobileBytesTx).coerceAtLeast(0L)
                    segWifiBytes += dWifiRx + dWifiTx
                    segCellBytes += dMobRx + dMobTx
                    segRadioMs += dRadio

                    val agg = uidDeltas.getOrPut(uid) { UidAgg() }
                    agg.cpuUser += dUser
                    agg.cpuSys += dSys
                    agg.wlDuration += dWl
                    agg.wlCount += dWlCnt
                    agg.radioMs += dRadio
                    agg.wifiRx += dWifiRx
                    agg.wifiTx += dWifiTx
                    agg.wifiScans += dWifiScans
                    agg.wifiRunMs += dWifiRun
                    agg.gpsMs += dGps

                    for ((tName, tStat) in n.wakelockTags) {
                        val prev = c.wakelockTags[tName]?.durationMs ?: 0L
                        val diff = (tStat.durationMs - prev).coerceIn(0L, maxHw)
                        if (diff > 0) {
                            agg.tags[tName] = (agg.tags[tName] ?: 0L) + diff
                        }
                    }
                }

                totalWifiBytesAgg += segWifiBytes
                totalCellBytesAgg += segCellBytes

                val segDrop = (snapshots[segment.startIdx].batteryLevel - snapshots[segment.endIdx].batteryLevel).coerceAtLeast(0)

                // Determine Wi-Fi vs Cellular time allocation for this segment:
                // When connected to Wi-Fi at home, background carrier traffic (VoLTE/IMS/MMS) is typically < 2 MB.
                // Significant cellular usage (> 2 MB or mobile radio active > 10 min without Wi-Fi) indicates active cellular usage.
                val isStrictlyWifi = segCellBytes <= 2_000_000L && (segWifiBytes > 5_000_000L || segCellBytes == 0L)
                val isStrictlyCellular = segCellBytes > 2_000_000L && segWifiBytes <= 5_000_000L

                val cellMs: Long
                val wifiMs: Long
                val cellDropPortion: Int
                val wifiDropPortion: Int

                if (isStrictlyWifi) {
                    cellMs = 0L
                    wifiMs = segment.durationMs
                    cellDropPortion = 0
                    wifiDropPortion = segDrop
                } else if (isStrictlyCellular) {
                    cellMs = segment.durationMs
                    wifiMs = 0L
                    cellDropPortion = segDrop
                    wifiDropPortion = 0
                } else if (segCellBytes > 2_000_000L && segWifiBytes > 5_000_000L) {
                    // Mixed segment: user spent part of the time on Wi-Fi and part out on Cellular.
                    // Estimate cellular active time based on mobile radio active duration (typical duty cycle ~40% during mobile use)
                    // or data volume (~20 MB/hr mobile baseline), capped within segment duration.
                    val estCellMsFromRadio = (segRadioMs * 2.5).toLong()
                    val estCellMsFromBytes = ((segCellBytes.toDouble() / (20.0 * 1024.0 * 1024.0)) * 3_600_000.0).toLong()
                    val estCellMs = maxOf(estCellMsFromRadio, estCellMsFromBytes).coerceIn(600_000L, segment.durationMs)
                    cellMs = estCellMs
                    wifiMs = (segment.durationMs - cellMs).coerceAtLeast(0L)

                    val cellRatio = if (segment.durationMs > 0L) cellMs.toDouble() / segment.durationMs.toDouble() else 0.0
                    cellDropPortion = (segDrop * cellRatio).roundToInt().coerceIn(0, segDrop)
                    wifiDropPortion = (segDrop - cellDropPortion).coerceAtLeast(0)
                } else {
                    // Default idle standby without notable data on either interface: default to Wi-Fi unless radio was heavily active
                    if (segRadioMs > 60_000L && segWifiBytes == 0L) {
                        cellMs = segment.durationMs
                        wifiMs = 0L
                        cellDropPortion = segDrop
                        wifiDropPortion = 0
                    } else {
                        cellMs = 0L
                        wifiMs = segment.durationMs
                        cellDropPortion = 0
                        wifiDropPortion = segDrop
                    }
                }

                cellSegmentMs += cellMs
                wifiSegmentMs += wifiMs
                cellSegmentDrop += cellDropPortion
                wifiSegmentDrop += wifiDropPortion

                for ((pkg, nStat) in eAl) {
                    val cStat = sAl[pkg]
                    val dWake = if (cStat != null) (nStat.wakeupCount - cStat.wakeupCount).coerceAtLeast(0) else nStat.wakeupCount
                    if (dWake > 0) {
                        packageAlarms[pkg] = (packageAlarms[pkg] ?: 0) + dWake
                    }
                    val actions = packageAlarmActions.getOrPut(pkg) { mutableMapOf() }
                    nStat.topAlarmActions.forEach { (action, count) ->
                        val prev = cStat?.topAlarmActions?.get(action) ?: 0
                        val dAct = (count - prev).coerceAtLeast(0)
                        if (dAct > 0) {
                            actions[action] = (actions[action] ?: 0) + dAct
                        }
                    }
                }
            }
        }

        // Hardware attribution totals
        var totCpuMah = 0.0
        var totWlMah = 0.0
        var totRadioMah = 0.0
        var totWifiMah = 0.0
        var totGpsMah = 0.0

        val wakelockLeaders = mutableListOf<WakelockLeader>()

        for ((uid, delta) in uidDeltas) {
            val uidMetric = UidMetrics(
                uid = uid,
                cpuUserTimeMs = delta.cpuUser,
                cpuSystemTimeMs = delta.cpuSys,
                partialWakelockDurationMs = delta.wlDuration,
                wakelockCount = delta.wlCount,
                mobileRadioActiveMs = delta.radioMs,
                wifiScanCount = delta.wifiScans,
                wifiRunningMs = delta.wifiRunMs,
                wifiBytesRx = delta.wifiRx,
                wifiBytesTx = delta.wifiTx,
                gpsDurationMs = delta.gpsMs
            )
            val (_, breakdown) = attributionModel.calculateEnergy(uidMetric)
            totCpuMah += breakdown.cpuMah
            totWlMah += breakdown.wakelockMah
            totRadioMah += breakdown.mobileRadioMah
            totWifiMah += breakdown.wifiMah
            totGpsMah += breakdown.gpsMah

            if (delta.wlDuration > 10_000L || delta.wlCount > 5) {
                val resolved = uidResolver.resolve(uid)
                val topTag = delta.tags.maxByOrNull { it.value }?.key ?: ""
                val savingsMah = ((delta.wlDuration / 3_600_000.0) * powerProfile.cpuAwakeMa * 10.0).roundToInt() / 10.0
                val savingsPct = ((savingsMah / powerProfile.batteryCapacityMah * 100.0) * 10.0).roundToInt() / 10.0
                wakelockLeaders.add(
                    WakelockLeader(
                        uid = uid,
                        packageName = resolved.primaryPackageName,
                        appLabel = resolved.appLabel,
                        isSystem = resolved.isSystemApp,
                        durationMs = delta.wlDuration,
                        count = delta.wlCount,
                        topTag = topTag,
                        potentialSavingsMah = savingsMah,
                        potentialSavingsPercent = savingsPct
                    )
                )
            }
        }

        wakelockLeaders.sortByDescending { it.durationMs }

        val alarmLeaders = packageAlarms.map { (pkg, wakeups) ->
            val topAction = packageAlarmActions[pkg]?.maxByOrNull { it.value }?.key ?: ""
            val savingsMah = ((wakeups * 0.05) * 10.0).roundToInt() / 10.0
            val savingsPct = ((savingsMah / powerProfile.batteryCapacityMah * 100.0) * 10.0).roundToInt() / 10.0
            AlarmLeader(
                packageName = pkg,
                appLabel = pkg.substringAfterLast('.').replaceFirstChar { it.uppercase() },
                isSystem = pkg.startsWith("com.android.") || pkg.startsWith("com.google.android.") && !pkg.contains("apps"),
                wakeupCount = wakeups,
                topAction = topAction,
                potentialSavingsMah = savingsMah,
                potentialSavingsPercent = savingsPct
            )
        }.sortedByDescending { it.wakeupCount }

        val totHardwareMah = totCpuMah + totWlMah + totRadioMah + totWifiMah + totGpsMah
        val hardwareBreakdown = HardwareEnergyBreakdown(
            cpuMah = (totCpuMah * 10.0).roundToInt() / 10.0,
            wakelockMah = (totWlMah * 10.0).roundToInt() / 10.0,
            radioMah = (totRadioMah * 10.0).roundToInt() / 10.0,
            wifiMah = (totWifiMah * 10.0).roundToInt() / 10.0,
            gpsMah = (totGpsMah * 10.0).roundToInt() / 10.0,
            totalMah = (totHardwareMah * 10.0).roundToInt() / 10.0
        )

        val disHours = totalDischargeMs.toDouble() / 3_600_000.0
        val avgDrainRate = if (disHours > 0.05) ((totalDischargeDrop / disHours) * 100.0).roundToInt() / 100.0 else 0.0
        val screenOffTotal = (totalDischargeMs - totalScreenOnMs).coerceAtLeast(0L)
        val overallSleepEff = if (screenOffTotal > 0) ((totalDeepSleepMs.toDouble() / screenOffTotal.toDouble()) * 100.0).coerceIn(0.0, 100.0) else 100.0

        val wifiH = wifiSegmentMs.toDouble() / 3_600_000.0
        val cellH = cellSegmentMs.toDouble() / 3_600_000.0
        val wifiRate = if (wifiH > 0.05) ((wifiSegmentDrop / wifiH) * 100.0).roundToInt() / 100.0 else 0.0
        val cellRate = if (cellH > 0.05) ((cellSegmentDrop / cellH) * 100.0).roundToInt() / 100.0 else 0.0
        val penalty = if (wifiRate > 0.1) ((cellRate / wifiRate) * 10.0).roundToInt() / 10.0 else 1.0

        val networkComparison = NetworkDrainComparison(
            wifiHours = (wifiH * 10.0).roundToInt() / 10.0,
            cellularHours = (cellH * 10.0).roundToInt() / 10.0,
            wifiDataMb = ((totalWifiBytesAgg / (1024.0 * 1024.0)) * 10.0).roundToInt() / 10.0,
            cellularDataMb = ((totalCellBytesAgg / (1024.0 * 1024.0)) * 10.0).roundToInt() / 10.0,
            wifiEnergyMah = (totWifiMah * 10.0).roundToInt() / 10.0,
            cellularEnergyMah = (totRadioMah * 10.0).roundToInt() / 10.0,
            wifiDrainRatePerHour = wifiRate,
            cellularDrainRatePerHour = cellRate,
            cellularPenaltyMultiplier = penalty
        )

        // Build actionable insights based on evidence
        val insights = mutableListOf<ActionableInsight>()

        // 1. Standby / Doze Efficiency Insight
        if (overallSleepEff >= 75.0) {
            insights.add(
                ActionableInsight(
                    title = "Optimal Doze Efficiency (${(overallSleepEff).roundToInt()}%)",
                    description = "Your device sleeps soundly during screen-off intervals with minimal parasitic drain.",
                    severity = InsightSeverity.SUCCESS
                )
            )
        } else if (overallSleepEff >= 60.0) {
            insights.add(
                ActionableInsight(
                    title = "Moderate Sleep Efficiency (${(overallSleepEff).roundToInt()}%)",
                    description = "Your phone spends ${(totalAwakeScreenOffMs / 3_600_000.0 * 10.0).roundToInt() / 10.0}h awake in standby. Background sync and wakelocks are moderately degrading battery life.",
                    severity = InsightSeverity.WARNING
                )
            )
        } else {
            insights.add(
                ActionableInsight(
                    title = "Doze Inhibited (${(overallSleepEff).roundToInt()}%)",
                    description = "Phone is awake for more than 40% of screen-off time. Heavy background activity is preventing deep sleep.",
                    severity = InsightSeverity.ALERT
                )
            )
        }

        // 2. Wakelock Offender Insight
        val topWl = wakelockLeaders.firstOrNull { !it.isSystem }
        if (topWl != null && topWl.durationMs > 1_800_000L) {
            val mins = topWl.durationMs / 60_000L
            insights.add(
                ActionableInsight(
                    title = "Persistent Wakelock: ${topWl.appLabel}",
                    description = "${topWl.appLabel} held the CPU awake for $mins minutes across ${topWl.count} events (${topWl.topTag.take(40)}). Restricting background activity for this app will save noticeable power (~${topWl.potentialSavingsMah.toInt()} mAh).",
                    severity = InsightSeverity.ALERT
                )
            )
        }

        // 3. Alarm Storm Insight
        val topAlarm = alarmLeaders.firstOrNull()
        if (topAlarm != null && topAlarm.wakeupCount > 500) {
            insights.add(
                ActionableInsight(
                    title = "Alarm Storm: ${topAlarm.packageName}",
                    description = "Generated ${topAlarm.wakeupCount} hardware wakeups, periodically dragging the processor out of low-power suspend.",
                    severity = InsightSeverity.WARNING
                )
            )
        }

        // 4. Cellular vs Wi-Fi Standby Penalty Insight
        if (cellH > 2.0 && penalty > 1.05) {
            insights.add(
                ActionableInsight(
                    title = "Cellular Standby Penalty (${penalty}x vs Wi-Fi)",
                    description = "Mobile radio standby drains ${cellRate}%/hr compared to ${wifiRate}%/hr on Wi-Fi. Staying on Wi-Fi saves noticeable standby power.",
                    severity = InsightSeverity.INFO
                )
            )
        }

        // 5. Thermal Insight
        if (maxTemp > 42.0) {
            insights.add(
                ActionableInsight(
                    title = "Elevated Battery Temperatures (${maxTemp}°C)",
                    description = "Peak thermal events reached ${maxTemp}°C. Avoiding fast charging under heavy usage will preserve battery longevity.",
                    severity = InsightSeverity.WARNING
                )
            )
        } else {
            insights.add(
                ActionableInsight(
                    title = "Excellent Battery Thermals (Avg ${avgTemp}°C)",
                    description = "Cell temperature stayed well below thermal throttling limits (peak ${maxTemp}°C), optimal for battery lifespan.",
                    severity = InsightSeverity.SUCCESS
                )
            )
        }

        return OverallAnalytics(
            totalDays = (totalDays * 10.0).roundToInt() / 10.0,
            totalSnapshots = snapshots.size,
            totalDischargeDrop = totalDischargeDrop,
            totalChargeGain = totalChargeGain,
            totalDischargeHours = (disHours * 10.0).roundToInt() / 10.0,
            totalChargeHours = (totalChargeMs.toDouble() / 3_600_000.0 * 10.0).roundToInt() / 10.0,
            avgDrainRatePerHour = avgDrainRate,
            avgDeepSleepEfficiency = (overallSleepEff * 10.0).roundToInt() / 10.0,
            minTempC = minTemp,
            maxTempC = maxTemp,
            avgTempC = avgTemp,
            minVoltageMv = minVolt,
            maxVoltageMv = maxVolt,
            estimatedCapacityMah = powerProfile.batteryCapacityMah,
            dailySummaries = dailySummaries,
            hardwareEnergy = hardwareBreakdown,
            networkComparison = networkComparison,
            topWakelockLeaders = wakelockLeaders.take(15),
            topAlarmLeaders = alarmLeaders.take(15),
            insights = insights
        )
    }
}
