package io.github.cloolalang.notspotdetector.model

import kotlin.math.roundToInt

/**
 * Ping result for the KPI history tape during an active test.
 * A segment holds while the color stays the same, and its label tracks the latest ping.
 * No signal and a test with no ping yet are grey. Total loss is black. Loss above 5% is red.
 * Otherwise the color is the ping time: up to 50 ms blue, 75 ms green, 150 ms yellow, slower red.
 */
object ActiveTestHistory {
    const val VISIBLE = 12
    const val BLUE_MAX_MS = 50L
    const val GREEN_MAX_MS = 75L
    const val YELLOW_MAX_MS = 150L
    const val HIGH_LOSS_PERCENT = 5f

    fun current(stats: ConnectivityStats): ActiveTestVisit? {
        if (!stats.isMonitoring) return null
        if (TechnologyHistory.isNoSignalPeriod(stats) ||
            stats.isPassiveOnlySession ||
            stats.pingsSent <= 0
        ) {
            return ActiveTestVisit(band = ActiveTestBand.NO_MEASUREMENT)
        }
        val loss = stats.packetLossPercent
        if (loss >= 100f) {
            return ActiveTestVisit(
                band = ActiveTestBand.TOTAL_LOSS,
                rttMs = stats.rttMs,
                packetLossPercent = loss
            )
        }
        if (loss > HIGH_LOSS_PERCENT) {
            return ActiveTestVisit(
                band = ActiveTestBand.HIGH_LOSS,
                rttMs = stats.rttMs,
                packetLossPercent = loss
            )
        }
        val rttMs = stats.rttMs ?: return ActiveTestVisit(band = ActiveTestBand.NO_MEASUREMENT)
        val band = when {
            rttMs <= BLUE_MAX_MS -> ActiveTestBand.FAST
            rttMs <= GREEN_MAX_MS -> ActiveTestBand.OK
            rttMs <= YELLOW_MAX_MS -> ActiveTestBand.SLOW
            else -> ActiveTestBand.VERY_SLOW
        }
        return ActiveTestVisit(band = band, rttMs = rttMs, packetLossPercent = loss)
    }

    fun record(tape: ActiveTestTape, next: ActiveTestVisit?, nowMs: Long = 0L): ActiveTestTape {
        if (next == null) return tape
        val last = tape.entries.lastOrNull()
        if (last != null && last.band == next.band) {
            if (last.rttMs == next.rttMs && last.packetLossPercent.roundToInt() == next.packetLossPercent.roundToInt()) {
                return tape
            }
            val updated = last.copy(rttMs = next.rttMs, packetLossPercent = next.packetLossPercent)
            return tape.copy(entries = tape.entries.dropLast(1) + updated)
        }
        val entry = ActiveTestSegment(
            band = next.band,
            rttMs = next.rttMs,
            packetLossPercent = next.packetLossPercent,
            startedAtMs = nowMs
        )
        return tape.copy(
            entries = TapeTimeline.retainForScale(tape.entries + entry, nowMs) { it.startedAtMs }
        )
    }

    fun label(visit: ActiveTestVisit): String = label(visit.band, visit.rttMs, visit.packetLossPercent)

    fun label(segment: ActiveTestSegment): String = label(segment.band, segment.rttMs, segment.packetLossPercent)

    private fun label(band: ActiveTestBand, rttMs: Long?, packetLossPercent: Float): String {
        return when (band) {
            ActiveTestBand.NO_MEASUREMENT -> TechnologyHistory.LABEL_NO_SIGNAL
            ActiveTestBand.TOTAL_LOSS -> "100%"
            ActiveTestBand.HIGH_LOSS -> rttMs?.let { "$it ms" }
                ?: "${packetLossPercent.roundToInt()}%"
            else -> rttMs?.let { "$it ms" } ?: TechnologyHistory.LABEL_NO_SIGNAL
        }
    }
}

enum class ActiveTestBand {
    NO_MEASUREMENT,
    TOTAL_LOSS,
    HIGH_LOSS,
    FAST,
    OK,
    SLOW,
    VERY_SLOW
}

data class ActiveTestVisit(
    val band: ActiveTestBand,
    val rttMs: Long? = null,
    val packetLossPercent: Float = 0f
)

data class ActiveTestSegment(
    val band: ActiveTestBand,
    val rttMs: Long? = null,
    val packetLossPercent: Float = 0f,
    val startedAtMs: Long = 0L
)

data class ActiveTestTape(
    val entries: List<ActiveTestSegment> = emptyList()
)
