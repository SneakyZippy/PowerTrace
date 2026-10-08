package com.antigravity.battery.data.export

import com.antigravity.battery.core.model.ObservationSession
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Exports observation sessions and culprit diagnostics to Markdown reports and JSON.
 */
object DiagnosticExporter {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun exportToMarkdown(session: ObservationSession): String {
        val delta = session.delta ?: return "# Battery Session: ${session.title}\n\n*No delta data available.*"
        val startDateStr = dateFormat.format(Date(session.startTimeMs))
        val endDateStr = session.endTimeMs?.let { dateFormat.format(Date(it)) } ?: "Ongoing"

        val durationHours = delta.durationMs.toDouble() / 3_600_000.0

        val builder = StringBuilder()
        builder.appendLine("# Android Battery Diagnostic Report: ${session.title}")
        builder.appendLine()
        builder.appendLine("## Executive Summary")
        builder.appendLine("- **Window:** $startDateStr to $endDateStr (${String.format(Locale.US, "%.1f", durationHours)} hours)")
        builder.appendLine("- **Battery Level:** ${delta.batteryLevelStart}% -> ${delta.batteryLevelEnd}% (-${delta.batteryPercentDrop}%)")
        builder.appendLine("- **Idle Drain Rate:** ${String.format(Locale.US, "%.2f", delta.drainRatePercentPerHour)} %/hour")
        builder.appendLine("- **Deep Sleep (Doze):** ${delta.deepSleepDurationMs / 60000} minutes")
        builder.appendLine("- **Awake (Screen Off):** ${delta.awakeScreenOffMs / 60000} minutes")
        builder.appendLine("- **Deep Sleep Efficiency:** ${String.format(Locale.US, "%.1f", delta.deepSleepEfficiencyPercent)}%")

        if (delta.isInterrupted) {
            builder.appendLine()
            builder.appendLine("> ⚠️ **Session Interrupted:** ${delta.interruptionReason}")
        }

        builder.appendLine()
        builder.appendLine("## Top Battery & Wakeup Culprits")
        builder.appendLine()
        builder.appendLine("| App Label | Package | Est. mAh | Wakelock Time | Wakeup Alarms | Flags |")
        builder.appendLine("| :--- | :--- | :--- | :--- | :--- | :--- |")

        delta.culprits.take(15).forEach { c ->
            val flags = c.warningBadges.joinToString(", ") { it.name }
            val wakelockMinutes = c.partialWakelockMs / 60000
            val mahStr = String.format(Locale.US, "%.1f", c.totalEstimatedMah)
            builder.appendLine("| ${c.appLabel} | `${c.primaryPackageName}` | **$mahStr** | ${wakelockMinutes}m | ${c.wakeupAlarmCount} | $flags |")
        }

        builder.appendLine()
        builder.appendLine("## Detailed Culprit Analysis")
        delta.culprits.take(5).forEach { c ->
            builder.appendLine("### ${c.appLabel} (`${c.primaryPackageName}` - UID ${c.uid})")
            builder.appendLine("- **Estimated Total Energy:** ${String.format(Locale.US, "%.2f", c.totalEstimatedMah)} mAh")
            builder.appendLine("  - CPU: ${String.format(Locale.US, "%.2f", c.energyBreakdown.cpuMah)} mAh (${c.cpuUserTimeMs / 1000}s user / ${c.cpuSystemTimeMs / 1000}s sys)")
            builder.appendLine("  - Wakelock: ${String.format(Locale.US, "%.2f", c.energyBreakdown.wakelockMah)} mAh (${c.partialWakelockMs / 1000}s hold, ${c.wakelockCount} locks)")
            builder.appendLine("  - Radio / Wi-Fi / GPS: ${String.format(Locale.US, "%.2f", c.energyBreakdown.mobileRadioMah + c.energyBreakdown.wifiMah + c.energyBreakdown.gpsMah)} mAh")
            builder.appendLine("- **Wakeup Alarms:** ${c.wakeupAlarmCount}")

            if (c.topWakelockTags.isNotEmpty()) {
                builder.appendLine("- **Top Wakelock Tags:**")
                c.topWakelockTags.forEach { tag ->
                    builder.appendLine("  - `${tag.tag}`: ${tag.durationMs / 1000}s (${tag.count} holds)")
                }
            }

            if (c.topAlarmActions.isNotEmpty()) {
                builder.appendLine("- **Top Alarm Triggers:**")
                c.topAlarmActions.entries.take(4).forEach { (act, cnt) ->
                    builder.appendLine("  - `$act`: $cnt wakeups")
                }
            }
            builder.appendLine()
        }

        return builder.toString()
    }
}
