package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DistanceHistoryTest {

    @Test
    fun current_isTheLatestMeasurement() {
        val samples = listOf(
            RsrpSample(timestampMs = 1_000L, rsrpDbm = -90, distanceMetres = 200),
            RsrpSample(timestampMs = 2_000L, rsrpDbm = -90, distanceMetres = 469),
            RsrpSample(timestampMs = 3_000L, rsrpDbm = -95, distanceMetres = 8_000)
        )

        assertEquals(DistanceVisit(469), DistanceHistory.current(samples, nowMs = 2_500L, noSignal = false))
        assertEquals(DistanceVisit(8_000), DistanceHistory.current(samples, nowMs = 3_000L, noSignal = false))
    }

    @Test
    fun current_holdsWhenTheLatestSampleHasNoDistance() {
        val samples = listOf(
            RsrpSample(timestampMs = 1_000L, rsrpDbm = -90, distanceMetres = 469),
            RsrpSample(timestampMs = 2_000L, rsrpDbm = -90, distanceMetres = null)
        )

        assertNull(DistanceHistory.current(samples, nowMs = 2_000L, noSignal = false))
        assertNull(DistanceHistory.current(emptyList(), nowMs = 0L, noSignal = false))
    }

    @Test
    fun current_noSignalIsGreyEvenWhenADistanceIsPresent() {
        val samples = listOf(
            RsrpSample(timestampMs = 1_000L, rsrpDbm = -90, distanceMetres = 200)
        )

        assertEquals(
            DistanceVisit(metres = null),
            DistanceHistory.current(samples, nowMs = 1_000L, noSignal = true)
        )
    }

    @Test
    fun record_keepsEventsInsideTheWindowAndHoldsUntilTheMeasurementChanges() {
        var tape = DistanceTape()
        tape = DistanceHistory.record(tape, null)
        tape = DistanceHistory.record(tape, DistanceVisit(200), nowMs = 1_000L)
        tape = DistanceHistory.record(tape, DistanceVisit(200), nowMs = 2_000L)
        tape = DistanceHistory.record(tape, DistanceVisit(null), nowMs = 3_000L)
        tape = DistanceHistory.record(tape, DistanceVisit(800), nowMs = 4_000L)
        tape = DistanceHistory.record(tape, DistanceVisit(2_000), nowMs = 5_000L)
        tape = DistanceHistory.record(tape, DistanceVisit(4_000), nowMs = 6_000L)

        assertEquals(listOf(200, null, 800, 2_000, 4_000), tape.entries.map { it.metres })
        assertEquals(1_000L, tape.entries.first().startedAtMs)
    }
}
