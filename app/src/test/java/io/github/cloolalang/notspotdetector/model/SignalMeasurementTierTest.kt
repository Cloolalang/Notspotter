package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SignalMeasurementTierTest {

    private val settings = PassiveSignalSettings(
        noSignalRsrpDbm = -125,
        poorRsrpMinDbm = -120,
        fairRsrpMinDbm = -105,
        goodRsrpMinDbm = -100,
        mildRsrpMinDbm = -95,
        veryStrongRsrpMinDbm = -80,
        rsrqFairMinDb = -18
    )

    @Test
    fun displayNumbersMapOneThroughSix() {
        assertEquals(1, SignalMeasurementTier.VERY_STRONG.displayNumber)
        assertEquals(2, SignalStrengthTier.MILD.displayNumber)
        assertEquals(3, SignalStrengthTier.GOOD.displayNumber)
        assertEquals(4, SignalStrengthTier.FAIR.displayNumber)
        assertEquals(5, SignalStrengthTier.POOR.displayNumber)
        assertEquals(6, SignalStrengthTier.CRITICAL.displayNumber)
        assertEquals(7, SignalMeasurementTier.G2_STRONG.displayNumber)
        assertEquals(8, SignalMeasurementTier.G2_WEAK.displayNumber)
        assertEquals(9, SignalMeasurementTier.DEADZONE.displayNumber)
        assertEquals(null, SignalMeasurementTier.NO_SIGNAL.displayNumber)
    }

    @Test
    fun veryStrongRsrpMapsToVeryStrongTier() {
        val stats = baseStats(rsrpDbm = -75, rsrqDb = -12)
        assertEquals(SignalMeasurementTier.VERY_STRONG, stats.resolveSignalMeasurementTier(settings))
    }

    @Test
    fun criticalRsrqMapsToCriticalTier() {
        val stats = baseStats(rsrpDbm = -95, rsrqDb = -20)
        assertEquals(SignalMeasurementTier.CRITICAL, stats.resolveSignalMeasurementTier(settings))
    }

    @Test
    fun weakRsrpMapsToPoorTier() {
        val stats = baseStats(rsrpDbm = -110, rsrqDb = -12)
        assertEquals(SignalMeasurementTier.POOR, stats.resolveSignalMeasurementTier(settings))
    }

    @Test
    fun limitedServiceOverridesRsrpTier() {
        val stats = baseStats(rsrpDbm = -75, rsrqDb = -12, isLimitedService = true)
        assertEquals(SignalMeasurementTier.LIMITED_SERVICE, stats.resolveSignalMeasurementTier(settings))
    }

    @Test
    fun noSignalRsrpMapsToNoSignalTier() {
        val stats = baseStats(rsrpDbm = -126, rsrqDb = -12)
        assertEquals(SignalMeasurementTier.NO_SIGNAL, stats.resolveSignalMeasurementTier(settings))
    }

    @Test
    fun completeNoServiceMapsToDeadzoneTier() {
        val stats = ConnectivityStats(
            isMonitoring = true,
            isCompleteNoService = true,
            cellularAvailable = false,
            signalPermissionGranted = true
        )
        assertEquals(SignalMeasurementTier.DEADZONE, stats.resolveSignalMeasurementTier(settings))
    }

    private fun baseStats(
        rsrpDbm: Int?,
        rsrqDb: Int?,
        isLimitedService: Boolean = false
    ): ConnectivityStats {
        return ConnectivityStats(
            cellularAvailable = true,
            rsrpDbm = rsrpDbm,
            rsrqDb = rsrqDb,
            isLimitedService = isLimitedService,
            signalPermissionGranted = true
        )
    }
}
