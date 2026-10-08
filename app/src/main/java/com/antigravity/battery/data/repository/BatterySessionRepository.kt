package com.antigravity.battery.data.repository

import com.antigravity.battery.core.model.CulpritApp
import com.antigravity.battery.core.model.EnergyBreakdown
import com.antigravity.battery.core.model.ObservationSession
import com.antigravity.battery.core.model.SessionDelta
import com.antigravity.battery.core.model.SessionStatus
import com.antigravity.battery.core.model.WakelockTagStat
import com.antigravity.battery.data.db.BatteryDatabase
import com.antigravity.battery.data.db.SessionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

class BatterySessionRepository(private val database: BatteryDatabase) {

    private val dao = database.sessionDao()

    fun getAllSessions(): Flow<List<SessionEntity>> {
        return dao.getAllSessions()
    }

    suspend fun saveSession(session: ObservationSession) {
        val delta = session.delta ?: return
        val entity = SessionEntity(
            id = session.id,
            title = session.title,
            startTimeMs = session.startTimeMs,
            endTimeMs = session.endTimeMs ?: System.currentTimeMillis(),
            durationMs = delta.durationMs,
            batteryStartPercent = delta.batteryLevelStart,
            batteryEndPercent = delta.batteryLevelEnd,
            batteryPercentDrop = delta.batteryPercentDrop,
            drainRatePercentPerHour = delta.drainRatePercentPerHour,
            deepSleepEfficiencyPercent = delta.deepSleepEfficiencyPercent,
            deepSleepDurationMs = delta.deepSleepDurationMs,
            awakeScreenOffMs = delta.awakeScreenOffMs,
            screenOnDurationMs = delta.screenOnDurationMs,
            isInterrupted = delta.isInterrupted,
            interruptionReason = delta.interruptionReason,
            culpritsJson = serializeCulprits(delta.culprits)
        )
        dao.insertSession(entity)
    }

    suspend fun deleteSession(sessionId: String) {
        dao.deleteSessionById(sessionId)
    }

    suspend fun clearAll() {
        dao.clearAll()
    }

    fun deserializeCulprits(jsonStr: String): List<CulpritApp> {
        val list = mutableListOf<CulpritApp>()
        return try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val uid = obj.getInt("uid")
                val pkg = obj.getString("primaryPackageName")
                val label = obj.getString("appLabel")
                val isSystem = obj.optBoolean("isSystemApp", false)
                val totalMah = obj.getDouble("totalEstimatedMah")

                val energyObj = obj.optJSONObject("energyBreakdown") ?: JSONObject()
                val energy = EnergyBreakdown(
                    cpuMah = energyObj.optDouble("cpuMah", 0.0),
                    wakelockMah = energyObj.optDouble("wakelockMah", 0.0),
                    mobileRadioMah = energyObj.optDouble("mobileRadioMah", 0.0),
                    wifiMah = energyObj.optDouble("wifiMah", 0.0),
                    gpsMah = energyObj.optDouble("gpsMah", 0.0)
                )

                val cpuUser = obj.optLong("cpuUserTimeMs", 0L)
                val cpuSys = obj.optLong("cpuSystemTimeMs", 0L)
                val wakelockMs = obj.optLong("partialWakelockMs", 0L)
                val wakelockCount = obj.optInt("wakelockCount", 0)
                val alarms = obj.optInt("wakeupAlarmCount", 0)
                val radioMs = obj.optLong("mobileRadioActiveMs", 0L)
                val wifiScans = obj.optInt("wifiScanCount", 0)
                val gpsMs = obj.optLong("gpsDurationMs", 0L)

                list.add(
                    CulpritApp(
                        uid = uid,
                        primaryPackageName = pkg,
                        appLabel = label,
                        isSystemApp = isSystem,
                        totalEstimatedMah = totalMah,
                        energyBreakdown = energy,
                        cpuUserTimeMs = cpuUser,
                        cpuSystemTimeMs = cpuSys,
                        partialWakelockMs = wakelockMs,
                        wakelockCount = wakelockCount,
                        wakeupAlarmCount = alarms,
                        mobileRadioActiveMs = radioMs,
                        wifiScanCount = wifiScans,
                        gpsDurationMs = gpsMs
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun serializeCulprits(culprits: List<CulpritApp>): String {
        val jsonArray = JSONArray()
        culprits.forEach { c ->
            val obj = JSONObject().apply {
                put("uid", c.uid)
                put("primaryPackageName", c.primaryPackageName)
                put("appLabel", c.appLabel)
                put("isSystemApp", c.isSystemApp)
                put("totalEstimatedMah", c.totalEstimatedMah)
                put("cpuUserTimeMs", c.cpuUserTimeMs)
                put("cpuSystemTimeMs", c.cpuSystemTimeMs)
                put("partialWakelockMs", c.partialWakelockMs)
                put("wakelockCount", c.wakelockCount)
                put("wakeupAlarmCount", c.wakeupAlarmCount)
                put("mobileRadioActiveMs", c.mobileRadioActiveMs)
                put("wifiScanCount", c.wifiScanCount)
                put("gpsDurationMs", c.gpsDurationMs)

                val energyObj = JSONObject().apply {
                    put("cpuMah", c.energyBreakdown.cpuMah)
                    put("wakelockMah", c.energyBreakdown.wakelockMah)
                    put("mobileRadioMah", c.energyBreakdown.mobileRadioMah)
                    put("wifiMah", c.energyBreakdown.wifiMah)
                    put("gpsMah", c.energyBreakdown.gpsMah)
                }
                put("energyBreakdown", energyObj)
            }
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }
}
