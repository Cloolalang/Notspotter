package io.github.cloolalang.notspotdetector.model

/**
 * Primary intracell dominance for the KPI history tape.
 * Each segment is the same gap the cell-metrics row shows
 * ([ConnectivityStats.primaryIntracellDominanceDb]). A new segment starts when that gap
 * changes. A blank metrics value is a grey segment. Low is under
 * [PRIMARY_LAYER_LOW_DOMINANCE_THRESHOLD_DB] and high is that threshold or more.
 */
object PrimaryIntracellDominanceHistory {
    /**
     * Latest cell-metrics dominance while monitoring.
     * Null when monitoring is stopped, so the tape holds still.
     * A null gap inside the visit is a blank metrics value (grey).
     */
    fun current(
        stats: ConnectivityStats,
        settings: PassiveSignalSettings = PassiveSignalSettings()
    ): IntracellDominanceVisit? {
        if (!stats.isMonitoring) return null
        return IntracellDominanceVisit(gapDb = stats.primaryIntracellDominanceDb(settings))
    }

    fun record(
        tape: IntracellDominanceTape,
        next: IntracellDominanceVisit?,
        nowMs: Long = 0L,
        retainMs: Long = MonitoringSettings.MAX_TAPE_WINDOW_MS
    ): IntracellDominanceTape {
        if (next == null) return tape
        if (tape.entries.lastOrNull()?.gapDb == next.gapDb) return tape
        val entry = IntracellDominanceSegment(gapDb = next.gapDb, startedAtMs = nowMs)
        return tape.copy(
            entries = TapeTimeline.retainForScale(tape.entries + entry, nowMs, retainMs) { it.startedAtMs }
        )
    }
}

/** [gapDb] is null for no signal or no intracell comparison. */
data class IntracellDominanceVisit(
    val gapDb: Int?
)

data class IntracellDominanceSegment(
    val gapDb: Int?,
    val startedAtMs: Long = 0L
)

data class IntracellDominanceTape(
    val entries: List<IntracellDominanceSegment> = emptyList()
)
