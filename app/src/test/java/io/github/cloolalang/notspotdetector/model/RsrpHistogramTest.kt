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

        assertEquals(5, bins.size)
        assertEquals(-95, bins[0].labelDbm)
        assertEquals(1, bins[0].count)
        assertEquals(-105, bins[1].labelDbm)
        assertEquals(3, bins[1].count)
        assertEquals(-115, bins[2].labelDbm)
        assertEquals(4, bins[2].count)
        assertEquals(RsrpHistogramBinKind.OTHER, bins[3].kind)
        assertEquals(0, bins[3].count)
        assertEquals(RsrpHistogramBinKind.NO_SIGNAL, bins[4].kind)
        assertEquals(1, bins[4].count)
    }

    @Test
    fun buildThresholdBins_equalToThresholdGoesInOtherSamples() {
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
        assertEquals(RsrpHistogramBinKind.OTHER, bins[1].kind)
        assertEquals(1, bins[1].count)
        assertEquals(RsrpHistogramBinKind.NO_SIGNAL, bins[2].kind)
        assertEquals(0, bins[2].count)
    }

    @Test
    fun buildThresholdBins_weakMeasuredSamplesGoInOther() {
        val nowMs = 10_000L
        val samples = listOf(
            RsrpSample(timestampMs = 9_000L, rsrpDbm = -120),
            RsrpSample(timestampMs = 8_500L, rsrpDbm = -126),
            RsrpSample(timestampMs = 8_000L, rsrpDbm = -90)
        )

        val bins = RsrpHistogram.buildThresholdBins(
            samples,
            nowMs,
            windowMs = 30_000L,
            thresholdsDbm = listOf(-95, -105, -115)
        )

        assertEquals(1, bins[0].count)
        assertEquals(1, bins[1].count)
        assertEquals(1, bins[2].count)
        assertEquals(RsrpHistogramBinKind.OTHER, bins[3].kind)
        assertEquals(2, bins[3].count)
        assertEquals(0, bins[4].count)
    }

    @Test
    fun elapsedSinceFirstSample_isNullWhenEmpty() {
        assertEquals(null, RsrpHistogram.elapsedSinceFirstSampleMs(emptyList(), nowMs = 10_000L))
    }

    @Test
    fun elapsedSinceFirstSample_usesOldestTimestamp() {
        val samples = listOf(
            RsrpSample(timestampMs = 8_000L, rsrpDbm = -90),
            RsrpSample(timestampMs = 4_000L, rsrpDbm = -100),
            RsrpSample(timestampMs = 9_000L, rsrpDbm = null)
        )
        assertEquals(6_000L, RsrpHistogram.elapsedSinceFirstSampleMs(samples, nowMs = 10_000L))
    }

    @Test
    fun elapsedSinceFirstSample_doesNotGoNegative() {
        val samples = listOf(RsrpSample(timestampMs = 12_000L, rsrpDbm = -80))
        assertEquals(0L, RsrpHistogram.elapsedSinceFirstSampleMs(samples, nowMs = 10_000L))
    }

    @Test
    fun elapsedSinceFirstSample_stopsAtSampleWindow() {
        val samples = listOf(RsrpSample(timestampMs = 0L, rsrpDbm = -90))
        assertEquals(
            30_000L,
            RsrpHistogram.elapsedSinceFirstSampleMs(
                samples,
                nowMs = 80_000L,
                windowMs = 30_000L
            )
        )
    }

    @Test
    fun thresholdBarColor_usesOccupancyBands() {
        val threshold = RsrpHistogramBin(labelDbm = -95, count = 95, kind = RsrpHistogramBinKind.SIGNAL)
        assertEquals(
            RsrpHistogramThresholdBarColor.GREEN,
            RsrpHistogram.thresholdBarColor(threshold.copy(count = 95), totalSamples = 100)
        )
        assertEquals(
            RsrpHistogramThresholdBarColor.ORANGE,
            RsrpHistogram.thresholdBarColor(threshold.copy(count = 94), totalSamples = 100)
        )
        assertEquals(
            RsrpHistogramThresholdBarColor.ORANGE,
            RsrpHistogram.thresholdBarColor(threshold.copy(count = 90), totalSamples = 100)
        )
        assertEquals(
            RsrpHistogramThresholdBarColor.YELLOW,
            RsrpHistogram.thresholdBarColor(threshold.copy(count = 89), totalSamples = 100)
        )
    }

    @Test
    fun thresholdBarColor_otherAndNaAreAlwaysRed() {
        assertEquals(
            RsrpHistogramThresholdBarColor.RED,
            RsrpHistogram.thresholdBarColor(
                RsrpHistogramBin(labelDbm = null, count = 100, kind = RsrpHistogramBinKind.OTHER),
                totalSamples = 100
            )
        )
        assertEquals(
            RsrpHistogramThresholdBarColor.RED,
            RsrpHistogram.thresholdBarColor(
                RsrpHistogramBin(labelDbm = null, count = 1, kind = RsrpHistogramBinKind.NO_SIGNAL),
                totalSamples = 100
            )
        )
    }

    @Test
    fun thresholdBarFillFraction_compressesBelowEightyPercent() {
        assertEquals(0f, RsrpHistogram.thresholdBarFillFraction(0), 0.001f)
        assertEquals(0.15f, RsrpHistogram.thresholdBarFillFraction(80), 0.001f)
        assertEquals(1f, RsrpHistogram.thresholdBarFillFraction(100), 0.001f)
        val mid = RsrpHistogram.thresholdBarFillFraction(90)
        assertEquals(0.575f, mid, 0.001f)
    }

    @Test
    fun windowStats_computesMeanMedianAndStdev() {
        val nowMs = 10_000L
        val samples = listOf(
            RsrpSample(timestampMs = 9_000L, rsrpDbm = -100),
            RsrpSample(timestampMs = 9_100L, rsrpDbm = -102),
            RsrpSample(timestampMs = 9_200L, rsrpDbm = -104),
            RsrpSample(timestampMs = 9_300L, rsrpDbm = null),
            RsrpSample(timestampMs = 1_000L, rsrpDbm = -70)
        )
        val stats = RsrpHistogram.windowStats(samples, nowMs, windowMs = 2_000L)
        assertEquals(3, stats?.sampleCount)
        assertEquals(-102.0, stats?.meanDbm ?: 0.0, 0.001)
        assertEquals(-102.0, stats?.medianDbm ?: 0.0, 0.001)
        assertEquals(2.0, stats?.stdevDbm ?: 0.0, 0.001)
    }

    @Test
    fun formatWindowStat_dropsTrailingZero() {
        assertEquals("-102", RsrpHistogram.formatWindowStat(-102.0))
        assertEquals("-102.4", RsrpHistogram.formatWindowStat(-102.36))
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

        assertEquals(listOf(0, 0, 0, 0, 2), bins.map { it.count })
        assertEquals(RsrpHistogramBinKind.OTHER, bins[3].kind)
        assertEquals(RsrpHistogramBinKind.NO_SIGNAL, bins.last().kind)
    }
}
