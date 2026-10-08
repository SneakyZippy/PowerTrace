package com.antigravity.battery.core.dump

/**
 * Realistic Mock Diagnostic Dump Source providing baseline and post-overnight observation snapshots.
 * Allows instant verification on emulators or devices without physical ADB setup.
 */
class MockDiagnosticDumpSource(
    private var isEndSnapshot: Boolean = false
) : DiagnosticDumpSource {

    fun setSnapshotStage(endSnapshot: Boolean) {
        this.isEndSnapshot = endSnapshot
    }

    override suspend fun getBatterystatsCheckin(): String {
        return if (!isEndSnapshot) {
            getStartBatterystats()
        } else {
            getEndBatterystats()
        }
    }

    override suspend fun getAlarmDump(): String {
        return if (!isEndSnapshot) {
            getStartAlarmDump()
        } else {
            getEndAlarmDump()
        }
    }

    override suspend fun getPowerDump(): String {
        return """
            Power Manager State:
              mWakefulness=Awake
              Wake Locks: size=2
                PARTIAL_WAKE_LOCK              'AudioMix' ACQ=-1h2m (uid=1041 pid=1234)
                PARTIAL_WAKE_LOCK              '*job*' ACQ=-5m (uid=1000 pid=1600)
        """.trimIndent()
    }

    private fun getStartBatterystats(): String = """
        9,0,i,vers,16,180,Q,10
        9,0,i,uid,1000,android
        9,0,i,uid,10085,com.google.android.gms
        9,0,i,uid,10182,com.popular.social.app
        9,0,i,uid,10214,com.instant.messenger
        9,0,l,bt,10000000,8000000,10000000,8000000,1700000000000,1700000000000,95,95,4200,4200,280,280
        9,1000,l,wl,*alarm*,120000,450,0
        9,1000,l,cpr,850000,420000,10,250000,450000,150000
        9,10085,l,wl,GCM_CONN,60000,120,0
        9,10085,l,cpr,300000,150000,5,180000,100000,20000
        9,10085,l,nt,120000,4500,2000,150,150,20,50,10,12000000,5
        9,10182,l,wl,VideoPreloadService,45000,15,0
        9,10182,l,cpr,120000,40000,2,80000,35000,5000
        9,10182,l,wfl,5,15000
        9,10214,l,wl,KeepAliveSync,30000,60,0
        9,10214,l,cpr,90000,30000,3,60000,25000,5000
        9,10214,l,nt,45000,1200,800,40,40,15,30,5,4500000,2
    """.trimIndent()

    private fun getEndBatterystats(): String = """
        9,0,i,vers,16,180,Q,10
        9,0,i,uid,1000,android
        9,0,i,uid,10085,com.google.android.gms
        9,0,i,uid,10182,com.popular.social.app
        9,0,i,uid,10214,com.instant.messenger
        9,0,l,bt,38800000,12500000,38800000,12500000,1700028800000,1700028800000,95,84,4200,3850,280,295
        9,1000,l,wl,*alarm*,380000,1250,0
        9,1000,l,wl,ActivityManager-Sleep,150000,30,0
        9,1000,l,cpr,1450000,680000,15,450000,750000,250000
        9,10085,l,wl,GCM_CONN,420000,620,0
        9,10085,l,wl,*network*,180000,210,0
        9,10085,l,cpr,890000,390000,12,520000,290000,80000
        9,10085,l,nt,640000,28000,18500,850,850,60,120,40,64000000,25
        9,10182,l,wl,VideoPreloadService,3495000,195,0
        9,10182,l,wl,AnalyticsWorker,620000,48,0
        9,10182,l,cpr,1420000,440000,25,980000,380000,60000
        9,10182,l,wfl,65,185000
        9,10214,l,wl,KeepAliveSync,890000,480,0
        9,10214,l,cpr,420000,160000,8,280000,110000,30000
        9,10214,l,nt,525000,14500,9200,380,380,45,90,20,52500000,18
    """.trimIndent()

    private fun getStartAlarmDump(): String = """
        Alarm Stats:
          u0a85:com.google.android.gms +12m10s running, 12 wakeups:
            +1m2s 4 alarms: cmp={com.google.android.gms/com.google.android.gms.gcm.GcmService}
          u0a182:com.popular.social.app +2m15s running, 3 wakeups:
            +45s 3 alarms: act=com.popular.social.app.ACTION_SYNC
          u0a214:com.instant.messenger +4m30s running, 8 wakeups:
            +1m15s 8 alarms: act=com.instant.messenger.PING
    """.trimIndent()

    private fun getEndAlarmDump(): String = """
        Alarm Stats:
          u0a85:com.google.android.gms +45m30s running, 64 wakeups:
            +8m20s 48 alarms: cmp={com.google.android.gms/com.google.android.gms.gcm.GcmService}
            +2m10s 16 alarms: act=com.google.android.intent.action.GCM_RECONNECT
          u0a182:com.popular.social.app +38m40s running, 42 wakeups:
            +15m30s 36 alarms: act=com.popular.social.app.ACTION_BACKGROUND_FETCH
            +3m10s 6 alarms: act=com.popular.social.app.ACTION_SYNC
          u0a214:com.instant.messenger +52m10s running, 98 wakeups:
            +18m45s 90 alarms: act=com.instant.messenger.PING
            +2m12s 8 alarms: act=com.instant.messenger.HEARTBEAT
    """.trimIndent()
}
