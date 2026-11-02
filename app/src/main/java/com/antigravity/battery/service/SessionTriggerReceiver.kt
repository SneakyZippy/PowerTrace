package com.antigravity.battery.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.antigravity.battery.core.engine.ChargingBenchmarkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SessionTriggerReceiver : BroadcastReceiver() {

    companion object {
        var onTriggerEvent: ((String) -> Unit)? = null
    }

    override fun onReceive(context: Context, intent: Intent) {
        val benchmarkManager = ChargingBenchmarkManager.getInstance(context)

        when (intent.action) {
            Intent.ACTION_USER_PRESENT -> {
                onTriggerEvent?.invoke("SCREEN_UNLOCKED")
                if (ContinuousMonitoringService.isRunning) {
                    ContinuousMonitoringService.triggerSnapshot(context)
                }
            }
            Intent.ACTION_POWER_CONNECTED -> {
                onTriggerEvent?.invoke("CHARGER_CONNECTED")
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        benchmarkManager.onPowerConnected()
                        if (ContinuousMonitoringService.isRunning) {
                            ContinuousMonitoringService.triggerSnapshot(context)
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                onTriggerEvent?.invoke("CHARGER_DISCONNECTED")
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        benchmarkManager.onPowerDisconnected()
                        if (ContinuousMonitoringService.isRunning) {
                            ContinuousMonitoringService.triggerSnapshot(context)
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
