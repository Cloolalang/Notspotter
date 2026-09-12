package io.github.cloolalang.notspotdetector

import io.github.cloolalang.notspotdetector.model.ConnectivityStats
import io.github.cloolalang.notspotdetector.model.MockNetworkScenario
import io.github.cloolalang.notspotdetector.model.MonitoringSettings
import io.github.cloolalang.notspotdetector.model.PassiveMockSettings
import io.github.cloolalang.notspotdetector.model.PassiveSignalSettings
import io.github.cloolalang.notspotdetector.model.shouldAllowCellReselectVoice
import io.github.cloolalang.notspotdetector.model.shouldAllowG2CampedPeriodicVoice
import io.github.cloolalang.notspotdetector.model.shouldAllowG2WeakPeriodicVoice
import io.github.cloolalang.notspotdetector.model.shouldAllowLimitedServicePeriodicVoice
import io.github.cloolalang.notspotdetector.model.shouldPlayLimitedServiceSignalOverlay
import io.github.cloolalang.notspotdetector.model.shouldPlayTier5StylePeriodicVoice
import io.github.cloolalang.notspotdetector.model.isSignalLowVoiceCamp
import io.github.cloolalang.notspotdetector.model.toConnectivityStats
import io.github.cloolalang.notspotdetector.network.CellularSignalReader
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MonitorStateCellReselectTest {

    private val passiveSettings = PassiveSignalSettings()
    private val monitor2gFallback = MonitoringSettings(monitor2gFallback = true)

    @Before
    fun setUp() {
        MonitorState.setRunning(false)
        MonitorState.setMonitoringSettings(monitor2gFallback)
        MonitorState.setPassiveSignalSettings(passiveSettings)
        MonitorState.beginPassiveOnlySession()
        MonitorState.setRunning(true)
    }

    @After
    fun tearDown() {
        MonitorState.setRunning(false)
    }

    @Test
    fun noSignalRxssBlocksCellReselectVoice() {
        val noSignal = ConnectivityStats(
            isMonitoring = true,
            isPassiveOnlySession = true,
            noSignalActive = true,
            radioAccessType = CellularSignalReader.RADIO_4G,
            lteEarfcn = 1_800,
            ltePci = 42,
            signalPermissionGranted = true,
            cellIdentityPermissionGranted = true
        )
        assertFalse(noSignal.shouldAllowCellReselectVoice(passiveSettings))
    }

    @Test
    fun searching2gRxssBlocksCellReselectVoice() {
        // RXSS 11 (searching for home 2G) is catalogued as Signal = "—" (transition), not "No",
        // but there is still no camped cell to report — the stale LTE identity from before the
        // no-signal episode must not trigger a cell-reselect announcement.
        val searching2g = ConnectivityStats(
            isMonitoring = true,
            isPassiveOnlySession = true,
            noSignalActive = true,
            searching2gFallbackActive = true,
            monitor2gFallbackEnabled = true,
            radioAccessType = CellularSignalReader.RADIO_4G,
            lteEarfcn = 1_800,
            ltePci = 42,
            signalPermissionGranted = true,
            cellIdentityPermissionGranted = true
        )
        assertFalse(searching2g.shouldAllowCellReselectVoice(passiveSettings))
    }

    @Test
    fun deadzoneRxssBlocksCellReselectVoice() {
        val deadzone = ConnectivityStats(
            isMonitoring = true,
            isPassiveOnlySession = true,
            isCompleteNoService = true,
            signalPermissionGranted = true,
            cellIdentityPermissionGranted = true
        )
        assertFalse(deadzone.shouldAllowCellReselectVoice(passiveSettings))
    }

    @Test
    fun campedLteWithNoRsrpMeasurementBlocksCellReselectVoice() {
        // Camped on LTE (radioAccessType present) but no RSRP reading yet — e.g. right after
        // losing signal, before the two-poll noSignalActive debounce confirms it. The
        // measurement tier resolves to UNAVAILABLE (not NO_SIGNAL), so this must be checked
        // directly or a stale/flickering PCI would still trigger a cell-reselect announcement.
        val campedNoMeasurement = ConnectivityStats(
            isMonitoring = true,
            isPassiveOnlySession = true,
            cellularAvailable = true,
            noSignalActive = false,
            radioAccessType = CellularSignalReader.RADIO_4G,
            rsrpDbm = null,
            rsrqDb = null,
            lteEarfcn = 1_800,
            ltePci = 42,
            signalPermissionGranted = true,
            cellIdentityPermissionGranted = true
        )
        assertFalse(campedNoMeasurement.shouldAllowCellReselectVoice(passiveSettings))
    }

    @Test
    fun fairSignalAllowsCellReselectVoice() {
        val mock = PassiveMockSettings(
            enabled = true,
            scenario = MockNetworkScenario.HOME_4G,
            rsrpDbm = -95
        )
        val stats = mock.toConnectivityStats(
            monitor2gFallback = true,
            passiveSettings = passiveSettings,
            passiveIdleMode = false,
            passiveOnlySession = true
        )
        assertTrue(stats.shouldAllowCellReselectVoice(passiveSettings))
    }

    @Test
    fun lteNoSignalCamp_doesNotAnnounceCellReselectEvenIfIdentityFlickers() {
        val mock = PassiveMockSettings(
            enabled = true,
            scenario = MockNetworkScenario.HOME_4G,
            rsrpDbm = -95
        )
        MonitorState.updateStats(
            mock.toConnectivityStats(
                monitor2gFallback = true,
                passiveSettings = passiveSettings,
                passiveIdleMode = false,
                passiveOnlySession = true
            )
        )

        val noSignalCamp = mock.copy(rsrpDbm = -130).toConnectivityStats(
            monitor2gFallback = true,
            passiveSettings = passiveSettings,
            passiveIdleMode = false,
            passiveOnlySession = true
        )
        MonitorState.updateStats(noSignalCamp)
        val events = MonitorState.updateStats(
            noSignalCamp.copy(
                lteEarfcn = 1_900,
                ltePci = 99
            )
        )

        assertNull(events.cellChangeAnnouncement)
        assertFalse(events.cellIdentityChanged)
    }

    @Test
    fun g2NoSignalRxssBlocksCampedPeriodicVoice() {
        val g2NoSignal = ConnectivityStats(
            isMonitoring = true,
            isPassiveOnlySession = true,
            isOn2g = true,
            monitor2gFallbackEnabled = true,
            noSignalActive = true,
            hasHomeGsmSignal = true,
            radioAccessType = CellularSignalReader.RADIO_2G,
            signalPermissionGranted = true
        )
        assertFalse(g2NoSignal.shouldAllowG2CampedPeriodicVoice(passiveSettings))
    }

    @Test
    fun g2StrongCampAllowsPeriodicCampedVoice() {
        val mock = PassiveMockSettings(
            enabled = true,
            scenario = MockNetworkScenario.HOME_2G,
            rsrpDbm = -95
        )
        val stats = mock.toConnectivityStats(
            monitor2gFallback = true,
            passiveSettings = passiveSettings,
            passiveIdleMode = false,
            passiveOnlySession = true
        )
        assertTrue(stats.shouldAllowG2CampedPeriodicVoice(passiveSettings))
    }

    @Test
    fun limitedAlt2gWithSignalSuppressesG2CampedPeriodicVoice() {
        val mock = PassiveMockSettings(
            enabled = true,
            scenario = MockNetworkScenario.ALT_OPERATOR_2G,
            rsrpDbm = -95
        )
        val stats = mock.toConnectivityStats(
            monitor2gFallback = true,
            passiveSettings = passiveSettings,
            passiveIdleMode = false,
            passiveOnlySession = true
        )
        // Strong 2G overlay (RXSS 7) is not the weak/no-signal case — VA-14 now cycles every 30s
        // for it instead of staying permanently suppressed.
        assertTrue(stats.shouldAllowLimitedServicePeriodicVoice(passiveSettings))
        assertFalse(stats.shouldAllowG2CampedPeriodicVoice(passiveSettings))
        assertFalse(stats.shouldAllowG2WeakPeriodicVoice(passiveSettings))
    }

    @Test
    fun limitedServiceWithSignalAllowsPeriodicVoice() {
        val mock = PassiveMockSettings(
            enabled = true,
            scenario = MockNetworkScenario.ALT_OPERATOR_4G,
            rsrpDbm = -75
        )
        val stats = mock.toConnectivityStats(
            monitor2gFallback = true,
            passiveSettings = passiveSettings,
            passiveIdleMode = false,
            passiveOnlySession = true
        )
        // Measurable (non-critical) RSRP overlay must let the 30s VA-14 "limited service" voice cycle.
        assertTrue(stats.shouldAllowLimitedServicePeriodicVoice(passiveSettings))
        assertTrue(stats.shouldPlayLimitedServiceSignalOverlay(passiveSettings))
    }

    @Test
    fun limitedServiceCriticalOverlaySuppressesPeriodicVoiceForVa15() {
        val mock = PassiveMockSettings(
            enabled = true,
            scenario = MockNetworkScenario.ALT_OPERATOR_4G,
            rsrpDbm = -122
        )
        val stats = mock.toConnectivityStats(
            monitor2gFallback = true,
            passiveSettings = passiveSettings,
            passiveIdleMode = false,
            passiveOnlySession = true
        )
        // Critical (tier 6) overlay hands off to the shared VA-15 "signal low" periodic instead.
        assertTrue(stats.isSignalLowVoiceCamp(passiveSettings))
        assertFalse(stats.shouldAllowLimitedServicePeriodicVoice(passiveSettings))
    }

    @Test
    fun limitedServiceNoSignalRxssBlocksPeriodicVoice() {
        val mock = PassiveMockSettings(
            enabled = true,
            scenario = MockNetworkScenario.ALT_OPERATOR_4G,
            rsrpDbm = -130
        )
        val stats = mock.toConnectivityStats(
            monitor2gFallback = true,
            passiveSettings = passiveSettings,
            passiveIdleMode = false,
            passiveOnlySession = true
        )
        assertFalse(stats.shouldAllowLimitedServicePeriodicVoice(passiveSettings))
    }

    @Test
    fun limitedAlt2gWeakOverlay_onlyTier5JobHandlesPeriodicVoice_notG2Job() {
        // Regression: limited visited-2G weak overlay (RXSS 13 overlay 8) must be handled
        // exclusively by the tier5-style periodic job (shared VA-15/VA-18 wording). If the G2
        // job's weak-voice branch also returned true here, both periodic jobs would schedule and
        // the same "signal low" announcement would play twice every 30s.
        val mock = PassiveMockSettings(
            enabled = true,
            scenario = MockNetworkScenario.ALT_OPERATOR_2G,
            rsrpDbm = -110
        )
        val stats = mock.toConnectivityStats(
            monitor2gFallback = true,
            passiveSettings = passiveSettings,
            passiveIdleMode = false,
            passiveOnlySession = true
        )
        assertTrue(stats.isSignalLowVoiceCamp(passiveSettings))
        assertTrue(stats.shouldPlayTier5StylePeriodicVoice(passiveSettings))
        assertFalse(stats.shouldAllowG2WeakPeriodicVoice(passiveSettings))
        assertFalse(stats.shouldAllowG2CampedPeriodicVoice(passiveSettings))
    }

    @Test
    fun g2WeakCampAllowsPeriodicWeakVoice() {
        val mock = PassiveMockSettings(
            enabled = true,
            scenario = MockNetworkScenario.HOME_2G,
            rsrpDbm = -110
        )
        val stats = mock.toConnectivityStats(
            monitor2gFallback = true,
            passiveSettings = passiveSettings,
            passiveIdleMode = false,
            passiveOnlySession = true
        )
        assertTrue(stats.shouldAllowG2WeakPeriodicVoice(passiveSettings))
    }

    @Test
    fun g2NoSignalRxssBlocksPeriodicWeakVoice() {
        val g2NoSignal = ConnectivityStats(
            isMonitoring = true,
            isPassiveOnlySession = true,
            isOn2g = true,
            monitor2gFallbackEnabled = true,
            noSignalActive = true,
            hasHomeGsmSignal = true,
            radioAccessType = CellularSignalReader.RADIO_2G,
            rsrpDbm = -110,
            signalPermissionGranted = true
        )
        assertFalse(g2NoSignal.shouldAllowG2WeakPeriodicVoice(passiveSettings))
    }

    @Test
    fun firstCampedSnapshotDoesNotCountAsReselect() {
        MonitorState.updateStats(campedHome4g(pci = 42))

        assertEquals(0, MonitorState.stats.value.cellReselectsPerMinute)
    }

    @Test
    fun pciChangeIncrementsRollingReselectRate() {
        MonitorState.updateStats(campedHome4g(pci = 42))
        MonitorState.updateStats(campedHome4g(pci = 99))

        assertEquals(1, MonitorState.stats.value.cellReselectsPerMinute)
    }

    @Test
    fun twoPciChangesCountAsTwoReselectsPerMinute() {
        MonitorState.updateStats(campedHome4g(pci = 42))
        MonitorState.updateStats(campedHome4g(pci = 99))
        MonitorState.updateStats(campedHome4g(pci = 7))

        assertEquals(2, MonitorState.stats.value.cellReselectsPerMinute)
    }

    @Test
    fun noSignalIdentityFlickerDoesNotIncrementReselectRate() {
        val mock = PassiveMockSettings(
            enabled = true,
            scenario = MockNetworkScenario.HOME_4G,
            rsrpDbm = -95
        )
        MonitorState.updateStats(
            mock.toConnectivityStats(
                monitor2gFallback = true,
                passiveSettings = passiveSettings,
                passiveIdleMode = false,
                passiveOnlySession = true
            )
        )
        MonitorState.updateStats(campedHome4g(pci = 99))
        assertEquals(1, MonitorState.stats.value.cellReselectsPerMinute)

        val noSignalCamp = mock.copy(rsrpDbm = -130).toConnectivityStats(
            monitor2gFallback = true,
            passiveSettings = passiveSettings,
            passiveIdleMode = false,
            passiveOnlySession = true
        )
        MonitorState.updateStats(noSignalCamp)
        MonitorState.updateStats(
            noSignalCamp.copy(
                lteEarfcn = 1_900,
                ltePci = 7
            )
        )

        assertEquals(1, MonitorState.stats.value.cellReselectsPerMinute)
    }

    @Test
    fun stoppingMonitorClearsReselectRate() {
        MonitorState.updateStats(campedHome4g(pci = 42))
        MonitorState.updateStats(campedHome4g(pci = 99))
        assertEquals(1, MonitorState.stats.value.cellReselectsPerMinute)

        MonitorState.setRunning(false)

        assertEquals(0, MonitorState.stats.value.cellReselectsPerMinute)
    }

    private fun campedHome4g(pci: Int): ConnectivityStats {
        val mock = PassiveMockSettings(
            enabled = true,
            scenario = MockNetworkScenario.HOME_4G,
            rsrpDbm = -95
        )
        return mock.toConnectivityStats(
            monitor2gFallback = true,
            passiveSettings = passiveSettings,
            passiveIdleMode = false,
            passiveOnlySession = true
        ).copy(ltePci = pci)
    }
}
