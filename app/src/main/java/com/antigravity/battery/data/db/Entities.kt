package com.antigravity.battery.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val durationMs: Long,
    val batteryStartPercent: Int,
    val batteryEndPercent: Int,
    val batteryPercentDrop: Int,
    val drainRatePercentPerHour: Double,
    val deepSleepEfficiencyPercent: Double,
    val deepSleepDurationMs: Long,
    val awakeScreenOffMs: Long,
    val screenOnDurationMs: Long,
    val isInterrupted: Boolean,
    val interruptionReason: String?,
    val culpritsJson: String,
    val createdAt: Long = System.currentTimeMillis()
)
