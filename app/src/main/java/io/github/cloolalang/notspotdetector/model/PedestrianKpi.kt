package io.github.cloolalang.notspotdetector.model

import io.github.cloolalang.notspotdetector.network.CellularSignalReader
import kotlin.math.abs

/**
 * Which zone-test tape colours the line between pedestrian marks.
 * Each phone keeps its own choice. The colours match that tape.
 */
enum class PedestrianKpi {
    RSRP,
    RSRQ,
    EARFCN_PCI,
    TECHNOLOGY,
    DISTANCE,
    INTRACELL,
    BANDWIDTH,
    ACTIVE_TEST,
    CALL;

    /** Written into the saved file name. No slash, so the name stays a single token. */
    val fileToken: String
        get() = when (this) {
            RSRP -> "RSRP"
            RSRQ -> "RSRQ"
            EARFCN_PCI -> "EARFCN-PCI"
            TECHNOLOGY -> "Technology"
            DISTANCE -> "Distance"
            INTRACELL -> "Dominance"
            BANDWIDTH -> "Bandwidth"
            ACTIVE_TEST -> "Active"
            CALL -> "Call"
        }

    companion object {
        fun fromStoredName(name: String?): PedestrianKpi {
            return entries.firstOrNull { it.name == name } ?: RSRP
        }
    }
}

/** The zone-test tapes a pedestrian leg can be coloured from. */
data class PedestrianTapes(
    val rsrp: RsrpBinTape = RsrpBinTape(),
    val rsrq: RsrqBinTape = RsrqBinTape(),
    val carrier: PrimaryCarrierTape = PrimaryCarrierTape(),
    val technology: TechnologyTape = TechnologyTape(),
    val distance: DistanceTape = DistanceTape(),
    val intracell: IntracellDominanceTape = IntracellDominanceTape(),
    val bandwidth: AggregatedBandwidthTape = AggregatedBandwidthTape(),
    val activeTest: ActiveTestTape = ActiveTestTape(),
    val call: CallResultTape = CallResultTape()
)

object PedestrianKpiColors {
    const val NO_SIGNAL = 0xFF90A4AEL
    const val RED = 0xFFFF1744L
    const val ORANGE = 0xFFFF9800L
    const val YELLOW = 0xFFFFEB3BL
    const val GREEN = 0xFF66BB6AL
    const val LIGHT_BLUE = 0xFF81D4FAL
    const val BLACK = 0xFF000000L

    fun colorAt(kpi: PedestrianKpi, tapes: PedestrianTapes, timeMs: Long): Long {
        return when (kpi) {
            PedestrianKpi.RSRP -> WalkRoute.rsrpRouteColor(latest(tapes.rsrp.entries, timeMs) { it.startedAtMs }?.bin)
            PedestrianKpi.RSRQ -> WalkRoute.rsrqRouteColor(latest(tapes.rsrq.entries, timeMs) { it.startedAtMs }?.bin)
            PedestrianKpi.EARFCN_PCI -> {
                val entry = latest(tapes.carrier.entries, timeMs) { it.startedAtMs }
                if (entry == null) NO_SIGNAL else PrimaryCarrierHistory.colorArgb(entry)
            }
            PedestrianKpi.TECHNOLOGY -> technologyColor(
                latest(tapes.technology.entries, timeMs) { it.startedAtMs }?.label
            )
            PedestrianKpi.DISTANCE -> distanceColor(
                latest(tapes.distance.entries, timeMs) { it.startedAtMs }?.metres
            )
            PedestrianKpi.INTRACELL -> intracellColor(
                latest(tapes.intracell.entries, timeMs) { it.startedAtMs }?.gapDb
            )
            PedestrianKpi.BANDWIDTH -> bandwidthColor(
                latest(tapes.bandwidth.entries, timeMs) { it.startedAtMs }?.band
            )
            PedestrianKpi.ACTIVE_TEST -> activeColor(
                latest(tapes.activeTest.entries, timeMs) { it.startedAtMs }?.band
            )
            PedestrianKpi.CALL -> callColor(
                latest(tapes.call.entries, timeMs) { it.startedAtMs }?.band
            )
        }
    }

    fun changeTimes(kpi: PedestrianKpi, tapes: PedestrianTapes): List<Long> {
        return when (kpi) {
            PedestrianKpi.RSRP -> tapes.rsrp.entries.map { it.startedAtMs }
            PedestrianKpi.RSRQ -> tapes.rsrq.entries.map { it.startedAtMs }
            PedestrianKpi.EARFCN_PCI -> tapes.carrier.entries.map { it.startedAtMs }
            PedestrianKpi.TECHNOLOGY -> tapes.technology.entries.map { it.startedAtMs }
            PedestrianKpi.DISTANCE -> tapes.distance.entries.map { it.startedAtMs }
            PedestrianKpi.INTRACELL -> tapes.intracell.entries.map { it.startedAtMs }
            PedestrianKpi.BANDWIDTH -> tapes.bandwidth.entries.map { it.startedAtMs }
            PedestrianKpi.ACTIVE_TEST -> tapes.activeTest.entries.map { it.startedAtMs }
            PedestrianKpi.CALL -> tapes.call.entries.map { it.startedAtMs }
        }
    }

