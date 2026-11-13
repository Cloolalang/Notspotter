package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CallResultHistoryTest {

    @Test
    fun current_followsTheLivePhaseThenTheLastResult() {
        assertNull(CallResultHistory.current(CallTestStatus()))
        assertEquals(
            CallResultBand.DIALING,
            CallResultHistory.current(CallTestStatus(running = true, phase = CallTestPhase.DIALING))?.band
        )
        assertEquals(
            CallResultBand.UP,
            CallResultHistory.current(CallTestStatus(running = true, phase = CallTestPhase.IN_CALL))?.band
        )
        assertEquals(
            CallResultBand.SUCCESS,
            CallResultHistory.current(
                CallTestStatus(phase = CallTestPhase.GAP, lastOutcome = CallResultBand.SUCCESS)
            )?.band
        )
        assertEquals(
            CallResultBand.FAIL,
            CallResultHistory.current(
                CallTestStatus(phase = CallTestPhase.IDLE, lastOutcome = CallResultBand.FAIL)
            )?.band
        )
    }

    @Test
    fun record_keepsOneSegmentUntilTheResultChanges() {
        val dialing = CallResultHistory.record(
            CallResultTape(),
            CallResultVisit(CallResultBand.DIALING),
            nowMs = 1_000L
        )
        val stillDialing = CallResultHistory.record(
            dialing,
            CallResultVisit(CallResultBand.DIALING),
            nowMs = 2_000L
        )
        val failed = CallResultHistory.record(
            stillDialing,
            CallResultVisit(CallResultBand.FAIL),
            nowMs = 3_000L
        )
        assertEquals(1, stillDialing.entries.size)
        assertEquals(listOf(CallResultBand.DIALING, CallResultBand.FAIL), failed.entries.map { it.band })
        assertEquals(3_000L, failed.entries.last().startedAtMs)
    }
}
