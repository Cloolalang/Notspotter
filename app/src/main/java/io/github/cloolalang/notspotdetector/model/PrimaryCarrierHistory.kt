package io.github.cloolalang.notspotdetector.model

import io.github.cloolalang.notspotdetector.network.CellularSignalReader

/**
 * Last few primary-carrier visits for the level-histogram tape.
 * Each nominal band frequency has its own hue. Low bands such as band 20 stay orange,
 * L2600 stays blue, and other bands (band 1 at 2100 MHz, band 3 at 1800 MHz) each
 * have a different hue. A new PCI on that band takes the next shade and keeps it.
 */
object PrimaryCarrierHistory {
    const val VISIBLE_PAIRS = 4
    const val SHADES_PER_BAND = 8
    const val LOW_BAND_HUE = 28f
    const val L2600_HUE = 215f
    private const val RATE_WINDOW_MS = 60_000L

    /** Low bands such as band 20 (L800) share this orange hue. */
    private const val LOW_BAND_BELOW_MHZ = 1_000

    private val HUE_BY_MHZ = mapOf(
        1500 to 332f,
        1600 to 300f,
        1800 to 168f,
        1900 to 92f,
        2000 to 52f,
        2100 to 278f,
        2300 to 188f,
        2500 to 200f,
        2600 to L2600_HUE,
        3500 to 252f,
        3700 to 292f,
        4700 to 312f,
        5200 to 342f
    )

    /** Hue for this band frequency, or null when the band is unknown. */
    fun hueForMhz(nominalMhz: Int?): Float? {
        if (nominalMhz == null) return null
        if (nominalMhz < LOW_BAND_BELOW_MHZ) return LOW_BAND_HUE
        return HUE_BY_MHZ[nominalMhz] ?: fallbackHue(nominalMhz)
    }

    private fun fallbackHue(mhz: Int): Float {
        var hue = (mhz * 47 % 360).toFloat()
        if (hue in 10f..45f || hue in 200f..230f) {
            hue = (hue + 60f) % 360f
        }
        return hue
    }

    /**
     * Milliseconds for the tape to travel one pair from right to left.
     * Null while the reselect rate is zero, so the tape holds still.
     */
    fun segmentDurationMs(reselectsPerMinute: Int): Long? {
        if (reselectsPerMinute <= 0) return null
        return (RATE_WINDOW_MS / reselectsPerMinute).coerceAtLeast(1L)
    }

    fun pairFromStats(stats: ConnectivityStats): PrimaryCarrierPair? {
        val (channel, pci, mhz) = when (stats.radioAccessType) {
            CellularSignalReader.RADIO_5G -> Triple(
                stats.nrEarfcn,
                stats.nrPci,
                stats.nrBand?.let { NrBand.nominalMhz(it) }
            )
            CellularSignalReader.RADIO_2G -> Triple(
                stats.gsmEarfcn,
                stats.gsmBsic,
                stats.gsmEarfcn?.let { GsmBand.forArfcn(it)?.mhz }
            )
            else -> Triple(
                stats.lteEarfcn,
                stats.ltePci,
                stats.lteEarfcn?.let { EutraBand.forEarfcn(it)?.nominalMhz() }
            )
        }
        if (channel == null || pci == null) return null
        return PrimaryCarrierPair(channel = channel, pci = pci, nominalMhz = mhz)
    }

    fun record(tape: PrimaryCarrierTape, next: PrimaryCarrierPair?): PrimaryCarrierTape {
        if (next == null) return tape
        val last = tape.entries.lastOrNull()
        if (last != null && last.pair.sameCell(next)) {
            if (last.pair == next) return tape
            val (shades, shade) = shadeFor(tape.shades, next)
            val refreshed = last.copy(
                pair = next,
                shade = shade
            )
            return tape.copy(
                entries = tape.entries.dropLast(1) + refreshed,
                shades = shades
            )
        }
        val (shades, shade) = shadeFor(tape.shades, next)
        val entry = PrimaryCarrierTapeEntry(
            pair = next,
            shade = shade
        )
        return tape.copy(
            entries = (tape.entries + entry).takeLast(VISIBLE_PAIRS),
            shades = shades
        )
    }

    private fun shadeFor(
        shades: Map<CarrierShadeKey, Int>,
        pair: PrimaryCarrierPair
    ): Pair<Map<CarrierShadeKey, Int>, Int> {
        val key = CarrierShadeKey(nominalMhz = pair.nominalMhz, pci = pair.pci)
        shades[key]?.let { return shades to it }
        val usedOnBand = shades.count { it.key.nominalMhz == pair.nominalMhz }
        val shade = usedOnBand % SHADES_PER_BAND
        return (shades + (key to shade)) to shade
    }
}

data class PrimaryCarrierPair(
    val channel: Int,
    val pci: Int,
    val nominalMhz: Int?
) {
    fun sameCell(other: PrimaryCarrierPair): Boolean {
        return channel == other.channel && pci == other.pci
    }
}

data class CarrierShadeKey(
    val nominalMhz: Int?,
    val pci: Int
)

data class PrimaryCarrierTapeEntry(
    val pair: PrimaryCarrierPair,
    val shade: Int
)

data class PrimaryCarrierTape(
    val entries: List<PrimaryCarrierTapeEntry> = emptyList(),
    val shades: Map<CarrierShadeKey, Int> = emptyMap()
)
