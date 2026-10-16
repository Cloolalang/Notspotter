package io.github.cloolalang.notspotdetector.model

import io.github.cloolalang.notspotdetector.network.CellularSignalReader

/**
 * Last few primary-carrier visits for the level-histogram tape.
 * Colour family follows the band’s nominal frequency; each new PCI on that band
 * takes the next shade and keeps it for the session.
 */
object PrimaryCarrierHistory {
    const val VISIBLE_PAIRS = 4
    const val SHADES_PER_BAND = 8
    private const val RATE_WINDOW_MS = 60_000L

    /** Low bands such as band 20 (L800). */
    private const val LOW_BAND_BELOW_MHZ = 1_000

    /** High bands such as band 7 (L2600) start here. Mid bands sit between the two. */
    private const val HIGH_BAND_FROM_MHZ = 2_300

    fun familyForMhz(nominalMhz: Int?): CarrierTapeFamily {
        return when {
            nominalMhz == null -> CarrierTapeFamily.NEUTRAL
            nominalMhz < LOW_BAND_BELOW_MHZ -> CarrierTapeFamily.ORANGE
            nominalMhz < HIGH_BAND_FROM_MHZ -> CarrierTapeFamily.GREEN
            else -> CarrierTapeFamily.BLUE
        }
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
                family = familyForMhz(next.nominalMhz),
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
            family = familyForMhz(next.nominalMhz),
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

enum class CarrierTapeFamily {
    ORANGE,
    GREEN,
    BLUE,
    NEUTRAL
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
    val family: CarrierTapeFamily,
    val shade: Int
)

data class PrimaryCarrierTape(
    val entries: List<PrimaryCarrierTapeEntry> = emptyList(),
    val shades: Map<CarrierShadeKey, Int> = emptyMap()
)
