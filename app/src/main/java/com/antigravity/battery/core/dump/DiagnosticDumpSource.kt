package com.antigravity.battery.core.dump

interface DiagnosticDumpSource {
    suspend fun getBatterystatsCheckin(): String
    suspend fun getAlarmDump(): String
    suspend fun getPowerDump(): String
}
