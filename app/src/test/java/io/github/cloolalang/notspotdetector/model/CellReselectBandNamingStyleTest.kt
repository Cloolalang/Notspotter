package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CellReselectBandNamingStyleTest {

    @Test
    fun fromId_knownIds_resolveToMatchingEnum() {
        assertEquals(
            CellReselectBandNamingStyle.BAND_NUMBER,
            CellReselectBandNamingStyle.fromId("band_number")
        )
        assertEquals(
            CellReselectBandNamingStyle.MHZ_NICKNAME,
            CellReselectBandNamingStyle.fromId("mhz_nickname")
        )
    }

    @Test
    fun fromId_unknownOrNullId_fallsBackToDefault() {
        assertEquals(CellReselectBandNamingStyle.DEFAULT, CellReselectBandNamingStyle.fromId(null))
        assertEquals(CellReselectBandNamingStyle.DEFAULT, CellReselectBandNamingStyle.fromId("bogus"))
        assertEquals(CellReselectBandNamingStyle.DEFAULT, CellReselectBandNamingStyle.fromId(""))
    }

    @Test
    fun default_isMhzNickname() {
        assertEquals(CellReselectBandNamingStyle.MHZ_NICKNAME, CellReselectBandNamingStyle.DEFAULT)
    }
}
