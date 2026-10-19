package io.github.cloolalang.notspotdetector.model

import kotlin.math.roundToInt

/**
 * Serving-cell timing advance. LTE and GSM values are the 3GPP index. NR is the
 * microsecond reading from [android.telephony.CellSignalStrengthNr.getTimingAdvanceMicros].
 * [oneWayMetres] is the approximate distance from the tower.
 */
data class TimingAdvanceReading(
    val value: Int,
    val kind: TimingAdvanceKind
) {
    fun oneWayMetres(): Int {
        val metres = when (kind) {
            TimingAdvanceKind.LTE -> value * LTE_METRES_PER_INDEX
            TimingAdvanceKind.GSM -> value * GSM_METRES_PER_INDEX
            TimingAdvanceKind.NR_MICROS -> value * NR_METRES_PER_MICROSECOND
        }
        return metres.roundToInt()
    }

    /** Index or microseconds, then the approximate one-way distance. */
    fun formatDisplay(): String {
        val reading = when (kind) {
            TimingAdvanceKind.NR_MICROS -> "$value μs"
            else -> value.toString()
        }
        return "$reading · ${distanceLabel(oneWayMetres())}"
    }

    private fun distanceLabel(metres: Int): String {
        if (metres < 1_000) return "$metres m"
        val wholeKm = metres / 1_000
        if (wholeKm >= 10) return "$wholeKm km"
        var tenths = ((metres % 1_000) + 50) / 100
        var km = wholeKm
        if (tenths == 10) {
            km += 1
            tenths = 0
        }
        return "$km.$tenths km"
    }

    private companion object {
        const val LTE_METRES_PER_INDEX = 78.125
        const val GSM_METRES_PER_INDEX = 553.85
        const val NR_METRES_PER_MICROSECOND = 150.0
    }
}

enum class TimingAdvanceKind {
    LTE,
    GSM,
    NR_MICROS
}

object TimingAdvance {
    const val LTE_MAX_INDEX = 1_282
    const val GSM_MAX_INDEX = 219

    fun takeLteIndex(value: Int): TimingAdvanceReading? =
        takeIndex(value, LTE_MAX_INDEX, TimingAdvanceKind.LTE)

    fun takeGsmIndex(value: Int): TimingAdvanceReading? =
        takeIndex(value, GSM_MAX_INDEX, TimingAdvanceKind.GSM)

    fun takeNrMicros(value: Int): TimingAdvanceReading? {
        if (value < 0 || value == Int.MAX_VALUE || value > 2_000) return null
        return TimingAdvanceReading(value, TimingAdvanceKind.NR_MICROS)
    }

    private fun takeIndex(value: Int, max: Int, kind: TimingAdvanceKind): TimingAdvanceReading? {
        if (value !in 0..max) return null
        return TimingAdvanceReading(value, kind)
    }
}
