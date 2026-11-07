package io.github.cloolalang.notspotdetector.multiue

import io.github.cloolalang.notspotdetector.model.multiUePinLines
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/** Floor plan JPEG and walk-mark positions shared by the master. */
object MultiUeFloorPlan {
    data class PinPoint(val x: Float, val y: Float)

    private val _jpeg = MutableStateFlow<ByteArray?>(null)
    val jpeg: StateFlow<ByteArray?> = _jpeg.asStateFlow()

    private val _pins = MutableStateFlow<List<PinPoint>>(emptyList())
    val pins: StateFlow<List<PinPoint>> = _pins.asStateFlow()

    private val _pinLines = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val pinLines: SharedFlow<String> = _pinLines.asSharedFlow()

    fun setJpeg(bytes: ByteArray) {
        if (bytes.isEmpty()) return
        _jpeg.value = bytes
    }

    fun clearPlan() {
        _jpeg.value = ByteArray(0)
    }

    fun publishPins(previous: List<Pair<Float, Float>>, next: List<Pair<Float, Float>>) {
        _pins.value = next.map { PinPoint(it.first, it.second) }
        multiUePinLines(previous, next).forEach { line -> _pinLines.tryEmit(line) }
    }

    fun clearPins() {
        _pins.value = emptyList()
    }

    fun undoPin() {
        val current = _pins.value
        if (current.isEmpty()) return
        _pins.value = current.dropLast(1)
    }

    fun addPin(x: Float, y: Float) {
        _pins.value = _pins.value + PinPoint(x, y)
    }
}
