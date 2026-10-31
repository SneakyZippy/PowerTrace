package com.antigravity.battery.ui.components

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
import androidx.compose.material.icons.filled.Warning
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
import com.antigravity.battery.core.model.ChargeTestSession
import com.antigravity.battery.ui.theme.AccentBlue
import com.antigravity.battery.ui.theme.CulpritAmber
import com.antigravity.battery.ui.theme.DarkSurface
import com.antigravity.battery.ui.theme.GreenPrimary
import com.antigravity.battery.ui.theme.TextPrimary
import com.antigravity.battery.ui.theme.TextSecondary
import com.antigravity.battery.ui.util.TimeFormatUtils
import java.util.Locale

@Composable
fun SessionMetricsSummaryCard(
    session: ChargeTestSession,
    durationMin: Long,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem("Peak Power", String.format(Locale.US, "%.1f W", session.peakWattage), GreenPrimary)
                MetricItem("Avg Power", String.format(Locale.US, "%.1f W", session.avgWattage), TextPrimary)
                MetricItem("Duration", TimeFormatUtils.formatDurationHuman(session.durationMs), AccentBlue)
                MetricItem("Gained", "+${session.batteryGained}%", GreenPrimary)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem("Battery Range", "${session.startBatteryLevel}% → ${session.endBatteryLevel}%")
                MetricItem("Peak Current", "${session.peakCurrentMa} mA")
                MetricItem(
                    label = "Max Temp",
                    value = String.format(Locale.US, "%.1f°C", session.maxTempCelsius),
                    highlightColor = if (session.maxTempCelsius >= 39f) CulpritAmber else TextPrimary
                )
            }

            if (session.isThrottled) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = CulpritAmber, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Thermal throttling was detected during this charge session",
                        style = MaterialTheme.typography.bodySmall,
                        color = CulpritAmber
                    )
                }
            }
        }
    }
}

@Composable
fun MetricItem(
    label: String,
    value: String,
    highlightColor: Color = TextPrimary
) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = highlightColor
        )
    }
}
