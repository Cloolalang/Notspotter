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

class MonitorStateMockLevelRangeDFilterTest {

    private val passiveSettings = PassiveSignalSettings(
        levelRangeDFilter = RxssStateFilterSettings(
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
    fun mockSliderIntoRxss5_doesNotConfirmUntilMeasurementPolls() {
        val tier4Rsrp = passiveSettings.fairRsrpMinDbm + 5
        val tier5Rsrp = passiveSettings.fairRsrpMinDbm - 2

        MonitorState.setPassiveMockSettings(
            PassiveMockSettings(
                enabled = true,
                scenario = MockNetworkScenario.HOME_4G,
                rsrpDbm = tier4Rsrp
            )
        )
        val tier4Stats = MonitorState.stats.value
        MonitorState.updateStats(tier4Stats)
        MonitorState.updateStats(tier4Stats)
        assertEquals(
            Rxss.LEVEL_RANGE_C,
            MonitorState.stats.value.resolveSignalMeasurementTier(passiveSettings).displayNumber
        )

        repeat(4) {
            MonitorState.setPassiveMockSettings(
                PassiveMockSettings(
                    enabled = true,
                    scenario = MockNetworkScenario.HOME_4G,
                    rsrpDbm = tier5Rsrp
                )
            )
        }
        assertEquals(
            "mock slider must not burn through the RXSS 5 wait",
            Rxss.LEVEL_RANGE_C,
            MonitorState.stats.value.resolveSignalMeasurementTier(passiveSettings).displayNumber
        )
        assertEquals(tier5Rsrp, MonitorState.stats.value.rsrpDbm)

        val pending = MonitorState.stats.value
        MonitorState.updateStats(pending)
        assertEquals(
            Rxss.LEVEL_RANGE_C,
            MonitorState.stats.value.resolveSignalMeasurementTier(passiveSettings).displayNumber
        )

        MonitorState.updateStats(MonitorState.stats.value)
        assertEquals(
            Rxss.LEVEL_RANGE_D,
            MonitorState.stats.value.resolveSignalMeasurementTier(passiveSettings).displayNumber
        )
    }
}
