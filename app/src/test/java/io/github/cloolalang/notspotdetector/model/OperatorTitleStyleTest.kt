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
    fun resolveLabel_ignoresVisitedCampWhenHomeKnown() {
        val stats = ConnectivityStats(
            isLimitedService = true,
            homeNetworkOperatorName = "Vodafone UK",
            servingNetworkOperatorName = "EE",
            networkOperatorName = "EE",
            homePlmn = "23415",
            plmn = "23430"
        )
        assertEquals("Vodafone", OperatorTitleStyle.resolveLabel(stats))
        assertEquals(UkOperatorBrand.VODAFONE, OperatorTitleStyle.detectBrand(stats))
    }

    @Test
    fun resolveLabel_doesNotFallBackToServingOperator() {
        val stats = ConnectivityStats(
            networkOperatorName = "EE",
            servingNetworkOperatorName = "EE"
        )
        assertEquals(null, OperatorTitleStyle.resolveLabel(stats))
    }

    @Test
    fun detectBrand_doesNotUseServingWhenHomeIsUnrecognizedBrand() {
        val stats = ConnectivityStats(
            homeNetworkOperatorName = "Three UK",
            networkOperatorName = "EE",
            servingNetworkOperatorName = "EE"
        )
        assertEquals(UkOperatorBrand.OTHER, OperatorTitleStyle.detectBrand(stats))
        assertEquals("Three UK", OperatorTitleStyle.resolveLabel(stats))
    }

    @Test
    fun isLowOrNoSignalForTitle_usesTierFiveAndAbove() {
        assertTrue(SignalMeasurementTier.POOR.isLowOrNoSignalForTitle())
        assertTrue(SignalMeasurementTier.CRITICAL.isLowOrNoSignalForTitle())
        assertTrue(SignalMeasurementTier.G2_STRONG.isLowOrNoSignalForTitle())
        assertTrue(SignalMeasurementTier.G2_WEAK.isLowOrNoSignalForTitle())
        assertTrue(SignalMeasurementTier.DEADZONE.isLowOrNoSignalForTitle())
        assertTrue(SignalMeasurementTier.NO_SIGNAL.isLowOrNoSignalForTitle())
        assertTrue(SignalMeasurementTier.SEARCHING_2G.isLowOrNoSignalForTitle())
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
