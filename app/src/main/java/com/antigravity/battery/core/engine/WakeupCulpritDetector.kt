package com.antigravity.battery.core.engine

import com.antigravity.battery.core.model.CulpritWarningBadge
import com.antigravity.battery.core.model.UidMetrics

/**
 * Evaluates hardware and alarm metrics to identify runaway offenders preventing Doze (Deep Sleep).
 */
class WakeupCulpritDetector {

    companion object {
        private const val RUNAWAY_WAKELOCK_THRESHOLD_MS = 10 * 60 * 1000L // 10 minutes
        private const val RUNAWAY_RADIO_THRESHOLD_MS = 5 * 60 * 1000L     // 5 minutes
        private const val HIGH_CPU_THRESHOLD_MS = 10 * 60 * 1000L         // 10 minutes
        private const val AGGRESSIVE_WAKEUPS_PER_HOUR = 12.0
    }

    fun detectBadges(
        deltaUid: UidMetrics,
        wakeupAlarms: Int,
        sessionDurationMs: Long,
        isScreenOffDominant: Boolean
    ): List<CulpritWarningBadge> {
        val badges = mutableListOf<CulpritWarningBadge>()

        // 1. Runaway Partial Wakelock (> 10 minutes hold time)
        if (deltaUid.partialWakelockDurationMs >= RUNAWAY_WAKELOCK_THRESHOLD_MS) {
            badges.add(CulpritWarningBadge.RUNAWAY_WAKELOCK)
        }

        // 2. Aggressive Wakeup Alarms
        val sessionHours = (sessionDurationMs.toDouble() / 3_600_000.0).coerceAtLeast(0.25)
        val alarmsPerHour = wakeupAlarms / sessionHours
        if (alarmsPerHour >= AGGRESSIVE_WAKEUPS_PER_HOUR || wakeupAlarms >= 25) {
            badges.add(CulpritWarningBadge.AGGRESSIVE_WAKEUPS)
        }

        // 3. Mobile Radio Keep-Alive
        if (deltaUid.mobileRadioActiveMs >= RUNAWAY_RADIO_THRESHOLD_MS) {
            badges.add(CulpritWarningBadge.RADIO_KEEP_ALIVE)
        }

        // 4. High Background CPU during screen-off
        val totalCpu = deltaUid.cpuUserTimeMs + deltaUid.cpuSystemTimeMs
        if (isScreenOffDominant && totalCpu >= HIGH_CPU_THRESHOLD_MS) {
            badges.add(CulpritWarningBadge.HIGH_BACKGROUND_CPU)
        }

        return badges
    }
}
