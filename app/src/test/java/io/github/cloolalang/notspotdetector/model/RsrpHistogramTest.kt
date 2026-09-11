package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Test

class RsrpHistogramTest {

    @Test
    fun binIndex_mapsStrongestAndWeakestValues() {
        assertEquals(0, RsrpHistogram.binIndexForRsrp(-70))
        assertEquals(1, RsrpHistogram.binIndexForRsrp(-79))
        assertEquals(2, RsrpHistogram.binIndexForRsrp(-80))
        assertEquals(11, RsrpHistogram.binIndexForRsrp(-126))
    }

    @Test
    fun binIndex_clampsOutsideRange() {
        assertEquals(0, RsrpHistogram.binIndexForRsrp(-60))
        assertEquals(11, RsrpHistogram.binIndexForRsrp(-130))
    }

    @Test
    fun buildBins_countsSamplesInRollingWindow() {
        val nowMs = 100_000L
        val samples = listOf(
            RsrpSample(timestampMs = 95_000L, rsrpDbm = -75),
            RsrpSample(timestampMs = 96_000L, rsrpDbm = -85),
            RsrpSample(timestampMs = 97_000L, rsrpDbm = -85),
            RsrpSample(timestampMs = 60_000L, rsrpDbm = -110)
        )

        val bins = RsrpHistogram.buildBins(samples, nowMs, windowMs = 30_000L)

        assertEquals(-75, bins[1].labelDbm)
        assertEquals(1, bins[1].count)
        assertEquals(-85, bins[3].labelDbm)
        assertEquals(2, bins[3].count)
        assertEquals(0, bins[11].count)
        assertEquals(3, RsrpHistogram.totalSamples(samples, nowMs, windowMs = 30_000L))
    }

    @Test
    fun buildBins_respectsCustomWindow() {
        val nowMs = 100_000L
        val samples = listOf(
            RsrpSample(timestampMs = 95_000L, rsrpDbm = -75),
            RsrpSample(timestampMs = 50_000L, rsrpDbm = -90)
        )

        assertEquals(1, RsrpHistogram.totalSamples(samples, nowMs, windowMs = 30_000L))
        assertEquals(2, RsrpHistogram.totalSamples(samples, nowMs, windowMs = 60_000L))
    }

    @Test
    fun activeBins_omitsEmptyBins() {
        val bins = listOf(
            RsrpHistogramBin(labelDbm = -70, count = 0),
            RsrpHistogramBin(labelDbm = -75, count = 2),
            RsrpHistogramBin(labelDbm = -80, count = 0),
            RsrpHistogramBin(labelDbm = -85, count = 1)
        )

        val active = RsrpHistogram.activeBins(bins)

        assertEquals(2, active.size)
        assertEquals(1, active[0].index)
        assertEquals(-75, active[0].value.labelDbm)
        assertEquals(3, active[1].index)
        assertEquals(-85, active[1].value.labelDbm)
    }

    @Test
    fun bandForLabelDbm_mapsFixedThresholds() {
        assertEquals(RsrpHistogramBand.EXCELLENT, RsrpHistogram.bandForLabelDbm(-70))
        assertEquals(RsrpHistogramBand.EXCELLENT, RsrpHistogram.bandForLabelDbm(-79))
        assertEquals(RsrpHistogramBand.GOOD, RsrpHistogram.bandForLabelDbm(-80))
        assertEquals(RsrpHistogramBand.GOOD, RsrpHistogram.bandForLabelDbm(-95))
        assertEquals(RsrpHistogramBand.FAIR, RsrpHistogram.bandForLabelDbm(-100))
        assertEquals(RsrpHistogramBand.FAIR, RsrpHistogram.bandForLabelDbm(-110))
        assertEquals(RsrpHistogramBand.POOR, RsrpHistogram.bandForLabelDbm(-115))
        assertEquals(RsrpHistogramBand.POOR, RsrpHistogram.bandForLabelDbm(-120))
        assertEquals(RsrpHistogramBand.CRITICAL, RsrpHistogram.bandForLabelDbm(-125))
    }

    @Test
    fun buildThresholdBins_countsSamplesStrictlyStrongerThanEachThreshold() {
        val nowMs = 100_000L
        val samples = listOf(
            RsrpSample(timestampMs = 95_000L, rsrpDbm = -90),
            RsrpSample(timestampMs = 96_000L, rsrpDbm = -100),
            RsrpSample(timestampMs = 97_000L, rsrpDbm = -110),
            RsrpSample(timestampMs = 98_000L, rsrpDbm = -95),
            RsrpSample(timestampMs = 99_000L, rsrpDbm = null),
            RsrpSample(timestampMs = 50_000L, rsrpDbm = -80)
        )

        val bins = RsrpHistogram.buildThresholdBins(
            samples = samples,
            nowMs = nowMs,
            windowMs = 30_000L,
            thresholdsDbm = listOf(-95, -105, -115)
        )

        assertEquals(4, bins.size)
        assertEquals(-95, bins[0].labelDbm)
        assertEquals(1, bins[0].count)
        assertEquals(-105, bins[1].labelDbm)
        assertEquals(3, bins[1].count)
        assertEquals(-115, bins[2].labelDbm)
        assertEquals(4, bins[2].count)
        assertEquals(null, bins[3].labelDbm)
        assertEquals(1, bins[3].count)
    }

    @Test
    fun buildThresholdBins_equalToThresholdDoesNotCount() {
        val nowMs = 10_000L
        val samples = listOf(
            RsrpSample(timestampMs = 9_000L, rsrpDbm = -105)
        )

        val bins = RsrpHistogram.buildThresholdBins(
            samples,
            nowMs,
            windowMs = 30_000L,
            thresholdsDbm = listOf(-105)
        )

        assertEquals(0, bins[0].count)
        assertEquals(null, bins[1].labelDbm)
        assertEquals(0, bins[1].count)
    }

    @Test
    fun buildThresholdBins_nullSamplesOnlyGoInTrailingBin() {
        val nowMs = 10_000L
        val samples = listOf(
            RsrpSample(timestampMs = 9_000L, rsrpDbm = null),
            RsrpSample(timestampMs = 8_500L, rsrpDbm = null)
        )

        val bins = RsrpHistogram.buildThresholdBins(
            samples,
            nowMs,
            windowMs = 30_000L,
            thresholdsDbm = listOf(-95, -105, -115)
        )

        assertEquals(listOf(0, 0, 0, 2), bins.map { it.count })
        assertEquals(null, bins.last().labelDbm)
    }
}
