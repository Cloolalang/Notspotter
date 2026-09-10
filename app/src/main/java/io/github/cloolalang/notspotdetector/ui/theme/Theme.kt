package io.github.cloolalang.notspotdetector.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// Brand color scheme — Bondi Blue (cyan) primary, Scarpa Flow (slate) secondary, Sushi (green)
// tertiary/accent. See Color.kt for the full palette and tonal reasoning.
private val DarkColorScheme = darkColorScheme(
    primary = BondiBlueLight,
    onPrimary = OnBondiBlueContainerLight,
    primaryContainer = BondiBlueContainerDark,
    onPrimaryContainer = OnBondiBlueContainerDark,
    secondary = ScarpaFlowLight,
    onSecondary = OnScarpaFlowContainerLight,
    secondaryContainer = ScarpaFlowContainerDark,
    onSecondaryContainer = OnScarpaFlowContainerDark,
    tertiary = SushiLight,
    onTertiary = OnSushiContainerLight,
    tertiaryContainer = SushiContainerDark,
    onTertiaryContainer = OnSushiContainerDark
)

private val LightColorScheme = lightColorScheme(
    primary = BondiBlue,
    onPrimary = OnBondiBlue,
    primaryContainer = BondiBlueContainerLight,
    onPrimaryContainer = OnBondiBlueContainerLight,
    secondary = ScarpaFlow,
    onSecondary = OnScarpaFlow,
    secondaryContainer = ScarpaFlowContainerLight,
    onSecondaryContainer = OnScarpaFlowContainerLight,
    tertiary = Sushi,
    onTertiary = OnSushi,
    tertiaryContainer = SushiContainerLight,
    onTertiaryContainer = OnSushiContainerLight
)

@Composable
fun NotspotDetectorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color (Material You, wallpaper-derived) is available on Android 12+, but defaults
    // to off so the brand palette (Bondi Blue / Scarpa Flow / Sushi) is what's actually shown.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}