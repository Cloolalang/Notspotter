package io.github.cloolalang.notspotdetector.model

/**
 * Which carriers fire the cell-reselect bell and voice.
 * [LTE_ONLY] is the default: an NR-only handover on EN-DC stays quiet.
 * Standalone 5G still announces, because the NR cell is the only carrier.
 */
enum class CellReselectAlertScope(val id: String) {
    LTE_ONLY("lte_only"),
    BOTH("both");

    companion object {
        val DEFAULT = LTE_ONLY

        fun fromId(id: String?): CellReselectAlertScope {
            if (id.isNullOrBlank()) return DEFAULT
            return entries.find { it.id == id } ?: DEFAULT
        }
    }
}
