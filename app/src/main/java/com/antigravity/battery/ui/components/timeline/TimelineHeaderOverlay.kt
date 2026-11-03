package com.antigravity.battery.ui.components.timeline

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.antigravity.battery.core.engine.TimelinePoint
import com.antigravity.battery.ui.theme.AccentBlue
import com.antigravity.battery.ui.theme.CulpritAmber
import com.antigravity.battery.ui.theme.DarkSurfaceVariant
import com.antigravity.battery.ui.theme.GreenPrimary
import com.antigravity.battery.ui.theme.TextPrimary
import com.antigravity.battery.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TimelineHeaderOverlay(
    activePoint: TimelinePoint,
    isScrubbed: Boolean,
    zoomState: TimelineZoomState,
    canvasWidth: Float,
    isMultiDay: Boolean,
    rateText: String?,
    onClearSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeFormat = if (isMultiDay) {
        SimpleDateFormat("MMM dd • HH:mm:ss", Locale.getDefault())
    } else {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    }

    val statusColor = when {
        activePoint.isCharging -> AccentBlue
        activePoint.batteryPercent <= 20 -> CulpritAmber
        else -> GreenPrimary
    }

    val statusLabel = when {
        activePoint.isCharging -> "⚡ Charging"
        activePoint.isScreenOn -> "📱 Screen On"
        else -> "💤 Screen Off"
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Point Status Details
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${activePoint.batteryPercent}%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = timeFormat.format(Date(activePoint.timestampMs)),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                if (isScrubbed) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant)
                            .clickable { onClearSelection() }
                            .padding(2.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear selection",
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (activePoint.isCharging) AccentBlue else TextSecondary
                )
                if (!rateText.isNullOrBlank()) {
                    Text(
                        text = "• $rateText",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (rateText.startsWith("+")) GreenPrimary else CulpritAmber,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Zoom Controls
        TimelineZoomControls(
            zoomState = zoomState,
            canvasWidth = canvasWidth
        )
    }
}

@Composable
private fun TimelineZoomControls(
    zoomState: TimelineZoomState,
    canvasWidth: Float
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (zoomState.isZoomed) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(AccentBlue.copy(alpha = 0.2f))
                    .clickable { zoomState.reset() }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = String.format(Locale.US, "%.1fx", zoomState.zoomScale),
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentBlue,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        IconButton(
            onClick = { zoomState.zoomOut(canvasWidth) },
            enabled = zoomState.isZoomed,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                Icons.Default.Remove,
                contentDescription = "Zoom out",
                tint = if (zoomState.isZoomed) TextPrimary else TextSecondary.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp)
            )
        }

        IconButton(
            onClick = { zoomState.zoomIn(canvasWidth) },
            enabled = zoomState.zoomScale < TimelineZoomState.MAX_ZOOM,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Zoom in",
                tint = if (zoomState.zoomScale < TimelineZoomState.MAX_ZOOM) TextPrimary else TextSecondary.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
