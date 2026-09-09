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
import io.github.cloolalang.notspotdetector.model.toConnectivityStats
import io.github.cloolalang.notspotdetector.network.CellularSignalReader
import org.junit.After
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
        assertFalse(stats.shouldAllowLimitedServicePeriodicVoice(passiveSettings))
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
        assertFalse(stats.shouldAllowLimitedServicePeriodicVoice(passiveSettings))
        assertTrue(stats.shouldPlayLimitedServiceSignalOverlay(passiveSettings))
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
}
