package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RsrpBandTest {

    @Test
    fun fromRxssNumber_mapsTwoThroughFiveToBandsAD() {
        assertEquals(RsrpBand.A, RsrpBand.fromRxssNumber(2))
        assertEquals(RsrpBand.B, RsrpBand.fromRxssNumber(3))
        assertEquals(RsrpBand.C, RsrpBand.fromRxssNumber(4))
        assertEquals(RsrpBand.D, RsrpBand.fromRxssNumber(5))
    }

    @Test
    fun fromRxssNumber_leavesOtherRxssUnmapped() {
        assertNull(RsrpBand.fromRxssNumber(1))
        assertNull(RsrpBand.fromRxssNumber(6))
        assertNull(RsrpBand.fromRxssNumber(10))
    }

    @Test
    fun levelRangeName_usesRxssNumber() {
        assertEquals("RXSS 2", RsrpBand.A.levelRangeName)
        assertEquals("RXSS 5", RsrpBand.D.levelRangeName)
    }

    @Test
    fun fromMeasurementTier_matchesRxssNumbers() {
        assertEquals(RsrpBand.A, RsrpBand.fromMeasurementTier(SignalMeasurementTier.MILD))
        assertEquals(RsrpBand.D, RsrpBand.fromMeasurementTier(SignalMeasurementTier.POOR))
        assertNull(RsrpBand.fromMeasurementTier(SignalMeasurementTier.CRITICAL))
    }
}
