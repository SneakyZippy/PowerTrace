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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.battery.core.engine.TimeWindowFilter
import com.antigravity.battery.core.engine.TimelineViewData
import com.antigravity.battery.core.model.CulpritApp
import com.antigravity.battery.ui.components.BatteryTimelineChart
import com.antigravity.battery.ui.components.DozeSleepEfficiencyBar
import com.antigravity.battery.ui.components.MetricStatTile
import com.antigravity.battery.ui.components.WarningBadgeChip
import com.antigravity.battery.ui.theme.AccentBlue
import com.antigravity.battery.ui.theme.CulpritAmber
import com.antigravity.battery.ui.theme.CulpritRed
import com.antigravity.battery.ui.theme.DarkBackground
import com.antigravity.battery.ui.theme.DarkSurface
import com.antigravity.battery.ui.theme.DarkSurfaceVariant
import com.antigravity.battery.ui.theme.GreenPrimary
import com.antigravity.battery.ui.theme.TextPrimary
import com.antigravity.battery.ui.theme.TextSecondary
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.antigravity.battery.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import com.antigravity.battery.ui.components.MetricExplanation
import com.antigravity.battery.ui.components.MetricExplanationDialog
import com.antigravity.battery.ui.components.TelemetryGlossary
import com.antigravity.battery.ui.components.TelemetryGuideBottomSheet
import java.util.Locale

