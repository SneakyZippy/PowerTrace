package com.antigravity.battery.core.model

import org.json.JSONArray
import org.json.JSONObject

object SnapshotSerializer {

    fun serializeUidMetrics(map: Map<Int, UidMetrics>): String {
        val array = JSONArray()
        map.values.forEach { u ->
            val obj = JSONObject().apply {
                put("uid", u.uid)
                put("cpuUser", u.cpuUserTimeMs)
                put("cpuSys", u.cpuSystemTimeMs)
                put("wlDuration", u.partialWakelockDurationMs)
                put("wlCount", u.wakelockCount)
                put("radioMs", u.mobileRadioActiveMs)
                put("mobRx", u.mobileBytesRx)
                put("mobTx", u.mobileBytesTx)
                put("wifiScans", u.wifiScanCount)
                put("wifiRunMs", u.wifiRunningMs)
                put("wifiRx", u.wifiBytesRx)
                put("wifiTx", u.wifiBytesTx)
                put("gpsMs", u.gpsDurationMs)

                if (u.wakelockTags.isNotEmpty()) {
                    val tagsArray = JSONArray()
                    u.wakelockTags.values.forEach { tag ->
                        val tagObj = JSONObject().apply {
                            put("tag", tag.tag)
                            put("dur", tag.durationMs)
                            put("cnt", tag.count)
                        }
                        tagsArray.put(tagObj)
                    }
                    put("tags", tagsArray)
                }
            }
            array.put(obj)
        }
        return array.toString()
    }

    fun deserializeUidMetrics(jsonStr: String): Map<Int, UidMetrics> {
        val result = mutableMapOf<Int, UidMetrics>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val uid = obj.getInt("uid")
                val tagsMap = mutableMapOf<String, WakelockTagStat>()

                val tagsArray = obj.optJSONArray("tags")
                if (tagsArray != null) {
                    for (j in 0 until tagsArray.length()) {
                        val t = tagsArray.getJSONObject(j)
                        val tagName = t.getString("tag")
                        tagsMap[tagName] = WakelockTagStat(
                            tag = tagName,
                            durationMs = t.optLong("dur", 0L),
                            count = t.optInt("cnt", 1)
                        )
                    }
                }

                result[uid] = UidMetrics(
                    uid = uid,
                    cpuUserTimeMs = obj.optLong("cpuUser", 0L),
                    cpuSystemTimeMs = obj.optLong("cpuSys", 0L),
                    partialWakelockDurationMs = obj.optLong("wlDuration", 0L),
                    wakelockCount = obj.optInt("wlCount", 0),
                    mobileRadioActiveMs = obj.optLong("radioMs", 0L),
                    mobileBytesRx = obj.optLong("mobRx", 0L),
                    mobileBytesTx = obj.optLong("mobTx", 0L),
                    wifiScanCount = obj.optInt("wifiScans", 0),
                    wifiRunningMs = obj.optLong("wifiRunMs", 0L),
                    wifiBytesRx = obj.optLong("wifiRx", 0L),
                    wifiBytesTx = obj.optLong("wifiTx", 0L),
                    gpsDurationMs = obj.optLong("gpsMs", 0L),
                    wakelockTags = tagsMap
                )
            }
        } catch (_: Exception) {}
        return result
    }

    fun serializePackageAlarms(map: Map<String, PackageAlarmMetrics>): String {
        val array = JSONArray()
        map.values.forEach { a ->
            val obj = JSONObject().apply {
                put("pkg", a.packageName)
                put("wakeups", a.wakeupCount)
                if (a.topAlarmActions.isNotEmpty()) {
                    val actObj = JSONObject()
                    a.topAlarmActions.forEach { (k, v) -> actObj.put(k, v) }
                    put("actions", actObj)
                }
            }
            array.put(obj)
        }
        return array.toString()
    }

    fun deserializePackageAlarms(jsonStr: String): Map<String, PackageAlarmMetrics> {
        val result = mutableMapOf<String, PackageAlarmMetrics>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val pkg = obj.getString("pkg")
                val wakeups = obj.optInt("wakeups", 0)
                val actionsMap = mutableMapOf<String, Int>()

                val actObj = obj.optJSONObject("actions")
                if (actObj != null) {
                    val keys = actObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        actionsMap[key] = actObj.getInt(key)
                    }
                }

                result[pkg] = PackageAlarmMetrics(
                    packageName = pkg,
                    wakeupCount = wakeups,
                    topAlarmActions = actionsMap
                )
            }
        } catch (_: Exception) {}
        return result
    }
}
