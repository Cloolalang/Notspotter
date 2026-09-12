package io.github.cloolalang.notspotdetector.model

/**
 * "4G layers detected" — split into a primary-channel reading and an alternate-layer reading,
 * computed by [io.github.cloolalang.notspotdetector.network.CellularSignalReader] from
 * [android.telephony.TelephonyManager.getAllCellInfo], deduplicated/grouped by EARFCN and scoped
 * to the expected PLMN:
 *
 * - [primaryLayerCellCount]: number of distinct detected cells (sectors) on the serving EARFCN,
 *   including PCI-only neighbours that omit a channel (treated as intra-frequency).
 * - [alternateLayerCount]: number of *distinct* other EARFCNs detected (each a separate
 *   frequency layer/band the UE could potentially reselect to).
 * - [alternateLayerCellCount]: total number of detected cells across all of those alternate
 *   EARFCNs combined (i.e. [alternateLayerCount] layers may have multiple sectors each).
 *
 * Example: primary channel EARFCN 3501 (band 8) with 2 sectors, plus EARFCN 223 (band 1, 2
 * sectors) and EARFCN 2850 (band 7, 1 sector) as neighbours ⇒
 * `LteLayerResilienceReading(primaryLayerCellCount = 2, alternateLayerCount = 2, alternateLayerCellCount = 3)`.
 *
 * - [primaryLayerDominanceDb]: "primary cell level dominance" — the RSRP gap in dB between the
 *   serving/primary cell and the next-strongest *other* sector sharing the same (primary) EARFCN
 *   (an intra-channel neighbour). A small gap means a nearby sector on the same channel is
 *   nearly as strong as the one currently camped on, so a reselection/handover to it is more
 *   plausible; see [primaryLayerDominance]. Null when there's no other sector detected on the
 *   primary EARFCN, or RSRP isn't available for the comparison — i.e. dominance is undefined
 *   without a competing intra-channel sector.
 */
data class LteLayerResilienceReading(
    val primaryLayerCellCount: Int,
    val alternateLayerCount: Int,
    val alternateLayerCellCount: Int,
    val primaryLayerDominanceDb: Int? = null
)

/** Gaps smaller than this (in dB) are considered "low" dominance — see [primaryLayerDominance]. */
const val PRIMARY_LAYER_LOW_DOMINANCE_THRESHOLD_DB = 6

enum class PrimaryLayerDominance {
    LOW,
    HIGH
}

/**
 * Categorizes [LteLayerResilienceReading.primaryLayerDominanceDb] against
 * [PRIMARY_LAYER_LOW_DOMINANCE_THRESHOLD_DB]: gaps below the threshold are [PrimaryLayerDominance.LOW]
 * (the primary sector's lead over the next-strongest intra-channel sector is thin), otherwise
 * [PrimaryLayerDominance.HIGH]. Null when [LteLayerResilienceReading.primaryLayerDominanceDb] is
 * null (no competing intra-channel sector detected).
 */
fun LteLayerResilienceReading.primaryLayerDominance(): PrimaryLayerDominance? {
    val gapDb = primaryLayerDominanceDb ?: return null
    return if (gapDb < PRIMARY_LAYER_LOW_DOMINANCE_THRESHOLD_DB) {
        PrimaryLayerDominance.LOW
    } else {
        PrimaryLayerDominance.HIGH
    }
}
