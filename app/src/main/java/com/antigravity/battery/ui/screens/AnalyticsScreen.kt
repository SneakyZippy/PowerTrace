package com.antigravity.battery.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.battery.core.model.OverallAnalytics
import com.antigravity.battery.ui.components.MetricStatTile
import com.antigravity.battery.ui.screens.analytics.AlarmLeaderRow
import com.antigravity.battery.ui.screens.analytics.DailySummaryRow
import com.antigravity.battery.ui.screens.analytics.ExportBottomSheet
import com.antigravity.battery.ui.screens.analytics.HardwareAttributionCard
import com.antigravity.battery.ui.screens.analytics.InsightCard
import com.antigravity.battery.ui.screens.analytics.NetworkDrainComparisonCard
import com.antigravity.battery.ui.screens.analytics.WakelockLeaderRow
import com.antigravity.battery.ui.theme.AccentBlue
import com.antigravity.battery.ui.theme.CulpritAmber
import com.antigravity.battery.ui.theme.CulpritRed
import com.antigravity.battery.ui.theme.DarkBackground
import com.antigravity.battery.ui.theme.DarkSurface
import com.antigravity.battery.ui.theme.GreenPrimary
import com.antigravity.battery.ui.theme.TextPrimary
import com.antigravity.battery.ui.theme.TextSecondary
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
                Spacer(modifier = Modifier.height(20.dp))
                OutlinedButton(
                    onClick = onRefresh,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenPrimary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GreenPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Refresh Analytics")
                }
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
                        val hasDischarge = analytics.totalDischargeHours > 0.05
                        MetricStatTile(
                            label = "DOZE EFFICIENCY",
                            value = if (hasDischarge) "${analytics.avgDeepSleepEfficiency.toInt()}" else "--",
                            unit = if (hasDischarge) "%" else "",
                            valueColor = if (!hasDischarge) TextSecondary else if (analytics.avgDeepSleepEfficiency >= 80) GreenPrimary else if (analytics.avgDeepSleepEfficiency >= 65) CulpritAmber else CulpritRed,
                            modifier = Modifier.weight(1f)
                        )
                        MetricStatTile(
                            label = "AVG CELL TEMP",
                            value = if (analytics.avgTempC > 0.0) String.format(Locale.US, "%.1f", analytics.avgTempC) else "--",
                            unit = if (analytics.avgTempC > 0.0) "°C" else "",
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
