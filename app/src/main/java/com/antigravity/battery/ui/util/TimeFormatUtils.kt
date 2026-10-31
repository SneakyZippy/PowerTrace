package com.antigravity.battery.ui.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object TimeFormatUtils {

    /**
     * Formats start and end timestamps into a clear range.
     * E.g.:
     *   Same day: "22:15 → 23:45"
     *   Crosses midnight (+1 day): "22:45 → 01:37 (+1d)"
     *   Crosses multiple days: "Oct 30, 22:45 → Nov 01, 01:37"
     */
    fun formatTimeRange(startTimeMs: Long, endTimeMs: Long): String {
        if (startTimeMs <= 0L) return "--"
        val effectiveEndMs = if (endTimeMs >= startTimeMs) endTimeMs else startTimeMs

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val startStr = timeFormat.format(Date(startTimeMs))
        val endStr = timeFormat.format(Date(effectiveEndMs))

        val startCal = Calendar.getInstance().apply { timeInMillis = startTimeMs }
        val endCal = Calendar.getInstance().apply { timeInMillis = effectiveEndMs }

        val startDay = startCal.get(Calendar.DAY_OF_YEAR)
        val endDay = endCal.get(Calendar.DAY_OF_YEAR)
        val startYear = startCal.get(Calendar.YEAR)
        val endYear = endCal.get(Calendar.YEAR)

        return when {
            startYear == endYear && startDay == endDay -> {
                "$startStr → $endStr"
            }
            startYear == endYear && (endDay - startDay == 1) -> {
                "$startStr → $endStr (+1d)"
            }
            else -> {
                val dayFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                "${dayFormat.format(Date(startTimeMs))} → ${dayFormat.format(Date(effectiveEndMs))}"
            }
        }
    }

    /**
     * Formats duration in milliseconds into a concise, human-readable string.
     * E.g.:
     *   < 1 min: "< 1m"
     *   5 min: "5m"
     *   2 hours 52 min: "2h 52m"
     *   1 day 4 hours: "1d 4h"
     */
    fun formatDurationHuman(durationMs: Long): String {
        if (durationMs < 60_000L) {
            return "< 1m"
        }
        val totalMinutes = durationMs / 60_000L
        val days = totalMinutes / (24 * 60)
        val hours = (totalMinutes % (24 * 60)) / 60
        val minutes = totalMinutes % 60

        return when {
            days > 0 -> {
                if (hours > 0) "${days}d ${hours}h" else "${days}d"
            }
            hours > 0 -> {
                if (minutes > 0) "${hours}h ${minutes}m" else "${hours}h"
            }
            else -> {
                "${minutes}m"
            }
        }
    }

    /**
     * Formats a timestamp into a human-friendly date header for grouped lists.
     * E.g. "Today", "Yesterday", "Oct 30, 2026"
     */
    fun formatDateHeader(timestampMs: Long): String {
        if (timestampMs <= 0L) return "Unknown Date"
        val now = System.currentTimeMillis()
        val nowCal = Calendar.getInstance().apply { timeInMillis = now }
        val targetCal = Calendar.getInstance().apply { timeInMillis = timestampMs }

        val nowYear = nowCal.get(Calendar.YEAR)
        val targetYear = targetCal.get(Calendar.YEAR)
        val nowDay = nowCal.get(Calendar.DAY_OF_YEAR)
        val targetDay = targetCal.get(Calendar.DAY_OF_YEAR)

        return when {
            nowYear == targetYear && nowDay == targetDay -> "Today"
            nowYear == targetYear && (nowDay - targetDay == 1) -> "Yesterday"
            nowYear == targetYear -> SimpleDateFormat("EEEE, MMM dd", Locale.getDefault()).format(Date(timestampMs))
            else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestampMs))
        }
    }
}
