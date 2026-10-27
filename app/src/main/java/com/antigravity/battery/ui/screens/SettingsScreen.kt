package com.antigravity.battery.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.antigravity.battery.core.model.PowerProfileData
import com.antigravity.battery.core.permission.PermissionState
import com.antigravity.battery.ui.components.TelemetryGuideBottomSheet
import com.antigravity.battery.ui.screens.settings.AboutAppCard
import com.antigravity.battery.ui.screens.settings.ChargingBenchmarkSettingsCard
import com.antigravity.battery.ui.screens.settings.ContinuousTrackingCard
import com.antigravity.battery.ui.screens.settings.HardwareCalibrationCard
import com.antigravity.battery.ui.screens.settings.PermissionSetupCard
import com.antigravity.battery.ui.screens.settings.TelemetryGuideCard
import com.antigravity.battery.ui.theme.DarkBackground
import com.antigravity.battery.ui.theme.TextPrimary
import com.antigravity.battery.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    useMockSource: Boolean,
    onToggleMockSource: (Boolean) -> Unit,
    powerProfile: PowerProfileData,
    onUpdateCpuAwakeMa: (Double) -> Unit,
    onUpdateRadioMa: (Double) -> Unit,
    isContinuousEnabled: Boolean = false,
    onToggleContinuous: (Boolean) -> Unit = {},
    showContinuousNotification: Boolean = true,
    onToggleShowNotification: (Boolean) -> Unit = {},
    isAutoPromptEnabled: Boolean = true,
    onToggleAutoPrompt: (Boolean) -> Unit = {},
    minDurationMinutes: Int = 5,
    onUpdateMinDuration: (Int) -> Unit = {},
    minBatteryGain: Int = 5,
    onUpdateMinGain: (Int) -> Unit = {},
    isDisconnectGraceEnabled: Boolean = true,
    onToggleDisconnectGrace: (Boolean) -> Unit = {},
    disconnectGraceSeconds: Int = 60,
    onUpdateDisconnectGraceSeconds: (Int) -> Unit = {},
    permissionState: PermissionState? = null,
    onOpenSetup: () -> Unit = {}
) {
    var isGuideOpen by remember { mutableStateOf(false) }
    val guideSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "PowerTrace Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary
        )
        Text(
            text = "System diagnostics and calibration preferences",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ADB Privileges & Permissions Setup Card
        PermissionSetupCard(
            permissionState = permissionState,
            onOpenSetup = onOpenSetup
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 24/7 Background Telemetry Preferences Card
        ContinuousTrackingCard(
            isContinuousEnabled = isContinuousEnabled,
            onToggleContinuous = onToggleContinuous,
            showContinuousNotification = showContinuousNotification,
            onToggleShowNotification = onToggleShowNotification
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Charger & Cable Testing Settings Card
        ChargingBenchmarkSettingsCard(
            isAutoPromptEnabled = isAutoPromptEnabled,
            onToggleAutoPrompt = onToggleAutoPrompt,
            minDurationMinutes = minDurationMinutes,
            onUpdateMinDuration = onUpdateMinDuration,
            minBatteryGain = minBatteryGain,
            onUpdateMinGain = onUpdateMinGain,
            isDisconnectGraceEnabled = isDisconnectGraceEnabled,
            onToggleDisconnectGrace = onToggleDisconnectGrace,
            disconnectGraceSeconds = disconnectGraceSeconds,
            onUpdateDisconnectGraceSeconds = onUpdateDisconnectGraceSeconds
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Device Calibration Status & Advanced Tuning Card
        HardwareCalibrationCard(
            powerProfile = powerProfile,
            onUpdateCpuAwakeMa = onUpdateCpuAwakeMa,
            onUpdateRadioMa = onUpdateRadioMa,
            useMockSource = useMockSource,
            onToggleMockSource = onToggleMockSource
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Telemetry Guide Card
        TelemetryGuideCard(
            onOpenGuide = { isGuideOpen = true }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // System Architecture & About Card
        AboutAppCard()
    }

    if (isGuideOpen) {
        TelemetryGuideBottomSheet(
            sheetState = guideSheetState,
            onDismiss = { isGuideOpen = false }
        )
    }
}
