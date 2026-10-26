package com.antigravity.battery.core.model

import java.util.UUID

/**
 * Domain model for a power adapter / brick.
 */
data class PowerBrick(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val manufacturer: String = "",
    val ratedWattage: Float = 0f,
    val portType: String = "USB-C", // USB-C, USB-A, Multi-port, Wireless
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Domain model for a USB / charging cable.
 */
data class Cable(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val manufacturer: String = "",
    val ratedWattage: Float = 0f,
    val lengthMeters: Float = 1.0f,
    val cableType: String = "Type-C to Type-C", // C-to-C, A-to-C, Lightning, etc.
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Domain model for a completed or ongoing charge test session.
 */
data class ChargeTestSession(
    val id: String = UUID.randomUUID().toString(),
    val brickId: String? = null,
    val cableId: String? = null,
    val brickName: String = "Unknown Brick",
    val cableName: String = "Unknown Cable",
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
    val isThrottled: Boolean = false,
    val notes: String = ""
) {
    val durationMs: Long get() = (endTimeMs - startTimeMs).coerceAtLeast(0L)
    val batteryGained: Int get() = (endBatteryLevel - startBatteryLevel).coerceAtLeast(0)
    val startTempCelsius: Float get() = startTempDeciC / 10.0f
    val maxTempCelsius: Float get() = maxTempDeciC / 10.0f
}

/**
 * High-frequency snapshot during charging.
 */
data class ChargeTelemetrySample(
    val id: Long = 0,
    val sessionId: String,
    val timestampMs: Long,
    val batteryLevel: Int,
    val voltageMv: Int,
    val currentMa: Int,
    val wattage: Float,
    val tempDeciC: Int
) {
    val tempCelsius: Float get() = tempDeciC / 10.0f
    val voltageV: Float get() = voltageMv / 1000.0f
}

/**
 * Information passed when a recent charging session is eligible for resumption after a brief disconnect.
 */
data class ReconnectPromptSession(
    val previousSession: ChargeTestSession,
    val previousSamples: List<ChargeTelemetrySample> = emptyList(),
    val disconnectedDurationMs: Long = 0L
)

/**
 * Live instant charging telemetry emitted to the UI HUD.
 */
data class LiveChargeTelemetry(
    val isCharging: Boolean = false,
    val chargePlugType: String = "Unplugged", // AC, USB, Wireless
    val currentWattage: Float = 0f,
    val currentMa: Int = 0,
    val voltageMv: Int = 0,
    val batteryLevelPercent: Int = 0,
    val tempDeciCelsius: Int = 0,
    val peakWattageSoFar: Float = 0f,
    val avgWattageSoFar: Float = 0f,
    val sessionDurationMs: Long = 0L,
    val isThrottlingDetected: Boolean = false,
    val isGracePeriodActive: Boolean = false,
    val gracePeriodRemainingSeconds: Int = 0
) {
    val tempCelsius: Float get() = tempDeciCelsius / 10.0f
    val voltageV: Float get() = voltageMv / 1000.0f
}
