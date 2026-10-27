package com.antigravity.battery.ui.screens.overview

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.battery.core.engine.TimelineViewData
import com.antigravity.battery.ui.theme.AccentBlue
import com.antigravity.battery.ui.theme.CulpritAmber
import com.antigravity.battery.ui.theme.CulpritRed
import com.antigravity.battery.ui.theme.DarkBackground
import com.antigravity.battery.ui.theme.DarkSurface
import com.antigravity.battery.ui.theme.GreenPrimary
import com.antigravity.battery.ui.theme.TextPrimary
import com.antigravity.battery.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun OvernightSleepReportCard(
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
