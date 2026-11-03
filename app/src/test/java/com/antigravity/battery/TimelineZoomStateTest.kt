package com.antigravity.battery

import com.antigravity.battery.core.engine.TimelinePoint
import com.antigravity.battery.ui.components.timeline.TimelineZoomState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TimelineZoomStateTest {

    private lateinit var state: TimelineZoomState

    @Before
    fun setUp() {
        state = TimelineZoomState()
    }

    @Test
    fun initialState_hasDefaultZoomAndPan() {
        assertEquals(1.0f, state.zoomScale, 0.001f)
        assertEquals(0.0f, state.panOffset, 0.001f)
        assertNull(state.selectedPointIndex)
        assertFalse(state.isZoomed)
        assertFalse(state.isScrubbing)
    }

    @Test
    fun zoomIn_increasesZoomScale() {
        val width = 1000f
        state.zoomIn(width)

        assertTrue(state.zoomScale > 1.0f)
        assertTrue(state.isZoomed)
    }

    @Test
    fun zoomOut_clampedToMinZoom() {
        val width = 1000f
        state.zoomOut(width)

        assertEquals(TimelineZoomState.MIN_ZOOM, state.zoomScale, 0.001f)
        assertEquals(0.0f, state.panOffset, 0.001f)
    }

    @Test
    fun zoomIn_repeatedly_clampedToMaxZoom() {
        val width = 1000f
        for (i in 0 until 10) {
            state.zoomIn(width)
        }

        assertEquals(TimelineZoomState.MAX_ZOOM, state.zoomScale, 0.001f)
    }

    @Test
    fun toggleZoom_togglesBetween1xAnd3x() {
        val width = 1000f
        state.toggleZoom(500f, width)

        assertEquals(3.0f, state.zoomScale, 0.001f)
        assertTrue(state.isZoomed)

        state.toggleZoom(500f, width)
        assertEquals(1.0f, state.zoomScale, 0.001f)
        assertFalse(state.isZoomed)
    }

    @Test
    fun reset_restoresInitialState() {
        val width = 1000f
        state.zoomIn(width)
        state.toggleZoom(200f, width)

        state.reset()
        assertEquals(1.0f, state.zoomScale, 0.001f)
        assertEquals(0.0f, state.panOffset, 0.001f)
        assertNull(state.selectedPointIndex)
    }

    @Test
    fun screenXToTimestamp_and_timestampToScreenX_areInvertible() {
        val startMs = 1_000_000L
        val timeSpanMs = 3_600_000L // 1 hour
        val width = 1000f

        // At 1x zoom
        val testX = 350f
        val ts = state.screenXToTimestamp(testX, startMs, timeSpanMs, width)
        val invertedX = state.timestampToScreenX(ts, startMs, timeSpanMs, width)
        assertEquals(testX, invertedX, 0.5f)

        // At 2x zoom
        state.applyZoom(2.0f, 500f, width)
        val testX2 = 400f
        val ts2 = state.screenXToTimestamp(testX2, startMs, timeSpanMs, width)
        val invertedX2 = state.timestampToScreenX(ts2, startMs, timeSpanMs, width)
        assertEquals(testX2, invertedX2, 0.5f)
    }

    @Test
    fun scrubTo_findsClosestPointCorrectly() {
        val baseTime = 1_700_000_000_000L
        val points = listOf(
            TimelinePoint(baseTime, 100, isCharging = false, isScreenOn = false),
            TimelinePoint(baseTime + 10_000L, 99, isCharging = false, isScreenOn = true),
            TimelinePoint(baseTime + 20_000L, 98, isCharging = false, isScreenOn = true),
            TimelinePoint(baseTime + 30_000L, 97, isCharging = true, isScreenOn = false)
        )
        val width = 1000f
        val timeSpanMs = 30_000L

        // Scrub near index 1 (approx x = 333f)
        state.scrubTo(330f, width, points, baseTime, timeSpanMs)
        assertEquals(1, state.selectedPointIndex)
        assertTrue(state.isScrubbing)

        // Scrub near end (x = 980f)
        state.scrubTo(980f, width, points, baseTime, timeSpanMs)
        assertEquals(3, state.selectedPointIndex)

        // Clear selection
        state.clearSelection()
        assertNull(state.selectedPointIndex)
        assertFalse(state.isScrubbing)
    }
}
