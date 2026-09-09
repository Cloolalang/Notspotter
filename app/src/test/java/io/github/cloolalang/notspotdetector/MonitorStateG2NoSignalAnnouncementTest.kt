package io.github.cloolalang.notspotdetector

import io.github.cloolalang.notspotdetector.model.MockNetworkScenario
import io.github.cloolalang.notspotdetector.model.MonitoringSettings
import io.github.cloolalang.notspotdetector.model.PassiveMockSettings
import io.github.cloolalang.notspotdetector.model.PassiveSignalSettings
import io.github.cloolalang.notspotdetector.model.toConnectivityStats
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MonitorStateG2NoSignalAnnouncementTest {

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
    fun enteringNoSignalOnHome2g_defersImmediateVoiceToPeriodicTimer() {
        val mock = PassiveMockSettings(
            enabled = true,
            scenario = MockNetworkScenario.HOME_2G,
            rsrpDbm = -95
        )
        val home2gWithSignal = mock.toConnectivityStats(
            monitor2gFallback = true,
            passiveSettings = passiveSettings,
            passiveIdleMode = false,
            passiveOnlySession = true
        )
        MonitorState.updateStats(home2gWithSignal)
        MonitorState.updateStats(home2gWithSignal)

        val noSignalRsrp = PassiveSignalSettings.MIN_RSRP_DBM
        val home2gNoSignal = mock.copy(rsrpDbm = noSignalRsrp).toConnectivityStats(
            monitor2gFallback = true,
            passiveSettings = passiveSettings,
            passiveIdleMode = false,
            passiveOnlySession = true
        )
        MonitorState.updateStats(home2gNoSignal)
        val events = MonitorState.updateStats(home2gNoSignal)

        assertNull(events.noSignalStateAnnouncement)
        assertFalse(events.noSignalStateChanged)
        assertTrue(events.g2PeriodicReset)
    }

    @Test
    fun home2gStrongToWeak_resetsG2PeriodicForSignalLowVoice() {
        val mock = PassiveMockSettings(
            enabled = true,
            scenario = MockNetworkScenario.HOME_2G,
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
        MonitorState.updateStats(
            mock.toConnectivityStats(
                monitor2gFallback = true,
                passiveSettings = passiveSettings,
                passiveIdleMode = false,
                passiveOnlySession = true
            )
        )

        val events = MonitorState.updateStats(
            mock.copy(rsrpDbm = -110).toConnectivityStats(
                monitor2gFallback = true,
                passiveSettings = passiveSettings,
                passiveIdleMode = false,
                passiveOnlySession = true
            )
        )

        assertTrue(events.g2PeriodicReset)
    }

    @Test
    fun home4gToHome2gNoSignal_playsTechnologyFirstAndResetsPeriodicForNoSignal() {
        val mock = PassiveMockSettings(
            enabled = true,
            scenario = MockNetworkScenario.HOME_4G,
            rsrpDbm = PassiveSignalSettings.MIN_RSRP_DBM
        )
        MonitorState.updateStats(
            mock.toConnectivityStats(
                monitor2gFallback = true,
                passiveSettings = passiveSettings,
                passiveIdleMode = false,
                passiveOnlySession = true
            )
        )
        MonitorState.updateStats(
            mock.toConnectivityStats(
                monitor2gFallback = true,
                passiveSettings = passiveSettings,
                passiveIdleMode = false,
                passiveOnlySession = true
            )
        )

        val home2gNoSignal = mock.copy(scenario = MockNetworkScenario.HOME_2G).toConnectivityStats(
            monitor2gFallback = true,
            passiveSettings = passiveSettings,
            passiveIdleMode = false,
            passiveOnlySession = true
        )
        val transitionEvents = MonitorState.updateStats(home2gNoSignal)
        val debouncedEvents = MonitorState.updateStats(home2gNoSignal)

        assertFalse(transitionEvents.radioTechnologyChanged)
        assertTrue(transitionEvents.g2FallbackAnnounced)
        assertTrue(transitionEvents.g2PeriodicReset)
        assertNull(debouncedEvents.noSignalStateAnnouncement)
        assertFalse(debouncedEvents.noSignalStateChanged)
    }
}
