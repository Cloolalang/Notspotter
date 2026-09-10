package io.github.cloolalang.notspotdetector.model

import io.github.cloolalang.notspotdetector.network.CellularSignalReader
import org.junit.Assert.assertEquals
import org.junit.Test

class CellIdentityAnnouncementTest {

    @Test
    fun format_lteChange_usesChannelAndPci() {
        val previous = CellIdentitySnapshot(lteEarfcn = 1_800, ltePci = 42)
        val next = CellIdentitySnapshot(lteEarfcn = 6_400, ltePci = 123)

        val announcement = CellIdentityAnnouncement.format(previous, next, CellularSignalReader.RADIO_4G)

        assertEquals("4 G, cell reselect, channel 6 4 0 0, PCI 1 2 3", announcement)
    }

    @Test
    fun format_pciOnlyChange_keepsChannel() {
        val previous = CellIdentitySnapshot(lteEarfcn = 6_400, ltePci = 42)
        val next = CellIdentitySnapshot(lteEarfcn = 6_400, ltePci = 123)

        val announcement = CellIdentityAnnouncement.format(previous, next, CellularSignalReader.RADIO_4G)

        assertEquals("4 G, cell reselect, channel 6 4 0 0, PCI 1 2 3", announcement)
    }

    @Test
    fun format_includesNetworkOperatorAtStart() {
        val previous = CellIdentitySnapshot(lteEarfcn = 1_800, ltePci = 42)
        val next = CellIdentitySnapshot(lteEarfcn = 6_400, ltePci = 123)

        val announcement = CellIdentityAnnouncement.format(
            previous,
            next,
            CellularSignalReader.RADIO_4G,
            networkOperatorName = "EE"
        )

        assertEquals("E E, 4 G, cell reselect, channel 6 4 0 0, PCI 1 2 3", announcement)
    }

    @Test
    fun format_visitedCamp_appendsVisitedRoleToOperator() {
        val previous = CellIdentitySnapshot(lteEarfcn = 1_800, ltePci = 42)
        val next = CellIdentitySnapshot(lteEarfcn = 6_400, ltePci = 123)

        val announcement = CellIdentityAnnouncement.format(
            previous,
            next,
            CellularSignalReader.RADIO_4G,
            networkOperatorName = "EE",
            campedOnVisitedOperator = true
        )

        assertEquals("E E visited, 4 G, cell reselect, channel 6 4 0 0, PCI 1 2 3", announcement)
    }

    @Test
    fun format_gsmChange_usesChannelAndBsic() {
        val previous = CellIdentitySnapshot(gsmEarfcn = 62, gsmBsic = 12)
        val next = CellIdentitySnapshot(gsmEarfcn = 71, gsmBsic = 15)

        val announcement = CellIdentityAnnouncement.format(
            previous,
            next,
            CellularSignalReader.RADIO_2G,
            networkOperatorName = "EE"
        )

        assertEquals("E E, 2 G, cell reselect, channel 7 1, BSIC 1 5", announcement)
    }

    @Test
    fun format_endcChange_includesLteAndNrPrefixes() {
        val previous = CellIdentitySnapshot(
            lteEarfcn = 1_800,
            ltePci = 42,
            nrEarfcn = 633_456,
            nrPci = 10
        )
        val next = CellIdentitySnapshot(
            lteEarfcn = 1_800,
            ltePci = 43,
            nrEarfcn = 633_456,
            nrPci = 11
        )

        val announcement = CellIdentityAnnouncement.format(
            previous,
            next,
            CellularSignalReader.RADIO_5G_ENDC
        )

        assertEquals(
            "5 G E N D C, cell reselect, LTE channel 1 8 0 0, PCI 4 3, NR channel 6 3 3 4 5 6, PCI 1 1",
            announcement
        )
    }

