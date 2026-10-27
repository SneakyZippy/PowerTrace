package com.antigravity.battery.core.engine

import android.content.Context
import com.antigravity.battery.core.model.ChargeTelemetrySample
import com.antigravity.battery.core.model.ChargeTestSession
import com.antigravity.battery.core.model.LiveChargeTelemetry
import com.antigravity.battery.service.ChargingNotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

object MockChargeSimulator {

    fun simulate(
        scope: CoroutineScope,
        context: Context,
        activeBrickId: String?,
        activeCableId: String?,
        activeBrickName: String,
        activeCableName: String,
        onTelemetryUpdated: (LiveChargeTelemetry) -> Unit,
        onSessionCompleted: (ChargeTestSession, List<ChargeTelemetrySample>) -> Unit,
        onComplete: () -> Unit
    ): Job {
        return scope.launch {
            val simSessionId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            val simStartTime = now - (12 * 60 * 1000L) // 12 min simulated duration
            val simSamples = mutableListOf<ChargeTelemetrySample>()

            var simPeakWattage = 0f
            var simPeakCurrentMa = 0
            var simWattageSum = 0.0
            var simSampleCount = 0
            var simMaxTemp = 295

            val mockWatts = listOf(15.2f, 25.8f, 38.5f, 44.6f, 42.1f, 36.4f, 30.8f)
            for ((index, w) in mockWatts.withIndex()) {
                val voltageMv = 4200 + (index * 20)
                val currentMa = ((w * 1_000_000f) / voltageMv).toInt()
                val temp = 300 + (index * 10)

                simPeakWattage = maxOf(simPeakWattage, w)
                simPeakCurrentMa = maxOf(simPeakCurrentMa, currentMa)
                simMaxTemp = maxOf(simMaxTemp, temp)
                simWattageSum += w
                simSampleCount++

                val sample = ChargeTelemetrySample(
                    sessionId = simSessionId,
                    timestampMs = now - ((mockWatts.size - index) * 1000L),
                    batteryLevel = 22 + (index * 2),
                    voltageMv = voltageMv,
                    currentMa = currentMa,
                    wattage = w,
                    tempDeciC = temp
                )
                simSamples.add(sample)

                onTelemetryUpdated(
                    LiveChargeTelemetry(
                        isCharging = true,
                        chargePlugType = "AC Wall Charger (Simulation)",
                        currentWattage = w,
                        currentMa = currentMa,
                        voltageMv = voltageMv,
                        batteryLevelPercent = 22 + (index * 2),
                        tempDeciCelsius = temp,
                        peakWattageSoFar = simPeakWattage,
                        avgWattageSoFar = (simWattageSum / simSampleCount).toFloat(),
                        sessionDurationMs = 12 * 60 * 1000L,
                        isThrottlingDetected = false
                    )
                )
                delay(600L)
            }

            // Conclude simulation session
            val simSession = ChargeTestSession(
                id = simSessionId,
                brickId = activeBrickId,
                cableId = activeCableId,
                brickName = activeBrickName.ifBlank { "Stock OEM Charger" },
                cableName = activeCableName.ifBlank { "Stock USB-C Cable" },
                startTimeMs = simStartTime,
                endTimeMs = System.currentTimeMillis(),
                startBatteryLevel = 22,
                endBatteryLevel = 36,
                peakWattage = simPeakWattage,
                avgWattage = (simWattageSum / simSampleCount).toFloat(),
                minVoltageMv = 4200,
                maxVoltageMv = 4340,
                peakCurrentMa = simPeakCurrentMa,
                startTempDeciC = 295,
                maxTempDeciC = simMaxTemp,
                isThrottled = false
            )

            onSessionCompleted(simSession, simSamples)
            ChargingNotificationHelper.showSessionFinishedNotification(context, simSession)

            onComplete()
        }
    }
}
