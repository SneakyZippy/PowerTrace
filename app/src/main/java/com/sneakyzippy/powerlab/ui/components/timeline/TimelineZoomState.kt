package com.sneakyzippy.powerlab.ui.components.timeline

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.sneakyzippy.powerlab.core.engine.TimelinePoint

/**
 * State holder for timeline chart zooming, horizontal panning, and scrub cursor tracking.
 */
@Stable
class TimelineZoomState {
    var zoomScale by mutableFloatStateOf(1f)
        private set

    var panOffset by mutableFloatStateOf(0f)
        private set

    var selectedPointIndex by mutableStateOf<Int?>(null)
        private set

    var isScrubbing by mutableStateOf(false)
        private set

    val isZoomed: Boolean
        get() = zoomScale > 1.05f

    fun clearSelection() {
        selectedPointIndex = null
        isScrubbing = false
    }

    fun reset() {
        zoomScale = 1f
        panOffset = 0f
        selectedPointIndex = null
        isScrubbing = false
    }

    fun zoomIn(width: Float) {
        applyZoom(1.5f, width / 2f, width)
    }

    fun zoomOut(width: Float) {
        applyZoom(1f / 1.5f, width / 2f, width)
    }

    fun toggleZoom(pivotX: Float, width: Float) {
        if (isZoomed) {
            reset()
        } else {
            applyZoom(3.0f, pivotX, width)
        }
    }

    fun applyZoom(multiplier: Float, pivotX: Float, width: Float) {
        onPinchPan(multiplier, 0f, pivotX, width)
    }

    fun onPinchPan(zoomMultiplier: Float, panDeltaX: Float, pivotX: Float, width: Float) {
        if (width <= 0f) return
        val oldZoom = zoomScale
        val newZoom = (oldZoom * zoomMultiplier).coerceIn(MIN_ZOOM, MAX_ZOOM)
        val oldVirtualWidth = width * oldZoom
        val newVirtualWidth = width * newZoom

        // Calculate pivot in virtual coordinates
        val pivotVirtualX = pivotX + panOffset
        val pivotFraction = if (oldVirtualWidth > 0f) {
            (pivotVirtualX / oldVirtualWidth).coerceIn(0f, 1f)
        } else 0.5f

        // Calculate new pan offset preserving pivot location on screen
        val newPan = (pivotFraction * newVirtualWidth) - pivotX - panDeltaX
        val maxPan = (newVirtualWidth - width).coerceAtLeast(0f)

        zoomScale = newZoom
        panOffset = newPan.coerceIn(0f, maxPan)
    }

    fun scrubTo(screenX: Float, width: Float, points: List<TimelinePoint>, startMs: Long, timeSpanMs: Long) {
        if (points.isEmpty() || width <= 0f || timeSpanMs <= 0L) return
        isScrubbing = true
        val targetTime = screenXToTimestamp(screenX, startMs, timeSpanMs, width)
        selectedPointIndex = findClosestPointIndex(points, targetTime)
    }

    fun screenXToTimestamp(screenX: Float, startMs: Long, timeSpanMs: Long, width: Float): Long {
        if (width <= 0f || timeSpanMs <= 0L) return startMs
        val virtualWidth = width * zoomScale
        val virtualX = (screenX + panOffset).coerceIn(0f, virtualWidth)
        val fraction = (virtualX / virtualWidth).coerceIn(0f, 1f)
        return startMs + (fraction * timeSpanMs).toLong()
    }

    fun timestampToScreenX(timestampMs: Long, startMs: Long, timeSpanMs: Long, width: Float): Float {
        if (width <= 0f || timeSpanMs <= 0L) return 0f
        val virtualWidth = width * zoomScale
        val fraction = ((timestampMs - startMs).toFloat() / timeSpanMs).coerceIn(0f, 1f)
        val virtualX = fraction * virtualWidth
        return virtualX - panOffset
    }

    fun visibleTimeRange(startMs: Long, timeSpanMs: Long, width: Float): Pair<Long, Long> {
        val start = screenXToTimestamp(0f, startMs, timeSpanMs, width)
        val end = screenXToTimestamp(width, startMs, timeSpanMs, width)
        return Pair(start, end)
    }

    private fun findClosestPointIndex(points: List<TimelinePoint>, targetTime: Long): Int {
        var low = 0
        var high = points.lastIndex
        while (low <= high) {
            val mid = (low + high).ushr(1)
            val midVal = points[mid].timestampMs
            if (midVal < targetTime) low = mid + 1
            else if (midVal > targetTime) high = mid - 1
            else return mid
        }
        val p1 = low.coerceIn(0, points.lastIndex)
        val p2 = high.coerceIn(0, points.lastIndex)
        return if (kotlin.math.abs(points[p1].timestampMs - targetTime) <
            kotlin.math.abs(points[p2].timestampMs - targetTime)
        ) p1 else p2
    }

    companion object {
        const val MIN_ZOOM = 1.0f
        const val MAX_ZOOM = 8.0f
    }
}

@Composable
fun rememberTimelineZoomState(): TimelineZoomState {
    return remember { TimelineZoomState() }
}
