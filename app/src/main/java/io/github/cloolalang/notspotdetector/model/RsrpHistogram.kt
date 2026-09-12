package io.github.cloolalang.notspotdetector.model

import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** What a histogram bar represents. */
enum class RsrpHistogramBinKind {
    /** A 5 dB level bin or a “stronger than” threshold floor. */
    SIGNAL,
    /** Measured RSRP that is not stronger than any configured threshold. */
    OTHER,
    /** Sample recorded with no RSRP (no signal). */
    NO_SIGNAL
}

/**
 * A single histogram bar. [labelDbm] is the bin edge or threshold for [RsrpHistogramBinKind.SIGNAL],
 * and null for [RsrpHistogramBinKind.OTHER] / [RsrpHistogramBinKind.NO_SIGNAL].
 */
data class RsrpHistogramBin(
    val labelDbm: Int?,
    val count: Int,
    val kind: RsrpHistogramBinKind = RsrpHistogramBinKind.SIGNAL
)

/** Fixed RSRP bands for histogram bar colouring (independent of passive tier settings). */
enum class RsrpHistogramBand {
    EXCELLENT,
    GOOD,
    FAIR,
    POOR,
    CRITICAL
}

/** Occupancy colour for a threshold-histogram bar. */
enum class RsrpHistogramThresholdBarColor {
    GREEN,
    ORANGE,
    YELLOW,
    RED
}

/** Mean, median, and sample standard deviation of in-window measured RSRP values. */
data class RsrpWindowStats(
    val meanDbm: Double,
    val medianDbm: Double,
    val stdevDbm: Double,
    val sampleCount: Int
)

/** Which histogram is shown — only one mode is visible at a time. */
enum class RsrpHistogramBinningMode {
    /** Existing 5 dB level bins from −70 to −126 dBm. */
    LEVEL,
    /** Three “stronger than” floors plus other-samples and no-signal (N/A) bars. */
    THRESHOLD;

    companion object {
        val DEFAULT = THRESHOLD

        fun fromStoredName(name: String?): RsrpHistogramBinningMode {
            if (name.isNullOrBlank()) return DEFAULT
            return entries.find { it.name == name } ?: DEFAULT
        }
    }
}

object RsrpHistogram {
    const val MIN_RSRP_DBM = -126
    const val MAX_RSRP_DBM = -70
    const val BIN_SIZE_DB = 5
    const val DEFAULT_WINDOW_MS = 300_000L
    /** Histogram occupancy sample rate while monitoring (one last-known RSRP per tick). */
    const val SAMPLE_INTERVAL_MS = 1_000L
    const val BIN_COUNT = (MAX_RSRP_DBM - MIN_RSRP_DBM + BIN_SIZE_DB - 1) / BIN_SIZE_DB

    const val DEFAULT_THRESHOLD_1_DBM = -95
    const val DEFAULT_THRESHOLD_2_DBM = -105
    const val DEFAULT_THRESHOLD_3_DBM = -115
    const val MIN_THRESHOLD_DBM = MIN_RSRP_DBM
    const val MAX_THRESHOLD_DBM = MAX_RSRP_DBM
    const val THRESHOLD_BIN_COUNT = 3
    const val THRESHOLD_GREEN_MIN_PERCENT = 95
    const val THRESHOLD_ORANGE_MIN_PERCENT = 90
    /** Occupancy below this is compressed into the bottom of the threshold-bar scale. */
    const val THRESHOLD_BAR_PERCENT_FLOOR = 80
    /** Share of bar height reserved for 0–[THRESHOLD_BAR_PERCENT_FLOOR]% occupancy. */
    const val THRESHOLD_BAR_LOW_SPAN = 0.15f

    fun binIndexForRsrp(rsrpDbm: Int): Int {
        val clamped = rsrpDbm.coerceIn(MIN_RSRP_DBM, MAX_RSRP_DBM)
        val distanceFromStrongest = MAX_RSRP_DBM - clamped
        return (distanceFromStrongest / BIN_SIZE_DB).coerceIn(0, BIN_COUNT - 1)
    }

