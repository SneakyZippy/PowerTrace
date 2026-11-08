package com.antigravity.battery

import android.app.Application
import com.antigravity.battery.core.engine.ChargingBenchmarkManager
import com.antigravity.battery.service.ChargingJobService

class BatteryApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ChargingJobService.schedule(this)
        ChargingBenchmarkManager.getInstance(this).sampleCurrentState()
    }
}
