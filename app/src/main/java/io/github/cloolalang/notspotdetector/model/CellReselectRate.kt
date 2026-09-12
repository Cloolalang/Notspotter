package io.github.cloolalang.notspotdetector.model

/**
 * Rolling count of serving-cell reselects in the last minute.
 *
 * A reselect is a PCI / EARFCN / NR-ARFCN / BSIC change after the cell-identity baseline
 * is ready, while camped (same events as RXSS 9 / VA-10). The first camp snapshot is not
 * counted. The rate is independent of whether cell-reselect voice is enabled.
 */
object CellReselectRate {
    const val WINDOW_MS = 60_000L

    fun record(
        timestampsMs: List<Long>,
        nowMs: Long,
        reselectOccurred: Boolean
    ): List<Long> {
        val pruned = prune(timestampsMs, nowMs)
        return if (reselectOccurred) pruned + nowMs else pruned
    }

    fun countPerMinute(timestampsMs: List<Long>, nowMs: Long): Int {
        return prune(timestampsMs, nowMs).size
    }

    private fun prune(timestampsMs: List<Long>, nowMs: Long): List<Long> {
        val cutoff = nowMs - WINDOW_MS
        return timestampsMs.filter { it > cutoff }
    }
}
