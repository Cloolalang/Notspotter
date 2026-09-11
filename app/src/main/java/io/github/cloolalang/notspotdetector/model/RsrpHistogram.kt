package io.github.cloolalang.notspotdetector.model

/**
 * A single histogram bar. [labelDbm] is null for the special "no signal" bin, which tallies
 * [RsrpSample]s whose `rsrpDbm` was null (e.g. recorded during a no-signal state) rather than
 * discarding them — see [RsrpHistogram.buildBins].
 */
data class RsrpHistogramBin(
    val labelDbm: Int?,
    val count: Int
)

/** Fixed RSRP bands for histogram bar colouring (independent of passive tier settings). */
enum class RsrpHistogramBand {
    EXCELLENT,
    GOOD,
    FAIR,
    POOR,
    CRITICAL
}

/** Which histogram is shown — only one mode is visible at a time. */
enum class RsrpHistogramBinningMode {
    /** Existing 5 dB level bins from −70 to −126 dBm. */
    LEVEL,
    /** Three “stronger than” floors plus a trailing no-signal (N/A) bar. */
    THRESHOLD;

    companion object {
        val DEFAULT = LEVEL

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
    const val DEFAULT_WINDOW_MS = 30_000L
    /** Histogram occupancy sample rate while monitoring (one last-known RSRP per tick). */
    const val SAMPLE_INTERVAL_MS = 1_000L
    const val BIN_COUNT = (MAX_RSRP_DBM - MIN_RSRP_DBM + BIN_SIZE_DB - 1) / BIN_SIZE_DB

    const val DEFAULT_THRESHOLD_1_DBM = -95
    const val DEFAULT_THRESHOLD_2_DBM = -105
    const val DEFAULT_THRESHOLD_3_DBM = -115
    const val MIN_THRESHOLD_DBM = MIN_RSRP_DBM
    const val MAX_THRESHOLD_DBM = MAX_RSRP_DBM
    const val THRESHOLD_BIN_COUNT = 3

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
        return signalBins + RsrpHistogramBin(labelDbm = null, count = nullCount)
    }

    /**
     * Three cumulative bins plus a trailing "no signal" bin (`labelDbm = null`).
     * Each threshold bar is the number of in-window samples whose RSRP is **strictly greater
     * than** that floor. Null / no-signal samples are counted only in the trailing bar.
     */
    fun buildThresholdBins(
        samples: List<RsrpSample>,
        nowMs: Long,
        windowMs: Long,
        thresholdsDbm: List<Int>
    ): List<RsrpHistogramBin> {
        val cutoff = nowMs - windowMs
        var nullCount = 0
        val thresholdBins = thresholdsDbm.map { rawThreshold ->
            val threshold = rawThreshold.coerceIn(MIN_THRESHOLD_DBM, MAX_THRESHOLD_DBM)
            var count = 0
            for (sample in samples) {
                if (sample.timestampMs < cutoff) continue
                val rsrpDbm = sample.rsrpDbm
                if (rsrpDbm == null) continue
                if (rsrpDbm > threshold) count++
            }
            RsrpHistogramBin(labelDbm = threshold, count = count)
        }
        for (sample in samples) {
            if (sample.timestampMs < cutoff) continue
            if (sample.rsrpDbm == null) nullCount++
        }
        return thresholdBins + RsrpHistogramBin(labelDbm = null, count = nullCount)
    }

    fun totalSamples(
        samples: List<RsrpSample>,
        nowMs: Long,
        windowMs: Long = DEFAULT_WINDOW_MS
    ): Int {
        val cutoff = nowMs - windowMs
        return samples.count { it.timestampMs >= cutoff }
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
