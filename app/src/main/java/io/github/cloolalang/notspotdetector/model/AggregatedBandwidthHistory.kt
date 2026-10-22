package io.github.cloolalang.notspotdetector.model

/**
 * Sum of primary LTE, every secondary LTE carrier, and every NR carrier.
 * The tape shows that total. No signal, and a camp with no bandwidth, are grey.
 * 5 MHz is red, 10 yellow, 15 orange, and up to 30 green. Above 30 MHz the blue
 * deepens as the total grows.
 */
object AggregatedBandwidthHistory {
    const val VISIBLE = 12

    fun current(stats: ConnectivityStats): AggregatedBandwidthVisit? {
        if (!stats.isMonitoring) return null
        if (TechnologyHistory.isNoSignalPeriod(stats)) {
            return AggregatedBandwidthVisit(band = AggregatedBandwidthBand.NONE)
        }
        val khz = stats.aggregatedBandwidthKhz ?: return AggregatedBandwidthVisit(band = AggregatedBandwidthBand.NONE)
        return AggregatedBandwidthVisit(band = bandFor(khz), khz = khz)
    }

    fun bandFor(khz: Int): AggregatedBandwidthBand {
        return when {
            khz <= 5_000 -> AggregatedBandwidthBand.MHZ_5
            khz <= 10_000 -> AggregatedBandwidthBand.MHZ_10
            khz <= 15_000 -> AggregatedBandwidthBand.MHZ_15
            khz <= 30_000 -> AggregatedBandwidthBand.MHZ_20
            khz <= 40_000 -> AggregatedBandwidthBand.BLUE_40
            khz <= 60_000 -> AggregatedBandwidthBand.BLUE_60
            khz <= 100_000 -> AggregatedBandwidthBand.BLUE_100
            khz <= 150_000 -> AggregatedBandwidthBand.BLUE_150
            else -> AggregatedBandwidthBand.BLUE_WIDE
        }
    }

    fun record(
        tape: AggregatedBandwidthTape,
        next: AggregatedBandwidthVisit?,
        nowMs: Long = 0L
    ): AggregatedBandwidthTape {
        if (next == null) return tape
        val last = tape.entries.lastOrNull()
        if (last != null && last.band == next.band) {
            if (last.khz == next.khz) return tape
            return tape.copy(entries = tape.entries.dropLast(1) + last.copy(khz = next.khz))
        }
        val entry = AggregatedBandwidthSegment(band = next.band, khz = next.khz, startedAtMs = nowMs)
        return tape.copy(
            entries = TapeTimeline.retainForScale(tape.entries + entry, nowMs) { it.startedAtMs }
        )
    }

    fun label(visit: AggregatedBandwidthVisit): String = label(visit.band, visit.khz)

    fun label(segment: AggregatedBandwidthSegment): String = label(segment.band, segment.khz)

    private fun label(band: AggregatedBandwidthBand, khz: Int?): String {
        if (band == AggregatedBandwidthBand.NONE || khz == null) return TechnologyHistory.LABEL_NO_SIGNAL
        return CarrierBandwidth.formatMhz(khz)
    }
}

enum class AggregatedBandwidthBand {
    NONE,
    MHZ_5,
    MHZ_10,
    MHZ_15,
    MHZ_20,
    BLUE_40,
    BLUE_60,
    BLUE_100,
    BLUE_150,
    BLUE_WIDE
}

data class AggregatedBandwidthVisit(
    val band: AggregatedBandwidthBand,
    val khz: Int? = null
)

data class AggregatedBandwidthSegment(
    val band: AggregatedBandwidthBand,
    val khz: Int? = null,
    val startedAtMs: Long = 0L
)

data class AggregatedBandwidthTape(
    val entries: List<AggregatedBandwidthSegment> = emptyList()
)
