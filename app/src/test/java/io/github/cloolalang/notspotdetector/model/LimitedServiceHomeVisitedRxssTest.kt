package io.github.cloolalang.notspotdetector.model

import io.github.cloolalang.notspotdetector.network.CellularSignalReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LimitedServiceHomeVisitedRxssTest {

    private val settings = PassiveSignalSettings(
        noSignalRsrpDbm = -125,
        poorRsrpMinDbm = -120,
        fairRsrpMinDbm = -105,
        goodRsrpMinDbm = -100,
        mildRsrpMinDbm = -95,
        veryStrongRsrpMinDbm = -80
    )

    @Test
    fun limitedHome2g_mapsToRxss22() {
        val stats = limitedHome2g(rsrpDbm = -95)
        assertTrue(stats.isLimitedServiceHome2g())
        assertFalse(stats.isLimitedServiceAlt2g())
        assertEquals(SignalMeasurementTier.LIMITED_HOME_2G, stats.resolveSignalMeasurementTier(settings))
        assertEquals(Rxss.G2_GOOD, stats.resolveLimitedServiceSignalOverlayRxss(settings))
    }

    @Test
    fun limitedHome2gLowSignal_mapsToRxss22Overlay8() {
        val stats = limitedHome2g(rsrpDbm = -110)
        assertEquals(SignalMeasurementTier.LIMITED_HOME_2G, stats.resolveSignalMeasurementTier(settings))
        assertEquals(Rxss.G2_WEAK, stats.resolveLimitedServiceSignalOverlayRxss(settings))
        assertTrue(stats.isG2WeakSignal(settings))
        assertTrue(stats.isSignalLowVoiceCamp(settings))
    }

    @Test
    fun limitedHome2gNoSignal_mapsToRxss21Not15() {
        val stats = limitedHome2g(rsrpDbm = -130)
        assertEquals(
            SignalMeasurementTier.LIMITED_HOME_2G_NO_SIGNAL,
            stats.resolveSignalMeasurementTier(settings)
        )
        assertNull(stats.resolveLimitedServiceSignalOverlayRxss(settings))
        assertEquals(Rxss.LIMITED_HOME_2G_NO_SIGNAL, stats.resolveSignalMeasurementTier(settings).rxssNumber)
        assertFalse(stats.resolveSignalMeasurementTier(settings) == SignalMeasurementTier.G2_NO_SIGNAL)
    }

    @Test
    fun limitedVisited2g_mapsToRxss13WithoutFallbackToggle() {
        val stats = limitedVisited2g(rsrpDbm = -95).copy(monitor2gFallbackEnabled = false)
        assertTrue(stats.isLimitedServiceAlt2g())
        assertEquals(SignalMeasurementTier.LIMITED_ALT_2G, stats.resolveSignalMeasurementTier(settings))
        assertEquals(Rxss.G2_GOOD, stats.resolveLimitedServiceSignalOverlayRxss(settings))
    }

    @Test
    fun limitedVisited2gLowSignal_mapsToRxss13Overlay8() {
        val stats = limitedVisited2g(rsrpDbm = -110)
        assertEquals(SignalMeasurementTier.LIMITED_ALT_2G, stats.resolveSignalMeasurementTier(settings))
        assertEquals(Rxss.G2_WEAK, stats.resolveLimitedServiceSignalOverlayRxss(settings))
    }

    @Test
    fun limitedVisited2gNoSignal_mapsToRxss23() {
        val stats = limitedVisited2g(rsrpDbm = -130)
        assertEquals(
            SignalMeasurementTier.LIMITED_ALT_2G_NO_SIGNAL,
            stats.resolveSignalMeasurementTier(settings)
        )
    }

    @Test
    fun limitedVisited4g_mapsToRxss12() {
        val stats = limitedVisited4g(rsrpDbm = -75)
        assertTrue(stats.isLimitedServiceVisited4g())
        assertEquals(SignalMeasurementTier.LIMITED_SERVICE, stats.resolveSignalMeasurementTier(settings))
        assertEquals(Rxss.SIGNAL_HIGH, stats.resolveLimitedServiceSignalOverlayRxss(settings))
    }

    @Test
    fun limitedVisited4gLowSignal_mapsToRxss12Overlay6() {
        val stats = limitedVisited4g(rsrpDbm = -122)
        assertEquals(SignalMeasurementTier.LIMITED_SERVICE, stats.resolveSignalMeasurementTier(settings))
        assertEquals(Rxss.SIGNAL_LOW, stats.resolveLimitedServiceSignalOverlayRxss(settings))
        assertTrue(stats.isTier6CriticalSignal(settings))
    }

    @Test
    fun limitedHome4g_mapsToRxss19Not12() {
        val stats = limitedHome4g(rsrpDbm = -95)
        assertTrue(stats.isLimitedServiceHome4g())
        assertEquals(SignalMeasurementTier.LIMITED_HOME_4G, stats.resolveSignalMeasurementTier(settings))
        assertEquals(Rxss.LEVEL_RANGE_B, stats.resolveLimitedServiceSignalOverlayRxss(settings))
    }

    @Test
    fun inServiceHome2gNoSignal_staysRxss15() {
        val stats = ConnectivityStats(
            isMonitoring = true,
            isOn2g = true,
            monitor2gFallbackEnabled = true,
            noSignalActive = true,
            signalPermissionGranted = true,
            cellularAvailable = true,
            isLimitedService = false,
            rsrpDbm = -130,
            radioAccessType = CellularSignalReader.RADIO_2G,
            homeNetworkOperatorName = "Vodafone",
            servingNetworkOperatorName = "Vodafone",
            homePlmn = "23415",
            plmn = "23415"
        )
        assertEquals(SignalMeasurementTier.G2_NO_SIGNAL, stats.resolveSignalMeasurementTier(settings))
        assertEquals(Rxss.HOME_2G_NO_SIGNAL, stats.resolveSignalMeasurementTier(settings).rxssNumber)
    }

    @Test
    fun searchingHome2g_staysRxss11() {
        val stats = ConnectivityStats(
            isMonitoring = true,
            noSignalActive = true,
            searching2gFallbackActive = true,
            signalPermissionGranted = true
        )
        assertEquals(SignalMeasurementTier.SEARCHING_2G, stats.resolveSignalMeasurementTier(settings))
        assertEquals(Rxss.SEARCH_HOME_2G, stats.resolveSignalMeasurementTier(settings).rxssNumber)
    }

    @Test
    fun mockHomeLimited2g_mapsToRxss22() {
        val stats = PassiveMockSettings(
            enabled = true,
            scenario = MockNetworkScenario.HOME_LIMITED_2G,
            rsrpDbm = -95
        ).toConnectivityStats(
            monitor2gFallback = true,
            passiveSettings = settings,
            passiveIdleMode = false,
            passiveOnlySession = true
        )
        assertEquals(SignalMeasurementTier.LIMITED_HOME_2G, stats.resolveSignalMeasurementTier(settings))
    }

    @Test
    fun mockHomeLimited4g_mapsToRxss19() {
        val stats = PassiveMockSettings(
            enabled = true,
            scenario = MockNetworkScenario.HOME_LIMITED_4G,
            rsrpDbm = -95
        ).toConnectivityStats(
            monitor2gFallback = true,
            passiveSettings = settings,
            passiveIdleMode = false,
            passiveOnlySession = true
        )
        assertEquals(SignalMeasurementTier.LIMITED_HOME_4G, stats.resolveSignalMeasurementTier(settings))
    }

    private fun limitedHome2g(rsrpDbm: Int): ConnectivityStats {
        return ConnectivityStats(
            isMonitoring = true,
            isLimitedService = true,
            isOn2g = true,
            monitor2gFallbackEnabled = true,
            signalPermissionGranted = true,
            cellularAvailable = true,
            hasHomeGsmSignal = true,
            rsrpDbm = rsrpDbm,
            radioAccessType = CellularSignalReader.RADIO_2G,
            homeNetworkOperatorName = "Vodafone",
            servingNetworkOperatorName = "Vodafone",
            homePlmn = "23415",
            plmn = "23415"
        )
    }

    private fun limitedVisited2g(rsrpDbm: Int): ConnectivityStats {
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

    private fun limitedVisited4g(rsrpDbm: Int): ConnectivityStats {
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

    private fun limitedHome4g(rsrpDbm: Int): ConnectivityStats {
        return ConnectivityStats(
            isMonitoring = true,
            isLimitedService = true,
            signalPermissionGranted = true,
            cellularAvailable = true,
            rsrpDbm = rsrpDbm,
            rsrqDb = -12,
            radioAccessType = CellularSignalReader.RADIO_4G,
            homeNetworkOperatorName = "Vodafone",
            servingNetworkOperatorName = "Vodafone",
            homePlmn = "23415",
            plmn = "23415"
        )
    }
}
