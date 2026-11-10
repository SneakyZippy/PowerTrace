package com.sneakyzippy.powerlab.ui.components.timeline

import android.graphics.Paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sneakyzippy.powerlab.core.engine.TimelinePoint
import com.sneakyzippy.powerlab.ui.theme.AccentBlue
import com.sneakyzippy.powerlab.ui.theme.GreenPrimary
import java.text.SimpleDateFormat
import java.util.Date

internal object TimelineDrawingUtils {

    fun DrawScope.drawHorizontalGrid(chartHeight: Float, width: Float) {
        val levels = listOf(0, 25, 50, 75, 100)
        for (level in levels) {
            val y = chartHeight - (level / 100f * chartHeight)
            drawLine(
                color = Color.White.copy(alpha = 0.06f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )
        }
    }

    fun DrawScope.drawStateBands(
        points: List<TimelinePoint>,
        zoomState: TimelineZoomState,
        startMs: Long,
        timeSpanMs: Long,
        chartHeight: Float,
        width: Float
    ) {
        if (points.size < 2) return

        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]

            val x1 = zoomState.timestampToScreenX(p1.timestampMs, startMs, timeSpanMs, width)
            val x2 = zoomState.timestampToScreenX(p2.timestampMs, startMs, timeSpanMs, width)

            if (x2 < 0f || x1 > width) continue

            if (p1.isCharging || p2.isCharging) {
                val startX = x1.coerceAtLeast(0f)
                val endX = x2.coerceAtMost(width)
                if (endX > startX) {
                    drawRect(
                        color = AccentBlue.copy(alpha = 0.15f),
                        topLeft = Offset(startX, 0f),
                        size = Size(endX - startX, chartHeight)
                    )
                }
            }

            if (p1.isScreenOn || p2.isScreenOn) {
                val startX = x1.coerceAtLeast(0f)
                val endX = x2.coerceAtMost(width)
                if (endX > startX) {
                    drawRect(
                        color = GreenPrimary.copy(alpha = 0.45f),
                        topLeft = Offset(startX, chartHeight - 3.dp.toPx()),
                        size = Size(endX - startX, 3.dp.toPx())
                    )
                }
            }
        }
    }

    fun DrawScope.drawBatteryCurve(
        points: List<TimelinePoint>,
        zoomState: TimelineZoomState,
        startMs: Long,
        timeSpanMs: Long,
        chartHeight: Float,
        width: Float
    ) {
        if (points.isEmpty()) return

        val linePath = Path()
        val fillPath = Path()
        var isPathStarted = false
        var firstVisibleX = 0f

        for (i in points.indices) {
            val p = points[i]
            val x = zoomState.timestampToScreenX(p.timestampMs, startMs, timeSpanMs, width)
            val y = chartHeight - (p.batteryPercent / 100f * chartHeight)

            if (!isPathStarted) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, chartHeight)
                fillPath.lineTo(x, y)
                firstVisibleX = x
                isPathStarted = true
            } else {
                linePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }

        if (isPathStarted) {
            val lastX = zoomState.timestampToScreenX(points.last().timestampMs, startMs, timeSpanMs, width)
            fillPath.lineTo(lastX, chartHeight)
            fillPath.lineTo(firstVisibleX, chartHeight)
            fillPath.close()

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

            drawPath(
                path = linePath,
                color = GreenPrimary,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }

    fun DrawScope.drawScrubCursor(
        points: List<TimelinePoint>,
        zoomState: TimelineZoomState,
        startMs: Long,
        timeSpanMs: Long,
        chartHeight: Float,
        width: Float
    ) {
        val idx = zoomState.selectedPointIndex ?: return
        val point = points.getOrNull(idx) ?: return
        val x = zoomState.timestampToScreenX(point.timestampMs, startMs, timeSpanMs, width)

        if (x < -10f || x > width + 10f) return

        val y = chartHeight - (point.batteryPercent / 100f * chartHeight)

        drawLine(
            color = Color.White.copy(alpha = 0.55f),
            start = Offset(x, 0f),
            end = Offset(x, chartHeight),
            strokeWidth = 1.5.dp.toPx()
        )

        drawCircle(
            color = Color.White,
            radius = 5.5.dp.toPx(),
            center = Offset(x, y)
        )

        drawCircle(
            color = if (point.isCharging) AccentBlue else GreenPrimary,
            radius = 3.5.dp.toPx(),
            center = Offset(x, y)
        )
    }

    fun DrawScope.drawTimeTicks(
        zoomState: TimelineZoomState,
        startMs: Long,
        timeSpanMs: Long,
        chartHeight: Float,
        width: Float,
        formatter: SimpleDateFormat
    ) {
        val textPaint = Paint().apply {
            color = android.graphics.Color.argb(120, 180, 180, 180)
            textSize = 10.sp.toPx()
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val tickCount = 4
        for (i in 0..tickCount) {
            val fraction = i.toFloat() / tickCount
            val screenX = fraction * width
            val tickTimeMs = zoomState.screenXToTimestamp(screenX, startMs, timeSpanMs, width)
            val text = formatter.format(Date(tickTimeMs))
            val y = chartHeight + 16.dp.toPx()

            drawContext.canvas.nativeCanvas.drawText(text, screenX, y, textPaint)
        }
    }
}
