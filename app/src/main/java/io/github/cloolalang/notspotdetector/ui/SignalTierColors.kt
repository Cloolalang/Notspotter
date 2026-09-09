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
    val tier0 = Color(0xFF880E4F)
    val tier9 = Color(0xFF00838F)
    val tier10 = Color(0xFFB71C1C)
    val tier11 = Color(0xFFC62828)
    val tier12 = Color(0xFFEF6C00)
    val tier13 = Color(0xFFAD1457)
    val tier14 = Color(0xFF5D4037)
    val tier15 = Color(0xFF8E0000)
    val tier28 = Color(0xFF00695C)
    val tier29 = Color(0xFF1565C0)
    val tier30 = Color(0xFF4527A0)
    val noSignal = tier10

    fun forRxssNumber(number: Int): Color = forTierNumber(number)

    fun forTierNumber(number: Int): Color {
        return when (number) {
            0 -> tier0
            1 -> tier1
            2 -> tier2
            3 -> tier3
            4 -> tier4
            5 -> tier5
            6 -> tier6
            7 -> tier7
            8 -> tier8
            9 -> tier9
            10 -> tier10
            11 -> tier11
            12 -> tier12
            13 -> tier13
            14 -> tier14
            15 -> tier15
            20 -> tier10
            23 -> tier15
            28 -> tier28
            29 -> tier29
            30 -> tier30
            else -> tier6
        }
    }

    fun forStrengthTier(tier: SignalStrengthTier): Color = forRxssNumber(tier.rxssNumber)

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
            SignalMeasurementTier.G2_NO_SIGNAL -> tier15
            SignalMeasurementTier.DEADZONE -> tier0
            SignalMeasurementTier.NO_SIGNAL -> tier10
            SignalMeasurementTier.SEARCHING_2G -> tier11
            SignalMeasurementTier.LIMITED_SERVICE -> tier12
            SignalMeasurementTier.LIMITED_ALT_2G -> tier13
            SignalMeasurementTier.LIMITED_4G_NO_SIGNAL -> tier10
            SignalMeasurementTier.LIMITED_ALT_2G_NO_SIGNAL -> tier15
            SignalMeasurementTier.RSRQ_POOR -> tier14
            SignalMeasurementTier.PERMISSION_REQUIRED,
            SignalMeasurementTier.UNAVAILABLE -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    }
}
