package io.github.cloolalang.notspotdetector.model

import io.github.cloolalang.notspotdetector.network.CellularSignalReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SignalStateAnnouncementTest {

    @Test
    fun formatTier5SignalLow_includesOperatorTechnologyAndPhrase() {
        assertEquals(
            "E E, 4 G, signal low",
            SignalStateAnnouncement.formatTier5SignalLowAnnouncement("EE", CellularSignalReader.RADIO_4G)
        )
        assertEquals(
            "4 G, signal low",
            SignalStateAnnouncement.formatTier5SignalLowAnnouncement(null, CellularSignalReader.RADIO_4G)
        )
    }

    @Test
    fun formatTechnologyChange_includesOperatorAndSpokenRat() {
        val announcement = SignalStateAnnouncement.formatTechnologyChange(
            CellularSignalReader.RADIO_4G,
            "EE"
        )
        assertEquals("E E, 4 G", announcement)
    }

    @Test
    fun formatNoSignalChange_wifiCallingUsesDedicatedPhraseWithoutTech() {
        assertEquals(
            "E E, wifi calling, no cellular signal",
            SignalStateAnnouncement.formatNoSignalChange(
                active = true,
                networkOperatorName = "EE",
                radioAccessType = CellularSignalReader.RADIO_4G,
                isWifiCallingActive = true
            )
        )
        assertEquals(
            "E E, cellular signal restored",
            SignalStateAnnouncement.formatNoSignalChange(
                active = false,
                networkOperatorName = "EE",
                radioAccessType = CellularSignalReader.RADIO_4G,
                isWifiCallingActive = true
            )
        )
    }

    @Test
    fun formatNoSignalAnnouncement_stats_wifiCallingUsesDedicatedPhrase() {
        val stats = ConnectivityStats(
            isMonitoring = true,
            networkOperatorName = "Vodafone",
            isWifiCallingActive = true,
            noSignalActive = true
        )
        assertEquals(
            "Vodafone, wifi calling, no cellular signal",
            SignalStateAnnouncement.formatNoSignalAnnouncement(stats)
        )
        assertEquals(
            "Vodafone, cellular signal restored",
            SignalStateAnnouncement.formatSignalRestoredAnnouncement(stats)
        )
    }

    @Test
    fun formatNoSignalChange_announcesEnterAndExit() {
        assertEquals(
            "4 G, no signal",
            SignalStateAnnouncement.formatNoSignalChange(
                active = true,
                networkOperatorName = null,
                radioAccessType = CellularSignalReader.RADIO_4G
            )
        )
        assertEquals(
            "E E, 4 G, no signal",
            SignalStateAnnouncement.formatNoSignalChange(
                active = true,
                networkOperatorName = "EE",
                radioAccessType = CellularSignalReader.RADIO_4G
            )
        )
        assertEquals(
            "E E, 4 G, signal restored",
            SignalStateAnnouncement.formatNoSignalChange(
                active = false,
                networkOperatorName = "EE",
                radioAccessType = CellularSignalReader.RADIO_4G
            )
        )
        assertEquals(
            "2 G, signal restored",
            SignalStateAnnouncement.formatNoSignalChange(
                active = false,
                networkOperatorName = null,
                radioAccessType = CellularSignalReader.RADIO_2G
            )
        )
    }

    @Test
    fun formatNoSignalAnnouncement_uses2gWhenCampedOn2gWithoutRadioAccessType() {
        val stats = ConnectivityStats(
            isOn2g = true,
            monitor2gFallbackEnabled = true,
            networkOperatorName = "Vodafone UK",
            radioAccessType = null,
            noSignalActive = true
        )

        assertEquals(
            "Vodafone UK, 2 G, no signal",
            SignalStateAnnouncement.formatNoSignalAnnouncement(stats)
        )
    }

    @Test
    fun formatNoSignalAnnouncement_usesLastKnownRadioAccessTypeWhenCurrentIsBlank() {
        val stats = ConnectivityStats(
            networkOperatorName = "Vodafone UK",
            radioAccessType = null,
            noSignalActive = true
        )

        assertEquals(
            "Vodafone UK, 2 G, no signal",
            SignalStateAnnouncement.formatNoSignalAnnouncement(stats, CellularSignalReader.RADIO_2G)
        )
    }

    @Test
    fun formatNoSignalAnnouncement_uses2gWhenPhoneRestrictedTo2gOnly() {
        val stats = ConnectivityStats(
            restrictedTo2gNetwork = true,
            networkOperatorName = "Vodafone UK",
            radioAccessType = null,
            noSignalActive = true
        )

        assertEquals(
            "Vodafone UK, 2 G, no signal",
            SignalStateAnnouncement.formatNoSignalAnnouncement(stats)
        )
    }

    @Test
    fun formatLimitedServiceChange_announcesLimitedServiceEntryOnly() {
        val stats = ConnectivityStats(
            isLimitedService = true,
            networkOperatorName = "Vodafone UK",
            homeNetworkOperatorName = "Vodafone UK",
            servingNetworkOperatorName = "Vodafone UK",
            radioAccessType = CellularSignalReader.RADIO_2G,
            signalPermissionGranted = true
        )
        assertEquals(
            "Vodafone UK, 2 G, limited service",
            SignalStateAnnouncement.formatLimitedServiceChange(stats)
        )
    }

    @Test
    fun formatLimitedServiceAnnouncement_includesHomeAndVisitedOperators() {
        val stats = ConnectivityStats(
            isLimitedService = true,
            homeNetworkOperatorName = "Vodafone UK",
            servingNetworkOperatorName = "EE",
            homePlmn = "23415",
            plmn = "23430",
            radioAccessType = CellularSignalReader.RADIO_4G
        )

        assertEquals(
            "Vodafone UK home, E E visited, 4 G, limited service",
            SignalStateAnnouncement.formatLimitedServiceAnnouncement(stats)
        )
    }

    @Test
    fun formatLimitedServiceAnnouncement_mockVisited4gNamesHomeThenVisitedWithRoles() {
        val stats = PassiveMockSettings(scenario = MockNetworkScenario.ALT_OPERATOR_4G).toConnectivityStats(
            monitor2gFallback = true,
            passiveSettings = PassiveSignalSettings(),
            passiveIdleMode = false,
            passiveOnlySession = true
        )

        assertEquals(
            "Vodafone home, E E visited, 4 G, limited service",
            SignalStateAnnouncement.formatLimitedServiceAnnouncement(stats)
        )
    }

    @Test
    fun formatLimitedServiceAnnouncement_mockVisited2gNamesHomeThenVisitedWithRoles() {
        val stats = PassiveMockSettings(scenario = MockNetworkScenario.ALT_OPERATOR_2G).toConnectivityStats(
            monitor2gFallback = true,
            passiveSettings = PassiveSignalSettings(),
            passiveIdleMode = false,
            passiveOnlySession = true
        )

        assertEquals(
            "Vodafone home, E E visited, 2 G, limited service",
            SignalStateAnnouncement.formatLimitedServiceAnnouncement(stats)
        )
    }

    @Test
    fun formatLimitedServiceAnnouncement_omitsDuplicateHomeAndServingOperator() {
        val stats = ConnectivityStats(
            isLimitedService = true,
            homeNetworkOperatorName = "Vodafone UK",
            servingNetworkOperatorName = "Vodafone UK",
            homePlmn = "23415",
            plmn = "23415",
            radioAccessType = CellularSignalReader.RADIO_2G
        )

        assertEquals(
            "Vodafone UK, 2 G, limited service",
            SignalStateAnnouncement.formatLimitedServiceAnnouncement(stats)
        )
    }

    @Test
    fun formatG2CampedAnnouncement_includesOperatorAnd2g() {
        assertEquals(
            "Vodafone UK, 2 G",
            SignalStateAnnouncement.formatG2CampedAnnouncement("Vodafone UK")
        )
        assertEquals(
            "2 G",
            SignalStateAnnouncement.formatG2CampedAnnouncement(null)
        )
    }

    @Test
    fun formatSearching2gAnnouncement_includesLteRatAndSearchingPhrase() {
        assertEquals(
            "Vodafone UK, 4 G, no signal, searching 2 G",
            SignalStateAnnouncement.formatSearching2gAnnouncement(
                "Vodafone UK",
                CellularSignalReader.RADIO_4G
            )
        )
    }

    @Test
    fun formatDeadzoneAnnouncement_includesOperatorWhenKnown() {
        assertEquals(
            "Vodafone UK, deadzone, no service, no SOS calls",
            SignalStateAnnouncement.formatDeadzoneAnnouncement("Vodafone UK")
        )
        assertEquals(
            "deadzone, no service, no SOS calls",
            SignalStateAnnouncement.formatDeadzoneAnnouncement(null)
        )
    }

    @Test
    fun speakOperatorNameDisabled_omitsOperatorAcrossAnnouncementTypes() {
        assertEquals(
            "4 G",
            SignalStateAnnouncement.formatTechnologyChange(
                CellularSignalReader.RADIO_4G,
                "EE",
                speakOperatorNameEnabled = false
            )
        )
        assertEquals(
            "4 G, no signal",
            SignalStateAnnouncement.formatNoSignalAnnouncement(
                "EE",
                CellularSignalReader.RADIO_4G,
                speakOperatorNameEnabled = false
            )
        )
        assertEquals(
            "2 G, limited service",
            SignalStateAnnouncement.formatLimitedServiceAnnouncement(
                "EE",
                CellularSignalReader.RADIO_2G,
                speakOperatorNameEnabled = false
            )
        )
        assertEquals(
            "2 G",
            SignalStateAnnouncement.formatG2CampedAnnouncement("EE", speakOperatorNameEnabled = false)
        )
        assertEquals(
            "deadzone, no service, no SOS calls",
            SignalStateAnnouncement.formatDeadzoneAnnouncement("EE", speakOperatorNameEnabled = false)
        )
    }

    @Test
    fun speakTechnologyDisabled_omitsTechnologyAcrossAnnouncementTypes() {
        assertEquals(
            "E E",
            SignalStateAnnouncement.formatTechnologyChange(
                CellularSignalReader.RADIO_4G,
                "EE",
                speakTechnologyEnabled = false
            )
        )
        assertEquals(
            "E E, no signal",
            SignalStateAnnouncement.formatNoSignalAnnouncement(
                "EE",
                CellularSignalReader.RADIO_4G,
                speakTechnologyEnabled = false
            )
        )
        assertEquals(
            "E E, limited service",
            SignalStateAnnouncement.formatLimitedServiceAnnouncement(
                "EE",
                CellularSignalReader.RADIO_2G,
                speakTechnologyEnabled = false
            )
        )
        assertEquals(
            "E E",
            SignalStateAnnouncement.formatG2CampedAnnouncement("EE", speakTechnologyEnabled = false)
        )
    }

    @Test
    fun isLteNrRadioAccessType_recognizesLteAndNr() {
        assertTrue(SignalStateAnnouncement.isLteNrRadioAccessType(CellularSignalReader.RADIO_4G))
        assertTrue(SignalStateAnnouncement.isLteNrRadioAccessType(CellularSignalReader.RADIO_5G))
        assertFalse(SignalStateAnnouncement.isLteNrRadioAccessType(CellularSignalReader.RADIO_2G))
        assertFalse(SignalStateAnnouncement.isLteNrRadioAccessType(null))
    }

    @Test
    fun formatTier5SignalLow_visitedLimited4gUsesVisitedOperatorPhrase() {
        val stats = PassiveMockSettings(
            scenario = MockNetworkScenario.ALT_OPERATOR_4G,
            rsrpDbm = -122
        ).toConnectivityStats(
            monitor2gFallback = true,
            passiveSettings = PassiveSignalSettings(),
            passiveIdleMode = false,
            passiveOnlySession = true
        )

        assertEquals(
            "E E visited, 4 G, signal low",
            SignalStateAnnouncement.formatTier5SignalLowAnnouncement(stats)
        )
    }

    @Test
    fun formatNoSignalAnnouncement_visitedLimited4gUsesVisitedOperatorPhrase() {
        val stats = PassiveMockSettings(
            scenario = MockNetworkScenario.ALT_OPERATOR_4G,
            rsrpDbm = -130
        ).toConnectivityStats(
            monitor2gFallback = true,
            passiveSettings = PassiveSignalSettings(),
            passiveIdleMode = false,
            passiveOnlySession = true
        )

        assertEquals(
            "E E visited, 4 G, no signal",
            SignalStateAnnouncement.formatNoSignalAnnouncement(stats)
        )
    }

    @Test
    fun formatTier5SignalLow_visitedLimited2gUsesVisitedOperatorPhrase() {
        val stats = PassiveMockSettings(
            scenario = MockNetworkScenario.ALT_OPERATOR_2G,
            rsrpDbm = -110
        ).toConnectivityStats(
            monitor2gFallback = true,
            passiveSettings = PassiveSignalSettings(),
            passiveIdleMode = false,
            passiveOnlySession = true
        )

        assertEquals(
            "E E visited, 2 G, signal low",
            SignalStateAnnouncement.formatTier5SignalLowAnnouncement(stats)
        )
    }

    @Test
    fun formatSignalRestoredAnnouncement_visitedLimited4gUsesVisitedOperatorPhrase() {
        val stats = PassiveMockSettings(
            scenario = MockNetworkScenario.ALT_OPERATOR_4G,
            rsrpDbm = -75
        ).toConnectivityStats(
            monitor2gFallback = true,
            passiveSettings = PassiveSignalSettings(),
            passiveIdleMode = false,
            passiveOnlySession = true
        )

        assertEquals(
            "E E visited, 4 G, signal restored",
            SignalStateAnnouncement.formatSignalRestoredAnnouncement(stats)
        )
    }

    @Test
    fun formatSignalRestoredAnnouncement_visitedLimited2gUsesVisitedOperatorPhrase() {
        val stats = PassiveMockSettings(
            scenario = MockNetworkScenario.ALT_OPERATOR_2G,
            rsrpDbm = -95
        ).toConnectivityStats(
            monitor2gFallback = true,
            passiveSettings = PassiveSignalSettings(),
            passiveIdleMode = false,
            passiveOnlySession = true
        )

        assertEquals(
            "E E visited, 2 G, signal restored",
            SignalStateAnnouncement.formatSignalRestoredAnnouncement(stats)
        )
    }

    @Test
    fun formatNoSignalAnnouncement_visitedLimited2gUsesVisitedOperatorPhrase() {
        val stats = PassiveMockSettings(
            scenario = MockNetworkScenario.ALT_OPERATOR_2G,
            rsrpDbm = -130
        ).toConnectivityStats(
            monitor2gFallback = true,
            passiveSettings = PassiveSignalSettings(),
            passiveIdleMode = false,
            passiveOnlySession = true
        )

        assertEquals(
            "E E visited, 2 G, no signal",
            SignalStateAnnouncement.formatNoSignalAnnouncement(stats)
        )
    }
}
