package io.github.cloolalang.notspotdetector.model

import io.github.cloolalang.notspotdetector.network.CellularSignalReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PrimaryCarrierHistoryTest {

    @Test
    fun hue_lowBandsAreOrange_l2600IsBlue_band1AndBand3Differ() {
        assertEquals(PrimaryCarrierHistory.LOW_BAND_HUE, PrimaryCarrierHistory.hueForMhz(800)!!, 0.01f)
        assertEquals(PrimaryCarrierHistory.L2600_HUE, PrimaryCarrierHistory.hueForMhz(2600)!!, 0.01f)
        assertNull(PrimaryCarrierHistory.hueForMhz(null))
        assertNotEquals(PrimaryCarrierHistory.hueForMhz(1800), PrimaryCarrierHistory.hueForMhz(2100))
    }

    @Test
    fun pairFromStats_earfcn1363IsBand3_earfcn223IsBand1() {
        val band3 = PrimaryCarrierHistory.pairFromStats(
            ConnectivityStats(
                radioAccessType = CellularSignalReader.RADIO_4G,
                lteEarfcn = 1_363,
                ltePci = 10
            )
        )
        val band1 = PrimaryCarrierHistory.pairFromStats(
            ConnectivityStats(
                radioAccessType = CellularSignalReader.RADIO_4G,
                lteEarfcn = 223,
                ltePci = 10
            )
        )
        assertEquals(1800, band3?.nominalMhz)
        assertEquals(2100, band1?.nominalMhz)
        assertNotEquals(
            PrimaryCarrierHistory.hueForMhz(band3?.nominalMhz),
            PrimaryCarrierHistory.hueForMhz(band1?.nominalMhz)
        )
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
        assertEquals(PrimaryCarrierHistory.LOW_BAND_HUE, PrimaryCarrierHistory.hueForMhz(pair?.nominalMhz)!!, 0.01f)
    }

    @Test
    fun record_keepsEventsInsideTheWindowAndAssignsANewShadePerPciOnThatBand() {
        var tape = PrimaryCarrierTape()
        val visits = listOf(10, 20, 30, 40, 50).map { pci ->
            PrimaryCarrierPair(channel = 3_000, pci = pci, nominalMhz = 2600)
        }
        visits.forEach { tape = PrimaryCarrierHistory.record(tape, it) }

        assertEquals(listOf(10, 20, 30, 40, 50), tape.entries.map { it.pair!!.pci })
        assertEquals(PrimaryCarrierHistory.L2600_HUE, PrimaryCarrierHistory.hueForMhz(tape.entries.last().pair!!.nominalMhz)!!, 0.01f)
        assertEquals(listOf(0, 1, 2, 3, 4), tape.entries.map { it.shade })
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
        assertEquals(PrimaryCarrierHistory.LOW_BAND_HUE, PrimaryCarrierHistory.hueForMhz(tape.entries[1].pair!!.nominalMhz)!!, 0.01f)
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

    @Test
    fun record_noSignalIsAGreyGapAndTheSameCellAfterwardsIsANewVisit() {
        val cell = PrimaryCarrierPair(channel = 3_000, pci = 10, nominalMhz = 2600)
        var tape = PrimaryCarrierHistory.record(PrimaryCarrierTape(), cell, nowMs = 1_000L)
        tape = PrimaryCarrierHistory.record(
            tape,
            PrimaryCarrierVisit(noSignal = true),
            nowMs = 2_000L
        )
        tape = PrimaryCarrierHistory.record(
            tape,
            PrimaryCarrierVisit(noSignal = true),
            nowMs = 3_000L
        )
        tape = PrimaryCarrierHistory.record(tape, cell, nowMs = 4_000L)

        assertEquals(3, tape.entries.size)
        assertEquals(true, tape.entries[1].noSignal)
        assertEquals(null, tape.entries[1].pair)
        assertEquals(2_000L, tape.entries[1].startedAtMs)
        assertEquals(4_000L, tape.entries[2].startedAtMs)
        assertEquals(
            TechnologyHistory.LABEL_NO_SIGNAL,
            TechnologyHistory.technologyFromStats(
                ConnectivityStats(isMonitoring = true, noSignalActive = true, lteEarfcn = 3_000, ltePci = 10)
            )
        )
        assertEquals(
            PrimaryCarrierVisit(noSignal = true),
            PrimaryCarrierHistory.visitFromStats(
                ConnectivityStats(
                    isMonitoring = true,
                    isCompleteNoService = true,
                    lteEarfcn = 3_000,
                    ltePci = 10,
                    radioAccessType = CellularSignalReader.RADIO_4G
                )
            )
        )
    }
}
