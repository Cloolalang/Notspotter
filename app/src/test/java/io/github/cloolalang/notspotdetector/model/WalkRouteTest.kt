package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WalkRouteTest {

    @Test
    fun elapsedLabel_countsMinutesAndSecondsFromTheZoneStart() {
        assertEquals("0:00", WalkRoute.elapsedLabel(0L))
        assertEquals("0:30", WalkRoute.elapsedLabel(30_000L))
        assertEquals("1:30", WalkRoute.elapsedLabel(90_000L))
        assertEquals("60:00", WalkRoute.elapsedLabel(3_600_000L))
    }

    @Test
    fun labelStep_isWiderOnALongerWalk() {
        assertEquals(30_000L, WalkRoute.labelStepMs(2L * 60_000L))
        assertEquals(60_000L, WalkRoute.labelStepMs(10L * 60_000L))
        assertEquals(120_000L, WalkRoute.labelStepMs(30L * 60_000L))
    }

    @Test
    fun tickTimes_landEveryThirtySecondsAndStopAtTheEnd() {
        assertEquals(listOf(0L, 30_000L), WalkRoute.tickTimes(0L, 50_000L))
        assertEquals(listOf(1_000L), WalkRoute.tickTimes(1_000L, 1_000L))
    }

    @Test
    fun spans_splitALegWhereTheRsrpBandChanges() {
        val pins = listOf(
            WalkPin(placedAtMs = 0L, xFraction = 0f, yFraction = 0f),
            WalkPin(placedAtMs = 1_000L, xFraction = 1f, yFraction = 0f)
        )
        val segments = listOf(
            RsrpBinSegment(RsrpBin.ABOVE_95, startedAtMs = 0L),
            RsrpBinSegment(RsrpBin.BELOW_115, startedAtMs = 500L)
        )

        val spans = WalkRoute.spans(pins, segments)

        assertEquals(2, spans.size)
        assertEquals(RsrpBin.ABOVE_95, spans[0].bin)
        assertEquals(0f, spans[0].start.x, 0.001f)
        assertEquals(0.5f, spans[0].end.x, 0.001f)
        assertEquals(RsrpBin.BELOW_115, spans[1].bin)
        assertEquals(0.5f, spans[1].start.x, 0.001f)
        assertEquals(1f, spans[1].end.x, 0.001f)
    }

    @Test
    fun spans_keepTheBandThatWasAlreadyActive() {
        val pins = listOf(
            WalkPin(placedAtMs = 1_000L, xFraction = 0f, yFraction = 0f),
            WalkPin(placedAtMs = 2_000L, xFraction = 1f, yFraction = 1f)
        )
        val segments = listOf(RsrpBinSegment(RsrpBin.ABOVE_105, startedAtMs = 0L))

        val spans = WalkRoute.spans(pins, segments)

        assertEquals(1, spans.size)
        assertEquals(RsrpBin.ABOVE_105, spans[0].bin)
    }

    @Test
    fun spans_needTwoPinsAtDifferentTimes() {
        val pin = WalkPin(placedAtMs = 5L, xFraction = 0.2f, yFraction = 0.4f)
        assertEquals(emptyList<RouteSpan>(), WalkRoute.spans(listOf(pin), emptyList()))
        assertEquals(
            emptyList<RouteSpan>(),
            WalkRoute.spans(listOf(pin, pin.copy(xFraction = 0.8f)), emptyList())
        )
    }

    @Test
    fun coloredSpans_followCarrierTapeCuts() {
        val pins = listOf(
            WalkPin(placedAtMs = 0L, xFraction = 0f, yFraction = 0f),
            WalkPin(placedAtMs = 1_000L, xFraction = 1f, yFraction = 0f)
        )
        val entries = listOf(
            PrimaryCarrierTapeEntry(
                pair = PrimaryCarrierPair(channel = 223, pci = 10, nominalMhz = 2100),
                shade = 0,
                startedAtMs = 0L
            ),
            PrimaryCarrierTapeEntry(
                pair = PrimaryCarrierPair(channel = 223, pci = 20, nominalMhz = 2100),
                shade = 1,
                startedAtMs = 400L
            )
        )

        val spans = WalkRoute.coloredSpans(pins, entries.map { it.startedAtMs }) { atMs ->
            PrimaryCarrierHistory.colorArgb(WalkRoute.carrierEntryAt(entries, atMs)!!)
        }

        assertEquals(2, spans.size)
        assertEquals(0.4f, spans[0].end.x, 0.001f)
        assertEquals(0.4f, spans[1].start.x, 0.001f)
        assertNotEquals(spans[0].colorArgb, spans[1].colorArgb)
    }

    @Test
    fun offsetSpan_movesPerpendicularToTravel() {
        val (from, to) = WalkRoute.offsetSpan(
            start = ImagePoint(0f, 0f),
            end = ImagePoint(10f, 0f),
            offsetPx = 5f
        )
        assertEquals(0f, from.x, 0.001f)
        assertEquals(5f, from.y, 0.001f)
        assertEquals(10f, to.x, 0.001f)
        assertEquals(5f, to.y, 0.001f)
    }

    @Test
    fun ticksOnRoute_followTimeBetweenThePinsThatBracketThem() {
        val pins = listOf(
            WalkPin(placedAtMs = 10_000L, xFraction = 0f, yFraction = 0f),
            WalkPin(placedAtMs = 70_000L, xFraction = 1f, yFraction = 1f)
        )

        val ticks = WalkRoute.ticksOnRoute(pins, zoneStartMs = 0L, zoneEndMs = 90_000L)

        assertEquals(listOf(30_000L, 60_000L), ticks.map { it.atMs })
        assertEquals(1f / 3f, ticks[0].point.x, 0.001f)
        assertEquals(1f / 3f, ticks[0].point.y, 0.001f)
        assertEquals(5f / 6f, ticks[1].point.x, 0.001f)
    }

    @Test
    fun zoomAbout_keepsThePointUnderTheFingers() {
        val camera = WalkRoute.zoomAbout(
            viewWidth = 100f,
            viewHeight = 100f,
            imageWidth = 100f,
            imageHeight = 100f,
            camera = PlanCamera(),
            centroidX = 0f,
            centroidY = 0f,
            zoomFactor = 2f
        )
        assertEquals(2f, camera.zoom, 0.001f)
        val stillThere = WalkRoute.fractionInView(
            tapX = 0f,
            tapY = 0f,
            viewWidth = 100f,
            viewHeight = 100f,
            imageWidth = 100f,
            imageHeight = 100f,
            camera = camera
        )
        assertEquals(0f, stillThere!!.x, 0.001f)
        assertEquals(0f, stillThere.y, 0.001f)
    }

    @Test
    fun clampPan_afterNinetyDegreeRotateAllowsTheLongEdgesIntoView() {
        // Landscape plan in a square view: after 90°, former left/right need vertical pan.
        val zoomed = WalkRoute.zoomAbout(
            viewWidth = 100f,
            viewHeight = 100f,
            imageWidth = 200f,
            imageHeight = 100f,
            camera = PlanCamera(),
            centroidX = 50f,
            centroidY = 50f,
            zoomFactor = 2f
        )
        val rotated = WalkRoute.rotateAbout(
            viewWidth = 100f,
            viewHeight = 100f,
            imageWidth = 200f,
            imageHeight = 100f,
            camera = zoomed,
            centroidX = 50f,
            centroidY = 50f,
            deltaDegrees = 90f
        )
        val panned = WalkRoute.moveCentre(
            viewWidth = 100f,
            viewHeight = 100f,
            imageWidth = 200f,
            imageHeight = 100f,
            camera = rotated,
            deltaX = 0f,
            deltaY = 1_000f
        )
        assertEquals(90f, panned.rotationDegrees, 0.001f)
        // Unrotated clamp would keep panY at 0; AABB clamp must allow vertical travel.
        assert(panned.panY > 1f) { "expected vertical pan after 90° rotate, got ${panned.panY}" }
    }

    @Test
    fun insertOnNearestLeg_placesAMarkBetweenTwoPins() {
        val pins = listOf(
            WalkPin(placedAtMs = 0L, xFraction = 0f, yFraction = 0.5f),
            WalkPin(placedAtMs = 1_000L, xFraction = 1f, yFraction = 0.5f)
        )
        val insert = WalkRoute.insertOnNearestLeg(
            pins = pins,
            tapX = 50f,
            tapY = 50f,
            viewWidth = 100f,
            viewHeight = 100f,
            imageWidth = 100f,
            imageHeight = 100f,
            camera = PlanCamera(),
            maxDistancePx = 20f
        )
        assertEquals(1, insert!!.insertBeforeIndex)
        assertEquals(0.5f, insert.pin.xFraction, 0.02f)
        assertEquals(0.5f, insert.pin.yFraction, 0.02f)
        assertEquals(500L, insert.pin.placedAtMs)
    }

    @Test
    fun rotateAbout_keepsThePointUnderTheFingers() {
        val start = PlanCamera(zoom = 2f)
        val under = WalkRoute.fractionInView(
            tapX = 25f,
            tapY = 50f,
            viewWidth = 100f,
            viewHeight = 100f,
            imageWidth = 100f,
            imageHeight = 100f,
            camera = start
        )!!
        val camera = WalkRoute.rotateAbout(
            viewWidth = 100f,
            viewHeight = 100f,
            imageWidth = 100f,
            imageHeight = 100f,
            camera = start,
            centroidX = 25f,
            centroidY = 50f,
            deltaDegrees = 90f
        )
        assertEquals(90f, camera.rotationDegrees, 0.001f)
        val stillThere = WalkRoute.fractionInView(
            tapX = 25f,
            tapY = 50f,
            viewWidth = 100f,
            viewHeight = 100f,
            imageWidth = 100f,
            imageHeight = 100f,
            camera = camera
        )
        assertEquals(under.x, stillThere!!.x, 0.02f)
        assertEquals(under.y, stillThere.y, 0.02f)
    }

    @Test
    fun zoomIn_reachesTheCloserLimit() {
        val camera = WalkRoute.zoomAbout(
            viewWidth = 100f,
            viewHeight = 100f,
            imageWidth = 100f,
            imageHeight = 100f,
            camera = PlanCamera(),
            centroidX = 50f,
            centroidY = 50f,
            zoomFactor = 100f
        )
        assertEquals(WalkRoute.MAX_ZOOM, camera.zoom, 0.001f)
    }

    @Test
    fun zoomOut_stopsAtAFittedPicture() {
        val camera = WalkRoute.zoomAbout(
            viewWidth = 100f,
            viewHeight = 100f,
            imageWidth = 100f,
            imageHeight = 100f,
            camera = PlanCamera(zoom = 2f),
            centroidX = 50f,
            centroidY = 50f,
            zoomFactor = 0.1f
        )
        assertEquals(1f, camera.zoom, 0.001f)
        assertEquals(0f, camera.panX, 0.001f)
        assertEquals(0f, camera.panY, 0.001f)
    }

    @Test
    fun moveCentre_keepsAZoomedPlanCoveringTheView() {
        val camera = WalkRoute.moveCentre(
            viewWidth = 100f,
            viewHeight = 100f,
            imageWidth = 100f,
            imageHeight = 100f,
            camera = PlanCamera(zoom = 2f),
            deltaX = 1_000f,
            deltaY = -1_000f
        )
        assertEquals(50f, camera.panX, 0.001f)
        assertEquals(-50f, camera.panY, 0.001f)
    }

    @Test
    fun snapLeg_usesTheNearestHorizontalVerticalOrDiagonal() {
        val from = WalkPin(placedAtMs = 0L, xFraction = 0f, yFraction = 0f)
        val horizontal = WalkRoute.snapLeg(
            from = from,
            tap = ImagePoint(1f, 0.1f),
            imageWidth = 100f,
            imageHeight = 100f,
            mode = WalkSnapMode.Degrees45
        )
        assertEquals(1f, horizontal.x, 0.02f)
        assertEquals(0f, horizontal.y, 0.02f)

        val diagonal = WalkRoute.snapLeg(
            from = from,
            tap = ImagePoint(1f, 0.8f),
            imageWidth = 100f,
            imageHeight = 100f,
            mode = WalkSnapMode.Degrees45
        )
        assertEquals(diagonal.x, diagonal.y, 0.02f)

        val vertical = WalkRoute.snapLeg(
            from = from,
            tap = ImagePoint(0.15f, 1f),
            imageWidth = 100f,
            imageHeight = 100f,
            mode = WalkSnapMode.Degrees45
        )
        assertEquals(0f, vertical.x, 0.02f)
        assertEquals(1f, vertical.y, 0.02f)
    }

    @Test
    fun snapLeg_keepsADiagonalAtFortyFiveDegreesOnAWidePlan() {
        val snapped = WalkRoute.snapLeg(
            from = WalkPin(placedAtMs = 0L, xFraction = 0f, yFraction = 0f),
            tap = ImagePoint(0.5f, 0.8f),
            imageWidth = 200f,
            imageHeight = 100f,
            mode = WalkSnapMode.Degrees45
        )
        val pixelX = snapped.x * 200f
        val pixelY = snapped.y * 100f
        assertEquals(pixelX, pixelY, 1f)
    }

    @Test
    fun snapLeg_offLeavesTheTapWhereItIs() {
        val tap = ImagePoint(0.37f, 0.61f)
        val snapped = WalkRoute.snapLeg(
            from = WalkPin(placedAtMs = 0L, xFraction = 0f, yFraction = 0f),
            tap = tap,
            imageWidth = 100f,
            imageHeight = 100f,
            mode = WalkSnapMode.Off
        )
        assertEquals(tap.x, snapped.x, 0.0001f)
        assertEquals(tap.y, snapped.y, 0.0001f)
    }

    @Test
    fun snapLeg_twentyTwoPointFiveAllowsAShallowerDiagonal() {
        val from = WalkPin(placedAtMs = 0L, xFraction = 0f, yFraction = 0f)
        // About 22° in image pixels: 45° mode snaps flat; 22.5° mode keeps the shallow diagonal.
        val tap = ImagePoint(1f, 0.4f)
        val coarse = WalkRoute.snapLeg(from, tap, 100f, 100f, WalkSnapMode.Degrees45)
        val fine = WalkRoute.snapLeg(from, tap, 100f, 100f, WalkSnapMode.Degrees22_5)
        assertEquals(0f, coarse.y, 0.02f)
        val fineAngle = kotlin.math.atan2(fine.y.toDouble(), fine.x.toDouble())
        assertEquals(Math.PI / 8.0, fineAngle, 0.05)
    }

    @Test
    fun fractionInView_ignoresTheLetterboxAroundThePlan() {
        val center = WalkRoute.fractionInView(
            tapX = 50f,
            tapY = 50f,
            viewWidth = 100f,
            viewHeight = 100f,
            imageWidth = 200f,
            imageHeight = 100f
        )
        assertEquals(0.5f, center!!.x, 0.001f)
        assertEquals(0.5f, center.y, 0.001f)
        assertNull(
            WalkRoute.fractionInView(
                tapX = 50f,
                tapY = 10f,
                viewWidth = 100f,
                viewHeight = 100f,
                imageWidth = 200f,
                imageHeight = 100f
            )
        )
    }
}
