package com.antigravity.battery.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import com.antigravity.battery.core.model.ChargeTelemetrySample
import kotlinx.coroutines.flow.Flow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.battery.core.model.Cable
import com.antigravity.battery.core.model.ChargeTestSession
import com.antigravity.battery.core.model.LiveChargeTelemetry
import com.antigravity.battery.core.model.PowerBrick
import com.antigravity.battery.ui.components.AddCableDialog
import com.antigravity.battery.ui.components.AddPowerBrickDialog
import com.antigravity.battery.ui.theme.AccentBlue
import com.antigravity.battery.ui.theme.CulpritAmber
import com.antigravity.battery.ui.theme.DarkBackground
import com.antigravity.battery.ui.theme.DarkSurface
import com.antigravity.battery.ui.theme.GreenPrimary
import com.antigravity.battery.ui.theme.PurpleAccent
import com.antigravity.battery.ui.theme.TextPrimary
import com.antigravity.battery.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessoriesScreen(
    liveTelemetry: LiveChargeTelemetry,
    powerBricks: List<PowerBrick>,
    cables: List<Cable>,
    chargingSessions: List<ChargeTestSession>,
    onSaveBrick: (PowerBrick) -> Unit,
    onDeleteBrick: (PowerBrick) -> Unit,
    onSaveCable: (Cable) -> Unit,
    onDeleteCable: (Cable) -> Unit,
    onDeleteSession: (ChargeTestSession) -> Unit,
    onUpdateSession: (ChargeTestSession) -> Unit = {},
    onManualTagSession: (ChargeTestSession) -> Unit,
    onSimulateTest: (onComplete: () -> Unit) -> Unit,
    onStartPolling: () -> Unit = {},
    onStopPolling: () -> Unit = {},
    onFinalizeGraceNow: () -> Unit = {},
    getSamplesForSession: (String) -> Flow<List<ChargeTelemetrySample>> = { kotlinx.coroutines.flow.emptyFlow() }
) {
    DisposableEffect(Unit) {
        onStartPolling()
        onDispose {
            onStopPolling()
        }
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Live HUD", "Catalog", "Leaderboard")

    var showAddBrickDialog by remember { mutableStateOf(false) }
    var showAddCableDialog by remember { mutableStateOf(false) }
    var isSimulating by remember { mutableStateOf(false) }

    var selectedHudBrickId by remember { mutableStateOf<String?>(null) }
    var selectedHudCableId by remember { mutableStateOf<String?>(null) }

    var selectedSessionForDetail by remember { mutableStateOf<ChargeTestSession?>(null) }
    val detailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Box(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Screen Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Accessories & Benchmarks",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Measure real-time wattage and benchmark chargers & cables",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            // Tabs
            SecondaryTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = DarkSurface,
                contentColor = GreenPrimary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTabIndex == index) GreenPrimary else TextSecondary
                            )
                        }
                    )
                }
            }

            // Tab Content
            when (selectedTabIndex) {
                0 -> LiveChargeHudTab(
                    telemetry = liveTelemetry,
                    isSimulating = isSimulating,
                    powerBricks = powerBricks,
                    cables = cables,
                    selectedBrickId = selectedHudBrickId,
                    selectedCableId = selectedHudCableId,
                    onSelectBrick = { selectedHudBrickId = it },
                    onSelectCable = { selectedHudCableId = it },
                    onAddNewBrick = { showAddBrickDialog = true },
                    onAddNewCable = { showAddCableDialog = true },
                    onFinalizeGraceNow = onFinalizeGraceNow,
                    onSimulate = {
                        isSimulating = true
                        onSimulateTest {
                            isSimulating = false
                        }
                    },
                    onSaveCurrent = {
                        val chosenBrick = powerBricks.find { it.id == selectedHudBrickId }
                        val chosenCable = cables.find { it.id == selectedHudCableId }

                        val session = ChargeTestSession(
                            brickId = selectedHudBrickId,
                            cableId = selectedHudCableId,
                            brickName = chosenBrick?.name ?: "Unspecified Charger",
                            cableName = chosenCable?.name ?: "Unspecified Cable",
                            startTimeMs = System.currentTimeMillis() - liveTelemetry.sessionDurationMs,
                            endTimeMs = System.currentTimeMillis(),
                            startBatteryLevel = (liveTelemetry.batteryLevelPercent - 5).coerceAtLeast(0),
                            endBatteryLevel = liveTelemetry.batteryLevelPercent,
                            peakWattage = liveTelemetry.peakWattageSoFar,
                            avgWattage = liveTelemetry.avgWattageSoFar,
                            minVoltageMv = liveTelemetry.voltageMv,
                            maxVoltageMv = liveTelemetry.voltageMv,
                            peakCurrentMa = if (liveTelemetry.currentMa > 0) liveTelemetry.currentMa else 0,
                            startTempDeciC = liveTelemetry.tempDeciCelsius,
                            maxTempDeciC = liveTelemetry.tempDeciCelsius,
                            isThrottled = liveTelemetry.isThrottlingDetected
                        )
                        onManualTagSession(session)
                    }
                )
                1 -> AccessoryCatalogTab(
                    powerBricks = powerBricks,
                    cables = cables,
                    sessions = chargingSessions,
                    onAddBrick = { showAddBrickDialog = true },
                    onDeleteBrick = onDeleteBrick,
                    onAddCable = { showAddCableDialog = true },
                    onDeleteCable = onDeleteCable
                )
                2 -> LeaderboardTab(
                    sessions = chargingSessions,
                    onSelectSession = { selectedSessionForDetail = it },
                    onDeleteSession = onDeleteSession
                )
            }
        }

        // Session Detail Curve Bottom Sheet
        if (selectedSessionForDetail != null) {
            ChargeSessionDetailBottomSheet(
                session = selectedSessionForDetail!!,
                sheetState = detailSheetState,
                samplesFlow = getSamplesForSession(selectedSessionForDetail!!.id),
                powerBricks = powerBricks,
                cables = cables,
                onUpdateSession = { updated ->
                    selectedSessionForDetail = updated
                    onUpdateSession(updated)
                },
                onDelete = {
                    onDeleteSession(it)
                    selectedSessionForDetail = null
                },
                onDismiss = { selectedSessionForDetail = null }
            )
        }

        // Add Brick Dialog
        if (showAddBrickDialog) {
            AddPowerBrickDialog(
                onDismiss = { showAddBrickDialog = false },
                onConfirm = { brick ->
                    onSaveBrick(brick)
                    selectedHudBrickId = brick.id
                    showAddBrickDialog = false
                }
            )
        }

        // Add Cable Dialog
        if (showAddCableDialog) {
            AddCableDialog(
                onDismiss = { showAddCableDialog = false },
                onConfirm = { cable ->
                    onSaveCable(cable)
                    selectedHudCableId = cable.id
                    showAddCableDialog = false
                }
            )
        }
    }
}

