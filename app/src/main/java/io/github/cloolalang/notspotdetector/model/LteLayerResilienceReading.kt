package io.github.cloolalang.notspotdetector.model

/**
 * "4G layers detected" — split into a primary-channel reading and an alternate-layer reading,
 * computed by [io.github.cloolalang.notspotdetector.network.CellularSignalReader] from
 * [android.telephony.TelephonyManager.getAllCellInfo], deduplicated/grouped by EARFCN and scoped
 * to the expected PLMN:
 *
 * - [primaryLayerCellCount]: number of detected cells (sectors) sharing the *same* EARFCN as the
 *   current serving/primary LTE channel — i.e. intra-channel resilience on the layer you're
 *   actually camped on.
 * - [alternateLayerCount]: number of *distinct* other EARFCNs detected (each a separate
 *   frequency layer/band the UE could potentially reselect to).
 * - [alternateLayerCellCount]: total number of detected cells across all of those alternate
 *   EARFCNs combined (i.e. [alternateLayerCount] layers may have multiple sectors each).
 *
 * Example: primary channel EARFCN 3501 (band 8) with 2 sectors, plus EARFCN 223 (band 1, 2
 * sectors) and EARFCN 2850 (band 7, 1 sector) as neighbours ⇒
 * `LteLayerResilienceReading(primaryLayerCellCount = 2, alternateLayerCount = 2, alternateLayerCellCount = 3)`.
 */
data class LteLayerResilienceReading(
    val primaryLayerCellCount: Int,
    val alternateLayerCount: Int,
    val alternateLayerCellCount: Int
)
