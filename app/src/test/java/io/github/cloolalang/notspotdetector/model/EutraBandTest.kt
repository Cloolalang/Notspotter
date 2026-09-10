package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EutraBandTest {

    @Test
    fun forEarfcn_6300_resolvesToBand20L800() {
        val info = EutraBand.forEarfcn(6_300)

        assertEquals(20, info?.band)
        assertEquals("L800", info?.mhzNickname)
    }

    @Test
    fun forEarfcn_3500_resolvesToBand8L900() {
        val info = EutraBand.forEarfcn(3_500)

        assertEquals(8, info?.band)
        assertEquals("L900", info?.mhzNickname)
    }

    @Test
    fun forEarfcn_3000_resolvesToBand7L2600() {
        val info = EutraBand.forEarfcn(3_000)

        assertEquals(7, info?.band)
        assertEquals("L2600", info?.mhzNickname)
    }

    @Test
    fun forEarfcn_outOfRange_returnsNull() {
        assertNull(EutraBand.forEarfcn(-1))
        assertNull(EutraBand.forEarfcn(100_000))
    }
}
