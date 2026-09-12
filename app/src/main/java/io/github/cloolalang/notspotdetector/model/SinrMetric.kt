package io.github.cloolalang.notspotdetector.model

import kotlin.math.round

/**
 * Valid ranges for public Android SINR / SNIR getters.
 * LTE [android.telephony.CellSignalStrengthLte.getRssnr] is documented as −20…+30 dB.
 * NR [android.telephony.CellSignalStrengthNr.getSsSinr] is documented as −23…+40 dB.
 *
 * Many Qualcomm / Samsung HALs still report tenths of a dB (for example 126 → 12.6 dB).
 * Those values sit outside the documented integer range and were previously dropped, so SNIR
 * showed “—”. Convert tenths when the raw reading is outside the integer range but inside
 * the 10× window.
 */
object SinrMetric {
    const val LTE_RSSNR_MIN_DB = -20
    const val LTE_RSSNR_MAX_DB = 30
    const val NR_SS_SINR_MIN_DB = -23
    const val NR_SS_SINR_MAX_DB = 40

    fun takeLteRssnr(value: Int): Int? =
        normalize(value, LTE_RSSNR_MIN_DB, LTE_RSSNR_MAX_DB)

    fun takeNrSsSinr(value: Int): Int? =
        normalize(value, NR_SS_SINR_MIN_DB, NR_SS_SINR_MAX_DB)

    private fun normalize(value: Int, minDb: Int, maxDb: Int): Int? {
        if (value == Int.MAX_VALUE || value == Int.MIN_VALUE) return null
        if (value in minDb..maxDb) return value
        if (value in (minDb * 10)..(maxDb * 10)) {
            return round(value / 10.0).toInt()
        }
        return null
    }
}
