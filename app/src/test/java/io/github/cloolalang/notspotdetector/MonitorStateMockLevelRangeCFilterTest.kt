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

class MonitorStateMockLevelRangeCFilterTest {

    private val passiveSettings = PassiveSignalSettings(
        levelRangeCFilter = RxssStateFilterSettings(
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
    fun mockSliderIntoRxss4_doesNotConfirmUntilMeasurementPolls() {
        val tier3Rsrp = passiveSettings.goodRsrpMinDbm + 1
        val tier4Rsrp = passiveSettings.fairRsrpMinDbm + 5

        MonitorState.setPassiveMockSettings(
            PassiveMockSettings(
                enabled = true,
                scenario = MockNetworkScenario.HOME_4G,
                rsrpDbm = tier3Rsrp
            )
        )
        val tier3Stats = MonitorState.stats.value
        MonitorState.updateStats(tier3Stats)
        MonitorState.updateStats(tier3Stats)
        assertEquals(
            Rxss.LEVEL_RANGE_B,
            MonitorState.stats.value.resolveSignalMeasurementTier(passiveSettings).displayNumber
        )

        repeat(4) {
            MonitorState.setPassiveMockSettings(
                PassiveMockSettings(
                    enabled = true,
                    scenario = MockNetworkScenario.HOME_4G,
                    rsrpDbm = tier4Rsrp
                )
            )
        }
        assertEquals(
            "mock slider must not burn through the RXSS 4 wait",
            Rxss.LEVEL_RANGE_B,
            MonitorState.stats.value.resolveSignalMeasurementTier(passiveSettings).displayNumber
        )
        assertEquals(tier4Rsrp, MonitorState.stats.value.rsrpDbm)

        val pending = MonitorState.stats.value
        MonitorState.updateStats(pending)
        assertEquals(
            Rxss.LEVEL_RANGE_B,
            MonitorState.stats.value.resolveSignalMeasurementTier(passiveSettings).displayNumber
        )

        MonitorState.updateStats(MonitorState.stats.value)
        assertEquals(
            Rxss.LEVEL_RANGE_C,
            MonitorState.stats.value.resolveSignalMeasurementTier(passiveSettings).displayNumber
        )
    }
}
