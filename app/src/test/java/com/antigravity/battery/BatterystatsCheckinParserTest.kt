package com.antigravity.battery

import com.antigravity.battery.core.parser.BatterystatsCheckinParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BatterystatsCheckinParserTest {

    private val parser = BatterystatsCheckinParser()

    @Test
    fun testParseDeviceLevelMetrics() {
        val checkin = """
            9,0,i,vers,16,180,Q,10
            9,0,l,bt,10000000,8000000,10000000,8000000,1700000000000,1700000000000,95,90,4200,4100,280,290
        """.trimIndent()

        val snapshot = parser.parse(checkin, 1700000000000L)
        val dev = snapshot.deviceMetrics

        assertEquals(90, dev.batteryLevelPercent)
        assertEquals(4100, dev.voltageMv)
        assertEquals(290, dev.temperatureDeciCelsius)
        assertEquals(10000000L, dev.totalRealtimeMs)
        assertEquals(8000000L, dev.totalUptimeMs)
    }

    @Test
    fun testParseUidWakelocksAndCpu() {
        val checkin = """
            9,0,i,vers,16,180,Q,10
            9,0,l,bt,10000000,8000000,10000000,8000000,1700000000000,1700000000000,95,95,4200,4200,280,280
            9,10182,l,wl,VideoPreloadService,3450000,195,0
            9,10182,l,wl,AnalyticsWorker,620000,48,0
            9,10182,l,cpr,1420000,440000,25,980000,380000,60000
            9,10182,l,wfl,65,185000
            9,10182,l,nt,640000,28000,18500,850,850,200,120,40,640000000,12
        """.trimIndent()

        val snapshot = parser.parse(checkin)
        val uidStat = snapshot.uidMetricsMap[10182]

        assertNotNull(uidStat)
        assertEquals(3450000L + 620000L, uidStat!!.partialWakelockDurationMs)
        assertEquals(195 + 48, uidStat.wakelockCount)
        assertEquals(1420000L, uidStat.cpuUserTimeMs)
        assertEquals(440000L, uidStat.cpuSystemTimeMs)
        assertEquals(65, uidStat.wifiScanCount)
        assertEquals(640000L, uidStat.mobileRadioActiveMs)
        assertEquals(12, uidStat.mobileRadioActiveCount)

        assertEquals(2, uidStat.wakelockTags.size)
        assertTrue(uidStat.wakelockTags.containsKey("VideoPreloadService"))
        assertEquals(3450000L, uidStat.wakelockTags["VideoPreloadService"]?.durationMs)
    }

    @Test
    fun testParseModernAndroidCheckinFormat() {
        val checkin = """
            9,0,i,vers,36,216,BP3A.250905.014,CP3A.260905.009
            9,0,l,bt,7,4133678,1110279,15744101,12720704,1791455104184,3912300,888900,4897,4897500,4900000,0
            9,10003,l,wl,*job*r/com.digibites.accubattery/JobService,0,f,0,0,0,0,88,p,2,0,341,572,572,bp,0,0,0,0,0,w,0,0,0,0
            9,10003,l,cpu,2394386,1479213,0
            9,10003,l,nt,0,0,60571,49359,0,0,181,130,0,0,0,0,0,0,0,0,0,0,0,0,0,0
        """.trimIndent()

        val snapshot = parser.parse(checkin)
        val uidStat = snapshot.uidMetricsMap[10003]

        assertNotNull(uidStat)
        assertEquals(572L, uidStat!!.partialWakelockDurationMs)
        assertEquals(2, uidStat.wakelockCount)
        assertEquals(2394386L, uidStat.cpuUserTimeMs)
        assertEquals(1479213L, uidStat.cpuSystemTimeMs)
        assertTrue(uidStat.wakelockTags.containsKey("*job*r/com.digibites.accubattery/JobService"))
        assertEquals(572L, uidStat.wakelockTags["*job*r/com.digibites.accubattery/JobService"]?.durationMs)
    }

    @Test
    fun testParseMobileRadioMicrosecondsConversion() {
        // Line with 330,222,000 microseconds active time (~330 seconds active)
        val checkin = """
            9,0,l,bt,10000000,8000000,10000000,8000000,1700000000000,1700000000000,95,95,4200,4200,280,280
            9,0,l,nt,15000000,2000000,0,0,10000,2000,0,0,330222000,45
        """.trimIndent()

        val snapshot = parser.parse(checkin)
        val kernelStat = snapshot.uidMetricsMap[0]

        assertNotNull(kernelStat)
        // 330222000 us / 1000 = 330222 ms (approx 330s active, NOT 330222s)
        assertEquals(330222L, kernelStat!!.mobileRadioActiveMs)
        assertEquals(45, kernelStat.mobileRadioActiveCount)
    }

    @Test
    fun testShortNtLineDoesNotFallbackToBytes() {
        // Line with only 9 elements (no mobileActiveTime field)
        val checkin = """
            9,0,l,bt,10000000,8000000,10000000,8000000,1700000000000,1700000000000,95,95,4200,4200,280,280
            9,10200,l,nt,987654321,50000,0,0,500
        """.trimIndent()

        val snapshot = parser.parse(checkin)
        val appStat = snapshot.uidMetricsMap[10200]

        assertNotNull(appStat)
        // Should NOT treat mobileBytesRx (987654321) as active duration
        assertEquals(0L, appStat!!.mobileRadioActiveMs)
    }
}
