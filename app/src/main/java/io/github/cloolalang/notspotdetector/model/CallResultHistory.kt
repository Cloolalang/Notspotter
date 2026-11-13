package io.github.cloolalang.notspotdetector.model

/**
 * Call-test result for the KPI history tape.
 * Dialling and the time the call is up are their own colours. A finished attempt stays
 * green for a success or red for a failure until the next attempt starts.
 */
object CallResultHistory {
    fun current(status: CallTestStatus): CallResultVisit? {
        val band = when (status.phase) {
            CallTestPhase.DIALING -> CallResultBand.DIALING
            CallTestPhase.IN_CALL,
            CallTestPhase.WAITING_FOR_HANGUP -> CallResultBand.UP
            CallTestPhase.GAP,
            CallTestPhase.IDLE -> status.lastOutcome ?: return null
        }
        return CallResultVisit(band)
    }

    fun record(
        tape: CallResultTape,
        next: CallResultVisit?,
        nowMs: Long = 0L,
        retainMs: Long = MonitoringSettings.MAX_TAPE_WINDOW_MS
    ): CallResultTape {
        if (next == null) return tape
        if (tape.entries.lastOrNull()?.band == next.band) return tape
        val entry = CallResultSegment(band = next.band, startedAtMs = nowMs)
        return tape.copy(
            entries = TapeTimeline.retainForScale(tape.entries + entry, nowMs, retainMs) { it.startedAtMs }
        )
    }
}

enum class CallResultBand {
    DIALING,
    UP,
    SUCCESS,
    FAIL
}

data class CallResultVisit(
    val band: CallResultBand
)

data class CallResultSegment(
    val band: CallResultBand,
    val startedAtMs: Long = 0L
)

data class CallResultTape(
    val entries: List<CallResultSegment> = emptyList()
)
