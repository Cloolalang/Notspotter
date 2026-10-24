package io.github.cloolalang.notspotdetector.model

/**
 * A single RSRP reading recorded for the histogram. [rsrpDbm] is null when no RSRP was available
 * at the time (e.g. a no-signal state) — these are still recorded so the histogram can show how
 * often "no signal" occurred within the window, rather than silently dropping them.
 * [rsrqDb] is the RSRQ from the same tick, when the modem reported one. 2G has none.
 */
data class RsrpSample(
    val timestampMs: Long,
    val rsrpDbm: Int?,
    /** Timing-advance one-way distance, when the modem reported it on this tick. */
    val distanceMetres: Int? = null,
    /** RSRQ in dB from the same tick. Null when the modem did not report one. */
    val rsrqDb: Int? = null
)
