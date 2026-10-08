package com.antigravity.battery

import com.antigravity.battery.core.model.PowerProfileData
import com.antigravity.battery.core.model.UidMetrics
import com.antigravity.battery.core.power.EnergyAttributionModel
import org.junit.Assert.assertEquals
import org.junit.Test

class EnergyAttributionModelTest {

    @Test
    fun testCalculateEnergyAttributionFormulas() {
        val profile = PowerProfileData(
            cpuAwakeMa = 28.0,
            cpuClustersMa = listOf(55.0, 240.0),
            radioActiveMa = 135.0,
            wifiActiveMa = 85.0,
            wifiScanMa = 75.0,
            gpsActiveMa = 60.0
        )
        val model = EnergyAttributionModel(profile)

        // 1 hour of partial wakelock (3,600,000 ms)
        val uidMetrics = UidMetrics(
            uid = 10100,
            cpuUserTimeMs = 3_600_000L, // 1 hour CPU
            cpuSystemTimeMs = 0L,
            partialWakelockDurationMs = 3_600_000L, // 1 hour wakelock
            mobileRadioActiveMs = 1_800_000L // 0.5 hour radio
        )

        val (totalMah, breakdown) = model.calculateEnergy(uidMetrics)

        // Expected Wakelock: (3,600,000 / 3,600,000) * 28.0 = 28.0 mAh
        assertEquals(28.0, breakdown.wakelockMah, 0.1)

        // Expected Radio: (1,800,000 / 3,600,000) * 135.0 = 67.5 mAh
        assertEquals(67.5, breakdown.mobileRadioMah, 0.1)

        // Expected CPU: 75% little (55mA) + 25% big (240mA) = 41.25 + 60 = 101.25 mAh
        assertEquals(101.25, breakdown.cpuMah, 0.2)

        // Total should be sum
        val expectedTotal = 101.25 + 28.0 + 67.5
        assertEquals(expectedTotal, totalMah, 0.5)
    }
}
