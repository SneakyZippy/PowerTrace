package com.antigravity.battery.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.battery.core.engine.TimelinePoint
import com.antigravity.battery.ui.theme.AccentBlue
import com.antigravity.battery.ui.theme.DarkBackground
import com.antigravity.battery.ui.theme.DarkSurface
import com.antigravity.battery.ui.theme.DarkSurfaceVariant
import com.antigravity.battery.ui.theme.GreenPrimary
import com.antigravity.battery.ui.theme.TextPrimary
import com.antigravity.battery.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BatteryTimelineChart(
    points: List<TimelinePoint>,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) {
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
        return
    }

    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    val startMs = points.first().timestampMs
    val endMs = points.last().timestampMs
    val timeSpanMs = (endMs - startMs).coerceAtLeast(1L)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(210.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header with scrub info or current status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val activePoint = selectedPointIndex?.let { points.getOrNull(it) } ?: points.last()
                val statusText = if (activePoint.isCharging) "⚡ Charging" else "Discharging"
                val statusColor = if (activePoint.isCharging) AccentBlue else GreenPrimary

                Text(
                    text = "${activePoint.batteryPercent}%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Text(
                    text = "• ${timeFormat.format(Date(activePoint.timestampMs))} ($statusText)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Chart Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(points) {
                        detectTapGestures(
                            onPress = { offset ->
                                val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                                val targetTime = startMs + (fraction * timeSpanMs).toLong()
                                val closestIndex = points.indices.minByOrNull {
                                    kotlin.math.abs(points[it].timestampMs - targetTime)
                                }
                                selectedPointIndex = closestIndex
                            }
                        )
                    }
            ) {
                val w = size.width
                val h = size.height
                val bottomPadding = 20f
                val chartHeight = h - bottomPadding

                // Draw horizontal guide lines (0%, 25%, 50%, 75%, 100%)
                for (level in listOf(0, 25, 50, 75, 100)) {
                    val y = chartHeight - (level / 100f * chartHeight)
                    drawLine(
                        color = Color.White.copy(alpha = 0.06f),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1f
                    )
                }

                // Draw charging background highlights
                for (i in 0 until points.size - 1) {
                    val p1 = points[i]
                    val p2 = points[i + 1]
                    if (p1.isCharging || p2.isCharging) {
                        val x1 = ((p1.timestampMs - startMs).toFloat() / timeSpanMs) * w
                        val x2 = ((p2.timestampMs - startMs).toFloat() / timeSpanMs) * w
                        drawRect(
                            color = AccentBlue.copy(alpha = 0.15f),
                            topLeft = Offset(x1, 0f),
                            size = androidx.compose.ui.geometry.Size((x2 - x1).coerceAtLeast(2f), chartHeight)
                        )
                    }
                }

                // Build battery curve path
                val linePath = Path()
                val fillPath = Path()

                points.forEachIndexed { index, p ->
                    val x = if (timeSpanMs > 0) ((p.timestampMs - startMs).toFloat() / timeSpanMs) * w else 0f
                    val y = chartHeight - (p.batteryPercent / 100f * chartHeight)

                    if (index == 0) {
                        linePath.moveTo(x, y)
                        fillPath.moveTo(x, chartHeight)
                        fillPath.lineTo(x, y)
                    } else {
                        linePath.lineTo(x, y)
                        fillPath.lineTo(x, y)
                    }

                    if (index == points.size - 1) {
                        fillPath.lineTo(x, chartHeight)
                        fillPath.close()
                    }
                }

                // Draw area gradient fill
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            GreenPrimary.copy(alpha = 0.28f),
                            GreenPrimary.copy(alpha = 0.02f)
                        ),
                        startY = 0f,
                        endY = chartHeight
                    )
                )

                // Draw primary battery curve line
                drawPath(
                    path = linePath,
                    color = GreenPrimary,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Draw scrub indicator cursor if active
                selectedPointIndex?.let { idx ->
                    val selPoint = points.getOrNull(idx)
                    if (selPoint != null) {
                        val selX = ((selPoint.timestampMs - startMs).toFloat() / timeSpanMs) * w
                        val selY = chartHeight - (selPoint.batteryPercent / 100f * chartHeight)

                        drawLine(
                            color = Color.White.copy(alpha = 0.5f),
                            start = Offset(selX, 0f),
                            end = Offset(selX, chartHeight),
                            strokeWidth = 1.5.dp.toPx()
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 5.dp.toPx(),
                            center = Offset(selX, selY)
                        )
                        drawCircle(
                            color = GreenPrimary,
                            radius = 3.dp.toPx(),
                            center = Offset(selX, selY)
                        )
                    }
                }
            }
        }
    }
}
