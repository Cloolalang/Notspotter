package io.github.cloolalang.notspotdetector.multiue

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Asks the open screen to empty the KPI tapes after a graph clear. */
object MultiUeGraphClear {
    private val _tapes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val tapes = _tapes.asSharedFlow()

    fun resetTapes() {
        _tapes.tryEmit(Unit)
    }
}
