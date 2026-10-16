package io.github.cloolalang.notspotdetector.model

/** Formats the serving cell’s nominal band frequency for the cellular metrics row. */
object ServingBandMhz {

    fun fromLteEarfcn(earfcn: Int?): Int? =
        earfcn?.let { EutraBand.forEarfcn(it)?.nominalMhz() }

    fun fromGsmArfcn(arfcn: Int?): Int? =
        arfcn?.let { GsmBand.forArfcn(it)?.mhz }

    fun fromNrBand(nBand: Int?): Int? =
        nBand?.let { NrBand.nominalMhz(it) }

    fun formatMhz(vararg mhz: Int?): String? {
        val values = mhz.filterNotNull().distinct()
        if (values.isEmpty()) return null
        return values.joinToString(" / ")
    }

    /** Formats a single layer as `&lt;MHz&gt;(&lt;band&gt;)`, e.g. `800(20)` or `3500(n78)`. */
    fun formatMhzBand(mhz: Int?, band: Int?, nr: Boolean = false): String? {
        val bandLabel = band?.let { if (nr) "n$it" else it.toString() }
        return when {
            mhz != null && bandLabel != null -> "$mhz($bandLabel)"
            mhz != null -> mhz.toString()
            bandLabel != null -> bandLabel
            else -> null
        }
    }

    /**
     * Metrics value for Band (MHz). EN-DC joins LTE and NR as `2600(7) / 3500(n78)`.
     */
    fun formatDisplay(
        lteEarfcn: Int? = null,
        gsmArfcn: Int? = null,
        nrBand: Int? = null
    ): String? {
        val parts = buildList {
            lteEarfcn?.let { earfcn ->
                val info = EutraBand.forEarfcn(earfcn)
                formatMhzBand(info?.nominalMhz(), info?.band)?.let(::add)
            }
            gsmArfcn?.let { arfcn ->
                val info = GsmBand.forArfcn(arfcn)
                formatMhzBand(info?.mhz, info?.eutraBand)?.let(::add)
            }
            nrBand?.let { band ->
                formatMhzBand(fromNrBand(band), band, nr = true)?.let(::add)
            }
        }.distinct()
        return parts.takeIf { it.isNotEmpty() }?.joinToString(" / ")
    }
}
