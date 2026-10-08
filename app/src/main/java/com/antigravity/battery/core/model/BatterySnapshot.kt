package com.antigravity.battery.core.model

/**
 * Immutable snapshot of system battery and hardware states at a given timestamp.
 */
data class BatterySnapshot(
    val timestampMs: Long,
    val deviceMetrics: DeviceLevelMetrics,
    val uidMetricsMap: Map<Int, UidMetrics>,
    val packageAlarms: Map<String, PackageAlarmMetrics>
)

data class DeviceLevelMetrics(
    val timestampMs: Long,
    val batteryLevelPercent: Int,
    val voltageMv: Int,
    val temperatureDeciCelsius: Int, // e.g. 312 for 31.2°C
    val isScreenOn: Boolean,
    val totalRealtimeMs: Long,
    val totalUptimeMs: Long,
    val screenOnDurationMs: Long,
    val screenOffDurationMs: Long
)

data class UidMetrics(
    val uid: Int,
    val cpuUserTimeMs: Long = 0L,
    val cpuSystemTimeMs: Long = 0L,
    val cpuClusterTimesMs: List<Long> = emptyList(), // per-cluster or frequency bucket ms
    val partialWakelockDurationMs: Long = 0L,
    val wakelockCount: Int = 0,
    val mobileRadioActiveMs: Long = 0L,
    val mobileRadioActiveCount: Int = 0,
    val mobileBytesRx: Long = 0L,
    val mobileBytesTx: Long = 0L,
    val wifiScanCount: Int = 0,
    val wifiRunningMs: Long = 0L,
    val wifiBytesRx: Long = 0L,
    val wifiBytesTx: Long = 0L,
    val gpsDurationMs: Long = 0L,
    val wakelockTags: Map<String, WakelockTagStat> = emptyMap()
)

data class WakelockTagStat(
    val tag: String,
    val durationMs: Long,
    val count: Int
)

data class PackageAlarmMetrics(
    val packageName: String,
    val wakeupCount: Int,
    val nonWakeupCount: Int = 0,
    val topAlarmActions: Map<String, Int> = emptyMap()
)
