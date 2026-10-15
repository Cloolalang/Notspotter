package io.github.cloolalang.notspotdetector.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PingCycleAccumulatorTest {

    @Test
    fun successfulCycleDiscardsWarmupRtt() {
        val cycle = PingCycleAccumulator(skipWarmup = false)
        assertFalse(cycle.onProbe(400L))
        assertFalse(cycle.onProbe(40L))
        assertFalse(cycle.onProbe(50L))

        assertEquals(2, cycle.measuredSent)
        assertEquals(0, cycle.measuredFailed)
        assertEquals(listOf(40L, 50L), cycle.rtts)
        assertFalse(cycle.timedOut)
    }

    @Test
    fun warmupTimeoutCountsAsLossAndStopsTheCycle() {
        val cycle = PingCycleAccumulator(skipWarmup = false)
        assertTrue(cycle.onProbe(null))

        assertEquals(1, cycle.measuredSent)
        assertEquals(1, cycle.measuredFailed)
        assertTrue(cycle.rtts.isEmpty())
        assertTrue(cycle.timedOut)
    }

    @Test
    fun measuredTimeoutStopsRemainingPings() {
        val cycle = PingCycleAccumulator(skipWarmup = false)
        assertFalse(cycle.onProbe(80L))
        assertFalse(cycle.onProbe(45L))
        assertTrue(cycle.onProbe(null))

        assertEquals(2, cycle.measuredSent)
        assertEquals(1, cycle.measuredFailed)
        assertEquals(listOf(45L), cycle.rtts)
        assertTrue(cycle.timedOut)
    }

    @Test
    fun retryAfterTimeoutSkipsWarmup() {
        val cycle = PingCycleAccumulator(skipWarmup = true)
        assertTrue(cycle.onProbe(null))

        assertEquals(1, cycle.measuredSent)
        assertEquals(1, cycle.measuredFailed)
        assertTrue(cycle.timedOut)
        assertEquals(2, PingCycleAccumulator.probeCount(pingsPerTest = 2, skipWarmup = true))
    }

    @Test
    fun successfulRetryStillMeasuresEveryConfiguredPing() {
        val cycle = PingCycleAccumulator(skipWarmup = true)
        assertFalse(cycle.onProbe(30L))
        assertFalse(cycle.onProbe(32L))

        assertEquals(2, cycle.measuredSent)
        assertEquals(0, cycle.measuredFailed)
        assertEquals(listOf(30L, 32L), cycle.rtts)
        assertFalse(cycle.timedOut)
    }

    @Test
    fun timeoutUsesShortRetryAndSuccessKeepsTheTestInterval() {
        assertEquals(500L, PingCycleAccumulator.delayAfterCycle(timedOut = true, testIntervalMs = 5_000L))
        assertEquals(5_000L, PingCycleAccumulator.delayAfterCycle(timedOut = false, testIntervalMs = 5_000L))
        assertEquals(3, PingCycleAccumulator.probeCount(pingsPerTest = 2, skipWarmup = false))
    }
}
