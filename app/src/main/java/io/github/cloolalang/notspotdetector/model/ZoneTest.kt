package io.github.cloolalang.notspotdetector.model

/**
 * A zone test keeps KPI history from the moment it starts until it stops, and draws that
 * whole span on one scale so the bars compress as the run gets longer.
 */
object ZoneTest {
    /** Upper bound so a very long run cannot grow the tape lists without limit. */
    const val RETAIN_MS = 8L * 60L * 60L * 1000L

    fun jpegFileName(name: String, operator: String?): String =
        fileName(prefix = "Zone test", name = name, operator = operator)

    fun walkJpegFileName(name: String, operator: String?): String =
        fileName(prefix = "Zone walk", name = name, operator = operator)

    /**
     * Sample window for the zone-test RSRP and RSRQ tapes.
     * When [openForWholeTest] is on and the run has a start time, the window is the time
     * since that start. Otherwise the histogram sample window is used.
     */
    fun sampleWindowMs(
        openForWholeTest: Boolean,
        wallStartMs: Long?,
        nowMs: Long,
        sliderWindowMs: Long
    ): Long {
        if (!openForWholeTest || wallStartMs == null) return sliderWindowMs
        return (nowMs - wallStartMs).coerceAtLeast(0L)
    }

    private fun fileName(prefix: String, name: String, operator: String?): String {
        val safeName = safePart(name).ifBlank { "zone" }
        val operatorPart = operator?.let(::safePart)?.takeIf { it.isNotEmpty() }
        return if (operatorPart == null) {
            "$prefix - $safeName.jpg"
        } else {
            "$prefix - $safeName - $operatorPart.jpg"
        }
    }

    private fun safePart(value: String): String {
        return value
            .trim()
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .take(MAX_NAME_LENGTH)
    }

    private const val MAX_NAME_LENGTH = 60
}
