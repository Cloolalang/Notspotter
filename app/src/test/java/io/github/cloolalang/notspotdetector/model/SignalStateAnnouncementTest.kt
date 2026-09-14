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
    fun formatMockNetworkAnnouncement_isFixedPhraseWithoutOperatorOrTech() {
        assertEquals("mock network", SignalStateAnnouncement.formatMockNetworkAnnouncement())
        assertEquals(
            SignalStateAnnouncement.PHRASE_MOCK_NETWORK,
            SignalStateAnnouncement.formatMockNetworkAnnouncement()
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
    fun formatLimitedServiceChange_announcesLimitedServiceEntry() {
        val stats = ConnectivityStats(
            isLimitedService = true,
            networkOperatorName = "Vodafone UK",
            homeNetworkOperatorName = "Vodafone UK",
            servingNetworkOperatorName = "Vodafone UK",
            radioAccessType = CellularSignalReader.RADIO_2G,
            signalPermissionGranted = true
        )
        assertEquals(
            "Vodafone UK, 2 G, home limited service",
            SignalStateAnnouncement.formatLimitedServiceChange(stats)
        )
    }

    @Test
    fun formatInServiceAnnouncement_speaksHomeInServiceOn2gAnd4g() {
        val silent = VoicePhraseOptions(
            speakOperatorName = false,
            speakTechnology = false,
            speakBand = false
        )
        val spoken = VoicePhraseOptions(
            speakOperatorName = true,
            speakTechnology = true,
            speakBand = false
        )
        assertEquals(
            "home in service",
            SignalStateAnnouncement.formatInServiceAnnouncement(homeInService(CellularSignalReader.RADIO_4G), phrases = silent)
        )
        assertEquals(
            "Vodafone, 4 G, home in service",
            SignalStateAnnouncement.formatInServiceAnnouncement(homeInService(CellularSignalReader.RADIO_4G), phrases = spoken)
        )
        assertEquals(
            "home in service",
            SignalStateAnnouncement.formatInServiceAnnouncement(homeInService(CellularSignalReader.RADIO_2G), phrases = silent)
        )
        assertEquals(
            "Vodafone, 2 G, home in service",
            SignalStateAnnouncement.formatInServiceAnnouncement(homeInService(CellularSignalReader.RADIO_2G), phrases = spoken)
        )
    }

    @Test
    fun formatInServiceAnnouncement_speaksRoamingInServiceOn2gAnd4g() {
        val silent = VoicePhraseOptions(
            speakOperatorName = false,
            speakTechnology = false,
            speakBand = false
        )
        val spoken = VoicePhraseOptions(
            speakOperatorName = true,
            speakTechnology = true,
            speakBand = false
        )
        assertEquals(
            "roaming in service",
            SignalStateAnnouncement.formatInServiceAnnouncement(roamingInService(CellularSignalReader.RADIO_4G), phrases = silent)
        )
        assertEquals(
            "E E, 4 G, roaming in service",
            SignalStateAnnouncement.formatInServiceAnnouncement(roamingInService(CellularSignalReader.RADIO_4G), phrases = spoken)
        )
        assertEquals(
            "roaming in service",
            SignalStateAnnouncement.formatInServiceAnnouncement(roamingInService(CellularSignalReader.RADIO_2G), phrases = silent)
        )
        assertEquals(
            "E E, 2 G, roaming in service",
            SignalStateAnnouncement.formatInServiceAnnouncement(roamingInService(CellularSignalReader.RADIO_2G), phrases = spoken)
        )
    }

    @Test
    fun formatLimitedServiceChange_announcesHomeInServiceOnFullCamp() {
        assertEquals(
            "home in service",
            SignalStateAnnouncement.formatLimitedServiceChange(
                homeInService(CellularSignalReader.RADIO_4G),
                phrases = VoicePhraseOptions(
                    speakOperatorName = false,
                    speakTechnology = false
                )
            )
        )
        assertEquals(
            "home in service",
            SignalStateAnnouncement.formatLimitedServiceChange(
                homeInService(CellularSignalReader.RADIO_2G),
                phrases = VoicePhraseOptions(
                    speakOperatorName = false,
                    speakTechnology = false
                )
            )
        )
    }

    @Test
    fun isInServiceAnnouncement_matchesHomeAndRoamingPhrases() {
        assertTrue(SignalStateAnnouncement.isInServiceAnnouncement("home in service"))
        assertTrue(SignalStateAnnouncement.isInServiceAnnouncement("roaming in service"))
        assertTrue(SignalStateAnnouncement.isInServiceAnnouncement("in-service"))
        assertFalse(SignalStateAnnouncement.isInServiceAnnouncement("home limited service"))
        assertFalse(SignalStateAnnouncement.isInServiceAnnouncement(null))
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
            "Vodafone UK home, E E visited, 4 G, visiting limited service",
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
            "Vodafone home, E E visited, 4 G, visiting limited service",
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
            "Vodafone home, E E visited, 2 G, visiting limited service",
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
            "Vodafone UK, 2 G, home limited service",
            SignalStateAnnouncement.formatLimitedServiceAnnouncement(stats)
        )
    }

    @Test
    fun formatLimitedServiceAnnouncement_roleTogglesOffKeepPlainLimitedService() {
        val home = ConnectivityStats(
            isLimitedService = true,
            homeNetworkOperatorName = "Vodafone UK",
            servingNetworkOperatorName = "Vodafone UK",
            homePlmn = "23415",
            plmn = "23415",
            radioAccessType = CellularSignalReader.RADIO_2G
        )
        val visited = ConnectivityStats(
            isLimitedService = true,
            homeNetworkOperatorName = "Vodafone UK",
            servingNetworkOperatorName = "EE",
            homePlmn = "23415",
            plmn = "23430",
            radioAccessType = CellularSignalReader.RADIO_4G
        )
        val off = VoicePhraseOptions(
            speakHomeLimitedService = false,
            speakVisitingLimitedService = false
        )
        assertEquals(
            "Vodafone UK, 2 G, limited service",
            SignalStateAnnouncement.formatLimitedServiceAnnouncement(home, phrases = off)
        )
        assertEquals(
            "Vodafone UK home, E E visited, 4 G, limited service",
            SignalStateAnnouncement.formatLimitedServiceAnnouncement(visited, phrases = off)
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
            "Vodafone UK, dead zone, no service, no SOS calls",
            SignalStateAnnouncement.formatDeadzoneAnnouncement("Vodafone UK")
        )
        assertEquals(
            "dead zone, no service, no SOS calls",
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
            "2 G, home limited service",
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
            "dead zone, no service, no SOS calls",
            SignalStateAnnouncement.formatDeadzoneAnnouncement("EE", speakOperatorNameEnabled = false)
        )
    }

    @Test
    fun speakTechnologyDisabled_omitsTechOnTechnologyChangeAnnouncement() {
        assertEquals(
            "E E",
            SignalStateAnnouncement.formatTechnologyChange(
                CellularSignalReader.RADIO_4G,
                "EE",
                speakTechnologyEnabled = false
            )
        )
    }

    @Test
    fun speakBand_isSpokenAfterOperatorAndTechnology() {
        assertEquals(
            "E E, 4 G, B, twenty, no signal",
            SignalStateAnnouncement.formatNoSignalAnnouncement(
                "EE",
                CellularSignalReader.RADIO_4G,
                speakBandEnabled = true,
                bandPhrase = "B, twenty"
            )
        )
        assertEquals(
            "E E, 4 G, B, twenty",
            SignalStateAnnouncement.formatTechnologyChange(
                CellularSignalReader.RADIO_4G,
                "EE",
                speakBandEnabled = true,
                bandPhrase = "B, twenty"
            )
        )
        assertEquals(
            "E E, 4 G, B, twenty, home limited service",
            SignalStateAnnouncement.formatLimitedServiceAnnouncement(
                "EE",
                CellularSignalReader.RADIO_4G,
                speakBandEnabled = true,
                bandPhrase = "B, twenty"
            )
        )
    }

    @Test
    fun speakBand_followsCellReselectNamingStyle() {
        val speakBand = VoicePhraseOptions(speakBand = true)
        val earfcn6400 = ConnectivityStats(
            networkOperatorName = "EE",
            radioAccessType = CellularSignalReader.RADIO_4G,
            lteEarfcn = 6400,
            noSignalActive = true
        )

        assertEquals(
            "E E, 4 G, eight hundred",
            SignalStateAnnouncement.previewTechnologyChange(
                "EE",
                phrases = speakBand,
                bandNamingStyle = CellReselectBandNamingStyle.MHZ_NICKNAME
            )
        )
        assertEquals(
            "E E, 4 G, twenty",
            SignalStateAnnouncement.previewTechnologyChange(
                "EE",
                phrases = speakBand,
                bandNamingStyle = CellReselectBandNamingStyle.BAND_NUMBER
            )
        )
        assertEquals(
            "E E, 4 G, eight hundred, no signal",
            SignalStateAnnouncement.previewNoSignal(
                "EE",
                phrases = speakBand,
                bandNamingStyle = CellReselectBandNamingStyle.MHZ_NICKNAME
            )
        )
        assertEquals(
            "E E, 4 G, twenty, no signal",
            SignalStateAnnouncement.formatNoSignalAnnouncement(
                earfcn6400,
                phrases = speakBand,
                bandNamingStyle = CellReselectBandNamingStyle.BAND_NUMBER
            )
        )
        assertEquals(
            "E E, 4 G, eight hundred, signal low",
            SignalStateAnnouncement.previewTier5SignalLow(
                "EE",
                phrases = speakBand,
                bandNamingStyle = CellReselectBandNamingStyle.MHZ_NICKNAME
            )
        )
        assertEquals(
            "E E, 4 G, twenty, home in service",
            SignalStateAnnouncement.previewInService(
                "EE",
                phrases = speakBand,
                bandNamingStyle = CellReselectBandNamingStyle.BAND_NUMBER
            )
        )
        assertEquals(
            "2 G, nine hundred",
            SignalStateAnnouncement.formatG2CampedAnnouncement(
                networkOperatorName = null,
                phrases = speakBand,
                gsmEarfcn = 62,
                bandNamingStyle = CellReselectBandNamingStyle.MHZ_NICKNAME
            )
        )
        assertEquals(
            "4 G, eight, no signal, searching 2 G",
            SignalStateAnnouncement.formatSearching2gAnnouncement(
                networkOperatorName = null,
                lastKnownLteNrRadioAccessType = CellularSignalReader.RADIO_4G,
                phrases = speakBand,
                gsmEarfcn = 62,
                bandNamingStyle = CellReselectBandNamingStyle.BAND_NUMBER
            )
        )
        assertEquals(
            "E E home, E E visited, 4 G, eight hundred, visiting limited service",
            SignalStateAnnouncement.previewLimitedService(
                "EE",
                phrases = speakBand,
                bandNamingStyle = CellReselectBandNamingStyle.MHZ_NICKNAME
            )
        )
    }

    @Test
    fun speakTechnologyDisabled_omitsTechnologyAcrossOtherAnnouncementTypes() {
        assertEquals(
            "E E, no signal",
            SignalStateAnnouncement.formatNoSignalAnnouncement(
                "EE",
                CellularSignalReader.RADIO_4G,
                speakTechnologyEnabled = false
            )
        )
        assertEquals(
            "E E, home limited service",
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
            "E E visited, 4 G, no signal, visiting limited service",
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
            "E E visited, 2 G, no signal, visiting limited service",
            SignalStateAnnouncement.formatNoSignalAnnouncement(stats)
        )
    }

    private fun homeInService(radioAccessType: String): ConnectivityStats {
        return ConnectivityStats(
            isLimitedService = false,
            networkServiceMode = NetworkServiceMode.IN_SERVICE,
            isOn2g = radioAccessType == CellularSignalReader.RADIO_2G,
            networkOperatorName = "Vodafone",
            homeNetworkOperatorName = "Vodafone",
            servingNetworkOperatorName = "Vodafone",
            homePlmn = "23415",
            plmn = "23415",
            radioAccessType = radioAccessType
        )
    }

    private fun roamingInService(radioAccessType: String): ConnectivityStats {
        return ConnectivityStats(
            isLimitedService = false,
            networkServiceMode = NetworkServiceMode.IN_SERVICE,
            isNetworkRoaming = true,
            isOn2g = radioAccessType == CellularSignalReader.RADIO_2G,
            networkOperatorName = "EE",
            homeNetworkOperatorName = "Vodafone",
            servingNetworkOperatorName = "EE",
            homePlmn = "23415",
            plmn = "23430",
            radioAccessType = radioAccessType
        )
    }
}
