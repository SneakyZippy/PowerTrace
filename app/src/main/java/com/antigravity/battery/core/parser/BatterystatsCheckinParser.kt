package com.antigravity.battery.core.parser

import com.antigravity.battery.core.model.BatterySnapshot
import com.antigravity.battery.core.model.DeviceLevelMetrics
import com.antigravity.battery.core.model.UidMetrics
import com.antigravity.battery.core.model.WakelockTagStat

/**
 * Robust parser for `dumpsys batterystats --checkin` CSV format across Android 8.0 through 15+.
 * Android checkin format syntax:
 *   version,uid,aggregation_type,section_name,param1,param2,...
 */
class BatterystatsCheckinParser {

    fun parse(
        rawCheckin: String,
        timestampMs: Long = System.currentTimeMillis()
    ): BatterySnapshot {
        var deviceMetrics = DeviceLevelMetrics(
            timestampMs = timestampMs,
            batteryLevelPercent = 100,
            voltageMv = 4000,
            temperatureDeciCelsius = 250,
            isScreenOn = false,
            totalRealtimeMs = 0L,
            totalUptimeMs = 0L,
            screenOnDurationMs = 0L,
            screenOffDurationMs = 0L
        )

        val uidMetricsBuilder = mutableMapOf<Int, MutableUidStat>()

        rawCheckin.lineSequence().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) return@forEach

            val parts = trimmed.split(",")
            if (parts.size < 4) return@forEach

            val uid = parts[1].toIntOrNull() ?: return@forEach
            val section = parts[3]