    /** One colour per second-leg, split again where the selected tape changes. */
    fun legs(pins: List<WalkPin>, kpi: PedestrianKpi, tapes: PedestrianTapes): List<ColoredRouteSpan> {
        return WalkRoute.coloredSpans(pins, changeTimes(kpi, tapes)) { atMs ->
            colorAt(kpi, tapes, atMs)
        }
    }

    fun distanceColor(metres: Int?): Long {
        return when {
            metres == null -> NO_SIGNAL
            metres < 500 -> LIGHT_BLUE
            metres < 1_000 -> GREEN
            metres < 3_000 -> YELLOW
            else -> RED
        }
    }

    fun intracellColor(gapDb: Int?): Long {
        return when (gapDb?.let(::primaryLayerDominance)) {
            null -> NO_SIGNAL
            PrimaryLayerDominance.LOW -> RED
            PrimaryLayerDominance.HIGH -> GREEN
        }
    }

    fun bandwidthColor(band: AggregatedBandwidthBand?): Long {
        return when (band) {
            null, AggregatedBandwidthBand.NONE -> NO_SIGNAL
            AggregatedBandwidthBand.MHZ_5 -> RED
            AggregatedBandwidthBand.MHZ_10 -> YELLOW
            AggregatedBandwidthBand.MHZ_15 -> ORANGE
            AggregatedBandwidthBand.MHZ_20 -> GREEN
            AggregatedBandwidthBand.BLUE_40 -> 0xFFB3E5FCL
            AggregatedBandwidthBand.BLUE_60 -> LIGHT_BLUE
            AggregatedBandwidthBand.BLUE_100 -> 0xFF29B6F6L
            AggregatedBandwidthBand.BLUE_150 -> 0xFF0288D1L
            AggregatedBandwidthBand.BLUE_WIDE -> 0xFF01579BL
        }
    }

    fun activeColor(band: ActiveTestBand?): Long {
        return when (band) {
            null, ActiveTestBand.NO_MEASUREMENT -> NO_SIGNAL
            ActiveTestBand.TOTAL_LOSS -> BLACK
            ActiveTestBand.HIGH_LOSS, ActiveTestBand.VERY_SLOW -> RED
            ActiveTestBand.SLOW -> YELLOW
            ActiveTestBand.OK -> GREEN
            ActiveTestBand.FAST -> LIGHT_BLUE
        }
    }

    fun callColor(band: CallResultBand?): Long {
        return when (band) {
            null -> NO_SIGNAL
            CallResultBand.DIALING -> YELLOW
            CallResultBand.UP -> LIGHT_BLUE
            CallResultBand.SUCCESS -> GREEN
            CallResultBand.FAIL -> RED
        }
    }

    fun technologyColor(label: String?): Long {
        if (label == null || label == TechnologyHistory.LABEL_NO_SIGNAL) return NO_SIGNAL
        val hue = when (label) {
            CellularSignalReader.RADIO_2G -> 28f
            CellularSignalReader.RADIO_4G -> 215f
            TechnologyHistory.LABEL_4G_PLUS -> 195f
            CellularSignalReader.RADIO_5G -> 278f
            CellularSignalReader.RADIO_5G_ENDC -> 168f
            else -> return 0xFF546E7AL
        }
        return hsvToArgb(hue, 0.72f, 0.55f)
    }

    private fun <T> latest(items: List<T>, timeMs: Long, startedAt: (T) -> Long): T? {
        if (items.isEmpty()) return null
        val ordered = items.sortedBy(startedAt)
        val index = ordered.indexOfLast { startedAt(it) <= timeMs }
        if (index < 0) return null
        return ordered[index]
    }

    private fun hsvToArgb(hue: Float, saturation: Float, value: Float): Long {
        val h = ((hue % 360f) + 360f) % 360f
        val c = value * saturation
        val x = c * (1f - abs((h / 60f) % 2f - 1f))
        val m = value - c
        val (r1, g1, b1) = when {
            h < 60f -> Triple(c, x, 0f)
            h < 120f -> Triple(x, c, 0f)
            h < 180f -> Triple(0f, c, x)
            h < 240f -> Triple(0f, x, c)
            h < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        val r = ((r1 + m) * 255f).toInt().coerceIn(0, 255)
        val g = ((g1 + m) * 255f).toInt().coerceIn(0, 255)
        val b = ((b1 + m) * 255f).toInt().coerceIn(0, 255)
        return 0xFF000000L or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong()
    }
}
