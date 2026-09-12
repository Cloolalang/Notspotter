package io.github.cloolalang.notspotdetector.model

import kotlin.math.roundToInt

/**
 * Piecewise-linear slider mapping that gives the lower end of a range more track travel,
 * so short pulse durations and weaker RSRP values are easier to set.
 */
object ExpandedRangeSlider {
    const val DEFAULT_LOWER_FRACTION = 0.55f
    const val PULSE_DURATION_SPLIT_MS = 200

    fun valueFromPosition(
        position: Float,
        min: Int,
        split: Int,
        max: Int,
        lowerFraction: Float = DEFAULT_LOWER_FRACTION
    ): Int {
        if (max <= min) return min
        val boundedSplit = split.coerceIn(min, max)
        val pos = position.coerceIn(0f, 1f)
        val fraction = lowerFraction.coerceIn(0.05f, 0.95f)
        val value = if (pos <= fraction) {
            min + (boundedSplit - min) * (pos / fraction)
        } else {
            boundedSplit + (max - boundedSplit) * ((pos - fraction) / (1f - fraction))
        }
        return value.roundToInt().coerceIn(min, max)
    }

    fun positionFromValue(
        value: Int,
        min: Int,
        split: Int,
        max: Int,
        lowerFraction: Float = DEFAULT_LOWER_FRACTION
    ): Float {
        if (max <= min) return 0f
        val boundedSplit = split.coerceIn(min, max)
        val fraction = lowerFraction.coerceIn(0.05f, 0.95f)
        val clamped = value.coerceIn(min, max)
        return if (clamped <= boundedSplit) {
            if (boundedSplit == min) 0f
            else fraction * (clamped - min).toFloat() / (boundedSplit - min).toFloat()
        } else {
            if (max == boundedSplit) 1f
            else fraction + (1f - fraction) * (clamped - boundedSplit).toFloat() / (max - boundedSplit).toFloat()
        }.coerceIn(0f, 1f)
    }

    fun snapToStep(value: Int, min: Int, max: Int, step: Int): Int {
        if (step <= 0) return value.coerceIn(min, max)
        val snapped = min + (((value - min).toFloat() / step).roundToInt() * step)
        return snapped.coerceIn(min, max)
    }
}
