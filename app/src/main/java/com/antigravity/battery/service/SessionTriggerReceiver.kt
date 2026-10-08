package com.antigravity.battery.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class SessionTriggerReceiver : BroadcastReceiver() {

    companion object {
        var onTriggerEvent: ((String) -> Unit)? = null
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_USER_PRESENT -> {
                onTriggerEvent?.invoke("SCREEN_UNLOCKED")
                if (ContinuousMonitoringService.isRunning) {
                    ContinuousMonitoringService.triggerSnapshot(context)
                }
            }
            Intent.ACTION_POWER_CONNECTED -> {
                onTriggerEvent?.invoke("CHARGER_CONNECTED")
                if (ContinuousMonitoringService.isRunning) {
                    ContinuousMonitoringService.triggerSnapshot(context)
                }
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                onTriggerEvent?.invoke("CHARGER_DISCONNECTED")
                if (ContinuousMonitoringService.isRunning) {
                    ContinuousMonitoringService.triggerSnapshot(context)
                }
            }
        }
    }
}
