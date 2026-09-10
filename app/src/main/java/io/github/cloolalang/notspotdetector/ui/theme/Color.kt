package io.github.cloolalang.notspotdetector.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Brand palette — see brand guidance: cyan is the primary/logo color, slate is the secondary
 * corporate tone, green is the accent ("sustainability") tone. Each brand color has a light-theme
 * value (used as-is, with a dark "on" color for containers) and a lighter tint used as the
 * corresponding accent in dark theme, following Material 3's tonal convention of using a lighter,
 * lower-saturation tone of the same hue for dark surfaces.
 */

// Bondi Blue (Cyan) — primary brand / logo color.
val BondiBlue = Color(0xFF0097A6)
val BondiBlueLight = Color(0xFF8CD0D7)
val OnBondiBlue = Color(0xFFFFFFFF)
val BondiBlueContainerLight = Color(0xFFD9EFF2)
val OnBondiBlueContainerLight = Color(0xFF003339)
val BondiBlueContainerDark = Color(0xFF00626C)
val OnBondiBlueContainerDark = Color(0xFFCDEEF2)

// Scarpa Flow (Slate Blue/Grey) — secondary / corporate text & tone.
val ScarpaFlow = Color(0xFF485463)
val ScarpaFlowLight = Color(0xFFB6BBC1)
val OnScarpaFlow = Color(0xFFFFFFFF)
val ScarpaFlowContainerLight = Color(0xFFE4E5E8)
val OnScarpaFlowContainerLight = Color(0xFF1E2833)
val ScarpaFlowContainerDark = Color(0xFF323B45)
val OnScarpaFlowContainerDark = Color(0xFFD8DCE1)

// Sushi (Green) — accent / sustainability tone.
val Sushi = Color(0xFF79A52B)
val SushiLight = Color(0xFFC3D7A0)
val OnSushi = Color(0xFF14210A)
val SushiContainerLight = Color(0xFFEBF2DF)
val OnSushiContainerLight = Color(0xFF1F2D0C)
val SushiContainerDark = Color(0xFF4F6B1C)
val OnSushiContainerDark = Color(0xFFE3EFD3)
