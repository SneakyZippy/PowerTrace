package com.antigravity.battery.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.antigravity.battery.MainActivity
import com.antigravity.battery.core.engine.ChargingBenchmarkManager

/**
 * Handles notification actions for charger reconnect prompts and grace period finalization.
 */
class ChargingActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_CONTINUE_SESSION = "com.antigravity.battery.action.CONTINUE_SESSION"
        const val ACTION_START_NEW_SESSION = "com.antigravity.battery.action.START_NEW_SESSION"
        const val ACTION_FINISH_SESSION_NOW = "com.antigravity.battery.action.FINISH_SESSION_NOW"
        const val ACTION_DISCARD_PAUSED_SESSION = "com.antigravity.battery.action.DISCARD_PAUSED_SESSION"
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
                val appIntent = Intent(context, MainActivity::class.java).apply {
                    action = ChargingNotificationHelper.ACTION_TAG_ACCESSORIES
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                context.startActivity(appIntent)
            }
            ACTION_DISCARD_PAUSED_SESSION -> {
                benchmarkManager.discardPausedSessionNow()
            }
        }
    }
}
