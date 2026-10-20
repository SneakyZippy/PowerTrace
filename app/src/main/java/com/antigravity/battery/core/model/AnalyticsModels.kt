package com.antigravity.battery.core.model

enum class InsightSeverity {
    SUCCESS,
    INFO,
    WARNING,
    ALERT
}

data class ActionableInsight(
    val title: String,
    val description: String,
    val severity: InsightSeverity
)

data class DailyAnalyticsSummary(
    val dateString: String,
    val startLevel: Int,
    val endLevel: Int,
    val minLevel: Int,
    val maxLevel: Int,
    val dropPercent: Int,
    val gainPercent: Int,
    val dischargeHours: Double,
    val deepSleepHours: Double,
    val awakeScreenOffHours: Double,
    val screenOnHours: Double,
    val deepSleepEfficiency: Double,
    val drainRatePerHour: Double,
    val minTempC: Double,
    val maxTempC: Double,
    val avgTempC: Double,
    val minVoltageMv: Int,
    val maxVoltageMv: Int
)

data class HardwareEnergyBreakdown(
    val cpuMah: Double,
    val wakelockMah: Double,
    val radioMah: Double,
    val wifiMah: Double,
    val gpsMah: Double,
    val totalMah: Double
)

data class NetworkDrainComparison(
    val wifiHours: Double = 0.0,
    val cellularHours: Double = 0.0,
    val wifiDataMb: Double = 0.0,
    val cellularDataMb: Double = 0.0,
    val wifiEnergyMah: Double = 0.0,
    val cellularEnergyMah: Double = 0.0,
    val wifiDrainRatePerHour: Double = 0.0,
    val cellularDrainRatePerHour: Double = 0.0,
    val cellularPenaltyMultiplier: Double = 1.0
)

data class WakelockLeader(
    val uid: Int,
    val packageName: String,
    val appLabel: String,
    val isSystem: Boolean,
    val durationMs: Long,
    val count: Int,
    val topTag: String,
    val potentialSavingsMah: Double = 0.0,
    val potentialSavingsPercent: Double = 0.0
)

data class AlarmLeader(
    val packageName: String,
    val appLabel: String,
    val isSystem: Boolean,
    val wakeupCount: Int,
    val topAction: String,
    val potentialSavingsMah: Double = 0.0,
    val potentialSavingsPercent: Double = 0.0
)

data class OverallAnalytics(
    val totalDays: Double,
    val totalSnapshots: Int,
    val totalDischargeDrop: Int,
    val totalChargeGain: Int,
    val totalDischargeHours: Double,
    val totalChargeHours: Double,
    val avgDrainRatePerHour: Double,
    val avgDeepSleepEfficiency: Double,
    val minTempC: Double,
    val maxTempC: Double,
    val avgTempC: Double,
    val minVoltageMv: Int,
    val maxVoltageMv: Int,
    val estimatedCapacityMah: Double,
    val dailySummaries: List<DailyAnalyticsSummary>,
    val hardwareEnergy: HardwareEnergyBreakdown,
    val networkComparison: NetworkDrainComparison = NetworkDrainComparison(),
    val topWakelockLeaders: List<WakelockLeader>,
    val topAlarmLeaders: List<AlarmLeader>,
    val insights: List<ActionableInsight>
)
