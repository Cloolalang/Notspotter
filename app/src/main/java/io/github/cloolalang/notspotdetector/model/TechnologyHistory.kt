package io.github.cloolalang.notspotdetector.model

import io.github.cloolalang.notspotdetector.network.CellularSignalReader

/**
 * Last few radio technologies for the level-histogram tape, under the primary-carrier tape.
 * A visit is kept while that technology stays current. The next different technology
 * (2G, 4G, 4G+, 5G, or 5G EN-DC) starts a new segment. 4G+ is LTE while the status-bar
 * icon is the plus form. A no-signal period is its own segment.
 */
object TechnologyHistory {
    const val VISIBLE = 4
    const val LABEL_4G_PLUS = "4G+"
    const val LABEL_NO_SIGNAL = "—"
    private const val RATE_WINDOW_MS = 60_000L

    /**
     * Milliseconds for the tape to travel one technology from right to left.
     * Null while the change rate is zero, so the tape holds still.
     */
    fun segmentDurationMs(changesPerMinute: Int): Long? {
        if (changesPerMinute <= 0) return null
        return (RATE_WINDOW_MS / changesPerMinute).coerceAtLeast(1L)
    }

    fun technologyFromStats(stats: ConnectivityStats): String? {
        if (isNoSignalPeriod(stats)) return LABEL_NO_SIGNAL
        val type = stats.radioAccessType?.takeIf { it.isNotBlank() } ?: return null
        if (type == CellularSignalReader.RADIO_4G && stats.ltePlusIcon) return LABEL_4G_PLUS
        return type
    }

    /** Debounced no-signal, dead zone, or complete no service while monitoring. */
    fun isNoSignalPeriod(stats: ConnectivityStats): Boolean {
        if (!stats.isMonitoring) return false
        return stats.noSignalActive || stats.isDeadzoneConfirmed() || stats.isCompleteNoService
    }

    /**
     * Share of the tape for each segment, oldest first. The last segment runs from
     * when it started to [nowMs], so the right-hand end is the technology in use now.
     */
    fun spanFractions(entries: List<TechnologySegment>, nowMs: Long): List<Float> {
        if (entries.isEmpty()) return emptyList()
        if (entries.size == 1) return listOf(1f)
        val durations = entries.mapIndexed { index, segment ->
            val endMs = if (index == entries.lastIndex) nowMs else entries[index + 1].startedAtMs
            (endMs - segment.startedAtMs).coerceAtLeast(0L)
        }
        val total = durations.sum()
        if (total <= 0L) return List(entries.size) { 1f / entries.size }
        return durations.map { it.toFloat() / total.toFloat() }
    }

    fun record(tape: TechnologyTape, next: String?, nowMs: Long = 0L): TechnologyTape {
        if (next == null) return tape
        if (tape.entries.lastOrNull()?.label == next) return tape
        val entry = TechnologySegment(label = next, startedAtMs = nowMs)
        return tape.copy(
            entries = TapeTimeline.retainForScale(tape.entries + entry, nowMs) { it.startedAtMs }
        )
    }
}

data class TechnologySegment(
    val label: String,
    val startedAtMs: Long = 0L
)

data class TechnologyTape(
    val entries: List<TechnologySegment> = emptyList()
)

/** How long ago a tape segment started, for the scale under a stationary tape. */
object TapeAge {
    fun amount(startedAtMs: Long, nowMs: Long): TapeAgeAmount {
        val ageMs = (nowMs - startedAtMs).coerceAtLeast(0L)
        val hours = ageMs / 3_600_000L
        if (hours >= 1L) return TapeAgeAmount(hours, TapeAgeUnit.HOURS)
        val minutes = ageMs / 60_000L
        if (minutes >= 1L) return TapeAgeAmount(minutes, TapeAgeUnit.MINUTES)
        return TapeAgeAmount(ageMs / 1_000L, TapeAgeUnit.SECONDS)
    }
}

enum class TapeAgeUnit {
    SECONDS,
    MINUTES,
    HOURS
}

data class TapeAgeAmount(
    val value: Long,
    val unit: TapeAgeUnit
)
