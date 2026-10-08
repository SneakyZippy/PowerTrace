package com.antigravity.battery

import com.antigravity.battery.core.parser.AlarmDumpsysParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmDumpsysParserTest {

    private val parser = AlarmDumpsysParser()

    @Test
    fun testParseAlarmsAndActions() {
        val alarmDump = """
            Alarm Stats:
              u0a85:com.google.android.gms +45m30s running, 64 wakeups:
                +8m20s 48 alarms: cmp={com.google.android.gms/com.google.android.gms.gcm.GcmService}
                +2m10s 16 alarms: act=com.google.android.intent.action.GCM_RECONNECT
              u0a182:com.popular.social.app +38m40s running, 42 wakeups:
                +15m30s 36 alarms: act=com.popular.social.app.ACTION_BACKGROUND_FETCH
        """.trimIndent()

        val result = parser.parse(alarmDump)

        val gms = result["com.google.android.gms"]
        assertNotNull(gms)
        assertEquals(64, gms!!.wakeupCount)
        assertEquals(2, gms.topAlarmActions.size)
        assertEquals(16, gms.topAlarmActions["com.google.android.intent.action.GCM_RECONNECT"])

        val social = result["com.popular.social.app"]
        assertNotNull(social)
        assertEquals(42, social!!.wakeupCount)
        assertEquals(36, social.topAlarmActions["com.popular.social.app.ACTION_BACKGROUND_FETCH"])
    }
}
