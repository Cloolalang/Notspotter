package io.github.cloolalang.notspotdetector.model

/**
 * A single RSRP reading recorded for the histogram. [rsrpDbm] is null when no RSRP was available
 * at the time (e.g. a no-signal state) — these are still recorded so the histogram can show how
 * often "no signal" occurred within the window, rather than silently dropping them.
 */
data class RsrpSample(
    val timestampMs: Long,
    val rsrpDbm: Int?
)
