package io.github.cloolalang.notspotdetector.multiue

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * A settings-profile save or load from the master.
 * [apply] is false for a save, which only stores the named profile.
 */
object MultiUeProfileSync {
    data class Event(val apply: Boolean, val payload: String)

    private val _incoming = MutableSharedFlow<Event>(extraBufferCapacity = 4)
    val incoming = _incoming.asSharedFlow()

    fun offer(apply: Boolean, payload: String) {
        _incoming.tryEmit(Event(apply, payload))
    }
}
