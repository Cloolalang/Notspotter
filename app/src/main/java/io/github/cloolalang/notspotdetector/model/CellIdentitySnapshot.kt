package io.github.cloolalang.notspotdetector.model

data class CellIdentitySnapshot(
    val lteEarfcn: Int? = null,
    val ltePci: Int? = null,
    val nrEarfcn: Int? = null,
    val nrPci: Int? = null,
    /** Serving NR operating band — see [CellularRadioMetrics.nrBand]. */
    val nrBand: Int? = null,
    val gsmEarfcn: Int? = null,
    val gsmBsic: Int? = null
) {
    fun hasAnyIdentity(): Boolean {
        return lteEarfcn != null || ltePci != null || nrEarfcn != null || nrPci != null ||
            gsmEarfcn != null || gsmBsic != null
    }

    /**
     * Serving-cell identity change used for the reselect-rate metric.
     * PCI / channel / BSIC only — an NR band-label drift is not a reselect.
     * Empty → first identity is a camp, not a reselect.
     */
    fun isServingCellReselectFrom(previous: CellIdentitySnapshot): Boolean {
        if (!hasAnyIdentity() || !previous.hasAnyIdentity()) return false
        return lteEarfcn != previous.lteEarfcn ||
            ltePci != previous.ltePci ||
            nrEarfcn != previous.nrEarfcn ||
            nrPci != previous.nrPci ||
            gsmEarfcn != previous.gsmEarfcn ||
            gsmBsic != previous.gsmBsic
    }

    fun isSameServingCellAs(other: CellIdentitySnapshot): Boolean {
        return hasAnyIdentity() && other.hasAnyIdentity() && !isServingCellReselectFrom(other)
    }

    companion object {
        fun fromStats(stats: ConnectivityStats): CellIdentitySnapshot {
            return CellIdentitySnapshot(
                lteEarfcn = stats.lteEarfcn,
                ltePci = stats.ltePci,
                nrEarfcn = stats.nrEarfcn,
                nrPci = stats.nrPci,
                nrBand = stats.nrBand,
                gsmEarfcn = stats.gsmEarfcn,
                gsmBsic = stats.gsmBsic
            )
        }

        fun fromMetrics(metrics: CellularRadioMetrics): CellIdentitySnapshot {
            return CellIdentitySnapshot(
                lteEarfcn = metrics.lteEarfcn,
                ltePci = metrics.ltePci,
                nrEarfcn = metrics.nrEarfcn,
                nrPci = metrics.nrPci,
                nrBand = metrics.nrBand,
                gsmEarfcn = metrics.gsmEarfcn,
                gsmBsic = metrics.gsmBsic
            )
        }
    }
}
