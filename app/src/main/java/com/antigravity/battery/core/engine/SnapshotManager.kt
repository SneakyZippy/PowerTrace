package com.antigravity.battery.core.engine

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.SystemClock
import android.util.Log
import com.antigravity.battery.core.dump.DiagnosticDumpSource
import com.antigravity.battery.core.model.BatterySnapshot
import com.antigravity.battery.core.model.DeviceLevelMetrics
import com.antigravity.battery.core.parser.AlarmDumpsysParser
import com.antigravity.battery.core.parser.BatterystatsCheckinParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Orchestrates the acquisition of a point-in-time snapshot by pulling from dumpsys
 * sources, validating with Android hardware APIs, and parsing them into domain models.
 */
class SnapshotManager(
    private var dumpSource: DiagnosticDumpSource,
    private val context: Context? = null,
    private val checkinParser: BatterystatsCheckinParser = BatterystatsCheckinParser(),
    private val alarmParser: AlarmDumpsysParser = AlarmDumpsysParser()
) {

    companion object {
        private const val TAG = "SnapshotManager"
    }

    fun setDumpSource(source: DiagnosticDumpSource) {
        this.dumpSource = source
    }

    fun getDumpSource(): DiagnosticDumpSource = dumpSource

    suspend fun captureSnapshot(timestampMs: Long = System.currentTimeMillis()): BatterySnapshot = withContext(Dispatchers.IO) {
        val rawCheckin = try {
            dumpSource.getBatterystatsCheckin()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to capture batterystats checkin: ${e.message}", e)
            ""
        }

        val rawAlarm = try {
            dumpSource.getAlarmDump()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to capture alarm dump: ${e.message}", e)
            ""
        }

        val baseSnapshot = checkinParser.parse(rawCheckin, timestampMs)
        val alarms = alarmParser.parse(rawAlarm)

        Log.d(TAG, "captureSnapshot: rawCheckin=${rawCheckin.length} chars, uids=${baseSnapshot.uidMetricsMap.size}, rawAlarm=${rawAlarm.length} chars, alarms=${alarms.size}")

        // Enrich DeviceLevelMetrics with hardware ground truth if available
        val enrichedDeviceMetrics = enrichDeviceMetrics(baseSnapshot.deviceMetrics, timestampMs)

        baseSnapshot.copy(
            deviceMetrics = enrichedDeviceMetrics,
            packageAlarms = alarms
        )
    }

    private fun enrichDeviceMetrics(parsed: DeviceLevelMetrics, nowMs: Long): DeviceLevelMetrics {
        var batteryLevel = parsed.batteryLevelPercent
        var voltageMv = parsed.voltageMv
        var tempDeciCelsius = parsed.temperatureDeciCelsius

        val ctx = context
        if (ctx != null) {
            try {
                val batteryStatus = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                if (batteryStatus != null) {
                    val rawLevel = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                    if (rawLevel >= 0 && scale > 0) {
                        batteryLevel = (rawLevel * 100) / scale
                    }
                    val volt = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
                    if (volt > 0) {
                        voltageMv = volt
                    }
                    val temp = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
                    if (temp > 0) {
                        tempDeciCelsius = temp
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not read battery broadcast: ${e.message}")
            }
        }

        // Use hardware monotonic clocks if checkin values are 0 or uninitialized
        val realtimeMs = if (parsed.totalRealtimeMs > 0) parsed.totalRealtimeMs else SystemClock.elapsedRealtime()
        val uptimeMs = if (parsed.totalUptimeMs > 0) parsed.totalUptimeMs else SystemClock.uptimeMillis()

        return parsed.copy(
            batteryLevelPercent = batteryLevel,
            voltageMv = voltageMv,
            temperatureDeciCelsius = tempDeciCelsius,
            totalRealtimeMs = realtimeMs,
            totalUptimeMs = uptimeMs
        )
    }
}
