package com.sneakyzippy.powerlab.service

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.util.Log
import com.sneakyzippy.powerlab.core.engine.ChargingBenchmarkManager

/**
 * Background JobService scheduled with [JobInfo.Builder.setRequiresCharging] constraint.
 * Guarantees that charging start and disconnect events are delivered reliably
 * even when the application process was terminated or in background Doze mode.
 */
class ChargingJobService : JobService() {

    companion object {
        const val JOB_ID = 4001
        private const val TAG = "ChargingJobService"

        fun schedule(context: Context) {
            try {
                val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler ?: return
                val component = ComponentName(context, ChargingJobService::class.java)
                val jobInfo = JobInfo.Builder(JOB_ID, component)
                    .setRequiresCharging(true)
                    .setPersisted(true)
                    .build()

                scheduler.schedule(jobInfo)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule charging job: ${e.message}", e)
            }
        }
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        try {
            ChargingBenchmarkManager.getInstance(applicationContext).onPowerConnected()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onStartJob: ${e.message}", e)
        }
        return false
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        try {
            ChargingBenchmarkManager.getInstance(applicationContext).onPowerDisconnected()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onStopJob: ${e.message}", e)
        }
        return false
    }
}
