package io.github.cloolalang.notspotdetector.model

import io.github.cloolalang.notspotdetector.network.CellularSignalReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PrimaryCarrierHistoryTest {

    @Test
    fun family_band20IsOrange_band7IsBlue_midBandIsGreen() {
        assertEquals(CarrierTapeFamily.ORANGE, PrimaryCarrierHistory.familyForMhz(800))
        assertEquals(CarrierTapeFamily.BLUE, PrimaryCarrierHistory.familyForMhz(2600))
        assertEquals(CarrierTapeFamily.GREEN, PrimaryCarrierHistory.familyForMhz(1800))
        assertEquals(CarrierTapeFamily.NEUTRAL, PrimaryCarrierHistory.familyForMhz(null))
    }

    @Test
    fun pairFromStats_endcUsesLteEarfcn() {
        val pair = PrimaryCarrierHistory.pairFromStats(
            ConnectivityStats(
                radioAccessType = CellularSignalReader.RADIO_5G_ENDC,
                lteEarfcn = 3_000,
                ltePci = 42,
                nrBand = 78
            )
        )
        assertEquals(PrimaryCarrierPair(channel = 3_000, pci = 42, nominalMhz = 2600), pair)
    }

    @Test
    fun pairFromStats_band20EarfcnIs800() {
        val pair = PrimaryCarrierHistory.pairFromStats(
            ConnectivityStats(
                radioAccessType = CellularSignalReader.RADIO_4G,
                lteEarfcn = 6_300,
                ltePci = 7
            )
        )
        assertEquals(800, pair?.nominalMhz)
        assertEquals(CarrierTapeFamily.ORANGE, PrimaryCarrierHistory.familyForMhz(pair?.nominalMhz))
    }

    @Test
    fun record_keepsLastFourAndAssignsANewShadePerPciOnThatBand() {
        var tape = PrimaryCarrierTape()
        val visits = listOf(10, 20, 30, 40, 50).map { pci ->
            PrimaryCarrierPair(channel = 3_000, pci = pci, nominalMhz = 2600)
        }
        visits.forEach { tape = PrimaryCarrierHistory.record(tape, it) }

        assertEquals(listOf(20, 30, 40, 50), tape.entries.map { it.pair.pci })
        assertEquals(CarrierTapeFamily.BLUE, tape.entries.last().family)
        assertEquals(listOf(1, 2, 3, 4), tape.entries.map { it.shade })
        assertEquals(0, tape.shades[CarrierShadeKey(2600, 10)])
    }

    @Test
    fun record_samePciOnTheSameBandKeepsItsShade() {
        var tape = PrimaryCarrierTape()
        val first = PrimaryCarrierPair(3_000, 10, 2600)
        val other = PrimaryCarrierPair(6_300, 7, 800)
        val back = PrimaryCarrierPair(3_000, 10, 2600)
        tape = PrimaryCarrierHistory.record(tape, first)
        tape = PrimaryCarrierHistory.record(tape, other)
        tape = PrimaryCarrierHistory.record(tape, back)

        assertEquals(0, tape.entries.first().shade)
        assertEquals(0, tape.entries.last().shade)
        assertEquals(CarrierTapeFamily.ORANGE, tape.entries[1].family)
        assertEquals(0, tape.entries[1].shade)
    }

    @Test
    fun record_repeatOfTheCurrentPairDoesNotGrowTheTape() {
        val pair = PrimaryCarrierPair(3_000, 10, 2600)
        val once = PrimaryCarrierHistory.record(PrimaryCarrierTape(), pair)
        val twice = PrimaryCarrierHistory.record(once, pair)
        assertEquals(once, twice)
    }

    @Test
    fun segmentDuration_followsReselectRate() {
        assertNull(PrimaryCarrierHistory.segmentDurationMs(0))
        assertEquals(60_000L, PrimaryCarrierHistory.segmentDurationMs(1))
        assertEquals(10_000L, PrimaryCarrierHistory.segmentDurationMs(6))
    }
}
