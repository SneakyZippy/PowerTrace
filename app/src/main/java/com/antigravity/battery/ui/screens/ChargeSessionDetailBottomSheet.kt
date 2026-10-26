package com.antigravity.battery.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.battery.core.model.Cable
import com.antigravity.battery.core.model.ChargeTelemetrySample
import com.antigravity.battery.core.model.ChargeTestSession
import com.antigravity.battery.core.model.PowerBrick
import com.antigravity.battery.ui.components.ChargeCurveChart
import com.antigravity.battery.ui.components.EditChargeSessionDialog
import com.antigravity.battery.ui.theme.AccentBlue
import com.antigravity.battery.ui.theme.CulpritAmber
import com.antigravity.battery.ui.theme.CulpritRed
import com.antigravity.battery.ui.theme.DarkBackground
import com.antigravity.battery.ui.theme.DarkSurface
import com.antigravity.battery.ui.theme.DarkSurfaceVariant
import com.antigravity.battery.ui.theme.GreenPrimary
import com.antigravity.battery.ui.theme.TextPrimary
import com.antigravity.battery.ui.theme.TextSecondary
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChargeSessionDetailBottomSheet(
    session: ChargeTestSession,
    sheetState: SheetState,
    samplesFlow: Flow<List<ChargeTelemetrySample>>,
    powerBricks: List<PowerBrick> = emptyList(),
    cables: List<Cable> = emptyList(),
    onUpdateSession: (ChargeTestSession) -> Unit = {},
    onDelete: (ChargeTestSession) -> Unit,
    onDismiss: () -> Unit
) {
    val samples by samplesFlow.collectAsState(initial = emptyList())
    var currentSession by remember(session) { mutableStateOf(session) }
    var showEditDialog by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
    val durationMin = (currentSession.durationMs / 60000).coerceAtLeast(1)

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
                .verticalScroll(rememberScrollState())
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
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(GreenPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = GreenPrimary)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "${currentSession.brickName} + ${currentSession.cableName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = dateFormat.format(Date(currentSession.startTimeMs)),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                IconButton(
                    onClick = { showEditDialog = true },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit details",
                        tint = GreenPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Stat Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "PEAK POWER",
                    value = String.format(Locale.US, "%.1f W", currentSession.peakWattage),
                    color = GreenPrimary
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "AVG POWER",
                    value = String.format(Locale.US, "%.1f W", currentSession.avgWattage),
                    color = TextPrimary
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "DURATION",
                    value = "${durationMin}m",
                    color = AccentBlue
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "GAIN",
                    value = "+${currentSession.batteryGained}%",
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Charging Curve Graph
            ChargeCurveChart(samples = samples)

            Spacer(modifier = Modifier.height(16.dp))

            // Notes & Environmental Conditions Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Notes,
                                contentDescription = null,
                                tint = AccentBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Notes & Conditions",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        IconButton(
                            onClick = { showEditDialog = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit notes",
                                tint = AccentBlue,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (currentSession.notes.isNotBlank()) {
                        Text(
                            text = currentSession.notes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            lineHeight = 20.sp
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurfaceVariant.copy(alpha = 0.5f))
                                .clickable { showEditDialog = true }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Add notes (e.g. cold room, gaming, screen off, case)",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Additional Technical Specs Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Hardware Telemetry Details",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    DetailRow(
                        label = "Battery Range Tested",
                        value = "${currentSession.startBatteryLevel}% → ${currentSession.endBatteryLevel}%"
                    )
                    DetailRow(
                        label = "Peak Charging Current",
                        value = "${currentSession.peakCurrentMa} mA (${String.format(Locale.US, "%.2f A", currentSession.peakCurrentMa / 1000f)})"
                    )
                    DetailRow(
                        label = "Battery Cell Voltage Range",
                        value = "${currentSession.minVoltageMv} mV – ${currentSession.maxVoltageMv} mV"
                    )
                    DetailRow(
                        label = "Battery Temperature",
                        value = "${String.format(Locale.US, "%.1f°C", currentSession.startTempCelsius)} → ${String.format(Locale.US, "%.1f°C", currentSession.maxTempCelsius)}"
                    )
                    DetailRow(
                        label = "Assigned Charger",
                        value = currentSession.brickName
                    )
                    DetailRow(
                        label = "Assigned Cable",
                        value = currentSession.cableName
                    )
                }
            }

            if (currentSession.isThrottled) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CulpritAmber.copy(alpha = 0.15f))
                        .padding(12.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = CulpritAmber, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Thermal throttling detected: Battery reached elevated temperatures causing charging wattage to scale back.",
                        style = MaterialTheme.typography.bodySmall,
                        color = CulpritAmber
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onDelete(currentSession)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CulpritRed)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete")
                }

                OutlinedButton(
                    onClick = { showEditDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenPrimary)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit")
                }

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                ) {
                    Text("Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (showEditDialog) {
            EditChargeSessionDialog(
                session = currentSession,
                powerBricks = powerBricks,
                cables = cables,
                onDismiss = { showEditDialog = false },
                onSave = { updated ->
                    currentSession = updated
                    onUpdateSession(updated)
                }
            )
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    }
}
