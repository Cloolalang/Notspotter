package io.github.cloolalang.notspotdetector.model

object SpecialCellMatcher {

    const val RESELECT_HOLD_INTERVALS = 4L
    const val RESELECT_HOLD_MIN_MS = 3_000L
    const val RESELECT_HOLD_MAX_MS = 8_000L

    fun reselectHoldMs(passiveMeasurementIntervalMs: Long): Long {
        return (passiveMeasurementIntervalMs * RESELECT_HOLD_INTERVALS)
            .coerceIn(RESELECT_HOLD_MIN_MS, RESELECT_HOLD_MAX_MS)
    }

    fun match(
        stats: ConnectivityStats,
        catalog: SpecialCellCatalog,
        @Suppress("UNUSED_PARAMETER") fiveGFeaturesEnabled: Boolean = true,
        blankStaleIdentity: Boolean = false
    ): SpecialCellMatch? {
        if (catalog.isEmpty || blankStaleIdentity) return null
        if (!stats.cellIdentityPermissionGranted && stats.ltePci == null) {
            return null
        }

        val earfcn = stats.lteEarfcn
        val pci = stats.ltePci
        if (earfcn == null || pci == null) return null

        val matches = catalog.cells.filter { cell ->
            cell.rat == SpecialCellRat.G4 &&
                cell.channel == earfcn &&
                cell.pci == pci
        }
        if (matches.isEmpty()) return null
        // Camped/serving PLMN only — never the SIM home PLMN. A roaming SIM in limited
        // service still camps on the listed cell's EARFCN/PCI; its home PLMN would hide the hit.
        val campedPlmn = normalizePlmn(stats.plmn)
        val preferred = matches.firstOrNull { cell ->
            campedPlmn != null && normalizePlmn(cell.plmn) == campedPlmn
        } ?: matches.first()
        return SpecialCellMatch(preferred, SpecialCellLayer.LTE)
    }

    fun servingIdentitySummary(stats: ConnectivityStats): String? {
        val earfcn = stats.lteEarfcn
        val pci = stats.ltePci
        if (earfcn == null || pci == null) return null
        return "$earfcn/$pci"
    }

    fun hasServingIdentity(stats: ConnectivityStats): Boolean {
        return stats.lteEarfcn != null && stats.ltePci != null
    }

    internal fun normalizePlmn(raw: String?): String? {
        val digits = raw?.filter { it.isDigit() }.orEmpty()
        return digits.takeIf { it.length >= 5 }
    }
}
