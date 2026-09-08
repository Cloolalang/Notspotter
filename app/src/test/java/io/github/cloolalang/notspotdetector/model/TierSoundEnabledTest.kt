package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TierSoundEnabledTest {

    @Test
    fun shouldPlayCurrentTierSignalPulse_respectsVeryStrongToggle() {
        val settings = PassiveSignalSettings(
            veryStrongRsrpMinDbm = -80,
            veryStrongTierSoundEnabled = false
        )
        val stats = ConnectivityStats(
            isMonitoring = true,
            cellularAvailable = true,
            signalPermissionGranted = true,
            rsrpDbm = -75
        )

        assertTrue(stats.shouldPlayVeryStrongSignalIndicator(settings))
        assertFalse(stats.shouldPlayCurrentTierSignalPulse(settings))
    }

    @Test
    fun shouldPlayCurrentTierSignalPulse_respectsResolvedTierToggle() {
        val settings = PassiveSignalSettings(
            mildRsrpMinDbm = -95,
            goodRsrpMinDbm = -100,
            fairTierSoundEnabled = false
        )
        val stats = ConnectivityStats(
            isMonitoring = true,
            cellularAvailable = true,
            signalPermissionGranted = true,
            rsrpDbm = -102,
            rsrqDb = -10
        )

        assertFalse(stats.shouldPlayCurrentTierSignalPulse(settings))

        val enabled = settings.copy(fairTierSoundEnabled = true)
        assertTrue(stats.shouldPlayCurrentTierSignalPulse(enabled))
    }
}
