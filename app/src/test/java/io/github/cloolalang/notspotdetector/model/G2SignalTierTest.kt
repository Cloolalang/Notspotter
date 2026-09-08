package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class G2SignalTierTest {

    private val settings = PassiveSignalSettings()

    @Test
    fun resolveG2SignalStrengthTier_splitsAtMinus100Dbm() {
        assertEquals(SignalStrengthTier.G2_STRONG, settings.resolveG2SignalStrengthTier(-65))
        assertEquals(SignalStrengthTier.G2_STRONG, settings.resolveG2SignalStrengthTier(-99))
        assertEquals(SignalStrengthTier.G2_STRONG, settings.resolveG2SignalStrengthTier(-100))
        assertEquals(SignalStrengthTier.G2_WEAK, settings.resolveG2SignalStrengthTier(-105))
        assertEquals(null, settings.resolveG2SignalStrengthTier(-126))
    }

    @Test
    fun resolveSignalMeasurementTier_usesG2TiersOn2gFallback() {
        val strong = g2Stats(rsrpDbm = -85)
        val weak = g2Stats(rsrpDbm = -105)

        assertEquals(SignalMeasurementTier.G2_STRONG, strong.resolveSignalMeasurementTier(settings))
        assertEquals(SignalMeasurementTier.G2_WEAK, weak.resolveSignalMeasurementTier(settings))
    }

    @Test
    fun resolveSignalMeasurementTier_ignoresRegularTiersOn2gFallback() {
        val stats = g2Stats(rsrpDbm = -85, rsrqDb = -20)

        assertEquals(SignalMeasurementTier.G2_STRONG, stats.resolveSignalMeasurementTier(settings))
    }

    @Test
    fun shouldPlayCurrentTierSignalPulse_respectsG2TierToggle() {
        val stats = g2Stats(rsrpDbm = -85)
        val disabled = settings.copy(g2StrongTierSoundEnabled = false)

        assertFalse(stats.shouldPlayCurrentTierSignalPulse(disabled))
        assertTrue(stats.shouldPlayCurrentTierSignalPulse(settings))
    }

    private fun g2Stats(rsrpDbm: Int, rsrqDb: Int? = null): ConnectivityStats {
        return ConnectivityStats(
            isMonitoring = true,
            cellularAvailable = true,
            signalPermissionGranted = true,
            isOn2g = true,
            monitor2gFallbackEnabled = true,
            hasHomeGsmSignal = true,
            rsrpDbm = rsrpDbm,
            rsrqDb = rsrqDb
        )
    }
}
