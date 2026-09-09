package io.github.cloolalang.notspotdetector.model

import io.github.cloolalang.notspotdetector.network.CellularSignalReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PassiveMockSettingsTest {

    private val passiveSettings = PassiveSignalSettings()

    @Test
    fun normalized_acceptsMockRsrpDownToMinus130() {
        val settings = PassiveMockSettings(rsrpDbm = -130).normalized()
        assertEquals(-130, settings.rsrpDbm)
    }

    @Test
    fun home4gScenario_isInServiceOn4g() {
        val radio = PassiveMockSettings(scenario = MockNetworkScenario.HOME_4G).toRadioMetrics()
        assertEquals(CellularSignalReader.RADIO_4G, radio.radioAccessType)
        assertFalse(radio.isOn2g)
        assertFalse(radio.isLimitedService)
        assertFalse(radio.isCompleteNoService)
    }

    @Test
    fun home2gScenario_isCampedOnHome2g() {
        val radio = PassiveMockSettings(scenario = MockNetworkScenario.HOME_2G).toRadioMetrics()
        assertEquals(CellularSignalReader.RADIO_2G, radio.radioAccessType)
        assertTrue(radio.isOn2g)
        assertTrue(radio.hasHomeGsmSignal)
    }

    @Test
    fun alt4gScenario_isLimitedServiceWithAltOperator() {
        val radio = PassiveMockSettings(scenario = MockNetworkScenario.ALT_OPERATOR_4G).toRadioMetrics()
        assertTrue(radio.isLimitedService)
        assertEquals(PassiveMockSettings.MOCK_VISITED_OPERATOR, radio.servingNetworkOperatorName)
        assertEquals(PassiveMockSettings.MOCK_HOME_OPERATOR, radio.homeNetworkOperatorName)
    }

    @Test
    fun alt2gScenario_isLimitedAlt2gWithoutHomeGsm() {
        val radio = PassiveMockSettings(scenario = MockNetworkScenario.ALT_OPERATOR_2G).toRadioMetrics()
        assertTrue(radio.isLimitedService)
        assertTrue(radio.isOn2g)
        assertFalse(radio.hasHomeGsmSignal)
    }

    @Test
    fun alt2gScenario_mapsToTier13() {
        val stats = PassiveMockSettings(scenario = MockNetworkScenario.ALT_OPERATOR_2G).toConnectivityStats(
            monitor2gFallback = true,
            passiveSettings = passiveSettings,
            passiveIdleMode = false,
            passiveOnlySession = true
        )
        assertEquals(SignalMeasurementTier.LIMITED_ALT_2G, stats.resolveSignalMeasurementTier(passiveSettings))
    }

    @Test
    fun noServiceScenario_isCompleteDeadzone() {
        val stats = PassiveMockSettings(scenario = MockNetworkScenario.NO_SERVICE).toConnectivityStats(
            monitor2gFallback = true,
            passiveSettings = passiveSettings,
            passiveIdleMode = false,
            passiveOnlySession = true
        )
        assertTrue(stats.isCompleteNoService)
        assertEquals(SignalMeasurementTier.DEADZONE, stats.resolveSignalMeasurementTier(passiveSettings))
    }

    @Test
    fun appliesMockSignalStrength_forCampedScenariosOnly() {
        assertTrue(MockNetworkScenario.HOME_4G.appliesMockSignalStrength())
        assertTrue(MockNetworkScenario.HOME_2G.appliesMockSignalStrength())
        assertFalse(MockNetworkScenario.NO_SERVICE.appliesMockSignalStrength())
        assertFalse(MockNetworkScenario.SEARCHING_2G.appliesMockSignalStrength())
    }

    @Test
    fun searching2gScenario_isNotCompleteNoService() {
        val stats = PassiveMockSettings(scenario = MockNetworkScenario.SEARCHING_2G).toConnectivityStats(
            monitor2gFallback = true,
            passiveSettings = passiveSettings,
            passiveIdleMode = false,
            passiveOnlySession = true
        )
        assertFalse(stats.isCompleteNoService)
        assertFalse(stats.cellularAvailable)
    }
}
