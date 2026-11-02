package io.github.cloolalang.notspotdetector.model

/**
 * How the RXSS 9 cell-reselect alternative "band" announcement speaks the E-UTRA band
 * (see [CellIdentityAnnouncement], `cellChangeSpeakBandEnabled` in [AudioVolumeSettings]).
 */
enum class CellReselectBandNamingStyle(val id: String) {
    /** Option A — speak the E-UTRA band number, e.g. "band 2 0" (band 20). */
    BAND_NUMBER("band_number"),

    /** Option B — speak the band's common MHz nickname, e.g. "band L 8 0 0" (band 20 = L800). */
    MHZ_NICKNAME("mhz_nickname");

    companion object {
        val DEFAULT = BAND_NUMBER

        fun fromId(id: String?): CellReselectBandNamingStyle {
            if (id.isNullOrBlank()) return DEFAULT
            return entries.find { it.id == id } ?: DEFAULT
        }
    }
}
