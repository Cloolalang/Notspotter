package io.github.cloolalang.notspotdetector

import io.github.cloolalang.notspotdetector.model.AudioVolumeSettings
import io.github.cloolalang.notspotdetector.model.MockNetworkScenario
import io.github.cloolalang.notspotdetector.model.MonitoringSettings
import io.github.cloolalang.notspotdetector.model.MonitoringUpdateEvents
import io.github.cloolalang.notspotdetector.model.PassiveMockSettings
import io.github.cloolalang.notspotdetector.model.PassiveSignalSettings
import io.github.cloolalang.notspotdetector.model.VoicePhraseOptions
import io.github.cloolalang.notspotdetector.model.toConnectivityStats
import io.github.cloolalang.notspotdetector.model.Rxss
import io.github.cloolalang.notspotdetector.model.SignalMeasurementTier
import io.github.cloolalang.notspotdetector.model.resolveLimitedServiceSignalOverlayRxss
import io.github.cloolalang.notspotdetector.model.resolveSignalMeasurementTier
import io.github.cloolalang.notspotdetector.model.shouldPlayLimited4gNoSignalCampTier
import io.github.cloolalang.notspotdetector.model.shouldPlayLimitedAlt2gNoSignalCampTier
import io.github.cloolalang.notspotdetector.model.shouldPlayLimitedServiceCampTier
import io.github.cloolalang.notspotdetector.model.shouldPlayLimitedServiceSignalOverlay
import io.github.cloolalang.notspotdetector.model.MonitoringAnnouncementKind
import io.github.cloolalang.notspotdetector.model.shouldPlayLimitedVisitedNoSignalVoiceAnnouncements
import io.github.cloolalang.notspotdetector.model.shouldPlaySignalStrengthInterval
import io.github.cloolalang.notspotdetector.model.shouldPlayTier5StylePeriodicVoice
import io.github.cloolalang.notspotdetector.model.shouldAllowG2WeakPeriodicVoice
import io.github.cloolalang.notspotdetector.model.SignalStateAnnouncement
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MonitorStateMockLimitedServiceOverlayTest {

    private val passiveSettings = PassiveSignalSettings(
        noSignalRsrpDbm = -125,
        poorRsrpMinDbm = -120,
        fairRsrpMinDbm = -105,
        goodRsrpMinDbm = -100,
        mildRsrpMinDbm = -95,
        veryStrongRsrpMinDbm = -80
    )

    @Before
    fun setUp() {
        MonitorState.setRunning(false)
        MonitorState.setMonitoringSettings(MonitoringSettings(monitor2gFallback = true))
        MonitorState.setPassiveSignalSettings(passiveSettings)
        MonitorState.setAudioVolumes(
            AudioVolumeSettings(
                noSignalPhrases = VoicePhraseOptions(
                    speakOperatorName = true,
                    speakTechnology = true,
                    speakBand = false
                ),
                signalLowPhrases = VoicePhraseOptions(
                    speakOperatorName = true,
                    speakTechnology = true,
                    speakBand = false
                )
            )
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
    fun mockVisited4g_rsrpSlider_updatesOverlayThroughMonitorState() {
        pushMockRsrp(MockNetworkScenario.ALT_OPERATOR_4G, -75)
        var stats = MonitorState.stats.value
        assertEquals(-75, stats.rsrpDbm)
        assertEquals(SignalMeasurementTier.LIMITED_SERVICE, stats.resolveSignalMeasurementTier(passiveSettings))
        assertEquals(Rxss.SIGNAL_HIGH, stats.resolveLimitedServiceSignalOverlayRxss(passiveSettings))
        assertTrue(stats.shouldPlayLimitedServiceSignalOverlay(passiveSettings))
        assertFalse(stats.shouldPlayLimitedServiceCampTier(passiveSettings))
        assertFalse(stats.noSignalActive)
        assertTrue(stats.shouldPlaySignalStrengthInterval(passiveSettings))

        pushMockRsrp(MockNetworkScenario.ALT_OPERATOR_4G, -122)
        stats = MonitorState.stats.value
        assertEquals(-122, stats.rsrpDbm)
        assertEquals(Rxss.SIGNAL_LOW, stats.resolveLimitedServiceSignalOverlayRxss(passiveSettings))
        assertTrue(stats.shouldPlayTier5StylePeriodicVoice(passiveSettings))
        assertEquals(
            "E E visited, 4 G, signal low",
            SignalStateAnnouncement.formatTier5SignalLowAnnouncement(stats)
        )

        pushMockRsrp(MockNetworkScenario.ALT_OPERATOR_4G, -130)
        stats = MonitorState.stats.value
        assertEquals(-130, stats.rsrpDbm)
        assertEquals(
            SignalMeasurementTier.LIMITED_4G_NO_SIGNAL,
            stats.resolveSignalMeasurementTier(passiveSettings)
        )
        assertNull(stats.resolveLimitedServiceSignalOverlayRxss(passiveSettings))
        assertFalse(stats.shouldPlayLimitedServiceCampTier(passiveSettings))
        assertTrue(stats.shouldPlayLimited4gNoSignalCampTier(passiveSettings))
        assertTrue(stats.shouldPlayLimitedVisitedNoSignalVoiceAnnouncements(passiveSettings))
        assertEquals(
            "E E visited, 4 G, no signal, limited service",
            MonitorState.formatNoSignalAnnouncement(stats)
        )
    }

    @Test
    fun mockVisited2g_rsrpSlider_updatesOverlayThroughMonitorState() {
        pushMockRsrp(MockNetworkScenario.ALT_OPERATOR_2G, -95)
        var stats = MonitorState.stats.value
        assertEquals(-95, stats.rsrpDbm)
        assertEquals(SignalMeasurementTier.LIMITED_ALT_2G, stats.resolveSignalMeasurementTier(passiveSettings))
        assertEquals(Rxss.G2_GOOD, stats.resolveLimitedServiceSignalOverlayRxss(passiveSettings))
        assertTrue(stats.shouldPlayLimitedServiceSignalOverlay(passiveSettings))

        pushMockRsrp(MockNetworkScenario.ALT_OPERATOR_2G, -110)
        stats = MonitorState.stats.value
        assertEquals(Rxss.G2_WEAK, stats.resolveLimitedServiceSignalOverlayRxss(passiveSettings))
        // Only the tier5-style periodic job (VA-15/VA-18 shared) should handle this — the G2 job's
        // weak-voice branch must stay false here, otherwise the same "signal low" announcement
        // would be scheduled and spoken twice every 30s.
        assertTrue(stats.shouldPlayTier5StylePeriodicVoice(passiveSettings))
        assertFalse(stats.shouldAllowG2WeakPeriodicVoice(passiveSettings))
        assertEquals(
            "E E visited, 2 G, signal low",
            SignalStateAnnouncement.formatTier5SignalLowAnnouncement(stats)
        )

        pushMockRsrp(MockNetworkScenario.ALT_OPERATOR_2G, -130)
        stats = MonitorState.stats.value
        assertEquals(
            SignalMeasurementTier.LIMITED_ALT_2G_NO_SIGNAL,
            stats.resolveSignalMeasurementTier(passiveSettings)
        )
        assertTrue(stats.shouldPlayLimitedAlt2gNoSignalCampTier(passiveSettings))
        assertTrue(stats.shouldPlayLimitedVisitedNoSignalVoiceAnnouncements(passiveSettings))
        assertEquals(
            "E E visited, 2 G, no signal, limited service",
            MonitorState.formatNoSignalAnnouncement(stats)
        )
    }

    @Test
    fun mockVisited4gNoSignalToStrong_announcesSignalRestored() {
        val events = transitionMockRsrp(
            scenario = MockNetworkScenario.ALT_OPERATOR_4G,
            fromRsrp = -130,
            toRsrp = -75
        )

        assertTrue(events.noSignalStateChanged)
        assertEquals(
            "E E visited, 4 G, signal restored",
            events.noSignalStateAnnouncement
        )
        assertNull(events.tier5Announcement)
    }

    @Test
    fun mockVisited4gNoSignalToWeak_announcesSignalLowNotRestored() {
        val events = transitionMockRsrp(
            scenario = MockNetworkScenario.ALT_OPERATOR_4G,
            fromRsrp = -130,
            toRsrp = -122
        )

        assertNull(events.noSignalStateAnnouncement)
        assertTrue(events.tier5Announced)
        assertTrue(events.tier5Immediate)
        assertEquals(
            "E E visited, 4 G, signal low",
            events.tier5Announcement
        )
        assertTrue(events.immediateAnnouncements().any { it.kind == MonitoringAnnouncementKind.TIER5 })
    }

    @Test
    fun mockVisited2gNoSignalToStrong_announcesSignalRestored() {
        val events = transitionMockRsrp(
            scenario = MockNetworkScenario.ALT_OPERATOR_2G,
            fromRsrp = -130,
            toRsrp = -95
        )

        assertTrue(events.noSignalStateChanged)
        assertEquals(
            "E E visited, 2 G, signal restored",
            events.noSignalStateAnnouncement
        )
    }

    @Test
    fun mockVisited2gNoSignalToWeak_announcesSignalLowNotRestored() {
        val events = transitionMockRsrp(
            scenario = MockNetworkScenario.ALT_OPERATOR_2G,
            fromRsrp = -130,
            toRsrp = -110
        )

        assertNull(events.noSignalStateAnnouncement)
        assertTrue(events.tier5Announced)
        assertEquals(
            "E E visited, 2 G, signal low",
            events.tier5Announcement
        )
    }

    @Test
    fun home4gNoSignalThenVisited4gStrong_restoresRsrpForOverlay() {
        pushMockRsrp(MockNetworkScenario.HOME_4G, -130)
        assertTrue(MonitorState.stats.value.noSignalActive)

        pushMockRsrp(MockNetworkScenario.ALT_OPERATOR_4G, -75)
        val stats = MonitorState.stats.value
        assertEquals(-75, stats.rsrpDbm)
        assertEquals(Rxss.SIGNAL_HIGH, stats.resolveLimitedServiceSignalOverlayRxss(passiveSettings))
    }

    @Test
    fun setPassiveMockSettings_pushesMockStatsImmediately() {
        MonitorState.setPassiveMockSettings(
            PassiveMockSettings(
                enabled = true,
                scenario = MockNetworkScenario.ALT_OPERATOR_4G,
                rsrpDbm = -122
            )
        )
        val stats = MonitorState.stats.value
        assertEquals(-122, stats.rsrpDbm)
        assertEquals(Rxss.SIGNAL_LOW, stats.resolveLimitedServiceSignalOverlayRxss(passiveSettings))
    }

    private fun pushMockRsrp(scenario: MockNetworkScenario, rsrpDbm: Int) {
        MonitorState.setPassiveMockSettings(
            PassiveMockSettings(
                enabled = true,
                scenario = scenario,
                rsrpDbm = rsrpDbm
            )
        )
    }

    private fun transitionMockRsrp(
        scenario: MockNetworkScenario,
        fromRsrp: Int,
        toRsrp: Int
    ): MonitoringUpdateEvents {
        val noSignalStats = PassiveMockSettings(
            enabled = true,
            scenario = scenario,
            rsrpDbm = fromRsrp
        ).toConnectivityStats(
            monitor2gFallback = true,
            passiveSettings = passiveSettings,
            passiveIdleMode = false,
            passiveOnlySession = true
        )
        MonitorState.updateStats(noSignalStats)
        MonitorState.updateStats(noSignalStats)

        val recoveredStats = PassiveMockSettings(
            enabled = true,
            scenario = scenario,
            rsrpDbm = toRsrp
        ).toConnectivityStats(
            monitor2gFallback = true,
            passiveSettings = passiveSettings,
            passiveIdleMode = false,
            passiveOnlySession = true
        )
        return MonitorState.updateStats(recoveredStats)
    }
}
