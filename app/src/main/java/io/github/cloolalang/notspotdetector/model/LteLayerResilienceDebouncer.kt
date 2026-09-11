package io.github.cloolalang.notspotdetector.model

/**
 * Requires [confirmationPolls] consecutive matching readings before flipping the confirmed
 * [LteLayerResilienceReading]. Mirrors [NoSignalDebouncer]'s confirmation logic, generalized to a
 * nullable data-class reading instead of a [Boolean], so a single flickering neighbour-cell poll
 * doesn't cause the displayed "4G layers detected" values to jump around. The whole reading is
 * debounced as one atomic unit (rather than debouncing each of its three numbers independently)
 * so the displayed primary/alternate values never disagree with each other mid-transition.
 */
class LteLayerResilienceDebouncer(
    private val confirmationPolls: Int = 2
) {
    var confirmedReading: LteLayerResilienceReading? = null
        private set

    private var pendingTarget: LteLayerResilienceReading? = null
    private var pendingCount: Int = 0
    private var pendingTargetSet: Boolean = false

    data class UpdateResult(
        val confirmedReading: LteLayerResilienceReading?,
        val transitioned: Boolean
    )

    fun update(rawReading: LteLayerResilienceReading?): UpdateResult {
        if (rawReading == confirmedReading) {
            pendingTargetSet = false
            pendingTarget = null
            pendingCount = 0
            return UpdateResult(confirmedReading, transitioned = false)
        }

        if (!pendingTargetSet || pendingTarget != rawReading) {
            pendingTargetSet = true
            pendingTarget = rawReading
            pendingCount = 1
        } else {
            pendingCount++
        }

        if (pendingCount < confirmationPolls) {
            return UpdateResult(confirmedReading, transitioned = false)
        }

        val previous = confirmedReading
        confirmedReading = rawReading
        pendingTargetSet = false
        pendingTarget = null
        pendingCount = 0
        return UpdateResult(confirmedReading, transitioned = previous != confirmedReading)
    }

    fun reset() {
        confirmedReading = null
        pendingTargetSet = false
        pendingTarget = null
        pendingCount = 0
    }
}
