package io.github.cloolalang.notspotdetector

import io.github.cloolalang.notspotdetector.model.MockNetworkScenario
import io.github.cloolalang.notspotdetector.model.MonitoringSettings
import io.github.cloolalang.notspotdetector.model.PassiveMockSettings
import io.github.cloolalang.notspotdetector.model.PassiveSignalSettings
import io.github.cloolalang.notspotdetector.model.Rxss
import io.github.cloolalang.notspotdetector.model.RxssStateFilterSettings
import io.github.cloolalang.notspotdetector.model.resolveSignalMeasurementTier
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MonitorStateMockLowSignalFilterTest {

    private val passiveSettings = PassiveSignalSettings(
        lowSignalFilter = RxssStateFilterSettings(
            enabled = true,
            windowSeconds = 1,
            balancePercent = 50
        )
    )

    @Before
    fun setUp() {
        MonitorState.setRunning(false)
        MonitorState.setMonitoringSettings(MonitoringSettings(monitor2gFallback = true))
        MonitorState.setPassiveSignalSettings(passiveSettings)
        MonitorState.beginPassiveOnlySession()
        MonitorState.setRunning(true)
    }

    @After
    fun tearDown() {
        MonitorState.setRunning(false)
    }

    @Test
    fun mockSliderIntoRxss6_doesNotConfirmUntilMeasurementPolls() {
        val tier5Rsrp = passiveSettings.poorRsrpMinDbm + 1
        val tier6Rsrp = passiveSettings.poorRsrpMinDbm - 2

        MonitorState.setPassiveMockSettings(
            PassiveMockSettings(
                enabled = true,
                scenario = MockNetworkScenario.HOME_4G,
                rsrpDbm = tier5Rsrp
            )
        )
        val tier5Stats = MonitorState.stats.value
        MonitorState.updateStats(tier5Stats)
        MonitorState.updateStats(tier5Stats)
        assertEquals(
            Rxss.LEVEL_RANGE_D,
            MonitorState.stats.value.resolveSignalMeasurementTier(passiveSettings).displayNumber
        )

        repeat(4) {
            MonitorState.setPassiveMockSettings(
                PassiveMockSettings(
                    enabled = true,
                    scenario = MockNetworkScenario.HOME_4G,
                    rsrpDbm = tier6Rsrp
                )
            )
        }
        assertEquals(
            "mock slider must not burn through the RXSS 6 wait",
            Rxss.LEVEL_RANGE_D,
            MonitorState.stats.value.resolveSignalMeasurementTier(passiveSettings).displayNumber
        )
        assertEquals(tier6Rsrp, MonitorState.stats.value.rsrpDbm)

        val pending = MonitorState.stats.value
        MonitorState.updateStats(pending)
        assertEquals(
            Rxss.LEVEL_RANGE_D,
            MonitorState.stats.value.resolveSignalMeasurementTier(passiveSettings).displayNumber
        )

        MonitorState.updateStats(MonitorState.stats.value)
        assertEquals(
            Rxss.SIGNAL_LOW,
            MonitorState.stats.value.resolveSignalMeasurementTier(passiveSettings).displayNumber
        )
    }
}
