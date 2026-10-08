package com.antigravity.battery.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.antigravity.battery.MainActivity
import com.antigravity.battery.R
import com.antigravity.battery.core.dump.BinderDumpSource
import com.antigravity.battery.core.engine.SnapshotManager
import com.antigravity.battery.core.model.SnapshotSerializer
import com.antigravity.battery.data.db.BatteryDatabase
import com.antigravity.battery.data.db.PeriodicSnapshotEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 24/7 Background daemon taking periodic snapshots (every 15 min) and on state transitions
 * (screen on/off, charger plug/unplug) to provide full day/week overviews without manual intervention.
 */
class ContinuousMonitoringService : Service() {

    companion object {
        const val CHANNEL_ID = "volttrace_continuous_channel"
        const val NOTIFICATION_ID = 2002
        const val ACTION_START = "com.antigravity.battery.action.START_CONTINUOUS"
        const val ACTION_STOP = "com.antigravity.battery.action.STOP_CONTINUOUS"
        const val ACTION_TRIGGER_SNAPSHOT = "com.antigravity.battery.action.TRIGGER_SNAPSHOT"

        var isRunning = false
            private set

        fun start(context: Context) {
            val intent = Intent(context, ContinuousMonitoringService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, ContinuousMonitoringService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun triggerSnapshot(context: Context) {
            val intent = Intent(context, ContinuousMonitoringService::class.java).apply {
                action = ACTION_TRIGGER_SNAPSHOT
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var heartbeatJob: Job? = null
    private lateinit var snapshotManager: SnapshotManager
    private lateinit var database: BatteryDatabase

    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON,
                Intent.ACTION_SCREEN_OFF,
                Intent.ACTION_POWER_CONNECTED,
                Intent.ACTION_POWER_DISCONNECTED -> {
                    serviceScope.launch {
                        captureAndPersistSnapshot()
                    }
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        database = BatteryDatabase.getInstance(this)
        snapshotManager = SnapshotManager(BinderDumpSource(this), this)

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        registerReceiver(stateReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                if (!isRunning) {
                    isRunning = true
                    startForeground(NOTIFICATION_ID, buildNotification("Initializing 24/7 background tracking..."))
                    startPeriodicHeartbeat()
                    serviceScope.launch { captureAndPersistSnapshot() }
                }
            }
            ACTION_TRIGGER_SNAPSHOT -> {
                serviceScope.launch { captureAndPersistSnapshot() }
            }
            ACTION_STOP -> {
                isRunning = false
                heartbeatJob?.cancel()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startPeriodicHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = serviceScope.launch {
            while (isActive) {
                delay(15 * 60 * 1000L) // 15-minute heartbeat
                captureAndPersistSnapshot()
                pruneOldData()
            }
        }
    }

    private suspend fun captureAndPersistSnapshot() {
        try {
            val nowMs = System.currentTimeMillis()
            val snapshot = snapshotManager.captureSnapshot(nowMs)
            val dev = snapshot.deviceMetrics

            val bm = getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val isCharging = bm?.isCharging ?: false

            val entity = PeriodicSnapshotEntity(
                timestampMs = nowMs,
                batteryLevel = dev.batteryLevelPercent,
                voltageMv = dev.voltageMv,
                tempDeciCelsius = dev.temperatureDeciCelsius,
                isCharging = isCharging,
                isScreenOn = dev.isScreenOn,
                totalRealtimeMs = dev.totalRealtimeMs,
                totalUptimeMs = dev.totalUptimeMs,
                screenOnDurationMs = dev.screenOnDurationMs,
                screenOffDurationMs = dev.screenOffDurationMs,
                uidMetricsJson = SnapshotSerializer.serializeUidMetrics(snapshot.uidMetricsMap),
                packageAlarmsJson = SnapshotSerializer.serializePackageAlarms(snapshot.packageAlarms)
            )

            database.continuousSnapshotDao().insertSnapshot(entity)

            // Update notification
            val tempC = dev.temperatureDeciCelsius / 10.0
            val status = if (isCharging) "Charging ⚡" else "Monitoring"
            val text = "Battery: ${dev.batteryLevelPercent}% • $status • ${String.format(java.util.Locale.US, "%.1f", tempC)}°C"
            val manager = getSystemService(NotificationManager::class.java)
            manager.notify(NOTIFICATION_ID, buildNotification(text))

        } catch (e: Exception) {
            Log.e("ContinuousService", "Error capturing periodic snapshot: ${e.message}", e)
        }
    }

    private suspend fun pruneOldData() {
        try {
            val fourteenDaysAgoMs = System.currentTimeMillis() - (14 * 24 * 3600 * 1000L)
            database.continuousSnapshotDao().pruneSnapshotsOlderThan(fourteenDaysAgoMs)
        } catch (_: Exception) {}
    }

    private fun buildNotification(statusText: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingOpen = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("PowerTrace 24/7 Diagnostics")
            .setContentText(statusText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setContentIntent(pendingOpen)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Continuous Battery Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live status of 24/7 background battery and wakelock tracking"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        isRunning = false
        heartbeatJob?.cancel()
        try { unregisterReceiver(stateReceiver) } catch (_: Exception) {}
        super.onDestroy()
    }
}
