package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RsrqTierTest {

    private val settings = PassiveSignalSettings(
        rsrqFairMinDb = -18,
        rsrqTierSoundEnabled = true,
        rsrqTierCoupledToSignalTier = true,
        rsrqTierWhiteNoiseVolume = 0.5f
    )

    @Test
    fun coupledWhiteNoiseMix_activeWhenRsrqPoorDuringPassiveOnly() {
        val stats = ConnectivityStats(
            isMonitoring = true,
            isPassiveOnlySession = true,
            cellularAvailable = true,
            signalPermissionGranted = true,
            rsrpDbm = -95,
            rsrqDb = -20
        )

        assertTrue(settings.shouldCoupleRsrqWhiteNoise(stats.rsrqDb, stats.isPassiveOnlySession))
        assertEquals(0.5, settings.rsrqTierWhiteNoiseMix(stats.rsrqDb, stats.isPassiveOnlySession), 0.001)
    }

    @Test
    fun decoupledRsrqTier_playsIndependently() {
        val decoupled = settings.copy(rsrqTierCoupledToSignalTier = false)
        val stats = ConnectivityStats(
            isMonitoring = true,
            isPassiveOnlySession = true,
            cellularAvailable = true,
            signalPermissionGranted = true,
            rsrpDbm = -95,
            rsrqDb = -20
        )

        assertFalse(decoupled.shouldCoupleRsrqWhiteNoise(stats.rsrqDb, stats.isPassiveOnlySession))
        assertTrue(stats.shouldPlayDecoupledRsrqTier(decoupled))
    }

    @Test
    fun rsrqFairMinDb_clampsToSliderRange() {
        val belowMin = settings.copy(rsrqFairMinDb = -25).normalized()
        assertEquals(PassiveSignalSettings.RSRQ_FAIR_MIN_DB, belowMin.rsrqFairMinDb)

        val aboveMax = settings.copy(rsrqFairMinDb = -10).normalized()
        assertEquals(PassiveSignalSettings.RSRQ_FAIR_MAX_DB, aboveMax.rsrqFairMinDb)
    }

    @Test
    fun rsrqTierClickInterval_capsAtFiveSeconds() {
        val capped = settings.copy(rsrqTierClickIntervalMs = 10_000).normalized()
        assertEquals(PassiveSignalSettings.MAX_RSRQ_TIER_CLICK_INTERVAL_MS, capped.rsrqTierClickIntervalMs)
    }

    @Test
    fun resolveSignalStrengthTier_ignoresRsrq() {
        val tier = settings.resolveSignalStrengthTier(rsrpDbm = -94)
        assertEquals(SignalStrengthTier.MILD, tier)
    }
}
