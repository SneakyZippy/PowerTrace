package com.antigravity.battery.core.model

enum class SessionStatus {
    IDLE,
    RECORDING,
    COMPLETED,
    INTERRUPTED_REBOOT,
    INTERRUPTED_CHARGER
}

data class ObservationSession(
    val id: String,
    val title: String,
    val startTimeMs: Long,
    val endTimeMs: Long? = null,
    val startSnapshot: BatterySnapshot,
    val endSnapshot: BatterySnapshot? = null,
    val status: SessionStatus = SessionStatus.RECORDING,
    val delta: SessionDelta? = null
)

data class SessionDelta(
    val durationMs: Long,
    val batteryLevelStart: Int,
    val batteryLevelEnd: Int,
    val batteryPercentDrop: Int,
    val voltageDropMv: Int,
    val tempDeltaDeciCelsius: Int,
    val drainRatePercentPerHour: Double,
    val realtimeMs: Long,
    val uptimeMs: Long,
    val screenOnDurationMs: Long,
    val screenOffDurationMs: Long,
    val deepSleepDurationMs: Long,
    val awakeScreenOffMs: Long,
    val deepSleepEfficiencyPercent: Double,
    val isInterrupted: Boolean,
    val interruptionReason: String? = null,
    val culprits: List<CulpritApp>
)

data class CulpritApp(
    val uid: Int,
    val primaryPackageName: String,
    val appLabel: String,
    val sharedPackageNames: List<String> = emptyList(),
    val isSystemApp: Boolean = false,
    val totalEstimatedMah: Double,
    val energyBreakdown: EnergyBreakdown,
    val cpuUserTimeMs: Long,
    val cpuSystemTimeMs: Long,
    val partialWakelockMs: Long,
    val wakelockCount: Int,
    val wakeupAlarmCount: Int,
    val mobileRadioActiveMs: Long,
    val wifiScanCount: Int,
    val gpsDurationMs: Long,
    val mobileBytesRx: Long = 0L,
    val mobileBytesTx: Long = 0L,
    val wifiBytesRx: Long = 0L,
    val wifiBytesTx: Long = 0L,
    val topWakelockTags: List<WakelockTagStat> = emptyList(),
    val topAlarmActions: Map<String, Int> = emptyMap(),
    val warningBadges: List<CulpritWarningBadge> = emptyList()
)

data class EnergyBreakdown(
    val cpuMah: Double,
    val wakelockMah: Double,
    val mobileRadioMah: Double,
    val wifiMah: Double,
    val gpsMah: Double
)

enum class CulpritWarningBadge {
    RUNAWAY_WAKELOCK,       // Wakelock held > 10 min while screen off
    AGGRESSIVE_WAKEUPS,     // High frequency wakeup alarms (>12/hr)
    RADIO_KEEP_ALIVE,       // Excessive background radio hold time
    HIGH_BACKGROUND_CPU     // Excessive CPU usage during screen-off session
}
