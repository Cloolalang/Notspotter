package io.github.cloolalang.notspotdetector.model

/**
 * Requires [confirmationPolls] consecutive matching readings before flipping the
 * confirmed no-signal state (both entering and leaving).
 */
class NoSignalDebouncer(
    private val confirmationPolls: Int = 2
) {
    var confirmedActive: Boolean = false
        private set

    private var pendingTarget: Boolean? = null
    private var pendingCount: Int = 0

    data class UpdateResult(
        val confirmedActive: Boolean,
        val transitioned: Boolean
    )

    fun update(rawActive: Boolean): UpdateResult {
        if (rawActive == confirmedActive) {
            pendingTarget = null
            pendingCount = 0
            return UpdateResult(confirmedActive, transitioned = false)
        }

        if (pendingTarget != rawActive) {
            pendingTarget = rawActive
            pendingCount = 1
        } else {
            pendingCount++
        }

        if (pendingCount < confirmationPolls) {
            return UpdateResult(confirmedActive, transitioned = false)
        }

        val previous = confirmedActive
        confirmedActive = rawActive
        pendingTarget = null
        pendingCount = 0
        return UpdateResult(confirmedActive, transitioned = previous != confirmedActive)
    }

    fun reset() {
        confirmedActive = false
        pendingTarget = null
        pendingCount = 0
    }
}
