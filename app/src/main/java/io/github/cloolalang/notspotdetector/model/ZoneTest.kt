package io.github.cloolalang.notspotdetector.model

/**
 * A zone test keeps KPI history from the moment it starts until it stops, and draws that
 * whole span on one scale so the bars compress as the run gets longer.
 */
object ZoneTest {
    /** Upper bound so a very long run cannot grow the tape lists without limit. */
    const val RETAIN_MS = 8L * 60L * 60L * 1000L

    fun jpegFileName(name: String, operator: String?): String {
        val trimmed = name.trim().ifBlank { "zone" }
        val safeName = trimmed
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .take(MAX_NAME_LENGTH)
        val operatorPart = operator
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.replace(Regex("[\\\\/:*?\"<>|]"), "_")
            ?.take(MAX_NAME_LENGTH)
        return if (operatorPart == null) {
            "Zone test - $safeName.jpg"
        } else {
            "Zone test - $safeName - $operatorPart.jpg"
        }
    }

    private const val MAX_NAME_LENGTH = 60
}
