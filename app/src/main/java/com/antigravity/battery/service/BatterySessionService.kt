package com.antigravity.battery.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.antigravity.battery.MainActivity
import com.antigravity.battery.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground Service that holds an active monitoring session during overnight screen-off periods,
 * preventing Android's Low Memory Killer from disposing the session state.
 */
class BatterySessionService : Service {

    constructor() : super()

    companion object {
        const val CHANNEL_ID = "battery_monitoring_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.antigravity.battery.action.START"
        const val ACTION_STOP = "com.antigravity.battery.action.STOP"
        const val EXTRA_START_BATTERY = "extra_start_battery"

        var isServiceRunning = false
            private set

        fun start(context: Context, startBatteryPercent: Int) {
            val intent = Intent(context, BatterySessionService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_START_BATTERY, startBatteryPercent)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, BatterySessionService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var tickerJob: Job? = null
    private var startTimestampMs = 0L
    private var startBattery = 100

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                isServiceRunning = true
                startBattery = intent.getIntExtra(EXTRA_START_BATTERY, 100)
                startTimestampMs = System.currentTimeMillis()
                startForeground(NOTIFICATION_ID, buildNotification(0L))
                startTimer()
            }
            ACTION_STOP -> {
                isServiceRunning = false
                tickerJob?.cancel()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startTimer() {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch {
            while (isActive) {
                delay(10_000L) // Update notification every 10 seconds
                val elapsedMs = System.currentTimeMillis() - startTimestampMs
                val notification = buildNotification(elapsedMs)
                val manager = getSystemService(NotificationManager::class.java)
                manager.notify(NOTIFICATION_ID, notification)
            }
        }
    }

    private fun buildNotification(elapsedMs: Long): Notification {
        val minutes = (elapsedMs / 1000) / 60
        val seconds = (elapsedMs / 1000) % 60
        val timeFormatted = String.format("%02d:%02d", minutes, seconds)

        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingOpen = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, BatterySessionService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStop = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Observation Session Active ($timeFormatted)")
            .setContentText("Baseline Battery: $startBattery% • Capturing wake locks & alarms")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setContentIntent(pendingOpen)
            .addAction(android.R.drawable.ic_media_pause, "Stop & Analyze", pendingStop)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Battery Observation Sessions",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows ongoing battery diagnostics monitoring session"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        isServiceRunning = false
        tickerJob?.cancel()
        super.onDestroy()
    }
}
