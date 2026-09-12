package io.github.cloolalang.notspotdetector.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import io.github.cloolalang.notspotdetector.R

/** Exo 2 (SIL Open Font License 1.1) — bundled Latin weights for all UI text. */
val Exo2FontFamily = FontFamily(
    Font(R.font.exo2_regular, FontWeight.Normal),
    Font(R.font.exo2_medium, FontWeight.Medium),
    Font(R.font.exo2_semibold, FontWeight.SemiBold),
    Font(R.font.exo2_bold, FontWeight.Bold)
)

private fun TextStyle.withExo2(): TextStyle = copy(fontFamily = Exo2FontFamily)

private val DefaultTypography = Typography()

val Typography = DefaultTypography.copy(
    displayLarge = DefaultTypography.displayLarge.withExo2(),
    displayMedium = DefaultTypography.displayMedium.withExo2(),
    displaySmall = DefaultTypography.displaySmall.withExo2(),
    headlineLarge = DefaultTypography.headlineLarge.withExo2(),
    headlineMedium = DefaultTypography.headlineMedium.withExo2(),
    headlineSmall = DefaultTypography.headlineSmall.withExo2(),
    titleLarge = DefaultTypography.titleLarge.withExo2(),
    titleMedium = DefaultTypography.titleMedium.withExo2(),
    titleSmall = DefaultTypography.titleSmall.withExo2(),
    bodyLarge = DefaultTypography.bodyLarge.withExo2(),
    bodyMedium = DefaultTypography.bodyMedium.withExo2(),
    bodySmall = DefaultTypography.bodySmall.withExo2(),
    labelLarge = DefaultTypography.labelLarge.withExo2(),
    labelMedium = DefaultTypography.labelMedium.withExo2(),
    labelSmall = DefaultTypography.labelSmall.withExo2()
)
