package com.antigravity.battery.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.antigravity.battery.core.model.Cable
import com.antigravity.battery.core.model.PowerBrick
import com.antigravity.battery.ui.theme.AccentBlue
import com.antigravity.battery.ui.theme.DarkBackground
import com.antigravity.battery.ui.theme.DarkSurface
import com.antigravity.battery.ui.theme.DarkSurfaceVariant
import com.antigravity.battery.ui.theme.GreenPrimary
import com.antigravity.battery.ui.theme.TextPrimary
import com.antigravity.battery.ui.theme.TextSecondary

@Composable
fun AddPowerBrickDialog(
    initialBrick: PowerBrick? = null,
    onDismiss: () -> Unit,
    onConfirm: (PowerBrick) -> Unit
) {
    var name by remember { mutableStateOf(initialBrick?.name ?: "") }
    var manufacturer by remember { mutableStateOf(initialBrick?.manufacturer ?: "") }
    var wattageText by remember {
        mutableStateOf(
            initialBrick?.let {
                if (it.ratedWattage > 0f) {
                    if (it.ratedWattage % 1f == 0f) it.ratedWattage.toInt().toString() else it.ratedWattage.toString()
                } else ""
            } ?: "65"
        )
    }
    var portType by remember { mutableStateOf(initialBrick?.portType ?: "USB-C") }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Text(
                text = if (initialBrick != null) "Edit Power Brick" else "Add Power Brick",
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        showError = false
                    },
                    isError = showError,
                    supportingText = if (showError) { { Text("Charger name is required", color = AccentBlue) } } else null,
                    label = { Text("Charger Name (e.g. Anker 65W GaN)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = GreenPrimary,
                        unfocusedBorderColor = DarkSurfaceVariant,
                        focusedLabelColor = GreenPrimary,
                        unfocusedLabelColor = TextSecondary,
                        cursorColor = GreenPrimary
                    )
                )

                OutlinedTextField(
                    value = manufacturer,
                    onValueChange = { manufacturer = it },
                    label = { Text("Brand / Manufacturer (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = GreenPrimary,
                        unfocusedBorderColor = DarkSurfaceVariant,
                        focusedLabelColor = GreenPrimary,
                        unfocusedLabelColor = TextSecondary,
                        cursorColor = GreenPrimary
                    )
                )

                OutlinedTextField(
                    value = wattageText,
                    onValueChange = { wattageText = it },
                    label = { Text("Rated Wattage (e.g. 65)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = GreenPrimary,
                        unfocusedBorderColor = DarkSurfaceVariant,
                        focusedLabelColor = GreenPrimary,
                        unfocusedLabelColor = TextSecondary,
                        cursorColor = GreenPrimary
                    )
                )

                OutlinedTextField(
                    value = portType,
                    onValueChange = { portType = it },
                    label = { Text("Port Type (e.g. USB-C PD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = GreenPrimary,
                        unfocusedBorderColor = DarkSurfaceVariant,
                        focusedLabelColor = GreenPrimary,
                        unfocusedLabelColor = TextSecondary,
                        cursorColor = GreenPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        showError = true
                    } else {
                        val brick = (initialBrick?.copy(
                            name = name.trim(),
                            manufacturer = manufacturer.trim(),
                            ratedWattage = wattageText.toFloatOrNull() ?: 0f,
                            portType = portType.trim().ifBlank { "USB-C" }
                        ) ?: PowerBrick(
                            name = name.trim(),
                            manufacturer = manufacturer.trim(),
                            ratedWattage = wattageText.toFloatOrNull() ?: 0f,
                            portType = portType.trim().ifBlank { "USB-C" }
                        ))
                        onConfirm(brick)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
            ) {
                Text(
                    text = if (initialBrick != null) "Save Changes" else "Add Brick",
                    color = DarkBackground,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
fun AddCableDialog(
    initialCable: Cable? = null,
    onDismiss: () -> Unit,
    onConfirm: (Cable) -> Unit
) {
    var name by remember { mutableStateOf(initialCable?.name ?: "") }
    var manufacturer by remember { mutableStateOf(initialCable?.manufacturer ?: "") }
    var wattageText by remember {
        mutableStateOf(
            initialCable?.let {
                if (it.ratedWattage > 0f) {
                    if (it.ratedWattage % 1f == 0f) it.ratedWattage.toInt().toString() else it.ratedWattage.toString()
                } else ""
            } ?: "100"
        )
    }
    var lengthText by remember {
        mutableStateOf(
            initialCable?.let {
                if (it.lengthMeters % 1f == 0f) it.lengthMeters.toInt().toString() else it.lengthMeters.toString()
            } ?: "1.0"
        )
    }
    var cableType by remember { mutableStateOf(initialCable?.cableType ?: "Type-C to Type-C") }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Text(
                text = if (initialCable != null) "Edit Charging Cable" else "Add Charging Cable",
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        showError = false
                    },
                    isError = showError,
                    supportingText = if (showError) { { Text("Cable name is required", color = AccentBlue) } } else null,
                    label = { Text("Cable Name (e.g. Braided 100W Cable)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = DarkSurfaceVariant,
                        focusedLabelColor = AccentBlue,
                        unfocusedLabelColor = TextSecondary,
                        cursorColor = AccentBlue
                    )
                )

                OutlinedTextField(
                    value = manufacturer,
                    onValueChange = { manufacturer = it },
                    label = { Text("Brand / Manufacturer (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = DarkSurfaceVariant,
                        focusedLabelColor = AccentBlue,
                        unfocusedLabelColor = TextSecondary,
                        cursorColor = AccentBlue
                    )
                )

                OutlinedTextField(
                    value = wattageText,
                    onValueChange = { wattageText = it },
                    label = { Text("Rated Wattage (e.g. 100)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = DarkSurfaceVariant,
                        focusedLabelColor = AccentBlue,
                        unfocusedLabelColor = TextSecondary,
                        cursorColor = AccentBlue
                    )
                )

                OutlinedTextField(
                    value = lengthText,
                    onValueChange = { lengthText = it },
                    label = { Text("Length in meters (e.g. 1.0 or 2.0)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = DarkSurfaceVariant,
                        focusedLabelColor = AccentBlue,
                        unfocusedLabelColor = TextSecondary,
                        cursorColor = AccentBlue
                    )
                )

                OutlinedTextField(
                    value = cableType,
                    onValueChange = { cableType = it },
                    label = { Text("Cable Type (e.g. Type-C to Type-C)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = DarkSurfaceVariant,
                        focusedLabelColor = AccentBlue,
                        unfocusedLabelColor = TextSecondary,
                        cursorColor = AccentBlue
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        showError = true
                    } else {
                        val cable = (initialCable?.copy(
                            name = name.trim(),
                            manufacturer = manufacturer.trim(),
                            ratedWattage = wattageText.toFloatOrNull() ?: 0f,
                            lengthMeters = lengthText.toFloatOrNull() ?: 1.0f,
                            cableType = cableType.trim().ifBlank { "Type-C to Type-C" }
                        ) ?: Cable(
                            name = name.trim(),
                            manufacturer = manufacturer.trim(),
                            ratedWattage = wattageText.toFloatOrNull() ?: 0f,
                            lengthMeters = lengthText.toFloatOrNull() ?: 1.0f,
                            cableType = cableType.trim().ifBlank { "Type-C to Type-C" }
                        ))
                        onConfirm(cable)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) {
                Text(
                    text = if (initialCable != null) "Save Changes" else "Add Cable",
                    color = DarkBackground,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
