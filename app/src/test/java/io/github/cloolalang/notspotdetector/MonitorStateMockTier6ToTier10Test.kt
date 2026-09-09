package io.github.cloolalang.notspotdetector

import io.github.cloolalang.notspotdetector.model.MockNetworkScenario
import io.github.cloolalang.notspotdetector.model.MonitoringAnnouncementKind
import io.github.cloolalang.notspotdetector.model.MonitoringSettings
import io.github.cloolalang.notspotdetector.model.NO_SIGNAL_TIER_NUMBER
import io.github.cloolalang.notspotdetector.model.PassiveMockSettings
import io.github.cloolalang.notspotdetector.model.PassiveSignalSettings
import io.github.cloolalang.notspotdetector.model.SEARCHING_2G_TIER_NUMBER
import io.github.cloolalang.notspotdetector.model.resolveSignalMeasurementTier
import io.github.cloolalang.notspotdetector.model.toConnectivityStats
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MonitorStateMockTier6ToTier10Test {

    private val passiveSettings = PassiveSignalSettings()
    private val monitor2gFallback = MonitoringSettings(monitor2gFallback = true)

    @Before
    fun setUp() {
        MonitorState.setRunning(false)
        MonitorState.setMonitoringSettings(monitor2gFallback)
        MonitorState.setPassiveSignalSettings(passiveSettings)
        MonitorState.beginPassiveOnlySession()
        MonitorState.setRunning(true)
        MonitorState.setPassiveMockSettings(PassiveMockSettings(enabled = true, scenario = MockNetworkScenario.HOME_4G))
    }

    @After
    fun tearDown() {
        MonitorState.setRunning(false)
    }

    @Test
    fun mockHome4gCriticalToNoSignal_announcesNoSignalAndStaysOnTier10() {
        val mock = PassiveMockSettings(
            enabled = true,
            scenario = MockNetworkScenario.HOME_4G,
            rsrpDbm = passiveSettings.poorRsrpMinDbm - 2
        )
        val criticalStats = mock.toConnectivityStats(
            monitor2gFallback = true,
            passiveSettings = passiveSettings,
            passiveIdleMode = false,
            passiveOnlySession = true
        )
        MonitorState.updateStats(criticalStats)
        MonitorState.updateStats(criticalStats)

        val noSignalStats = mock.copy(rsrpDbm = PassiveSignalSettings.MIN_RSRP_DBM).toConnectivityStats(
            monitor2gFallback = true,
            passiveSettings = passiveSettings,
            passiveIdleMode = false,
            passiveOnlySession = true
        )
        MonitorState.updateStats(noSignalStats)
        val events = MonitorState.updateStats(noSignalStats)

        assertEquals(
            NO_SIGNAL_TIER_NUMBER,
            MonitorState.stats.value.resolveSignalMeasurementTier(passiveSettings).displayNumber
        )
        assertFalse(MonitorState.stats.value.searching2gFallbackActive)
        assertTrue(events.noSignalStateChanged)
        assertTrue(events.noSignalStateAnnouncement!!.contains("no signal"))
    }

    @Test
    fun mockHome4gNoSignal_doesNotEnterSearching2gWhen2gFallbackEnabled() {
        val mock = PassiveMockSettings(
            enabled = true,
            scenario = MockNetworkScenario.HOME_4G,
            rsrpDbm = PassiveSignalSettings.MIN_RSRP_DBM
        )
        val noSignalStats = mock.toConnectivityStats(
            monitor2gFallback = true,
            passiveSettings = passiveSettings,
            passiveIdleMode = false,
            passiveOnlySession = true
        )
        MonitorState.updateStats(noSignalStats)
        MonitorState.updateStats(noSignalStats)

        val tier = MonitorState.stats.value.resolveSignalMeasurementTier(passiveSettings)
        assertEquals(NO_SIGNAL_TIER_NUMBER, tier.displayNumber)
        assertFalse(MonitorState.stats.value.searching2gFallbackActive)
        assertFalse(tier.displayNumber == SEARCHING_2G_TIER_NUMBER)
    }

    @Test
    fun mockHome4gTier10ToTier6_announcesSignalLowNotRestored() {
        val mock = PassiveMockSettings(
            enabled = true,
            scenario = MockNetworkScenario.HOME_4G,
            rsrpDbm = PassiveSignalSettings.MIN_RSRP_DBM
        )
        val noSignalStats = mock.toConnectivityStats(
            monitor2gFallback = true,
            passiveSettings = passiveSettings,
            passiveIdleMode = false,
            passiveOnlySession = true
        )
        MonitorState.updateStats(noSignalStats)
        MonitorState.updateStats(noSignalStats)

        val criticalRsrp = passiveSettings.poorRsrpMinDbm - 2
        val criticalStats = mock.copy(rsrpDbm = criticalRsrp).toConnectivityStats(
            monitor2gFallback = true,
            passiveSettings = passiveSettings,
            passiveIdleMode = false,
            passiveOnlySession = true
        )
        MonitorState.updateStats(criticalStats)
        val events = MonitorState.updateStats(criticalStats)

        assertNull(events.noSignalStateAnnouncement)
        assertFalse(events.noSignalStateChanged)
        assertTrue(events.tier5Announced)
        assertTrue(events.tier5Immediate)
        assertTrue(events.tier5Announcement!!.contains("signal low"))
        assertFalse(events.tier5Announcement!!.contains("signal restored"))
        assertTrue(
            events.immediateAnnouncements().any { it.kind == MonitoringAnnouncementKind.TIER5 }
        )
    }

    @Test
    fun mockHome4gTier10ToTier5_announcesSignalRestoredNotSignalLow() {
        val mock = PassiveMockSettings(
            enabled = true,
            scenario = MockNetworkScenario.HOME_4G,
            rsrpDbm = PassiveSignalSettings.MIN_RSRP_DBM
        )
        val noSignalStats = mock.toConnectivityStats(
            monitor2gFallback = true,
            passiveSettings = passiveSettings,
            passiveIdleMode = false,
            passiveOnlySession = true
        )
        MonitorState.updateStats(noSignalStats)
        MonitorState.updateStats(noSignalStats)

        val tier5Rsrp = passiveSettings.poorRsrpMinDbm + 1
        val tier5Stats = mock.copy(rsrpDbm = tier5Rsrp).toConnectivityStats(
            monitor2gFallback = true,
            passiveSettings = passiveSettings,
            passiveIdleMode = false,
            passiveOnlySession = true
        )
        MonitorState.updateStats(tier5Stats)
        val events = MonitorState.updateStats(tier5Stats)

        assertTrue(events.noSignalStateChanged)
        assertTrue(events.noSignalStateAnnouncement!!.contains("signal restored"))
        assertNull(events.tier5Announcement)
        assertFalse(events.tier5Immediate)
    }
}
