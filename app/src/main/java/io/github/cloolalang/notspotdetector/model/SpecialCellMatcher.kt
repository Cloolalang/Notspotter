package io.github.cloolalang.notspotdetector.model

object SpecialCellMatcher {

    fun match(
        stats: ConnectivityStats,
        catalog: SpecialCellCatalog,
        fiveGFeaturesEnabled: Boolean = true,
        blankStaleIdentity: Boolean = false
    ): SpecialCellMatch? {
        if (catalog.isEmpty || blankStaleIdentity) return null
        if (!stats.cellIdentityPermissionGranted && stats.ltePci == null && stats.nrPci == null &&
            stats.gsmBsic == null
        ) {
            return null
        }

        val candidates = buildList {
            if (stats.lteEarfcn != null && stats.ltePci != null) {
                add(Candidate(SpecialCellRat.G4, stats.lteEarfcn, stats.ltePci, SpecialCellLayer.LTE))
            }
            if (fiveGFeaturesEnabled && stats.nrEarfcn != null && stats.nrPci != null) {
                add(Candidate(SpecialCellRat.G5, stats.nrEarfcn, stats.nrPci, SpecialCellLayer.NR))
            }
            if (stats.gsmEarfcn != null && stats.gsmBsic != null) {
                add(Candidate(SpecialCellRat.G2, stats.gsmEarfcn, stats.gsmBsic, SpecialCellLayer.GSM))
            }
        }
        if (candidates.isEmpty()) return null

        val reportedPlmns = listOf(stats.plmn, stats.homePlmn)
        for (candidate in candidates) {
            val matches = catalog.cells.filter { cell ->
                cell.rat == candidate.rat &&
                    cell.channel == candidate.channel &&
                    cell.pci == candidate.pci &&
                    plmnAllows(cell.plmn, reportedPlmns)
            }
            if (matches.isEmpty()) continue
            val preferred = matches.firstOrNull { cell ->
                !normalizePlmn(cell.plmn).isNullOrBlank() &&
                    reportedPlmns.any { normalizePlmn(it) == normalizePlmn(cell.plmn) }
            } ?: matches.first()
            return SpecialCellMatch(preferred, candidate.layer)
        }
        return null
    }

    fun servingIdentitySummary(stats: ConnectivityStats): String? {
        val parts = buildList {
            if (stats.lteEarfcn != null && stats.ltePci != null) {
                add("4G ${stats.lteEarfcn}/${stats.ltePci}")
            }
            if (stats.nrEarfcn != null && stats.nrPci != null) {
                add("5G ${stats.nrEarfcn}/${stats.nrPci}")
            }
            if (stats.gsmEarfcn != null && stats.gsmBsic != null) {
                add("2G ${stats.gsmEarfcn}/${stats.gsmBsic}")
            }
        }
        return parts.takeIf { it.isNotEmpty() }?.joinToString(", ")
    }

    fun hasServingIdentity(stats: ConnectivityStats): Boolean {
        return (stats.lteEarfcn != null && stats.ltePci != null) ||
            (stats.nrEarfcn != null && stats.nrPci != null) ||
            (stats.gsmEarfcn != null && stats.gsmBsic != null)
    }

    /**
     * Apply a CSV PLMN filter only when the phone also reported a PLMN.
     * Missing or oddly formatted telephony PLMN must not hide a channel/PCI hit.
     */
    internal fun plmnAllows(cellPlmn: String?, reportedPlmns: List<String?>): Boolean {
        val wanted = normalizePlmn(cellPlmn) ?: return true
        val reported = reportedPlmns.mapNotNull(::normalizePlmn)
        if (reported.isEmpty()) return true
        return wanted in reported
    }

    internal fun normalizePlmn(raw: String?): String? {
        val digits = raw?.filter { it.isDigit() }.orEmpty()
        return digits.takeIf { it.length >= 5 }
    }

    private data class Candidate(
        val rat: SpecialCellRat,
        val channel: Int,
        val pci: Int,
        val layer: SpecialCellLayer
    )
}
