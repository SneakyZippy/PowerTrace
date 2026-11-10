package com.sneakyzippy.powerlab.ui.components.timeline

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.sneakyzippy.powerlab.core.engine.TimelinePoint
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun TimelineChartCanvas(
    points: List<TimelinePoint>,
    zoomState: TimelineZoomState,
    startMs: Long,
    timeSpanMs: Long,
    isMultiDay: Boolean,
    modifier: Modifier = Modifier
) {
    val tickTimeFormat = remember(isMultiDay) {
        if (isMultiDay) SimpleDateFormat("d/M HH:mm", Locale.getDefault())
        else SimpleDateFormat("HH:mm", Locale.getDefault())
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(zoomState, points) {
                var lastTapTime = 0L
                var lastTapPos = Offset.Zero

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val w = size.width.toFloat()
                    val now = System.currentTimeMillis()

                    if (now - lastTapTime < 320L && (down.position - lastTapPos).getDistance() < 50f) {
                        zoomState.toggleZoom(down.position.x, w)
                        lastTapTime = 0L
                    } else {
                        lastTapTime = now
                        lastTapPos = down.position
                        zoomState.scrubTo(down.position.x, w, points, startMs, timeSpanMs)
                    }

                    var isMulti = false
                    var prevDistance = 0f
                    var prevCentroid = Offset.Zero

                    do {
                        val event = awaitPointerEvent()
                        val activePointers = event.changes.filter { it.pressed }

                        if (activePointers.size >= 2) {
                            isMulti = true
                            val p1 = activePointers[0].position
                            val p2 = activePointers[1].position
                            val currentDistance = (p1 - p2).getDistance()
                            val currentCentroid = (p1 + p2) / 2f

                            if (prevDistance > 0f) {
                                val zoomMultiplier = currentDistance / prevDistance
                                val panDeltaX = currentCentroid.x - prevCentroid.x
                                zoomState.onPinchPan(zoomMultiplier, panDeltaX, currentCentroid.x, w)
                            }
                            prevDistance = currentDistance
                            prevCentroid = currentCentroid
                            activePointers.forEach { it.consume() }
                        } else if (activePointers.size == 1) {
                            prevDistance = 0f
                            if (!isMulti) {
                                val ptr = activePointers.first()
                                zoomState.scrubTo(ptr.position.x, w, points, startMs, timeSpanMs)
                                ptr.consume()
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
    ) {
        val w = size.width
        val h = size.height
        val bottomPadding = 22.dp.toPx()
        val chartHeight = h - bottomPadding

        with(TimelineDrawingUtils) {
            drawHorizontalGrid(chartHeight, w)

            clipRect(0f, 0f, w, chartHeight) {
                drawStateBands(points, zoomState, startMs, timeSpanMs, chartHeight, w)
                drawBatteryCurve(points, zoomState, startMs, timeSpanMs, chartHeight, w)
                drawScrubCursor(points, zoomState, startMs, timeSpanMs, chartHeight, w)
            }

            drawTimeTicks(zoomState, startMs, timeSpanMs, chartHeight, w, tickTimeFormat)
        }
    }
}
