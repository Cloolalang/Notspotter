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

    /** Formats a single layer as `&lt;MHz&gt;(&lt;band&gt;)`, e.g. `800(20)`. */
    fun formatMhzBand(mhz: Int?, band: Int?): String? {
        return when {
            mhz != null && band != null -> "$mhz($band)"
            mhz != null -> mhz.toString()
            band != null -> band.toString()
            else -> null
        }
    }

    /**
     * Metrics value for Band (MHz). EN-DC may join LTE and NR as `800(20) / 3500(78)`.
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
                formatMhzBand(fromNrBand(band), band)?.let(::add)
            }
        }.distinct()
        return parts.takeIf { it.isNotEmpty() }?.joinToString(" / ")
    }
}
