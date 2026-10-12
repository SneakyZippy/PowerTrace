package com.antigravity.battery.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Automatically resumes 24/7 continuous telemetry collection when the device reboots
 * or when the app package is updated, if the user previously enabled it.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Log.d(TAG, "Received ${intent.action}")
                if (ContinuousMonitoringService.isServiceActive(context)) {
                    Log.i(TAG, "Resuming 24/7 continuous telemetry after reboot...")
                    ContinuousMonitoringService.start(context)
                } else {
                    Log.d(TAG, "Continuous monitoring is disabled, skipping start on boot.")
                }
            }
        }
    }
}