    fun labelDbmForBin(index: Int): Int {
        return MAX_RSRP_DBM - index * BIN_SIZE_DB
    }

    /**
     * Builds the signal-strength bins plus a trailing "no signal" bin (`labelDbm = null`) for
     * samples recorded with a null RSRP (e.g. during a no-signal state) — those are binned
     * alongside the normal samples rather than being excluded from the histogram entirely.
     */
    fun buildBins(
        samples: List<RsrpSample>,
        nowMs: Long,
        windowMs: Long = DEFAULT_WINDOW_MS
    ): List<RsrpHistogramBin> {
        val cutoff = nowMs - windowMs
        val counts = IntArray(BIN_COUNT)
        var nullCount = 0
        for (sample in samples) {
            if (sample.timestampMs < cutoff) continue
            val rsrpDbm = sample.rsrpDbm
            if (rsrpDbm == null) {
                nullCount++
            } else {
                counts[binIndexForRsrp(rsrpDbm)]++
            }
        }
        val signalBins = List(BIN_COUNT) { index ->
            RsrpHistogramBin(
                labelDbm = labelDbmForBin(index),
                count = counts[index]
            )
        }
        return signalBins + RsrpHistogramBin(
            labelDbm = null,
            count = nullCount,
            kind = RsrpHistogramBinKind.NO_SIGNAL
        )
    }

    /**
     * Three cumulative bins, then an “other samples” bar, then a no-signal (N/A) bar.
     * Each threshold bar is the number of in-window samples whose RSRP is **strictly greater
     * than** that floor. Measured RSRP that is not stronger than any floor goes in other.
     * Null / no-signal samples are counted only in the trailing N/A bar.
     */
    fun buildThresholdBins(
        samples: List<RsrpSample>,
        nowMs: Long,
        windowMs: Long,
        thresholdsDbm: List<Int>
    ): List<RsrpHistogramBin> {
        val cutoff = nowMs - windowMs
        val floors = thresholdsDbm.map { it.coerceIn(MIN_THRESHOLD_DBM, MAX_THRESHOLD_DBM) }
        val thresholdCounts = IntArray(floors.size)
        var otherCount = 0
        var nullCount = 0
        for (sample in samples) {
            if (sample.timestampMs < cutoff) continue
            val rsrpDbm = sample.rsrpDbm
            if (rsrpDbm == null) {
                nullCount++
                continue
            }
            var inAnyThreshold = false
            for (index in floors.indices) {
                if (rsrpDbm > floors[index]) {
                    thresholdCounts[index]++
                    inAnyThreshold = true
                }
            }
            if (!inAnyThreshold) {
                otherCount++
            }
        }
        val thresholdBins = floors.mapIndexed { index, floor ->
            RsrpHistogramBin(
                labelDbm = floor,
                count = thresholdCounts[index],
                kind = RsrpHistogramBinKind.SIGNAL
            )
        }
        return thresholdBins +
            RsrpHistogramBin(
                labelDbm = null,
                count = otherCount,
                kind = RsrpHistogramBinKind.OTHER
            ) +
            RsrpHistogramBin(
                labelDbm = null,
                count = nullCount,
                kind = RsrpHistogramBinKind.NO_SIGNAL
            )
    }

    fun totalSamples(
        samples: List<RsrpSample>,
        nowMs: Long,
        windowMs: Long = DEFAULT_WINDOW_MS
    ): Int {
        val cutoff = nowMs - windowMs
        return samples.count { it.timestampMs >= cutoff }
    }

    /**
     * Wall time since the oldest retained sample, capped at [windowMs] so the indicator
     * stops once the sample window is full. Null when the histogram is empty.
     */
    fun elapsedSinceFirstSampleMs(
        samples: List<RsrpSample>,
        nowMs: Long,
        windowMs: Long = Long.MAX_VALUE
    ): Long? {
        val firstMs = samples.minOfOrNull { it.timestampMs } ?: return null
        val elapsed = (nowMs - firstMs).coerceAtLeast(0L)
        return elapsed.coerceAtMost(windowMs.coerceAtLeast(0L))
    }

