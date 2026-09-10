package io.github.cloolalang.notspotdetector.model

import io.github.cloolalang.notspotdetector.network.CellularSignalReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LimitedServiceSignalOverlayTest {

    private val settings = PassiveSignalSettings(
        noSignalRsrpDbm = -125,
        poorRsrpMinDbm = -120,
        fairRsrpMinDbm = -105,
        goodRsrpMinDbm = -100,
        mildRsrpMinDbm = -95,
        veryStrongRsrpMinDbm = -80
    )

    @Test
    fun limitedAlt4gNoSignalMapsToRxss20() {
        val stats = limitedAlt4g(rsrpDbm = -130)
        assertTrue(stats.isLimitedServiceNoSignalCamp(settings))
        assertEquals(
            SignalMeasurementTier.LIMITED_4G_NO_SIGNAL,
            stats.resolveSignalMeasurementTier(settings)
        )
        assertNull(stats.resolveLimitedServiceSignalOverlayRxss(settings))
        assertFalse(stats.shouldAllowLimitedServicePeriodicVoice(settings))
    }

    @Test
    fun limitedAlt4gVeryStrong_playsSignalStrengthInterval() {
        val stats = limitedAlt4g(rsrpDbm = -75)
        assertTrue(stats.shouldPlayLimitedServiceSignalOverlay(settings))
        assertTrue(stats.shouldPlayVeryStrongSignalIndicator(settings))
        assertTrue(stats.shouldPlaySignalStrengthInterval(settings))
        // A measurable-signal overlay (not the critical/weak "signal low" overlay) must still let
        // the 30s VA-14 "limited service" reminder cycle.
        assertTrue(stats.shouldAllowLimitedServicePeriodicVoice(settings))
    }

    @Test
    fun limitedAlt4gCriticalRsrpShowsOverlay6() {
        val stats = limitedAlt4g(rsrpDbm = -122)
        assertFalse(stats.isLimitedServiceNoSignalCamp(settings))
        assertEquals(SignalMeasurementTier.LIMITED_SERVICE, stats.resolveSignalMeasurementTier(settings))
        assertEquals(Rxss.SIGNAL_LOW, stats.resolveLimitedServiceSignalOverlayRxss(settings))
        assertTrue(stats.isTier6CriticalSignal(settings))
        assertFalse(stats.shouldAllowLimitedServicePeriodicVoice(settings))
    }

    @Test
    fun limitedAlt2gNoSignalMapsToRxss23() {
        val stats = limitedAlt2g(rsrpDbm = -130)
        assertTrue(stats.isLimitedServiceNoSignalCamp(settings))
        assertEquals(
            SignalMeasurementTier.LIMITED_ALT_2G_NO_SIGNAL,
            stats.resolveSignalMeasurementTier(settings)
        )
        assertNull(stats.resolveLimitedServiceSignalOverlayRxss(settings))
    }

    @Test
    fun limitedAlt2gWeakRxShowsOverlay8() {
        val stats = limitedAlt2g(rsrpDbm = -110)
        assertFalse(stats.isLimitedServiceNoSignalCamp(settings))
        assertEquals(SignalMeasurementTier.LIMITED_ALT_2G, stats.resolveSignalMeasurementTier(settings))
        assertEquals(Rxss.G2_WEAK, stats.resolveLimitedServiceSignalOverlayRxss(settings))
        assertTrue(stats.isG2WeakSignal(settings))
        // VA-18 "signal low" periodic takes over on the weak overlay — VA-14 stays suppressed.
        assertFalse(stats.shouldAllowLimitedServicePeriodicVoice(settings))
    }

    @Test
    fun limitedAlt2gStrongRxShowsOverlay7() {
        val stats = limitedAlt2g(rsrpDbm = -95)
        assertEquals(Rxss.G2_GOOD, stats.resolveLimitedServiceSignalOverlayRxss(settings))
        assertFalse(stats.isG2WeakSignal(settings))
        // Strong 2G overlay is not the critical/weak "signal low" case — VA-14 must cycle every 30s.
        assertTrue(stats.shouldAllowLimitedServicePeriodicVoice(settings))
    }

    @Test
    fun mockVisited4gNoSignal_resolvesRxss20() {
        val stats = PassiveMockSettings(
            enabled = true,
            scenario = MockNetworkScenario.ALT_OPERATOR_4G,
            rsrpDbm = -130
        ).toConnectivityStats(
            monitor2gFallback = true,
            passiveSettings = settings,
            passiveIdleMode = false,
            passiveOnlySession = true
        )
        assertEquals(
            SignalMeasurementTier.LIMITED_4G_NO_SIGNAL,
            stats.resolveSignalMeasurementTier(settings)
        )
    }

    private fun limitedAlt4g(rsrpDbm: Int): ConnectivityStats {
        return ConnectivityStats(
            isMonitoring = true,
            isLimitedService = true,
            signalPermissionGranted = true,
            cellularAvailable = true,
            rsrpDbm = rsrpDbm,
            rsrqDb = -12,
            radioAccessType = CellularSignalReader.RADIO_4G,
            homeNetworkOperatorName = "Vodafone",
            servingNetworkOperatorName = "EE",
            homePlmn = "23415",
            plmn = "23430"
        )
    }

    private fun limitedAlt2g(rsrpDbm: Int): ConnectivityStats {
        return ConnectivityStats(
            isMonitoring = true,
            isLimitedService = true,
            isOn2g = true,
            monitor2gFallbackEnabled = true,
            signalPermissionGranted = true,
            cellularAvailable = true,
            hasHomeGsmSignal = false,
            rsrpDbm = rsrpDbm,
            radioAccessType = CellularSignalReader.RADIO_2G,
            homeNetworkOperatorName = "Vodafone",
            servingNetworkOperatorName = "EE",
            homePlmn = "23415",
            plmn = "23430"
        )
    }
}
