package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeadzoneTierTest {

    private val settings = PassiveSignalSettings()

    @Test
    fun resolveSignalMeasurementTier_mapsCompleteNoServiceToDeadzone() {
        val stats = deadzoneStats()

        assertEquals(SignalMeasurementTier.DEADZONE, stats.resolveSignalMeasurementTier(settings))
        assertEquals(DEADZONE_TIER_NUMBER, stats.resolveSignalMeasurementTier(settings).displayNumber)
    }

    @Test
    fun shouldPlayDeadzoneTier_requiresMonitoringCompleteNoServiceAndToggle() {
        val stats = deadzoneStats()
        val disabled = settings.copy(deadzoneTierSoundEnabled = false)

        assertTrue(stats.shouldPlayDeadzoneTier(settings))
        assertFalse(stats.shouldPlayDeadzoneTier(disabled))
        assertFalse(stats.copy(isMonitoring = false).shouldPlayDeadzoneTier(settings))
        assertFalse(stats.copy(isCompleteNoService = false).shouldPlayDeadzoneTier(settings))
    }

    @Test
    fun shouldPlayContinuousFlatline_disabledWhenDeadzoneTierSoundEnabled() {
        val stats = deadzoneStats().copy(noSignalActive = true)

        assertFalse(stats.shouldPlayContinuousFlatline(settings))
        assertTrue(stats.shouldPlayContinuousFlatline(settings.copy(deadzoneTierSoundEnabled = false)))
    }

    @Test
    fun computeDeadzoneClickIntervalMs_usesDeadzonePulseDuration() {
        val stats = deadzoneStats().copy(isPassiveOnlySession = true)
        val custom = settings.copy(
            deadzoneTierClickIntervalMs = 400,
            deadzoneTierPulseDurationMs = 300
        )

        assertEquals(400L, stats.computeDeadzoneClickIntervalMs(custom))
    }

    @Test
    fun deadzoneTierPulseDuration_isIndependentOfGlobalPulseDuration() {
        val custom = settings.copy(deadzoneTierPulseDurationMs = 400)
        assertEquals(400, custom.normalized().deadzoneTierPulseDurationMs)
    }

    private fun deadzoneStats(): ConnectivityStats {
        return ConnectivityStats(
            isMonitoring = true,
            noSignalActive = true,
            isCompleteNoService = true,
            cellularAvailable = false,
            signalPermissionGranted = true
        )
    }
}
