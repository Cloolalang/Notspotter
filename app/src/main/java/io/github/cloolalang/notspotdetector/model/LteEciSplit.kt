package io.github.cloolalang.notspotdetector.model

/**
 * Macro LTE split of the 28-bit ECI: 20-bit eNodeB ID and 8-bit logical cell ID.
 * eNodeB ID is `ECI / 256`. Logical cell ID is the remainder.
 */
data class LteEciSplit(
    val enbId: Int,
    val cellId: Int
) {
    companion object {
        fun fromEci(eci: Int?): LteEciSplit? {
            if (eci == null || eci !in 0..LTE_ECI_MAX) return null
            return LteEciSplit(
                enbId = eci / CELL_ID_SPAN,
                cellId = eci % CELL_ID_SPAN
            )
        }

        /** 8-bit logical cell ID, so the eNodeB ID is ECI divided by 256. */
        private const val CELL_ID_SPAN = 256

        /** LTE 28-bit ECI maximum (`CellIdentityLte.getCi()`). */
        private const val LTE_ECI_MAX = 0x0FFFFFFF
    }
}
