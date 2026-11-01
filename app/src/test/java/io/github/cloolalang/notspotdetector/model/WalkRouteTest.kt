package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
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
            zoom = camera.zoom,
            panX = camera.panX,
            panY = camera.panY
        )
        assertEquals(0f, stillThere!!.x, 0.001f)
        assertEquals(0f, stillThere.y, 0.001f)
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
            imageHeight = 100f
        )
        assertEquals(1f, horizontal.x, 0.02f)
        assertEquals(0f, horizontal.y, 0.02f)

        val diagonal = WalkRoute.snapLeg(
            from = from,
            tap = ImagePoint(1f, 0.8f),
            imageWidth = 100f,
            imageHeight = 100f
        )
        assertEquals(diagonal.x, diagonal.y, 0.02f)

        val vertical = WalkRoute.snapLeg(
            from = from,
            tap = ImagePoint(0.15f, 1f),
            imageWidth = 100f,
            imageHeight = 100f
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
            imageHeight = 100f
        )
        val pixelX = snapped.x * 200f
        val pixelY = snapped.y * 100f
        assertEquals(pixelX, pixelY, 1f)
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
