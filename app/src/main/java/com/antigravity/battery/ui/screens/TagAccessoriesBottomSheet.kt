package com.antigravity.battery.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.antigravity.battery.ui.theme.CulpritRed
import com.antigravity.battery.ui.theme.DarkBackground
import com.antigravity.battery.ui.theme.DarkSurface
import com.antigravity.battery.ui.theme.DarkSurfaceVariant
import com.antigravity.battery.ui.theme.GreenPrimary
import com.antigravity.battery.ui.theme.TextPrimary
import com.antigravity.battery.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TagAccessoriesBottomSheet(
    session: ChargeTestSession,
    powerBricks: List<PowerBrick>,
    cables: List<Cable>,
    onSave: (brickId: String?, cableId: String?, brickName: String, cableName: String, notes: String) -> Unit,
    onSaveNewBrick: (PowerBrick) -> Unit,
    onSaveNewCable: (Cable) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedBrickId by remember { mutableStateOf(session.brickId ?: powerBricks.firstOrNull()?.id) }
    var selectedCableId by remember { mutableStateOf(session.cableId ?: cables.firstOrNull()?.id) }
    var notes by remember { mutableStateOf(session.notes) }

    var showAddBrickDialog by remember { mutableStateOf(false) }
    var showAddCableDialog by remember { mutableStateOf(false) }
    var showDismissConfirmDialog by remember { mutableStateOf(false) }

    val durationMin = (session.durationMs / 60000).coerceAtLeast(1)

    val quickTags = listOf(
        "❄️ Cold room",
        "☀️ Warm room",
        "📱 Screen off",
        "🎮 Gaming while charging",
        "🛡️ Case off",
        "📦 Thick case",
        "✈️ Airplane mode",
        "🔌 Car charger"
    )

    fun saveAndDismiss() {
        val chosenBrick = powerBricks.find { it.id == selectedBrickId }
        val chosenCable = cables.find { it.id == selectedCableId }
        onSave(
            selectedBrickId,
            selectedCableId,
            chosenBrick?.name ?: "Unspecified Charger",
            chosenCable?.name ?: "Unspecified Cable",
            notes.trim()
        )
    }

    // Modal Bottom Sheet for Tagging
    ModalBottomSheet(
        onDismissRequest = {
            // Intercept swipe down / scrim tap so the user does NOT accidentally lose their test
            showDismissConfirmDialog = true
        },
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
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Associate this test with your charger and cable",
                        style = MaterialTheme.typography.bodyMedium,
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
                        MetricItem("Peak Power", String.format(Locale.US, "%.1f W", session.peakWattage), GreenPrimary)
                        MetricItem("Avg Power", String.format(Locale.US, "%.1f W", session.avgWattage), TextPrimary)
                        MetricItem("Duration", "${durationMin}m", AccentBlue)
                        MetricItem("Gained", "+${session.batteryGained}%", GreenPrimary)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricItem("Battery Range", "${session.startBatteryLevel}% → ${session.endBatteryLevel}%")
                        MetricItem("Peak Current", "${session.peakCurrentMa} mA")
                        MetricItem("Max Temp", String.format(Locale.US, "%.1f°C", session.maxTempCelsius), if (session.maxTempCelsius >= 39f) CulpritAmber else TextPrimary)
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

            Spacer(modifier = Modifier.height(20.dp))

            // Charger Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Which charger did you use?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "+ New Brick",
                    style = MaterialTheme.typography.labelMedium,
                    color = GreenPrimary,
                    modifier = Modifier.clickable { showAddBrickDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedBrickId == null,
                    onClick = { selectedBrickId = null },
                    label = { Text("Unspecified") },
                    leadingIcon = if (selectedBrickId == null) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = DarkSurface,
                        labelColor = TextSecondary,
                        selectedContainerColor = GreenPrimary.copy(alpha = 0.2f),
                        selectedLabelColor = GreenPrimary,
                        selectedLeadingIconColor = GreenPrimary
                    )
                )

                powerBricks.forEach { brick ->
                    val isSelected = selectedBrickId == brick.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedBrickId = brick.id },
                        label = { Text(brick.name) },
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

            Spacer(modifier = Modifier.height(20.dp))

            // Cable Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Which cable did you use?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "+ New Cable",
                    style = MaterialTheme.typography.labelMedium,
                    color = AccentBlue,
                    modifier = Modifier.clickable { showAddCableDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedCableId == null,
                    onClick = { selectedCableId = null },
                    label = { Text("Unspecified / Direct") },
                    leadingIcon = if (selectedCableId == null) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = DarkSurface,
                        labelColor = TextSecondary,
                        selectedContainerColor = AccentBlue.copy(alpha = 0.2f),
                        selectedLabelColor = AccentBlue,
                        selectedLeadingIconColor = AccentBlue
                    )
                )

                cables.forEach { cable ->
                    val isSelected = selectedCableId == cable.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCableId = cable.id },
                        label = { Text(cable.name) },
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

            // Notes Input & Quick Tags
            Text(
                text = "Notes & Environmental Conditions",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                placeholder = {
                    Text(
                        "e.g. Charging in cold room, screen off, gaming, thick case...",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary.copy(alpha = 0.7f)
                    )
                },
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
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Tag Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                quickTags.forEach { tag ->
                    SuggestionChip(
                        onClick = {
                            notes = if (notes.isBlank()) {
                                tag
                            } else if (!notes.contains(tag)) {
                                "$notes • $tag"
                            } else {
                                notes
                            }
                        },
                        label = { Text(tag, fontSize = 11.sp, color = TextPrimary) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = DarkSurfaceVariant
                        ),
                        border = null,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        // Show prompt instead of discarding silently
                        showDismissConfirmDialog = true
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CulpritRed)
                ) {
                    Text("Discard")
                }
                Button(
                    onClick = {
                        saveAndDismiss()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    shape = RoundedCornerShape(10.dp)
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

    // Confirmation / Accept-Discard Dialog when swiping down or clicking discard
    if (showDismissConfirmDialog) {
        AlertDialog(
            onDismissRequest = {
                // If tapped outside or back pressed on the dialog, safely save to history
                // so the user NEVER loses their benchmark data!
                saveAndDismiss()
                showDismissConfirmDialog = false
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(18.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = GreenPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Save Charging Benchmark?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "You finished a charging test (+${session.batteryGained}% in ${durationMin}m, peak ${String.format(Locale.US, "%.1f", session.peakWattage)} W).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Would you like to save this benchmark test to your history or discard it?",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        saveAndDismiss()
                        showDismissConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save to History", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            showDismissConfirmDialog = false
                            coroutineScope.launch { sheetState.show() }
                        }
                    ) {
                        Text("Keep Tagging", color = TextSecondary)
                    }

                    TextButton(
                        onClick = {
                            showDismissConfirmDialog = false
                            onDismiss() // Explicitly discards session
                        }
                    ) {
                        Text("Discard", color = CulpritRed, fontWeight = FontWeight.Bold)
                    }
                }
            }
        )
    }
}

@Composable
private fun MetricItem(
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
