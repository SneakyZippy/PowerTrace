package com.antigravity.battery.ui.screens.culprit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.antigravity.battery.core.model.CulpritApp
import com.antigravity.battery.ui.components.MetricExplanation
import com.antigravity.battery.ui.components.TelemetryGlossary
import com.antigravity.battery.ui.theme.DarkSurface
import com.antigravity.battery.ui.theme.TextPrimary
import java.util.Locale

@Composable
fun CulpritHardwareAttributionCard(
    culprit: CulpritApp,
    onExplain: (MetricExplanation) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
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
                    onClick = { onExplain(TelemetryGlossary.CPU_PROCESSING) }
                )
                EnergyRowItem(
                    label = "Wakelock Hold",
                    mahValue = "${String.format(Locale.US, "%.2f", culprit.energyBreakdown.wakelockMah)} mAh",
                    detail = "${culprit.partialWakelockMs / 1000}s hold",
                    onClick = { onExplain(TelemetryGlossary.WAKELOCK_HOLD) }
                )

                // Mobile Radio with network transfer volume
                val mobileDataText = if (culprit.mobileBytesRx > 0 || culprit.mobileBytesTx > 0) {
                    " • ${formatDataVolume(culprit.mobileBytesRx)} ↓  ${formatDataVolume(culprit.mobileBytesTx)} ↑"
                } else ""
                EnergyRowItem(
                    label = "Mobile Radio",
                    mahValue = "${String.format(Locale.US, "%.2f", culprit.energyBreakdown.mobileRadioMah)} mAh",
                    detail = "${culprit.mobileRadioActiveMs / 1000}s active$mobileDataText",
                    onClick = { onExplain(TelemetryGlossary.MOBILE_RADIO) }
                )

                // Wi-Fi with network transfer volume
                val wifiDataText = if (culprit.wifiBytesRx > 0 || culprit.wifiBytesTx > 0) {
                    " • ${formatDataVolume(culprit.wifiBytesRx)} ↓  ${formatDataVolume(culprit.wifiBytesTx)} ↑"
                } else ""
                EnergyRowItem(
                    label = "Wi-Fi Activity",
                    mahValue = "${String.format(Locale.US, "%.2f", culprit.energyBreakdown.wifiMah)} mAh",
                    detail = "${culprit.wifiScanCount} scans$wifiDataText",
                    onClick = { onExplain(TelemetryGlossary.WIFI_ACTIVITY) }
                )

                if (culprit.gpsDurationMs > 0) {
                    EnergyRowItem(
                        label = "GPS Location",
                        mahValue = "${String.format(Locale.US, "%.2f", culprit.energyBreakdown.gpsMah)} mAh",
                        detail = "${culprit.gpsDurationMs / 1000}s active",
                        onClick = { onExplain(TelemetryGlossary.GPS_LOCATION) }
                    )
                }
            }
        }
    }
}
