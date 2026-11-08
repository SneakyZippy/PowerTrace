package com.antigravity.battery.ui.components

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
import com.antigravity.battery.core.model.ChargeTelemetrySample
import com.antigravity.battery.core.model.ChargeTestSession
import com.antigravity.battery.ui.theme.AccentBlue
import com.antigravity.battery.ui.theme.CulpritAmber
import com.antigravity.battery.ui.theme.DarkSurface
import com.antigravity.battery.ui.theme.DarkSurfaceVariant
import com.antigravity.battery.ui.theme.GreenPrimary
import com.antigravity.battery.ui.theme.TextPrimary
import com.antigravity.battery.ui.theme.TextSecondary

@Composable
fun ChargeCurveChart(
    samples: List<ChargeTelemetrySample>,
    session: ChargeTestSession? = null,
    modifier: Modifier = Modifier
) {
    if (samples.isEmpty()) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(14.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No curve telemetry samples recorded for this session.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
        return
    }

    var selectedSampleIndex by remember { mutableStateOf<Int?>(null) }

    val fullSamples = remember(samples, session) {
        ChargeCurveDataHelper.reconcileSamples(samples, session)
    }

    val displaySamples = remember(fullSamples) {
        ChargeCurveDataHelper.downsample(fullSamples, maxPoints = 120)
    }

    val maxWattage = remember(displaySamples) {
        (displaySamples.maxOfOrNull { it.wattage } ?: 10f).coerceAtLeast(5f) * 1.15f
    }
    val minTemp = remember(displaySamples) {
        (displaySamples.minOfOrNull { it.tempCelsius } ?: 25f).coerceAtLeast(15f) - 2f
    }
    val maxTemp = remember(displaySamples) {
        (displaySamples.maxOfOrNull { it.tempCelsius } ?: 40f) + 2f
    }
    val minLevel = remember(displaySamples) {
        ((displaySamples.minOfOrNull { it.batteryLevel } ?: 0) - 2).coerceAtLeast(0)
    }
    val maxLevel = remember(displaySamples) {
        ((displaySamples.maxOfOrNull { it.batteryLevel } ?: 100) + 2).coerceAtMost(100)
    }
    val levelRange = remember(minLevel, maxLevel) {
        (maxLevel - minLevel).coerceAtLeast(5)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with legend
            ChargeCurveHeaderWithLegend()

            Spacer(modifier = Modifier.height(12.dp))

            // Canvas Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(displaySamples) {
                            val startMs = displaySamples.first().timestampMs
                            val durationMs = (displaySamples.last().timestampMs - startMs).coerceAtLeast(1L)
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val touchRatio = (down.position.x / size.width).coerceIn(0f, 1f)
                                val targetTime = startMs + (touchRatio * durationMs).toLong()
                                selectedSampleIndex = displaySamples.indices.minByOrNull { 
                                    kotlin.math.abs(displaySamples[it].timestampMs - targetTime) 
                                } ?: 0

                                do {
                                    val event = awaitPointerEvent()
                                    val ptr = event.changes.firstOrNull { it.pressed }
                                    if (ptr != null) {
                                        val moveRatio = (ptr.position.x / size.width).coerceIn(0f, 1f)
                                        val moveTime = startMs + (moveRatio * durationMs).toLong()
                                        selectedSampleIndex = displaySamples.indices.minByOrNull { 
                                            kotlin.math.abs(displaySamples[it].timestampMs - moveTime) 
                                        } ?: 0
                                        ptr.consume()
                                    }
                                } while (event.changes.any { it.pressed })
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height

                    // Grid lines (3 horizontal lines)
                    val gridSteps = 3
                    for (i in 0..gridSteps) {
                        val y = h * (i.toFloat() / gridSteps)
                        drawLine(
                            color = DarkSurfaceVariant.copy(alpha = 0.6f),
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    if (displaySamples.size < 2) return@Canvas

                    // --- Paths ---
                    val wPath = Path()
                    val fillPath = Path()
                    val tempPath = Path()
                    val levelPath = Path()

                    val tempRange = (maxTemp - minTemp).coerceAtLeast(1f)
                    val startMs = displaySamples.first().timestampMs
                    val durationMs = (displaySamples.last().timestampMs - startMs).coerceAtLeast(1L)

                    displaySamples.forEachIndexed { i, sample ->
                        val x = ((sample.timestampMs - startMs).toFloat() / durationMs) * w

                        // Wattage Y
                        val normW = (sample.wattage / maxWattage).coerceIn(0f, 1f)
                        val yW = h - (normW * h)

                        // Temp Y
                        val normTemp = ((sample.tempCelsius - minTemp) / tempRange).coerceIn(0f, 1f)
                        val yTemp = h - (normTemp * h)

                        // Battery Level Y
                        val normLevel = ((sample.batteryLevel - minLevel).toFloat() / levelRange).coerceIn(0f, 1f)
                        val yLevel = h - (normLevel * h)

                        if (i == 0) {
                            wPath.moveTo(x, yW)
                            fillPath.moveTo(x, h)
                            fillPath.lineTo(x, yW)
                            tempPath.moveTo(x, yTemp)
                            levelPath.moveTo(x, yLevel)
                        } else {
                            val prevSample = displaySamples[i - 1]
                            val prevX = ((prevSample.timestampMs - startMs).toFloat() / durationMs) * w
                            val prevYW = h - ((prevSample.wattage / maxWattage).coerceIn(0f, 1f) * h)
                            val prevYTemp = h - (((prevSample.tempCelsius - minTemp) / tempRange).coerceIn(0f, 1f) * h)
                            val prevYLevel = h - (((prevSample.batteryLevel - minLevel).toFloat() / levelRange).coerceIn(0f, 1f) * h)

                            val cX = (prevX + x) / 2f
                            wPath.cubicTo(cX, prevYW, cX, yW, x, yW)
                            fillPath.cubicTo(cX, prevYW, cX, yW, x, yW)
                            tempPath.cubicTo(cX, prevYTemp, cX, yTemp, x, yTemp)
                            levelPath.cubicTo(cX, prevYLevel, cX, yLevel, x, yLevel)
                        }
                    }

                    fillPath.lineTo(w, h)
                    fillPath.close()

                    // Draw gradient fill under wattage curve
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                GreenPrimary.copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        )
                    )

                    // Draw Wattage Line (Green)
                    drawPath(
                        path = wPath,
                        color = GreenPrimary,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw Temperature Line (Amber)
                    drawPath(
                        path = tempPath,
                        color = CulpritAmber,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw Battery Level Line (AccentBlue)
                    drawPath(
                        path = levelPath,
                        color = AccentBlue,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw selected pointer
                    selectedSampleIndex?.let { idx ->
                        val sample = displaySamples[idx]
                        val x = ((sample.timestampMs - startMs).toFloat() / durationMs) * w
                        val yW = h - ((sample.wattage / maxWattage).coerceIn(0f, 1f) * h)
                        val yLevel = h - (((sample.batteryLevel - minLevel).toFloat() / levelRange).coerceIn(0f, 1f) * h)

                        drawLine(
                            color = TextPrimary.copy(alpha = 0.5f),
                            start = Offset(x, 0f),
                            end = Offset(x, h),
                            strokeWidth = 1.dp.toPx()
                        )

                        drawCircle(
                            color = GreenPrimary,
                            radius = 5.dp.toPx(),
                            center = Offset(x, yW)
                        )

                        drawCircle(
                            color = AccentBlue,
                            radius = 4.dp.toPx(),
                            center = Offset(x, yLevel)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Inspection bar / Time indicator
            val inspected = selectedSampleIndex?.let { displaySamples.getOrNull(it) } ?: displaySamples.lastOrNull()
            if (inspected != null) {
                ChargeCurveInspectionBar(inspected = inspected)
            }
        }
    }
}
