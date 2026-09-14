package io.github.cloolalang.notspotdetector.model

import io.github.cloolalang.notspotdetector.network.CellularSignalReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
    fun normalized_acceptsMockRsrpDownToMinus140() {
        val settings = PassiveMockSettings(rsrpDbm = -140).normalized()
        assertEquals(-140, settings.rsrpDbm)
        assertEquals(-140, PassiveMockSettings(rsrpDbm = -150).normalized().rsrpDbm)
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
    fun home5gEndcScenario_isInServiceOnEndcWithNrSecondary() {
        val radio = PassiveMockSettings(scenario = MockNetworkScenario.HOME_5G_ENDC).toRadioMetrics()
        assertEquals(CellularSignalReader.RADIO_5G_ENDC, radio.radioAccessType)
        assertFalse(radio.isOn2g)
        assertFalse(radio.isLimitedService)
        assertFalse(radio.isCompleteNoService)
        assertTrue(radio.hasLteNrSignal)
        assertEquals(PassiveMockSettings.MOCK_LTE_EARFCN, radio.lteEarfcn)
        assertEquals(PassiveMockSettings.MOCK_NR_EARFCN, radio.nrEarfcn)
        assertEquals(PassiveMockSettings.MOCK_NR_PCI, radio.nrPci)
        assertEquals(PassiveMockSettings.MOCK_HOME_OPERATOR, radio.homeNetworkOperatorName)
    }

    @Test
    fun home5gEndcScenario_usesLteNrSignalStrengthAndMapsToRsrpTiers() {
        assertTrue(MockNetworkScenario.HOME_5G_ENDC.usesLteNrSignalStrength())
        assertTrue(MockNetworkScenario.HOME_5G_ENDC.appliesMockSignalStrength())
        val stats = PassiveMockSettings(scenario = MockNetworkScenario.HOME_5G_ENDC, rsrpDbm = -80).toConnectivityStats(
            monitor2gFallback = true,
            passiveSettings = passiveSettings,
            passiveIdleMode = false,
            passiveOnlySession = true
        )
        assertTrue(stats.cellularAvailable)
        assertEquals(CellularSignalReader.RADIO_5G_ENDC, stats.radioAccessType)
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
    fun home5gSaScenario_isInServiceOnStandaloneNr() {
        val radio = PassiveMockSettings(scenario = MockNetworkScenario.HOME_5G).toRadioMetrics()
        assertEquals(CellularSignalReader.RADIO_5G, radio.radioAccessType)
        assertNull(radio.lteEarfcn)
        assertEquals(PassiveMockSettings.MOCK_NR_EARFCN, radio.nrEarfcn)
        assertFalse(radio.isNetworkRoaming)
        assertTrue(MockNetworkScenario.HOME_5G.isFiveGScenario())
        assertTrue(MockNetworkScenario.HOME_5G.usesLteNrSignalStrength())
    }

    @Test
    fun roaming4gScenario_isRegisteredRoamingInService() {
        val stats = PassiveMockSettings(scenario = MockNetworkScenario.ROAMING_4G).toConnectivityStats(
            monitor2gFallback = true,
            passiveSettings = passiveSettings,
            passiveIdleMode = false,
            passiveOnlySession = true
        )
        assertFalse(stats.isLimitedService)
        assertTrue(stats.isNetworkRoaming)
        assertEquals(NetworkServiceMode.IN_SERVICE, stats.networkServiceMode)
        assertEquals(PassiveMockSettings.MOCK_VISITED_OPERATOR, stats.servingNetworkOperatorName)
        assertEquals(PassiveMockSettings.MOCK_HOME_OPERATOR, stats.homeNetworkOperatorName)
        assertEquals(ServiceStateMetricLabel.IN_SERVICE_ROAMING, stats.copy(signalPermissionGranted = true).resolveServiceStateMetricLabel())
        assertEquals("roaming in service", SignalStateAnnouncement.formatInServiceAnnouncement(
            stats,
            phrases = VoicePhraseOptions(speakOperatorName = false, speakTechnology = false)
        ))
    }

    @Test
    fun roaming2gScenario_isRegisteredRoamingInService() {
        val radio = PassiveMockSettings(scenario = MockNetworkScenario.ROAMING_2G).toRadioMetrics()
        assertTrue(radio.isOn2g)
        assertTrue(radio.isNetworkRoaming)
        assertFalse(radio.isLimitedService)
        assertEquals(CellularSignalReader.RADIO_2G, radio.radioAccessType)
        assertTrue(MockNetworkScenario.ROAMING_2G.usesG2SignalStrength())
    }

    @Test
    fun roaming5gScenarios_areRegisteredRoaming() {
        val sa = PassiveMockSettings(scenario = MockNetworkScenario.ROAMING_5G).toRadioMetrics()
        assertEquals(CellularSignalReader.RADIO_5G, sa.radioAccessType)
        assertTrue(sa.isNetworkRoaming)
        assertNull(sa.lteEarfcn)
        val endc = PassiveMockSettings(scenario = MockNetworkScenario.ROAMING_5G_ENDC).toRadioMetrics()
        assertEquals(CellularSignalReader.RADIO_5G_ENDC, endc.radioAccessType)
        assertTrue(endc.isNetworkRoaming)
        assertEquals(PassiveMockSettings.MOCK_ALT_LTE_EARFCN, endc.lteEarfcn)
    }

    @Test
    fun voiceOnlyNoData_appliesToCampedScenariosOnly() {
        val home = PassiveMockSettings(
            scenario = MockNetworkScenario.HOME_4G,
            voiceOnlyNoData = true
        ).toConnectivityStats(
            monitor2gFallback = true,
            passiveSettings = passiveSettings,
            passiveIdleMode = false,
            passiveOnlySession = true
        )
        assertTrue(home.isVoiceOnlyNoData)
        assertEquals(
            ServiceStateMetricLabel.IN_SERVICE_VOICE_ONLY,
            home.copy(signalPermissionGranted = true).resolveServiceStateMetricLabel()
        )
        val roaming = PassiveMockSettings(
            scenario = MockNetworkScenario.ROAMING_4G,
            voiceOnlyNoData = true
        ).toConnectivityStats(
            monitor2gFallback = true,
            passiveSettings = passiveSettings,
            passiveIdleMode = false,
            passiveOnlySession = true
        )
        assertEquals(
            ServiceStateMetricLabel.IN_SERVICE_VOICE_ONLY_ROAMING,
            roaming.copy(signalPermissionGranted = true).resolveServiceStateMetricLabel()
        )
        val deadzone = PassiveMockSettings(
            scenario = MockNetworkScenario.NO_SERVICE,
            voiceOnlyNoData = true
        ).toRadioMetrics()
        assertFalse(deadzone.isVoiceOnlyNoData)
        assertFalse(MockNetworkScenario.NO_SERVICE.supportsVoiceOnlyNoData())
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
