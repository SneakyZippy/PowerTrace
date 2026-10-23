package com.antigravity.battery.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DismissNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ChargingNotificationHelper.cancelNotification(context)
    }
}
