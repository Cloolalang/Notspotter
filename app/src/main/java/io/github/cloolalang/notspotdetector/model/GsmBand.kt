package io.github.cloolalang.notspotdetector.model

/**
 * GSM operating band from a BCCH ARFCN, per 3GPP TS 45.005.
 * Used by [CellIdentityAnnouncement] so RXSS 9 “speak band” can replace channel/BSIC on 2G.
 *
 * [eutraBand] is the matching E-UTRA band number where the frequencies coincide
 * (GSM 900 → 8, DCS 1800 → 3, GSM 850 → 5). European/UK mapping: ARFCN 512–885 is DCS 1800.
 */
data class GsmBandInfo(val mhz: Int, val eutraBand: Int?)

object GsmBand {

    private class BandRange(val mhz: Int, val arfcn: IntRange, val eutraBand: Int?)

    private val RANGES = listOf(
        BandRange(450, 259..293, eutraBand = 31),
        BandRange(480, 306..340, eutraBand = null),
        BandRange(750, 438..511, eutraBand = null),
        BandRange(810, 350..425, eutraBand = null),
        BandRange(850, 128..251, eutraBand = 5),
        BandRange(900, 1..124, eutraBand = 8),
        BandRange(900, 975..1023, eutraBand = 8),
        BandRange(1800, 512..885, eutraBand = 3)
    )

    fun forArfcn(arfcn: Int): GsmBandInfo? {
        if (arfcn == 0) return GsmBandInfo(mhz = 900, eutraBand = 8)
        val range = RANGES.find { arfcn in it.arfcn } ?: return null
        return GsmBandInfo(range.mhz, range.eutraBand)
    }
}