    fun occupancyPercent(count: Int, totalSamples: Int): Int {
        if (totalSamples <= 0) return 0
        return ((count * 100f) / totalSamples).roundToInt()
    }

    /**
     * Threshold floors: green at ≥95%, orange at 90–94%, yellow below 90%.
     * Other-samples and N/A bars are always red.
     */
    fun thresholdBarColor(
        bin: RsrpHistogramBin,
        totalSamples: Int
    ): RsrpHistogramThresholdBarColor {
        if (bin.kind != RsrpHistogramBinKind.SIGNAL) {
            return RsrpHistogramThresholdBarColor.RED
        }
        val percent = occupancyPercent(bin.count, totalSamples)
        return when {
            percent >= THRESHOLD_GREEN_MIN_PERCENT -> RsrpHistogramThresholdBarColor.GREEN
            percent >= THRESHOLD_ORANGE_MIN_PERCENT -> RsrpHistogramThresholdBarColor.ORANGE
            else -> RsrpHistogramThresholdBarColor.YELLOW
        }
    }

    /**
     * Maps occupancy percent to a bar fill. 0–80% is compressed into the bottom 15% of
     * the column so 80–100% (the range that matters for threshold floors) uses most of
     * the height.
     */
    fun thresholdBarFillFraction(percent: Int): Float {
        val p = percent.coerceIn(0, 100)
        val floor = THRESHOLD_BAR_PERCENT_FLOOR
        val lowSpan = THRESHOLD_BAR_LOW_SPAN
        if (p <= floor) {
            return (p / floor.toFloat()) * lowSpan
        }
        return lowSpan + ((p - floor) / (100f - floor)) * (1f - lowSpan)
    }

    /**
     * Mean, median, and sample standard deviation of in-window non-null RSRP samples.
     * Null when the window has no measured RSRP.
     */
    fun windowStats(
        samples: List<RsrpSample>,
        nowMs: Long,
        windowMs: Long = DEFAULT_WINDOW_MS
    ): RsrpWindowStats? {
        val cutoff = nowMs - windowMs
        val values = samples.mapNotNull { sample ->
            if (sample.timestampMs < cutoff) null else sample.rsrpDbm
        }
        if (values.isEmpty()) return null
        val mean = values.average()
        val sorted = values.sorted()
        val mid = sorted.size / 2
        val median = if (sorted.size % 2 == 1) {
            sorted[mid].toDouble()
        } else {
            (sorted[mid - 1] + sorted[mid]) / 2.0
        }
        val stdev = if (values.size == 1) {
            0.0
        } else {
            val variance = values.sumOf { value ->
                val delta = value - mean
                delta * delta
            } / (values.size - 1)
            sqrt(variance)
        }
        return RsrpWindowStats(
            meanDbm = mean,
            medianDbm = median,
            stdevDbm = stdev,
            sampleCount = values.size
        )
    }

    fun formatWindowStat(value: Double): String {
        val rounded = (value * 10.0).roundToInt() / 10.0
        return if (rounded == rounded.toLong().toDouble()) {
            rounded.toLong().toString()
        } else {
            String.format(Locale.US, "%.1f", rounded)
        }
    }

    fun activeBins(bins: List<RsrpHistogramBin>): List<IndexedValue<RsrpHistogramBin>> {
        return bins.withIndex()
            .filter { (_, bin) -> bin.count > 0 }
            .map { (index, bin) -> IndexedValue(index, bin) }
    }

    /**
     * Maps a bin label (upper edge dBm) to a fixed signal-strength band, or null for the
     * "no signal" bin (see [buildBins]) — callers should render that bin in a neutral/grey
     * color rather than one of the signal-strength bands.
     */
    fun bandForLabelDbm(labelDbm: Int?): RsrpHistogramBand? {
        if (labelDbm == null) return null
        return when {
            labelDbm > -80 -> RsrpHistogramBand.EXCELLENT
            labelDbm >= -95 -> RsrpHistogramBand.GOOD
            labelDbm >= -110 -> RsrpHistogramBand.FAIR
            labelDbm >= -120 -> RsrpHistogramBand.POOR
            else -> RsrpHistogramBand.CRITICAL
        }
    }
}
