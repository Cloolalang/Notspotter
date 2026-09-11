package io.github.cloolalang.notspotdetector.model

/**
 * Requires [confirmationPolls] consecutive matching readings before flipping the confirmed "4G
 * layer resilience" layer count — see [CellularRadioMetrics.lteLayerResilienceLayerCount]. Mirrors
 * [NoSignalDebouncer]'s confirmation logic, generalized to a nullable [Int] instead of a
 * [Boolean], so a single flickering neighbour-cell reading doesn't cause the displayed layer
 * count to jump around.
 */
class LteLayerResilienceDebouncer(
    private val confirmationPolls: Int = 2
) {
    var confirmedLayerCount: Int? = null
        private set

    private var pendingTarget: Int? = null
    private var pendingCount: Int = 0
    private var pendingTargetSet: Boolean = false

    data class UpdateResult(
        val confirmedLayerCount: Int?,
        val transitioned: Boolean
    )

    fun update(rawLayerCount: Int?): UpdateResult {
        if (rawLayerCount == confirmedLayerCount) {
            pendingTargetSet = false
            pendingTarget = null
            pendingCount = 0
            return UpdateResult(confirmedLayerCount, transitioned = false)
        }

        if (!pendingTargetSet || pendingTarget != rawLayerCount) {
            pendingTargetSet = true
            pendingTarget = rawLayerCount
            pendingCount = 1
        } else {
            pendingCount++
        }

        if (pendingCount < confirmationPolls) {
            return UpdateResult(confirmedLayerCount, transitioned = false)
        }

        val previous = confirmedLayerCount
        confirmedLayerCount = rawLayerCount
        pendingTargetSet = false
        pendingTarget = null
        pendingCount = 0
        return UpdateResult(confirmedLayerCount, transitioned = previous != confirmedLayerCount)
    }

    fun reset() {
        confirmedLayerCount = null
        pendingTargetSet = false
        pendingTarget = null
        pendingCount = 0
    }
}
