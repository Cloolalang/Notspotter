package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VeryStrongSignalTest {

    @Test
    fun veryStrongRsrpUsesConfigurableThreshold() {
        val settings = PassiveSignalSettings(veryStrongRsrpMinDbm = -80)
        assertTrue(settings.isVeryStrongRsrp(-79))
        assertFalse(settings.isVeryStrongRsrp(-80))
        assertFalse(settings.isVeryStrongRsrp(-81))
    }

    @Test
    fun veryStrongTierUsesLowSignalVolumeAndGlobalPulseDuration() {
        val volumes = AudioVolumeSettings(
            lowSignalClickVolume = 0.55f,
            signalPulseDurationMs = 320,
            levelRangeBcdClickVolume = 0.9f,
            levelRangeBcdPulseDurationMs = 180
        ).normalized()

        assertEquals(0.55f, volumes.veryStrongTierClickVolume())
        assertEquals(320, volumes.veryStrongTierPulseDurationMs())
        assertEquals(0.9f, volumes.clickVolumeForTier(SignalStrengthTier.MILD))
        assertEquals(180, volumes.pulseDurationMsForTier(SignalStrengthTier.MILD))
    }

    @Test
    fun veryStrongTierPulseFrequencyIsIndependentOfSignalPulseFrequency() {
        val volumes = AudioVolumeSettings(
            signalPulseFrequencyHz = 600,
            veryStrongTierPulseFrequencyHz = 900
        ).normalized()
        assertEquals(600, volumes.signalPulseFrequencyHz)
        assertEquals(900, volumes.veryStrongTierPulseFrequencyHz)
    }

    @Test
    fun levelRangeBcdPulseSettingsAreIndependentOfGlobalSignalPulse() {
        val volumes = AudioVolumeSettings(
            signalPulseFrequencyHz = 600,
            signalPulseDurationMs = 250,
            lowSignalClickVolume = 0.5f,
            levelRangeBcdPulseFrequencyHz = 700,
            levelRangeBcdPulseDurationMs = 300,
            levelRangeBcdClickVolume = 0.8f
        ).normalized()
        assertEquals(700, volumes.pulseFrequencyHzForTier(SignalStrengthTier.GOOD))
        assertEquals(300, volumes.pulseDurationMsForTier(SignalStrengthTier.FAIR))
        assertEquals(0.8f, volumes.clickVolumeForTier(SignalStrengthTier.POOR))
        assertEquals(700, volumes.pulseFrequencyHzForTier(SignalStrengthTier.MILD))
        assertEquals(0.8f, volumes.clickVolumeForTier(SignalStrengthTier.MILD))
    }

    @Test
    fun veryStrongTierPulseFrequencyDefaultsTo750Hz() {
        assertEquals(750, AudioVolumeSettings.DEFAULT_VERY_STRONG_TIER_PULSE_FREQUENCY_HZ)
    }

    @Test
    fun normalizedKeepsVeryStrongAboveMild() {
        val settings = PassiveSignalSettings(
            mildRsrpMinDbm = -95,
            veryStrongRsrpMinDbm = -96
        ).normalized()
        assertTrue(settings.veryStrongRsrpMinDbm > settings.mildRsrpMinDbm)
    }
}
