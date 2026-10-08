package com.antigravity.battery.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import com.antigravity.battery.core.model.CulpritApp
import com.antigravity.battery.core.model.SessionDelta
import com.antigravity.battery.ui.components.DozeSleepEfficiencyBar
import com.antigravity.battery.ui.components.MetricStatTile
import com.antigravity.battery.ui.components.WarningBadgeChip
import com.antigravity.battery.ui.theme.CulpritAmber
import com.antigravity.battery.ui.theme.CulpritRed
import com.antigravity.battery.ui.theme.DarkBackground
import com.antigravity.battery.ui.theme.DarkSurface
import com.antigravity.battery.ui.theme.DarkSurfaceVariant
import com.antigravity.battery.ui.theme.GreenPrimary
import com.antigravity.battery.ui.theme.TextPrimary
import com.antigravity.battery.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun DashboardScreen(
    delta: SessionDelta?,
    modifier: Modifier = Modifier.fillMaxSize(),
    onSelectCulprit: (CulpritApp) -> Unit
) {
    if (delta == null) {
        Box(
            modifier = modifier
                .background(DarkBackground)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "No Active Analysis",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Start and stop an Observation Session to calculate delta consumption and identify wakelock offenders.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .background(DarkBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Results Dashboard",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
            Text(
                text = "Exact delta attribution over observation window",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        // Interruption Warning Banner
        if (delta.isInterrupted) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CulpritAmber.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = CulpritAmber)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Session Interrupted", fontWeight = FontWeight.Bold, color = CulpritAmber)
                            Text(
                                delta.interruptionReason ?: "Counter anomaly detected",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }

        // Executive Stat Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricStatTile(
                    label = "BATTERY DRAIN",
                    value = "-${delta.batteryPercentDrop}",
                    unit = "%",
                    valueColor = if (delta.batteryPercentDrop > 5) CulpritRed else GreenPrimary,
                    modifier = Modifier.weight(1f)
                )
                MetricStatTile(
                    label = "DRAIN RATE",
                    value = String.format(Locale.US, "%.1f", delta.drainRatePercentPerHour),
                    unit = "%/hr",
                    valueColor = if (delta.drainRatePercentPerHour > 1.5) CulpritAmber else GreenPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val durationHours = delta.durationMs.toDouble() / 3_600_000.0
                MetricStatTile(
                    label = "WINDOW DURATION",
                    value = String.format(Locale.US, "%.1f", durationHours),
                    unit = "hrs",
                    modifier = Modifier.weight(1f)
                )
                MetricStatTile(
                    label = "DOZE EFFICIENCY",
                    value = "${delta.deepSleepEfficiencyPercent.toInt()}",
                    unit = "%",
                    valueColor = if (delta.deepSleepEfficiencyPercent >= 80) GreenPrimary else CulpritRed,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Sleep state distribution bar
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    DozeSleepEfficiencyBar(
                        deepSleepMs = delta.deepSleepDurationMs,
                        awakeScreenOffMs = delta.awakeScreenOffMs,
                        screenOnMs = delta.screenOnDurationMs
                    )
                }
            }
        }

        // Top Culprits List Header
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Top Culprits (Ranked by mAh)",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = "${delta.culprits.size} apps detected",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }

        // Ranked Culprits Items
        items(delta.culprits) { culprit ->
            CulpritListItem(
                culprit = culprit,
                onClick = { onSelectCulprit(culprit) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CulpritListItem(
    culprit: CulpritApp,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App Avatar
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = culprit.appLabel.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = GreenPrimary,
                        fontSize = 18.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // App info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = culprit.appLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = culprit.primaryPackageName,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    if (culprit.sharedPackageNames.size > 1) {
                        Text(
                            text = "+ ${culprit.sharedPackageNames.size - 1} shared packages",
                            fontSize = 10.sp,
                            color = GreenPrimary
                        )
                    }
                }

                // Estimated mAh
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${String.format(Locale.US, "%.1f", culprit.totalEstimatedMah)} mAh",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (culprit.totalEstimatedMah > 15.0) CulpritRed else TextPrimary
                    )
                    val wlMins = culprit.partialWakelockMs / 60000
                    Text(
                        text = "${wlMins}m wl • ${culprit.wakeupAlarmCount} alarms",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            // Warning chips row if any
            if (culprit.warningBadges.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    culprit.warningBadges.forEach { badge ->
                        WarningBadgeChip(badge = badge)
                    }
                }
            }
        }
    }
}
