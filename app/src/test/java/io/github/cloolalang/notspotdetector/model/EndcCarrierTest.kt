package io.github.cloolalang.notspotdetector.model

import io.github.cloolalang.notspotdetector.network.CellularSignalReader
import org.junit.Assert.assertEquals
import org.junit.Test

class EndcCarrierTest {

    @Test
    fun merge_keepsEndcWhenSignalStrengthOnlyReportsLte() {
        assertEquals(
            CellularSignalReader.RADIO_5G_ENDC,
            EndcCarrier.mergeRadioAccessType(
                fromSignal = CellularSignalReader.RADIO_4G,
                fromCell = CellularSignalReader.RADIO_5G_ENDC
            )
        )
    }

    @Test
    fun fill_usesTheModemNrChannelOnAnLteCamp() {
        val filled = EndcCarrier.fillFromPhysicalChannel(
            radioAccessType = CellularSignalReader.RADIO_4G,
            hasLteCamp = true,
            nrEarfcn = null,
            nrPci = null,
            nrBand = null,
            physical = NrCarrierReading(earfcn = 627264, pci = 12, band = 78)
        )

        assertEquals(CellularSignalReader.RADIO_5G_ENDC, filled.radioAccessType)
        assertEquals(627264, filled.nrEarfcn)
        assertEquals(12, filled.nrPci)
        assertEquals(78, filled.nrBand)
    }

    @Test
    fun fill_keepsACellInfoNrIdentity() {
        val filled = EndcCarrier.fillFromPhysicalChannel(
            radioAccessType = CellularSignalReader.RADIO_5G_ENDC,
            hasLteCamp = true,
            nrEarfcn = 156600,
            nrPci = 40,
            nrBand = 28,
            physical = NrCarrierReading(earfcn = 627264, pci = 12, band = 78)
        )

        assertEquals(156600, filled.nrEarfcn)
        assertEquals(40, filled.nrPci)
        assertEquals(28, filled.nrBand)
    }

    @Test
    fun fill_doesNotRelabel2g() {
        val filled = EndcCarrier.fillFromPhysicalChannel(
            radioAccessType = CellularSignalReader.RADIO_2G,
            hasLteCamp = false,
            nrEarfcn = null,
            nrPci = null,
            nrBand = null,
            physical = NrCarrierReading(earfcn = 627264, pci = 12, band = 78)
        )

        assertEquals(CellularSignalReader.RADIO_2G, filled.radioAccessType)
        assertEquals(null, filled.nrEarfcn)
    }

    @Test
    fun isNrDownlink_acceptsNrTypeHighChannelUnknownTypeAndNrOnlyBand() {
        assertEquals(true, EndcCarrier.isNrDownlink(networkType = 20, downlinkChannel = 151600, band = 28))
        assertEquals(true, EndcCarrier.isNrDownlink(networkType = 13, downlinkChannel = 627264, band = null))
        assertEquals(true, EndcCarrier.isNrDownlink(networkType = 0, downlinkChannel = 151600, band = null))
        assertEquals(true, EndcCarrier.isNrDownlink(networkType = 13, downlinkChannel = Int.MAX_VALUE, band = 78))
        assertEquals(false, EndcCarrier.isNrDownlink(networkType = 13, downlinkChannel = 6300, band = 20))
        assertEquals(false, EndcCarrier.isNrDownlink(networkType = 0, downlinkChannel = 6300, band = null))
    }
}
