package io.github.cloolalang.notspotdetector.ui

import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object TierSliderSupport {
    /** One discrete step per dBm/dB so slider values always align with stored integers. */
    fun discreteIntRangeSteps(valueRange: IntRange): Int {
        if (valueRange.first >= valueRange.last) return 0
        return valueRange.last - valueRange.first
    }

    fun tierClickIndexSteps(minStep: Int, maxStep: Int): Int {
        if (minStep >= maxStep) return 0
        return (maxStep - minStep - 1).coerceAtLeast(0)
    }

    fun isValidIntRange(valueRange: IntRange): Boolean {
        return valueRange.first <= valueRange.last
    }

    fun isValidClickIndexRange(minStep: Int, maxStep: Int): Boolean {
        return minStep < maxStep
    }
}

@Composable
fun tierSliderColors(accentColor: Color) = SliderDefaults.colors(
    thumbColor = accentColor,
    activeTrackColor = accentColor,
    inactiveTrackColor = accentColor.copy(alpha = 0.28f)
)
