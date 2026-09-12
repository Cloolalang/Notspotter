package io.github.cloolalang.notspotdetector.model

/**
 * E-UTRA (LTE) operating band lookup from a downlink EARFCN, per 3GPP TS 36.101 Table 5.7.3-1.
 * Used by [CellIdentityAnnouncement] to speak "band &lt;n&gt;" (RXSS 9 cell-reselect alternative
 * announcement) instead of the raw channel number — see RXSS_CATALOGUE.md / VOICE_ANNOUNCEMENTS.md.
 *
 * [mhzNickname] is the common informal band name used by network-scanner tools — "L" (LTE) followed
 * by the nominal downlink frequency in MHz, e.g. band 20 (791-821 MHz DL) -> "L800", band 8 -> "L900".
 */
data class EutraBandInfo(val band: Int, val mhzNickname: String) {
    /** Nominal downlink MHz parsed from [mhzNickname], e.g. "L800" → 800. */
    fun nominalMhz(): Int? = mhzNickname.removePrefix("L").toIntOrNull()
}

object EutraBand {

    private class BandRange(val band: Int, val dlEarfcn: IntRange, val mhzNickname: String)

    private val RANGES = listOf(
        BandRange(1, 0..599, "L2100"),
        BandRange(2, 600..1199, "L1900"),
        BandRange(3, 1200..1949, "L1800"),
        BandRange(4, 1950..2399, "L2100"),
        BandRange(5, 2400..2649, "L850"),
        BandRange(6, 2650..2749, "L850"),
        BandRange(7, 2750..3449, "L2600"),
        BandRange(8, 3450..3799, "L900"),
        BandRange(9, 3800..4149, "L1800"),
        BandRange(10, 4150..4749, "L2100"),
        BandRange(11, 4750..4949, "L1500"),
        BandRange(12, 5010..5179, "L700"),
        BandRange(13, 5180..5279, "L700"),
        BandRange(14, 5280..5379, "L700"),
        BandRange(17, 5730..5849, "L700"),
        BandRange(18, 5850..5999, "L800"),
        BandRange(19, 6000..6149, "L800"),
        BandRange(20, 6150..6449, "L800"),
        BandRange(21, 6450..6599, "L1500"),
        BandRange(22, 6600..7399, "L3500"),
        BandRange(23, 7500..7699, "L2000"),
        BandRange(24, 7700..8039, "L1600"),
        BandRange(25, 8040..8689, "L1900"),
        BandRange(26, 8690..9039, "L850"),
        BandRange(27, 9040..9209, "L800"),
        BandRange(28, 9210..9659, "L700"),
        BandRange(29, 9660..9769, "L700"),
        BandRange(30, 9770..9869, "L2300"),
        BandRange(31, 9870..9919, "L450"),
        BandRange(32, 9920..10359, "L1500"),
        BandRange(33, 36000..36199, "L1900"),
        BandRange(34, 36200..36349, "L2000"),
        BandRange(35, 36350..36949, "L1900"),
        BandRange(36, 36950..37549, "L1900"),
        BandRange(37, 37550..37749, "L1900"),
        BandRange(38, 37750..38249, "L2600"),
        BandRange(39, 38250..38649, "L1900"),
        BandRange(40, 38650..39649, "L2300"),
        BandRange(41, 39650..41589, "L2500"),
        BandRange(42, 41590..43589, "L3500"),
        BandRange(43, 43590..45589, "L3700"),
        BandRange(44, 45590..46589, "L700"),
        BandRange(45, 46590..46789, "L1500"),
        BandRange(46, 46790..54539, "L5200"),
        BandRange(66, 66436..67335, "L2100")
    )

    /** Resolves the E-UTRA band for a downlink EARFCN, or null if it falls outside all known bands. */
    fun forEarfcn(earfcn: Int): EutraBandInfo? {
        val range = RANGES.find { earfcn in it.dlEarfcn } ?: return null
        return EutraBandInfo(range.band, range.mhzNickname)
    }
}
