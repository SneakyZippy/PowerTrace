package com.sneakyzippy.powerlab

import android.app.Application
import com.sneakyzippy.powerlab.core.engine.ChargingBenchmarkManager
import com.sneakyzippy.powerlab.service.ChargingJobService

class BatteryApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ChargingJobService.schedule(this)
        ChargingBenchmarkManager.getInstance(this).sampleCurrentState()
    }
}
