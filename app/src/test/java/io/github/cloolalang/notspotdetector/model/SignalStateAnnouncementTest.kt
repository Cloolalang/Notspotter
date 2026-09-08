package io.github.cloolalang.notspotdetector.model

import io.github.cloolalang.notspotdetector.network.CellularSignalReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SignalStateAnnouncementTest {

    @Test
    fun formatTechnologyChange_includesOperatorAndSpokenRat() {
        val announcement = SignalStateAnnouncement.formatTechnologyChange(
            CellularSignalReader.RADIO_4G,
            "EE"
        )
        assertEquals("E E, Technology change, 4 G", announcement)
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
            "E E, Signal restored, 4 G",
            SignalStateAnnouncement.formatNoSignalChange(
                active = false,
                networkOperatorName = "EE",
                radioAccessType = CellularSignalReader.RADIO_4G
            )
        )
        assertEquals(
            "Signal restored, 2 G",
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
    fun formatLimitedServiceChange_announcesEnterAndExitWithTechnology() {
        assertEquals(
            "Limited service, 4 G",
            SignalStateAnnouncement.formatLimitedServiceChange(
                active = true,
                networkOperatorName = null,
                radioAccessType = CellularSignalReader.RADIO_4G
            )
        )
        assertEquals(
            "Vodafone UK, Limited service, 2 G",
            SignalStateAnnouncement.formatLimitedServiceChange(
                active = true,
                networkOperatorName = "Vodafone UK",
                radioAccessType = CellularSignalReader.RADIO_2G
            )
        )
        assertEquals(
            "E E, Full service, 4 G",
            SignalStateAnnouncement.formatLimitedServiceChange(
                active = false,
                networkOperatorName = "EE",
                radioAccessType = CellularSignalReader.RADIO_4G
            )
        )
        assertEquals(
            "Full service, 2 G",
            SignalStateAnnouncement.formatLimitedServiceChange(
                active = false,
                networkOperatorName = null,
                radioAccessType = CellularSignalReader.RADIO_2G
            )
        )
    }

    @Test
    fun formatLimitedServiceAnnouncement_includesHomeAndAlternativeOperators() {
        val stats = ConnectivityStats(
            isLimitedService = true,
            homeNetworkOperatorName = "Vodafone UK",
            servingNetworkOperatorName = "EE",
            homePlmn = "23415",
            plmn = "23430",
            radioAccessType = CellularSignalReader.RADIO_4G
        )

        assertEquals(
            "Vodafone UK, E E, Limited service, 4 G",
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
            "Vodafone UK, Limited service, 2 G",
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
            "Vodafone UK, all technologies dead zone, scanning",
            SignalStateAnnouncement.formatDeadzoneAnnouncement("Vodafone UK")
        )
        assertEquals(
            "all technologies dead zone, scanning",
            SignalStateAnnouncement.formatDeadzoneAnnouncement(null)
        )
    }

    @Test
    fun isLteNrRadioAccessType_recognizesLteAndNr() {
        assertTrue(SignalStateAnnouncement.isLteNrRadioAccessType(CellularSignalReader.RADIO_4G))
        assertTrue(SignalStateAnnouncement.isLteNrRadioAccessType(CellularSignalReader.RADIO_5G))
        assertFalse(SignalStateAnnouncement.isLteNrRadioAccessType(CellularSignalReader.RADIO_2G))
        assertFalse(SignalStateAnnouncement.isLteNrRadioAccessType(null))
    }
}
