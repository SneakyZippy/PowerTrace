package com.antigravity.battery.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.battery.core.model.PowerProfileData
import com.antigravity.battery.ui.theme.CulpritAmber
import com.antigravity.battery.ui.theme.DarkSurface
import com.antigravity.battery.ui.theme.GreenPrimary
import com.antigravity.battery.ui.theme.TextPrimary
import com.antigravity.battery.ui.theme.TextSecondary

@Composable
fun HardwareCalibrationCard(
    powerProfile: PowerProfileData,
    onUpdateCpuAwakeMa: (Double) -> Unit,
    onUpdateRadioMa: (Double) -> Unit,
    useMockSource: Boolean,
    onToggleMockSource: (Boolean) -> Unit
) {
    var isAdvancedExpanded by remember { mutableStateOf(false) }

    Column {
        // Device Calibration Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Hardware Profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text("Calibrated for your device", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(GreenPrimary.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GreenPrimary, modifier = Modifier.height(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("OPTIMAL", color = GreenPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                InfoRow("CPU Idle Coefficient", "${powerProfile.cpuAwakeMa.toInt()} mA (Base awake)")
                InfoRow("Cellular Transceiver", "${powerProfile.radioActiveMa.toInt()} mA (Active)")
                InfoRow("Wi-Fi Transceiver", "${powerProfile.wifiActiveMa.toInt()} mA (Active)")
                InfoRow("Profile Source", powerProfile.sourceDescription)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Advanced Calibration - Collapsed & Locked by Default
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isAdvancedExpanded = !isAdvancedExpanded },
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isAdvancedExpanded) Icons.Default.Tune else Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (isAdvancedExpanded) GreenPrimary else CulpritAmber
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Advanced Calibration (Locked)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = if (isAdvancedExpanded) "Coefficients unlocked for editing" else "Tap to expand manual tuning",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Icon(
                        imageVector = if (isAdvancedExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = TextSecondary
                    )
                }

                AnimatedVisibility(visible = isAdvancedExpanded) {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(CulpritAmber.copy(alpha = 0.1f))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "⚠️ Modifying these values alters the mathematical formulas used to convert active seconds into estimated mAh drain.",
                                color = CulpritAmber,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // CPU Awake Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("CPU Awake Screen-Off Current", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            Text("${powerProfile.cpuAwakeMa.toInt()} mA", fontWeight = FontWeight.Bold, color = GreenPrimary)
                        }
                        Slider(
                            value = powerProfile.cpuAwakeMa.toFloat(),
                            onValueChange = { onUpdateCpuAwakeMa(it.toDouble()) },
                            valueRange = 10f..80f,
                            colors = SliderDefaults.colors(thumbColor = GreenPrimary, activeTrackColor = GreenPrimary)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Radio Active Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Cellular Radio Active Current", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            Text("${powerProfile.radioActiveMa.toInt()} mA", fontWeight = FontWeight.Bold, color = GreenPrimary)
                        }
                        Slider(
                            value = powerProfile.radioActiveMa.toFloat(),
                            onValueChange = { onUpdateRadioMa(it.toDouble()) },
                            valueRange = 50f..300f,
                            colors = SliderDefaults.colors(thumbColor = GreenPrimary, activeTrackColor = GreenPrimary)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Reset to defaults
                        OutlinedButton(
                            onClick = {
                                onUpdateCpuAwakeMa(28.0)
                                onUpdateRadioMa(135.0)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Reset Coefficients to Defaults")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Test Harness Source Switcher
                        Text("Data Source", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = !useMockSource,
                                onClick = { onToggleMockSource(false) },
                                colors = RadioButtonDefaults.colors(selectedColor = GreenPrimary)
                            )
                            Text("Live System Dumpsys (Default)", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = useMockSource,
                                onClick = { onToggleMockSource(true) },
                                colors = RadioButtonDefaults.colors(selectedColor = GreenPrimary)
                            )
                            Text("Mock Simulation Harness (Test)", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
