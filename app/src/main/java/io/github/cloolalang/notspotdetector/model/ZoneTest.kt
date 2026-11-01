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
