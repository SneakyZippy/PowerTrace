package com.sneakyzippy.powerlab.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sneakyzippy.powerlab.core.model.ChargeTelemetrySample
import com.sneakyzippy.powerlab.core.model.ChargeTestSession
import com.sneakyzippy.powerlab.ui.components.CompareChargeCurveChart
import com.sneakyzippy.powerlab.ui.components.SessionConditionChipsRow
import com.sneakyzippy.powerlab.ui.theme.AccentBlue
import com.sneakyzippy.powerlab.ui.theme.CulpritAmber
import com.sneakyzippy.powerlab.ui.theme.DarkBackground
import com.sneakyzippy.powerlab.ui.theme.DarkSurface
import com.sneakyzippy.powerlab.ui.theme.DarkSurfaceVariant
import com.sneakyzippy.powerlab.ui.theme.GreenPrimary
import com.sneakyzippy.powerlab.ui.theme.TextPrimary
import com.sneakyzippy.powerlab.ui.theme.TextSecondary
import com.sneakyzippy.powerlab.ui.util.TimeFormatUtils
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareChargeSessionsBottomSheet(
    initialSessionA: ChargeTestSession,
    initialSessionB: ChargeTestSession,
    sheetState: SheetState,
    getSamplesForSession: (String) -> Flow<List<ChargeTelemetrySample>>,
    onDismiss: () -> Unit
) {
    var swapped by remember { mutableStateOf(false) }
    val sessionA = if (swapped) initialSessionB else initialSessionA
    val sessionB = if (swapped) initialSessionA else initialSessionB

    val samplesA by getSamplesForSession(sessionA.id).collectAsState(initial = emptyList())
    val samplesB by getSamplesForSession(sessionB.id).collectAsState(initial = emptyList())

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkBackground,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(GreenPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CompareArrows, contentDescription = null, tint = GreenPrimary)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Compare Charging Cycles",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Side-by-side power, thermal & speed telemetry",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                IconButton(
                    onClick = { swapped = !swapped },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Swap A and B",
                        tint = AccentBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Session A & B Header Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CompareSessionHeaderCard(
                    badge = "A",
                    accentColor = GreenPrimary,
                    session = sessionA,
                    modifier = Modifier.weight(1f)
                )
                CompareSessionHeaderCard(
                    badge = "B",
                    accentColor = AccentBlue,
                    session = sessionB,
                    modifier = Modifier.weight(1f)
                )
            }

            // Quick Verdict Banner
            CompareVerdictBanner(sessionA = sessionA, sessionB = sessionB)

            // Visual Overlay Chart
            CompareChargeCurveChart(
                sessionA = sessionA,
                samplesA = samplesA,
                sessionB = sessionB,
                samplesB = samplesB
            )

            // Full Side-by-Side Metrics Table
            CompareMetricsTable(sessionA = sessionA, sessionB = sessionB)

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun CompareSessionHeaderCard(
    badge: String,
    accentColor: Color,
    session: ChargeTestSession,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd • HH:mm", Locale.getDefault()) }
    Card(
        modifier = modifier.border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(badge, color = accentColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = dateFormat.format(Date(session.startTimeMs)),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = session.brickName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = session.cableName,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (session.notes.isNotBlank()) {
                SessionConditionChipsRow(notes = session.notes, compact = true)
            }
        }
    }
}

@Composable
private fun CompareVerdictBanner(sessionA: ChargeTestSession, sessionB: ChargeTestSession) {
    val avgDelta = sessionA.avgWattage - sessionB.avgWattage
    val tempDelta = sessionA.maxTempCelsius - sessionB.maxTempCelsius
    val fasterLabel = if (avgDelta >= 0f) "A" else "B"
    val pctDiff = if (sessionB.avgWattage > 0f && sessionA.avgWattage > 0f) {
        val base = minOf(sessionA.avgWattage, sessionB.avgWattage)
        ((kotlin.math.abs(avgDelta) / base) * 100f).toInt()
    } else 0

    val summary = buildString {
        if (pctDiff >= 2) {
            append("Session $fasterLabel averaged $pctDiff% higher power ")
            append(String.format(Locale.US, "(%+.1f W). ", avgDelta))
        } else {
            append("Both sessions delivered similar average power. ")
        }
        if (kotlin.math.abs(tempDelta) >= 0.5f) {
            val cooler = if (tempDelta < 0f) "A" else "B"
            append(String.format(Locale.US, "Session %s ran %.1f°C cooler at peak.", cooler, kotlin.math.abs(tempDelta)))
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Text(
            text = summary.trim(),
            style = MaterialTheme.typography.bodySmall,
            color = TextPrimary,
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
private fun CompareMetricsTable(sessionA: ChargeTestSession, sessionB: ChargeTestSession) {
    val rateA = if (sessionA.durationMs > 0L) (sessionA.batteryGained * 3_600_000f) / sessionA.durationMs else 0f
    val rateB = if (sessionB.durationMs > 0L) (sessionB.batteryGained * 3_600_000f) / sessionB.durationMs else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Metric", style = MaterialTheme.typography.labelSmall, color = TextSecondary, modifier = Modifier.weight(1.2f))
                Text("A", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = GreenPrimary, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                Text("B", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = AccentBlue, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
            }
            HorizontalDivider(color = DarkSurfaceVariant)

            CompareRow("Peak Power", String.format(Locale.US, "%.1f W", sessionA.peakWattage), String.format(Locale.US, "%.1f W", sessionB.peakWattage))
            CompareRow("Avg Power", String.format(Locale.US, "%.1f W", sessionA.avgWattage), String.format(Locale.US, "%.1f W", sessionB.avgWattage))
            CompareRow("Duration", TimeFormatUtils.formatDurationHuman(sessionA.durationMs), TimeFormatUtils.formatDurationHuman(sessionB.durationMs))
            CompareRow("Battery Gain", "${sessionA.startBatteryLevel}→${sessionA.endBatteryLevel}% (+${sessionA.batteryGained}%)", "${sessionB.startBatteryLevel}→${sessionB.endBatteryLevel}% (+${sessionB.batteryGained}%)")
            CompareRow("Charge Speed", String.format(Locale.US, "%.1f %%/hr", rateA), String.format(Locale.US, "%.1f %%/hr", rateB))
            CompareRow("Peak Temp", String.format(Locale.US, "%.1f°C", sessionA.maxTempCelsius), String.format(Locale.US, "%.1f°C", sessionB.maxTempCelsius), highlightWarning = sessionA.maxTempCelsius >= 39f || sessionB.maxTempCelsius >= 39f)
            CompareRow("Temp Rise", String.format(Locale.US, "%+.1f°C", sessionA.maxTempCelsius - sessionA.startTempCelsius), String.format(Locale.US, "%+.1f°C", sessionB.maxTempCelsius - sessionB.startTempCelsius))
            CompareRow("Throttled", if (sessionA.isThrottled) "Yes" else "No", if (sessionB.isThrottled) "Yes" else "No", highlightWarning = sessionA.isThrottled || sessionB.isThrottled)
            CompareRow("Peak Current", "${sessionA.peakCurrentMa} mA", "${sessionB.peakCurrentMa} mA")
            CompareRow("Voltage Range", "${sessionA.minVoltageMv}–${sessionA.maxVoltageMv}", "${sessionB.minVoltageMv}–${sessionB.maxVoltageMv}")
        }
    }
}

@Composable
private fun CompareRow(
    label: String,
    valueA: String,
    valueB: String,
    highlightWarning: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.weight(1.2f)
        )
        Text(
            text = valueA,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = if (highlightWarning) CulpritAmber else TextPrimary,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = valueB,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = if (highlightWarning) CulpritAmber else TextPrimary,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}
