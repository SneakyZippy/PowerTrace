package com.antigravity.battery.core.model

/**
 * Hardware power draw coefficients in mA (milliamperes) used to convert hardware activity times to mAh.
 */
data class PowerProfileData(
    val cpuAwakeMa: Double = 28.0,          // Base system draw when CPU is awake in screen-off
    val cpuClustersMa: List<Double> = listOf(55.0, 240.0), // Little cluster, Big cluster mA
    val radioActiveMa: Double = 135.0,      // Cellular transceiver active mA
    val wifiActiveMa: Double = 85.0,        // Wi-Fi transceiver active mA
    val wifiScanMa: Double = 75.0,          // Wi-Fi active scan mA
    val gpsActiveMa: Double = 60.0,         // GPS sensor active mA
    val isReflectedFromSystem: Boolean = false,
    val sourceDescription: String = "Calibrated Default"
)
