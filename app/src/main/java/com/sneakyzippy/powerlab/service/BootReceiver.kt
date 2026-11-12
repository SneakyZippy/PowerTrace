package com.sneakyzippy.powerlab.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.sneakyzippy.powerlab.MainActivity
import com.sneakyzippy.powerlab.R

/**
 * Automatically resumes 24/7 continuous telemetry when the device reboots
 * or when the app package is updated, if the user previously enabled it.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
        private const val RESUME_CHANNEL_ID = "powerlab_boot_resume"
        private const val NOTIFICATION_ID_RESUME_MONITORING = 2005
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Log.d(TAG, "Received ${intent.action}")
                ChargingJobService.schedule(context)
                if (ContinuousMonitoringService.isServiceActive(context)) {
                    try {
                        Log.i(TAG, "Resuming 24/7 continuous telemetry after reboot...")
                        ContinuousMonitoringService.start(context)
                    } catch (e: IllegalStateException) {
                        // Android 15+ restricts starting specialUse FGS from BOOT_COMPLETED
                        Log.w(TAG, "Background FGS start blocked on boot; prompting user to resume.", e)
                        showResumeNotification(context)
                    }
                } else {
                    Log.d(TAG, "Continuous monitoring is disabled, skipping start on boot.")
                }
            }
        }
    }

    private fun showResumeNotification(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                RESUME_CHANNEL_ID,
                "Monitoring Status",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Prompts to resume 24/7 battery monitoring after a device reboot"
            }
            manager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_RESUME_MONITORING,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, RESUME_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_bolt)
            .setContentTitle("Resume 24/7 Battery Monitoring")
            .setContentText("Device restarted. Tap to open PowerLab and resume continuous telemetry.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(NOTIFICATION_ID_RESUME_MONITORING, notification)
    }
}
