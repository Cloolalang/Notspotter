package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CriticalSignalTierTest {

    @Test
    fun criticalTierUsesLowSignalClickVolumeNotLevelRangeBcd() {
        val volumes = AudioVolumeSettings(
            lowSignalClickVolume = 0.35f,
            levelRangeBcdClickVolume = 0.92f
        ).normalized()

        assertEquals(0.35f, volumes.criticalTierClickVolume())
        assertEquals(0.35f, volumes.clickVolumeForTier(SignalStrengthTier.CRITICAL))
        assertEquals(0.92f, volumes.clickVolumeForTier(SignalStrengthTier.POOR))
    }

    @Test
    fun criticalTierPulseDurationUsesPassiveSettings() {
        val settings = PassiveSignalSettings(
            criticalTierPulseDurationMs = 180,
            mildTierPulseDurationMs = 300
        ).normalized()

        assertEquals(180, settings.pulseDurationMsForTier(SignalStrengthTier.CRITICAL))
        assertEquals(300, settings.pulseDurationMsForTier(SignalStrengthTier.MILD))
    }
}
