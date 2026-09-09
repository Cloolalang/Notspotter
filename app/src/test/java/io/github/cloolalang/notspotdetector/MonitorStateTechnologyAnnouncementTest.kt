package io.github.cloolalang.notspotdetector

import io.github.cloolalang.notspotdetector.model.ConnectivityStats
import io.github.cloolalang.notspotdetector.model.MockNetworkScenario
import io.github.cloolalang.notspotdetector.model.MonitoringAnnouncementKind
import io.github.cloolalang.notspotdetector.model.MonitoringSettings
import io.github.cloolalang.notspotdetector.model.PassiveMockSettings
import io.github.cloolalang.notspotdetector.model.PassiveSignalSettings
import io.github.cloolalang.notspotdetector.model.toConnectivityStats
import io.github.cloolalang.notspotdetector.network.CellularSignalReader
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MonitorStateTechnologyAnnouncementTest {

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
    fun mockHome4gToHome2g_announcesTechnologyChangeImmediately() {
        val mock = PassiveMockSettings(enabled = true, scenario = MockNetworkScenario.HOME_4G)
        val home4g = mock.toConnectivityStats(
            monitor2gFallback = true,
            passiveSettings = passiveSettings,
            passiveIdleMode = false,
            passiveOnlySession = true
        )
        val baseline = MonitorState.updateStats(home4g)
        assertFalse(baseline.radioTechnologyChanged)

        val home2g = mock.copy(scenario = MockNetworkScenario.HOME_2G).toConnectivityStats(
            monitor2gFallback = true,
            passiveSettings = passiveSettings,
            passiveIdleMode = false,
            passiveOnlySession = true
        )
        val events = MonitorState.updateStats(home2g)

        assertTrue(events.cellIdentityChanged)
        assertTrue(events.cellChangeAnnouncement!!.contains("cell reselect"))
        assertTrue(events.radioTechnologyChanged)
        assertNotNull(events.technologyChangeAnnouncement)
        assertTrue(events.technologyChangeAnnouncement!!.contains("2 G"))
        assertEquals(CellularSignalReader.RADIO_2G, events.technologyChangeTargetRadioAccessType)
        assertFalse(events.g2FallbackAnnounced)
    }

    @Test
    fun mockHome2gToHome4g_announcesCellReselectAndTechnologyChange() {
        val mock = PassiveMockSettings(enabled = true, scenario = MockNetworkScenario.HOME_2G)
        MonitorState.updateStats(
            mock.toConnectivityStats(
                monitor2gFallback = true,
                passiveSettings = passiveSettings,
                passiveIdleMode = false,
                passiveOnlySession = true
            )
        )

        val events = MonitorState.updateStats(
            mock.copy(scenario = MockNetworkScenario.HOME_4G).toConnectivityStats(
                monitor2gFallback = true,
                passiveSettings = passiveSettings,
                passiveIdleMode = false,
                passiveOnlySession = true
            )
        )

        assertTrue(events.cellIdentityChanged)
        assertTrue(events.radioTechnologyChanged)
        assertTrue(events.technologyChangeAnnouncement!!.contains("4 G"))
    }

    @Test
    fun mockHome4gToAlt4g_announcesCellReselectAndLimitedService() {
        val mock = PassiveMockSettings(enabled = true, scenario = MockNetworkScenario.HOME_4G)
        MonitorState.updateStats(
            mock.toConnectivityStats(
                monitor2gFallback = true,
                passiveSettings = passiveSettings,
                passiveIdleMode = false,
                passiveOnlySession = true
            )
        )

        val events = MonitorState.updateStats(
            mock.copy(scenario = MockNetworkScenario.ALT_OPERATOR_4G).toConnectivityStats(
                monitor2gFallback = true,
                passiveSettings = passiveSettings,
                passiveIdleMode = false,
                passiveOnlySession = true
            )
        )

        assertTrue(events.cellIdentityChanged)
        assertFalse(events.radioTechnologyChanged)
        assertTrue(events.limitedServiceStateChanged)
        assertTrue(events.limitedServiceStateAnnouncement!!.contains("limited service"))
    }

    @Test
    fun mockHome4gToAlt4g_playsLimitedServiceBeforeCellReselect() {
        val mock = PassiveMockSettings(enabled = true, scenario = MockNetworkScenario.HOME_4G)
        MonitorState.updateStats(
            mock.toConnectivityStats(
                monitor2gFallback = true,
                passiveSettings = passiveSettings,
                passiveIdleMode = false,
                passiveOnlySession = true
            )
        )

        val events = MonitorState.updateStats(
            mock.copy(scenario = MockNetworkScenario.ALT_OPERATOR_4G).toConnectivityStats(
                monitor2gFallback = true,
                passiveSettings = passiveSettings,
                passiveIdleMode = false,
                passiveOnlySession = true
            )
        )

        val order = events.immediateAnnouncements().map { it.kind }
        val limitedIndex = order.indexOf(MonitoringAnnouncementKind.LIMITED_SERVICE_STATE)
        val cellIndex = order.indexOf(MonitoringAnnouncementKind.CELL_IDENTITY)
        assertTrue(limitedIndex >= 0)
        assertTrue(cellIndex >= 0)
        assertTrue(limitedIndex < cellIndex)
    }

    @Test
    fun lteNoSignalThen2gCamp_usesG2FallbackNotTechnologyChange() {
        val lteNoSignal = ConnectivityStats(
            isMonitoring = true,
            isPassiveOnlySession = true,
            monitor2gFallbackEnabled = true,
            cellularAvailable = false,
            radioAccessType = CellularSignalReader.RADIO_4G,
            isOn2g = false,
            noSignalActive = true,
            networkOperatorName = PassiveMockSettings.MOCK_HOME_OPERATOR
        )
        MonitorState.updateStats(lteNoSignal)
        MonitorState.updateStats(lteNoSignal)

        val camped2g = lteNoSignal.copy(
            cellularAvailable = true,
            radioAccessType = CellularSignalReader.RADIO_2G,
            isOn2g = true,
            noSignalActive = false,
            hasHomeGsmSignal = true,
            hasLteNrSignal = false
        )
        val events = MonitorState.updateStats(camped2g)

        assertFalse(events.radioTechnologyChanged)
        assertTrue(events.g2FallbackAnnounced)
        assertNotNull(events.g2FallbackAnnouncement)
        assertTrue(events.g2FallbackAnnouncement!!.contains("2 G"))
    }

    @Test
    fun leavingLimitedService_doesNotAnnounceFullService() {
        val homeMock = PassiveMockSettings(enabled = true, scenario = MockNetworkScenario.HOME_4G)
        val altMock = homeMock.copy(scenario = MockNetworkScenario.ALT_OPERATOR_4G)
        MonitorState.updateStats(
            homeMock.toConnectivityStats(
                monitor2gFallback = true,
                passiveSettings = passiveSettings,
                passiveIdleMode = false,
                passiveOnlySession = true
            )
        )
        val enterLimited = MonitorState.updateStats(
            altMock.toConnectivityStats(
                monitor2gFallback = true,
                passiveSettings = passiveSettings,
                passiveIdleMode = false,
                passiveOnlySession = true
            )
        )
        assertTrue(enterLimited.limitedServiceStateChanged)

        val events = MonitorState.updateStats(
            homeMock.toConnectivityStats(
                monitor2gFallback = true,
                passiveSettings = passiveSettings,
                passiveIdleMode = false,
                passiveOnlySession = true
            )
        )

        assertFalse(events.limitedServiceStateChanged)
        assertNull(events.limitedServiceStateAnnouncement)
    }
}