    @Test
    fun format_speakBandEnabledWithBandNumberStyle_speaksBandInsteadOfChannelAndPci() {
        val previous = CellIdentitySnapshot(lteEarfcn = 1_800, ltePci = 42)
        val next = CellIdentitySnapshot(lteEarfcn = 6_400, ltePci = 123)

        val announcement = CellIdentityAnnouncement.format(
            previous,
            next,
            CellularSignalReader.RADIO_4G,
            speakBandEnabled = true,
            bandNamingStyle = CellReselectBandNamingStyle.BAND_NUMBER
        )

        assertEquals("4 G, band, twenty", announcement)
    }

    @Test
    fun format_speakBandEnabledWithMhzNicknameStyle_speaksNicknameInsteadOfChannelAndPci() {
        val previous = CellIdentitySnapshot(lteEarfcn = 1_800, ltePci = 42)
        val next = CellIdentitySnapshot(lteEarfcn = 6_400, ltePci = 123)

        val announcement = CellIdentityAnnouncement.format(
            previous,
            next,
            CellularSignalReader.RADIO_4G,
            speakBandEnabled = true,
            bandNamingStyle = CellReselectBandNamingStyle.MHZ_NICKNAME
        )

        assertEquals("4 G, band, eight hundred", announcement)
    }

    @Test
    fun format_speakBandEnabledWithMhzNicknameStyle_speaksL2600AsTwentySixHundred() {
        val previous = CellIdentitySnapshot(lteEarfcn = 1_800, ltePci = 42)
        // 3000 is within band 7's downlink EARFCN range (2750..3449), mhzNickname "L2600".
        val next = CellIdentitySnapshot(lteEarfcn = 3_000, ltePci = 123)

        val announcement = CellIdentityAnnouncement.format(
            previous,
            next,
            CellularSignalReader.RADIO_4G,
            speakBandEnabled = true,
            bandNamingStyle = CellReselectBandNamingStyle.MHZ_NICKNAME
        )

        assertEquals("4 G, band, twenty six hundred", announcement)
    }

    @Test
    fun format_speakOperatorNameDisabled_omitsOperator() {
        val previous = CellIdentitySnapshot(lteEarfcn = 1_800, ltePci = 42)
        val next = CellIdentitySnapshot(lteEarfcn = 6_400, ltePci = 123)

        val announcement = CellIdentityAnnouncement.format(
            previous,
            next,
            CellularSignalReader.RADIO_4G,
            networkOperatorName = "EE",
            speakOperatorNameEnabled = false
        )

        assertEquals("4 G, cell reselect, channel 6 4 0 0, PCI 1 2 3", announcement)
    }

    @Test
    fun format_speakTechnologyDisabled_omitsTechnology() {
        val previous = CellIdentitySnapshot(lteEarfcn = 1_800, ltePci = 42)
        val next = CellIdentitySnapshot(lteEarfcn = 6_400, ltePci = 123)

        val announcement = CellIdentityAnnouncement.format(
            previous,
            next,
            CellularSignalReader.RADIO_4G,
            networkOperatorName = "EE",
            speakTechnologyEnabled = false
        )

        assertEquals("E E, cell reselect, channel 6 4 0 0, PCI 1 2 3", announcement)
    }

    @Test
    fun format_speakBandEnabledWithNoLteEarfcn_fallsBackToNormalPhrasing() {
        val previous = CellIdentitySnapshot(gsmEarfcn = 62, gsmBsic = 12)
        val next = CellIdentitySnapshot(gsmEarfcn = 71, gsmBsic = 15)

        val announcement = CellIdentityAnnouncement.format(
            previous,
            next,
            CellularSignalReader.RADIO_2G,
            speakBandEnabled = true,
            bandNamingStyle = CellReselectBandNamingStyle.BAND_NUMBER
        )

        assertEquals("2 G, cell reselect, channel 7 1, BSIC 1 5", announcement)
    }

    @Test
    fun previewText_speakBandEnabled_includesBandPhrase() {
        val preview = CellIdentityAnnouncement.previewText(
            networkOperatorName = null,
            speakBandEnabled = true,
            bandNamingStyle = CellReselectBandNamingStyle.BAND_NUMBER
        )

        assertEquals("4 G, band, twenty", preview)
    }
}
