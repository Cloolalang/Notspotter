package io.github.cloolalang.notspotdetector.model

/**
 * Timing-advance distance for the KPI history tape.
 * Each segment is the measured one-way distance, in metres. A new segment starts when that
 * measurement changes. No signal is a grey segment, the same condition as the other tapes.
 * A sample with no timing advance holds the last measurement.
 */
object DistanceHistory {
    const val VISIBLE = 4

    /**
     * Latest measurement at [nowMs]. [noSignal] wins, so the grey segment starts with the
     * other tapes. Null when there is no distance on the latest sample.
     */
    fun current(
        samples: List<RsrpSample>,
        nowMs: Long,
        noSignal: Boolean
    ): DistanceVisit? {
        if (noSignal) return DistanceVisit(metres = null)
        val latest = samples
            .filter { it.timestampMs <= nowMs }
            .maxByOrNull { it.timestampMs }
            ?: return null
        val metres = latest.distanceMetres ?: return null
        return DistanceVisit(metres = metres)
    }

    fun record(tape: DistanceTape, next: DistanceVisit?, nowMs: Long = 0L): DistanceTape {
        if (next == null) return tape
        if (tape.entries.lastOrNull()?.metres == next.metres) return tape
        val entry = DistanceSegment(metres = next.metres, startedAtMs = nowMs)
        return tape.copy(
            entries = TapeTimeline.retainForScale(tape.entries + entry, nowMs) { it.startedAtMs }
        )
    }
}

/** [metres] is null for a no-signal period. */
data class DistanceVisit(
    val metres: Int?
)

data class DistanceSegment(
    val metres: Int?,
    val startedAtMs: Long = 0L
)

data class DistanceTape(
    val entries: List<DistanceSegment> = emptyList()
)
