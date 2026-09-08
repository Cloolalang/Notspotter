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

        assertEquals("Cell reselect, channel 6 4 0 0, PCI 1 2 3", announcement)
    }

    @Test
    fun format_pciOnlyChange_keepsChannel() {
        val previous = CellIdentitySnapshot(lteEarfcn = 6_400, ltePci = 42)
        val next = CellIdentitySnapshot(lteEarfcn = 6_400, ltePci = 123)

        val announcement = CellIdentityAnnouncement.format(previous, next, CellularSignalReader.RADIO_4G)

        assertEquals("Cell reselect, channel 6 4 0 0, PCI 1 2 3", announcement)
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

        assertEquals("E E, Cell reselect, channel 6 4 0 0, PCI 1 2 3", announcement)
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
            "Cell reselect, LTE channel 1 8 0 0, PCI 4 3, NR channel 6 3 3 4 5 6, PCI 1 1",
            announcement
        )
    }
}
