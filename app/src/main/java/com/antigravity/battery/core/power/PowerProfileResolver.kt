package com.antigravity.battery.core.power

import android.content.Context
import android.util.Log
import com.antigravity.battery.core.model.PowerProfileData

/**
 * Resolves platform hardware power draw metrics from internal `com.android.internal.os.PowerProfile`.
 * Incorporates multi-cluster CPU and radio fallbacks when reflection is restricted by OEM SELinux.
 */
class PowerProfileResolver(private val context: Context) {

    companion object {
        private const val TAG = "PowerProfileResolver"
        private const val POWER_PROFILE_CLASS = "com.android.internal.os.PowerProfile"
    }

    fun resolve(): PowerProfileData {
        return try {
            val powerProfileClass = Class.forName(POWER_PROFILE_CLASS)
            val constructor = powerProfileClass.getConstructor(Context::class.java)
            val powerProfileInstance = constructor.newInstance(context)

            val getAveragePowerMethod = powerProfileClass.getMethod("getAveragePower", String::class.java)

            fun queryPower(key: String, fallback: Double): Double {
                return try {
                    val result = getAveragePowerMethod.invoke(powerProfileInstance, key) as? Double
                    if (result != null && result > 0.0) result else fallback
                } catch (_: Exception) {
                    fallback
                }
            }

            val cpuAwake = queryPower("cpu.awake", 28.0)
            val radioActive = queryPower("radio.active", 135.0)
            val wifiActive = queryPower("wifi.active", 85.0)
            val wifiScan = queryPower("wifi.scan", 75.0)
            val gpsActive = queryPower("gps.on", 60.0)

            // Attempt to resolve CPU clusters
            val clusters = mutableListOf<Double>()
            try {
                val getNumClustersMethod = powerProfileClass.getMethod("getNumCpuClusters")
                val getClusterPowerMethod = powerProfileClass.getMethod("getAveragePowerForCpuCluster", Int::class.javaPrimitiveType)
                val numClusters = (getNumClustersMethod.invoke(powerProfileInstance) as? Int) ?: 1
                for (i in 0 until numClusters) {
                    val clusterPower = (getClusterPowerMethod.invoke(powerProfileInstance, i) as? Double) ?: 100.0
                    clusters.add(clusterPower)
                }
            } catch (_: Exception) {
                clusters.add(queryPower("cpu.active", 65.0))
                clusters.add(240.0) // Big cluster fallback
            }

            PowerProfileData(
                cpuAwakeMa = cpuAwake,
                cpuClustersMa = if (clusters.isNotEmpty()) clusters else listOf(55.0, 240.0),
                radioActiveMa = radioActive,
                wifiActiveMa = wifiActive,
                wifiScanMa = wifiScan,
                gpsActiveMa = gpsActive,
                isReflectedFromSystem = true,
                sourceDescription = "Reflected from System PowerProfile"
            )
        } catch (e: Exception) {
            Log.w(TAG, "Reflection on PowerProfile failed (${e.message}), using calibrated fallback profile.")
            PowerProfileData(
                cpuAwakeMa = 28.0,
                cpuClustersMa = listOf(55.0, 240.0),
                radioActiveMa = 135.0,
                wifiActiveMa = 85.0,
                wifiScanMa = 75.0,
                gpsActiveMa = 60.0,
                isReflectedFromSystem = false,
                sourceDescription = "Calibrated Reference Multi-Cluster Profile"
            )
        }
    }
}
