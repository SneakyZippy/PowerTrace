package com.antigravity.battery.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import com.antigravity.battery.core.engine.TimelinePoint
import com.antigravity.battery.ui.components.timeline.TimelineChartCanvas
import com.antigravity.battery.ui.components.timeline.TimelineHeaderOverlay
import com.antigravity.battery.ui.components.timeline.rememberTimelineZoomState
import com.antigravity.battery.ui.theme.DarkSurface
import com.antigravity.battery.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun BatteryTimelineChart(
    points: List<TimelinePoint>,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) {
        EmptyTimelineCard(modifier = modifier)
        return
    }

    val zoomState = rememberTimelineZoomState()
    var canvasWidthPx by remember { mutableFloatStateOf(0f) }

    // Downsample points if there are too many to guarantee butter-smooth 60fps rendering
    val displayPoints = remember(points) {
        if (points.size <= 150) {
            points
        } else {
            val step = (points.size / 120).coerceAtLeast(1)
            val decimated = mutableListOf<TimelinePoint>()
            decimated.add(points.first())
            for (i in 1 until points.size - 1 step step) {
                decimated.add(points[i])
            }
            if (decimated.last() != points.last()) {
                decimated.add(points.last())
            }
            decimated
        }
    }

    val startMs = points.first().timestampMs
    val endMs = points.last().timestampMs
    val timeSpanMs = (endMs - startMs).coerceAtLeast(1L)
    val isMultiDay = timeSpanMs > 24 * 3600 * 1000L

    val isScrubbed = zoomState.selectedPointIndex != null
    val activePoint = zoomState.selectedPointIndex?.let { points.getOrNull(it) } ?: points.last()

    // Calculate drain/charge rate around the active point if neighbor points exist
    val rateText = remember(activePoint, points) {
        calculateLocalRateText(activePoint, points)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(230.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            TimelineHeaderOverlay(
                activePoint = activePoint,
                isScrubbed = isScrubbed,
                zoomState = zoomState,
                canvasWidth = canvasWidthPx,
                isMultiDay = isMultiDay,
                rateText = rateText,
                onClearSelection = { zoomState.clearSelection() }
            )

            Spacer(modifier = Modifier.height(8.dp))

            TimelineChartCanvas(
                points = displayPoints,
                zoomState = zoomState,
                startMs = startMs,
                timeSpanMs = timeSpanMs,
                isMultiDay = isMultiDay,
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { canvasWidthPx = it.width.toFloat() }
            )
        }
    }
}

@Composable
private fun EmptyTimelineCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "No timeline points yet. Background recording active.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

private fun calculateLocalRateText(activePoint: TimelinePoint, points: List<TimelinePoint>): String? {
    val idx = points.indexOf(activePoint)
    if (idx <= 0 && points.size < 2) return null

    val prev = if (idx > 0) points[idx - 1] else points.first()
    val deltaBattery = activePoint.batteryPercent - prev.batteryPercent
    val deltaMs = activePoint.timestampMs - prev.timestampMs

    if (deltaMs < 30_000L) return null
    val hours = deltaMs / 3_600_000.0
    val ratePerHour = deltaBattery / hours

    return String.format(Locale.US, "%+.1f%%/h", ratePerHour)
}
