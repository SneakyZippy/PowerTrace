package com.antigravity.battery.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.battery.core.model.CulpritWarningBadge
import com.antigravity.battery.ui.theme.AccentBlue
import com.antigravity.battery.ui.theme.CulpritAmber
import com.antigravity.battery.ui.theme.CulpritRed
import com.antigravity.battery.ui.theme.DarkSurface
import com.antigravity.battery.ui.theme.DarkSurfaceVariant
import com.antigravity.battery.ui.theme.GreenPrimary
import com.antigravity.battery.ui.theme.PurpleAccent
import com.antigravity.battery.ui.theme.TextPrimary
import com.antigravity.battery.ui.theme.TextSecondary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon

@Composable
fun MetricStatTile(
    label: String,
    value: String,
    unit: String = "",
    valueColor: Color = TextPrimary,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = if (onClick != null) modifier.clickable { onClick() } else modifier,
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                if (onClick != null) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = TextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = valueColor
                )
                if (unit.isNotEmpty()) {
                    Text(
                        text = " $unit",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DozeSleepEfficiencyBar(
    deepSleepMs: Long,
    awakeScreenOffMs: Long,
    screenOnMs: Long,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val totalMs = (deepSleepMs + awakeScreenOffMs + screenOnMs).coerceAtLeast(1L).toFloat()
    val deepSleepRatio = (deepSleepMs / totalMs).coerceIn(0f, 1f)
    val awakeRatio = (awakeScreenOffMs / totalMs).coerceIn(0f, 1f)
    val screenOnRatio = (screenOnMs / totalMs).coerceIn(0f, 1f)

    Column(
        modifier = if (onClick != null) modifier.fillMaxWidth().clickable { onClick() } else modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Sleep State Distribution", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                if (onClick != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = TextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
            Text(
                "Deep Sleep: ${(deepSleepRatio * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = GreenPrimary,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(DarkSurfaceVariant)
        ) {
            if (deepSleepRatio > 0) {
                Box(
                    modifier = Modifier
                        .weight(deepSleepRatio)
                        .height(14.dp)
                        .background(GreenPrimary)
                )
            }
            if (awakeRatio > 0) {
                Box(
                    modifier = Modifier
                        .weight(awakeRatio)
                        .height(14.dp)
                        .background(CulpritAmber)
                )
            }
            if (screenOnRatio > 0) {
                Box(
                    modifier = Modifier
                        .weight(screenOnRatio)
                        .height(14.dp)
                        .background(AccentBlue)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LegendItem(color = GreenPrimary, text = "Deep Sleep (${deepSleepMs / 60000}m)")
            LegendItem(color = CulpritAmber, text = "Awake Screen Off (${awakeScreenOffMs / 60000}m)")
            if (screenOnMs > 0) {
                LegendItem(color = AccentBlue, text = "Screen On (${screenOnMs / 60000}m)")
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .padding(end = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
                .height(8.dp)
                .fillMaxWidth(0.04f)
        )
        Text(text = text, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

@Composable
fun WarningBadgeChip(
    badge: CulpritWarningBadge,
    onClick: (() -> Unit)? = null
) {
    val (color, text) = when (badge) {
        CulpritWarningBadge.RUNAWAY_WAKELOCK -> Pair(CulpritRed, "WAKELOCK > 10M")
        CulpritWarningBadge.AGGRESSIVE_WAKEUPS -> Pair(CulpritAmber, "FREQUENT ALARMS")
        CulpritWarningBadge.RADIO_KEEP_ALIVE -> Pair(PurpleAccent, "RADIO ACTIVE")
        CulpritWarningBadge.HIGH_BACKGROUND_CPU -> Pair(CulpritAmber, "HIGH BG CPU")
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.2f))
            .let { if (onClick != null) it.clickable { onClick() } else it }
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
