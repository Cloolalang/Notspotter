package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RsrqBinHistoryTest {

    @Test
    fun classify_usesNinetyFivePercentOfTheLastThirtySeconds() {
        assertEquals(
            RsrqBin.ABOVE_15,
            RsrqBinHistory.classify(window(good = 29, poor = 1, poorDb = -20), nowMs = 29_000L, noSignal = false)
        )
        assertNull(
            RsrqBinHistory.classify(window(good = 28, poor = 2, poorDb = -20), nowMs = 29_000L, noSignal = false)
        )
        assertEquals(
            RsrqBin.FROM_17_TO_15,
            RsrqBinHistory.classify(window(good = 29, goodDb = -16, poor = 1, poorDb = -20), nowMs = 29_000L, noSignal = false)
        )
        assertEquals(
            RsrqBin.FROM_17_TO_15,
            RsrqBinHistory.classify(window(good = 30, goodDb = -15), nowMs = 29_000L, noSignal = false)
        )
        assertEquals(
            RsrqBin.FROM_17_TO_15,
            RsrqBinHistory.classify(window(good = 30, goodDb = -17), nowMs = 29_000L, noSignal = false)
        )
        assertEquals(
            RsrqBin.BELOW_17,
            RsrqBinHistory.classify(window(good = 1, goodDb = -10, poor = 29, poorDb = -20), nowMs = 29_000L, noSignal = false)
        )
    }

    @Test
    fun classify_ignoresSamplesOlderThanThirtySeconds() {
        val recent = List(20) { index ->
            RsrpSample(timestampMs = 31_000L + index * 1_000L, rsrpDbm = -90, rsrqDb = -10)
        }
        val stale = List(20) { index ->
            RsrpSample(timestampMs = index * 1_000L, rsrpDbm = -90, rsrqDb = -20)
        }
        assertEquals(
            RsrqBin.ABOVE_15,
            RsrqBinHistory.classify(stale + recent, nowMs = 50_000L, noSignal = false)
        )
    }

    @Test
    fun classify_samplesWithNoRsrqDoNotHoldTheBinBlank() {
        val missing = List(5) { index ->
            RsrpSample(timestampMs = index * 1_000L, rsrpDbm = -90, rsrqDb = null)
        }
        val fresh = List(3) { index ->
            RsrpSample(timestampMs = 5_000L + index * 1_000L, rsrpDbm = -90, rsrqDb = -10)
        }
        assertEquals(
            RsrqBin.ABOVE_15,
            RsrqBinHistory.classify(missing + fresh, nowMs = 8_000L, noSignal = false)
        )
        assertEquals(
            RsrqBin.NO_RSRQ,
            RsrqBinHistory.classify(missing, nowMs = 8_000L, noSignal = false)
        )
    }

    @Test
    fun classify_noSignalIsGreyEvenWhenTheWindowIsStrong() {
        assertEquals(
            RsrqBin.NO_SIGNAL,
            RsrqBinHistory.classify(window(good = 30), nowMs = 29_000L, noSignal = true)
        )
        assertNull(RsrqBinHistory.classify(emptyList(), nowMs = 0L, noSignal = false))
    }

    @Test
    fun record_keepsEventsInsideTheWindowAndHoldsUntilTheBinChanges() {
        var tape = RsrqBinTape()
        tape = RsrqBinHistory.record(tape, null)
        tape = RsrqBinHistory.record(tape, RsrqBin.ABOVE_15, nowMs = 1_000L)
        tape = RsrqBinHistory.record(tape, RsrqBin.ABOVE_15, nowMs = 2_000L)
        tape = RsrqBinHistory.record(tape, RsrqBin.NO_SIGNAL, nowMs = 3_000L)
        tape = RsrqBinHistory.record(tape, RsrqBin.FROM_17_TO_15, nowMs = 4_000L)
        tape = RsrqBinHistory.record(tape, RsrqBin.BELOW_17, nowMs = 5_000L)
        tape = RsrqBinHistory.record(tape, RsrqBin.NO_RSRQ, nowMs = 6_000L)

        assertEquals(
            listOf(
                RsrqBin.ABOVE_15,
                RsrqBin.NO_SIGNAL,
                RsrqBin.FROM_17_TO_15,
                RsrqBin.BELOW_17,
                RsrqBin.NO_RSRQ
            ),
            tape.entries.map { it.bin }
        )
        assertEquals(1_000L, tape.entries.first().startedAtMs)
    }

    private fun window(
        good: Int,
        goodDb: Int = -10,
        poor: Int = 0,
        poorDb: Int = -20
    ): List<RsrpSample> {
        return List(good + poor) { index ->
            val rsrq = if (index < good) goodDb else poorDb
            RsrpSample(timestampMs = index * 1_000L, rsrpDbm = -90, rsrqDb = rsrq)
        }
    }
}
