package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SinrMetricTest {

    @Test
    fun takeLteRssnr_acceptsDocumentedRange() {
        assertEquals(-20, SinrMetric.takeLteRssnr(-20))
        assertEquals(0, SinrMetric.takeLteRssnr(0))
        assertEquals(30, SinrMetric.takeLteRssnr(30))
    }

    @Test
    fun takeLteRssnr_rejectsUnavailableAndOutOfRange() {
        assertNull(SinrMetric.takeLteRssnr(Int.MAX_VALUE))
        assertNull(SinrMetric.takeLteRssnr(-201))
        assertNull(SinrMetric.takeLteRssnr(301))
    }

    @Test
    fun takeLteRssnr_convertsTenthsOfADb() {
        assertEquals(13, SinrMetric.takeLteRssnr(126))
        assertEquals(-8, SinrMetric.takeLteRssnr(-75))
    }

    @Test
    fun takeNrSsSinr_acceptsDocumentedRange() {
        assertEquals(-23, SinrMetric.takeNrSsSinr(-23))
        assertEquals(12, SinrMetric.takeNrSsSinr(12))
        assertEquals(40, SinrMetric.takeNrSsSinr(40))
    }

    @Test
    fun takeNrSsSinr_rejectsUnavailableAndOutOfRange() {
        assertNull(SinrMetric.takeNrSsSinr(Int.MAX_VALUE))
        assertNull(SinrMetric.takeNrSsSinr(-231))
        assertNull(SinrMetric.takeNrSsSinr(401))
    }

    @Test
    fun takeNrSsSinr_convertsTenthsOfADb() {
        assertEquals(18, SinrMetric.takeNrSsSinr(184))
        assertEquals(-12, SinrMetric.takeNrSsSinr(-115))
    }
}
