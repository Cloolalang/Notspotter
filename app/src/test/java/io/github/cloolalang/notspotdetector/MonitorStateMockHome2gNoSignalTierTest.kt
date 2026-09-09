package io.github.cloolalang.notspotdetector

import io.github.cloolalang.notspotdetector.model.G2_NO_SIGNAL_TIER_NUMBER
import io.github.cloolalang.notspotdetector.model.MockNetworkScenario
import io.github.cloolalang.notspotdetector.model.MonitoringSettings
import io.github.cloolalang.notspotdetector.model.NO_SIGNAL_TIER_NUMBER
import io.github.cloolalang.notspotdetector.model.PassiveMockSettings
import io.github.cloolalang.notspotdetector.model.PassiveSignalSettings
import io.github.cloolalang.notspotdetector.model.resolveSignalMeasurementTier
import io.github.cloolalang.notspotdetector.model.toConnectivityStats
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MonitorStateMockHome2gNoSignalTierTest {

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
    fun mockHome2gNoSignal_resolvesToTier15NotTier10() {
        val mock = PassiveMockSettings(
            enabled = true,
            scenario = MockNetworkScenario.HOME_2G,
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

        assertEquals(
            G2_NO_SIGNAL_TIER_NUMBER,
            MonitorState.stats.value.resolveSignalMeasurementTier(passiveSettings).displayNumber
        )
    }

    @Test
    fun mockHome4gNoSignal_stillResolvesToTier10() {
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

        assertEquals(
            NO_SIGNAL_TIER_NUMBER,
            MonitorState.stats.value.resolveSignalMeasurementTier(passiveSettings).displayNumber
        )
    }
}
