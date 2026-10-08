package com.antigravity.battery.core.power

import com.antigravity.battery.core.model.EnergyBreakdown
import com.antigravity.battery.core.model.PowerProfileData
import com.antigravity.battery.core.model.UidMetrics

/**
 * Maps hardware active time durations to estimated milliampere-hours (mAh).
 */
class EnergyAttributionModel(
    private val powerProfile: PowerProfileData
) {

    companion object {
        private const val MS_PER_HOUR = 3_600_000.0
        private const val ESTIMATED_WIFI_SCAN_DURATION_MS = 2500.0
    }

    fun calculateEnergy(deltaUid: UidMetrics): Pair<Double, EnergyBreakdown> {
        // 1. CPU Energy calculation
        val cpuMah = if (deltaUid.cpuClusterTimesMs.isNotEmpty()) {
            deltaUid.cpuClusterTimesMs.mapIndexed { index, clusterMs ->
                val ma = powerProfile.cpuClustersMa.getOrElse(index) {
                    powerProfile.cpuClustersMa.lastOrNull() ?: 100.0
                }
                (clusterMs / MS_PER_HOUR) * ma
            }.sum()
        } else {
            // Aggregate user + system time distributed 75% little cluster, 25% big cluster
            val totalCpuMs = (deltaUid.cpuUserTimeMs + deltaUid.cpuSystemTimeMs).coerceAtLeast(0L)
            val littleMa = powerProfile.cpuClustersMa.firstOrNull() ?: 55.0
            val bigMa = powerProfile.cpuClustersMa.getOrNull(1) ?: 240.0
            val littleMah = ((totalCpuMs * 0.75) / MS_PER_HOUR) * littleMa
            val bigMah = ((totalCpuMs * 0.25) / MS_PER_HOUR) * bigMa
            littleMah + bigMah
        }

        // 2. Partial Wakelock Energy
        val wakelockMah = ((deltaUid.partialWakelockDurationMs.coerceAtLeast(0L)) / MS_PER_HOUR) * powerProfile.cpuAwakeMa

        // 3. Mobile Radio Active Energy
        val radioMah = ((deltaUid.mobileRadioActiveMs.coerceAtLeast(0L)) / MS_PER_HOUR) * powerProfile.radioActiveMa

        // 4. Wi-Fi Energy (Scans + active hold)
        val wifiScanMah = ((deltaUid.wifiScanCount * ESTIMATED_WIFI_SCAN_DURATION_MS) / MS_PER_HOUR) * powerProfile.wifiScanMa
        val wifiRunningMah = ((deltaUid.wifiRunningMs.coerceAtLeast(0L)) / MS_PER_HOUR) * powerProfile.wifiActiveMa
        val wifiMah = wifiScanMah + wifiRunningMah

        // 5. GPS Sensor Energy
        val gpsMah = ((deltaUid.gpsDurationMs.coerceAtLeast(0L)) / MS_PER_HOUR) * powerProfile.gpsActiveMa

        val breakdown = EnergyBreakdown(
            cpuMah = (cpuMah * 100.0).toLong() / 100.0,
            wakelockMah = (wakelockMah * 100.0).toLong() / 100.0,
            mobileRadioMah = (radioMah * 100.0).toLong() / 100.0,
            wifiMah = (wifiMah * 100.0).toLong() / 100.0,
            gpsMah = (gpsMah * 100.0).toLong() / 100.0
        )

        val totalMah = ((cpuMah + wakelockMah + radioMah + wifiMah + gpsMah) * 100.0).toLong() / 100.0
        return Pair(totalMah, breakdown)
    }
}
