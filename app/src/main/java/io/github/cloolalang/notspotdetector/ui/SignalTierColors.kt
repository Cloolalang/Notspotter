package io.github.cloolalang.notspotdetector.ui

import androidx.compose.foundation.isSystemInDarkTheme
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

    /**
     * Light/dark hex pair for each RXSS tier used by the Passive signal thresholds panel, tuned so
     * each tier's accent text stays readable against both a light and a dark Material surface
     * (darker tone for light theme, lighter tone of the same hue for dark theme).
     */
    private val panelTierTones: Map<Int, Pair<Long, Long>> = mapOf(
        0 to (0xFF880E4FL to 0xFFF48FB1L),
        1 to (0xFF1976D2L to 0xFF90CAF9L),
        2 to (0xFF1B5E20L to 0xFFA5D6A7L),
        3 to (0xFF558B2FL to 0xFFAED581L),
        4 to (0xFFE65100L to 0xFFFFD54FL),
        5 to (0xFFE65100L to 0xFFFFB74DL),
        6 to (0xFFC62828L to 0xFFEF9A9AL),
        7 to (0xFF558B2FL to 0xFFAED581L),
        8 to (0xFFE65100L to 0xFFFFB74DL),
        9 to (0xFF006064L to 0xFF80DEEAL),
        10 to (0xFFB71C1CL to 0xFFE57373L),
        11 to (0xFFC62828L to 0xFFEF9A9AL),
        12 to (0xFFE65100L to 0xFFFFCC80L),
        13 to (0xFFAD1457L to 0xFFF06292L),
        14 to (0xFF4E342EL to 0xFFBCAAA4L),
        15 to (0xFF8E0000L to 0xFFEF9A9AL),
        19 to (0xFFE65100L to 0xFFFFCC80L),
        20 to (0xFFB71C1CL to 0xFFE57373L),
        21 to (0xFF8E0000L to 0xFFEF9A9AL),
        22 to (0xFFAD1457L to 0xFFF06292L),
        23 to (0xFF8E0000L to 0xFFEF9A9AL),
        28 to (0xFF004D40L to 0xFF80CBC4L),
        29 to (0xFF0D47A1L to 0xFF64B5F6L),
        30 to (0xFF311B92L to 0xFFB39DDBL),
        // RXSS 31 (WiFi calling, no cellular signal) reuses RXSS 10's tone.
        31 to (0xFFB71C1CL to 0xFFE57373L)
    )

    @Composable
    fun forRxssNumber(number: Int): Color = forTierNumber(number)

    /**
     * Theme-aware accent color for the Passive signal thresholds panel. Picks a darker tone in
     * light theme and a lighter tone of the same hue in dark theme, so section titles and
     * sliders stay legible against the surface behind them.
     */
    @Composable
    fun forTierNumber(number: Int): Color {
        val (lightHex, darkHex) = panelTierTones[number] ?: (0xFFC62828L to 0xFFEF9A9AL)
        return Color(if (isSystemInDarkTheme()) darkHex else lightHex)
    }

    @Composable
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
            SignalMeasurementTier.LIMITED_HOME_4G -> tier12
            SignalMeasurementTier.LIMITED_HOME_2G -> tier13
            SignalMeasurementTier.LIMITED_4G_NO_SIGNAL -> tier10
            SignalMeasurementTier.LIMITED_HOME_2G_NO_SIGNAL -> tier15
            SignalMeasurementTier.LIMITED_ALT_2G_NO_SIGNAL -> tier15
            SignalMeasurementTier.RSRQ_POOR -> tier14
            // RXSS 31 reuses the RXSS 10 (no signal) accent — see RXSS_CATALOGUE.md.
            SignalMeasurementTier.WIFI_CALLING -> tier10
            SignalMeasurementTier.PERMISSION_REQUIRED,
            SignalMeasurementTier.UNAVAILABLE -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    }
}
