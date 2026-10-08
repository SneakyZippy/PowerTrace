package com.antigravity.battery.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * High-resolution periodic snapshot taken every 15-30 minutes and on state transitions
 * (screen on/off, charger connect/disconnect).
 */
@Entity(
    tableName = "periodic_snapshots",
    indices = [Index(value = ["timestampMs"])]
)
data class PeriodicSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMs: Long,
    val batteryLevel: Int,
    val voltageMv: Int,
    val tempDeciCelsius: Int,
    val isCharging: Boolean,
    val isScreenOn: Boolean,
    val totalRealtimeMs: Long,
    val totalUptimeMs: Long,
    val screenOnDurationMs: Long,
    val screenOffDurationMs: Long,
    val uidMetricsJson: String, // Serialized per-UID metrics
    val packageAlarmsJson: String // Serialized wakeup alarms
)

@Entity(tableName = "daily_rollups")
data class DailyRollupEntity(
    @PrimaryKey val dateString: String, // e.g. "2026-10-08"
    val startBattery: Int,
    val endBattery: Int,
    val totalDrainPercent: Int,
    val drainRatePercentPerHour: Double,
    val screenOnDurationMs: Long,
    val deepSleepDurationMs: Long,
    val awakeScreenOffMs: Long,
    val deepSleepEfficiencyPercent: Double,
    val topCulpritsJson: String,
    val updatedAt: Long = System.currentTimeMillis()
)
