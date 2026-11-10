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

    /** KPI drawn on the saved walk trail. */
    const val WALK_KPI = "RSRP"

    /**
     * Operator, then the plotted KPI when there is one, then an underscore.
     * A walk is pre-filled as VodafoneRSRP_ so the place name is typed after it.
     */
    fun fileNamePrefix(operator: String?, kpi: String? = null): String {
        val name = operator?.trim()?.takeIf { it.isNotEmpty() }
        val kpiPart = kpi?.trim()?.takeIf { it.isNotEmpty() }
        return when {
            name != null && kpiPart != null -> "${name}${kpiPart}_"
            name != null -> "${name}_"
            kpiPart != null -> "${kpiPart}_"
            else -> ""
        }
    }

    /**
     * The part of [zoneName] after this phone's operator prefix.
     * That part is what a master shares. Each phone keeps its own operator.
     */
    fun sharedNameSuffix(zoneName: String, operator: String?, kpi: String? = null): String {
        val prefix = fileNamePrefix(operator, kpi)
        return if (prefix.isNotEmpty() && zoneName.startsWith(prefix)) {
            zoneName.removePrefix(prefix)
        } else {
            zoneName
        }
    }

    fun nameWithOperator(operator: String?, sharedSuffix: String, kpi: String? = null): String {
        return fileNamePrefix(operator, kpi) + sharedSuffix
    }

    /** Saved walk trail, such as VodafoneRSRP_lobby.jpg. */
    fun walkJpegFileName(name: String, operator: String?): String {
        val prefix = fileNamePrefix(operator, WALK_KPI)
        val trimmed = name.trim()
        val operatorOnly = fileNamePrefix(operator)
        val full = when {
            trimmed.isEmpty() -> prefix + "zone"
            prefix.isNotEmpty() && trimmed.startsWith(prefix) -> trimmed
            operatorOnly.isNotEmpty() && trimmed.startsWith(operatorOnly) ->
                prefix + trimmed.removePrefix(operatorOnly)
            else -> prefix + trimmed
        }
        return safePart(full).ifBlank { "RSRP_zone" } + ".jpg"
    }

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
