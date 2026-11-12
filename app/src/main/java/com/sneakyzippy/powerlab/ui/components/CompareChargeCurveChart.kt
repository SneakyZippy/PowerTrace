package com.sneakyzippy.powerlab.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sneakyzippy.powerlab.core.model.ChargeTelemetrySample
import com.sneakyzippy.powerlab.core.model.ChargeTestSession
import com.sneakyzippy.powerlab.ui.theme.AccentBlue
import com.sneakyzippy.powerlab.ui.theme.DarkBackground
import com.sneakyzippy.powerlab.ui.theme.DarkSurface
import com.sneakyzippy.powerlab.ui.theme.DarkSurfaceVariant
import com.sneakyzippy.powerlab.ui.theme.GreenPrimary
import com.sneakyzippy.powerlab.ui.theme.TextPrimary
import com.sneakyzippy.powerlab.ui.theme.TextSecondary
import com.sneakyzippy.powerlab.ui.util.TimeFormatUtils
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

enum class CompareChartMetric(val label: String) {
    POWER("Power (W)"),
    TEMPERATURE("Temp (°C)"),
    BATTERY("Battery (%)")
}

private data class ElapsedPoint(
    val elapsedMs: Long,
    val sample: ChargeTelemetrySample
)

@Composable
fun CompareChargeCurveChart(
    sessionA: ChargeTestSession,
    samplesA: List<ChargeTelemetrySample>,
    sessionB: ChargeTestSession,
    samplesB: List<ChargeTelemetrySample>,
    modifier: Modifier = Modifier
) {
    var selectedMetric by remember { mutableStateOf(CompareChartMetric.POWER) }
    var scrubElapsedMs by remember { mutableStateOf<Long?>(null) }

    val pointsA = remember(samplesA, sessionA) {
        val reconciled = ChargeCurveDataHelper.reconcileSamples(samplesA, sessionA)
        val downsampled = ChargeCurveDataHelper.downsample(reconciled, maxPoints = 100)
        val start = downsampled.firstOrNull()?.timestampMs ?: 0L
        downsampled.map { ElapsedPoint(it.timestampMs - start, it) }
    }

    val pointsB = remember(samplesB, sessionB) {
        val reconciled = ChargeCurveDataHelper.reconcileSamples(samplesB, sessionB)
        val downsampled = ChargeCurveDataHelper.downsample(reconciled, maxPoints = 100)
        val start = downsampled.firstOrNull()?.timestampMs ?: 0L
        downsampled.map { ElapsedPoint(it.timestampMs - start, it) }
    }

    if (pointsA.isEmpty() && pointsB.isEmpty()) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .height(160.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(14.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No curve telemetry samples recorded for these sessions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
        return
    }

    val durationA = pointsA.lastOrNull()?.elapsedMs ?: 0L
    val durationB = pointsB.lastOrNull()?.elapsedMs ?: 0L
    val maxDurationMs = max(durationA, durationB).coerceAtLeast(1L)

    val allSamples = remember(pointsA, pointsB) {
        (pointsA + pointsB).map { it.sample }
    }

    val (minVal, maxVal) = remember(allSamples, selectedMetric) {
        when (selectedMetric) {
            CompareChartMetric.POWER -> {
                val maxW = (allSamples.maxOfOrNull { it.wattage } ?: 10f).coerceAtLeast(5f) * 1.15f
                0f to maxW
            }
            CompareChartMetric.TEMPERATURE -> {
                val minT = ((allSamples.minOfOrNull { it.tempCelsius } ?: 25f) - 2f).coerceAtLeast(10f)
                val maxT = (allSamples.maxOfOrNull { it.tempCelsius } ?: 40f) + 2f
                minT to max(maxT, minT + 4f)
            }
            CompareChartMetric.BATTERY -> {
                val minL = ((allSamples.minOfOrNull { it.batteryLevel } ?: 0) - 2).coerceAtLeast(0).toFloat()
                val maxL = ((allSamples.maxOfOrNull { it.batteryLevel } ?: 100) + 2).coerceAtMost(100).toFloat()
                minL to max(maxL, minL + 5f)
            }
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Overlay Curve",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CompareLegendDot(color = GreenPrimary, label = "Session A")
                    CompareLegendDot(color = AccentBlue, label = "Session B")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Metric selector chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompareChartMetric.values().forEach { metric ->
                    val isSelected = selectedMetric == metric
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedMetric = metric },
                        label = { Text(metric.label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = DarkBackground,
                            labelColor = TextSecondary,
                            selectedContainerColor = GreenPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = GreenPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(maxDurationMs) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val ratio = (down.position.x / size.width).coerceIn(0f, 1f)
                                scrubElapsedMs = (ratio * maxDurationMs).toLong()

                                do {
                                    val event = awaitPointerEvent()
                                    val ptr = event.changes.firstOrNull { it.pressed }
                                    if (ptr != null) {
                                        val moveRatio = (ptr.position.x / size.width).coerceIn(0f, 1f)
                                        scrubElapsedMs = (moveRatio * maxDurationMs).toLong()
                                        ptr.consume()
                                    }
                                } while (event.changes.any { it.pressed })
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height

                    for (i in 0..3) {
                        val y = h * (i.toFloat() / 3f)
                        drawLine(
                            color = DarkSurfaceVariant.copy(alpha = 0.6f),
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    drawSessionCurve(
                        points = pointsA,
                        maxDurationMs = maxDurationMs,
                        minVal = minVal,
                        maxVal = maxVal,
                        metric = selectedMetric,
                        color = GreenPrimary,
                        canvasWidth = w,
                        canvasHeight = h
                    )

                    drawSessionCurve(
                        points = pointsB,
                        maxDurationMs = maxDurationMs,
                        minVal = minVal,
                        maxVal = maxVal,
                        metric = selectedMetric,
                        color = AccentBlue,
                        canvasWidth = w,
                        canvasHeight = h
                    )

                    scrubElapsedMs?.let { targetMs ->
                        val x = (targetMs.toFloat() / maxDurationMs) * w
                        drawLine(
                            color = TextPrimary.copy(alpha = 0.5f),
                            start = Offset(x, 0f),
                            end = Offset(x, h),
                            strokeWidth = 1.dp.toPx()
                        )

                        closestPointAt(pointsA, targetMs)?.let { ptA ->
                            val yA = valueToY(metricValue(ptA.sample, selectedMetric), minVal, maxVal, h)
                            val xA = (ptA.elapsedMs.toFloat() / maxDurationMs) * w
                            drawCircle(color = GreenPrimary, radius = 5.dp.toPx(), center = Offset(xA, yA))
                        }
                        closestPointAt(pointsB, targetMs)?.let { ptB ->
                            val yB = valueToY(metricValue(ptB.sample, selectedMetric), minVal, maxVal, h)
                            val xB = (ptB.elapsedMs.toFloat() / maxDurationMs) * w
                            drawCircle(color = AccentBlue, radius = 5.dp.toPx(), center = Offset(xB, yB))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val activeElapsed = scrubElapsedMs ?: maxDurationMs
            val ptA = closestPointAt(pointsA, activeElapsed)
            val ptB = closestPointAt(pointsB, activeElapsed)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkBackground)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Elapsed: ${TimeFormatUtils.formatDurationHuman(activeElapsed)} (drag chart to inspect)",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = ptA?.let { formatSampleSummary("A", it.sample) } ?: "A: ended",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = GreenPrimary
                    )
                    Text(
                        text = ptB?.let { formatSampleSummary("B", it.sample) } ?: "B: ended",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = AccentBlue
                    )
                }
            }
        }
    }
}

private fun metricValue(sample: ChargeTelemetrySample, metric: CompareChartMetric): Float = when (metric) {
    CompareChartMetric.POWER -> sample.wattage
    CompareChartMetric.TEMPERATURE -> sample.tempCelsius
    CompareChartMetric.BATTERY -> sample.batteryLevel.toFloat()
}

private fun valueToY(value: Float, minVal: Float, maxVal: Float, height: Float): Float {
    val range = (maxVal - minVal).coerceAtLeast(0.01f)
    val norm = ((value - minVal) / range).coerceIn(0f, 1f)
    return height - (norm * height)
}

private fun closestPointAt(points: List<ElapsedPoint>, targetMs: Long): ElapsedPoint? {
    if (points.isEmpty()) return null
    val lastElapsed = points.last().elapsedMs
    if (targetMs > lastElapsed + 60_000L) return null
    return points.minByOrNull { abs(it.elapsedMs - targetMs) }
}

private fun DrawScope.drawSessionCurve(
    points: List<ElapsedPoint>,
    maxDurationMs: Long,
    minVal: Float,
    maxVal: Float,
    metric: CompareChartMetric,
    color: Color,
    canvasWidth: Float,
    canvasHeight: Float
) {
    if (points.size < 2) return

    val linePath = Path()
    val fillPath = Path()

    points.forEachIndexed { index, pt ->
        val x = (pt.elapsedMs.toFloat() / maxDurationMs) * canvasWidth
        val y = valueToY(metricValue(pt.sample, metric), minVal, maxVal, canvasHeight)

        if (index == 0) {
            linePath.moveTo(x, y)
            fillPath.moveTo(x, canvasHeight)
            fillPath.lineTo(x, y)
        } else {
            val prev = points[index - 1]
            val prevX = (prev.elapsedMs.toFloat() / maxDurationMs) * canvasWidth
            val prevY = valueToY(metricValue(prev.sample, metric), minVal, maxVal, canvasHeight)
            val cX = (prevX + x) / 2f
            linePath.cubicTo(cX, prevY, cX, y, x, y)
            fillPath.cubicTo(cX, prevY, cX, y, x, y)
        }
    }

    val lastX = (points.last().elapsedMs.toFloat() / maxDurationMs) * canvasWidth
    fillPath.lineTo(lastX, canvasHeight)
    fillPath.close()

    drawPath(
        path = fillPath,
        brush = Brush.verticalGradient(
            colors = listOf(color.copy(alpha = 0.18f), Color.Transparent)
        )
    )
    drawPath(
        path = linePath,
        color = color,
        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
    )
}

private fun formatSampleSummary(prefix: String, sample: ChargeTelemetrySample): String {
    return String.format(
        Locale.US,
        "%s: %.1fW • %.1f°C • %d%%",
        prefix,
        sample.wattage,
        sample.tempCelsius,
        sample.batteryLevel
    )
}

@Composable
private fun CompareLegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}
