package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SignalMeasurementTierTest {

    private val settings = PassiveSignalSettings(
        noSignalRsrpDbm = -125,
        poorRsrpMinDbm = -120,
        fairRsrpMinDbm = -105,
        goodRsrpMinDbm = -100,
        mildRsrpMinDbm = -95,
        veryStrongRsrpMinDbm = -80,
        rsrqFairMinDb = -18
    )

    @Test
    fun rxssNumbersMatchCatalogueForImplementedStates() {
        assertEquals(Rxss.SIGNAL_HIGH, SignalMeasurementTier.VERY_STRONG.rxssNumber)
        assertEquals(Rxss.LEVEL_RANGE_A, SignalStrengthTier.MILD.rxssNumber)
        assertEquals(Rxss.LEVEL_RANGE_B, SignalStrengthTier.GOOD.rxssNumber)
        assertEquals(Rxss.LEVEL_RANGE_C, SignalStrengthTier.FAIR.rxssNumber)
        assertEquals(Rxss.LEVEL_RANGE_D, SignalStrengthTier.POOR.rxssNumber)
        assertEquals(Rxss.SIGNAL_LOW, SignalStrengthTier.CRITICAL.rxssNumber)
        assertEquals(Rxss.G2_GOOD, SignalMeasurementTier.G2_STRONG.rxssNumber)
        assertEquals(Rxss.G2_WEAK, SignalMeasurementTier.G2_WEAK.rxssNumber)
        assertEquals(Rxss.DEADZONE, SignalMeasurementTier.DEADZONE.rxssNumber)
        assertEquals(Rxss.LTE_NR_NO_SIGNAL, SignalMeasurementTier.NO_SIGNAL.rxssNumber)
        assertEquals(Rxss.SEARCH_HOME_2G, SignalMeasurementTier.SEARCHING_2G.rxssNumber)
        assertEquals(Rxss.LIMITED_ALT_4G, SignalMeasurementTier.LIMITED_SERVICE.rxssNumber)
        assertEquals(Rxss.LIMITED_ALT_2G, SignalMeasurementTier.LIMITED_ALT_2G.rxssNumber)
        assertEquals(Rxss.RSRQ_POOR, SignalMeasurementTier.RSRQ_POOR.rxssNumber)
        assertEquals(Rxss.HOME_2G_NO_SIGNAL, SignalMeasurementTier.G2_NO_SIGNAL.rxssNumber)
        assertEquals(Rxss.CELL_CHANGE, MonitoringAnnouncementKind.CELL_IDENTITY.rxssNumber)
        assertEquals(Rxss.WIFI_CALLING_NO_SIGNAL, SignalMeasurementTier.WIFI_CALLING.rxssNumber)
    }

    @Test
    fun wifiCallingActiveMapsToWifiCallingTierNotNoSignalOrSearching() {
        val stats = ConnectivityStats(
            isMonitoring = true,
            isWifiCallingActive = true,
            noSignalActive = true,
            networkModePreference = NetworkModePreference.ALL_TECHNOLOGIES,
            monitor2gFallbackEnabled = true,
            signalPermissionGranted = true,
            cellularAvailable = false,
            searching2gFallbackActive = false
        )
        assertEquals(SignalMeasurementTier.WIFI_CALLING, stats.resolveSignalMeasurementTier(settings))
        assertTrue(stats.resolveSignalMeasurementTier(settings).isNoSignalRxss())
    }

    @Test
    fun wifiCallingInactiveWithNoSignalStillMapsToNoSignalTier() {
        val stats = ConnectivityStats(
            isMonitoring = true,
            isWifiCallingActive = false,
            noSignalActive = true,
            signalPermissionGranted = true,
            cellularAvailable = false
        )
        assertEquals(SignalMeasurementTier.NO_SIGNAL, stats.resolveSignalMeasurementTier(settings))
    }

    @Test
    fun home2gNoSignalMapsToG2NoSignalTierNotTier10() {
        val stats = ConnectivityStats(
            isMonitoring = true,
            isOn2g = true,
            monitor2gFallbackEnabled = true,
            noSignalActive = true,
            signalPermissionGranted = true,
            cellularAvailable = true,
            rsrpDbm = PassiveSignalSettings.MIN_RSRP_DBM
        )
        assertEquals(SignalMeasurementTier.G2_NO_SIGNAL, stats.resolveSignalMeasurementTier(settings))
    }

    @Test
    fun veryStrongRsrpMapsToVeryStrongTier() {
        val stats = baseStats(rsrpDbm = -75, rsrqDb = -12)
        assertEquals(SignalMeasurementTier.VERY_STRONG, stats.resolveSignalMeasurementTier(settings))
    }

    @Test
    fun poorRsrq_coexistsWithRsrpTier() {
        val stats = baseStats(rsrpDbm = -94, rsrqDb = -20)
        assertEquals(SignalMeasurementTier.MILD, stats.resolveSignalMeasurementTier(settings))
        assertTrue(stats.isRsrqPoor(settings))
    }

    @Test
    fun poorRsrqAndPoorRsrp_bothActive() {
        val stats = baseStats(rsrpDbm = -110, rsrqDb = -20)
        assertEquals(SignalMeasurementTier.POOR, stats.resolveSignalMeasurementTier(settings))
        assertTrue(stats.isRsrqPoor(settings))
    }

    @Test
    fun weakRsrpMapsToPoorTier() {
        val stats = baseStats(rsrpDbm = -110, rsrqDb = -12)
        assertEquals(SignalMeasurementTier.POOR, stats.resolveSignalMeasurementTier(settings))
    }

    @Test
    fun limitedServiceKeepsCampTierWithSignalOverlay() {
        val stats = baseStats(rsrpDbm = -75, rsrqDb = -12, isLimitedService = true)
        assertEquals(SignalMeasurementTier.LIMITED_SERVICE, stats.resolveSignalMeasurementTier(settings))
        assertEquals(Rxss.SIGNAL_HIGH, stats.resolveLimitedServiceSignalOverlayRxss(settings))
    }

    @Test
    fun noSignalRsrpMapsToNoSignalTier() {
        val stats = baseStats(rsrpDbm = -126, rsrqDb = -12)
        assertEquals(SignalMeasurementTier.NO_SIGNAL, stats.resolveSignalMeasurementTier(settings))
    }

    @Test
    fun searching2gFallbackMapsToSearchingTier() {
        val stats = ConnectivityStats(
            isMonitoring = true,
            noSignalActive = true,
            searching2gFallbackActive = true,
            signalPermissionGranted = true
        )
        assertEquals(SignalMeasurementTier.SEARCHING_2G, stats.resolveSignalMeasurementTier(settings))
    }

    @Test
    fun g2WeakSignal_detectedBelowRxSplit() {
        val stats = ConnectivityStats(
            isMonitoring = true,
            isOn2g = true,
            monitor2gFallbackEnabled = true,
            signalPermissionGranted = true,
            cellularAvailable = true,
            rsrpDbm = -110
        )
        assertTrue(stats.isG2WeakSignal(settings))
        assertEquals(SignalMeasurementTier.G2_WEAK, stats.resolveSignalMeasurementTier(settings))
    }

    @Test
    fun g2StrongSignal_isNotG2WeakSignal() {
        val stats = ConnectivityStats(
            isMonitoring = true,
            isOn2g = true,
            monitor2gFallbackEnabled = true,
            signalPermissionGranted = true,
            cellularAvailable = true,
            rsrpDbm = -95
        )
        assertFalse(stats.isG2WeakSignal(settings))
        assertEquals(SignalMeasurementTier.G2_STRONG, stats.resolveSignalMeasurementTier(settings))
    }

    @Test
    fun completeNoServiceMapsToDeadzoneTier() {
        val stats = ConnectivityStats(
            isMonitoring = true,
            isCompleteNoService = true,
            cellularAvailable = false,
            signalPermissionGranted = true
        )
        assertEquals(SignalMeasurementTier.DEADZONE, stats.resolveSignalMeasurementTier(settings))
    }

    private fun baseStats(
        rsrpDbm: Int?,
        rsrqDb: Int?,
        isLimitedService: Boolean = false
    ): ConnectivityStats {
        return ConnectivityStats(
            cellularAvailable = true,
            rsrpDbm = rsrpDbm,
            rsrqDb = rsrqDb,
            isLimitedService = isLimitedService,
            signalPermissionGranted = true
        )
    }
}
