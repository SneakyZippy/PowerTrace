package com.antigravity.battery.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.antigravity.battery.core.model.Cable
import com.antigravity.battery.core.model.ChargeTelemetrySample
import com.antigravity.battery.core.model.ChargeTestSession
import com.antigravity.battery.core.model.LiveChargeTelemetry
import com.antigravity.battery.core.model.PowerBrick
import com.antigravity.battery.ui.components.AddCableDialog
import com.antigravity.battery.ui.components.AddPowerBrickDialog
import com.antigravity.battery.ui.screens.accessories.AccessoryCatalogTab
import com.antigravity.battery.ui.screens.accessories.LeaderboardTab
import com.antigravity.battery.ui.screens.accessories.LiveChargeHudTab
import com.antigravity.battery.ui.theme.DarkBackground
import com.antigravity.battery.ui.theme.DarkSurface
import com.antigravity.battery.ui.theme.GreenPrimary
import com.antigravity.battery.ui.theme.TextPrimary
import com.antigravity.battery.ui.theme.TextSecondary
import kotlinx.coroutines.flow.Flow

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
