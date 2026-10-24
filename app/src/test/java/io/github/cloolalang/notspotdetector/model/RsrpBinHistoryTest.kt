package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RsrpBinHistoryTest {

    @Test
    fun classify_usesNinetyFivePercentOfTheLastThirtySeconds() {
        assertEquals(
            RsrpBin.ABOVE_95,
            RsrpBinHistory.classify(window(strong = 29, weak = 1, weakDbm = -120), nowMs = 29_000L, noSignal = false)
        )
        assertNull(
            RsrpBinHistory.classify(window(strong = 28, weak = 2, weakDbm = -120), nowMs = 29_000L, noSignal = false)
        )
        assertEquals(
            RsrpBin.ABOVE_105,
            RsrpBinHistory.classify(window(strong = 29, strongDbm = -100, weak = 1, weakDbm = -120), nowMs = 29_000L, noSignal = false)
        )
        assertEquals(
            RsrpBin.ABOVE_115,
            RsrpBinHistory.classify(window(strong = 29, strongDbm = -110, weak = 1, weakDbm = -120), nowMs = 29_000L, noSignal = false)
        )
        assertEquals(
            RsrpBin.BELOW_115,
            RsrpBinHistory.classify(window(strong = 1, strongDbm = -90, weak = 29, weakDbm = -120), nowMs = 29_000L, noSignal = false)
        )
    }

    @Test
    fun classify_ignoresSamplesOlderThanThirtySeconds() {
        val recent = List(20) { index ->
            RsrpSample(timestampMs = 31_000L + index * 1_000L, rsrpDbm = -90)
        }
        val stale = List(20) { index ->
            RsrpSample(timestampMs = index * 1_000L, rsrpDbm = -120)
        }
        assertEquals(
            RsrpBin.ABOVE_95,
            RsrpBinHistory.classify(stale + recent, nowMs = 50_000L, noSignal = false)
        )
    }

    @Test
    fun classify_samplesWithNoRsrpDoNotHoldTheBinBlank() {
        val staleStart = List(5) { index ->
            RsrpSample(timestampMs = index * 1_000L, rsrpDbm = null)
        }
        val fresh = List(3) { index ->
            RsrpSample(timestampMs = 5_000L + index * 1_000L, rsrpDbm = -90)
        }
        assertEquals(
            RsrpBin.ABOVE_95,
            RsrpBinHistory.classify(staleStart + fresh, nowMs = 8_000L, noSignal = false)
        )
        assertNull(RsrpBinHistory.classify(staleStart, nowMs = 8_000L, noSignal = false))
    }

    @Test
    fun classify_noSignalIsGreyEvenWhenTheWindowIsStrong() {
        assertEquals(
            RsrpBin.NO_SIGNAL,
            RsrpBinHistory.classify(window(strong = 30), nowMs = 29_000L, noSignal = true)
        )
        assertNull(RsrpBinHistory.classify(emptyList(), nowMs = 0L, noSignal = false))
    }

    @Test
    fun record_keepsEventsInsideTheWindowAndHoldsUntilTheBinChanges() {
        var tape = RsrpBinTape()
        tape = RsrpBinHistory.record(tape, null)
        tape = RsrpBinHistory.record(tape, RsrpBin.ABOVE_95, nowMs = 1_000L)
        tape = RsrpBinHistory.record(tape, RsrpBin.ABOVE_95, nowMs = 2_000L)
        tape = RsrpBinHistory.record(tape, RsrpBin.NO_SIGNAL, nowMs = 3_000L)
        tape = RsrpBinHistory.record(tape, RsrpBin.ABOVE_105, nowMs = 4_000L)
        tape = RsrpBinHistory.record(tape, RsrpBin.ABOVE_115, nowMs = 5_000L)
        tape = RsrpBinHistory.record(tape, RsrpBin.BELOW_115, nowMs = 6_000L)

        assertEquals(
            listOf(
                RsrpBin.ABOVE_95,
                RsrpBin.NO_SIGNAL,
                RsrpBin.ABOVE_105,
                RsrpBin.ABOVE_115,
                RsrpBin.BELOW_115
            ),
            tape.entries.map { it.bin }
        )
        assertEquals(1_000L, tape.entries.first().startedAtMs)
    }

    private fun window(
        strong: Int,
        strongDbm: Int = -90,
        weak: Int = 0,
        weakDbm: Int = -120
    ): List<RsrpSample> {
        return List(strong + weak) { index ->
            val rsrp = if (index < strong) strongDbm else weakDbm
            RsrpSample(timestampMs = index * 1_000L, rsrpDbm = rsrp)
        }
    }
}
