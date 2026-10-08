package com.antigravity.battery.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.antigravity.battery.ui.theme.AccentBlue
import com.antigravity.battery.ui.theme.CulpritAmber
import com.antigravity.battery.ui.theme.DarkBackground
import com.antigravity.battery.ui.theme.DarkSurface
import com.antigravity.battery.ui.theme.DarkSurfaceVariant
import com.antigravity.battery.ui.theme.GreenPrimary
import com.antigravity.battery.ui.theme.PurpleAccent
import com.antigravity.battery.ui.theme.TextPrimary
import com.antigravity.battery.ui.theme.TextSecondary

data class MetricExplanation(
    val id: String,
    val title: String,
    val category: String,
    val summary: String,
    val healthyTarget: String,
    val recommendation: String
)

object TelemetryGlossary {
    val DISCHARGE_DRAIN = MetricExplanation(
        id = "discharge_drain",
        title = "Discharge Drain (%)",
        category = "Device Summary",
        summary = "The total percentage drop in battery charge during the selected time period while disconnected from power.",
        healthyTarget = "Depends on duration & usage. When idle overnight (8 hrs), total drain should ideally remain under 2% to 4%.",
        recommendation = "If the drop is unexpectedly steep, inspect the Top Culprits list below to see which background apps consumed battery."
    )

    val AVG_DRAIN_RATE = MetricExplanation(
        id = "avg_drain_rate",
        title = "Average Drain Rate (%/hr)",
        category = "Device Summary",
        summary = "The hourly pace of battery consumption. Measures how quickly battery capacity depleted per hour of active or idle time.",
        healthyTarget = "Idle (Screen-Off): 0.5% – 1.2%/hr is excellent. Active (Screen-On): 8% – 16%/hr is normal.",
        recommendation = "Screen-off rates above 2.5%/hr indicate apps holding wakelocks, frequent network polling, or weak cellular signals."
    )

    val SCREEN_ON_TIME = MetricExplanation(
        id = "screen_on_time",
        title = "Screen On Time (SOT)",
        category = "Device Summary",
        summary = "Accumulated time that the display panel was turned on and illuminated during the observation interval.",
        healthyTarget = "Displays and GPU rendering draw the most power on modern smartphones (typically 200–600 mA).",
        recommendation = "To extend battery runtime, lower display brightness, reduce refresh rate, or shorten screen timeout in Android Settings."
    )

    val DOZE_EFFICIENCY = MetricExplanation(
        id = "doze_efficiency",
        title = "Doze Efficiency (%)",
        category = "Power & Sleep",
        summary = "The percentage of screen-off time that the Android OS successfully spent in deep CPU suspend (Doze) mode instead of awake.",
        healthyTarget = ">85% is ideal. 70%–85% is acceptable. Below 60% indicates severe background wakefulness.",
        recommendation = "Review apps with 'WAKELOCK > 10M' or 'FREQUENT ALARMS' badges and toggle them to 'Restricted' battery in Android Settings."
    )

    val DEEP_SLEEP = MetricExplanation(
        id = "deep_sleep",
        title = "Deep Sleep (CPU Suspend)",
        category = "Power & Sleep",
        summary = "The lowest power state of the system where CPU cores are suspended and current draw drops to minimal standby milliamps.",
        healthyTarget = "Should account for 85%+ of idle screen-off time when the device is not being used.",
        recommendation = "Only hardware timer interrupts and high-priority push notifications wake the device from this state."
    )

    val AWAKE_SCREEN_OFF = MetricExplanation(
        id = "awake_screen_off",
        title = "Awake Screen Off",
        category = "Power & Sleep",
        summary = "Time when the display was dark, but the CPU was prevented from entering low-power sleep by background tasks or wakelocks.",
        healthyTarget = "<15% of screen-off duration.",
        recommendation = "Look for apps in Top Culprits with high CPU or Wakelock duration that executed tasks while you were not using the phone."
    )

    val MONITORING_24_7 = MetricExplanation(
        id = "monitoring_24_7",
        title = "Continuous 24/7 Tracking",
        category = "PowerTrace Daemon",
        summary = "A lightweight background service that records periodic 15-minute battery snapshots and triggers on screen/power transitions.",
        healthyTarget = "Uses Android's native IPC binder interface directly without waking up CPU cores (<0.1% daily overhead).",
        recommendation = "Keep this enabled to maintain continuous day, week, and overnight telemetry history without manual start/stops."
    )

    val CPU_PROCESSING = MetricExplanation(
        id = "cpu_processing",
        title = "CPU Processing Energy",
        category = "Hardware Attribution",
        summary = "Calculated energy (mAh) drawn by CPU processor clusters executing application background or foreground threads.",
        healthyTarget = "Background processing for messaging or sync apps should be minimal (<15 mAh per day).",
        recommendation = "Apps flagged with 'HIGH BG CPU' ran intensive computing threads while the screen was turned off."
    )

