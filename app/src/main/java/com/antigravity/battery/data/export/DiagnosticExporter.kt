package com.antigravity.battery.data.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.antigravity.battery.core.model.ActionableInsight
import com.antigravity.battery.core.model.InsightSeverity
import com.antigravity.battery.core.model.ObservationSession
import com.antigravity.battery.core.model.OverallAnalytics
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generates rich HTML, CSV, and text diagnostic reports and coordinates sharing.
 */
object DiagnosticExporter {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    private val dateOnlyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /**
     * Shares a generated report file using Android's system share sheet and FileProvider.
     */
    fun shareReport(
        context: Context,
        content: String,
        fileName: String,
        mimeType: String,
        chooserTitle: String
    ) {
        try {
            val reportsDir = File(context.cacheDir, "reports")
            if (!reportsDir.exists()) reportsDir.mkdirs()

            val file = File(reportsDir, fileName)
            file.writeText(content, Charsets.UTF_8)

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, fileName.substringBeforeLast('.'))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, chooserTitle).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to share report: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Copies text summary directly to the Android system clipboard.
     */
    fun copyToClipboard(context: Context, text: String, label: String = "Battery Diagnostics") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Diagnostic summary copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    /**
     * Generates a modern, responsive, standalone dark-theme HTML report with embedded styles.
     */
    fun exportToHtml(analytics: OverallAnalytics, deviceName: String = "Google Pixel 9a"): String {
        val nowStr = dateFormat.format(Date())
        val net = analytics.networkComparison
        val hw = analytics.hardwareEnergy
        val totalHw = hw.totalMah.coerceAtLeast(1.0)

        val cpuPct = (hw.cpuMah / totalHw * 100.0).toInt()
        val wlPct = (hw.wakelockMah / totalHw * 100.0).toInt()
        val radioPct = (hw.radioMah / totalHw * 100.0).toInt()
        val wifiSensorsMah = hw.wifiMah + hw.gpsMah
        val wifiSensorsPct = (wifiSensorsMah / totalHw * 100.0).toInt()

        val html = StringBuilder()
        html.appendLine("<!DOCTYPE html>")
        html.appendLine("<html lang=\"en\">")
        html.appendLine("<head>")
        html.appendLine("  <meta charset=\"UTF-8\">")
        html.appendLine("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">")
        html.appendLine("  <title>PowerTrace Battery Diagnostic Report - $deviceName</title>")
        html.appendLine("  <style>")
        html.appendLine("    :root {")
        html.appendLine("      --bg: #0d1117;")
        html.appendLine("      --card-bg: #161b22;")
        html.appendLine("      --card-border: #30363d;")
        html.appendLine("      --text-primary: #f0f6fc;")
        html.appendLine("      --text-secondary: #8b949e;")
        html.appendLine("      --green: #2ea043;")
        html.appendLine("      --amber: #d29922;")
        html.appendLine("      --blue: #58a6ff;")
        html.appendLine("      --red: #f85149;")
        html.appendLine("    }")
        html.appendLine("    * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; }")
        html.appendLine("    body { background-color: var(--bg); color: var(--text-primary); padding: 24px; line-height: 1.5; }")
        html.appendLine("    .container { max-width: 900px; margin: 0 auto; }")
        html.appendLine("    header { border-bottom: 1px solid var(--card-border); padding-bottom: 16px; margin-bottom: 24px; }")
        html.appendLine("    h1 { font-size: 24px; font-weight: 700; color: var(--text-primary); margin-bottom: 4px; }")
        html.appendLine("    .subtitle { font-size: 14px; color: var(--green); font-weight: 600; }")
        html.appendLine("    .meta { font-size: 12px; color: var(--text-secondary); margin-top: 6px; }")
        html.appendLine("    .grid-4 { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 12px; margin-bottom: 24px; }")
        html.appendLine("    .stat-card { background: var(--card-bg); border: 1px solid var(--card-border); border-radius: 10px; padding: 16px; }")
        html.appendLine("    .stat-label { font-size: 11px; text-transform: uppercase; font-weight: 700; letter-spacing: 0.5px; color: var(--text-secondary); margin-bottom: 4px; }")
        html.appendLine("    .stat-val { font-size: 26px; font-weight: 800; color: var(--text-primary); }")
        html.appendLine("    .stat-unit { font-size: 14px; color: var(--text-secondary); font-weight: 500; }")
        html.appendLine("    .card { background: var(--card-bg); border: 1px solid var(--card-border); border-radius: 10px; padding: 20px; margin-bottom: 24px; }")
        html.appendLine("    .card-title { font-size: 16px; font-weight: 700; margin-bottom: 14px; display: flex; align-items: center; justify-content: space-between; }")
        html.appendLine("    .insight { padding: 12px 16px; border-radius: 8px; margin-bottom: 10px; border-left: 4px solid var(--green); background: rgba(46, 160, 67, 0.1); }")
        html.appendLine("    .insight.WARNING { border-left-color: var(--amber); background: rgba(210, 153, 34, 0.1); }")
        html.appendLine("    .insight.ALERT { border-left-color: var(--red); background: rgba(248, 81, 73, 0.1); }")
        html.appendLine("    .insight.INFO { border-left-color: var(--blue); background: rgba(88, 166, 255, 0.1); }")
        html.appendLine("    .insight-title { font-weight: 700; font-size: 14px; margin-bottom: 2px; }")
        html.appendLine("    .insight-desc { font-size: 13px; color: var(--text-secondary); }")
        html.appendLine("    .progress-bar { display: flex; height: 12px; border-radius: 6px; overflow: hidden; margin-bottom: 16px; background: #21262d; }")
        html.appendLine("    .bar-cpu { background: var(--green); }")
        html.appendLine("    .bar-wl { background: var(--amber); }")
        html.appendLine("    .bar-radio { background: var(--blue); }")
        html.appendLine("    .bar-other { background: var(--text-secondary); }")
        html.appendLine("    .hw-row { display: flex; justify-content: space-between; align-items: center; padding: 6px 0; border-bottom: 1px solid rgba(255,255,255,0.05); font-size: 13px; }")
        html.appendLine("    .hw-dot { display: inline-block; width: 10px; height: 10px; border-radius: 50%; margin-right: 8px; }")
        html.appendLine("    table { width: 100%; border-collapse: collapse; font-size: 13px; text-align: left; }")
        html.appendLine("    th { color: var(--text-secondary); padding: 8px 12px; border-bottom: 1px solid var(--card-border); font-weight: 600; }")
        html.appendLine("    td { padding: 10px 12px; border-bottom: 1px solid rgba(255,255,255,0.05); }")
        html.appendLine("    .tag { font-family: monospace; font-size: 11px; padding: 2px 6px; background: rgba(255,255,255,0.1); border-radius: 4px; }")
        html.appendLine("    .pill { font-size: 11px; font-weight: 600; padding: 2px 8px; border-radius: 12px; display: inline-block; }")
        html.appendLine("    .pill-green { background: rgba(46, 160, 67, 0.2); color: var(--green); }")
        html.appendLine("    .pill-amber { background: rgba(210, 153, 34, 0.2); color: var(--amber); }")
        html.appendLine("    .net-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }")
        html.appendLine("    .net-col { background: #21262d; padding: 14px; border-radius: 8px; }")
        html.appendLine("    footer { text-align: center; font-size: 12px; color: var(--text-secondary); margin-top: 32px; padding-top: 16px; border-top: 1px solid var(--card-border); }")
        html.appendLine("  </style>")
        html.appendLine("</head>")
        html.appendLine("<body>")
        html.appendLine("<div class=\"container\">")

        // Header
        html.appendLine("  <header>")
        html.appendLine("    <h1>⚡ PowerTrace Battery Diagnostic</h1>")
        html.appendLine("    <div class=\"subtitle\">${analytics.totalDays} Days Monitored • ${analytics.totalSnapshots} Continuous Snapshots</div>")
        html.appendLine("    <div class=\"meta\">Device: $deviceName • Exported: $nowStr</div>")
        html.appendLine("  </header>")

        // Hero Stats
        html.appendLine("  <div class=\"grid-4\">")
        html.appendLine("    <div class=\"stat-card\"><div class=\"stat-label\">Total Drain</div><div class=\"stat-val\">-${analytics.totalDischargeDrop}<span class=\"stat-unit\">%</span></div></div>")
        html.appendLine("    <div class=\"stat-card\"><div class=\"stat-label\">Avg Drain Rate</div><div class=\"stat-val\">${String.format(Locale.US, "%.1f", analytics.avgDrainRatePerHour)}<span class=\"stat-unit\">%/hr</span></div></div>")
        html.appendLine("    <div class=\"stat-card\"><div class=\"stat-label\">Doze Efficiency</div><div class=\"stat-val\">${analytics.avgDeepSleepEfficiency.toInt()}<span class=\"stat-unit\">%</span></div></div>")
        html.appendLine("    <div class=\"stat-card\"><div class=\"stat-label\">Avg Cell Temp</div><div class=\"stat-val\">${String.format(Locale.US, "%.1f", analytics.avgTempC)}<span class=\"stat-unit\">°C</span></div></div>")
        html.appendLine("  </div>")

        // Insights Section
        if (analytics.insights.isNotEmpty()) {
            html.appendLine("  <div class=\"card\">")
            html.appendLine("    <div class=\"card-title\">Diagnostic Findings & Insights</div>")
            analytics.insights.forEach { ins ->
                val cls = ins.severity.name
                html.appendLine("    <div class=\"insight $cls\">")
                html.appendLine("      <div class=\"insight-title\">${escapeHtml(ins.title)}</div>")
                html.appendLine("      <div class=\"insight-desc\">${escapeHtml(ins.description)}</div>")
                html.appendLine("    </div>")
            }
            html.appendLine("  </div>")
        }

        // Hardware Attribution
        html.appendLine("  <div class=\"card\">")
        html.appendLine("    <div class=\"card-title\">Hardware Subsystem Energy Attribution <span>Total: ${hw.totalMah.toInt()} mAh</span></div>")
        html.appendLine("    <div class=\"progress-bar\">")
        if (hw.cpuMah > 0) html.appendLine("      <div class=\"bar-cpu\" style=\"flex: ${hw.cpuMah}\"></div>")
        if (hw.wakelockMah > 0) html.appendLine("      <div class=\"bar-wl\" style=\"flex: ${hw.wakelockMah}\"></div>")
        if (hw.radioMah > 0) html.appendLine("      <div class=\"bar-radio\" style=\"flex: ${hw.radioMah}\"></div>")
        if (wifiSensorsMah > 0) html.appendLine("      <div class=\"bar-other\" style=\"flex: ${wifiSensorsMah}\"></div>")
        html.appendLine("    </div>")
        html.appendLine("    <div class=\"hw-row\"><div><span class=\"hw-dot\" style=\"background: var(--green)\"></span>CPU Core Computation</div><div><strong>${hw.cpuMah.toInt()} mAh</strong> ($cpuPct%)</div></div>")
        html.appendLine("    <div class=\"hw-row\"><div><span class=\"hw-dot\" style=\"background: var(--amber)\"></span>Screen-Off Partial Wakelocks</div><div><strong>${hw.wakelockMah.toInt()} mAh</strong> ($wlPct%)</div></div>")
        html.appendLine("    <div class=\"hw-row\"><div><span class=\"hw-dot\" style=\"background: var(--blue)\"></span>Cellular Radio Modem</div><div><strong>${hw.radioMah.toInt()} mAh</strong> ($radioPct%)</div></div>")
        html.appendLine("    <div class=\"hw-row\"><div><span class=\"hw-dot\" style=\"background: var(--text-secondary)\"></span>Wi-Fi & Sensors</div><div><strong>${wifiSensorsMah.toInt()} mAh</strong> ($wifiSensorsPct%)</div></div>")
        html.appendLine("  </div>")

        // Network Comparison
        html.appendLine("  <div class=\"card\">")
        html.appendLine("    <div class=\"card-title\">Network & Radio Standby Comparison <span>Cellular penalty: ${net.cellularPenaltyMultiplier}x</span></div>")
        html.appendLine("    <div class=\"net-grid\">")
        html.appendLine("      <div class=\"net-col\">")
        html.appendLine("        <h3 style=\"color: var(--green); margin-bottom: 8px;\">Wi-Fi Network</h3>")
        html.appendLine("        <div>Active Time: <strong>${net.wifiHours}h</strong></div>")
        html.appendLine("        <div>Data Traffic: <strong>${net.wifiDataMb.toInt()} MB</strong></div>")
        html.appendLine("        <div>Standby Drain Rate: <strong>${net.wifiDrainRatePerHour}%/hr</strong></div>")
        html.appendLine("        <div>Radio Energy: <strong>${net.wifiEnergyMah.toInt()} mAh</strong></div>")
        html.appendLine("      </div>")
        html.appendLine("      <div class=\"net-col\">")
        html.appendLine("        <h3 style=\"color: var(--blue); margin-bottom: 8px;\">Cellular Modem (LTE/5G)</h3>")
        html.appendLine("        <div>Active Time: <strong>${net.cellularHours}h</strong></div>")
        html.appendLine("        <div>Data Traffic: <strong>${net.cellularDataMb.toInt()} MB</strong></div>")
        html.appendLine("        <div>Standby Drain Rate: <strong>${net.cellularDrainRatePerHour}%/hr</strong></div>")
        html.appendLine("        <div>Modem Energy: <strong>${net.cellularEnergyMah.toInt()} mAh</strong></div>")
        html.appendLine("      </div>")
        html.appendLine("    </div>")
        html.appendLine("  </div>")

        // Top Wakelock Offenders
        if (analytics.topWakelockLeaders.isNotEmpty()) {
            html.appendLine("  <div class=\"card\">")
            html.appendLine("    <div class=\"card-title\">Top Standby Wakelock Offenders</div>")
            html.appendLine("    <table>")
            html.appendLine("      <thead><tr><th>App</th><th>Hold Time</th><th>Wakes</th><th>Top Tag</th><th>Potential Savings</th></tr></thead>")
            html.appendLine("      <tbody>")
            analytics.topWakelockLeaders.take(10).forEach { wl ->
                val h = wl.durationMs / 3600000L
                val m = (wl.durationMs % 3600000L) / 60000L
                val timeStr = if (h > 0) "${h}h ${m}m" else "${m}m"
                html.appendLine("        <tr>")
                html.appendLine("          <td><strong>${escapeHtml(wl.appLabel)}</strong><br><small style=\"color:var(--text-secondary)\">${escapeHtml(wl.packageName)}</small></td>")
                html.appendLine("          <td>$timeStr</td>")
                html.appendLine("          <td>${wl.count}</td>")
                html.appendLine("          <td><span class=\"tag\">${escapeHtml(wl.topTag.take(35))}</span></td>")
                html.appendLine("          <td><span class=\"pill pill-amber\">Save ~${wl.potentialSavingsMah.toInt()} mAh</span></td>")
                html.appendLine("        </tr>")
            }
            html.appendLine("      </tbody>")
            html.appendLine("    </table>")
            html.appendLine("  </div>")
        }

        // Daily History Table
        if (analytics.dailySummaries.isNotEmpty()) {
            html.appendLine("  <div class=\"card\">")
            html.appendLine("    <div class=\"card-title\">Daily Performance History</div>")
            html.appendLine("    <table>")
            html.appendLine("      <thead><tr><th>Date</th><th>Battery Change</th><th>Drain Rate</th><th>Deep Sleep</th><th>Thermals</th></tr></thead>")
            html.appendLine("      <tbody>")
            analytics.dailySummaries.reversed().forEach { d ->
                val dozeClass = if (d.deepSleepEfficiency >= 70.0) "pill-green" else "pill-amber"
                html.appendLine("        <tr>")
                html.appendLine("          <td><strong>${d.dateString}</strong></td>")
                html.appendLine("          <td>${d.startLevel}% &rarr; ${d.endLevel}% (-${d.dropPercent}%)</td>")
                html.appendLine("          <td>${String.format(Locale.US, "%.2f", d.drainRatePerHour)}%/hr</td>")
                html.appendLine("          <td><span class=\"pill $dozeClass\">${d.deepSleepEfficiency.toInt()}% Doze</span> (${d.deepSleepHours}h sleep / ${d.awakeScreenOffHours}h awake)</td>")
                html.appendLine("          <td>Avg ${String.format(Locale.US, "%.1f", d.avgTempC)}°C (Peak ${d.maxTempC}°C)</td>")
                html.appendLine("        </tr>")
            }
            html.appendLine("      </tbody>")
            html.appendLine("    </table>")
            html.appendLine("  </div>")
        }

        // Footer
        html.appendLine("  <footer>")
        html.appendLine("    PowerTrace Continuous Battery Diagnostics • Privacy-Preserving On-Device Telemetry")
        html.appendLine("  </footer>")

        html.appendLine("</div>")
        html.appendLine("</body>")
        html.appendLine("</html>")

        return html.toString()
    }

    /**
     * Exports tabular daily metrics and culprit rollups as standard CSV.
     */
    fun exportToCsv(analytics: OverallAnalytics): String {
        val csv = StringBuilder()
        csv.appendLine("# PowerTrace Battery Diagnostics CSV Export")
        csv.appendLine("# ExportedAt,${dateFormat.format(Date())}")
        csv.appendLine("# MonitoredDays,${analytics.totalDays}")
        csv.appendLine("# TotalDrainPercent,${analytics.totalDischargeDrop}")
        csv.appendLine("# AvgDrainRatePerHour,${analytics.avgDrainRatePerHour}")
        csv.appendLine("# DozeEfficiencyPercent,${analytics.avgDeepSleepEfficiency}")
        csv.appendLine()

        // Daily Performance Table
        csv.appendLine("## Daily Performance History")
        csv.appendLine("Date,StartPercent,EndPercent,DropPercent,DischargeHours,DrainRatePerHour,ScreenOnHours,DeepSleepHours,AwakeScreenOffHours,DozeEfficiencyPercent,AvgTempC,MaxTempC")
        analytics.dailySummaries.forEach { d ->
            csv.appendLine("${d.dateString},${d.startLevel},${d.endLevel},${d.dropPercent},${d.dischargeHours},${d.drainRatePerHour},${d.screenOnHours},${d.deepSleepHours},${d.awakeScreenOffHours},${d.deepSleepEfficiency},${d.avgTempC},${d.maxTempC}")
        }
        csv.appendLine()

        // Hardware Energy Breakdown Table
        csv.appendLine("## Hardware Energy Attribution (mAh)")
        csv.appendLine("Subsystem,EstimatedEnergyMah,PercentShare")
        val total = analytics.hardwareEnergy.totalMah.coerceAtLeast(1.0)
        csv.appendLine("CPU Core Computation,${analytics.hardwareEnergy.cpuMah},${(analytics.hardwareEnergy.cpuMah / total * 100.0).toInt()}%")
        csv.appendLine("Screen-Off Partial Wakelocks,${analytics.hardwareEnergy.wakelockMah},${(analytics.hardwareEnergy.wakelockMah / total * 100.0).toInt()}%")
        csv.appendLine("Cellular Radio Modem,${analytics.hardwareEnergy.radioMah},${(analytics.hardwareEnergy.radioMah / total * 100.0).toInt()}%")
        csv.appendLine("Wi-Fi & Sensors,${analytics.hardwareEnergy.wifiMah + analytics.hardwareEnergy.gpsMah},${((analytics.hardwareEnergy.wifiMah + analytics.hardwareEnergy.gpsMah) / total * 100.0).toInt()}%")
        csv.appendLine()

        // Top Wakelock Leaders Table
        csv.appendLine("## Top Standby Wakelock Leaders")
        csv.appendLine("AppLabel,PackageName,DurationMinutes,WakeCount,TopTag,PotentialSavingsMah")
        analytics.topWakelockLeaders.forEach { wl ->
            csv.appendLine("\"${wl.appLabel}\",\"${wl.packageName}\",${wl.durationMs / 60000L},${wl.count},\"${wl.topTag}\",${wl.potentialSavingsMah}")
        }
        csv.appendLine()

        // Top Alarm Leaders Table
        csv.appendLine("## Top Standby Wakeup Alarms")
        csv.appendLine("AppLabel,PackageName,WakeupCount,TopAction,PotentialSavingsMah")
        analytics.topAlarmLeaders.forEach { al ->
            csv.appendLine("\"${al.appLabel}\",\"${al.packageName}\",${al.wakeupCount},\"${al.topAction}\",${al.potentialSavingsMah}")
        }

        return csv.toString()
    }

    /**
     * Plain text / markdown summary for clipboard copying or quick messaging.
     */
    fun exportSummaryText(analytics: OverallAnalytics): String {
        val sb = StringBuilder()
        sb.appendLine("🔋 PowerTrace Battery Diagnostic Summary")
        sb.appendLine("Period: ${analytics.totalDays} Days Monitored (${analytics.totalSnapshots} snapshots)")
        sb.appendLine("• Total Discharge: -${analytics.totalDischargeDrop}% | Avg Drain Rate: ${String.format(Locale.US, "%.1f", analytics.avgDrainRatePerHour)}%/hr")
        sb.appendLine("• Doze Deep Sleep Efficiency: ${analytics.avgDeepSleepEfficiency.toInt()}% | Avg Cell Temp: ${String.format(Locale.US, "%.1f", analytics.avgTempC)}°C")
        sb.appendLine()
        sb.appendLine("⚙️ Hardware Energy Attribution:")
        val total = analytics.hardwareEnergy.totalMah.coerceAtLeast(1.0)
        sb.appendLine("• CPU: ${analytics.hardwareEnergy.cpuMah.toInt()} mAh (${(analytics.hardwareEnergy.cpuMah / total * 100.0).toInt()}%)")
        sb.appendLine("• Cellular Radio: ${analytics.hardwareEnergy.radioMah.toInt()} mAh (${(analytics.hardwareEnergy.radioMah / total * 100.0).toInt()}%)")
        sb.appendLine("• Wakelocks: ${analytics.hardwareEnergy.wakelockMah.toInt()} mAh (${(analytics.hardwareEnergy.wakelockMah / total * 100.0).toInt()}%)")
        sb.appendLine("• Wi-Fi & Sensors: ${(analytics.hardwareEnergy.wifiMah + analytics.hardwareEnergy.gpsMah).toInt()} mAh (${((analytics.hardwareEnergy.wifiMah + analytics.hardwareEnergy.gpsMah) / total * 100.0).toInt()}%)")
        sb.appendLine()
        sb.appendLine("📡 Radio Standby Comparison:")
        sb.appendLine("• Wi-Fi: ${analytics.networkComparison.wifiDrainRatePerHour}%/hr (${analytics.networkComparison.wifiHours}h)")
        sb.appendLine("• Cellular: ${analytics.networkComparison.cellularDrainRatePerHour}%/hr (${analytics.networkComparison.cellularHours}h) -> ${analytics.networkComparison.cellularPenaltyMultiplier}x drain penalty")
        sb.appendLine()
        if (analytics.topWakelockLeaders.isNotEmpty()) {
            sb.appendLine("⚠️ Top Standby Wakelocks:")
            analytics.topWakelockLeaders.take(5).forEach { wl ->
                sb.appendLine("• ${wl.appLabel}: ${wl.durationMs / 60000L}m hold (${wl.count} events) [Save ~${wl.potentialSavingsMah.toInt()} mAh]")
            }
        }
        return sb.toString()
    }

    private fun escapeHtml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

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

        return builder.toString()
    }
}
