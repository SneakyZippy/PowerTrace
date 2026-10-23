package com.antigravity.battery.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.battery.core.model.Cable
import com.antigravity.battery.core.model.ChargeTestSession
import com.antigravity.battery.core.model.PowerBrick
import com.antigravity.battery.ui.components.AddCableDialog
import com.antigravity.battery.ui.components.AddPowerBrickDialog
import com.antigravity.battery.ui.theme.AccentBlue
import com.antigravity.battery.ui.theme.CulpritAmber
import com.antigravity.battery.ui.theme.DarkBackground
import com.antigravity.battery.ui.theme.DarkSurface
import com.antigravity.battery.ui.theme.DarkSurfaceVariant
import com.antigravity.battery.ui.theme.GreenPrimary
import com.antigravity.battery.ui.theme.TextPrimary
import com.antigravity.battery.ui.theme.TextSecondary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TagAccessoriesBottomSheet(
    session: ChargeTestSession,
    sheetState: SheetState,
    powerBricks: List<PowerBrick>,
    cables: List<Cable>,
    onSave: (brickId: String?, cableId: String?, brickName: String, cableName: String, notes: String) -> Unit,
    onSaveNewBrick: (PowerBrick) -> Unit,
    onSaveNewCable: (Cable) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedBrickId by remember { mutableStateOf(session.brickId ?: powerBricks.firstOrNull()?.id) }
    var selectedCableId by remember { mutableStateOf(session.cableId ?: cables.firstOrNull()?.id) }
    var notes by remember { mutableStateOf(session.notes) }

    var showAddBrickDialog by remember { mutableStateOf(false) }
    var showAddCableDialog by remember { mutableStateOf(false) }

    val durationMin = (session.durationMs / 60000).coerceAtLeast(1)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkBackground,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(GreenPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = GreenPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Tag Charging Session",
                        style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Associate this test with your charger and cable",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Metrics Summary Card
            Box(
                modifier = Modifier
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
                        MetricItem(label = "Peak Power", value = String.format(Locale.US, "%.1f W", session.peakWattage), highlightColor = GreenPrimary)
                        MetricItem(label = "Avg Power", value = String.format(Locale.US, "%.1f W", session.avgWattage))
                        MetricItem(label = "Duration", value = "${durationMin}m")
                        MetricItem(label = "Battery", value = "+${session.batteryGained}%")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Tested: ${session.startBatteryLevel}% → ${session.endBatteryLevel}%",
                            style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        Text(
                            text = "Peak Temp: ${String.format(Locale.US, "%.1f°C", session.maxTempCelsius)}",
                            style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                    }

                    if (session.isThrottled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(CulpritAmber.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = CulpritAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Thermal throttling detected during charging",
                                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                                color = CulpritAmber
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Power Brick Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Power Brick",
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "+ Add New",
                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                    color = GreenPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { showAddBrickDialog = true }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                powerBricks.forEach { brick ->
                    val isSelected = selectedBrickId == brick.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedBrickId = brick.id },
                        label = {
                            Text(
                                text = "${brick.name}${if (brick.ratedWattage > 0) " (${brick.ratedWattage.toInt()}W)" else ""}"
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Power,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = DarkSurface,
                            labelColor = TextSecondary,
                            selectedContainerColor = GreenPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = GreenPrimary,
                            selectedLeadingIconColor = GreenPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Cable Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cable",
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "+ Add New",
                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                    color = AccentBlue,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { showAddCableDialog = true }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                cables.forEach { cable ->
                    val isSelected = selectedCableId == cable.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCableId = cable.id },
                        label = { Text(text = cable.name) },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Cable,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = DarkSurface,
                            labelColor = TextSecondary,
                            selectedContainerColor = AccentBlue.copy(alpha = 0.2f),
                            selectedLabelColor = AccentBlue,
                            selectedLeadingIconColor = AccentBlue
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Notes Input
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional, e.g. Port 1, hot day)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = GreenPrimary,
                    unfocusedBorderColor = DarkSurfaceVariant,
                    focusedLabelColor = GreenPrimary,
                    unfocusedLabelColor = TextSecondary,
                    cursorColor = GreenPrimary
                ),
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                ) {
                    Text("Discard")
                }
                Button(
                    onClick = {
                        val chosenBrick = powerBricks.find { it.id == selectedBrickId }
                        val chosenCable = cables.find { it.id == selectedCableId }
                        onSave(
                            selectedBrickId,
                            selectedCableId,
                            chosenBrick?.name ?: "Unspecified Charger",
                            chosenCable?.name ?: "Unspecified Cable",
                            notes
                        )
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) {
                    Text("Save Benchmark", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Dialogs to add brick / cable directly from sheet
        if (showAddBrickDialog) {
            AddPowerBrickDialog(
                onDismiss = { showAddBrickDialog = false },
                onConfirm = { newBrick ->
                    onSaveNewBrick(newBrick)
                    selectedBrickId = newBrick.id
                    showAddBrickDialog = false
                }
            )
        }

        if (showAddCableDialog) {
            AddCableDialog(
                onDismiss = { showAddCableDialog = false },
                onConfirm = { newCable ->
                    onSaveNewCable(newCable)
                    selectedCableId = newCable.id
                    showAddCableDialog = false
                }
            )
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    highlightColor: Color = TextPrimary
) {
    Column {
        Text(text = label, style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = TextSecondary)
        Text(
            text = value,
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = highlightColor
        )
    }
}
