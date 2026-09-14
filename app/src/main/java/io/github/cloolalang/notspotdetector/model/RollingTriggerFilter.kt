package io.github.cloolalang.notspotdetector.model

/**
 * Rolling confirmation of a boolean trigger at one sample per measurement cycle (~1 s).
 *
 * When the filter is inactive, the latest reading is used immediately. When active, the new
 * state must persist for [RxssStateFilterSettings.holdSeconds] further matching samples
 * (the first disagreeing sample starts the wait). That is a rolling average of 1.0 over the
 * hold window — a single flicker resets the count.
 */
class RollingTriggerFilter {
    var confirmedActive: Boolean = false
        private set

    private val samples = ArrayDeque<Boolean>()
    private var pendingTarget: Boolean? = null
    private var pendingCount: Int = 0

    data class UpdateResult(
        val confirmedActive: Boolean,
        val transitioned: Boolean
    )

    fun update(rawActive: Boolean, settings: RxssStateFilterSettings): UpdateResult {
        val normalized = settings.normalized()
        if (!normalized.isActive) {
            samples.clear()
            resetHold()
            val transitioned = confirmedActive != rawActive
            confirmedActive = rawActive
            return UpdateResult(confirmedActive, transitioned)
        }

        samples.addLast(rawActive)
        val windowSize = normalized.windowSeconds.coerceAtLeast(1)
        while (samples.size > windowSize) {
            samples.removeFirst()
        }

        if (rawActive == confirmedActive) {
            resetHold()
            return UpdateResult(confirmedActive, transitioned = false)
        }

        val holdSeconds = normalized.holdSeconds(entering = rawActive)
        if (holdSeconds <= 0) {
            return adopt(rawActive)
        }

        if (pendingTarget != rawActive) {
            pendingTarget = rawActive
            pendingCount = 0
            return UpdateResult(confirmedActive, transitioned = false)
        }
        pendingCount++

        val recent = samples.takeLast(holdSeconds.coerceAtMost(samples.size))
        val unanimous = recent.isNotEmpty() && recent.all { it == rawActive }
        if (pendingCount < holdSeconds || !unanimous) {
            return UpdateResult(confirmedActive, transitioned = false)
        }
        return adopt(rawActive)
    }

    fun reset() {
        confirmedActive = false
        samples.clear()
        resetHold()
    }

    private fun adopt(rawActive: Boolean): UpdateResult {
        val previous = confirmedActive
        confirmedActive = rawActive
        resetHold()
        return UpdateResult(confirmedActive, transitioned = previous != rawActive)
    }

    private fun resetHold() {
        pendingTarget = null
        pendingCount = 0
    }
}
