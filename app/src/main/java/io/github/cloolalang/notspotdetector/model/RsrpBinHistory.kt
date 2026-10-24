package io.github.cloolalang.notspotdetector.model

/**
 * Rolling RSRP bin for the histogram tape under the technology tape.
 * A bin is the level that at least 95% of samples from the last 30 seconds clear.
 * No signal is its own grey segment, the same condition as the other history tapes.
 */
object RsrpBinHistory {
    const val VISIBLE = 4
    const val WINDOW_MS = 30_000L
    const val MIN_PERCENT = 95
    const val STRONG_DBM = -95
    const val MID_DBM = -105
    const val WEAK_DBM = -115

    /**
     * Bin for [nowMs]. [noSignal] wins, so the grey segment starts with the other tapes.
     * Null when the window has no RSRP readings, or no signal bin yet covers 95% of them.
     * Samples with no RSRP (a stale scan, or a poll before the first fresh scan) are left
     * out, so they cannot hold the tape blank until they age out of the window.
     */
    fun classify(
        samples: List<RsrpSample>,
        nowMs: Long,
        noSignal: Boolean
    ): RsrpBin? {
        if (noSignal) return RsrpBin.NO_SIGNAL
        val cutoff = nowMs - WINDOW_MS
        var total = 0
        var aboveStrong = 0
        var aboveMid = 0
        var aboveWeak = 0
        var belowWeak = 0
        for (sample in samples) {
            if (sample.timestampMs < cutoff || sample.timestampMs > nowMs) continue
            val rsrpDbm = sample.rsrpDbm ?: continue
            total++
            if (rsrpDbm > STRONG_DBM) aboveStrong++
            if (rsrpDbm > MID_DBM) aboveMid++
            if (rsrpDbm > WEAK_DBM) aboveWeak++
            if (rsrpDbm < WEAK_DBM) belowWeak++
        }
        if (total == 0) return null
        return when {
            covers(aboveStrong, total) -> RsrpBin.ABOVE_95
            covers(aboveMid, total) -> RsrpBin.ABOVE_105
            covers(aboveWeak, total) -> RsrpBin.ABOVE_115
            covers(belowWeak, total) -> RsrpBin.BELOW_115
            else -> null
        }
    }

    fun record(tape: RsrpBinTape, next: RsrpBin?, nowMs: Long = 0L): RsrpBinTape {
        if (next == null) return tape
        if (tape.entries.lastOrNull()?.bin == next) return tape
        val entry = RsrpBinSegment(bin = next, startedAtMs = nowMs)
        return tape.copy(
            entries = TapeTimeline.retainForScale(tape.entries + entry, nowMs) { it.startedAtMs }
        )
    }

    private fun covers(count: Int, total: Int): Boolean {
        return count * 100 >= total * MIN_PERCENT
    }
}

enum class RsrpBin {
    ABOVE_95,
    ABOVE_105,
    ABOVE_115,
    BELOW_115,
    NO_SIGNAL
}

data class RsrpBinSegment(
    val bin: RsrpBin,
    val startedAtMs: Long = 0L
)

data class RsrpBinTape(
    val entries: List<RsrpBinSegment> = emptyList()
)
