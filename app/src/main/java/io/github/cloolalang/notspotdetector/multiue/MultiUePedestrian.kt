package io.github.cloolalang.notspotdetector.multiue

import io.github.cloolalang.notspotdetector.model.MultiUeProtocol
import io.github.cloolalang.notspotdetector.model.PdrPointShare
import io.github.cloolalang.notspotdetector.model.multiUePdrLines
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Pedestrian path shared by the master. Points are metres east and north.
 * Each slave colours them with its own KPI.
 */
object MultiUePedestrian {
    private val _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _points = MutableStateFlow<List<PdrPointShare>>(emptyList())
    val points: StateFlow<List<PdrPointShare>> = _points.asStateFlow()

    private val _lines = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val lines: SharedFlow<String> = _lines.asSharedFlow()

    fun setEnabled(on: Boolean) {
        if (_enabled.value == on) return
        _enabled.value = on
        _lines.tryEmit(MultiUeProtocol.pdrMode(on))
        if (!on) _points.value = emptyList()
    }

    fun publish(previous: List<PdrPointShare>, next: List<PdrPointShare>) {
        _points.value = next
        multiUePdrLines(previous, next).forEach { line -> _lines.tryEmit(line) }
    }

    fun setEnabledFromMaster(on: Boolean) {
        _enabled.value = on
        if (!on) _points.value = emptyList()
    }

    fun addPoint(elapsedMs: Long, eastMeters: Float, northMeters: Float) {
        if (!_enabled.value) return
        _points.value = _points.value + PdrPointShare(elapsedMs, eastMeters, northMeters)
    }

    fun clearPoints() {
        _points.value = emptyList()
    }

    /** What a newly connected slave needs: the mode, then the points so far. */
    fun linesForJoin(): List<String> {
        val mode = listOf(MultiUeProtocol.pdrMode(_enabled.value))
        if (!_enabled.value) return mode
        val points = _points.value.map {
            MultiUeProtocol.pdrPoint(it.elapsedMs, it.eastMeters, it.northMeters)
        }
        return mode + listOf(MultiUeProtocol.pdrClear()) + points
    }
}
