package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ServingBandMhzTest {

    @Test
    fun fromLteEarfcn_band20_is800() {
        assertEquals(800, ServingBandMhz.fromLteEarfcn(6_300))
    }

    @Test
    fun fromGsmArfcn_dcs1800() {
        assertEquals(1800, ServingBandMhz.fromGsmArfcn(600))
    }

    @Test
    fun fromNrBand_n78_is3500() {
        assertEquals(3500, ServingBandMhz.fromNrBand(78))
    }

    @Test
    fun formatMhz_endcJoinsDistinctValues() {
        assertEquals(
            "800 / 3500",
            ServingBandMhz.formatMhz(800, 3500)
        )
    }

    @Test
    fun formatMhz_dedupesSameFrequency() {
        assertEquals("800", ServingBandMhz.formatMhz(800, 800))
    }

    @Test
    fun formatMhz_empty_returnsNull() {
        assertNull(ServingBandMhz.formatMhz(null, null))
    }

    @Test
    fun formatDisplay_lteBand20_is800Over20() {
        assertEquals("800/20", ServingBandMhz.formatDisplay(lteEarfcn = 6_300))
    }

    @Test
    fun formatDisplay_gsm900_is900Over8() {
        assertEquals("900/8", ServingBandMhz.formatDisplay(gsmArfcn = 62))
    }

    @Test
    fun formatDisplay_nr78_is3500Over78() {
        assertEquals("3500/78", ServingBandMhz.formatDisplay(nrBand = 78))
    }

    @Test
    fun formatDisplay_endcJoinsLteAndNr() {
        assertEquals(
            "800/20 / 3500/78",
            ServingBandMhz.formatDisplay(lteEarfcn = 6_300, nrBand = 78)
        )
    }
}
