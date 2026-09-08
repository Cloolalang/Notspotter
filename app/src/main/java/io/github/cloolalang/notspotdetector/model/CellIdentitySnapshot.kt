package io.github.cloolalang.notspotdetector.model

data class CellIdentitySnapshot(
    val lteEarfcn: Int? = null,
    val ltePci: Int? = null,
    val nrEarfcn: Int? = null,
    val nrPci: Int? = null,
    val gsmEarfcn: Int? = null,
    val gsmBsic: Int? = null
) {
    fun hasAnyIdentity(): Boolean {
        return lteEarfcn != null || ltePci != null || nrEarfcn != null || nrPci != null ||
            gsmEarfcn != null || gsmBsic != null
    }

    companion object {
        fun fromStats(stats: ConnectivityStats): CellIdentitySnapshot {
            return CellIdentitySnapshot(
                lteEarfcn = stats.lteEarfcn,
                ltePci = stats.ltePci,
                nrEarfcn = stats.nrEarfcn,
                nrPci = stats.nrPci,
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
                gsmEarfcn = metrics.gsmEarfcn,
                gsmBsic = metrics.gsmBsic
            )
        }
    }
}
