package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AggregatedBandwidthHistoryTest {

    @Test
    fun bandFor_usesTheNamedWidthsAndDeepensBlueAbove30() {
        assertEquals(AggregatedBandwidthBand.MHZ_5, AggregatedBandwidthHistory.bandFor(5_000))
        assertEquals(AggregatedBandwidthBand.MHZ_10, AggregatedBandwidthHistory.bandFor(10_000))
        assertEquals(AggregatedBandwidthBand.MHZ_15, AggregatedBandwidthHistory.bandFor(15_000))
        assertEquals(AggregatedBandwidthBand.MHZ_20, AggregatedBandwidthHistory.bandFor(20_000))
        assertEquals(AggregatedBandwidthBand.MHZ_20, AggregatedBandwidthHistory.bandFor(30_000))
        assertEquals(AggregatedBandwidthBand.BLUE_40, AggregatedBandwidthHistory.bandFor(40_000))
        assertEquals(AggregatedBandwidthBand.BLUE_60, AggregatedBandwidthHistory.bandFor(50_000))
        assertEquals(AggregatedBandwidthBand.BLUE_100, AggregatedBandwidthHistory.bandFor(100_000))
        assertEquals(AggregatedBandwidthBand.BLUE_150, AggregatedBandwidthHistory.bandFor(130_000))
        assertEquals(AggregatedBandwidthBand.BLUE_WIDE, AggregatedBandwidthHistory.bandFor(200_000))
    }

    @Test
    fun current_isGreyWithoutABandwidthAndShowsTheSum() {
        assertNull(AggregatedBandwidthHistory.current(ConnectivityStats()))
        assertEquals(
            AggregatedBandwidthBand.NONE,
            AggregatedBandwidthHistory.current(
                ConnectivityStats(isMonitoring = true, noSignalActive = true, aggregatedBandwidthKhz = 20_000)
            )?.band
        )
        val visit = AggregatedBandwidthHistory.current(
            ConnectivityStats(isMonitoring = true, aggregatedBandwidthKhz = 130_000)
        )
        assertEquals(AggregatedBandwidthBand.BLUE_150, visit?.band)
        assertEquals("130 MHz", AggregatedBandwidthHistory.label(visit!!))
    }

    @Test
    fun current_marksTheSumAsAMinimumWhen4gPlusHasNoSecondaryWidth() {
        val unreported = ConnectivityStats(
            isMonitoring = true,
            ltePlusIcon = true,
            primaryCarrierBandwidthKhz = listOf(10_000),
            aggregatedBandwidthKhz = 10_000
        )
        val reported = unreported.copy(
            secondaryCarrierBandwidthKhz = listOf(5_000),
            aggregatedBandwidthKhz = 15_000
        )

        assertTrue(AggregatedBandwidthHistory.secondaryUnreported(unreported))
        assertEquals("10 MHz+", AggregatedBandwidthHistory.label(AggregatedBandwidthHistory.current(unreported)!!))
        assertFalse(AggregatedBandwidthHistory.secondaryUnreported(reported))
        assertEquals("15 MHz", AggregatedBandwidthHistory.label(AggregatedBandwidthHistory.current(reported)!!))
        assertFalse(
            AggregatedBandwidthHistory.secondaryUnreported(unreported.copy(ltePlusIcon = false))
        )
    }

    @Test
    fun record_holdsAShadeWhileTheSumUpdates() {
        val first = AggregatedBandwidthHistory.record(
            AggregatedBandwidthTape(),
            AggregatedBandwidthVisit(AggregatedBandwidthBand.BLUE_100, 80_000),
            nowMs = 1_000
        )
        val sameShade = AggregatedBandwidthHistory.record(
            first,
            AggregatedBandwidthVisit(AggregatedBandwidthBand.BLUE_100, 100_000),
            nowMs = 2_000
        )
        val wider = AggregatedBandwidthHistory.record(
            sameShade,
            AggregatedBandwidthVisit(AggregatedBandwidthBand.BLUE_150, 120_000),
            nowMs = 3_000
        )

        assertEquals(100_000, sameShade.entries.single().khz)
        assertEquals(1_000L, sameShade.entries.single().startedAtMs)
        assertEquals(
            listOf(AggregatedBandwidthBand.BLUE_100, AggregatedBandwidthBand.BLUE_150),
            wider.entries.map { it.band }
        )
    }
}
