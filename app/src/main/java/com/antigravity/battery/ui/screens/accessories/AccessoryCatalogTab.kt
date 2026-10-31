package com.antigravity.battery.ui.screens.accessories

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Power
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.battery.core.model.Cable
import com.antigravity.battery.core.model.ChargeTestSession
import com.antigravity.battery.core.model.PowerBrick
import com.antigravity.battery.ui.theme.AccentBlue
import com.antigravity.battery.ui.theme.DarkBackground
import com.antigravity.battery.ui.theme.DarkSurface
import com.antigravity.battery.ui.theme.DarkSurfaceVariant
import com.antigravity.battery.ui.theme.GreenPrimary
import com.antigravity.battery.ui.theme.TextPrimary
import com.antigravity.battery.ui.theme.TextSecondary
import com.antigravity.battery.ui.util.TimeFormatUtils
import java.util.Locale

@Composable
fun AccessoryCatalogTab(
    powerBricks: List<PowerBrick>,
    cables: List<Cable>,
    sessions: List<ChargeTestSession>,
    onAddBrick: () -> Unit,
    onEditBrick: (PowerBrick) -> Unit,
    onDeleteBrick: (PowerBrick) -> Unit,
    onAddCable: () -> Unit,
    onEditCable: (Cable) -> Unit,
    onDeleteCable: (Cable) -> Unit,
    onSelectSession: (ChargeTestSession) -> Unit = {}
) {
    var subTab by remember { mutableIntStateOf(0) }
    val expandedBricks = remember { mutableStateMapOf<String, Boolean>() }
    val expandedCables = remember { mutableStateMapOf<String, Boolean>() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Sub-toggle: Bricks vs Cables vs Pairings
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
                    text = "Chargers (${powerBricks.size})",
                    color = if (subTab == 0) DarkBackground else TextSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
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
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (subTab == 2) GreenPrimary.copy(alpha = 0.85f) else Color.Transparent)
                    .clickable { subTab = 2 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                val distinctCombosCount = remember(sessions) {
                    sessions.map { Pair(it.brickName, it.cableName) }.distinct().size
                }
                Text(
                    text = "Pairings ($distinctCombosCount)",
                    color = if (subTab == 2) DarkBackground else TextSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (subTab) {
            0 -> {
                // Chargers Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Power Adapters & Bricks", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text("Tap any charger to see all tested cable pairings", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                    Button(
                        onClick = onAddBrick,
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = DarkBackground, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (powerBricks.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                        Text("No power bricks saved yet. Tap 'Add' to create one.", color = TextSecondary)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(powerBricks, key = { it.id }) { brick ->
                            val brickSessions = sessions.filter { it.brickId == brick.id || it.brickName == brick.name }
                            val maxW = brickSessions.maxOfOrNull { it.peakWattage } ?: 0f

                            // Group by cable to show all combinations
                            val cableCombinations = remember(brickSessions) {
                                brickSessions.groupBy { it.cableName }.map { (cableName, list) ->
                                    val bestSession = list.maxByOrNull { it.peakWattage }!!
                                    Triple(cableName, list, bestSession)
                                }.sortedByDescending { it.third.peakWattage }
                            }

                            val isExpanded = expandedBricks[brick.id] ?: true

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expandedBricks[brick.id] = !isExpanded },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkSurface)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    // Main Brick Info
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
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
                                        }

                                        IconButton(onClick = { onEditBrick(brick) }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TextSecondary.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(onClick = { onDeleteBrick(brick) }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextSecondary.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                                        }
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            tint = TextSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Summary row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (maxW > 0f) "Highest tested: ${String.format(Locale.US, "%.1f W", maxW)}" else "No tests recorded yet",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (maxW > 0f) GreenPrimary else TextSecondary
                                        )
                                        Text(
                                            text = "${cableCombinations.size} cable pairing(s) • ${brickSessions.size} test(s)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary
                                        )
                                    }

                                    // Expandable Combinations Section
                                    AnimatedVisibility(visible = isExpanded) {
                                        Column(modifier = Modifier.padding(top = 12.dp)) {
                                            Text(
                                                text = "TESTED CABLE COMBINATIONS",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = GreenPrimary,
                                                fontSize = 11.sp
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))

                                            if (cableCombinations.isEmpty()) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(DarkSurfaceVariant)
                                                        .padding(10.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        "No cable pairings tested yet for this charger. Connect and run a test to populate combinations!",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = TextSecondary
                                                    )
                                                }
                                            } else {
                                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    cableCombinations.forEachIndexed { index, (cableName, pairSessions, bestSession) ->
                                                        val isTopPair = index == 0 && cableCombinations.size > 1
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(DarkSurfaceVariant)
                                                                .clickable { onSelectSession(bestSession) }
                                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                                Icon(Icons.Default.Cable, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                                                                Spacer(modifier = Modifier.width(8.dp))
                                                                Column {
                                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                                        Text(
                                                                            text = cableName,
                                                                            style = MaterialTheme.typography.bodyMedium,
                                                                            fontWeight = FontWeight.SemiBold,
                                                                            color = TextPrimary
                                                                        )
                                                                        if (isTopPair) {
                                                                            Spacer(modifier = Modifier.width(6.dp))
                                                                            Icon(Icons.Default.EmojiEvents, contentDescription = "Best combo", tint = GreenPrimary, modifier = Modifier.size(14.dp))
                                                                        }
                                                                    }
                                                                    Text(
                                                                        text = "${pairSessions.size} test(s) • Last: ${TimeFormatUtils.formatDateHeader(bestSession.startTimeMs)}",
                                                                        style = MaterialTheme.typography.labelSmall,
                                                                        color = TextSecondary
                                                                    )
                                                                }
                                                            }

                                                            Column(horizontalAlignment = Alignment.End) {
                                                                Text(
                                                                    text = String.format(Locale.US, "%.1f W", bestSession.peakWattage),
                                                                    style = MaterialTheme.typography.titleMedium,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = GreenPrimary
                                                                )
                                                                Text(
                                                                    text = String.format(Locale.US, "Avg %.1f W", bestSession.avgWattage),
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = TextSecondary
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                // Cables Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("USB & Charging Cables", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text("Tap any cable to see all tested charger pairings", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                    Button(
                        onClick = onAddCable,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = DarkBackground, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (cables.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                        Text("No cables saved yet. Tap 'Add' to create one.", color = TextSecondary)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(cables, key = { it.id }) { cable ->
                            val cableSessions = sessions.filter { it.cableId == cable.id || it.cableName == cable.name }
                            val maxW = cableSessions.maxOfOrNull { it.peakWattage } ?: 0f

                            // Group by charger to show all combinations
                            val chargerCombinations = remember(cableSessions) {
                                cableSessions.groupBy { it.brickName }.map { (brickName, list) ->
                                    val bestSession = list.maxByOrNull { it.peakWattage }!!
                                    Triple(brickName, list, bestSession)
                                }.sortedByDescending { it.third.peakWattage }
                            }

                            val isExpanded = expandedCables[cable.id] ?: true

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expandedCables[cable.id] = !isExpanded },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkSurface)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    // Main Cable Info
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
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
                                        }

                                        IconButton(onClick = { onEditCable(cable) }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TextSecondary.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(onClick = { onDeleteCable(cable) }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextSecondary.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                                        }
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            tint = TextSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Summary row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (maxW > 0f) "Highest tested: ${String.format(Locale.US, "%.1f W", maxW)}" else "No tests recorded yet",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (maxW > 0f) AccentBlue else TextSecondary
                                        )
                                        Text(
                                            text = "${chargerCombinations.size} charger pairing(s) • ${cableSessions.size} test(s)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary
                                        )
                                    }

                                    // Expandable Combinations Section
                                    AnimatedVisibility(visible = isExpanded) {
                                        Column(modifier = Modifier.padding(top = 12.dp)) {
                                            Text(
                                                text = "TESTED CHARGER COMBINATIONS",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = AccentBlue,
                                                fontSize = 11.sp
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))

                                            if (chargerCombinations.isEmpty()) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(DarkSurfaceVariant)
                                                        .padding(10.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        "No charger pairings tested yet for this cable. Connect and run a test to populate combinations!",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = TextSecondary
                                                    )
                                                }
                                            } else {
                                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    chargerCombinations.forEachIndexed { index, (brickName, pairSessions, bestSession) ->
                                                        val isTopPair = index == 0 && chargerCombinations.size > 1
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(DarkSurfaceVariant)
                                                                .clickable { onSelectSession(bestSession) }
                                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                                Icon(Icons.Default.Power, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(16.dp))
                                                                Spacer(modifier = Modifier.width(8.dp))
                                                                Column {
                                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                                        Text(
                                                                            text = brickName,
                                                                            style = MaterialTheme.typography.bodyMedium,
                                                                            fontWeight = FontWeight.SemiBold,
                                                                            color = TextPrimary
                                                                        )
                                                                        if (isTopPair) {
                                                                            Spacer(modifier = Modifier.width(6.dp))
                                                                            Icon(Icons.Default.EmojiEvents, contentDescription = "Best combo", tint = GreenPrimary, modifier = Modifier.size(14.dp))
                                                                        }
                                                                    }
                                                                    Text(
                                                                        text = "${pairSessions.size} test(s) • Last: ${TimeFormatUtils.formatDateHeader(bestSession.startTimeMs)}",
                                                                        style = MaterialTheme.typography.labelSmall,
                                                                        color = TextSecondary
                                                                    )
                                                                }
                                                            }

                                                            Column(horizontalAlignment = Alignment.End) {
                                                                Text(
                                                                    text = String.format(Locale.US, "%.1f W", bestSession.peakWattage),
                                                                    style = MaterialTheme.typography.titleMedium,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = GreenPrimary
                                                                )
                                                                Text(
                                                                    text = String.format(Locale.US, "Avg %.1f W", bestSession.avgWattage),
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = TextSecondary
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                // All Pairings / Head-to-Head Combinations View
                Column(modifier = Modifier.fillMaxSize()) {
                    Text("All Head-to-Head Pairings", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text("Ranked by maximum tested wattage for each charger + cable combination", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Spacer(modifier = Modifier.height(12.dp))

                    val allCombinations = remember(sessions) {
                        sessions.groupBy { Pair(it.brickName, it.cableName) }.map { (pair, list) ->
                            val bestSession = list.maxByOrNull { it.peakWattage }!!
                            Triple(pair, list, bestSession)
                        }.sortedByDescending { it.third.peakWattage }
                    }

                    if (allCombinations.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                            Text("No accessory pairings tested yet. Connect a charger to run a test!", color = TextSecondary)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(allCombinations) { (pair, list, bestSession) ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectSession(bestSession) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Power, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(pair.first, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Cable, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(pair.second, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "${list.size} test(s) • Last: ${TimeFormatUtils.formatDateHeader(bestSession.startTimeMs)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextSecondary
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Bolt, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(16.dp))
                                                Text(
                                                    text = String.format(Locale.US, "%.1f W", bestSession.peakWattage),
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = GreenPrimary
                                                )
                                            }
                                            Text(
                                                text = String.format(Locale.US, "Avg %.1f W", bestSession.avgWattage),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
