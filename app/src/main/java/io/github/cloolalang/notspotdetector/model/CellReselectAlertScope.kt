package io.github.cloolalang.notspotdetector.model

/**
 * Which carriers fire the cell-reselect bell and voice.
 * [BOTH] is the default: LTE and NR handovers each announce.
 * Standalone 5G still announces, because the NR cell is the only carrier.
 */
enum class CellReselectAlertScope(val id: String) {
    LTE_ONLY("lte_only"),
    BOTH("both");

    companion object {
        val DEFAULT = BOTH

        fun fromId(id: String?): CellReselectAlertScope {
            if (id.isNullOrBlank()) return DEFAULT
            return entries.find { it.id == id } ?: DEFAULT
        }
    }
}
