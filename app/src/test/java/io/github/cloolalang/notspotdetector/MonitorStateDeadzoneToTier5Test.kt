package io.github.cloolalang.notspotdetector

import io.github.cloolalang.notspotdetector.model.MockNetworkScenario
import io.github.cloolalang.notspotdetector.model.MonitoringAnnouncementKind
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

class MonitorStateDeadzoneToTier5Test {

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
    fun home4gToDeadzone_announcesDeadzoneOnlyNotNoSignal() {
        val home4g = PassiveMockSettings(
            enabled = true,
            scenario = MockNetworkScenario.HOME_4G,
            rsrpDbm = -95
        ).toConnectivityStats(
            monitor2gFallback = true,
            passiveSettings = passiveSettings,
            passiveIdleMode = false,
            passiveOnlySession = true
        )
        MonitorState.updateStats(home4g)
        MonitorState.updateStats(home4g)

        val deadzone = PassiveMockSettings(
            enabled = true,
            scenario = MockNetworkScenario.NO_SERVICE
        ).toConnectivityStats(
            monitor2gFallback = true,
            passiveSettings = passiveSettings,
            passiveIdleMode = false,
            passiveOnlySession = true
        )
        val firstDeadzonePoll = MonitorState.updateStats(deadzone)
        val secondDeadzonePoll = MonitorState.updateStats(deadzone)

        listOf(firstDeadzonePoll, secondDeadzonePoll).forEach { events ->
            assertNull(events.noSignalStateAnnouncement)
            assertFalse(events.noSignalStateChanged)
        }
        assertTrue(
            firstDeadzonePoll.deadzoneAnnounced ||
                secondDeadzonePoll.deadzoneAnnounced
        )
        assertTrue(
            (firstDeadzonePoll.deadzoneAnnouncement ?: secondDeadzonePoll.deadzoneAnnouncement)!!
                .contains("dead zone")
        )
    }

    @Test
    fun deadzoneEntry_suppressesDebouncedNoSignalVoiceAfterBaseline() {
        val deadzone = PassiveMockSettings(
            enabled = true,
            scenario = MockNetworkScenario.NO_SERVICE
        ).toConnectivityStats(
            monitor2gFallback = true,
            passiveSettings = passiveSettings,
            passiveIdleMode = false,
            passiveOnlySession = true
        )
        val firstPoll = MonitorState.updateStats(deadzone)
        val secondPoll = MonitorState.updateStats(deadzone)

        assertTrue(MonitorState.stats.value.isCompleteNoService)
        assertTrue(MonitorState.stats.value.noSignalActive)
        assertNull(secondPoll.noSignalStateAnnouncement)
        assertFalse(secondPoll.noSignalStateChanged)
        assertTrue(firstPoll.deadzoneAnnounced || secondPoll.deadzoneAnnounced)
    }

    @Test
    fun deadzoneToTier5_skipsSignalRestoredAndAnnouncesSignalLowImmediately() {
        val noService = PassiveMockSettings(
            enabled = true,
            scenario = MockNetworkScenario.NO_SERVICE
        )
        val deadzoneStats = noService.toConnectivityStats(
            monitor2gFallback = true,
            passiveSettings = passiveSettings,
            passiveIdleMode = false,
            passiveOnlySession = true
        )
        MonitorState.updateStats(deadzoneStats)
        MonitorState.updateStats(deadzoneStats)

        val tier5Rsrp = passiveSettings.poorRsrpMinDbm + 1
        val recoveredTier5 = noService.copy(
            scenario = MockNetworkScenario.HOME_4G,
            rsrpDbm = tier5Rsrp
        ).toConnectivityStats(
            monitor2gFallback = true,
            passiveSettings = passiveSettings,
            passiveIdleMode = false,
            passiveOnlySession = true
        )
        val events = MonitorState.updateStats(recoveredTier5)

        assertNull(events.noSignalStateAnnouncement)
        assertFalse(events.noSignalStateChanged)
        assertTrue(events.tier5Announced)
        assertTrue(events.tier5Immediate)
        assertTrue(events.tier5Announcement!!.contains("signal low"))
        assertFalse(events.tier5Announcement!!.contains("signal restored"))

        val order = events.immediateAnnouncements().map { it.kind }
        assertTrue(order.contains(MonitoringAnnouncementKind.TIER5))
        assertFalse(order.contains(MonitoringAnnouncementKind.NO_SIGNAL_STATE))
    }

    @Test
    fun deadzoneToFairSignal_stillAnnouncesSignalRestored() {
        val noService = PassiveMockSettings(
            enabled = true,
            scenario = MockNetworkScenario.NO_SERVICE
        )
        MonitorState.updateStats(
            noService.toConnectivityStats(
                monitor2gFallback = true,
                passiveSettings = passiveSettings,
                passiveIdleMode = false,
                passiveOnlySession = true
            )
        )
        MonitorState.updateStats(
            noService.toConnectivityStats(
                monitor2gFallback = true,
                passiveSettings = passiveSettings,
                passiveIdleMode = false,
                passiveOnlySession = true
            )
        )

        val recoveredFair = noService.copy(
            scenario = MockNetworkScenario.HOME_4G,
            rsrpDbm = -95
        ).toConnectivityStats(
            monitor2gFallback = true,
            passiveSettings = passiveSettings,
            passiveIdleMode = false,
            passiveOnlySession = true
        )
        val events = MonitorState.updateStats(recoveredFair)

        assertTrue(events.noSignalStateChanged)
        assertTrue(events.noSignalStateAnnouncement!!.contains("signal restored"))
        assertFalse(events.tier5Immediate)

        val followUp = MonitorState.updateStats(recoveredFair)
        assertNull(followUp.noSignalStateAnnouncement)
    }
}
