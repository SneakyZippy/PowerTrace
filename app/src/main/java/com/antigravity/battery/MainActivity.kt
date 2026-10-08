package com.antigravity.battery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.antigravity.battery.ui.AppNavTab
import com.antigravity.battery.ui.MainViewModel
import com.antigravity.battery.ui.screens.CulpritDetailBottomSheet
import com.antigravity.battery.ui.screens.HistoryScreen
import com.antigravity.battery.ui.screens.OverviewScreen
import com.antigravity.battery.ui.screens.SessionScreen
import com.antigravity.battery.ui.screens.SettingsScreen
import com.antigravity.battery.ui.screens.SetupScreen
import com.antigravity.battery.ui.theme.BatteryDiagnosticsTheme
import com.antigravity.battery.ui.theme.DarkBackground
import com.antigravity.battery.ui.theme.DarkSurface
import com.antigravity.battery.ui.theme.GreenPrimary
import com.antigravity.battery.ui.theme.TextSecondary

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                            AppNavTab.BENCHMARK -> SessionScreen(
                                activeSession = activeSession,
                                elapsedTimeFormatted = elapsedTime,
                                autoStopOnUnlock = autoStopUnlock,
                                autoStopOnCharger = autoStopCharger,
                                onToggleAutoStopUnlock = { viewModel.toggleAutoStopUnlock(it) },
                                onToggleAutoStopCharger = { viewModel.toggleAutoStopCharger(it) },
                                onStartSession = { viewModel.startBenchmarkSession(it) },
                                onStopSession = { viewModel.stopBenchmarkSession() },
                                onResetSession = { viewModel.resetBenchmarkSession() },
                                onSelectCulprit = { viewModel.selectCulprit(it) }
                            )
                            AppNavTab.HISTORY -> HistoryScreen(
                                sessions = historicalSessions,
                                onSelectSession = { entity ->
                                    viewModel.selectHistoricalSession(entity)
                                },
                                onDeleteSession = { viewModel.deleteSession(it) },
                                onClearAll = { viewModel.clearAllSessions() },
                                onExportMarkdown = { viewModel.exportMarkdown(it) }
                            )
                            AppNavTab.SETTINGS -> SettingsScreen(
                                useMockSource = useMock,
                                onToggleMockSource = { viewModel.setMockSource(it) },
                                powerProfile = powerProfile,
                                onUpdateCpuAwakeMa = { viewModel.updateCpuAwakeMa(it) },
                                onUpdateRadioMa = { viewModel.updateRadioMa(it) }
                            )
                            AppNavTab.SETUP -> SetupScreen(
                                permissionState = permissionState,
                                onRefreshPermissions = { viewModel.refreshPermissions() },
                                onContinueToSessions = { viewModel.setTab(AppNavTab.OVERVIEW) }
                            )
                        }

                        // Culprit Drill-down Bottom Sheet
                        if (selectedCulprit != null) {
                            val activeCulprits = if (currentTab == AppNavTab.BENCHMARK) {
                                activeSession?.delta?.culprits ?: emptyList()
                            } else {
                                timelineData?.topCulprits ?: emptyList()
                            }
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
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPermissions()
        viewModel.refreshTimelineData()
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
            tab = AppNavTab.BENCHMARK,
            label = "Benchmark",
            icon = Icons.Default.Timer,
            isSelected = currentTab == AppNavTab.BENCHMARK,
            onClick = { onTabSelected(AppNavTab.BENCHMARK) }
        )
        NavTabItem(
            tab = AppNavTab.HISTORY,
            label = "History",
            icon = Icons.Default.History,
            isSelected = currentTab == AppNavTab.HISTORY,
            onClick = { onTabSelected(AppNavTab.HISTORY) }
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
