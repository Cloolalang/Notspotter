package io.github.cloolalang.notspotdetector.multiue

import io.github.cloolalang.notspotdetector.model.MultiUeProtocol
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * How far down the main screen is scrolled, as a fraction of the scroll range
 * from 0 to [MultiUeProtocol.SCROLL_SCALE].
 * The master publishes it. A connected slave applies it.
 */
object MultiUeScroll {
    private val _fraction = MutableStateFlow<Int?>(null)
    val fraction: StateFlow<Int?> = _fraction.asStateFlow()

    fun publish(fraction: Int) {
        val next = fraction.coerceIn(0, MultiUeProtocol.SCROLL_SCALE)
        if (_fraction.value == next) return
        _fraction.value = next
    }

    fun replace(fraction: Int) {
        publish(fraction)
    }
}