enum class CulpritFilterTab(val label: String) {
    ALL("All"),
    USER("User Apps"),
    SYSTEM("System & Kernel")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    timelineData: TimelineViewData?,
    selectedFilter: TimeWindowFilter,
    isContinuousEnabled: Boolean,
    onToggleContinuous: (Boolean) -> Unit,
    onSelectFilter: (TimeWindowFilter) -> Unit,
    onSelectCulprit: (CulpritApp) -> Unit
) {
    var activeExplanation by remember { mutableStateOf<MetricExplanation?>(null) }
    var isGuideOpen by remember { mutableStateOf(false) }
    val guideSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var culpritFilter by remember { mutableStateOf(CulpritFilterTab.ALL) }
    var searchQuery by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.powertrace_app_icon),
                            contentDescription = "PowerTrace Logo",
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "PowerTrace",
                                style = MaterialTheme.typography.headlineMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "Continuous 24/7 battery telemetry",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Guide / Help Button
                        IconButton(
                            onClick = { isGuideOpen = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = "Battery Telemetry Guide",
                                tint = AccentBlue
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Interactive Daemon Toggle Chip
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkSurface)
                                .clickable { onToggleContinuous(!isContinuousEnabled) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isContinuousEnabled) GreenPrimary else TextSecondary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isContinuousEnabled) "24/7 ON" else "24/7 OFF",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isContinuousEnabled) GreenPrimary else TextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

        // Time Range Filter Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TimeWindowFilter.values().forEach { filter ->
                    val isSelected = filter == selectedFilter
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectFilter(filter) },
                        label = { Text(filter.label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
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

        // Interactive Battery Level Curve Chart
        item {
            BatteryTimelineChart(
                points = timelineData?.points ?: emptyList()
            )
        }

        // Overnight Sleep Diagnostics Report Card
        if (selectedFilter == TimeWindowFilter.OVERNIGHT_SLEEP && timelineData != null) {
            item {
                OvernightSleepReportCard(timelineData = timelineData)
            }
        }

        // Metric Statistics Grid
        if (timelineData != null) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricStatTile(
                        label = "DISCHARGE DRAIN",
                        value = "-${timelineData.totalDischargePercent}",
                        unit = "%",
                        valueColor = if (timelineData.totalDischargePercent > 15) CulpritRed else GreenPrimary,
                        modifier = Modifier.weight(1f),
                        onClick = { activeExplanation = TelemetryGlossary.DISCHARGE_DRAIN }
                    )
                    MetricStatTile(
                        label = "AVG DRAIN RATE",
                        value = String.format(Locale.US, "%.1f", timelineData.averageDrainRatePerHour),
                        unit = "%/hr",
                        valueColor = if (timelineData.averageDrainRatePerHour > 1.5) CulpritAmber else GreenPrimary,
                        modifier = Modifier.weight(1f),
                        onClick = { activeExplanation = TelemetryGlossary.AVG_DRAIN_RATE }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val sotMins = (timelineData.screenOnDurationMs / 60000)
                    val sotHours = sotMins / 60
                    val sotRemMins = sotMins % 60
                    MetricStatTile(
                        label = "SCREEN ON TIME",
                        value = "${sotHours}h ${sotRemMins}m",
                        valueColor = AccentBlue,
                        modifier = Modifier.weight(1f),
                        onClick = { activeExplanation = TelemetryGlossary.SCREEN_ON_TIME }
                    )
                    MetricStatTile(
                        label = "DOZE EFFICIENCY",
                        value = "${timelineData.deepSleepEfficiencyPercent.toInt()}",
                        unit = "%",
                        valueColor = if (timelineData.deepSleepEfficiencyPercent >= 80) GreenPrimary else CulpritRed,
                        modifier = Modifier.weight(1f),
                        onClick = { activeExplanation = TelemetryGlossary.DOZE_EFFICIENCY }
                    )
                }
            }

            // Sleep Distribution Bar
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        DozeSleepEfficiencyBar(
                            deepSleepMs = timelineData.deepSleepDurationMs,
                            awakeScreenOffMs = timelineData.awakeScreenOffMs,
                            screenOnMs = timelineData.screenOnDurationMs,
                            onClick = { activeExplanation = TelemetryGlossary.DEEP_SLEEP }
                        )
                    }
                }
            }

            // Culprits Header & Controls
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Top Culprits (${selectedFilter.label})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${timelineData.topCulprits.size} apps active",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name or package...", color = TextSecondary, fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenPrimary,
                        unfocusedBorderColor = DarkSurfaceVariant,
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Category Tabs: All | User Apps | System
                val allList = timelineData.topCulprits
                val userCount = allList.count { !it.isSystemApp }
                val systemCount = allList.count { it.isSystemApp }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CulpritFilterTab.values().forEach { tab ->
                        val count = when (tab) {
                            CulpritFilterTab.ALL -> allList.size
                            CulpritFilterTab.USER -> userCount
                            CulpritFilterTab.SYSTEM -> systemCount
                        }
                        val isSelected = tab == culpritFilter
                        FilterChip(
                            selected = isSelected,
                            onClick = { culpritFilter = tab },
                            label = { Text("${tab.label} ($count)", fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentBlue.copy(alpha = 0.2f),
                                selectedLabelColor = AccentBlue,
                                containerColor = DarkSurface,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }
            }

            // Culprits Items
            val allList = timelineData.topCulprits
            val filteredCulprits = allList.filter { culprit ->
                val matchesCategory = when (culpritFilter) {
                    CulpritFilterTab.ALL -> true
                    CulpritFilterTab.USER -> !culprit.isSystemApp
                    CulpritFilterTab.SYSTEM -> culprit.isSystemApp
                }
                val matchesSearch = if (searchQuery.isBlank()) true else {
                    culprit.appLabel.contains(searchQuery, ignoreCase = true) ||
                    culprit.primaryPackageName.contains(searchQuery, ignoreCase = true)
                }
                matchesCategory && matchesSearch
            }

            if (filteredCulprits.isEmpty()) {
                item {
                    val emptyMsg = if (searchQuery.isNotBlank()) {
                        "No apps matching \"$searchQuery\" in this window."
                    } else if (culpritFilter == CulpritFilterTab.USER) {
                        "No user apps active in this period."
                    } else {
                        "No app wake lock or drain activity recorded for this period yet."
                    }
                    Text(
                        text = emptyMsg,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            } else {
                items(filteredCulprits) { culprit ->
                    CulpritItemRow(
                        culprit = culprit,
                        onClick = { onSelectCulprit(culprit) }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Interactive Metric Explanation Dialog
    MetricExplanationDialog(
        explanation = activeExplanation,
        onDismiss = { activeExplanation = null }
    )

    // Complete Telemetry Guide Bottom Sheet
    if (isGuideOpen) {
        TelemetryGuideBottomSheet(
            sheetState = guideSheetState,
            onDismiss = { isGuideOpen = false }
        )
    }
}
}

@Composable
private fun OvernightSleepReportCard(
    timelineData: TimelineViewData
) {
    val drainRate = timelineData.averageDrainRatePerHour
    val efficiency = timelineData.deepSleepEfficiencyPercent
    val drop = timelineData.totalDischargePercent

    val (statusLabel, statusColor, statusDesc) = when {
        drainRate <= 0.8 -> Triple("EXCELLENT SLEEP 🌙", GreenPrimary, "Deep CPU suspend maintained. Minimal background wakefulness.")
        drainRate <= 1.5 -> Triple("HEALTHY STANDBY", AccentBlue, "Normal overnight idle drain with standard periodic syncs.")
        drainRate <= 2.5 -> Triple("ELEVATED SLEEP DRAIN ⚠️", CulpritAmber, "Background apps or wakelocks interrupted sleep more often than optimal.")
        else -> Triple("SEVERE DRAIN LEAK 🚨", CulpritRed, "Persistent alarms or held wakelocks prevented deep suspend.")
    }

    val topAwakeCulprit = timelineData.topCulprits
        .filter { it.wakeupAlarmCount > 0 || it.partialWakelockMs > 15000L }
        .maxByOrNull { it.wakeupAlarmCount * 10000L + it.partialWakelockMs }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bedtime,
                        contentDescription = null,
                        tint = AccentBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Overnight Sleep Verdict",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusLabel,
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Hourly Pace", fontSize = 11.sp, color = TextSecondary)
                    Text("${String.format(Locale.US, "%.1f", drainRate)}%/hr", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = statusColor)
                }
                Column {
                    Text("Deep Sleep", fontSize = 11.sp, color = TextSecondary)
                    Text("${efficiency.toInt()}%", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                }
                Column {
                    Text("Overnight Drop", fontSize = 11.sp, color = TextSecondary)
                    Text("-$drop%", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (drop > 5) CulpritRed else TextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = statusDesc,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            if (topAwakeCulprit != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkBackground)
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = CulpritAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Top Wake Source: ${topAwakeCulprit.appLabel}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            val wlSec = topAwakeCulprit.partialWakelockMs / 1000
                            Text(
                                text = "${topAwakeCulprit.wakeupAlarmCount} alarm wakeups • ${wlSec}s wakelock hold",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CulpritItemRow(
    culprit: CulpritApp,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val pm = context.packageManager
    val appIcon = remember(culprit.primaryPackageName) {
        try {
            val drawable = pm.getApplicationIcon(culprit.primaryPackageName)
            drawable.toBitmap(96, 96).asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (appIcon != null) {
                    Image(
                        bitmap = appIcon,
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = culprit.appLabel.take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = GreenPrimary,
                            fontSize = 17.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = culprit.appLabel,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                        if (culprit.isSystemApp) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(DarkSurfaceVariant)
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text("SYS", fontSize = 9.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Text(
                        text = culprit.primaryPackageName,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${String.format(Locale.US, "%.1f", culprit.totalEstimatedMah)} mAh",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (culprit.totalEstimatedMah > 20.0) CulpritRed else TextPrimary
                    )
                    val wlMins = culprit.partialWakelockMs / 60000
                    Text(
                        text = "${wlMins}m wl • ${culprit.wakeupAlarmCount} wakeups",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            if (culprit.warningBadges.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    culprit.warningBadges.forEach { WarningBadgeChip(it) }
                }
            }
        }
    }
}