    val WAKELOCK_HOLD = MetricExplanation(
        id = "wakelock_hold",
        title = "Partial Wakelock Hold",
        category = "Hardware Attribution",
        summary = "Energy drawn while an application held an Android PARTIAL_WAKE_LOCK, preventing CPU sleep.",
        healthyTarget = "Well-behaved apps only hold wakelocks for a few seconds to finish quick operations.",
        recommendation = "Apps holding wakelocks for over 10 minutes are flagged with [WAKELOCK > 10M]. Restrict their background activity."
    )

    val MOBILE_RADIO = MetricExplanation(
        id = "mobile_radio",
        title = "Mobile Radio Active Energy",
        category = "Hardware Attribution",
        summary = "Energy consumed by the 4G/5G cellular modem during active data transmission and power-tail states.",
        healthyTarget = "Cellular modems draw significant power (~120–250 mA) when active.",
        recommendation = "Apps sending frequent small background sync packets force the radio to remain active. Use Wi-Fi when available."
    )

    val WIFI_ACTIVITY = MetricExplanation(
        id = "wifi_activity",
        title = "Wi-Fi Scans & Network",
        category = "Hardware Attribution",
        summary = "Energy consumed performing Wi-Fi network beacon scans and transferring data over local Wi-Fi.",
        healthyTarget = "Wi-Fi is 2x–3x more power efficient than cellular data for heavy file and media transfers.",
        recommendation = "If an app has high scan counts, disable 'Wi-Fi Scanning for Location' in Android System Location settings."
    )

    val GPS_LOCATION = MetricExplanation(
        id = "gps_location",
        title = "GPS / GNSS Location",
        category = "Hardware Attribution",
        summary = "Energy consumed by the satellite positioning receiver when an app requests accurate coordinates.",
        healthyTarget = "Background apps should use coarse cell-tower or fused location rather than persistent GPS locks.",
        recommendation = "Change the app's location permission from 'Allow all the time' to 'Only while using the app'."
    )

    val WAKEUP_ALARMS = MetricExplanation(
        id = "wakeup_alarms",
        title = "Wakeup Alarms (RTC)",
        category = "Alarms & Triggers",
        summary = "Hardware Real-Time Clock (RTC) timer interrupts scheduled by apps that jolt the CPU out of deep sleep.",
        healthyTarget = "<12 wakeups per hour across all user applications combined.",
        recommendation = "Frequent wakeup alarms interrupt battery saving maintenance windows. Restrict app background execution."
    )

    val ALL_EXPLANATIONS = listOf(
        DISCHARGE_DRAIN,
        AVG_DRAIN_RATE,
        SCREEN_ON_TIME,
        DOZE_EFFICIENCY,
        DEEP_SLEEP,
        AWAKE_SCREEN_OFF,
        MONITORING_24_7,
        CPU_PROCESSING,
        WAKELOCK_HOLD,
        MOBILE_RADIO,
        WIFI_ACTIVITY,
        GPS_LOCATION,
        WAKEUP_ALARMS
    )
}

@Composable
fun MetricExplanationDialog(
    explanation: MetricExplanation?,
    onDismiss: () -> Unit
) {
    if (explanation == null) return

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = GreenPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = explanation.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = explanation.category.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AccentBlue
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // What it is
                Column {
                    Text(
                        text = "WHAT IT MEANS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = explanation.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                // Target
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = CulpritAmber,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "HEALTHY TARGET / BASELINE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = explanation.healthyTarget,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                // Recommendation
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = GreenPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "RECOMMENDATION",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = explanation.recommendation,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Got it", color = DarkBackground, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelemetryGuideBottomSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Device Summary", "Power & Sleep", "Hardware Attribution", "Alarms & Triggers")
    val listState = rememberLazyListState()

    LaunchedEffect(selectedCategory) {
        listState.scrollToItem(0)
    }

    val filtered = if (selectedCategory == "All") {
        TelemetryGlossary.ALL_EXPLANATIONS
    } else {
        TelemetryGlossary.ALL_EXPLANATIONS.filter { it.category == selectedCategory }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Battery Telemetry Guide",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Understand what each metric, formula, and state means",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Category Chips with smooth horizontal scroll and no vertical letter wrapping
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = cat == selectedCategory
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) GreenPrimary else DarkSurface)
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = cat,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) DarkBackground else TextSecondary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Explanation Items
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = item.category.uppercase(),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentBlue
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.summary,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = CulpritAmber,
                                    modifier = Modifier.size(13.dp).padding(top = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = item.healthyTarget,
                                    fontSize = 11.sp,
                                    color = CulpritAmber
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(28.dp))
                }
            }
        }
    }
}
