package io.github.cloolalang.notspotdetector.model

/**
 * Rolling confirmation filter for an RXSS trigger state.
 *
 * [windowSeconds] is 0–10 at the 1 Hz measurement cycle. **0** (or [enabled] off) applies no
 * filter. **1** waits one extra reading (~1 s) before the new state is adopted. Balance splits
 * that wait between **entry** (higher %) and **exit** (lower %).
 */
data class RxssStateFilterSettings(
    val enabled: Boolean = false,
    val windowSeconds: Int = DEFAULT_WINDOW_SECONDS,
    val balancePercent: Int = DEFAULT_BALANCE_PERCENT
) {
    fun normalized(): RxssStateFilterSettings {
        return copy(
            windowSeconds = windowSeconds.coerceIn(MIN_WINDOW_SECONDS, MAX_WINDOW_SECONDS),
            balancePercent = balancePercent.coerceIn(MIN_BALANCE_PERCENT, MAX_BALANCE_PERCENT)
        )
    }

    /** True when the filter holds the new state instead of passing the latest reading through. */
    val isActive: Boolean
        get() = enabled && windowSeconds > 0

    /**
     * Seconds the new trigger state must persist before it is adopted.
     * **0** means adopt on the current reading (instant that direction).
     */
    fun holdSeconds(entering: Boolean): Int {
        if (!isActive) return 0
        if (balancePercent == DEFAULT_BALANCE_PERCENT) return windowSeconds
        val entrySeconds = (windowSeconds * balancePercent + 50) / 100
        val exitSeconds = windowSeconds - entrySeconds
        return if (entering) entrySeconds else exitSeconds
    }

    companion object {
        const val MIN_WINDOW_SECONDS = 0
        const val MAX_WINDOW_SECONDS = 10
        const val DEFAULT_WINDOW_SECONDS = 0
        const val MIN_BALANCE_PERCENT = 0
        const val MAX_BALANCE_PERCENT = 100
        const val DEFAULT_BALANCE_PERCENT = 50

        val INACTIVE: RxssStateFilterSettings = RxssStateFilterSettings()

        /** Preserves the former two-poll no-signal debounce (~1 s at 1 Hz, equal enter/leave). */
        val DEFAULT_NO_SIGNAL: RxssStateFilterSettings = RxssStateFilterSettings(
            enabled = true,
            windowSeconds = 1,
            balancePercent = DEFAULT_BALANCE_PERCENT
        )
    }
}
