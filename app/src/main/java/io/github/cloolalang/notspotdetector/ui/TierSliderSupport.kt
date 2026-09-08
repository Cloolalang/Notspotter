package io.github.cloolalang.notspotdetector.ui

import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import io.github.cloolalang.notspotdetector.model.PassiveSignalSettings

object TierSliderSupport {
    /** Material sliders misbehave with thousands of discrete steps. */
    const val MAX_TIER_CLICK_SLIDER_STEPS = 400

    /** One discrete step per dBm/dB so slider values always align with stored integers. */
    fun discreteIntRangeSteps(valueRange: IntRange): Int {
        if (valueRange.first >= valueRange.last) return 0
        return valueRange.last - valueRange.first
    }

    fun tierClickIndexSteps(minStep: Int, maxStep: Int): Int {
        if (minStep >= maxStep) return 0
        return (maxStep - minStep - 1).coerceAtLeast(0)
    }

    /** Picks a coarse enough step size to keep tier click sliders responsive. */
    fun tierClickSliderStepSizeMs(minUiMs: Int): Int {
        val maxMs = PassiveSignalSettings.MAX_TIER_CLICK_INTERVAL_MS
        val span = maxMs - minUiMs
        if (span <= 0) return PassiveSignalSettings.TIER_CLICK_INTERVAL_STEP_MS

        val rawStep = (span + MAX_TIER_CLICK_SLIDER_STEPS - 1) / MAX_TIER_CLICK_SLIDER_STEPS
        return when {
            rawStep <= 10 -> 10
            rawStep <= 25 -> 25
            rawStep <= 50 -> 50
            rawStep <= 100 -> 100
            rawStep <= 250 -> 250
            rawStep <= 500 -> 500
            else -> 1_000
        }.coerceAtLeast(PassiveSignalSettings.TIER_CLICK_INTERVAL_STEP_MS)
    }

    fun tierClickSliderIndex(intervalMs: Int, minUiMs: Int, uiStepSize: Int): Int {
        return ((intervalMs.coerceAtLeast(minUiMs) + uiStepSize / 2) / uiStepSize)
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
