package io.github.cloolalang.notspotdetector.model

data class CallTestSettings(
    val phoneNumber: String = "",
    val callDurationSec: Int = DEFAULT_DURATION_SEC,
    val delayBetweenCallsSec: Int = DEFAULT_DELAY_SEC,
    /** When true, a call test that is still running stops after [MASTER_TIMEOUT_MS]. */
    val masterTimeoutEnabled: Boolean = DEFAULT_MASTER_TIMEOUT_ENABLED,
    /** When true, a running call test silences the call and mutes the microphone. */
    val muteCallAudio: Boolean = DEFAULT_MUTE_CALL_AUDIO
) {
    fun normalized(): CallTestSettings {
        return copy(
            phoneNumber = phoneNumber.take(MAX_NUMBER_CHARS),
            callDurationSec = callDurationSec.coerceIn(MIN_SECONDS, MAX_SECONDS),
            delayBetweenCallsSec = delayBetweenCallsSec.coerceIn(MIN_SECONDS, MAX_SECONDS)
        )
    }

    companion object {
        const val MIN_SECONDS = 5
        const val MAX_SECONDS = 120
        const val DEFAULT_DURATION_SEC = 15
        const val DEFAULT_DELAY_SEC = 10
        const val DEFAULT_MASTER_TIMEOUT_ENABLED = true
        const val DEFAULT_MUTE_CALL_AUDIO = false
        const val MASTER_TIMEOUT_MINUTES = 30
        const val MASTER_TIMEOUT_MS = MASTER_TIMEOUT_MINUTES * 60 * 1000L
        const val MAX_NUMBER_CHARS = 40
        const val SETUP_TIMEOUT_MS = 45_000L
        const val HANGUP_WAIT_MS = 20_000L
    }
}

enum class CallTestPhase {
    IDLE,
    DIALING,
    IN_CALL,
    GAP,
    WAITING_FOR_HANGUP
}

data class CallTestStatus(
    val running: Boolean = false,
    val phase: CallTestPhase = CallTestPhase.IDLE,
    val attempts: Int = 0,
    val successes: Int = 0,
    val failures: Int = 0,
    /** True when the phone will not report call state, so the test cannot score calls. */
    val blocked: Boolean = false,
    /** Set once an attempt finishes. The KPI tape holds this colour between calls. */
    val lastOutcome: CallResultBand? = null,
    /** True when Android has not accepted this app as the phone app, so it cannot hang up. */
    val needsDialer: Boolean = false,
    /** True when this phone still needs permission to place a call. */
    val needsCallPermission: Boolean = false
) {
    val successRatePercent: Int
        get() = CallTestScore.ratePercent(successes, attempts)
}

object CallTestScore {
    fun ratePercent(successes: Int, attempts: Int): Int {
        if (attempts <= 0) return 0
        return (successes.coerceAtLeast(0) * 100) / attempts
    }

    /**
     * A number the dialer can place. Spaces, dashes, and brackets are dropped.
     * A leading + is kept. Returns null when the text is not a dialable number.
     */
    fun dialableNumber(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        val builder = StringBuilder()
        for (char in trimmed) {
            when {
                char.isDigit() || char == '*' || char == '#' -> builder.append(char)
                char == '+' && builder.isEmpty() -> builder.append(char)
                char == ' ' || char == '-' || char == '(' || char == ')' -> Unit
                else -> return null
            }
        }
        if (builder.count { it.isDigit() } < 3) return null
        return builder.toString()
    }
}
