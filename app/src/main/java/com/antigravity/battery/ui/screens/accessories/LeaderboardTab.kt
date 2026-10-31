package com.antigravity.battery.ui.screens.accessories

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Power
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.antigravity.battery.core.model.ChargeTestSession
import com.antigravity.battery.ui.theme.AccentBlue
import com.antigravity.battery.ui.theme.CulpritAmber
import com.antigravity.battery.ui.theme.DarkSurface
import com.antigravity.battery.ui.theme.GreenPrimary
import com.antigravity.battery.ui.theme.TextPrimary
import com.antigravity.battery.ui.theme.TextSecondary
import com.antigravity.battery.ui.util.TimeFormatUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LeaderboardTab(
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
                        textAlign = TextAlign.Center
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
                                    text = "${dateFormat.format(Date(session.startTimeMs))} (${TimeFormatUtils.formatTimeRange(session.startTimeMs, session.endTimeMs)}) • ${TimeFormatUtils.formatDurationHuman(session.durationMs)}",
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
                                overflow = TextOverflow.Ellipsis
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
