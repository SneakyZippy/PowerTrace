package com.antigravity.battery.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "power_bricks")
data class PowerBrickEntity(
    @PrimaryKey val id: String,
    val name: String,
    val manufacturer: String,
    val ratedWattage: Float,
    val portType: String,
    val notes: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cables")
data class CableEntity(
    @PrimaryKey val id: String,
    val name: String,
    val manufacturer: String,
    val ratedWattage: Float,
    val lengthMeters: Float,
    val cableType: String,
    val notes: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "charge_test_sessions",
    indices = [
        Index(value = ["brickId"]),
        Index(value = ["cableId"]),
        Index(value = ["startTimeMs"])
    ]
)
data class ChargeTestSessionEntity(
    @PrimaryKey val id: String,
    val brickId: String?,
    val cableId: String?,
    val brickName: String,
    val cableName: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val startBatteryLevel: Int,
    val endBatteryLevel: Int,
    val peakWattage: Float,
    val avgWattage: Float,
    val minVoltageMv: Int,
    val maxVoltageMv: Int,
    val peakCurrentMa: Int,
    val startTempDeciC: Int,
    val maxTempDeciC: Int,
    val isThrottled: Boolean,
    val notes: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "charge_samples",
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["timestampMs"])
    ]
)
data class ChargeSampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val timestampMs: Long,
    val batteryLevel: Int,
    val voltageMv: Int,
    val currentMa: Int,
    val wattage: Float,
    val tempDeciC: Int
)
