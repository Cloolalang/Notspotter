package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GsmBandTest {

    @Test
    fun forArfcn_pGsmAndEgsm_resolveTo900() {
        assertEquals(900, GsmBand.forArfcn(0)?.mhz)
        assertEquals(8, GsmBand.forArfcn(0)?.eutraBand)
        assertEquals(900, GsmBand.forArfcn(62)?.mhz)
        assertEquals(8, GsmBand.forArfcn(62)?.eutraBand)
        assertEquals(900, GsmBand.forArfcn(71)?.mhz)
        assertEquals(900, GsmBand.forArfcn(124)?.mhz)
        assertEquals(900, GsmBand.forArfcn(975)?.mhz)
    }

    @Test
    fun forArfcn_dcs_resolvesTo1800() {
        assertEquals(1800, GsmBand.forArfcn(512)?.mhz)
        assertEquals(3, GsmBand.forArfcn(512)?.eutraBand)
        assertEquals(1800, GsmBand.forArfcn(600)?.mhz)
        assertEquals(3, GsmBand.forArfcn(600)?.eutraBand)
        assertEquals(1800, GsmBand.forArfcn(885)?.mhz)
    }

    @Test
    fun forArfcn_gsm850_resolvesTo850() {
        assertEquals(850, GsmBand.forArfcn(128)?.mhz)
        assertEquals(850, GsmBand.forArfcn(251)?.mhz)
    }

    @Test
    fun forArfcn_unknown_returnsNull() {
        assertNull(GsmBand.forArfcn(-1))
        assertNull(GsmBand.forArfcn(2000))
    }
}
