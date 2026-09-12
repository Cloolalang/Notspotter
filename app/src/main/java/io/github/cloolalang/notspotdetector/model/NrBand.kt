package io.github.cloolalang.notspotdetector.model

/**
 * Nominal downlink MHz for common NR operating bands (FR1), matching the informal
 * scanner nicknames used for LTE in [EutraBand].
 */
object NrBand {

    private val NOMINAL_MHZ = mapOf(
        1 to 2100,
        3 to 1800,
        5 to 850,
        7 to 2600,
        8 to 900,
        20 to 800,
        28 to 700,
        38 to 2600,
        40 to 2300,
        41 to 2500,
        77 to 3700,
        78 to 3500,
        79 to 4700
    )

    fun nominalMhz(nBand: Int): Int? = NOMINAL_MHZ[nBand]
}