// ==========================================
// 1. LIVE HUD TAB
// ==========================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LiveChargeHudTab(
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
                            telemetry.isGracePeriodActive -> "DISCONNECTED (GRACE: ${telemetry.gracePeriodRemainingSeconds}S)"
                            else -> "CHARGER DISCONNECTED (ON BATTERY)"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
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
                    androidx.compose.material3.OutlinedButton(
                        onClick = onFinalizeGraceNow,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = CulpritAmber)
                    ) {
                        Text("Finish Now", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
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
                        fontWeight = FontWeight.ExtraBold,
                        color = gaugeColor
                    )

                    Text(
                        text = "WATTS",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
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
                            fontWeight = FontWeight.Bold,
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
                            fontWeight = FontWeight.Bold,
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
                                    fontWeight = FontWeight.Bold,
                                    color = GreenPrimary
                                )
                            }
                            Column {
                                Text("Avg Power", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(
                                    text = String.format(Locale.US, "%.1f W", telemetry.avgWattageSoFar),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            Column {
                                Text("Duration", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(
                                    text = durationFormatted,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
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
                        Text("Finish & Tag This Test", color = DarkBackground, fontWeight = FontWeight.Bold)
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

@Composable
private fun MetricHudCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
    }
}

// ==========================================
// 2. CATALOG TAB
// ==========================================
@Composable
private fun AccessoryCatalogTab(
    powerBricks: List<PowerBrick>,
    cables: List<Cable>,
    sessions: List<ChargeTestSession>,
    onAddBrick: () -> Unit,
    onDeleteBrick: (PowerBrick) -> Unit,
    onAddCable: () -> Unit,
    onDeleteCable: (Cable) -> Unit
) {
    var subTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Sub-toggle: Bricks vs Cables
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurface)
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (subTab == 0) GreenPrimary else Color.Transparent)
                    .clickable { subTab = 0 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Power Bricks (${powerBricks.size})",
                    color = if (subTab == 0) DarkBackground else TextSecondary,
                    fontWeight = FontWeight.Bold
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (subTab == 1) AccentBlue else Color.Transparent)
                    .clickable { subTab = 1 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Cables (${cables.size})",
                    color = if (subTab == 1) DarkBackground else TextSecondary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (subTab == 0) {
            // Power Bricks List
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Saved Chargers", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                Button(
                    onClick = onAddBrick,
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = DarkBackground, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Brick", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (powerBricks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No power bricks saved yet. Tap 'Add Brick' to create one.", color = TextSecondary)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(powerBricks) { brick ->
                        val brickSessions = sessions.filter { it.brickId == brick.id }
                        val maxW = brickSessions.maxOfOrNull { it.peakWattage } ?: 0f

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(GreenPrimary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Power, contentDescription = null, tint = GreenPrimary)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(brick.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text(
                                        text = "${if (brick.manufacturer.isNotBlank()) "${brick.manufacturer} • " else ""}${brick.ratedWattage.toInt()}W (${brick.portType})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (maxW > 0f) "Highest tested: ${String.format(Locale.US, "%.1f W", maxW)} (${brickSessions.size} tests)" else "No tests recorded yet",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (maxW > 0f) GreenPrimary else TextSecondary
                                    )
                                }
                                IconButton(onClick = { onDeleteBrick(brick) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextSecondary.copy(alpha = 0.6f))
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Cables List
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Saved Cables", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                Button(
                    onClick = onAddCable,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = DarkBackground, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Cable", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (cables.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No cables saved yet. Tap 'Add Cable' to create one.", color = TextSecondary)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(cables) { cable ->
                        val cableSessions = sessions.filter { it.cableId == cable.id }
                        val maxW = cableSessions.maxOfOrNull { it.peakWattage } ?: 0f

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(AccentBlue.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Cable, contentDescription = null, tint = AccentBlue)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(cable.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text(
                                        text = "${if (cable.manufacturer.isNotBlank()) "${cable.manufacturer} • " else ""}${cable.lengthMeters}m (${cable.cableType})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (maxW > 0f) "Highest tested: ${String.format(Locale.US, "%.1f W", maxW)} (${cableSessions.size} tests)" else "No tests recorded yet",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (maxW > 0f) AccentBlue else TextSecondary
                                    )
                                }
                                IconButton(onClick = { onDeleteCable(cable) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextSecondary.copy(alpha = 0.6f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. LEADERBOARD TAB
// ==========================================
@Composable
private fun LeaderboardTab(
    sessions: List<ChargeTestSession>,
    onSelectSession: (ChargeTestSession) -> Unit,
    onDeleteSession: (ChargeTestSession) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    var selectedFilter by remember { mutableStateOf<String?>(null) }

    val topBrickSession = sessions.maxByOrNull { it.peakWattage }
    val topBrickName = topBrickSession?.brickName ?: "None yet"
    val topBrickWattage = topBrickSession?.peakWattage ?: 0f

    val topCableSession = sessions.maxByOrNull { it.peakWattage }
    val topCableName = topCableSession?.cableName ?: "None yet"
    val topCableWattage = topCableSession?.peakWattage ?: 0f

    val filteredSessions = remember(sessions, selectedFilter) {
        if (selectedFilter == null) sessions
        else sessions.filter { it.brickName == selectedFilter || it.cableName == selectedFilter }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // High Score / Top Performer Banner
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Fastest Charger", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (topBrickWattage > 0) String.format(Locale.US, "%.1f W", topBrickWattage) else "--",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = GreenPrimary
                        )
                        Text(
                            text = topBrickName,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            maxLines = 1
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Fastest Cable", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (topCableWattage > 0) String.format(Locale.US, "%.1f W", topCableWattage) else "--",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = AccentBlue
                        )
                        Text(
                            text = topCableName,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Sessions Header & Filter Chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Head-to-Head Test Results (${filteredSessions.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                if (sessions.isNotEmpty()) {
                    val allChargers = remember(sessions) { sessions.map { it.brickName }.distinct() }
                    val allCables = remember(sessions) { sessions.map { it.cableName }.distinct() }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedFilter == null,
                            onClick = { selectedFilter = null },
                            label = { Text("All (${sessions.size})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GreenPrimary.copy(alpha = 0.2f),
                                selectedLabelColor = GreenPrimary
                            )
                        )

                        allChargers.forEach { chargerName ->
                            val isSelected = selectedFilter == chargerName
                            val count = sessions.count { it.brickName == chargerName }
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedFilter = if (isSelected) null else chargerName },
                                label = { Text("$chargerName ($count)") },
                                leadingIcon = {
                                    Icon(Icons.Default.Power, contentDescription = null, modifier = Modifier.size(14.dp))
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GreenPrimary.copy(alpha = 0.2f),
                                    selectedLabelColor = GreenPrimary
                                )
                            )
                        }

                        allCables.forEach { cableName ->
                            val isSelected = selectedFilter == cableName
                            val count = sessions.count { it.cableName == cableName }
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedFilter = if (isSelected) null else cableName },
                                label = { Text("$cableName ($count)") },
                                leadingIcon = {
                                    Icon(Icons.Default.Cable, contentDescription = null, modifier = Modifier.size(14.dp))
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

        if (filteredSessions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (sessions.isEmpty()) "No charging tests saved yet.\nConnect a charger to test or click 'Simulate Fast-Charge Test'."
                        else "No test results matching '$selectedFilter'.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            items(filteredSessions.sortedByDescending { it.peakWattage }) { session ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectSession(session) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${session.brickName} + ${session.cableName}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${dateFormat.format(Date(session.startTimeMs))} • Tap for curve graph",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = String.format(Locale.US, "%.1f W", session.peakWattage),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = GreenPrimary
                                )
                                Text(
                                    text = String.format(Locale.US, "Avg %.1f W", session.avgWattage),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Stats Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Battery: ${session.startBatteryLevel}% → ${session.endBatteryLevel}% (+${session.batteryGained}%)",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )

                            Text(
                                text = "Max Temp: ${String.format(Locale.US, "%.1f°C", session.maxTempCelsius)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (session.maxTempCelsius >= 39f) CulpritAmber else TextSecondary
                            )

                            IconButton(
                                onClick = { onDeleteSession(session) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = TextSecondary.copy(alpha = 0.5f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        if (session.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "📝 ${session.notes}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }

                        if (session.isThrottled) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "⚠️ Thermal throttling detected during test",
                                style = MaterialTheme.typography.labelSmall,
                                color = CulpritAmber
                            )
                        }
                    }
                }
            }
        }
    }
}
