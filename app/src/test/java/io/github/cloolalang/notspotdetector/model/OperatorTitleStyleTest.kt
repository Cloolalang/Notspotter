package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OperatorTitleStyleTest {

    @Test
    fun detectBrand_recognizesUkOperators() {
        assertEquals(UkOperatorBrand.EE, OperatorTitleStyle.detectBrand("EE"))
        assertEquals(UkOperatorBrand.EE, OperatorTitleStyle.detectBrand("EE UK"))
        assertEquals(UkOperatorBrand.VODAFONE, OperatorTitleStyle.detectBrand("Vodafone UK"))
        assertEquals(UkOperatorBrand.VMO2, OperatorTitleStyle.detectBrand("O2"))
        assertEquals(UkOperatorBrand.VMO2, OperatorTitleStyle.detectBrand("Virgin Media O2"))
        assertEquals(UkOperatorBrand.VMO2, OperatorTitleStyle.detectBrand("VMO2"))
    }

    @Test
    fun shortLabel_usesBrandNames() {
        assertEquals("EE", OperatorTitleStyle.shortLabel(UkOperatorBrand.EE, "EE UK"))
        assertEquals("Vodafone", OperatorTitleStyle.shortLabel(UkOperatorBrand.VODAFONE, "Vodafone UK"))
        assertEquals("VMO2", OperatorTitleStyle.shortLabel(UkOperatorBrand.VMO2, "O2 UK"))
        assertEquals("Three UK", OperatorTitleStyle.shortLabel(UkOperatorBrand.OTHER, "Three UK"))
    }

    @Test
    fun resolveLabel_prefersHomeOperator() {
        val stats = ConnectivityStats(
            homeNetworkOperatorName = "Vodafone UK",
            networkOperatorName = "EE"
        )
        assertEquals("Vodafone", OperatorTitleStyle.resolveLabel(stats))
    }

    @Test
    fun isLowOrNoSignalForTitle_usesTierFiveAndAbove() {
        assertTrue(SignalMeasurementTier.POOR.isLowOrNoSignalForTitle())
        assertTrue(SignalMeasurementTier.CRITICAL.isLowOrNoSignalForTitle())
        assertTrue(SignalMeasurementTier.G2_STRONG.isLowOrNoSignalForTitle())
        assertTrue(SignalMeasurementTier.G2_WEAK.isLowOrNoSignalForTitle())
        assertTrue(SignalMeasurementTier.DEADZONE.isLowOrNoSignalForTitle())
        assertTrue(SignalMeasurementTier.NO_SIGNAL.isLowOrNoSignalForTitle())
        assertFalse(SignalMeasurementTier.FAIR.isLowOrNoSignalForTitle())
        assertFalse(SignalMeasurementTier.GOOD.isLowOrNoSignalForTitle())
        assertFalse(SignalMeasurementTier.VERY_STRONG.isLowOrNoSignalForTitle())
        assertFalse(SignalMeasurementTier.LIMITED_SERVICE.isLowOrNoSignalForTitle())
    }

    @Test
    fun statsLowOrNoSignal_usesMeasurementTier() {
        val good = ConnectivityStats(
            cellularAvailable = true,
            rsrpDbm = -85,
            rsrqDb = -10,
            signalPermissionGranted = true
        )
        val poor = good.copy(rsrpDbm = -115)

        assertFalse(good.isLowOrNoSignalForTitle())
        assertTrue(poor.isLowOrNoSignalForTitle())
    }
}
