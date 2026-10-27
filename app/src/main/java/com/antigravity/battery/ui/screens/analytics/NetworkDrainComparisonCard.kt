package com.antigravity.battery.ui.screens.analytics

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.antigravity.battery.core.model.NetworkDrainComparison
import com.antigravity.battery.ui.theme.AccentBlue
import com.antigravity.battery.ui.theme.CulpritAmber
import com.antigravity.battery.ui.theme.DarkBackground
import com.antigravity.battery.ui.theme.DarkSurface
import com.antigravity.battery.ui.theme.GreenPrimary
import com.antigravity.battery.ui.theme.TextPrimary
import com.antigravity.battery.ui.theme.TextSecondary

@Composable
fun NetworkDrainComparisonCard(comparison: NetworkDrainComparison) {
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
