package io.github.cloolalang.notspotdetector.multiue

import io.github.cloolalang.notspotdetector.model.AppSettingsSnapshot
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * The link service posts a master's settings here. The screen applies them when this phone
 * is a slave with settings sync on.
 */
object MultiUeSettingsSync {
    private val _incoming = MutableSharedFlow<AppSettingsSnapshot>(extraBufferCapacity = 1)
    val incoming = _incoming.asSharedFlow()

    private val _revision = MutableStateFlow(0)
    val revision: StateFlow<Int> = _revision.asStateFlow()

    fun offer(snapshot: AppSettingsSnapshot) {
        _incoming.tryEmit(snapshot)
    }

    /** Settings that are not held in [io.github.cloolalang.notspotdetector.MonitorState]. */
    fun poke() {
        _revision.update { it + 1 }
    }
}
