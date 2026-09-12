package io.github.cloolalang.notspotdetector.model

/**
 * One LTE sector from [android.telephony.TelephonyManager.getAllCellInfo].
 *
 * Neighbours often omit EARFCN and MCC/MNC: Android reports PCI (and sometimes RSRP) only.
 * Intra-frequency neighbours are on the serving EARFCN — a missing channel is not a missing cell.
 */
data class DetectedLteCell(
    val earfcn: Int? = null,
    val pci: Int? = null,
    val rsrpDbm: Int? = null,
    val isRegistered: Boolean = false
)

/**
 * Builds the primary / alternate layer reading from detected LTE cells.
 *
 * Idle-mode inter-frequency measurement is still limited by 3GPP TS 36.304 (a strong serving
 * cell often suppresses other EARFCNs — expected UE battery-save). Intra-frequency neighbours
 * should still appear on the primary layer.
 */
object LteLayerResilience {

    /**
     * Neighbour survey filter — looser than serving-cell identity selection.
     *
     * Serving-cell logic drops PLMN-unknown cells once any LTE cell matches the expected PLMN.
     * Neighbours commonly have a blank MCC/MNC, so that rule hides the intra-frequency set.
     * Count MATCH and UNKNOWN; drop unregistered hard mismatches (other operator / other SIM).
     */
    fun shouldCountNeighbour(
        isPlmnMismatch: Boolean,
        isRegistered: Boolean,
        acceptRegisteredPlmnMismatch: Boolean
    ): Boolean {
        if (!isPlmnMismatch) return true
        return isRegistered && acceptRegisteredPlmnMismatch
    }

    fun fromDetectedCells(
        cells: List<DetectedLteCell>,
        primaryEarfcn: Int?,
        primaryPci: Int? = null
    ): LteLayerResilienceReading {
        val usable = cells.filter { it.earfcn != null || it.pci != null }
        val resolvedPrimary = resolvePrimaryEarfcn(usable, primaryEarfcn, primaryPci)
        val unique = usable.distinctBy { cell ->
            val layer = effectiveEarfcn(cell, resolvedPrimary)
            if (cell.pci != null) {
                layer to cell.pci
            } else {
                layer to cell.rsrpDbm
            }
        }

        val byLayer = unique.groupBy { effectiveEarfcn(it, resolvedPrimary) }
        val primaryCells = if (resolvedPrimary != null) {
            byLayer[resolvedPrimary].orEmpty()
        } else {
            unique.filter { it.earfcn == null }
        }

        val primaryCount = when {
            primaryCells.isNotEmpty() -> primaryCells.size
            resolvedPrimary != null || primaryPci != null -> 1
            else -> 0
        }

        val alternate = byLayer.filterKeys { it != null && it != resolvedPrimary }

        return LteLayerResilienceReading(
            primaryLayerCellCount = primaryCount,
            alternateLayerCount = alternate.size,
            alternateLayerCellCount = alternate.values.sumOf { it.size },
            primaryLayerDominanceDb = dominanceDb(primaryCells)
        )
    }

    private fun resolvePrimaryEarfcn(
        cells: List<DetectedLteCell>,
        primaryEarfcn: Int?,
        primaryPci: Int?
    ): Int? {
        if (primaryEarfcn != null) return primaryEarfcn
        cells.firstOrNull { it.isRegistered && it.earfcn != null }?.earfcn?.let { return it }
        if (primaryPci != null) {
            cells.firstOrNull { it.pci == primaryPci && it.earfcn != null }?.earfcn?.let { return it }
        }
        return null
    }

    private fun effectiveEarfcn(cell: DetectedLteCell, primaryEarfcn: Int?): Int? {
        return cell.earfcn ?: primaryEarfcn
    }

    private fun dominanceDb(primaryCells: List<DetectedLteCell>): Int? {
        if (primaryCells.size < 2) return null
        val serving = primaryCells.firstOrNull { it.isRegistered }
            ?: primaryCells.maxByOrNull { it.rsrpDbm ?: Int.MIN_VALUE }
        val servingRsrp = serving?.rsrpDbm ?: return null
        val nextHighestRsrp = primaryCells
            .filter { it !== serving }
            .mapNotNull { it.rsrpDbm }
            .maxOrNull()
            ?: return null
        return servingRsrp - nextHighestRsrp
    }
}
