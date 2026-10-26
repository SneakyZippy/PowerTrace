package com.antigravity.battery.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.antigravity.battery.core.engine.ChargingBenchmarkManager

/**
 * Handles notification actions for charger reconnect prompts and grace period finalization.
 */
class ChargingActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_CONTINUE_SESSION = "com.antigravity.battery.action.CONTINUE_SESSION"
        const val ACTION_START_NEW_SESSION = "com.antigravity.battery.action.START_NEW_SESSION"
        const val ACTION_FINISH_SESSION_NOW = "com.antigravity.battery.action.FINISH_SESSION_NOW"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val benchmarkManager = ChargingBenchmarkManager.getInstance(context)
        when (intent.action) {
            ACTION_CONTINUE_SESSION -> {
                benchmarkManager.resumePreviousSession()
            }
            ACTION_START_NEW_SESSION -> {
                benchmarkManager.startNewSessionFromPrompt()
            }
            ACTION_FINISH_SESSION_NOW -> {
                benchmarkManager.finalizePausedSessionNow()
            }
        }
    }
}
