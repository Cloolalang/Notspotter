package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PedestrianTrackTest {

    @Test
    fun stepNorthAndEast() {
        val north = PedestrianTrack.afterStep(PedestrianPosition(), azimuthDegrees = 0f, strideMeters = 0.75f)
        assertEquals(0f, north.eastMeters, 0.001f)
        assertEquals(0.75f, north.northMeters, 0.001f)

        val east = PedestrianTrack.afterStep(PedestrianPosition(), azimuthDegrees = 90f, strideMeters = 0.75f)
        assertEquals(0.75f, east.eastMeters, 0.001f)
        assertEquals(0f, east.northMeters, 0.001f)
    }

    @Test
    fun secondSampleStaysPutWithoutAStep() {
        val origin = PedestrianPosition()
        val first = PedestrianTrack.appendSample(emptyList(), elapsedMs = 0L, position = origin)
        val second = PedestrianTrack.appendSample(first, elapsedMs = 1_000L, position = origin)
        assertEquals(2, second.size)
        assertEquals(first.single().eastMeters, second[1].eastMeters, 0.001f)
        assertEquals(first.single().northMeters, second[1].northMeters, 0.001f)
        assertEquals(1_000L, second[1].elapsedMs)
    }

    @Test
    fun projectionKeepsTheStartInTheMiddle() {
        val samples = listOf(
            PedestrianSample(0L, 0f, 0f),
            PedestrianSample(1_000L, 0f, 10f)
        )
        val pins = PedestrianTrack.project(samples, zoneStartMs = 5_000L)
        assertEquals(0.5f, pins.first().xFraction, 0.001f)
        assertEquals(0.5f, pins.first().yFraction, 0.001f)
        assertEquals(0.5f, pins[1].xFraction, 0.001f)
        assertTrue(pins[1].yFraction < 0.5f)
        assertEquals(6_000L, pins[1].placedAtMs)
        assertTrue(pins[1].yFraction > PedestrianTrack.PAGE_MARGIN - 0.002f)
    }

    @Test
    fun bearingWrapsToACompassDegree() {
        assertEquals(0, PedestrianTrack.bearingDegrees(0f))
        assertEquals(0, PedestrianTrack.bearingDegrees(359.6f))
        assertEquals(90, PedestrianTrack.bearingDegrees(90.4f))
        assertNull(PedestrianTrack.bearingDegrees(null))
    }

    @Test
    fun walkedDistanceFollowsThePath() {
        val samples = listOf(
            PedestrianSample(0L, 0f, 0f),
            PedestrianSample(1_000L, 0f, 0f),
            PedestrianSample(2_000L, 0f, 0.75f),
            PedestrianSample(3_000L, 0.75f, 0.75f)
        )
        assertEquals(1.5f, PedestrianTrack.walkedMeters(samples), 0.001f)
    }

    @Test
    fun gridUsesSmallerSquaresUntilTheWalkIsWideEnough() {
        assertEquals(2f, PedestrianTrack.gridSpacingMeters(4f), 0.001f)
        assertEquals(5f, PedestrianTrack.gridSpacingMeters(12f), 0.001f)
        val grid = PedestrianTrack.grid(4f)
        assertTrue(grid.vertical.any { kotlin.math.abs(it - 0.5f) < 0.001f })
        assertTrue(grid.horizontal.any { kotlin.math.abs(it - 0.5f) < 0.001f })
    }

    @Test
    fun undoMovesOnlyTheNewestMarksAtThatPlace() {
        val samples = listOf(
            PedestrianSample(0L, 0f, 0f),
            PedestrianSample(1_000L, 0f, 0.85f),
            PedestrianSample(2_000L, 0f, 1.7f),
            PedestrianSample(3_000L, 0f, 1.7f)
        )
        val retracted = PedestrianTrack.retractLastPosition(
            samples,
            from = PedestrianPosition(0f, 1.7f),
            to = PedestrianPosition(0f, 0.85f)
        )
        assertEquals(0.85f, retracted[1].northMeters, 0.001f)
        assertEquals(0.85f, retracted[2].northMeters, 0.001f)
        assertEquals(0.85f, retracted[3].northMeters, 0.001f)
        assertEquals(0f, retracted[0].northMeters, 0.001f)
    }

    @Test
    fun accelerometerCountsWhileTheStepDetectorIsQuiet() {
        assertTrue(
            PedestrianTrack.accelerometerMayCountStep(
                detectorPresent = false,
                lastDetectorStepMs = 5_000L,
                nowMs = 5_100L
            )
        )
        assertTrue(
            PedestrianTrack.accelerometerMayCountStep(
                detectorPresent = true,
                lastDetectorStepMs = Long.MIN_VALUE,
                nowMs = 5_000L
            )
        )
        assertFalse(
            PedestrianTrack.accelerometerMayCountStep(
                detectorPresent = true,
                lastDetectorStepMs = 5_000L,
                nowMs = 5_500L
            )
        )
        assertTrue(
            PedestrianTrack.accelerometerMayCountStep(
                detectorPresent = true,
                lastDetectorStepMs = 5_000L,
                nowMs = 5_000L + PedestrianTrack.DETECTOR_QUIET_MS
            )
        )
    }

    @Test
    fun legUsesTheSelectedKpiColour() {
        val pins = listOf(
            WalkPin(placedAtMs = 0L, xFraction = 0.5f, yFraction = 0.5f),
            WalkPin(placedAtMs = 1_000L, xFraction = 0.5f, yFraction = 0.4f)
        )
        val tapes = PedestrianTapes(
            rsrp = RsrpBinTape(listOf(RsrpBinSegment(RsrpBin.BELOW_115, 0L))),
            rsrq = RsrqBinTape(listOf(RsrqBinSegment(RsrqBin.ABOVE_15, 0L)))
        )
        val rsrp = PedestrianKpiColors.legs(pins, PedestrianKpi.RSRP, tapes).single()
        val rsrq = PedestrianKpiColors.legs(pins, PedestrianKpi.RSRQ, tapes).single()
        assertEquals(WalkRoute.rsrpRouteColor(RsrpBin.BELOW_115), rsrp.colorArgb)
        assertEquals(WalkRoute.rsrqRouteColor(RsrqBin.ABOVE_15), rsrq.colorArgb)
        assertNotEquals(rsrp.colorArgb, rsrq.colorArgb)
    }
}
