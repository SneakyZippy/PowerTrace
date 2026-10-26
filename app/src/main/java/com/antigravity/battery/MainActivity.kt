package com.antigravity.battery

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.antigravity.battery.service.ChargingNotificationHelper
import com.antigravity.battery.ui.AppNavTab
import com.antigravity.battery.ui.MainViewModel
import com.antigravity.battery.ui.components.ReconnectSessionDialog
import com.antigravity.battery.ui.screens.AccessoriesScreen
import com.antigravity.battery.ui.screens.AnalyticsScreen
import com.antigravity.battery.ui.screens.CulpritDetailBottomSheet
import com.antigravity.battery.ui.screens.HistoryScreen
import com.antigravity.battery.ui.screens.OverviewScreen
import com.antigravity.battery.ui.screens.SessionScreen
import com.antigravity.battery.ui.screens.SettingsScreen
import com.antigravity.battery.ui.screens.SetupScreen
import com.antigravity.battery.ui.screens.TagAccessoriesBottomSheet
import com.antigravity.battery.ui.theme.BatteryDiagnosticsTheme
import com.antigravity.battery.ui.theme.DarkBackground
import com.antigravity.battery.ui.theme.DarkSurface
import com.antigravity.battery.ui.theme.GreenPrimary
import com.antigravity.battery.ui.theme.TextSecondary

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                viewModel.refreshPermissions()
            }
        }

    private fun checkAndPromptNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasNotifPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasNotifPermission) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndPromptNotificationPermission()
        handleIntent(intent)

        setContent {
            BatteryDiagnosticsTheme {
                val currentTab by viewModel.currentTab.collectAsState()
                val permissionState by viewModel.permissionState.collectAsState()
                val activeSession by viewModel.activeSession.collectAsState()
                val selectedCulprit by viewModel.selectedCulprit.collectAsState()
                val elapsedTime by viewModel.elapsedTimeFormatted.collectAsState()
                val autoStopUnlock by viewModel.autoStopOnUnlock.collectAsState()
                val autoStopCharger by viewModel.autoStopOnCharger.collectAsState()
                val useMock by viewModel.useMockSource.collectAsState()
                val powerProfile by viewModel.powerProfile.collectAsState()
                val historicalSessions by viewModel.historicalSessions.collectAsState()

                // Continuous timeline data
                val timelineData by viewModel.timelineData.collectAsState()
                val selectedTimeFilter by viewModel.selectedTimeFilter.collectAsState()
                val isContinuousEnabled by viewModel.isContinuousTrackingEnabled.collectAsState()

                // Analytics data
                val analyticsData by viewModel.analyticsData.collectAsState()
                val isAnalyticsLoading by viewModel.isAnalyticsLoading.collectAsState()

                // Accessories & Charging Benchmark data
                val powerBricks by viewModel.powerBricks.collectAsState()
                val cables by viewModel.cables.collectAsState()
                val chargingSessions by viewModel.chargingSessions.collectAsState()
                val liveTelemetry by viewModel.liveChargingTelemetry.collectAsState()
                val pendingSession by viewModel.pendingCompletedSession.collectAsState()
                val reconnectPrompt by viewModel.reconnectPrompt.collectAsState()

                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = DarkBackground,
                    bottomBar = {
                        AppBottomNavigation(
                            currentTab = currentTab,
                            onTabSelected = { viewModel.setTab(it) }
                        )
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (currentTab) {
                            AppNavTab.OVERVIEW -> OverviewScreen(
                                timelineData = timelineData,
                                selectedFilter = selectedTimeFilter,
                                isContinuousEnabled = isContinuousEnabled,
                                onToggleContinuous = { viewModel.toggleContinuousTracking(it) },
                                onSelectFilter = { viewModel.setTimeFilter(it) },
                                onSelectCulprit = { viewModel.selectCulprit(it) }
                            )
                            AppNavTab.ACCESSORIES -> AccessoriesScreen(
                                liveTelemetry = liveTelemetry,
                                powerBricks = powerBricks,
                                cables = cables,
                                chargingSessions = chargingSessions,
                                onSaveBrick = { viewModel.savePowerBrick(it) },
                                onDeleteBrick = { viewModel.deletePowerBrick(it) },
                                onSaveCable = { viewModel.saveCable(it) },
                                onDeleteCable = { viewModel.deleteCable(it) },
                                onDeleteSession = { viewModel.deleteChargingSession(it) },
                                onUpdateSession = { viewModel.updateChargingSession(it) },
                                onManualTagSession = { viewModel.setPendingSessionManual(it) },
                                onSimulateTest = { onComplete ->
                                    viewModel.simulateChargingTest(onComplete)
                                },
                                onStartPolling = { viewModel.startChargingPolling() },
                                onStopPolling = { viewModel.stopChargingPolling() },
                                onFinalizeGraceNow = { viewModel.finalizePausedSessionNow() },
                                getSamplesForSession = { viewModel.getSamplesForSession(it) }
                            )
                            AppNavTab.ANALYTICS -> AnalyticsScreen(
                                analytics = analyticsData,
                                isLoading = isAnalyticsLoading,
                                onRefresh = { viewModel.refreshAnalytics() }
                            )
                            AppNavTab.SETTINGS -> {
                                val showContinuousNotification by viewModel.showContinuousNotification.collectAsState()
                                val isAutoPromptEnabled by viewModel.isAutoPromptEnabled.collectAsState()
                                val minDurationMinutes by viewModel.minDurationMinutes.collectAsState()
                                val minBatteryGain by viewModel.minBatteryGain.collectAsState()
                                val isDisconnectGraceEnabled by viewModel.isDisconnectGraceEnabled.collectAsState()
                                val disconnectGraceSeconds by viewModel.disconnectGraceSeconds.collectAsState()
                                SettingsScreen(
                                    useMockSource = useMock,
                                    onToggleMockSource = { viewModel.setMockSource(it) },
                                    powerProfile = powerProfile,
                                    onUpdateCpuAwakeMa = { viewModel.updateCpuAwakeMa(it) },
                                    onUpdateRadioMa = { viewModel.updateRadioMa(it) },
                                    isContinuousEnabled = isContinuousEnabled,
                                    onToggleContinuous = { viewModel.toggleContinuousTracking(it) },
                                    showContinuousNotification = showContinuousNotification,
                                    onToggleShowNotification = { viewModel.toggleShowContinuousNotification(it) },
                                    isAutoPromptEnabled = isAutoPromptEnabled,
                                    onToggleAutoPrompt = { viewModel.toggleAutoPromptEnabled(it) },
                                    minDurationMinutes = minDurationMinutes,
                                    onUpdateMinDuration = { viewModel.updateMinDurationMinutes(it) },
                                    minBatteryGain = minBatteryGain,
                                    onUpdateMinGain = { viewModel.updateMinBatteryGain(it) },
                                    isDisconnectGraceEnabled = isDisconnectGraceEnabled,
                                    onToggleDisconnectGrace = { viewModel.toggleDisconnectGraceEnabled(it) },
                                    disconnectGraceSeconds = disconnectGraceSeconds,
                                    onUpdateDisconnectGraceSeconds = { viewModel.updateDisconnectGraceSeconds(it) }
                                )
                            }
                            AppNavTab.SETUP -> SetupScreen(
                                permissionState = permissionState,
                                onRefreshPermissions = { viewModel.refreshPermissions() },
                                onContinueToSessions = { viewModel.setTab(AppNavTab.OVERVIEW) }
                            )
                        }

                        // Culprit Drill-down Bottom Sheet
                        if (selectedCulprit != null) {
                            val activeCulprits = timelineData?.topCulprits ?: emptyList()
                            val totalTrackedMah = activeCulprits.sumOf { it.totalEstimatedMah }
                            val culpritIndex = activeCulprits.indexOfFirst { it.uid == selectedCulprit?.uid }
                            val rank = if (culpritIndex >= 0) culpritIndex + 1 else null
                            val totalCount = activeCulprits.size.takeIf { it > 0 }

                            CulpritDetailBottomSheet(
                                culprit = selectedCulprit,
                                sheetState = sheetState,
                                totalDrainMah = totalTrackedMah,
                                rank = rank,
                                totalCulpritsCount = totalCount,
                                onDismiss = { viewModel.selectCulprit(null) }
                            )
                        }

                        // Tag Accessories Bottom Sheet (Triggered on Unplug or Simulation or Finish Test)
                        if (pendingSession != null) {
                            androidx.compose.runtime.key(pendingSession!!.first.id) {
                                TagAccessoriesBottomSheet(
                                    session = pendingSession!!.first,
                                    powerBricks = powerBricks,
                                    cables = cables,
                                    onSave = { brickId, cableId, brickName, cableName, notes ->
                                        viewModel.saveChargingSession(brickId, cableId, brickName, cableName, notes)
                                    },
                                    onSaveNewBrick = { viewModel.savePowerBrick(it) },
                                    onSaveNewCable = { viewModel.saveCable(it) },
                                    onDismiss = { viewModel.clearPendingSession() }
                                )
                            }
                        }

                        // Reconnect Prompt Dialog (Triggered when reconnected within short disconnect grace period)
                        if (reconnectPrompt != null) {
                            ReconnectSessionDialog(
                                prompt = reconnectPrompt!!,
                                onResumeSession = { viewModel.resumePreviousChargingSession() },
                                onStartNewSession = { viewModel.startNewChargingSession() },
                                onDismiss = { viewModel.resumePreviousChargingSession() }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleIntent(it) }
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action == ChargingNotificationHelper.ACTION_TAG_ACCESSORIES) {
            viewModel.setTab(AppNavTab.ACCESSORIES)
            ChargingNotificationHelper.cancelNotification(this)
        } else if (intent.action == ChargingNotificationHelper.ACTION_OPEN_RECONNECT_DIALOG) {
            viewModel.setTab(AppNavTab.ACCESSORIES)
            ChargingNotificationHelper.cancelReconnectNotification(this)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPermissions()
        viewModel.refreshTimelineData()
        viewModel.refreshAnalytics()
    }
}

@Composable
private fun AppBottomNavigation(
    currentTab: AppNavTab,
    onTabSelected: (AppNavTab) -> Unit
) {
    NavigationBar(containerColor = DarkSurface) {
        NavTabItem(
            tab = AppNavTab.OVERVIEW,
            label = "Overview",
            icon = Icons.Default.ShowChart,
            isSelected = currentTab == AppNavTab.OVERVIEW,
            onClick = { onTabSelected(AppNavTab.OVERVIEW) }
        )
        NavTabItem(
            tab = AppNavTab.ACCESSORIES,
            label = "Accessories",
            icon = Icons.Default.Bolt,
            isSelected = currentTab == AppNavTab.ACCESSORIES,
            onClick = { onTabSelected(AppNavTab.ACCESSORIES) }
        )
        NavTabItem(
            tab = AppNavTab.ANALYTICS,
            label = "Analytics",
            icon = Icons.Default.Insights,
            isSelected = currentTab == AppNavTab.ANALYTICS,
            onClick = { onTabSelected(AppNavTab.ANALYTICS) }
        )
        NavTabItem(
            tab = AppNavTab.SETTINGS,
            label = "Settings",
            icon = Icons.Default.Settings,
            isSelected = currentTab == AppNavTab.SETTINGS,
            onClick = { onTabSelected(AppNavTab.SETTINGS) }
        )
        NavTabItem(
            tab = AppNavTab.SETUP,
            label = "Setup",
            icon = Icons.Default.Security,
            isSelected = currentTab == AppNavTab.SETUP,
            onClick = { onTabSelected(AppNavTab.SETUP) }
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.NavTabItem(
    tab: AppNavTab,
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    NavigationBarItem(
        selected = isSelected,
        onClick = onClick,
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = GreenPrimary,
            selectedTextColor = GreenPrimary,
            unselectedIconColor = TextSecondary,
            unselectedTextColor = TextSecondary,
            indicatorColor = DarkSurface
        )
    )
}
