package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Level Ranges A–D (RXSS 2–5) each have an independent [PassiveSignalSettings] pulse duration field
 * (`mildTierPulseDurationMs`, `goodTierPulseDurationMs`, `fairTierPulseDurationMs`, `poorTierPulseDurationMs`).
 * Regression coverage for the bug where these were not exposed in the UI or persisted, so every range
 * silently played the same default-length pulse regardless of what was configured.
 */
class LevelRangeAbcdPulseDurationTest {

    @Test
    fun eachLevelRangeResolvesItsOwnConfiguredPulseDuration() {
        val settings = PassiveSignalSettings(
            mildTierPulseDurationMs = 120,
            goodTierPulseDurationMs = 240,
            fairTierPulseDurationMs = 360,
            poorTierPulseDurationMs = 480
        )

        assertEquals(120, settings.pulseDurationMsForTier(SignalStrengthTier.MILD))
        assertEquals(240, settings.pulseDurationMsForTier(SignalStrengthTier.GOOD))
        assertEquals(360, settings.pulseDurationMsForTier(SignalStrengthTier.FAIR))
        assertEquals(480, settings.pulseDurationMsForTier(SignalStrengthTier.POOR))
    }

    @Test
    fun maxPulseDurationMsIsLongestOfTheFourRanges() {
        val settings = PassiveSignalSettings(
            mildTierPulseDurationMs = 120,
            goodTierPulseDurationMs = 480,
            fairTierPulseDurationMs = 200,
            poorTierPulseDurationMs = 150
        )

        assertEquals(480, settings.levelRangeAbcdMaxPulseDurationMs())
    }

    @Test
    fun changingOneRangeDurationDoesNotAffectTheOthers() {
        val settings = PassiveSignalSettings(mildTierPulseDurationMs = 300)

        assertEquals(300, settings.pulseDurationMsForTier(SignalStrengthTier.MILD))
        assertEquals(
            PassiveSignalSettings.DEFAULT_GOOD_TIER_PULSE_DURATION_MS,
            settings.pulseDurationMsForTier(SignalStrengthTier.GOOD)
        )
        assertEquals(
            PassiveSignalSettings.DEFAULT_FAIR_TIER_PULSE_DURATION_MS,
            settings.pulseDurationMsForTier(SignalStrengthTier.FAIR)
        )
        assertEquals(
            PassiveSignalSettings.DEFAULT_POOR_TIER_PULSE_DURATION_MS,
            settings.pulseDurationMsForTier(SignalStrengthTier.POOR)
        )
    }
}
