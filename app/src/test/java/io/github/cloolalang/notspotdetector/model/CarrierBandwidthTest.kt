package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CarrierBandwidthTest {

    @Test
    fun fromChannels_splitsPrimaryAndSecondaryAndDropsNeighbours() {
        val bandwidths = CarrierBandwidth.fromChannels(
            listOf(
                ChannelBandwidth(CarrierBandwidth.CONNECTION_PRIMARY, 20_000),
                ChannelBandwidth(CarrierBandwidth.CONNECTION_SECONDARY, 10_000),
                ChannelBandwidth(CarrierBandwidth.CONNECTION_SECONDARY, 100_000),
                ChannelBandwidth(CarrierBandwidth.CONNECTION_SECONDARY, 10_000),
                ChannelBandwidth(CarrierBandwidth.CONNECTION_NONE, 5_000),
                ChannelBandwidth(CarrierBandwidth.CONNECTION_PRIMARY, 0)
            )
        )

        assertEquals(listOf(20_000), bandwidths.primaryDownlinkKhz)
        assertEquals(listOf(10_000, 100_000), bandwidths.secondaryDownlinkKhz)
    }

    @Test
    fun fromChannels_keepsTwoPrimariesAndUsesUnknownToFillAMissingPrimary() {
        val bothPrimary = CarrierBandwidth.fromChannels(
            listOf(
                ChannelBandwidth(CarrierBandwidth.CONNECTION_PRIMARY, 20_000),
                ChannelBandwidth(CarrierBandwidth.CONNECTION_PRIMARY, 100_000)
            )
        )
        val unknown = CarrierBandwidth.fromChannels(
            listOf(
                ChannelBandwidth(connectionStatus = Int.MAX_VALUE, downlinkKhz = 20_000),
                ChannelBandwidth(connectionStatus = -1, downlinkKhz = 10_000)
            )
        )

        assertEquals(listOf(20_000, 100_000), bothPrimary.primaryDownlinkKhz)
        assertEquals(listOf(20_000), unknown.primaryDownlinkKhz)
        assertEquals(listOf(10_000), unknown.secondaryDownlinkKhz)
    }

    @Test
    fun merge_prefersLabeledCarriersAndFallsBackToAnUnlabeledList() {
        val labeled = CarrierBandwidth.merge(
            physical = CarrierBandwidths(secondaryDownlinkKhz = listOf(100_000)),
            lteCell = CarrierBandwidths(primaryDownlinkKhz = listOf(20_000)),
            unlabeledKhz = listOf(5_000)
        )
        val fallback = CarrierBandwidth.merge(
            physical = CarrierBandwidths(),
            lteCell = CarrierBandwidths(),
            unlabeledKhz = listOf(15_000, 1_400)
        )

        assertEquals(listOf(20_000), labeled.primaryDownlinkKhz)
        assertEquals(listOf(100_000), labeled.secondaryDownlinkKhz)
        assertEquals(listOf(15_000), fallback.primaryDownlinkKhz)
        assertEquals(listOf(1_400), fallback.secondaryDownlinkKhz)
    }

    @Test
    fun formatMhz_usesWholeMegahertzAndTenths() {
        assertEquals("20 MHz", CarrierBandwidth.formatMhz(20_000))
        assertEquals("1.4 MHz", CarrierBandwidth.formatMhz(1_400))
        assertEquals("100 MHz", CarrierBandwidth.formatMhz(100_000))
        assertEquals("20 MHz, 100 MHz", CarrierBandwidth.formatList(listOf(20_000, 100_000)))
        assertNull(CarrierBandwidth.formatList(emptyList()))
    }

    @Test
    fun aggregatedKhz_sumsPrimaryLteSecondaryLteAndEveryNrCarrier() {
        val total = CarrierBandwidth.aggregatedKhz(
            physical = listOf(
                ChannelBandwidth(CarrierBandwidth.CONNECTION_PRIMARY, 20_000, CarrierRadio.LTE),
                ChannelBandwidth(CarrierBandwidth.CONNECTION_SECONDARY, 20_000, CarrierRadio.LTE),
                ChannelBandwidth(CarrierBandwidth.CONNECTION_SECONDARY, 10_000, CarrierRadio.LTE),
                ChannelBandwidth(CarrierBandwidth.CONNECTION_SECONDARY, 100_000, CarrierRadio.NR),
                ChannelBandwidth(CarrierBandwidth.CONNECTION_NONE, 5_000, CarrierRadio.LTE)
            ),
            lteCell = listOf(
                ChannelBandwidth(CarrierBandwidth.CONNECTION_PRIMARY, 20_000, CarrierRadio.LTE)
            ),
            unlabeledKhz = listOf(15_000)
        )

        assertEquals(150_000, total)
    }

    @Test
    fun aggregatedKhz_usesLteCellWhenTheModemReportHasOnlyNr() {
        val total = CarrierBandwidth.aggregatedKhz(
            physical = listOf(
                ChannelBandwidth(CarrierBandwidth.CONNECTION_SECONDARY, 100_000, CarrierRadio.NR)
            ),
            lteCell = listOf(
                ChannelBandwidth(CarrierBandwidth.CONNECTION_PRIMARY, 20_000, CarrierRadio.LTE),
                ChannelBandwidth(CarrierBandwidth.CONNECTION_SECONDARY, 10_000, CarrierRadio.LTE)
            ),
            unlabeledKhz = emptyList()
        )

        assertEquals(120_000, total)
    }

    @Test
    fun aggregatedKhz_usesAnUnlabeledListWhenNoCarrierWasNamed() {
        assertEquals(
            30_000,
            CarrierBandwidth.aggregatedKhz(emptyList(), emptyList(), listOf(20_000, 10_000))
        )
        assertNull(CarrierBandwidth.aggregatedKhz(emptyList(), emptyList(), emptyList()))
    }

    @Test
    fun aggregatedKhz_countsARepeatedPrimaryOnce() {
        val repeatedPrimary = List(3) {
            ChannelBandwidth(CarrierBandwidth.CONNECTION_PRIMARY, 20_000, CarrierRadio.LTE)
        }
        val total = CarrierBandwidth.aggregatedKhz(
            physical = repeatedPrimary + ChannelBandwidth(
                CarrierBandwidth.CONNECTION_SECONDARY,
                15_000,
                CarrierRadio.LTE
            ),
            lteCell = listOf(
                ChannelBandwidth(CarrierBandwidth.CONNECTION_PRIMARY, 20_000, CarrierRadio.LTE)
            ),
            unlabeledKhz = emptyList()
        )

        assertEquals(35_000, total)
    }

    @Test
    fun aggregatedKhz_usesTheModemReportInsteadOfExtraCellWidths() {
        val total = CarrierBandwidth.aggregatedKhz(
            physical = listOf(
                ChannelBandwidth(CarrierBandwidth.CONNECTION_PRIMARY, 20_000, CarrierRadio.LTE)
            ),
            lteCell = listOf(
                ChannelBandwidth(CarrierBandwidth.CONNECTION_PRIMARY, 20_000, CarrierRadio.LTE),
                ChannelBandwidth(CarrierBandwidth.CONNECTION_SECONDARY, 15_000, CarrierRadio.LTE)
            ),
            unlabeledKhz = emptyList()
        )

        assertEquals(20_000, total)
    }

    @Test
    fun aggregatedKhz_countsTwoCarriersOfTheSameWidthOnDifferentChannels() {
        val total = CarrierBandwidth.aggregatedKhz(
            physical = listOf(
                ChannelBandwidth(
                    CarrierBandwidth.CONNECTION_PRIMARY,
                    20_000,
                    CarrierRadio.LTE,
                    channelNumber = 100
                ),
                ChannelBandwidth(
                    CarrierBandwidth.CONNECTION_SECONDARY,
                    20_000,
                    CarrierRadio.LTE,
                    channelNumber = 200
                ),
                ChannelBandwidth(
                    CarrierBandwidth.CONNECTION_SECONDARY,
                    20_000,
                    CarrierRadio.LTE,
                    channelNumber = 100
                )
            ),
            lteCell = emptyList(),
            unlabeledKhz = emptyList()
        )

        assertEquals(40_000, total)
    }

    @Test
    fun aggregatedKhz_ignoresNeighbourCellsWhenTheModemReportIsMissing() {
        val total = CarrierBandwidth.aggregatedKhz(
            physical = emptyList(),
            lteCell = listOf(
                ChannelBandwidth(
                    connectionStatus = Int.MAX_VALUE,
                    downlinkKhz = 20_000,
                    radio = CarrierRadio.LTE,
                    registered = true
                ),
                ChannelBandwidth(
                    connectionStatus = Int.MAX_VALUE,
                    downlinkKhz = 15_000,
                    radio = CarrierRadio.LTE
                ),
                ChannelBandwidth(
                    connectionStatus = Int.MAX_VALUE,
                    downlinkKhz = 20_000,
                    radio = CarrierRadio.LTE
                ),
                ChannelBandwidth(
                    connectionStatus = Int.MAX_VALUE,
                    downlinkKhz = 20_000,
                    radio = CarrierRadio.LTE
                )
            ),
            unlabeledKhz = emptyList()
        )

        assertEquals(20_000, total)
    }

    @Test
    fun aggregatedKhz_usesTheCampedCellWhenAnotherCellIsMarkedPrimary() {
        val total = CarrierBandwidth.aggregatedKhz(
            physical = emptyList(),
            lteCell = listOf(
                ChannelBandwidth(
                    CarrierBandwidth.CONNECTION_PRIMARY,
                    15_000,
                    CarrierRadio.LTE,
                    channelNumber = 100
                ),
                ChannelBandwidth(
                    connectionStatus = Int.MAX_VALUE,
                    downlinkKhz = 5_000,
                    radio = CarrierRadio.LTE,
                    channelNumber = 6_200,
                    registered = true
                )
            ),
            unlabeledKhz = emptyList()
        )

        assertEquals(5_000, total)
    }

    @Test
    fun aggregatedKhz_usesTheModemWidthForTheCampedChannel() {
        val total = CarrierBandwidth.aggregatedKhz(
            physical = listOf(
                ChannelBandwidth(
                    CarrierBandwidth.CONNECTION_SECONDARY,
                    5_000,
                    CarrierRadio.LTE,
                    channelNumber = 6_200
                )
            ),
            lteCell = listOf(
                ChannelBandwidth(
                    CarrierBandwidth.CONNECTION_PRIMARY,
                    15_000,
                    CarrierRadio.LTE,
                    channelNumber = 6_200,
                    registered = true
                )
            ),
            unlabeledKhz = emptyList(),
            servingChannel = 6_200
        )

        assertEquals(5_000, total)
    }

    @Test
    fun aggregatedKhz_addsSecondariesFromServiceStateWhenTheCellScanNamesOnlyThePrimary() {
        val lteCell = listOf(
            ChannelBandwidth(
                CarrierBandwidth.CONNECTION_PRIMARY,
                20_000,
                CarrierRadio.LTE,
                registered = true
            )
        )
        val merged = CarrierBandwidth.merge(
            physical = CarrierBandwidths(),
            lteCell = CarrierBandwidth.fromChannels(CarrierBandwidth.lteServingChannels(lteCell)),
            unlabeledKhz = listOf(20_000, 10_000)
        )

        assertEquals(listOf(20_000), merged.primaryDownlinkKhz)
        assertEquals(listOf(10_000), merged.secondaryDownlinkKhz)
        assertEquals(
            30_000,
            CarrierBandwidth.aggregatedKhz(emptyList(), lteCell, listOf(20_000, 10_000))
        )
    }

    @Test
    fun aggregatedKhz_addsSecondariesFromServiceStateWhenTheModemNamesOnlyThePrimary() {
        val total = CarrierBandwidth.aggregatedKhz(
            physical = listOf(
                ChannelBandwidth(CarrierBandwidth.CONNECTION_PRIMARY, 15_000, CarrierRadio.LTE)
            ),
            lteCell = emptyList(),
            unlabeledKhz = listOf(20_000, 10_000, 5_000)
        )

        assertEquals(30_000, total)
    }

    @Test
    fun aggregatedKhz_prefersASingleLiveBandwidthOverTheCellIdentity() {
        val total = CarrierBandwidth.aggregatedKhz(
            physical = emptyList(),
            lteCell = listOf(
                ChannelBandwidth(
                    CarrierBandwidth.CONNECTION_PRIMARY,
                    15_000,
                    CarrierRadio.LTE,
                    registered = true
                )
            ),
            unlabeledKhz = listOf(5_000)
        )

        assertEquals(5_000, total)
    }
}
