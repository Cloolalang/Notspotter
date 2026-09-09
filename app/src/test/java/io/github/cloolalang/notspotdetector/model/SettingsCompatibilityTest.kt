package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsCompatibilityTest {

    @Test
    fun minTierClickIntervalIncludesPulseDurationAndGap() {
        assertEquals(
            275L,
            SettingsCompatibility.minTierClickIntervalMs(signalPulseDurationMs = 250)
        )
    }

    @Test
    fun minTierClickIntervalUiRoundsUpToStep() {
        assertEquals(280, SettingsCompatibility.minTierClickIntervalUiMs(signalPulseDurationMs = 250))
    }

    @Test
    fun normalizePassiveSignalSettingsRaisesTierIntervalsBelowPulseDuration() {
        val settings = PassiveSignalSettings(
            criticalTierClickIntervalMs = 50,
            levelRangeAbcdClickIntervalMs = 50
        )
        val normalized = SettingsCompatibility.normalizePassiveSignalSettings(
            settings,
            signalPulseDurationMs = 250
        )
        assertEquals(280, normalized.criticalTierClickIntervalMs)
        assertEquals(280, normalized.levelRangeAbcdClickIntervalMs)
        assertEquals(280, normalized.poorTierClickIntervalMs)
    }

    @Test
    fun normalizePassiveSignalSettingsUsesSeparateBcdPulseDurationForLevelRangesAToD() {
        val settings = PassiveSignalSettings(
            levelRangeAbcdClickIntervalMs = 50,
            criticalTierClickIntervalMs = 50
        )
        val normalized = SettingsCompatibility.normalizePassiveSignalSettings(
            settings,
            signalPulseDurationMs = 100,
            levelRangeBcdPulseDurationMs = 400
        )
        assertEquals(130, normalized.criticalTierClickIntervalMs)
        assertEquals(430, normalized.levelRangeAbcdClickIntervalMs)
        assertEquals(430, normalized.mildTierClickIntervalMs)
        assertEquals(430, normalized.goodTierClickIntervalMs)
        assertEquals(430, normalized.poorTierClickIntervalMs)
    }

    @Test
    fun normalizedFixesNoSignalRsrpAtNegative125Dbm() {
        val normalized = PassiveSignalSettings(noSignalRsrpDbm = -110).normalized()
        assertEquals(PassiveSignalSettings.DEFAULT_NO_SIGNAL_RSRP_DBM, normalized.noSignalRsrpDbm)
    }

    @Test
    fun normalizePassiveSignalSettingsRaisesDeadzoneClickIntervalBelowPulseDuration() {
        val settings = PassiveSignalSettings(
            deadzoneTierClickIntervalMs = 50,
            deadzoneTierPulseDurationMs = 250
        )
        val normalized = SettingsCompatibility.normalizePassiveSignalSettings(
            settings,
            signalPulseDurationMs = 100
        )
        assertEquals(280, normalized.deadzoneTierClickIntervalMs)
    }

    @Test
    fun passiveOnlyUsesConfiguredIntervalWhenLongEnough() {
        assertEquals(
            500L,
            SettingsCompatibility.resolveTierClickIntervalMs(
                configuredMs = 500L,
                signalPulseDurationMs = 250,
                isPassiveOnlySession = true
            )
        )
    }

    @Test
    fun passiveOnlyRaisesIntervalWhenShorterThanPulse() {
        assertEquals(
            275L,
            SettingsCompatibility.resolveTierClickIntervalMs(
                configuredMs = 50L,
                signalPulseDurationMs = 250,
                isPassiveOnlySession = true
            )
        )
    }

    @Test
    fun passiveOnlyHonorsConfiguredIntervalBelowLegacy125msFloor() {
        assertEquals(
            40L,
            SettingsCompatibility.resolveTierClickIntervalMs(
                configuredMs = 40L,
                signalPulseDurationMs = 10,
                isPassiveOnlySession = true
            )
        )
    }

    @Test
    fun activeSessionDoublesPassiveConfiguredInterval() {
        assertEquals(
            1_000L,
            SettingsCompatibility.resolveTierClickIntervalMs(
                configuredMs = 500L,
                signalPulseDurationMs = 250,
                isPassiveOnlySession = false
            )
        )
    }

    @Test
    fun warnsWhenTierIntervalMuchSlowerThanMeasurement() {
        assertTrue(
            SettingsCompatibility.isTierIntervalMuchSlowerThanMeasurement(
                tierIntervalMs = 5_000,
                measurementIntervalMs = 2_000L
            )
        )
    }
}
