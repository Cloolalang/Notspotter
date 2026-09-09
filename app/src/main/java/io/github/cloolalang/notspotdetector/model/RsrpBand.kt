package io.github.cloolalang.notspotdetector.model

/**
 * RSRP level bands A–D (highest to lowest usable signal), mapped from RXSS 2–5.
 * RXSS 1 (very strong) and RXSS 6 (critical) are outside this band ladder.
 */
enum class RsrpBand(val letter: Char) {
    A('A'),
    B('B'),
    C('C'),
    D('D');

    val rxssNumber: Int
        get() = when (this) {
            A -> SignalStrengthTier.MILD.displayNumber
            B -> SignalStrengthTier.GOOD.displayNumber
            C -> SignalStrengthTier.FAIR.displayNumber
            D -> SignalStrengthTier.POOR.displayNumber
        }

    val levelRangeName: String
        get() = "RXSS $rxssNumber"

    companion object {
        fun fromRxssNumber(number: Int): RsrpBand? = when (number) {
            SignalStrengthTier.MILD.displayNumber -> A
            SignalStrengthTier.GOOD.displayNumber -> B
            SignalStrengthTier.FAIR.displayNumber -> C
            SignalStrengthTier.POOR.displayNumber -> D
            else -> null
        }

        fun fromMeasurementTier(tier: SignalMeasurementTier): RsrpBand? = when (tier) {
            SignalMeasurementTier.MILD -> A
            SignalMeasurementTier.GOOD -> B
            SignalMeasurementTier.FAIR -> C
            SignalMeasurementTier.POOR -> D
            else -> null
        }
    }
}
