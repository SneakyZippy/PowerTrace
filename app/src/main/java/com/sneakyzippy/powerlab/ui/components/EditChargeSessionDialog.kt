package com.sneakyzippy.powerlab.ui.components

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
import com.sneakyzippy.powerlab.core.model.Cable
import com.sneakyzippy.powerlab.core.model.ChargeTestSession
import com.sneakyzippy.powerlab.core.model.PowerBrick
import com.sneakyzippy.powerlab.ui.theme.AccentBlue
import com.sneakyzippy.powerlab.ui.theme.DarkBackground
import com.sneakyzippy.powerlab.ui.theme.DarkSurface
import com.sneakyzippy.powerlab.ui.theme.DarkSurfaceVariant
import com.sneakyzippy.powerlab.ui.theme.GreenPrimary
import com.sneakyzippy.powerlab.ui.theme.TextPrimary
import com.sneakyzippy.powerlab.ui.theme.TextSecondary

@Composable
fun EditChargeSessionDialog(
    session: ChargeTestSession,
    powerBricks: List<PowerBrick>,
    cables: List<Cable>,
    onDismiss: () -> Unit,
    onSave: (ChargeTestSession) -> Unit
) {
    val parsedInitial = remember(session.notes) { SessionConditionTags.parseNotes(session.notes) }
    var selectedTags by remember { mutableStateOf(parsedInitial.tags) }
    var customNotes by remember { mutableStateOf(parsedInitial.customText) }
    var selectedBrickId by remember { mutableStateOf(session.brickId) }
    var selectedCableId by remember { mutableStateOf(session.cableId) }

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
                // Condition Chips & Optional Custom Note
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Test Conditions",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    ConditionTagChipsSelector(
                        selectedTags = selectedTags,
                        onToggleTag = { tag ->
                            selectedTags = SessionConditionTags.toggleTag(selectedTags, tag)
                        }
                    )
                    OutlinedTextField(
                        value = customNotes,
                        onValueChange = { customNotes = it },
                        placeholder = {
                            Text(
                                "Optional extra note (e.g. desk stand, ambient temp)...",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary.copy(alpha = 0.7f)
                            )
                        },
                        minLines = 1,
                        maxLines = 3,
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
                        notes = SessionConditionTags.formatNotes(selectedTags, customNotes),
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
