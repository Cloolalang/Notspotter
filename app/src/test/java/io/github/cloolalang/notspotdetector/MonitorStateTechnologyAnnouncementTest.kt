package io.github.cloolalang.notspotdetector

import io.github.cloolalang.notspotdetector.model.AudioVolumeSettings
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
        MonitorState.setAudioVolumes(
            AudioVolumeSettings(cellChangeSpeakBandEnabled = false)
        )
        MonitorState.beginPassiveOnlySession()
        MonitorState.setRunning(true)
    }

    @After
    fun tearDown() {
        MonitorState.setRunning(false)
        MonitorState.setAudioVolumes(AudioVolumeSettings())
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

        val limited = mock.copy(scenario = MockNetworkScenario.ALT_OPERATOR_4G).toConnectivityStats(
            monitor2gFallback = true,
            passiveSettings = passiveSettings,
            passiveIdleMode = false,
            passiveOnlySession = true
        )
        val first = MonitorState.updateStats(limited)
        var events = first
        repeat(3) { events = MonitorState.updateStats(limited) }

        assertTrue(first.cellIdentityChanged)
        assertFalse(first.limitedServiceStateChanged)
        assertFalse(events.radioTechnologyChanged)
        assertTrue(events.limitedServiceStateChanged)
        assertTrue(events.limitedServiceStateAnnouncement!!.contains("limited service"))
    }

    @Test
    fun mockHome4gToAlt4g_speaksCellReselectBeforeHeldLimitedService() {
        val mock = PassiveMockSettings(enabled = true, scenario = MockNetworkScenario.HOME_4G)
        MonitorState.updateStats(
            mock.toConnectivityStats(
                monitor2gFallback = true,
                passiveSettings = passiveSettings,
                passiveIdleMode = false,
                passiveOnlySession = true
            )
        )

        val limited = mock.copy(scenario = MockNetworkScenario.ALT_OPERATOR_4G).toConnectivityStats(
            monitor2gFallback = true,
            passiveSettings = passiveSettings,
            passiveIdleMode = false,
            passiveOnlySession = true
        )
        val first = MonitorState.updateStats(limited)
        var held = first
        repeat(3) { held = MonitorState.updateStats(limited) }

        assertTrue(first.cellIdentityChanged)
        assertFalse(first.limitedServiceStateChanged)
        assertTrue(held.limitedServiceStateChanged)
        assertFalse(held.cellIdentityChanged)
    }

    @Test
    fun onePollLimitedFlickerDuringReselect_doesNotSpeakInService() {
        val mock = PassiveMockSettings(enabled = true, scenario = MockNetworkScenario.HOME_4G)
        val home = mock.toConnectivityStats(
            monitor2gFallback = true,
            passiveSettings = passiveSettings,
            passiveIdleMode = false,
            passiveOnlySession = true
        )
        MonitorState.updateStats(home)

        val limited = mock.copy(scenario = MockNetworkScenario.ALT_OPERATOR_4G).toConnectivityStats(
            monitor2gFallback = true,
            passiveSettings = passiveSettings,
            passiveIdleMode = false,
            passiveOnlySession = true
        )
        val flicker = MonitorState.updateStats(limited)
        val recovered = MonitorState.updateStats(
            home.copy(
                ltePci = (home.ltePci ?: 0) + 7,
                lteEarfcn = (home.lteEarfcn ?: 1800) + 25
            )
        )

        assertFalse(flicker.limitedServiceStateChanged)
        assertFalse(recovered.limitedServiceStateChanged)
        assertTrue(recovered.cellIdentityChanged)
        assertFalse(
            recovered.limitedServiceStateAnnouncement.orEmpty().contains("in service")
        )
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
    fun leavingLimitedService_announcesHomeInServiceOn4g() {
        assertEquals(
            "home in service",
            leaveLimitedServiceAnnouncement(
                MockNetworkScenario.HOME_4G,
                MockNetworkScenario.ALT_OPERATOR_4G
            )
        )
    }

    @Test
    fun leavingLimitedService_announcesHomeInServiceOn2g() {
        assertEquals(
            "home in service",
            leaveLimitedServiceAnnouncement(
                MockNetworkScenario.HOME_2G,
                MockNetworkScenario.ALT_OPERATOR_2G
            )
        )
    }

    @Test
    fun homeToRoamingInService_announcesRoamingInServiceOn4g() {
        assertEquals("roaming in service", homeToRoamingAnnouncement(MockNetworkScenario.HOME_4G))
    }

    @Test
    fun homeToRoamingInService_announcesRoamingInServiceOn2g() {
        assertEquals("roaming in service", homeToRoamingAnnouncement(MockNetworkScenario.HOME_2G))
    }

    @Test
    fun cellReselectOntoDifferentPlmn_doesNotSpeakInService() {
        val home = mockStats(MockNetworkScenario.HOME_4G)
        MonitorState.updateStats(home)
        val reselected = home.copy(
            ltePci = (home.ltePci ?: 0) + 11,
            lteEarfcn = (home.lteEarfcn ?: 1800) + 50,
            plmn = PassiveMockSettings.MOCK_VISITED_PLMN,
            servingNetworkOperatorName = PassiveMockSettings.MOCK_VISITED_OPERATOR,
            networkOperatorName = PassiveMockSettings.MOCK_VISITED_OPERATOR
        )

        val first = MonitorState.updateStats(reselected)
        assertTrue(first.cellIdentityChanged)
        assertFalse(speaksInService(first))
        repeat(5) {
            assertFalse(speaksInService(MonitorState.updateStats(reselected)))
        }
    }

    @Test
    fun roamingFlagBlipDuringReselect_doesNotSpeakInService() {
        val home = mockStats(MockNetworkScenario.HOME_4G)
        MonitorState.updateStats(home)
        val blip = home.copy(
            ltePci = (home.ltePci ?: 0) + 3,
            isNetworkRoaming = true,
            plmn = PassiveMockSettings.MOCK_VISITED_PLMN,
            servingNetworkOperatorName = PassiveMockSettings.MOCK_VISITED_OPERATOR,
            networkOperatorName = PassiveMockSettings.MOCK_VISITED_OPERATOR
        )
        repeat(3) {
            assertFalse(speaksInService(MonitorState.updateStats(blip)))
        }
        val back = home.copy(ltePci = blip.ltePci)
        assertFalse(speaksInService(MonitorState.updateStats(back)))
    }

    @Test
    fun mockHome2gThenLive4g_announcesTechnologyChange() {
        val mock = PassiveMockSettings(enabled = true, scenario = MockNetworkScenario.HOME_2G)
        MonitorState.updateStats(
            mock.toConnectivityStats(
                monitor2gFallback = true,
                passiveSettings = passiveSettings,
                passiveIdleMode = false,
                passiveOnlySession = true
            )
        )

        val events = MonitorState.updateStats(live4gStats())

        assertTrue(events.radioTechnologyChanged)
        assertNotNull(events.technologyChangeAnnouncement)
        assertTrue(events.technologyChangeAnnouncement!!.contains("4 G"))
        assertEquals(CellularSignalReader.RADIO_4G, events.technologyChangeTargetRadioAccessType)
    }

    @Test
    fun mockHome2gThenBlankThenLive4g_announcesTechnologyChange() {
        val mock = PassiveMockSettings(enabled = true, scenario = MockNetworkScenario.HOME_2G)
        MonitorState.updateStats(
            mock.toConnectivityStats(
                monitor2gFallback = true,
                passiveSettings = passiveSettings,
                passiveIdleMode = false,
                passiveOnlySession = true
            )
        )

        val blank = ConnectivityStats(
            isMonitoring = true,
            isPassiveOnlySession = true,
            monitor2gFallbackEnabled = true,
            cellularAvailable = false,
            radioAccessType = null,
            isOn2g = false,
            networkOperatorName = PassiveMockSettings.MOCK_HOME_OPERATOR
        )
        MonitorState.updateStats(blank)
        MonitorState.updateStats(blank)

        val events = MonitorState.updateStats(live4gStats())

        assertTrue(events.radioTechnologyChanged)
        assertNotNull(events.technologyChangeAnnouncement)
        assertTrue(events.technologyChangeAnnouncement!!.contains("4 G"))
        assertEquals(CellularSignalReader.RADIO_4G, events.technologyChangeTargetRadioAccessType)
    }

    @Test
    fun endcFlapInGoodSignal_doesNotSpeak4gUntilItStays() {
        MonitorState.setMonitoringSettings(monitor2gFallback.copy(fiveGFeaturesEnabled = true))
        MonitorState.updateStats(live4gStats())

        val endc = live4gStats().copy(radioAccessType = CellularSignalReader.RADIO_5G_ENDC)
        val flap = MonitorState.updateStats(endc)
        val back = MonitorState.updateStats(live4gStats())

        assertFalse(flap.radioTechnologyChanged)
        assertFalse(back.radioTechnologyChanged)

        var stayed = MonitorState.updateStats(endc)
        repeat(2) { stayed = MonitorState.updateStats(endc) }
        assertTrue(stayed.radioTechnologyChanged)
        assertTrue(stayed.technologyChangeAnnouncement!!.contains("5 G"))
    }

    private fun confirmLimitedService(stats: ConnectivityStats): io.github.cloolalang.notspotdetector.model.MonitoringUpdateEvents {
        var events = MonitorState.updateStats(stats)
        repeat(3) { events = MonitorState.updateStats(stats) }
        return events
    }

    private fun mockStats(scenario: MockNetworkScenario): ConnectivityStats {
        return PassiveMockSettings(enabled = true, scenario = scenario).toConnectivityStats(
            monitor2gFallback = true,
            passiveSettings = passiveSettings,
            passiveIdleMode = false,
            passiveOnlySession = true
        )
    }

    private fun leaveLimitedServiceAnnouncement(
        homeScenario: MockNetworkScenario,
        limitedScenario: MockNetworkScenario
    ): String? {
        MonitorState.updateStats(mockStats(homeScenario))
        val enterLimited = confirmLimitedService(mockStats(limitedScenario))
        assertTrue(enterLimited.limitedServiceStateChanged)
        val events = MonitorState.updateStats(mockStats(homeScenario))
        assertTrue(events.limitedServiceStateChanged)
        return events.limitedServiceStateAnnouncement
    }

    private fun homeToRoamingAnnouncement(homeScenario: MockNetworkScenario): String? {
        val home = mockStats(homeScenario)
        MonitorState.updateStats(home)
        val roaming = home.copy(
            isNetworkRoaming = true,
            networkOperatorName = PassiveMockSettings.MOCK_VISITED_OPERATOR,
            servingNetworkOperatorName = PassiveMockSettings.MOCK_VISITED_OPERATOR,
            plmn = PassiveMockSettings.MOCK_VISITED_PLMN
        )
        var events = MonitorState.updateStats(roaming)
        assertFalse(events.limitedServiceStateChanged)
        repeat(2) {
            events = MonitorState.updateStats(roaming)
            assertFalse(events.limitedServiceStateChanged)
        }
        events = MonitorState.updateStats(roaming)
        assertTrue(events.limitedServiceStateChanged)
        return events.limitedServiceStateAnnouncement
    }

    private fun speaksInService(
        events: io.github.cloolalang.notspotdetector.model.MonitoringUpdateEvents
    ): Boolean {
        val text = events.limitedServiceStateAnnouncement.orEmpty()
        return text.contains("in service") || text.contains("in-service")
    }

    private fun live4gStats(): ConnectivityStats {
        return ConnectivityStats(
            isMonitoring = true,
            isPassiveOnlySession = true,
            monitor2gFallbackEnabled = true,
            cellularAvailable = true,
            rsrpDbm = -85,
            rsrqDb = -10,
            radioAccessType = CellularSignalReader.RADIO_4G,
            lteEarfcn = 1800,
            ltePci = 42,
            isOn2g = false,
            hasLteNrSignal = true,
            networkOperatorName = "EE",
            homeNetworkOperatorName = "EE",
            servingNetworkOperatorName = "EE"
        )
    }
}
