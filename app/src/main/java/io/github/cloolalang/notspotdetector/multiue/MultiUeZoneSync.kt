package io.github.cloolalang.notspotdetector.multiue

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Zone-test run, Whole test switch, clear, and save. The master publishes them.
 * A connected slave applies them to its own zone test, including writing its own JPEG.
 */
object MultiUeZoneSync {
    data class Snapshot(
        val running: Boolean,
        val wholeTest: Boolean
    )

    private val _state = MutableStateFlow<Snapshot?>(null)
    val state: StateFlow<Snapshot?> = _state.asStateFlow()

    /** The file-name text after the operator prefix. The operator itself stays on each phone. */
    private val _nameSuffix = MutableStateFlow<String?>(null)
    val nameSuffix: StateFlow<String?> = _nameSuffix.asStateFlow()

    fun set(running: Boolean, wholeTest: Boolean) {
        val next = Snapshot(running, wholeTest)
        if (_state.value == next) return
        _state.value = next
    }

    fun setNameSuffix(suffix: String) {
        if (_nameSuffix.value == suffix) return
        _nameSuffix.value = suffix
    }

    private val _clears = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val clears: SharedFlow<Unit> = _clears.asSharedFlow()

    fun clear() {
        _clears.tryEmit(Unit)
    }

    private val _saves = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val saves: SharedFlow<Unit> = _saves.asSharedFlow()

    fun save() {
        _saves.tryEmit(Unit)
    }
}
