package com.antigravity.battery.ui.screens.accessories

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Thermostat
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.battery.core.model.ChargeTestSession
import com.antigravity.battery.ui.theme.AccentBlue
import com.antigravity.battery.ui.theme.CulpritAmber
import com.antigravity.battery.ui.theme.DarkSurface
import com.antigravity.battery.ui.theme.DarkSurfaceVariant
import com.antigravity.battery.ui.theme.GreenPrimary
import com.antigravity.battery.ui.theme.TextPrimary
import com.antigravity.battery.ui.theme.TextSecondary
import com.antigravity.battery.ui.util.TimeFormatUtils
import java.util.Locale

@Composable
fun ChargingHistoryTab(
    sessions: List<ChargeTestSession>,
    onSelectSession: (ChargeTestSession) -> Unit,
    onDeleteSession: (ChargeTestSession) -> Unit
) {
    var selectedFilter by remember { mutableStateOf<String?>(null) }

    // Chronological order: newest sessions first
    val sortedSessions = remember(sessions) {
        sessions.sortedByDescending { it.startTimeMs }
    }

    val filteredSessions = remember(sortedSessions, selectedFilter) {
        if (selectedFilter == null) sortedSessions
        else sortedSessions.filter { it.brickName == selectedFilter || it.cableName == selectedFilter }
    }

    // Grouping by day
    val groupedByDate = remember(filteredSessions) {
        filteredSessions.groupBy { TimeFormatUtils.formatDateHeader(it.startTimeMs) }
    }

    val totalGained = remember(filteredSessions) {
        filteredSessions.sumOf { it.batteryGained }
    }

    val avgPeakWattage = remember(filteredSessions) {
        if (filteredSessions.isNotEmpty()) filteredSessions.map { it.peakWattage }.average().toFloat() else 0f
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Summary Card
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(GreenPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.History, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Charging History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("${sessions.size} recorded sessions", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                        }

                        if (avgPeakWattage > 0f) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = String.format(Locale.US, "%.1f W", avgPeakWattage),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = GreenPrimary
                                )
                                Text("Avg Peak", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                        }
                    }

                    if (sessions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Charge Injected: +$totalGained%", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            val totalHours = sessions.sumOf { it.durationMs } / 3_600_000.0
                            Text("Time Connected: ${String.format(Locale.US, "%.1f hrs", totalHours)}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                }
            }
        }

        // Filter chips row
        if (sessions.isNotEmpty()) {
            item {
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

                    allChargers.forEach { charger ->
                        val isSelected = selectedFilter == charger
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilter = if (isSelected) null else charger },
                            label = { Text(charger) },
                            leadingIcon = {
                                Icon(Icons.Default.Power, contentDescription = null, modifier = Modifier.size(14.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GreenPrimary.copy(alpha = 0.2f),
                                selectedLabelColor = GreenPrimary
                            )
                        )
                    }

                    allCables.forEach { cable ->
                        val isSelected = selectedFilter == cable
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilter = if (isSelected) null else cable },
                            label = { Text(cable) },
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

        if (filteredSessions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (sessions.isEmpty()) "No charging sessions recorded yet.\nPlug in your charger to benchmark charging speed and temperatures!"
                        else "No sessions found matching '$selectedFilter'.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Grouped by date headers
            groupedByDate.forEach { (dateHeader, dateSessions) ->
                item {
                    Text(
                        text = dateHeader,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }

                items(dateSessions, key = { it.id }) { session ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectSession(session) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Top Row: Accessories tags & delete button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Brick chip
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(GreenPrimary.copy(alpha = 0.12f))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Power, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(session.brickName, color = GreenPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }

                                    // Cable chip
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(AccentBlue.copy(alpha = 0.12f))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Cable, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(session.cableName, color = AccentBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = { onDeleteSession(session) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = TextSecondary.copy(alpha = 0.5f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Time Range & Duration Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = TimeFormatUtils.formatTimeRange(session.startTimeMs, session.endTimeMs),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(DarkSurfaceVariant)
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = TimeFormatUtils.formatDurationHuman(session.durationMs),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AccentBlue
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Stats Grid: Battery delta, Peak Power, Avg Power, Max Temp
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Battery", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                    Text(
                                        text = "${session.startBatteryLevel}% → ${session.endBatteryLevel}% (+${session.batteryGained}%)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Peak Power", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Bolt, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(14.dp))
                                        Text(
                                            text = String.format(Locale.US, "%.1f W", session.peakWattage),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = GreenPrimary
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Avg Power", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                    Text(
                                        text = String.format(Locale.US, "%.1f W", session.avgWattage),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Max Temp", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Thermostat,
                                            contentDescription = null,
                                            tint = if (session.maxTempCelsius >= 39f) CulpritAmber else TextSecondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = String.format(Locale.US, "%.1f°C", session.maxTempCelsius),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (session.maxTempCelsius >= 39f) CulpritAmber else TextSecondary
                                        )
                                    }
                                }
                            }

                            if (session.notes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "📝 ${session.notes}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (session.isThrottled) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "⚠️ Thermal throttling occurred",
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
}
