package com.antigravity.battery.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.antigravity.battery.core.model.CulpritApp
import com.antigravity.battery.ui.components.WarningBadgeChip
import com.antigravity.battery.ui.theme.AccentBlue
import com.antigravity.battery.ui.theme.DarkBackground
import com.antigravity.battery.ui.theme.DarkSurface
import com.antigravity.battery.ui.theme.DarkSurfaceVariant
import com.antigravity.battery.ui.theme.GreenPrimary
import com.antigravity.battery.ui.theme.TextPrimary
import com.antigravity.battery.ui.theme.TextSecondary
import java.util.Locale

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.antigravity.battery.core.model.CulpritWarningBadge
import com.antigravity.battery.ui.components.MetricExplanation
import com.antigravity.battery.ui.components.MetricExplanationDialog
import com.antigravity.battery.ui.components.TelemetryGlossary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CulpritDetailBottomSheet(
    culprit: CulpritApp?,
    sheetState: SheetState,
    totalDrainMah: Double = 0.0,
    rank: Int? = null,
    totalCulpritsCount: Int? = null,
    onDismiss: () -> Unit
) {
    if (culprit == null) return

    var activeExplanation by remember { mutableStateOf<MetricExplanation?>(null) }
    val context = LocalContext.current
    val pm = context.packageManager

    // Resolve app icon
    val appIcon = remember(culprit.primaryPackageName) {
        try {
            val drawable = pm.getApplicationIcon(culprit.primaryPackageName)
            drawable.toBitmap(96, 96).asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkBackground
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // App Icon or stylized initial
                        if (appIcon != null) {
                            Image(
                                bitmap = appIcon,
                                contentDescription = culprit.appLabel,
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(DarkSurfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = culprit.appLabel.take(1).uppercase(),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = GreenPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = culprit.appLabel,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "${culprit.primaryPackageName} (UID ${culprit.uid})",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    // Total mAh + proportion sub-label
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${String.format(Locale.US, "%.1f", culprit.totalEstimatedMah)} mAh",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = GreenPrimary
                        )
                        if (totalDrainMah > 0.0) {
                            val proportion = ((culprit.totalEstimatedMah / totalDrainMah) * 100.0).coerceIn(0.0, 100.0)
                            val rankPrefix = if (rank != null) {
                                if (totalCulpritsCount != null) "#$rank of $totalCulpritsCount • " else "#$rank • "
                            } else ""
                            Text(
                                text = "$rankPrefix${String.format(Locale.US, "%.1f", proportion)}% of drain",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )
                        }
                    }
                }

                // Action Bar (Settings shortcut & Badges)
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Warning badges
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        culprit.warningBadges.forEach { badge ->
                            val exp = when (badge) {
                                CulpritWarningBadge.RUNAWAY_WAKELOCK -> TelemetryGlossary.WAKELOCK_HOLD
                                CulpritWarningBadge.AGGRESSIVE_WAKEUPS -> TelemetryGlossary.WAKEUP_ALARMS
                                CulpritWarningBadge.RADIO_KEEP_ALIVE -> TelemetryGlossary.MOBILE_RADIO
                                CulpritWarningBadge.HIGH_BACKGROUND_CPU -> TelemetryGlossary.CPU_PROCESSING
                            }
                            WarningBadgeChip(badge = badge, onClick = { activeExplanation = exp })
                        }
                    }

                    // System App Info 1-tap shortcut (for non-kernel packages)
                    if (culprit.primaryPackageName.isNotEmpty() && !culprit.primaryPackageName.startsWith("android.kernel")) {
                        OutlinedButton(
                            onClick = {
                                try {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", culprit.primaryPackageName, null)
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = AccentBlue
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "App Info",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AccentBlue
                            )
                        }
                    }
                }
            }

            // Hardware & Network Energy Attribution Breakdown
            item {
                Text(
                    text = "Hardware Energy Attribution Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        EnergyRowItem(
                            label = "CPU Processing",
                            mahValue = "${String.format(Locale.US, "%.2f", culprit.energyBreakdown.cpuMah)} mAh",
                            detail = "${(culprit.cpuUserTimeMs + culprit.cpuSystemTimeMs) / 1000}s active",
                            onClick = { activeExplanation = TelemetryGlossary.CPU_PROCESSING }
                        )
                        EnergyRowItem(
                            label = "Wakelock Hold",
                            mahValue = "${String.format(Locale.US, "%.2f", culprit.energyBreakdown.wakelockMah)} mAh",
                            detail = "${culprit.partialWakelockMs / 1000}s hold",
                            onClick = { activeExplanation = TelemetryGlossary.WAKELOCK_HOLD }
                        )

                        // Mobile Radio with network transfer volume
                        val mobileDataText = if (culprit.mobileBytesRx > 0 || culprit.mobileBytesTx > 0) {
                            " • ${formatDataVolume(culprit.mobileBytesRx)} ↓  ${formatDataVolume(culprit.mobileBytesTx)} ↑"
                        } else ""
                        EnergyRowItem(
                            label = "Mobile Radio",
                            mahValue = "${String.format(Locale.US, "%.2f", culprit.energyBreakdown.mobileRadioMah)} mAh",
                            detail = "${culprit.mobileRadioActiveMs / 1000}s active$mobileDataText",
                            onClick = { activeExplanation = TelemetryGlossary.MOBILE_RADIO }
                        )

                        // Wi-Fi with network transfer volume
                        val wifiDataText = if (culprit.wifiBytesRx > 0 || culprit.wifiBytesTx > 0) {
                            " • ${formatDataVolume(culprit.wifiBytesRx)} ↓  ${formatDataVolume(culprit.wifiBytesTx)} ↑"
                        } else ""
                        EnergyRowItem(
                            label = "Wi-Fi Activity",
                            mahValue = "${String.format(Locale.US, "%.2f", culprit.energyBreakdown.wifiMah)} mAh",
                            detail = "${culprit.wifiScanCount} scans$wifiDataText",
                            onClick = { activeExplanation = TelemetryGlossary.WIFI_ACTIVITY }
                        )

                        if (culprit.gpsDurationMs > 0) {
                            EnergyRowItem(
                                label = "GPS Location",
                                mahValue = "${String.format(Locale.US, "%.2f", culprit.energyBreakdown.gpsMah)} mAh",
                                detail = "${culprit.gpsDurationMs / 1000}s active",
                                onClick = { activeExplanation = TelemetryGlossary.GPS_LOCATION }
                            )
                        }
                    }
                }
            }

            // Wakelock Tags Breakdown
            item {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { activeExplanation = TelemetryGlossary.WAKELOCK_HOLD }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Held Wakelock Tags (${culprit.topWakelockTags.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = TextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            if (culprit.topWakelockTags.isEmpty()) {
                item {
                    Text("No partial wake lock tags recorded.", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                items(culprit.topWakelockTags) { tag ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tag.tag,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${tag.durationMs / 1000}s",
                                    fontWeight = FontWeight.Bold,
                                    color = GreenPrimary,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "${tag.count} acquisitions",
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Alarm Triggers Breakdown
            item {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { activeExplanation = TelemetryGlossary.WAKEUP_ALARMS }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Wakeup Alarms (${culprit.wakeupAlarmCount} total)",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = TextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            if (culprit.topAlarmActions.isEmpty()) {
                item {
                    Text("No wakeup alarm actions recorded for this app.", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                items(culprit.topAlarmActions.entries.toList()) { (action, count) ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = action,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "$count wakeups",
                                fontWeight = FontWeight.Bold,
                                color = AccentBlue,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Shared Packages list
            if (culprit.sharedPackageNames.size > 1) {
                item {
                    Text(
                        text = "Constituent Shared Packages",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            culprit.sharedPackageNames.forEach { pkg ->
                                Text(text = "• $pkg", fontSize = 12.sp, color = TextSecondary)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(28.dp))
            }
        }

        // Explanation Dialog
        MetricExplanationDialog(
            explanation = activeExplanation,
            onDismiss = { activeExplanation = null }
        )
    }
}

@Composable
private fun EnergyRowItem(
    label: String,
    mahValue: String,
    detail: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clip(RoundedCornerShape(6.dp)).clickable { onClick() } else it }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f, fill = false)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                if (onClick != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = TextSecondary.copy(alpha = 0.4f),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
            Text(detail, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(mahValue, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}

private fun formatDataVolume(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val kb = bytes / 1024.0
    if (kb < 1024.0) return "${String.format(Locale.US, "%.1f", kb)} KB"
    val mb = kb / 1024.0
    if (mb < 1024.0) return "${String.format(Locale.US, "%.1f", mb)} MB"
    val gb = mb / 1024.0
    return "${String.format(Locale.US, "%.2f", gb)} GB"
}
