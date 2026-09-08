package io.github.cloolalang.notspotdetector.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import io.github.cloolalang.notspotdetector.model.SignalMeasurementTier
import io.github.cloolalang.notspotdetector.model.SignalStrengthTier

object SignalTierColors {
    val tier1 = Color(0xFF1976D2)
    val tier2 = Color(0xFF1B5E20)
    val tier3 = Color(0xFF66BB6A)
    val tier4 = Color(0xFFFFB300)
    val tier5 = Color(0xFFFF9800)
    val tier6 = Color(0xFFE53935)
    val tier7 = Color(0xFF66BB6A)
    val tier8 = Color(0xFFFF9800)
    val tier9 = Color(0xFF880E4F)
    val noSignal = Color(0xFFB71C1C)

    fun forTierNumber(number: Int): Color {
        return when (number) {
            1 -> tier1
            2 -> tier2
            3 -> tier3
            4 -> tier4
            5 -> tier5
            6 -> tier6
            7 -> tier7
            8 -> tier8
            9 -> tier9
            else -> tier6
        }
    }

    fun forStrengthTier(tier: SignalStrengthTier): Color = forTierNumber(tier.displayNumber)

    @Composable
    fun forMeasurementTier(tier: SignalMeasurementTier): Color {
        return when (tier) {
            SignalMeasurementTier.VERY_STRONG -> tier1
            SignalMeasurementTier.MILD -> tier2
            SignalMeasurementTier.GOOD -> tier3
            SignalMeasurementTier.FAIR -> tier4
            SignalMeasurementTier.POOR -> tier5
            SignalMeasurementTier.CRITICAL -> tier6
            SignalMeasurementTier.G2_STRONG -> tier7
            SignalMeasurementTier.G2_WEAK -> tier8
            SignalMeasurementTier.DEADZONE -> tier9
            SignalMeasurementTier.NO_SIGNAL -> noSignal
            SignalMeasurementTier.LIMITED_SERVICE -> tier5
            SignalMeasurementTier.PERMISSION_REQUIRED,
            SignalMeasurementTier.UNAVAILABLE -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    }
}
