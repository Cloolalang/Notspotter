package io.github.cloolalang.notspotdetector.model

/**
 * One time scale for the history tapes. The same fraction is the same moment on
 * the technology tape and the EARFCN/PCI tape. Fraction 0 is the oldest moment
 * and fraction 1 is now, drawn on the right.
 */
object TapeTimeline {
    fun windowStartMs(segmentStartsMs: List<Long>): Long? = segmentStartsMs.minOrNull()

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
