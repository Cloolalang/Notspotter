package io.github.cloolalang.notspotdetector.model

/**
 * Rolling RSRQ bin for the tape under the RSRP bin tape.
 * A bin is the quality that at least 95% of samples from the last 30 seconds reach.
 * Better than −15 dB is green, −17 dB through −15 dB is yellow, and worse than −17 dB is red.
 * No signal is its own grey segment. A window that has samples but no RSRQ (2G) is grey too,
 * so the last LTE colour does not stay on screen.
 */
object RsrqBinHistory {
    const val WINDOW_MS = 30_000L
    const val MIN_PERCENT = 95
    /** Green when RSRQ is strictly better than this (greater than −15 dB). */
    const val GREEN_ABOVE_DB = -15
    /** Yellow from this value through [GREEN_ABOVE_DB]. Red is strictly worse. */
    const val YELLOW_MIN_DB = -17

    /**
     * Bin for [nowMs]. [noSignal] wins, so the grey segment starts with the other tapes.
     * Null when the window has no samples yet, or no band covers 95% of the RSRQ readings.
     * Samples with no RSRQ are left out of the percentage. When every sample in the window
     * lacks RSRQ, the bin is [RsrqBin.NO_RSRQ] instead of holding the previous colour.
     */
    fun classify(
        samples: List<RsrpSample>,
        nowMs: Long,
        noSignal: Boolean
    ): RsrqBin? {
        if (noSignal) return RsrqBin.NO_SIGNAL
        val cutoff = nowMs - WINDOW_MS
        var inWindow = 0
        var total = 0
        var aboveGreen = 0
        var atLeastYellow = 0
        var belowYellow = 0
        for (sample in samples) {
            if (sample.timestampMs < cutoff || sample.timestampMs > nowMs) continue
            inWindow++
            val rsrqDb = sample.rsrqDb ?: continue
            total++
            if (rsrqDb > GREEN_ABOVE_DB) aboveGreen++
            if (rsrqDb >= YELLOW_MIN_DB) atLeastYellow++
            if (rsrqDb < YELLOW_MIN_DB) belowYellow++
        }
        if (total == 0) return if (inWindow > 0) RsrqBin.NO_RSRQ else null
        return when {
            covers(aboveGreen, total) -> RsrqBin.ABOVE_15
            covers(atLeastYellow, total) -> RsrqBin.FROM_17_TO_15
            covers(belowYellow, total) -> RsrqBin.BELOW_17
            else -> null
        }
    }

    fun record(tape: RsrqBinTape, next: RsrqBin?, nowMs: Long = 0L): RsrqBinTape {
        if (next == null) return tape
        if (tape.entries.lastOrNull()?.bin == next) return tape
        val entry = RsrqBinSegment(bin = next, startedAtMs = nowMs)
        return tape.copy(
            entries = TapeTimeline.retainForScale(tape.entries + entry, nowMs) { it.startedAtMs }
        )
    }

    private fun covers(count: Int, total: Int): Boolean {
        return count * 100 >= total * MIN_PERCENT
    }
}

enum class RsrqBin {
    /** Better than −15 dB. Green. */
    ABOVE_15,
    /** −17 dB through −15 dB. Yellow. */
    FROM_17_TO_15,
    /** Worse than −17 dB. Red. */
    BELOW_17,
    NO_SIGNAL,
    /** Samples in the window, none of them with RSRQ. */
    NO_RSRQ
}

data class RsrqBinSegment(
    val bin: RsrqBin,
    val startedAtMs: Long = 0L
)

data class RsrqBinTape(
    val entries: List<RsrqBinSegment> = emptyList()
)
