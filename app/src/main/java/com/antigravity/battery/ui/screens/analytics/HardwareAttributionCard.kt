package com.antigravity.battery.ui.screens.analytics

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.antigravity.battery.core.model.HardwareEnergyBreakdown
import com.antigravity.battery.ui.theme.AccentBlue
import com.antigravity.battery.ui.theme.CulpritAmber
import com.antigravity.battery.ui.theme.DarkSurface
import com.antigravity.battery.ui.theme.GreenPrimary
import com.antigravity.battery.ui.theme.TextPrimary
import com.antigravity.battery.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun HardwareAttributionCard(breakdown: HardwareEnergyBreakdown) {
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
fun HardwareRow(label: String, mah: Double, pct: Int, color: Color) {
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
