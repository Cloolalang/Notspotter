package io.github.cloolalang.notspotdetector.model

/**
 * One walk-debug line. [detail] is the sentence shown on the card and written to the file.
 * [suspect] marks a line that explains why the black page is not moving.
 */
data class PedestrianDebugSnapshot(
    val phase: String,
    val detail: String,
    val suspect: Boolean = false,
    /** Heading lines stay in the file. The on-screen line keeps the latest step or sample. */
    val banner: Boolean = true
) {
    val isSuspect: Boolean get() = suspect

    fun bannerLine(): String = "Walk debug: $detail"

    fun bodyForDedupe(): String = "$phase|$detail"

    fun logLine(timestamp: String): String = "$timestamp $phase $detail"
}
