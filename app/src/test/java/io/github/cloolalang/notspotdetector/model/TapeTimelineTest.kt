package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TapeTimelineTest {

    @Test
    fun aChangeAtTheSameMomentLandsAtTheSamePlaceOnBothTapes() {
        val windowStart = TapeTimeline.windowStartMs(nowMs = 9_000L, windowMs = 8_000L)
        assertEquals(1_000L, windowStart)
        val carrier = TapeTimeline.placements(
            segmentStartsMs = listOf(1_000L, 5_000L),
            windowStartMs = 1_000L,
            nowMs = 9_000L
        )
        val technology = TapeTimeline.placements(
            segmentStartsMs = listOf(4_000L, 5_000L),
            windowStartMs = 1_000L,
            nowMs = 9_000L
        )
        assertEquals(0.5f, carrier[1].startFraction, 0.001f)
        assertEquals(carrier[1].startFraction, technology[1].startFraction, 0.001f)
        assertEquals(1f, carrier.last().endFraction, 0.001f)
        assertEquals(1f, technology.last().endFraction, 0.001f)
        assertEquals(0.375f, technology[0].startFraction, 0.001f)
    }

    @Test
    fun theWindowStaysFixedWhenTheOnlyEventIsRecent() {
        val windowStart = TapeTimeline.windowStartMs(nowMs = 600_000L, windowMs = 600_000L)
        val placement = TapeTimeline.placements(
            segmentStartsMs = listOf(540_000L),
            windowStartMs = windowStart,
            nowMs = 600_000L
        ).single()

        assertEquals(0L, windowStart)
        assertEquals(0.9f, placement.startFraction, 0.001f)
        assertEquals(1f, placement.endFraction, 0.001f)
    }

    @Test
    fun retainForScaleKeepsTheSegmentThatCoversTheLeftEdge() {
        val kept = TapeTimeline.retainForScale(
            entries = listOf(0L, 1_000L, 8_000L),
            nowMs = 10_000L,
            retainMs = 5_000L,
            startedAtMs = { it }
        )

        assertEquals(listOf(1_000L, 8_000L), kept)
    }
}
