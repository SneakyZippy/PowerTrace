package com.antigravity.battery.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.antigravity.battery.MainActivity
import com.antigravity.battery.R
import com.antigravity.battery.core.model.ChargeTestSession

object ChargingNotificationHelper {

    const val CHANNEL_ID = "charging_benchmarks"
    const val NOTIFICATION_ID = 2001
    const val NOTIFICATION_ID_RECONNECT = 2003

    const val ACTION_TAG_ACCESSORIES = "com.antigravity.battery.ACTION_TAG_ACCESSORIES"
    const val ACTION_OPEN_RECONNECT_DIALOG = "com.antigravity.battery.ACTION_OPEN_RECONNECT_DIALOG"
    const val EXTRA_SESSION_ID = "extra_session_id"
    const val EXTRA_PEAK_W = "extra_peak_w"
    const val EXTRA_AVG_W = "extra_avg_w"
    const val EXTRA_DURATION_MIN = "extra_duration_min"
    const val EXTRA_BATTERY_GAIN = "extra_battery_gain"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Charger & Cable Tests",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Prompts to tag and save charging sessions for cables and power bricks"
                enableLights(true)
                setShowBadge(true)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    fun showSessionFinishedNotification(context: Context, session: ChargeTestSession) {
        createNotificationChannel(context)

        val durationMinutes = (session.durationMs / 60000).coerceAtLeast(1)
        val batteryGain = session.batteryGained

        // Main Tap Intent -> opens app with Tag Accessories bottom sheet
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_TAG_ACCESSORIES
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_SESSION_ID, session.id)
            putExtra(EXTRA_PEAK_W, session.peakWattage)
            putExtra(EXTRA_AVG_W, session.avgWattage)
            putExtra(EXTRA_DURATION_MIN, durationMinutes)
            putExtra(EXTRA_BATTERY_GAIN, batteryGain)
        }
        val pendingContentIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Dismiss action
        val dismissIntent = Intent(context, DismissNotificationReceiver::class.java)
        val pendingDismissIntent = PendingIntent.getBroadcast(
            context,
            NOTIFICATION_ID + 1,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "⚡ Charging Session (${durationMinutes}m, +$batteryGain%)"
        val body = String.format(
            "Peak: %.1fW | Avg: %.1fW. Tap to tag your charger and cable.",
            session.peakWattage,
            session.avgWattage
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingContentIntent)
            .addAction(R.mipmap.ic_launcher, "Tag & Save", pendingContentIntent)
            .addAction(R.mipmap.ic_launcher, "Dismiss", pendingDismissIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID, notification)
    }

    fun cancelNotification(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.cancel(NOTIFICATION_ID)
    }

    fun showReconnectPromptNotification(
        context: Context,
        session: ChargeTestSession,
        disconnectedDurationMs: Long
    ) {
        createNotificationChannel(context)

        val durationMinutes = (session.durationMs / 60000).coerceAtLeast(1)
        val batteryGain = session.batteryGained
        val disconnectedSec = (disconnectedDurationMs / 1000).coerceAtLeast(1)

        // Tapping opens MainActivity which shows the ReconnectSessionDialog
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_RECONNECT_DIALOG
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingContentIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_RECONNECT,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Continue Session
        val continueIntent = Intent(context, ChargingActionReceiver::class.java).apply {
            action = ChargingActionReceiver.ACTION_CONTINUE_SESSION
        }
        val pendingContinueIntent = PendingIntent.getBroadcast(
            context,
            NOTIFICATION_ID_RECONNECT + 1,
            continueIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Start New Session
        val newSessionIntent = Intent(context, ChargingActionReceiver::class.java).apply {
            action = ChargingActionReceiver.ACTION_START_NEW_SESSION
        }
        val pendingNewSessionIntent = PendingIntent.getBroadcast(
            context,
            NOTIFICATION_ID_RECONNECT + 2,
            newSessionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "⚡ Charger Reconnected (${disconnectedSec}s ago)"
        val body = String.format(
            "Previous session: %dm (+%d%%). Continue it or start a new one?",
            durationMinutes,
            batteryGain
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingContentIntent)
            .addAction(R.mipmap.ic_launcher, "Continue", pendingContinueIntent)
            .addAction(R.mipmap.ic_launcher, "New Session", pendingNewSessionIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID_RECONNECT, notification)
    }

    fun cancelReconnectNotification(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.cancel(NOTIFICATION_ID_RECONNECT)
    }
}
