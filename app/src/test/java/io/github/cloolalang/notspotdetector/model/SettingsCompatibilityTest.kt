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
            levelRangeAbcdClickIntervalMs = 50,
            poorTierClickIntervalMs = 50
        )
        val normalized = SettingsCompatibility.normalizePassiveSignalSettings(
            settings,
            signalPulseDurationMs = 250
        )
        assertEquals(280, normalized.criticalTierClickIntervalMs)
        // Shared A–D interval and poor interval are floored by those ranges' own pulse durations.
        assertEquals(
            SettingsCompatibility.minTierClickIntervalUiMs(
                PassiveSignalSettings.DEFAULT_POOR_TIER_PULSE_DURATION_MS
            ),
            normalized.levelRangeAbcdClickIntervalMs
        )
        assertEquals(
            SettingsCompatibility.minTierClickIntervalUiMs(
                PassiveSignalSettings.DEFAULT_POOR_TIER_PULSE_DURATION_MS
            ),
            normalized.poorTierClickIntervalMs
        )
    }

    @Test
    fun normalizePassiveSignalSettingsUsesEachLevelRangesOwnDurationForItsClickIntervalFloor() {
        val settings = PassiveSignalSettings(
            criticalTierClickIntervalMs = 50,
            mildTierPulseDurationMs = 150,
            mildTierClickIntervalMs = 50,
            goodTierPulseDurationMs = 400,
            goodTierClickIntervalMs = 50,
            fairTierPulseDurationMs = 200,
            fairTierClickIntervalMs = 50,
            poorTierPulseDurationMs = 180,
            poorTierClickIntervalMs = 50
        )
        val normalized = SettingsCompatibility.normalizePassiveSignalSettings(
            settings,
            signalPulseDurationMs = 100
        )
        assertEquals(130, normalized.criticalTierClickIntervalMs)
        // Each Level Range's click interval floor now derives from that same range's own pulse duration.
        assertEquals(180, normalized.mildTierClickIntervalMs)
        assertEquals(430, normalized.goodTierClickIntervalMs)
        assertEquals(230, normalized.fairTierClickIntervalMs)
        assertEquals(210, normalized.poorTierClickIntervalMs)
    }

    @Test
    fun normalizedClampsNoSignalRsrpToConfigurableRange() {
        assertEquals(
            PassiveSignalSettings.MAX_NO_SIGNAL_RSRP_DBM,
            PassiveSignalSettings(noSignalRsrpDbm = -110).normalized().noSignalRsrpDbm
        )
        assertEquals(
            PassiveSignalSettings.MIN_NO_SIGNAL_RSRP_DBM,
            PassiveSignalSettings(noSignalRsrpDbm = -140).normalized().noSignalRsrpDbm
        )
        assertEquals(
            -125,
            PassiveSignalSettings(noSignalRsrpDbm = -125).normalized().noSignalRsrpDbm
        )
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
    fun normalizePassiveSignalSettingsRaisesWifiCallingClickIntervalBelowPulseDuration() {
        val settings = PassiveSignalSettings(
            wifiCallingTierClickIntervalMs = 50,
            wifiCallingTierPulseDurationMs = 250
        )
        val normalized = SettingsCompatibility.normalizePassiveSignalSettings(
            settings,
            signalPulseDurationMs = 100
        )
        assertEquals(280, normalized.wifiCallingTierClickIntervalMs)
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
    fun passiveOnlyHonorsConfiguredIntervalAtThirtyMsFloor() {
        assertEquals(
            50L,
            SettingsCompatibility.resolveTierClickIntervalMs(
                configuredMs = 50L,
                signalPulseDurationMs = 20,
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
