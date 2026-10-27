package com.antigravity.battery.ui.screens.accessories

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.battery.core.model.Cable
import com.antigravity.battery.core.model.LiveChargeTelemetry
import com.antigravity.battery.core.model.PowerBrick
import com.antigravity.battery.ui.theme.AccentBlue
import com.antigravity.battery.ui.theme.CulpritAmber
import com.antigravity.battery.ui.theme.DarkBackground
import com.antigravity.battery.ui.theme.DarkSurface
import com.antigravity.battery.ui.theme.GreenPrimary
import com.antigravity.battery.ui.theme.PurpleAccent
import com.antigravity.battery.ui.theme.TextPrimary
import com.antigravity.battery.ui.theme.TextSecondary
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LiveChargeHudTab(
    telemetry: LiveChargeTelemetry,
    isSimulating: Boolean,
    powerBricks: List<PowerBrick>,
    cables: List<Cable>,
    selectedBrickId: String?,
    selectedCableId: String?,
    onSelectBrick: (String) -> Unit,
    onSelectCable: (String) -> Unit,
    onAddNewBrick: () -> Unit,
    onAddNewCable: () -> Unit,
    onSimulate: () -> Unit,
    onSaveCurrent: () -> Unit,
    onFinalizeGraceNow: () -> Unit = {}
) {
    val durationSec = (telemetry.sessionDurationMs / 1000L)
    val durationFormatted = String.format("%02d:%02d", durationSec / 60, durationSec % 60)

    val gaugeColor by animateColorAsState(
        targetValue = when {
            !telemetry.isCharging -> TextSecondary
            telemetry.currentWattage >= 30f -> GreenPrimary
            telemetry.currentWattage >= 18f -> AccentBlue
            telemetry.currentWattage >= 10f -> CulpritAmber
            else -> TextSecondary
        },
        label = "gaugeColor"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Charging Status Badge
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        when {
                            telemetry.isCharging -> GreenPrimary.copy(alpha = 0.15f)
                            telemetry.isGracePeriodActive -> CulpritAmber.copy(alpha = 0.15f)
                            else -> DarkSurface
                        }
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                telemetry.isCharging -> GreenPrimary
                                telemetry.isGracePeriodActive -> CulpritAmber
                                else -> TextSecondary
                            }
                        )
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            telemetry.isCharging -> "CONNECTED: ${telemetry.chargePlugType.uppercase()}"
                            telemetry.isGracePeriodActive -> "CHARGER DISCONNECTED (RESUME: ${telemetry.gracePeriodRemainingSeconds}S)"
                            else -> "CHARGER DISCONNECTED (ON BATTERY)"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = when {
                            telemetry.isCharging -> GreenPrimary
                            telemetry.isGracePeriodActive -> CulpritAmber
                            else -> TextSecondary
                        }
                    )
                    if (telemetry.isGracePeriodActive) {
                        Text(
                            text = "Reconnect now to resume session",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
                if (telemetry.isGracePeriodActive) {
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = onFinalizeGraceNow,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CulpritAmber)
                    ) {
                        Text("Finish Now", fontSize = 11.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                    }
                }
            }
        }

        // Giant Wattage Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (telemetry.isCharging) "LIVE CHARGING POWER" else "DISCONNECTED",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (telemetry.isCharging) String.format(Locale.US, "%.1f", telemetry.currentWattage) else "0.0",
                        fontSize = 64.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                        color = gaugeColor
                    )

                    Text(
                        text = "WATTS",
                        fontSize = 18.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = gaugeColor
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = when {
                            !telemetry.isCharging -> "Connect charger & cable to begin live test"
                            telemetry.currentWattage >= 30f -> "Ultra Fast Charging (USB-PD / SuperCharge)"
                            telemetry.currentWattage >= 18f -> "Fast Charging"
                            telemetry.currentWattage >= 10f -> "Standard Charging"
                            telemetry.currentWattage > 0f -> "Trickle / Battery Near Full"
                            else -> "Plugged in (low input / maintaining)"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        // Test Setup (Charger & Cable) Selector Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Test Setup (Charger & Cable)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Brick row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Charger:", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        TextButton(onClick = onAddNewBrick) {
                            Text("+ Add Brick", color = GreenPrimary, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        powerBricks.forEach { brick ->
                            val isSelected = selectedBrickId == brick.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectBrick(brick.id) },
                                label = { Text(brick.name) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Power,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GreenPrimary.copy(alpha = 0.2f),
                                    selectedLabelColor = GreenPrimary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Cable row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Cable:", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        TextButton(onClick = onAddNewCable) {
                            Text("+ Add Cable", color = AccentBlue, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        cables.forEach { cable ->
                            val isSelected = selectedCableId == cable.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectCable(cable.id) },
                                label = { Text(cable.name) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Cable,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentBlue.copy(alpha = 0.2f),
                                    selectedLabelColor = AccentBlue
                                )
                            )
                        }
                    }
                }
            }
        }

        // 4-Quadrant Metric Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricHudCard(
                    modifier = Modifier.weight(1f),
                    title = "VOLTAGE",
                    value = String.format(Locale.US, "%.2f V", telemetry.voltageV),
                    subtitle = "${telemetry.voltageMv} mV",
                    icon = Icons.Default.FlashOn,
                    color = AccentBlue
                )

                val isDischarging = !telemetry.isCharging || telemetry.currentMa <= 0
                MetricHudCard(
                    modifier = Modifier.weight(1f),
                    title = "CURRENT",
                    value = if (telemetry.isCharging && telemetry.currentMa > 0) "+${telemetry.currentMa} mA" else "${telemetry.currentMa} mA",
                    subtitle = if (!isDischarging) "Charging to battery" else "Discharge rate",
                    icon = Icons.Default.Speed,
                    color = if (!isDischarging) GreenPrimary else TextSecondary
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricHudCard(
                    modifier = Modifier.weight(1f),
                    title = "BATTERY",
                    value = "${telemetry.batteryLevelPercent}%",
                    subtitle = if (telemetry.isCharging) "Charging" else "On Battery",
                    icon = Icons.Default.Bolt,
                    color = PurpleAccent
                )
                MetricHudCard(
                    modifier = Modifier.weight(1f),
                    title = "TEMPERATURE",
                    value = String.format(Locale.US, "%.1f °C", telemetry.tempCelsius),
                    subtitle = if (telemetry.tempCelsius >= 39f) "Hot" else "Normal",
                    icon = Icons.Default.Thermostat,
                    color = if (telemetry.tempCelsius >= 39f) CulpritAmber else TextPrimary
                )
            }
        }

        // Live Benchmark Summary Card (ONLY active during real charging)
        if (telemetry.isCharging && telemetry.peakWattageSoFar > 0f) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Current Charge Test Stats",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Peak Power", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(
                                    text = String.format(Locale.US, "%.1f W", telemetry.peakWattageSoFar),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    color = GreenPrimary
                                )
                            }
                            Column {
                                Text("Avg Power", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(
                                    text = String.format(Locale.US, "%.1f W", telemetry.avgWattageSoFar),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            Column {
                                Text("Duration", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(
                                    text = durationFormatted,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }

                        if (telemetry.isThrottlingDetected) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CulpritAmber.copy(alpha = 0.15f))
                                    .padding(8.dp)
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = CulpritAmber, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Thermal throttling: wattage decreased due to battery temperature",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CulpritAmber
                                )
                            }
                        }
                    }
                }
            }
        }

        // Action Buttons
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Finish & Tag Button: ONLY when phone is actively charging!
                if (telemetry.isCharging) {
                    Button(
                        onClick = onSaveCurrent,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, tint = DarkBackground)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Finish & Tag This Test", color = DarkBackground, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    }
                }

                // Demo Simulation Button
                OutlinedButton(
                    onClick = onSimulate,
                    enabled = !isSimulating,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentBlue)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isSimulating) "Simulating Fast Charge..." else "Simulate Fast-Charge Test (Demo)")
                }
            }
        }
    }
}
