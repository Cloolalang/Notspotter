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
    fun pulseDurationMsForTier_usesG2TierSettings() {
        val custom = settings.copy(
            g2StrongTierPulseDurationMs = 180,
            g2WeakTierPulseDurationMs = 90
        )

        assertEquals(180, custom.pulseDurationMsForTier(SignalStrengthTier.G2_STRONG))
        assertEquals(90, custom.pulseDurationMsForTier(SignalStrengthTier.G2_WEAK))
    }

    @Test
    fun pulseFrequencyHzForTier_usesG2TierSettings() {
        val volumes = AudioVolumeSettings(
            signalPulseFrequencyHz = 500,
            levelRangeBcdPulseFrequencyHz = 610,
            g2StrongTierPulseFrequencyHz = 720,
            g2WeakTierPulseFrequencyHz = 480
        )

        assertEquals(720, volumes.pulseFrequencyHzForTier(SignalStrengthTier.G2_STRONG))
        assertEquals(480, volumes.pulseFrequencyHzForTier(SignalStrengthTier.G2_WEAK))
        assertEquals(610, volumes.pulseFrequencyHzForTier(SignalStrengthTier.FAIR))
    }

    @Test
    fun clickVolumeForTier_usesNoSignalToneVolumeForLteNrNoSignal() {
        val volumes = AudioVolumeSettings(
            lowSignalClickVolume = 0.3f,
            noSignalToneVolume = 0.7f
        )

        assertEquals(0.7f, volumes.clickVolumeForTier(SignalStrengthTier.NO_SIGNAL))
        assertEquals(0.7f, volumes.clickVolumeForTier(SignalStrengthTier.G2_NO_SIGNAL))
        assertEquals(0.7f, volumes.clickVolumeForTier(SignalStrengthTier.DEADZONE))
        assertEquals(0.7f, volumes.clickVolumeForTier(SignalStrengthTier.SEARCHING_2G))
    }

    @Test
    fun pulseFrequencyHzForTier_usesNoSignalTierSetting() {
        val volumes = AudioVolumeSettings(
            signalPulseFrequencyHz = 500,
            noSignalTierPulseFrequencyHz = 430
        )

        assertEquals(500, volumes.pulseFrequencyHzForTier(SignalStrengthTier.CRITICAL))
        assertEquals(430, volumes.pulseFrequencyHzForTier(SignalStrengthTier.NO_SIGNAL))
        assertEquals(430, volumes.pulseFrequencyHzForTier(SignalStrengthTier.G2_NO_SIGNAL))
        assertEquals(430, volumes.pulseFrequencyHzForTier(SignalStrengthTier.DEADZONE))
        assertEquals(430, volumes.pulseFrequencyHzForTier(SignalStrengthTier.SEARCHING_2G))
    }

    @Test
    fun shouldPlayTier5StylePeriodicVoice_onlyOnRxss6NotRxss5() {
        val tier5Rsrp = settings.poorRsrpMinDbm + 1
        val tier6Rsrp = settings.poorRsrpMinDbm - 1

        assertFalse(lteStats(tier5Rsrp).shouldPlayTier5StylePeriodicVoice(settings))
        assertTrue(lteStats(tier6Rsrp).shouldPlayTier5StylePeriodicVoice(settings))
    }

    @Test
    fun shouldPlayCurrentTierSignalPulse_respectsG2TierToggle() {
        val stats = g2Stats(rsrpDbm = -85)
        val disabled = settings.copy(g2StrongTierSoundEnabled = false)

        assertFalse(stats.shouldPlayCurrentTierSignalPulse(disabled))
        assertTrue(stats.shouldPlayCurrentTierSignalPulse(settings))
    }

    private fun lteStats(rsrpDbm: Int, rsrqDb: Int? = -10): ConnectivityStats {
        return ConnectivityStats(
            isMonitoring = true,
            cellularAvailable = true,
            signalPermissionGranted = true,
            isOn2g = false,
            monitor2gFallbackEnabled = true,
            rsrpDbm = rsrpDbm,
            rsrqDb = rsrqDb
        )
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
