package com.antigravity.battery.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import com.antigravity.battery.ui.theme.CulpritAmber
import com.antigravity.battery.ui.theme.DarkSurface
import com.antigravity.battery.ui.theme.DarkSurfaceVariant
import com.antigravity.battery.ui.theme.GreenPrimary
import com.antigravity.battery.ui.theme.TextPrimary
import com.antigravity.battery.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChargeCurveChart(
    samples: List<ChargeTelemetrySample>,
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
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    // Downsample if over 120 points for smooth rendering
    val displaySamples = remember(samples) {
        if (samples.size <= 120) samples
        else {
            val step = samples.size / 120.0
            (0 until 120).map { i -> samples[(i * step).toInt().coerceAtMost(samples.lastIndex)] }
        }
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

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CHARGING CURVE OVER TIME",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(GreenPrimary))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Power (W)", style = MaterialTheme.typography.labelSmall, color = GreenPrimary)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(CulpritAmber))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Temp (°C)", style = MaterialTheme.typography.labelSmall, color = CulpritAmber)
                    }
                }
            }

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
                            detectTapGestures(
                                onPress = { offset ->
                                    val index = ((offset.x / size.width) * (displaySamples.size - 1))
                                        .toInt()
                                        .coerceIn(0, displaySamples.lastIndex)
                                    selectedSampleIndex = index
                                }
                            )
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

                    // --- Wattage Path (Green) ---
                    val wPath = Path()
                    val fillPath = Path()

                    val tempPath = Path()

                    displaySamples.forEachIndexed { i, sample ->
                        val x = (i.toFloat() / (displaySamples.size - 1)) * w

                        // Wattage Y
                        val normW = (sample.wattage / maxWattage).coerceIn(0f, 1f)
                        val yW = h - (normW * h)

                        // Temp Y
                        val tempRange = (maxTemp - minTemp).coerceAtLeast(1f)
                        val normTemp = ((sample.tempCelsius - minTemp) / tempRange).coerceIn(0f, 1f)
                        val yTemp = h - (normTemp * h)

                        if (i == 0) {
                            wPath.moveTo(x, yW)
                            fillPath.moveTo(x, h)
                            fillPath.lineTo(x, yW)

                            tempPath.moveTo(x, yTemp)
                        } else {
                            val prevX = ((i - 1).toFloat() / (displaySamples.size - 1)) * w
                            val prevSample = displaySamples[i - 1]
                            val prevYW = h - ((prevSample.wattage / maxWattage).coerceIn(0f, 1f) * h)
                            val prevYTemp = h - (((prevSample.tempCelsius - minTemp) / tempRange).coerceIn(0f, 1f) * h)

                            val cX = (prevX + x) / 2f
                            wPath.cubicTo(cX, prevYW, cX, yW, x, yW)
                            fillPath.cubicTo(cX, prevYW, cX, yW, x, yW)

                            tempPath.cubicTo(cX, prevYTemp, cX, yTemp, x, yTemp)
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

                    // Draw Wattage Line
                    drawPath(
                        path = wPath,
                        color = GreenPrimary,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw Temperature Line (dashed/amber)
                    drawPath(
                        path = tempPath,
                        color = CulpritAmber,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw selected pointer
                    selectedSampleIndex?.let { idx ->
                        val sample = displaySamples[idx]
                        val x = (idx.toFloat() / (displaySamples.size - 1)) * w
                        val yW = h - ((sample.wattage / maxWattage).coerceIn(0f, 1f) * h)

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
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Inspection bar / Time indicator
            val inspected = selectedSampleIndex?.let { displaySamples.getOrNull(it) } ?: displaySamples.lastOrNull()
            if (inspected != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeFormat.format(Date(inspected.timestampMs)),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Text(
                        text = String.format(Locale.US, "%.1f W", inspected.wattage),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = GreenPrimary
                    )
                    Text(
                        text = String.format(Locale.US, "%.1f°C", inspected.tempCelsius),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = CulpritAmber
                    )
                    Text(
                        text = "${inspected.batteryLevel}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}
