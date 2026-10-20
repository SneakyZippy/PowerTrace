package com.antigravity.battery.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.math.roundToInt
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.battery.core.model.ActionableInsight
import com.antigravity.battery.core.model.AlarmLeader
import com.antigravity.battery.core.model.DailyAnalyticsSummary
import com.antigravity.battery.core.model.HardwareEnergyBreakdown
import com.antigravity.battery.core.model.InsightSeverity
import com.antigravity.battery.core.model.OverallAnalytics
import com.antigravity.battery.core.model.WakelockLeader
import com.antigravity.battery.ui.components.MetricStatTile
import com.antigravity.battery.ui.theme.AccentBlue
import com.antigravity.battery.ui.theme.CulpritAmber
import com.antigravity.battery.ui.theme.CulpritRed
import com.antigravity.battery.ui.theme.DarkBackground
import com.antigravity.battery.ui.theme.DarkSurface
import com.antigravity.battery.ui.theme.DarkSurfaceVariant
import com.antigravity.battery.ui.theme.GreenPrimary
import com.antigravity.battery.ui.theme.TextPrimary
import com.antigravity.battery.ui.theme.TextSecondary
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import com.antigravity.battery.core.model.NetworkDrainComparison
import com.antigravity.battery.data.export.DiagnosticExporter
import java.util.Locale

enum class StandbyOffenderTab(val label: String) {
    WAKELOCKS("Partial Wakelocks 🔒"),
    ALARMS("Wakeup Alarms ⏰")
}

