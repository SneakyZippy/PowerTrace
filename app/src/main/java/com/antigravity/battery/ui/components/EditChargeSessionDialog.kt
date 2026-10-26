package com.antigravity.battery.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Power
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.battery.core.model.Cable
import com.antigravity.battery.core.model.ChargeTestSession
import com.antigravity.battery.core.model.PowerBrick
import com.antigravity.battery.ui.theme.AccentBlue
import com.antigravity.battery.ui.theme.DarkBackground
import com.antigravity.battery.ui.theme.DarkSurface
import com.antigravity.battery.ui.theme.DarkSurfaceVariant
import com.antigravity.battery.ui.theme.GreenPrimary
import com.antigravity.battery.ui.theme.TextPrimary
import com.antigravity.battery.ui.theme.TextSecondary

@Composable
fun EditChargeSessionDialog(
    session: ChargeTestSession,
    powerBricks: List<PowerBrick>,
    cables: List<Cable>,
    onDismiss: () -> Unit,
    onSave: (ChargeTestSession) -> Unit
) {
    var notes by remember { mutableStateOf(session.notes) }
    var selectedBrickId by remember { mutableStateOf(session.brickId) }
    var selectedCableId by remember { mutableStateOf(session.cableId) }

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

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = GreenPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Edit Test Details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Notes Input
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Notes & Environmental Conditions",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        placeholder = {
                            Text(
                                "e.g. Charging in cold room, phone screen was off, gaming during charge...",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary.copy(alpha = 0.7f)
                            )
                        },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = GreenPrimary,
                            unfocusedBorderColor = DarkSurfaceVariant,
                            focusedLabelColor = GreenPrimary,
                            unfocusedLabelColor = TextSecondary,
                            cursorColor = GreenPrimary,
                            focusedContainerColor = DarkBackground,
                            unfocusedContainerColor = DarkBackground
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Quick suggestion chips
                    Text(
                        text = "Quick Suggestions",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
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
                }

                // Charger Selection
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Assigned Charger",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Unspecified option
                        FilterChip(
                            selected = selectedBrickId == null,
                            onClick = { selectedBrickId = null },
                            label = { Text("Unspecified", fontSize = 12.sp) },
                            leadingIcon = if (selectedBrickId == null) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GreenPrimary.copy(alpha = 0.2f),
                                selectedLabelColor = GreenPrimary,
                                containerColor = DarkBackground,
                                labelColor = TextSecondary
                            )
                        )

                        powerBricks.forEach { brick ->
                            val isSelected = selectedBrickId == brick.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedBrickId = brick.id },
                                label = { Text(brick.name, fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Power,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GreenPrimary.copy(alpha = 0.2f),
                                    selectedLabelColor = GreenPrimary,
                                    containerColor = DarkBackground,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }
                }

                // Cable Selection
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Assigned Cable",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Unspecified option
                        FilterChip(
                            selected = selectedCableId == null,
                            onClick = { selectedCableId = null },
                            label = { Text("Unspecified / Direct", fontSize = 12.sp) },
                            leadingIcon = if (selectedCableId == null) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentBlue.copy(alpha = 0.2f),
                                selectedLabelColor = AccentBlue,
                                containerColor = DarkBackground,
                                labelColor = TextSecondary
                            )
                        )

                        cables.forEach { cable ->
                            val isSelected = selectedCableId == cable.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCableId = cable.id },
                                label = { Text(cable.name, fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Cable,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentBlue.copy(alpha = 0.2f),
                                    selectedLabelColor = AccentBlue,
                                    containerColor = DarkBackground,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val chosenBrick = powerBricks.find { it.id == selectedBrickId }
                    val chosenCable = cables.find { it.id == selectedCableId }
                    val updated = session.copy(
                        notes = notes.trim(),
                        brickId = selectedBrickId,
                        cableId = selectedCableId,
                        brickName = chosenBrick?.name ?: "Unspecified Charger",
                        cableName = chosenCable?.name ?: "Unspecified Cable"
                    )
                    onSave(updated)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save Changes", color = DarkBackground, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
