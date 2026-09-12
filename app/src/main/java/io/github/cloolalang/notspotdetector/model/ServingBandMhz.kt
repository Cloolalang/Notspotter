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
}
