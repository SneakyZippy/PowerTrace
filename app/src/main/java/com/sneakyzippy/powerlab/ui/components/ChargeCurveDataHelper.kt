package com.sneakyzippy.powerlab.ui.components

import com.sneakyzippy.powerlab.core.model.ChargeTelemetrySample
import com.sneakyzippy.powerlab.core.model.ChargeTestSession

object ChargeCurveDataHelper {

    /**
     * Reconciles samples with the session boundary. If telemetry stopped recording early
     * (e.g. Due to sample caps in earlier versions), smoothly bridges the timeline
     * to the session's verified endTime and endBatteryLevel.
     */
    fun reconcileSamples(
        samples: List<ChargeTelemetrySample>,
        session: ChargeTestSession?
    ): List<ChargeTelemetrySample> {
        if (session == null || samples.isEmpty()) return samples

        val lastSample = samples.last()
        if (session.endTimeMs <= lastSample.timestampMs + 30_000L) {
            return samples
        }

        val padded = samples.toMutableList()
        val steps = 8
        val timeStep = (session.endTimeMs - lastSample.timestampMs) / steps
        val levelDiff = session.endBatteryLevel - lastSample.batteryLevel

        for (s in 1..steps) {
            val t = lastSample.timestampMs + (timeStep * s)
            val interpolatedLevel = lastSample.batteryLevel + ((levelDiff * s) / steps)
            padded.add(
                ChargeTelemetrySample(
                    sessionId = session.id,
                    timestampMs = t,
                    batteryLevel = interpolatedLevel,
                    voltageMv = lastSample.voltageMv,
                    currentMa = (lastSample.currentMa * (steps - s + 1) / steps).coerceAtLeast(0),
                    wattage = (lastSample.wattage * (steps - s + 1) / steps).coerceAtLeast(0f),
                    tempDeciC = session.maxTempDeciC.coerceAtLeast(lastSample.tempDeciC)
                )
            )
        }
        return padded
    }

    /**
     * Decimates samples for smooth 60fps canvas rendering if sample count exceeds maxPoints.
     */
    fun downsample(samples: List<ChargeTelemetrySample>, maxPoints: Int = 120): List<ChargeTelemetrySample> {
        if (samples.size <= maxPoints) return samples
        val step = samples.size.toDouble() / maxPoints
        return (0 until maxPoints).map { i ->
            samples[(i * step).toInt().coerceAtMost(samples.lastIndex)]
        }
    }
}
