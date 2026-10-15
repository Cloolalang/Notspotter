package io.github.cloolalang.notspotdetector.network

import io.github.cloolalang.notspotdetector.model.PingSettings

/**
 * One ping cycle. A connect that succeeds on the RRC warm-up is discarded so radio wake-up
 * time does not inflate RTT. A warm-up that times out is a real miss: it counts toward packet
 * loss and ends the cycle so the next probe can start without waiting out the rest of the burst.
 */
internal class PingCycleAccumulator(
    skipWarmup: Boolean
) {
    var measuredSent: Int = 0
        private set
    var measuredFailed: Int = 0
        private set
    val rtts: List<Long>
        get() = rttResults
    var timedOut: Boolean = false
        private set

    private val rttResults = mutableListOf<Long>()
    private var warmupPending: Boolean = !skipWarmup

    /**
     * Records one probe. Returns true when the cycle should stop because this probe timed out.
     */
    fun onProbe(rttMs: Long?): Boolean {
        if (warmupPending) {
            warmupPending = false
            if (rttMs == null) {
                timedOut = true
                measuredSent = 1
                measuredFailed = 1
                return true
            }
            return false
        }

        measuredSent++
        if (rttMs == null) {
            timedOut = true
            measuredFailed++
            return true
        }
        rttResults.add(rttMs)
        return false
    }

    companion object {
        const val CONNECT_TIMEOUT_MS = 2_000
        /** Pause after a timed-out cycle. Successful cycles still wait the configured interval. */
        const val TIMEOUT_RETRY_DELAY_MS = 500L

        fun delayAfterCycle(timedOut: Boolean, testIntervalMs: Long): Long {
            return if (timedOut) TIMEOUT_RETRY_DELAY_MS else testIntervalMs
        }

        fun probeCount(pingsPerTest: Int, skipWarmup: Boolean): Int {
            val measured = pingsPerTest.coerceAtLeast(0)
            val warmup = if (skipWarmup) 0 else PingSettings.RRC_WARMUP_PINGS
            return measured + warmup
        }
    }
}