@Composable
fun AnalyticsScreen(
    analytics: OverallAnalytics?,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedOffenderTab by remember { mutableStateOf(StandbyOffenderTab.WAKELOCKS) }
    var showExportSheet by remember { mutableStateOf(false) }

    if (isLoading && analytics == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(DarkBackground),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = GreenPrimary)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Analyzing continuous telemetry...", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            }
        }
        return
    }

    if (analytics == null || analytics.totalSnapshots == 0) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Telemetry Collected Yet",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Ensure 24/7 background tracking is enabled. Telemetry snapshots will automatically populate analytics here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Battery Analytics",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "${analytics.totalDays} Days Monitored • ${analytics.totalSnapshots} Snapshots",
                            style = MaterialTheme.typography.bodySmall,
                            color = GreenPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { showExportSheet = true },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(DarkSurface)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share Diagnostic Report",
                                tint = AccentBlue
                            )
                        }

                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(DarkSurface)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Analytics",
                                tint = GreenPrimary
                            )
                        }
                    }
                }
            }

        // Hero Metric Cards Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricStatTile(
                        label = "TOTAL DRAIN",
                        value = "-${analytics.totalDischargeDrop}",
                        unit = "%",
                        valueColor = if (analytics.totalDischargeDrop > 100) CulpritAmber else GreenPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricStatTile(
                        label = "AVG DRAIN RATE",
                        value = String.format(Locale.US, "%.1f", analytics.avgDrainRatePerHour),
                        unit = "%/hr",
                        valueColor = if (analytics.avgDrainRatePerHour > 3.0) CulpritAmber else GreenPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricStatTile(
                        label = "DOZE EFFICIENCY",
                        value = "${analytics.avgDeepSleepEfficiency.toInt()}",
                        unit = "%",
                        valueColor = if (analytics.avgDeepSleepEfficiency >= 75) GreenPrimary else if (analytics.avgDeepSleepEfficiency >= 60) CulpritAmber else CulpritRed,
                        modifier = Modifier.weight(1f)
                    )
                    MetricStatTile(
                        label = "AVG CELL TEMP",
                        value = String.format(Locale.US, "%.1f", analytics.avgTempC),
                        unit = "°C",
                        valueColor = if (analytics.avgTempC > 35) CulpritAmber else GreenPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Actionable Insights Section
        if (analytics.insights.isNotEmpty()) {
            item {
                Text(
                    text = "Diagnostic Insights & Findings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    analytics.insights.forEach { insight ->
                        InsightCard(insight = insight)
                    }
                }
            }
        }

        // Day-by-Day Historical Trend
        item {
            Text(
                text = "Daily Performance History",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                analytics.dailySummaries.reversed().forEach { day ->
                    DailySummaryRow(day = day)
                }
            }
        }

        // Hardware Subsystem Energy Attribution
        item {
            Text(
                text = "Hardware Energy Attribution",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            HardwareAttributionCard(breakdown = analytics.hardwareEnergy)
        }

        // Network & Modem Standby Comparison
        item {
            Text(
                text = "Network & Radio Standby Comparison",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            NetworkDrainComparisonCard(comparison = analytics.networkComparison)
        }

        // Standby Inhibitors (Wakelocks & Alarms)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Standby & Sleep Culprits",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StandbyOffenderTab.values().forEach { tab ->
                    val isSelected = tab == selectedOffenderTab
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedOffenderTab = tab },
                        label = { Text(tab.label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GreenPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = GreenPrimary,
                            containerColor = DarkSurface,
                            labelColor = TextSecondary
                        )
                    )
                }
            }
        }

        when (selectedOffenderTab) {
            StandbyOffenderTab.WAKELOCKS -> {
                if (analytics.topWakelockLeaders.isEmpty()) {
                    item {
                        Text("No persistent partial wakelocks recorded.", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    items(analytics.topWakelockLeaders) { leader ->
                        WakelockLeaderRow(leader = leader)
                    }
                }
            }
            StandbyOffenderTab.ALARMS -> {
                if (analytics.topAlarmLeaders.isEmpty()) {
                    item {
                        Text("No wakeup alarms recorded.", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    items(analytics.topAlarmLeaders) { leader ->
                        AlarmLeaderRow(leader = leader)
                    }
                }
            }
        }

        // Battery Cell & Voltage Environment
        item {
            Text(
                text = "Battery Health & Cell Limits",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Nominal Design Capacity", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Text("${analytics.estimatedCapacityMah.toInt()} mAh", style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Voltage Operating Range", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Text("${analytics.minVoltageMv} mV - ${analytics.maxVoltageMv} mV", style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Temperature Spectrum", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Text("${analytics.minTempC}°C - ${analytics.maxTempC}°C (avg ${analytics.avgTempC}°C)", style = MaterialTheme.typography.bodyMedium, color = GreenPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showExportSheet) {
        ExportBottomSheet(
            analytics = analytics,
            onDismiss = { showExportSheet = false }
        )
    }
}
}

@Composable
private fun InsightCard(insight: ActionableInsight) {
    val (bgColor, iconColor, icon) = when (insight.severity) {
        InsightSeverity.SUCCESS -> Triple(GreenPrimary.copy(alpha = 0.12f), GreenPrimary, Icons.Default.CheckCircle)
        InsightSeverity.INFO -> Triple(AccentBlue.copy(alpha = 0.12f), AccentBlue, Icons.Default.Info)
        InsightSeverity.WARNING -> Triple(CulpritAmber.copy(alpha = 0.12f), CulpritAmber, Icons.Default.Warning)
        InsightSeverity.ALERT -> Triple(CulpritRed.copy(alpha = 0.12f), CulpritRed, Icons.Default.Warning)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = insight.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = iconColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = insight.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary
                )
            }
        }
    }
}

@Composable
private fun DailySummaryRow(day: DailyAnalyticsSummary) {
    val effColor = if (day.deepSleepEfficiency >= 75) GreenPrimary else if (day.deepSleepEfficiency >= 60) CulpritAmber else CulpritRed

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = day.dateString,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(effColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${day.deepSleepEfficiency.toInt()}% Doze",
                        style = MaterialTheme.typography.labelSmall,
                        color = effColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Battery: ${day.startLevel}% → ${day.endLevel}% (-${day.dropPercent}%)",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = "${String.format(Locale.US, "%.2f", day.drainRatePerHour)}%/hr",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (day.drainRatePerHour > 3.0) CulpritAmber else GreenPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Deep Sleep: ${day.deepSleepHours}h • Awake: ${day.awakeScreenOffHours}h",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = "${day.avgTempC}°C avg",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun HardwareAttributionCard(breakdown: HardwareEnergyBreakdown) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val total = breakdown.totalMah.coerceAtLeast(1.0)
            val cpuPct = (breakdown.cpuMah / total * 100.0).roundToInt()
            val wlPct = (breakdown.wakelockMah / total * 100.0).roundToInt()
            val radioPct = (breakdown.radioMah / total * 100.0).roundToInt()
            val wifiSensorsMah = breakdown.wifiMah + breakdown.gpsMah
            val wifiSensorsPct = (wifiSensorsMah / total * 100.0).roundToInt()

            // Proportional Multi-color Progress Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
            ) {
                if (breakdown.cpuMah > 0) {
                    Box(modifier = Modifier.weight(breakdown.cpuMah.toFloat()).fillMaxSize().background(GreenPrimary))
                }
                if (breakdown.wakelockMah > 0) {
                    Box(modifier = Modifier.weight(breakdown.wakelockMah.toFloat()).fillMaxSize().background(CulpritAmber))
                }
                if (breakdown.radioMah > 0) {
                    Box(modifier = Modifier.weight(breakdown.radioMah.toFloat()).fillMaxSize().background(AccentBlue))
                }
                if (wifiSensorsMah > 0) {
                    Box(modifier = Modifier.weight(wifiSensorsMah.toFloat()).fillMaxSize().background(TextSecondary))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            HardwareRow(label = "CPU Core Computation", mah = breakdown.cpuMah, pct = cpuPct, color = GreenPrimary)
            HardwareRow(label = "Screen-Off Partial Wakelocks", mah = breakdown.wakelockMah, pct = wlPct, color = CulpritAmber)
            HardwareRow(label = "Cellular Radio Modem", mah = breakdown.radioMah, pct = radioPct, color = AccentBlue)
            HardwareRow(label = "Wi-Fi & Sensors", mah = wifiSensorsMah, pct = wifiSensorsPct, color = TextSecondary)
        }
    }
}

@Composable
private fun HardwareRow(label: String, mah: Double, pct: Int, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
        }
        Text(
            text = "${mah.toInt()} mAh ($pct%)",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun WakelockLeaderRow(leader: WakelockLeader) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
    val mins = leader.durationMs / 60_000L

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(CulpritAmber.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LockClock,
                        contentDescription = null,
                        tint = CulpritAmber,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = leader.appLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        if (leader.potentialSavingsMah > 5.0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(GreenPrimary.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Save ~${leader.potentialSavingsMah.toInt()} mAh",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GreenPrimary
                                )
                            }
                        }
                    }
                    Text(
                        text = if (leader.topTag.isNotEmpty()) leader.topTag.take(45) else leader.packageName,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (mins >= 60) "${mins / 60}h ${mins % 60}m" else "${mins}m",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = CulpritAmber
                    )
                    Text(
                        text = "${leader.count} wakes",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkBackground.copy(alpha = 0.6f))
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Package: ${leader.packageName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        if (leader.topTag.isNotEmpty()) {
                            Text(
                                text = "Wakelock Tag: ${leader.topTag}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextPrimary
                            )
                        }
                        Text(
                            text = "Held CPU awake for $mins minutes during standby. Restricting background battery usage prevents this app from interrupting deep sleep.",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = { openAppBatterySettings(context, leader.packageName) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Manage Background Battery", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlarmLeaderRow(leader: AlarmLeader) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(AccentBlue.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = null,
                        tint = AccentBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = leader.appLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        if (leader.potentialSavingsMah > 5.0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(GreenPrimary.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Save ~${leader.potentialSavingsMah.toInt()} mAh",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GreenPrimary
                                )
                            }
                        }
                    }
                    Text(
                        text = if (leader.topAction.isNotEmpty()) leader.topAction.substringAfterLast('.').take(35) else leader.packageName,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }

                Text(
                    text = "${leader.wakeupCount} alarms",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = AccentBlue
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkBackground.copy(alpha = 0.6f))
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Package: ${leader.packageName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        if (leader.topAction.isNotEmpty()) {
                            Text(
                                text = "Action Intent: ${leader.topAction}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextPrimary
                            )
                        }
                        Text(
                            text = "Fired ${leader.wakeupCount} hardware wakeups, forcing CPU resume from suspend. Restricting background sync will batch or suppress these alarms.",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = { openAppBatterySettings(context, leader.packageName) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentBlue),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Manage App Settings", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NetworkDrainComparisonCard(comparison: NetworkDrainComparison) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Modem vs Wi-Fi Standby",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                if (comparison.cellularPenaltyMultiplier > 1.05) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CulpritAmber.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${comparison.cellularPenaltyMultiplier}x Cell Penalty",
                            style = MaterialTheme.typography.labelSmall,
                            color = CulpritAmber,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Wi-Fi column
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkBackground.copy(alpha = 0.6f))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(GreenPrimary))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Wi-Fi", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = GreenPrimary)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("${comparison.wifiDrainRatePerHour}%/hr", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Standby Drain Rate", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("${comparison.wifiHours}h active • ${comparison.wifiDataMb.toInt()} MB", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text("${comparison.wifiEnergyMah.toInt()} mAh total", style = MaterialTheme.typography.bodySmall, color = GreenPrimary, fontWeight = FontWeight.SemiBold)
                }

                // Cellular column
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkBackground.copy(alpha = 0.6f))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(AccentBlue))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cellular 5G/LTE", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = AccentBlue)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "${comparison.cellularDrainRatePerHour}%/hr",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (comparison.cellularDrainRatePerHour > comparison.wifiDrainRatePerHour) CulpritAmber else TextPrimary
                    )
                    Text("Standby Drain Rate", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("${comparison.cellularHours}h active • ${comparison.cellularDataMb.toInt()} MB", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text("${comparison.cellularEnergyMah.toInt()} mAh modem", style = MaterialTheme.typography.bodySmall, color = AccentBlue, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Cellular baseband modems enter high-current RRC states during data exchange, consuming significantly more energy per megabyte than Wi-Fi.",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportBottomSheet(
    analytics: OverallAnalytics,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = TextSecondary) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Export Battery Diagnostics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Share an offline interactive report or raw telemetry data.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(6.dp))

            ExportActionRow(
                icon = Icons.Default.Description,
                title = "Share Interactive HTML Report",
                subtitle = "Self-contained dark report with charts, stats, and culprit tables",
                tint = GreenPrimary,
                onClick = {
                    onDismiss()
                    val html = DiagnosticExporter.exportToHtml(analytics)
                    DiagnosticExporter.shareReport(
                        context = context,
                        content = html,
                        fileName = "PowerTrace_Battery_Report.html",
                        mimeType = "text/html",
                        chooserTitle = "Share Interactive HTML Report"
                    )
                }
            )

            ExportActionRow(
                icon = Icons.Default.Tune,
                title = "Export CSV Spreadsheet",
                subtitle = "Daily metrics, hardware energy, and culprits in CSV format",
                tint = AccentBlue,
                onClick = {
                    onDismiss()
                    val csv = DiagnosticExporter.exportToCsv(analytics)
                    DiagnosticExporter.shareReport(
                        context = context,
                        content = csv,
                        fileName = "PowerTrace_Battery_Data.csv",
                        mimeType = "text/csv",
                        chooserTitle = "Export Battery CSV Data"
                    )
                }
            )

            ExportActionRow(
                icon = Icons.Default.ContentCopy,
                title = "Copy Summary to Clipboard",
                subtitle = "Quick formatted text summary for messages or forums",
                tint = CulpritAmber,
                onClick = {
                    onDismiss()
                    val text = DiagnosticExporter.exportSummaryText(analytics)
                    DiagnosticExporter.copyToClipboard(context, text)
                }
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ExportActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = DarkBackground),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
        }
    }
}

private fun openAppBatterySettings(context: Context, packageName: String) {
    if (packageName.isEmpty() || packageName.startsWith("android.kernel")) return
    try {
        val intent = Intent("android.settings.APP_BATTERY_USAGE").apply {
            data = Uri.parse("package:$packageName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open settings: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
