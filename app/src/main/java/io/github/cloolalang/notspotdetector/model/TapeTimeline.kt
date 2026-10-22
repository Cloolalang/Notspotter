package io.github.cloolalang.notspotdetector.model

/**
 * One fixed time scale for the history tapes. The right edge is now and the left
 * edge is now minus the chosen window, on every tape. The window does not shrink
 * or grow to fit the events. Fraction 0 is the left edge and fraction 1 is now.
 */
object TapeTimeline {
    fun windowStartMs(nowMs: Long, windowMs: Long): Long = nowMs - windowMs.coerceAtLeast(0L)

    /**
     * Keep the segment that still covers the left edge of the longest window, and
     * every segment after it. Older segments have already rolled off.
     */
    fun <T> retainForScale(
        entries: List<T>,
        nowMs: Long,
        retainMs: Long = MonitoringSettings.MAX_TAPE_WINDOW_MS,
        startedAtMs: (T) -> Long
    ): List<T> {
        if (entries.isEmpty()) return entries
        val cutoff = nowMs - retainMs
        val overlap = entries.indexOfLast { startedAtMs(it) <= cutoff }
        if (overlap <= 0) return entries
        return entries.drop(overlap)
    }

    fun placements(
        segmentStartsMs: List<Long>,
        windowStartMs: Long,
        nowMs: Long
    ): List<TapePlacement> {
        if (segmentStartsMs.isEmpty()) return emptyList()
        val spanMs = nowMs - windowStartMs
        if (spanMs <= 0L) {
            return segmentStartsMs.mapIndexed { index, _ ->
                if (index == segmentStartsMs.lastIndex) {
                    TapePlacement(startFraction = 0f, endFraction = 1f)
                } else {
                    TapePlacement(startFraction = 0f, endFraction = 0f)
                }
            }
        }
        val span = spanMs.toFloat()
        return segmentStartsMs.mapIndexed { index, startMs ->
            val endMs = if (index == segmentStartsMs.lastIndex) nowMs else segmentStartsMs[index + 1]
            val startFraction = ((startMs - windowStartMs).coerceAtLeast(0L).toFloat() / span)
                .coerceIn(0f, 1f)
            val endFraction = ((endMs - windowStartMs).coerceAtLeast(0L).toFloat() / span)
                .coerceIn(startFraction, 1f)
            TapePlacement(startFraction = startFraction, endFraction = endFraction)
        }
    }
}

data class TapePlacement(
    val startFraction: Float,
    val endFraction: Float
)
