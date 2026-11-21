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
    fun projectionKeepsTheWalkerInTheMiddle() {
        val samples = listOf(
            PedestrianSample(0L, 0f, 0f),
            PedestrianSample(1_000L, 0f, 10f)
        )
        val pins = PedestrianTrack.project(samples, zoneStartMs = 5_000L)
        assertEquals(0.5f, pins.last().xFraction, 0.001f)
        assertEquals(0.5f, pins.last().yFraction, 0.001f)
        assertEquals(0.5f, pins.first().xFraction, 0.001f)
        assertTrue(pins.first().yFraction > 0.5f)
        assertEquals(6_000L, pins.last().placedAtMs)
        assertTrue(pins.first().yFraction < 1f - PedestrianTrack.PAGE_MARGIN + 0.002f)
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

    @Test
    fun flatPhoneUsesTheScreenAxisAsUp() {
        val vertical = PedestrianTrack.verticalAcceleration(0.1f, -0.2f, 1.6f, 0f, 0f, 1f)
        assertEquals(1.6f, vertical, 0.001f)
    }

    @Test
    fun gravityAtRestLeavesNoVerticalBounce() {
        val upX = 0f
        val upY = 0f
        val upZ = 1f
        val linear = PedestrianTrack.removeGravity(0f, 0f, PedestrianTrack.GRAVITY_MPS2, upX, upY, upZ)
        val vertical = PedestrianTrack.verticalAcceleration(linear[0], linear[1], linear[2], upX, upY, upZ)
        assertEquals(0f, vertical, 0.001f)
    }

    @Test
    fun verticalBounceCountsOneStepPerFootfall() {
        val detector = VerticalStepDetector()
        assertFalse(detector.onVertical(0.2f, 0L))
        assertTrue(detector.onVertical(1.5f, 100L))
        assertFalse(detector.onVertical(-1.8f, 250L))
        assertFalse(detector.onVertical(0.1f, 500L))
        assertTrue(detector.onVertical(-1.4f, 900L))
    }

    @Test
    fun arrowFollowsThePhoneBeforeTheTest() {
        val snap = HeadingSnap()
        assertEquals(10f, snap.followPhone(10f), 0.001f)
        assertEquals(40f, snap.followPhone(40f), 0.001f)
    }

    @Test
    fun zoneTestLocksThePhoneDirection() {
        val snap = HeadingSnap()
        assertEquals(5f, snap.capture(5f), 0.001f)
        assertEquals(5f, snap.direction(20f, 22f), 0.001f)
        assertEquals(5f, snap.direction(350f, 22f), 0.001f)
    }

    @Test
    fun arrowStepsByTheSnapAngleAsThePhoneRotates() {
        val snap = HeadingSnap()
        snap.capture(0f)
        assertEquals(0f, snap.direction(21f, 22f), 0.001f)
        assertEquals(22f, snap.direction(22f, 22f), 0.001f)
        assertEquals(22f, snap.direction(43f, 22f), 0.001f)
        assertEquals(44f, snap.direction(50f, 22f), 0.001f)
        val rightAngle = HeadingSnap()
        rightAngle.capture(0f)
        assertEquals(0f, rightAngle.direction(89f, 90f), 0.001f)
        assertEquals(90f, rightAngle.direction(90f, 90f), 0.001f)
    }

    @Test
    fun arrowStepsBackWhenThePhoneTurnsTheOtherWay() {
        val snap = HeadingSnap()
        snap.capture(0f)
        assertEquals(338f, snap.direction(338f, 22f), 0.001f)
        assertEquals(338f, snap.direction(350f, 22f), 0.001f)
        assertEquals(0f, snap.direction(0f, 22f), 0.001f)
    }

    @Test
    fun headingSnapCrossesNorthWithoutAFalseTurn() {
        val snap = HeadingSnap()
        snap.capture(350f)
        assertEquals(350f, snap.direction(5f, 22f), 0.001f)
        assertEquals(12f, snap.direction(12f, 22f), 0.001f)
    }

    @Test
    fun headingSnapChoicesAre22Or45Or90() {
        assertEquals(22f, PedestrianTrack.coerceHeadingSnapDegrees(10f), 0.001f)
        assertEquals(22f, PedestrianTrack.coerceHeadingSnapDegrees(33f), 0.001f)
        assertEquals(45f, PedestrianTrack.coerceHeadingSnapDegrees(40f), 0.001f)
        assertEquals(45f, PedestrianTrack.coerceHeadingSnapDegrees(67f), 0.001f)
        assertEquals(90f, PedestrianTrack.coerceHeadingSnapDegrees(80f), 0.001f)
        assertEquals(2, PedestrianTrack.headingSnapChoiceIndex(90f))
    }

    @Test
    fun walkingDirectionIsRelativeToTheStart() {
        assertEquals(0f, PedestrianTrack.relativeHeading(10f, 10f), 0.001f)
        assertEquals(22f, PedestrianTrack.relativeHeading(10f, 32f), 0.001f)
        assertEquals(20f, PedestrianTrack.relativeHeading(350f, 10f), 0.001f)
        val forward = PedestrianTrack.afterStep(
            PedestrianPosition(),
            PedestrianTrack.relativeHeading(80f, 80f),
            0.75f
        )
        assertEquals(0f, forward.eastMeters, 0.001f)
        assertEquals(0.75f, forward.northMeters, 0.001f)
    }
}