            when {
                // Device-level summary section: 'bt' (battery lifetime)
                uid == 0 && section == "bt" -> {
                    deviceMetrics = parseBatterySection(parts, timestampMs)
                }

                // UID-level Partial / Background Wakelocks: 'wl'
                section == "wl" && parts.size >= 5 -> {
                    val tag = parts[4]
                    var durationMs = 0L
                    var count = 0

                    val pIndex = parts.indexOf("p")
                    if (pIndex != -1 && pIndex + 1 < parts.size) {
                        count = parts.getOrNull(pIndex + 1)?.toIntOrNull() ?: 0
                        // In standard checkin: "p", count, cur, max, min, total
                        durationMs = parts.getOrNull(pIndex + 5)?.toLongOrNull()
                            ?: parts.getOrNull(pIndex + 4)?.toLongOrNull()
                            ?: parts.getOrNull(pIndex + 3)?.toLongOrNull()
                            ?: 0L
                    } else if (parts.size >= 6) {
                        durationMs = parts[5].toLongOrNull() ?: 0L
                        count = parts.getOrNull(6)?.toIntOrNull() ?: 1
                    }

                    if (durationMs > 0L || count > 0) {
                        val stat = uidMetricsBuilder.getOrPut(uid) { MutableUidStat(uid) }
                        stat.partialWakelockDurationMs += durationMs
                        stat.wakelockCount += count
                        val existingTag = stat.wakelockTags[tag]
                        stat.wakelockTags[tag] = WakelockTagStat(
                            tag = tag,
                            durationMs = (existingTag?.durationMs ?: 0L) + durationMs,
                            count = (existingTag?.count ?: 0) + count
                        )
                    }
                }

                // UID-level Aggregated Wakelock: 'awl' (e.g. 9,uid,l,awl,totalFullMs,totalPartialMs)
                section == "awl" && parts.size >= 5 -> {
                    val awlDuration = parts.getOrNull(5)?.toLongOrNull()
                        ?: parts.getOrNull(4)?.toLongOrNull() ?: 0L
                    if (awlDuration > 0L) {
                        val stat = uidMetricsBuilder.getOrPut(uid) { MutableUidStat(uid) }
                        if (stat.partialWakelockDurationMs == 0L) {
                            stat.partialWakelockDurationMs = awlDuration
                        }
                    }
                }

                // UID-level CPU time: 'cpu' or legacy 'cpr'
                (section == "cpu" || section == "cpr") && parts.size >= 6 -> {
                    val userTimeMs = parts[4].toLongOrNull() ?: 0L
                    val systemTimeMs = parts[5].toLongOrNull() ?: 0L
                    val clusterTimes = mutableListOf<Long>()
                    for (i in 6 until parts.size) {
                        parts[i].toLongOrNull()?.let { clusterTimes.add(it) }
                    }

                    val stat = uidMetricsBuilder.getOrPut(uid) { MutableUidStat(uid) }
                    stat.cpuUserTimeMs += userTimeMs
                    stat.cpuSystemTimeMs += systemTimeMs
                    if (clusterTimes.isNotEmpty()) {
                        stat.cpuClusterTimesMs = clusterTimes
                    }
                }

                // UID-level Network Radio: 'nt'
                section == "nt" && parts.size >= 5 -> {
                    // In AOSP Batterystats checkin format:
                    // parts[4] = mobileBytesRx
                    // parts[5] = mobileBytesTx
                    // parts[6] = wifiBytesRx
                    // parts[7] = wifiBytesTx
                    // parts[8] = mobilePacketsRx
                    // parts[9] = mobilePacketsTx
                    // parts[10] = wifiPacketsRx
                    // parts[11] = wifiPacketsTx
                    // parts[12] = mobileActiveTime (reported in microseconds by BatteryStats).
                    // parts[13] = mobileActiveCount.
                    val mobRx = parts.getOrNull(4)?.toLongOrNull() ?: 0L
                    val mobTx = parts.getOrNull(5)?.toLongOrNull() ?: 0L
                    val wifiRx = parts.getOrNull(6)?.toLongOrNull() ?: 0L
                    val wifiTx = parts.getOrNull(7)?.toLongOrNull() ?: 0L

                    val mobileRadioActiveUs = if (parts.size >= 13) parts[12].toLongOrNull() ?: 0L else 0L
                    val mobileRadioActiveMs = if (mobileRadioActiveUs > 0L) mobileRadioActiveUs / 1000L else 0L
                    val activeCount = if (parts.size >= 14) parts[13].toIntOrNull() ?: 1 else 1

                    val stat = uidMetricsBuilder.getOrPut(uid) { MutableUidStat(uid) }
                    stat.mobileRadioActiveMs += mobileRadioActiveMs
                    stat.mobileRadioActiveCount += activeCount
                    stat.mobileBytesRx += mobRx
                    stat.mobileBytesTx += mobTx
                    stat.wifiBytesRx += wifiRx
                    stat.wifiBytesTx += wifiTx
                }

                // UID-level Wi-Fi scan: 'wfl'
                section == "wfl" && parts.size >= 5 -> {
                    val wifiScanCount = parts[4].toIntOrNull() ?: 0
                    val wifiRunningMs = parts.getOrNull(5)?.toLongOrNull() ?: 0L
                    val stat = uidMetricsBuilder.getOrPut(uid) { MutableUidStat(uid) }
                    stat.wifiScanCount += wifiScanCount
                    stat.wifiRunningMs += wifiRunningMs
                }

                // UID-level GPS: 'gprs'
                section == "gprs" && parts.size >= 5 -> {
                    val gpsDurationMs = parts[4].toLongOrNull() ?: 0L
                    val stat = uidMetricsBuilder.getOrPut(uid) { MutableUidStat(uid) }
                    stat.gpsDurationMs += gpsDurationMs
                }
            }
        }

        val finalUidMap = uidMetricsBuilder.mapValues { (_, v) -> v.toImmutable() }

        return BatterySnapshot(
            timestampMs = timestampMs,
            deviceMetrics = deviceMetrics,
            uidMetricsMap = finalUidMap,
            packageAlarms = emptyMap()
        )
    }

    private fun parseBatterySection(parts: List<String>, nowMs: Long): DeviceLevelMetrics {
        // Handle which parameter offset if parts[4] is short (0, 7 etc.)
        val isWhichOffset = (parts.getOrNull(4)?.length ?: 0) <= 2
        val realtimeIndex = if (isWhichOffset) 5 else 4
        val uptimeIndex = if (isWhichOffset) 6 else 5
        val screenOffIndex = if (isWhichOffset) 9 else 6

        val realtimeRaw = parts.getOrNull(realtimeIndex)?.toLongOrNull() ?: 0L
        val uptimeRaw = parts.getOrNull(uptimeIndex)?.toLongOrNull() ?: 0L

        val realtimeMs = if (realtimeRaw > 100_000_000_000L) realtimeRaw / 1000L else realtimeRaw
        val uptimeMs = if (uptimeRaw > 100_000_000_000L) uptimeRaw / 1000L else uptimeRaw

        val screenOffRaw = parts.getOrNull(screenOffIndex)?.toLongOrNull() ?: 0L
        val screenOffDurationMs = if (screenOffRaw > 100_000_000_000L) screenOffRaw / 1000L else screenOffRaw
        val screenOnDurationMs = (realtimeMs - screenOffDurationMs).coerceAtLeast(0L)

        // Try extracting battery level, voltage, temp
        val currentBatteryLevel = parts.getOrNull(11)?.toIntOrNull()
            ?: parts.getOrNull(10)?.toIntOrNull() ?: 100
        val currentVoltageMv = parts.getOrNull(13)?.toIntOrNull()
            ?: parts.getOrNull(12)?.toIntOrNull() ?: 4000
        val currentTemp = parts.getOrNull(15)?.toIntOrNull()
            ?: parts.getOrNull(14)?.toIntOrNull() ?: 280

        return DeviceLevelMetrics(
            timestampMs = nowMs,
            batteryLevelPercent = if (currentBatteryLevel in 0..100) currentBatteryLevel else 100,
            voltageMv = if (currentVoltageMv in 2000..5000) currentVoltageMv else 4000,
            temperatureDeciCelsius = if (currentTemp in 0..1000) currentTemp else 250,
            isScreenOn = screenOnDurationMs > 0 && screenOffDurationMs == 0L,
            totalRealtimeMs = realtimeMs,
            totalUptimeMs = uptimeMs,
            screenOnDurationMs = screenOnDurationMs,
            screenOffDurationMs = screenOffDurationMs
        )
    }

    private class MutableUidStat(val uid: Int) {
        var cpuUserTimeMs: Long = 0L
        var cpuSystemTimeMs: Long = 0L
        var cpuClusterTimesMs: List<Long> = emptyList()
        var partialWakelockDurationMs: Long = 0L
        var wakelockCount: Int = 0
        var mobileRadioActiveMs: Long = 0L
        var mobileRadioActiveCount: Int = 0
        var mobileBytesRx: Long = 0L
        var mobileBytesTx: Long = 0L
        var wifiScanCount: Int = 0
        var wifiRunningMs: Long = 0L
        var wifiBytesRx: Long = 0L
        var wifiBytesTx: Long = 0L
        var gpsDurationMs: Long = 0L
        val wakelockTags = mutableMapOf<String, WakelockTagStat>()

        fun toImmutable(): UidMetrics = UidMetrics(
            uid = uid,
            cpuUserTimeMs = cpuUserTimeMs,
            cpuSystemTimeMs = cpuSystemTimeMs,
            cpuClusterTimesMs = cpuClusterTimesMs,
            partialWakelockDurationMs = partialWakelockDurationMs,
            wakelockCount = wakelockCount,
            mobileRadioActiveMs = mobileRadioActiveMs,
            mobileRadioActiveCount = mobileRadioActiveCount,
            mobileBytesRx = mobileBytesRx,
            mobileBytesTx = mobileBytesTx,
            wifiScanCount = wifiScanCount,
            wifiRunningMs = wifiRunningMs,
            wifiBytesRx = wifiBytesRx,
            wifiBytesTx = wifiBytesTx,
            gpsDurationMs = gpsDurationMs,
            wakelockTags = wakelockTags
        )
    }
}
